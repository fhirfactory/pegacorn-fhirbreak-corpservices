package net.fhirfactory.pegacorn.fhirbreak.corpservices.ldapsync.workshops.transform.bean;

import java.util.ArrayList;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.ContactPoint;
import org.hl7.fhir.r4.model.ContactPoint.ContactPointSystem;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.HumanName.NameUse;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Practitioner;
import org.hl7.fhir.r4.model.PractitionerRole;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ResourceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.fhir.parser.IParser;
import net.fhirfactory.pegacorn.buildingblocks.datamodels.ldap.PractitionerLdapEntry;
import net.fhirfactory.pegacorn.components.dataparcel.DataParcelManifest;
import net.fhirfactory.pegacorn.components.dataparcel.DataParcelTypeDescriptor;
import net.fhirfactory.pegacorn.components.dataparcel.valuesets.DataParcelDirectionEnum;
import net.fhirfactory.pegacorn.components.dataparcel.valuesets.DataParcelNormalisationStatusEnum;
import net.fhirfactory.pegacorn.components.dataparcel.valuesets.DataParcelTypeEnum;
import net.fhirfactory.pegacorn.components.dataparcel.valuesets.DataParcelValidationStatusEnum;
import net.fhirfactory.pegacorn.components.dataparcel.valuesets.PolicyEnforcementPointApprovalStatusEnum;
import net.fhirfactory.pegacorn.petasos.model.uow.UoW;
import net.fhirfactory.pegacorn.petasos.model.uow.UoWPayload;
import net.fhirfactory.pegacorn.petasos.model.uow.UoWProcessingOutcomeEnum;
import net.fhirfactory.pegacorn.util.FHIRContextUtility;

/**
 * Transform FHIR resources within a bundle to a {@link PractitionerLdapEntry} and adds to a {@link UoW}
 * 
 * @author Brendan Douglas
 *
 */
@ApplicationScoped
public class TransformFhirToLdapEntry {
    private static final Logger LOG = LoggerFactory.getLogger(TransformFhirToLdapEntry.class);
    
