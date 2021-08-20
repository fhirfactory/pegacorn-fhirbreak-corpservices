package net.fhirfactory.pegacorn.fhirbreak.corpservices.ldapsync.workshops.transform.bean;

import java.util.ArrayList;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.ContactPoint;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Practitioner;
import org.hl7.fhir.r4.model.PractitionerRole;
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
	
	
	private PractitionerLdapEntry createLdapEntryFromBundle(Bundle bundle) {
		PractitionerLdapEntry ldapEntry = new PractitionerLdapEntry();
	
		Practitioner practitioner = null;
		List<ContactPoint> contactPoints = new ArrayList<>();
		List<Organization> organization = new ArrayList<>();
		PractitionerRole practitionerRole = null;
		
		// Extract all the resources from the bundle.
        for(Bundle.BundleEntryComponent entry: bundle.getEntry()){
            if(entry.getResource().getResourceType().equals(ResourceType.Practitioner)){
                practitioner = (Practitioner)entry.getResource();
            } else if (entry.getResource().getResourceType().equals(ResourceType.PractitionerRole)){
            	practitionerRole = (PractitionerRole)entry.getResource();
            }  else if (entry.getResource().getResourceType().equals(ResourceType.Organization)){
            	organization.add((Organization)entry.getResource());
            }
        }
        
        // Now convert to ldap entry
        ldapEntry.setEmailAddress(practitioner.getIdentifierFirstRep().getValue());
        ldapEntry.setGivenName(practitioner.getNameFirstRep().getGivenAsSingleString());
        ldapEntry.setSurname(practitioner.getNameFirstRep().getFamily());
        ldapEntry.setSuffix(practitioner.getNameFirstRep().getSuffixAsSingleString());
        ldapEntry.setPersonalTitle(practitioner.getNameFirstRep().getPrefixAsSingleString());
        
        LOG.info("Brendan.  email address: {}", ldapEntry.getEmailAddress().getValue());
        
		return ldapEntry;
	}
}
