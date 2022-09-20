package net.fhirfactory.pegacorn.fhirbreak.corpservices.ldapsync.workshops.interact;

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
import net.fhirfactory.pegacorn.fhirbreak.corpservices.ldapsync.workshops.interact.beans.LdapEntryWriter;
import net.fhirfactory.pegacorn.petasos.tasking.moa.wup.MessageBasedWUPEndpoint;
import net.fhirfactory.pegacorn.petasos.wup.helper.EgressActivityFinalisationRegistration;
import net.fhirfactory.pegacorn.workshops.InteractWorkshop;
import net.fhirfactory.pegacorn.wups.archetypes.petasosenabled.messageprocessingbased.InteractEgressAPIClientGatewayWUP;

/**
 * Writes an entry to LDAP.
 * 
 * @author Brendan Douglas
 *
 */
public abstract class LdapEntryWriterWUP extends InteractEgressAPIClientGatewayWUP  {
    
    @Inject
    private InteractWorkshop workshop;
		
	protected abstract String getSourceSystem();

	@Override
	protected List<DataParcelManifest> specifySubscriptionTopics() {
		List<DataParcelManifest>dataParcelManifests = new ArrayList<>();
		
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
		manifest.setSourceSystem(getSourceSystem());
		manifest.setInterSubsystemDistributable(false); 
		
		dataParcelManifests.add(manifest);
		
		return dataParcelManifests;
	}

	@Override
	protected String specifyWUPInstanceName() {
		return "ldapEntryWriterWUP";
	}

	@Override
	protected String specifyWUPInstanceVersion() {
		return "1.0.0";
	}

	@Override
	protected WorkshopInterface specifyWorkshop() {
		return workshop;
	}

	@Override
	protected MessageBasedWUPEndpoint specifyEgressEndpoint() {
		return null;
	}

	@Override
	public void configure() throws Exception {
		 fromIncludingPetasosServices(this.ingresFeed())
		 	.bean(LdapEntryWriter.class, "writerLdapEntry")
		 	.bean(EgressActivityFinalisationRegistration.class,"registerActivityFinishAndFinalisation(*,  Exchange)");
	}
}