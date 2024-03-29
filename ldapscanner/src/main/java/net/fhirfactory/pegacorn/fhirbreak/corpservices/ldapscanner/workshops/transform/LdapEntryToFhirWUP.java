package net.fhirfactory.pegacorn.fhirbreak.corpservices.ldapscanner.workshops.transform;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import net.fhirfactory.pegacorn.components.dataparcel.DataParcelManifest;
import net.fhirfactory.pegacorn.components.dataparcel.DataParcelTypeDescriptor;
import net.fhirfactory.pegacorn.components.dataparcel.valuesets.DataParcelDirectionEnum;
import net.fhirfactory.pegacorn.components.dataparcel.valuesets.DataParcelNormalisationStatusEnum;
import net.fhirfactory.pegacorn.components.dataparcel.valuesets.DataParcelTypeEnum;
import net.fhirfactory.pegacorn.components.dataparcel.valuesets.DataParcelValidationStatusEnum;
import net.fhirfactory.pegacorn.components.dataparcel.valuesets.PolicyEnforcementPointApprovalStatusEnum;
import net.fhirfactory.pegacorn.components.interfaces.topology.WorkshopInterface;
import net.fhirfactory.pegacorn.fhirbreak.corpservices.ldapscanner.workshops.transform.beans.PractitonerFHIRBundleBuilder;
import net.fhirfactory.pegacorn.workshops.TransformWorkshop;
import net.fhirfactory.pegacorn.wups.archetypes.petasosenabled.messageprocessingbased.MOAStandardWUP;

/**
 * Transforms an LDAP entry to a FHIR resource.
 * 
 * @author brendan_douglas_a
 *
 */
public abstract class LdapEntryToFhirWUP extends MOAStandardWUP {
	    
    private static String WUP_VERSION = "1.0.0";
    
	@Inject
	private TransformWorkshop workshop;
	
	@Inject
	private PractitonerFHIRBundleBuilder fhirBundleBuilder;
	
	protected abstract String getSourceSystem();

	@Override
	protected List<DataParcelManifest> specifySubscriptionTopics() {
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
		manifest.setSourceSystem(getSourceSystem());
		manifest.setInterSubsystemDistributable(false); 
		
		List<DataParcelManifest> subscribedTopics = new ArrayList<>();

        subscribedTopics.add(manifest);
        
        return (subscribedTopics);
	}

	@Override
	protected String specifyWUPInstanceName() {
		return "ldapEntryToFHIR";
	}

	@Override
	protected String specifyWUPInstanceVersion() {
		return WUP_VERSION;
	}

	@Override
	protected WorkshopInterface specifyWorkshop() {
		return workshop;
	}

	@Override
	public void configure() throws Exception {
		fromIncludingPetasosServices(this.ingresFeed())
         .bean(fhirBundleBuilder, "buildFHIRBundle")
		 .to(this.egressFeed());
	}
}
