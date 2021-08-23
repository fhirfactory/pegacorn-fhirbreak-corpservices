package net.fhirfactory.pegacorn.fhirbreak.corpservices.ldapscanner.workshops.transform.beans;

import java.sql.Date;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.ContactPoint;
import org.hl7.fhir.r4.model.ContactPoint.ContactPointSystem;
import org.hl7.fhir.r4.model.ContactPoint.ContactPointUse;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.HumanName.NameUse;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Identifier.IdentifierUse;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Period;
import org.hl7.fhir.r4.model.Practitioner;
import org.hl7.fhir.r4.model.PractitionerRole;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.codesystems.OrganizationType;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.fhir.parser.IParser;
import net.fhirfactory.pegacorn.buildingblocks.datamodels.ldap.PractitionerLdapEntry;
import net.fhirfactory.pegacorn.common.model.generalid.FDN;
import net.fhirfactory.pegacorn.common.model.generalid.RDN;
import net.fhirfactory.pegacorn.components.dataparcel.DataParcelManifest;
import net.fhirfactory.pegacorn.components.dataparcel.DataParcelTypeDescriptor;
import net.fhirfactory.pegacorn.components.dataparcel.valuesets.DataParcelDirectionEnum;
import net.fhirfactory.pegacorn.components.dataparcel.valuesets.DataParcelNormalisationStatusEnum;
import net.fhirfactory.pegacorn.components.dataparcel.valuesets.DataParcelTypeEnum;
import net.fhirfactory.pegacorn.components.dataparcel.valuesets.DataParcelTypeKeyEnum;
import net.fhirfactory.pegacorn.components.dataparcel.valuesets.DataParcelValidationStatusEnum;
import net.fhirfactory.pegacorn.components.dataparcel.valuesets.PolicyEnforcementPointApprovalStatusEnum;
import net.fhirfactory.pegacorn.internals.fhir.r4.resources.bundle.BundleContentHelper;
import net.fhirfactory.pegacorn.internals.fhir.r4.resources.organization.factories.OrganizationFactory;
import net.fhirfactory.pegacorn.internals.fhir.r4.resources.organization.factories.OrganizationResourceHelpers;
import net.fhirfactory.pegacorn.internals.fhir.r4.resources.practitioner.factories.PractitionerFactory;
import net.fhirfactory.pegacorn.internals.fhir.r4.resources.practitioner.factories.PractitionerResourceHelpers;
import net.fhirfactory.pegacorn.internals.fhir.r4.resources.practitionerrole.factories.PractitionerRoleFactory;
import net.fhirfactory.pegacorn.internals.fhir.r4.resources.practitionerrole.factories.PractitionerRoleResourceHelper;
import net.fhirfactory.pegacorn.internals.fhir.r4.resources.practitionerrole.factories.RoleFactory;
import net.fhirfactory.pegacorn.internals.fhir.r4.resources.resource.datatypes.ContactPointFactory;
import net.fhirfactory.pegacorn.petasos.model.uow.UoW;
import net.fhirfactory.pegacorn.petasos.model.uow.UoWPayload;
import net.fhirfactory.pegacorn.util.FHIRContextUtility;

/**
 * Create FHIR resoures from a single LDAP entry and adds them to a bundle.
 * 
 * @author Brendan Douglas
 *
 */
@ApplicationScoped
public class PractitonerFHIRBundleBuilder {
	
    private static final Logger LOG = LoggerFactory.getLogger(PractitonerFHIRBundleBuilder.class);
    
    @Inject
    private PractitionerResourceHelpers practitionerResourceHelper;
    
    @Inject
    private PractitionerFactory practitionerFactory;
    
    @Inject
    private ContactPointFactory contactPointFactory;
    
    @Inject
    private OrganizationFactory organizationFactory;
    
    @Inject
    private OrganizationResourceHelpers organizationResourceHelper;
    
