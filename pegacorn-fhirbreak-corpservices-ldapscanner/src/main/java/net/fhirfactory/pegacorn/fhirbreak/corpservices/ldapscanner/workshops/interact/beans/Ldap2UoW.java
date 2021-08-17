package net.fhirfactory.pegacorn.fhirbreak.corpservices.ldapscanner.workshops.interact.beans;

import java.io.IOException;
import java.util.List;

import org.apache.directory.api.ldap.model.cursor.CursorException;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.fhir.model.dstu2.resource.Bundle.Entry;
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

/**
 * Converts a list of LDAP {@link Entry} objects to a {@link UoW} 
 * 
 * @author Brendan Douglas
 *
 */
public class Ldap2UoW {
    private static final Logger LOG = LoggerFactory.getLogger(Ldap2UoW.class);
	
	 /**
	  * Read from LDAP and add the content to the unit of work.
	  * 
	 * @return
	 */
	public UoW encapsulateLdapData(List<PractitionerLdapEntry>entries) throws LdapException, IOException, CursorException {
		
		DataParcelTypeDescriptor descriptor = new DataParcelTypeDescriptor();
		descriptor.setDataParcelDefiner("FHIRFactory");
		descriptor.setDataParcelCategory("Operations");
		descriptor.setDataParcelSubCategory("Practitioners");
		descriptor.setDataParcelResource("LDAPRecord");

		DataParcelManifest manifest = new DataParcelManifest();
		manifest.setContentDescriptor(descriptor);
		manifest.setDataParcelFlowDirection(DataParcelDirectionEnum.INBOUND_DATA_PARCEL);
		manifest.setDataParcelType(DataParcelTypeEnum.GENERAL_DATA_PARCEL_TYPE);
		manifest.setEnforcementPointApprovalStatus(PolicyEnforcementPointApprovalStatusEnum.POLICY_ENFORCEMENT_POINT_APPROVAL_POSITIVE);
		manifest.setNormalisationStatus(DataParcelNormalisationStatusEnum.DATA_PARCEL_CONTENT_NORMALISATION_TRUE);
		manifest.setValidationStatus(DataParcelValidationStatusEnum.DATA_PARCEL_CONTENT_VALIDATED_TRUE);
		manifest.setSourceSystem("aether-fhirbreak-ldapscanner");
		manifest.setInterSubsystemDistributable(false); 

        UoWPayload emptyPayload = new UoWPayload();
        emptyPayload.setPayloadManifest(manifest);
        
        UoW newUoW = new UoW(emptyPayload);
        
        for (PractitionerLdapEntry entry : entries) {            
            UoWPayload contentPayload = new UoWPayload();
        
            contentPayload.setPayloadManifest(manifest);
            contentPayload.setPayload(entry.asJson().toString());
            
            newUoW.getEgressContent().getPayloadElements().add(contentPayload);
        }
        
        // Now, if we've gotten to here, then all is "good" and so we should set the UoW processing status accordingly.
        newUoW.setProcessingOutcome(UoWProcessingOutcomeEnum.UOW_OUTCOME_SUCCESS);
        
        return newUoW;
	 }
}
