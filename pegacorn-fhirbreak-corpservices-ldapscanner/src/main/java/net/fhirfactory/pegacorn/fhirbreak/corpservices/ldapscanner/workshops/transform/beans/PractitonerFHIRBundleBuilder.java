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
import net.fhirfactory.pegacorn.internals.fhir.r4.resources.resource.datatypes.ContactPointFactory;
import net.fhirfactory.pegacorn.petasos.model.uow.UoW;
import net.fhirfactory.pegacorn.petasos.model.uow.UoWPayload;
import net.fhirfactory.pegacorn.util.FHIRContextUtility;

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
    
    public UoW buildFHIRBundle(UoW incomingUoW) {
		
		// The UoW contains a JSON representation of the LDAP entry.  Retrieve this and convert.
        JSONObject jsonLdapEntry = new JSONObject(incomingUoW.getIngresContent().getPayload());
        PractitionerLdapEntry ldapEntry = new PractitionerLdapEntry("dc=practitioners,dc=com", jsonLdapEntry); 
       
        Practitioner practitioner = createPractitioner(ldapEntry);
        List<ContactPoint> contactPoints = createContactPoints(ldapEntry);
        List<Organization> organization = createOrganizationStructure(ldapEntry); 
        PractitionerRole practitionerRole = createPractitionerRole(ldapEntry, practitioner);
        
        List<Resource>resources = new ArrayList<>();
        resources.add(practitioner);
        resources.addAll(organization);
        resources.add(practitionerRole);
              
        Bundle fhirBundle = new Bundle();
        fhirBundle.setIdentifier(constuctBundleIdentifier());
        fhirBundle.setType(Bundle.BundleType.MESSAGE);
        fhirBundle.setTimestamp(Date.from(Instant.now()));
        
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
        contentPayload.setPayload(practitioner.getNameFirstRep().getGivenAsSingleString());
        
        UoW newUoW = new UoW(incomingUoW);
        
        newUoW.getEgressContent().addPayloadElement(contentPayload);
            
        LOG.info("Brendan:.  Practitioner name: {}", practitioner.getNameFirstRep().getGivenAsSingleString());
        
        return newUoW;
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

    
    private Practitioner createPractitioner(PractitionerLdapEntry ldapEntry) {
    	HumanName humanName = practitionerResourceHelper.constructHumanName(ldapEntry.getGivenName().getValue(), ldapEntry.getSurname().getValue(), null, ldapEntry.getPersonalTitle().getValue(), ldapEntry.getSuffix().getValue(), NameUse.OFFICIAL);
    	Identifier emailIdentifier = practitionerResourceHelper.buildPractitionerIdentifierFromEmail(ldapEntry.getEmailAddress().getValue());
    	
    	return practitionerFactory.buildPractitioner(humanName, emailIdentifier);
    }
    
    
    public List<ContactPoint> createContactPoints(PractitionerLdapEntry ldapEntry) {
    	List<ContactPoint> contactPoints = new ArrayList<>();
    	
    	contactPoints.add(contactPointFactory.buildContactPoint(ldapEntry.getMobileNumber().getValue(), ContactPointUse.WORK, ContactPointSystem.PHONE, 0));
    	contactPoints.add(contactPointFactory.buildContactPoint(ldapEntry.getPhoneNumber().getValue(), ContactPointUse.WORK, ContactPointSystem.PHONE, 1));
    	contactPoints.add(contactPointFactory.buildContactPoint(ldapEntry.getPager().getValue(), ContactPointUse.WORK, ContactPointSystem.PAGER, 2));
    	
    	return contactPoints;
    }
    
    
    public List<Organization> createOrganizationStructure(PractitionerLdapEntry ldapEntry) {    	
    	List<Organization> organizationStructure = new ArrayList<>();    
  
    	organizationStructure.add(organizationFactory.buildOrganization(ldapEntry.getDivision().getValue(), OrganizationType.OTHER, null));
    	organizationStructure.add(organizationFactory.buildOrganization(ldapEntry.getBranch().getValue(), OrganizationType.OTHER, organizationResourceHelper.buildOrganizationReference(ldapEntry.getDivision().getValue(), IdentifierUse.OFFICIAL)));
    	organizationStructure.add(organizationFactory.buildOrganization(ldapEntry.getSubSection().getValue(), OrganizationType.OTHER, organizationResourceHelper.buildOrganizationReference(ldapEntry.getBranch().getValue(), IdentifierUse.OFFICIAL)));
    	organizationStructure.add(organizationFactory.buildOrganization(ldapEntry.getSection().getValue(), OrganizationType.OTHER, organizationResourceHelper.buildOrganizationReference(ldapEntry.getSubSection().getValue(), IdentifierUse.OFFICIAL)));
 //   	organizationStructure.add(organizationFactory.buildOrganization(ldapEntry.getBusinessUnit().getValue(), OrganizationType.OTHER, organizationResourceHelper.buildOrganizationReference(ldapEntry.getSection().getValue(), IdentifierUse.OFFICIAL)));
    	
    	return organizationStructure;
    }
    
    
    public PractitionerRole createPractitionerRole(PractitionerLdapEntry ldapEntry, Practitioner practitioner) {
    	PractitionerRole practitionerRole = new PractitionerRole();
    	
    	practitionerRole.setPractitionerTarget(practitioner);
    	practitionerRole.addCode().addCoding().setDisplay(ldapEntry.getJobTitle().getValue());
    	
    	return practitionerRole;	
    }
}