    @Inject
    private BundleContentHelper bundleContentHelper;
    
    @Inject
    private FHIRContextUtility fhirContextUtility;
   
    @Inject
    private PractitionerRoleFactory practitionerRoleFactory;
    
    @Inject
    private RoleFactory roleFactory;
    
    @Inject
    private PractitionerRoleResourceHelper practitionerRoleResourceHelper;
    
    public UoW buildFHIRBundle(UoW incomingUoW) {
		
		// The UoW contains a JSON representation of the LDAP entry.  Retrieve this and convert.
        JSONObject jsonLdapEntry = new JSONObject(incomingUoW.getIngresContent().getPayload());
        PractitionerLdapEntry ldapEntry = new PractitionerLdapEntry(System.getenv("LDAP_SERVER_BASE_DN"), jsonLdapEntry); 
       
        // Create all the resources from a single LDAP entry.
        Practitioner practitioner = createPractitioner(ldapEntry);
        List<ContactPoint> contactPoints = createContactPoints(ldapEntry);
        List<Organization> organization = createOrganizationStructure(ldapEntry); 
        PractitionerRole practitionerRole = createPractitionerRole(ldapEntry, practitioner, contactPoints, organization.get(4));
        
        List<Resource>resources = new ArrayList<>();
        resources.add(practitioner);
        resources.addAll(organization);
        resources.add(practitionerRole);
       

        // Create a bundle.
        Bundle fhirBundle = new Bundle();
        fhirBundle.setIdentifier(constuctBundleIdentifier());
        fhirBundle.setType(Bundle.BundleType.MESSAGE);
        fhirBundle.setTimestamp(Date.from(Instant.now()));
        
     
        // Add all the resources to the bundle.
        for (Resource resource : resources) {
        	fhirBundle.addEntry(createBundleEntry(resource));
        }
        
        
        // Add the bundle to a UoW.
        IParser fhirResourceParser = fhirContextUtility.getJsonParser().setPrettyPrint(true);
        String payloadAsString = fhirResourceParser.encodeResourceToString(fhirBundle);
        
        DataParcelTypeDescriptor descriptor = new DataParcelTypeDescriptor();
		descriptor.setDataParcelDefiner("FHIRFactory");
		descriptor.setDataParcelCategory("Operations");
		descriptor.setDataParcelSubCategory("Practitioners");
		descriptor.setDataParcelResource("Practitioner FHIR Bundle");

		DataParcelManifest manifest = new DataParcelManifest();
		manifest.setContentDescriptor(descriptor);
		manifest.setDataParcelFlowDirection(DataParcelDirectionEnum.OUTBOUND_DATA_PARCEL);
		manifest.setDataParcelType(DataParcelTypeEnum.GENERAL_DATA_PARCEL_TYPE);
		manifest.setEnforcementPointApprovalStatus(PolicyEnforcementPointApprovalStatusEnum.POLICY_ENFORCEMENT_POINT_APPROVAL_NEGATIVE);
		manifest.setNormalisationStatus(DataParcelNormalisationStatusEnum.DATA_PARCEL_CONTENT_NORMALISATION_TRUE);
		manifest.setValidationStatus(DataParcelValidationStatusEnum.DATA_PARCEL_CONTENT_VALIDATED_TRUE);
		manifest.setSourceSystem("aether-fhirbreak-ldapscanner");
		manifest.setInterSubsystemDistributable(false); 

        UoWPayload contentPayload = new UoWPayload();
        
        contentPayload.setPayloadManifest(manifest);
        contentPayload.setPayload(payloadAsString);
        
        UoW newUoW = new UoW(incomingUoW);
        
        newUoW.getEgressContent().addPayloadElement(contentPayload);
                   
        return newUoW;
	}
    
    
    private Bundle.BundleEntryComponent createBundleEntry(Resource resource) {
        Bundle.BundleEntryComponent bundleEntry = new Bundle.BundleEntryComponent();
        bundleEntry.setResource(resource);
        
        return bundleEntry;
    }

    
    protected Identifier constuctBundleIdentifier() {
        Identifier bundleIdentifier = new Identifier();

        bundleIdentifier.setUse(Identifier.IdentifierUse.SECONDARY);

        CodeableConcept identifierType = new CodeableConcept();
        Coding identifierTypeCoding = new Coding();
        
        identifierTypeCoding.setCode("RI");
        identifierTypeCoding.setSystem("http://terminology.hl7.org/ValueSet/v2-0203");
        identifierType.addCoding(identifierTypeCoding);
        identifierType.setText("Generalized Resource Identifier");
        bundleIdentifier.setType(identifierType);
        bundleIdentifier.setSystem("AETHER"); 

        FDN fdn = new FDN();
        fdn.appendRDN(new RDN(DataParcelTypeKeyEnum.DATASET_DEFINER.getTopicKey(), "ACT Health"));
        fdn.appendRDN(new RDN(DataParcelTypeKeyEnum.DATASET_CATEGORY.getTopicKey(), "Operations"));
        fdn.appendRDN(new RDN(DataParcelTypeKeyEnum.DATASET_SUBCATEGORY.getTopicKey(), "Practitioners"));
        fdn.appendRDN(new RDN(DataParcelTypeKeyEnum.DATASET_RESOURCE.getTopicKey(), "Practitioners"));
        
        String token = fdn.getToken().getUnqualifiedToken();
        bundleIdentifier.setValue(token);
        bundleIdentifier.setPeriod(new Period());       
        bundleIdentifier.setAssigner(new Reference("ACT Health - AETHER"));
        
        return bundleIdentifier;
    }

    
    /**
     * Create the practitioner
     * 
     * @param ldapEntry
     * @return
     */
    private Practitioner createPractitioner(PractitionerLdapEntry ldapEntry) {
    	HumanName officalHumanName = practitionerResourceHelper.constructHumanName(ldapEntry.getGivenName(), ldapEntry.getSurname(), null, ldapEntry.getPersonalTitle(), ldapEntry.getSuffix(), NameUse.OFFICIAL);
    	Identifier emailIdentifier = practitionerResourceHelper.buildPractitionerIdentifierFromEmail(ldapEntry.getEmailAddress());
    	HumanName preferredHumanName = practitionerResourceHelper.constructHumanName(ldapEntry.getPreferredName(), ldapEntry.getSurname(), null, ldapEntry.getPersonalTitle(), ldapEntry.getSuffix(), NameUse.USUAL);
    	
    	Practitioner practitioner = practitionerFactory.buildPractitioner(officalHumanName, emailIdentifier);
   	
    	practitioner.addName(preferredHumanName);
    	
    	Identifier ags = createAdditionalPractitionerIdentifiers(ldapEntry.getAgsNumber(), "AGS");
    	Identifier gs1 = createAdditionalPractitionerIdentifiers(ldapEntry.getGS1(), "GS1");
    	Identifier irn = createAdditionalPractitionerIdentifiers(ldapEntry.getIRN(), "IRN");
    	
    	practitioner.addIdentifier(ags);
    	practitioner.addIdentifier(gs1);
    	practitioner.addIdentifier(irn);
    	
    	return practitioner;
    }
    
    
    /**
     * Create the contact points
     * 
     * @param ldapEntry
     * @return
     */
    public List<ContactPoint> createContactPoints(PractitionerLdapEntry ldapEntry) {
    	List<ContactPoint> contactPoints = new ArrayList<>();
    	
    	contactPoints.add(contactPointFactory.buildContactPoint(ldapEntry.getMobileNumber(), ContactPointUse.WORK, ContactPointSystem.SMS, 0));
    	contactPoints.add(contactPointFactory.buildContactPoint(ldapEntry.getPhoneNumber(), ContactPointUse.WORK, ContactPointSystem.PHONE, 1));
    	contactPoints.add(contactPointFactory.buildContactPoint(ldapEntry.getPager(), ContactPointUse.WORK, ContactPointSystem.PAGER, 2));
    	
    	return contactPoints;
    }
    
    
    /**
     * Create the organisational structure.
     * 
     * @param ldapEntry
     * @return
     */
    public List<Organization> createOrganizationStructure(PractitionerLdapEntry ldapEntry) {    	
    	List<Organization> organizationStructure = new ArrayList<>();    
  
    	organizationStructure.add(organizationFactory.buildOrganization(ldapEntry.getDivision(), OrganizationType.OTHER, null));
    	organizationStructure.add(organizationFactory.buildOrganization(ldapEntry.getBranch(), OrganizationType.OTHER, organizationResourceHelper.buildOrganizationReference(ldapEntry.getDivision(), IdentifierUse.OFFICIAL)));
    	organizationStructure.add(organizationFactory.buildOrganization(ldapEntry.getSection(), OrganizationType.OTHER, organizationResourceHelper.buildOrganizationReference(ldapEntry.getBranch(), IdentifierUse.OFFICIAL)));
    	organizationStructure.add(organizationFactory.buildOrganization(ldapEntry.getSubSection(), OrganizationType.OTHER, organizationResourceHelper.buildOrganizationReference(ldapEntry.getSection(), IdentifierUse.OFFICIAL)));
    	organizationStructure.add(organizationFactory.buildOrganization(ldapEntry.getBusinessUnit(), OrganizationType.OTHER, organizationResourceHelper.buildOrganizationReference(ldapEntry.getSubSection(), IdentifierUse.OFFICIAL)));
    	
    	return organizationStructure;
    }
    
    
    /**
     * Create the practitioner role
     * 
     * @param ldapEntry
     * @param practitioner
     * @return
     */
    public PractitionerRole createPractitionerRole(PractitionerLdapEntry ldapEntry, Practitioner practitioner, List<ContactPoint> contactPoints, Organization organisation) {
    	CodeableConcept roleCodeableConcept = roleFactory.buildRole(ldapEntry.getJobTitle());
    	   	
    	// Build the practitioner role
    	List<CodeableConcept>codeableConcepts = new ArrayList<>();
    	codeableConcepts.add(roleCodeableConcept);
    	PractitionerRole practitionerRole = practitionerRoleFactory.buildPractitionerRole(ldapEntry.getJobTitle(), ldapEntry.getJobTitle(), codeableConcepts);
   	
    	
    	// Associate the practitioner role with the practitioner.
    	Reference practitionerReference = practitionerResourceHelper.buildPractitionerReferenceUsingEmail(practitioner.getIdentifierFirstRep().getValue());
    	practitionerRole.setPractitioner(practitionerReference);
    	

    	// Add the organisation reference.
    	Reference organisationReference = organizationResourceHelper.buildOrganizationReference(organisation.getName(), organisation.getIdentifierFirstRep().getUse());
    	practitionerRole.setOrganization(organisationReference);
    	
    	// Add the contact points.
    	practitionerRole.setTelecom(contactPoints);
    	    	
    	return practitionerRole;	
    }
    
    
    /**
     * 
     * 
     * @param value
     * @param practitionerIdentifierType
     * @return
     */
    private Identifier createAdditionalPractitionerIdentifiers(String value, String practitionerIdentifierType) {
    	Identifier identifier = new Identifier();
        identifier.setUse(Identifier.IdentifierUse.OFFICIAL);
        identifier.setValue(value);
        
        CodeableConcept identifierType = new CodeableConcept();
        identifierType.setText(practitionerIdentifierType);
        
        identifier.setType(identifierType);
        
        Period validPeriod = new Period();
        validPeriod.setStart(Date.from(Instant.now()));
        identifier.setPeriod(validPeriod);
    	
        return identifier;
    }
}
