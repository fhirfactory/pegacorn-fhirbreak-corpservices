package net.fhirfactory.pegacorn.fhirbreak.corpservices.ldapscanner.workshops.transform;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fhirfactory.pegacorn.components.dataparcel.DataParcelManifest;
import net.fhirfactory.pegacorn.components.dataparcel.DataParcelTypeDescriptor;
import net.fhirfactory.pegacorn.components.dataparcel.valuesets.DataParcelDirectionEnum;
import net.fhirfactory.pegacorn.components.dataparcel.valuesets.DataParcelNormalisationStatusEnum;
import net.fhirfactory.pegacorn.components.dataparcel.valuesets.DataParcelTypeEnum;
import net.fhirfactory.pegacorn.components.dataparcel.valuesets.DataParcelValidationStatusEnum;
import net.fhirfactory.pegacorn.components.dataparcel.valuesets.PolicyEnforcementPointApprovalStatusEnum;
import net.fhirfactory.pegacorn.components.interfaces.topology.WorkshopInterface;
import net.fhirfactory.pegacorn.workshops.TransformWorkshop;
import net.fhirfactory.pegacorn.wups.archetypes.petasosenabled.messageprocessingbased.MOAStandardWUP;

/**
 * Transforms an LDAP entry to a FHIR resource.
 * 
 * @author brendan_douglas_a
 *
 */
public abstract class LdapEntryToFhirWUP extends MOAStandardWUP {
	
    private static final Logger LOG = LoggerFactory.getLogger(LdapEntryToFhirWUP.class);
    
    private static String WUP_VERSION = "1.0.0";
    
	@Inject
	private TransformWorkshop workshop;

	@Override
	protected Logger specifyLogger() {
		return LOG;
	}

	@Override
	protected List<DataParcelManifest> specifySubscriptionTopics() {
		DataParcelTypeDescriptor descriptor = new DataParcelTypeDescriptor();
		descriptor.setDataParcelDiscriminatorType("ldap-entry");
		
		DataParcelManifest manifest = new DataParcelManifest();
		manifest.setContentDescriptor(descriptor);
		manifest.setDataParcelFlowDirection(DataParcelDirectionEnum.INBOUND_DATA_PARCEL);
		manifest.setDataParcelType(DataParcelTypeEnum.GENERAL_DATA_PARCEL_TYPE);
		manifest.setEnforcementPointApprovalStatus(PolicyEnforcementPointApprovalStatusEnum.POLICY_ENFORCEMENT_POINT_APPROVAL_ANY);
		manifest.setNormalisationStatus(DataParcelNormalisationStatusEnum.DATA_PARCEL_CONTENT_NORMALISATION_FALSE);
		manifest.setValidationStatus(DataParcelValidationStatusEnum.DATA_PARCEL_CONTENT_VALIDATED_FALSE);
		manifest.setSourceSystem(DataParcelManifest.WILDCARD_CHARACTER);
		manifest.setIntendedTargetSystem(DataParcelManifest.WILDCARD_CHARACTER);
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

	}
}