    @Inject
    private FHIRContextUtility fhirContextUtility;
	
	
	public UoW convert(UoW incomingUoW) {
		
        IParser fhirResourceParser = fhirContextUtility.getJsonParser().setPrettyPrint(true);
        Bundle bundle = fhirResourceParser.parseResource(Bundle.class, incomingUoW.getIngresContent().getPayload());
		
		DataParcelTypeDescriptor descriptor = new DataParcelTypeDescriptor();
		descriptor.setDataParcelDefiner("FHIRFactory");
		descriptor.setDataParcelCategory("Operations");
		descriptor.setDataParcelSubCategory("Practitioners");
		descriptor.setDataParcelResource("Practitioner LDAP Entry");

		DataParcelManifest manifest = new DataParcelManifest();
		manifest.setContentDescriptor(descriptor);
		manifest.setDataParcelFlowDirection(DataParcelDirectionEnum.OUTBOUND_DATA_PARCEL);
		manifest.setDataParcelType(DataParcelTypeEnum.GENERAL_DATA_PARCEL_TYPE);
		manifest.setEnforcementPointApprovalStatus(PolicyEnforcementPointApprovalStatusEnum.POLICY_ENFORCEMENT_POINT_APPROVAL_NEGATIVE);
		manifest.setNormalisationStatus(DataParcelNormalisationStatusEnum.DATA_PARCEL_CONTENT_NORMALISATION_TRUE);
		manifest.setValidationStatus(DataParcelValidationStatusEnum.DATA_PARCEL_CONTENT_VALIDATED_TRUE);
		manifest.setSourceSystem("aether-fhirbreak-ldapsync");
		manifest.setInterSubsystemDistributable(false); 
		
    
	    UoWPayload contentPayload = new UoWPayload();
	    contentPayload.setPayload(createLdapEntryFromBundle(bundle).asJson().toString());
	    contentPayload.setPayloadManifest(manifest);
	        
	    UoW newUoW = new UoW(incomingUoW);
	    
	    newUoW.getEgressContent().getPayloadElements().add(contentPayload);
	                
	    // Now, if we've gotten to here, then all is "good" and so we should set the UoW processing status accordingly.
	    newUoW.setProcessingOutcome(UoWProcessingOutcomeEnum.UOW_OUTCOME_SUCCESS);
		
		return newUoW;
	}
	
	
	/**
	 * Transform the resources within the bundle to an LDAP entry.
	 * 
	 * @param bundle
	 * @return
	 */
	private PractitionerLdapEntry createLdapEntryFromBundle(Bundle bundle) {
		PractitionerLdapEntry ldapEntry = new PractitionerLdapEntry(System.getenv("LDAP_SERVER_BASE_DN"));
	
		Practitioner practitioner = null;
		List<Organization> organizations = new ArrayList<>();
		PractitionerRole practitionerRole = null;
		
		// Extract all the resources from the bundle.
        for(Bundle.BundleEntryComponent entry: bundle.getEntry()){
            if(entry.getResource().getResourceType().equals(ResourceType.Practitioner)){
                practitioner = (Practitioner)entry.getResource();
            } else if (entry.getResource().getResourceType().equals(ResourceType.PractitionerRole)){
            	practitionerRole = (PractitionerRole)entry.getResource();
            }  else if (entry.getResource().getResourceType().equals(ResourceType.Organization)){
            	organizations.add((Organization)entry.getResource());
            }
        }
        
        HumanName preferredHumanName = null;
        HumanName officalHumanName = null;
        
        for (HumanName name : practitioner.getName()) {
        	if (name.getUse() == NameUse.OFFICIAL) {
        		officalHumanName = name;
        	} else if (name.getUse() == NameUse.USUAL) {
        		preferredHumanName = name;
        	}
        }
        
        
        ldapEntry.setEmailAddress(practitioner.getIdentifierFirstRep().getValue());
        ldapEntry.setGivenName(officalHumanName.getGivenAsSingleString());
        ldapEntry.setSurname(officalHumanName.getFamily());
        ldapEntry.setSuffix(officalHumanName.getSuffixAsSingleString());
        ldapEntry.setPersonalTitle(officalHumanName.getPrefixAsSingleString());
        ldapEntry.setPreferredName(preferredHumanName.getGivenAsSingleString());
        
        for (ContactPoint contactPoint : practitionerRole.getTelecom()) {
        	if (contactPoint.getSystem() == ContactPointSystem.SMS) {
        		ldapEntry.setMobileNumber(contactPoint.getValue());
        	} else if (contactPoint.getSystem() == ContactPointSystem.PAGER) {
        		ldapEntry.setPager(contactPoint.getValue());
        	} else if (contactPoint.getSystem() == ContactPointSystem.PHONE) {
        		ldapEntry.setTelephoneNumber(contactPoint.getValue());
        	}
        }
                
        ldapEntry.setJobTitle(practitionerRole.getIdentifierFirstRep().getValue());
        

        // Populate the additional practitioner identifiers.
        for (Identifier identifier : practitioner.getIdentifier()) {
        	CodeableConcept identifierType = identifier.getType();
        	
        	if (identifierType.getText().equals("AGS")) {
        		ldapEntry.setAgsNumber(identifier.getValue());
        	} else if (identifierType.getText().equals("IRN")) {
        		ldapEntry.setIRN(identifier.getValue());
        	} else if (identifierType.getText().equals("GS1")) {
        		ldapEntry.setGS1(identifier.getValue());
        	}
        }
        
        // Populate the organisational structure details.
        
        // Get the business unit.
        Reference businessUnitReference = practitionerRole.getOrganization();
        Organization businessUnit = (Organization)getOrganisationComponent(businessUnitReference, organizations);
        ldapEntry.setBusinessUnit(businessUnit.getName());
        
        // Get the subsection
        Reference subsectionReference = businessUnit.getPartOf();
        Organization subsection = (Organization)getOrganisationComponent(subsectionReference, organizations);
        ldapEntry.setSubSection(subsection.getName());
        
        // Get the section
        Reference sectionReference = subsection.getPartOf();
        Organization section = (Organization)getOrganisationComponent(sectionReference, organizations);
        ldapEntry.setSection(section.getName());
        
        // Get the branch
        Reference branchReference = section.getPartOf();
        Organization branch = (Organization)getOrganisationComponent(branchReference, organizations);
        ldapEntry.setBranch(branch.getName());
        
        
        // Get the division
        Reference divisionReference = branch.getPartOf();
        Organization division = (Organization)getOrganisationComponent(divisionReference, organizations);
        ldapEntry.setDivision(division.getName());
        
		return ldapEntry;
	}
	
	
	private Resource getOrganisationComponent(Reference reference, List<Organization> organizations) {
		for (Organization organisation : organizations) {
			if (organisation.getName().equals(reference.getIdentifier().getValue())) {
				return organisation;
			}
		}
		
		return null;
	}
}
