package net.fhirfactory.pegacorn.fhirbreak.corpservices.smsgateway.common;

import javax.enterprise.context.ApplicationScoped;

import net.fhirfactory.pegacorn.components.dataparcel.DataParcelManifest;
import net.fhirfactory.pegacorn.components.dataparcel.DataParcelTypeDescriptor;
import net.fhirfactory.pegacorn.components.dataparcel.valuesets.DataParcelDirectionEnum;
import net.fhirfactory.pegacorn.components.dataparcel.valuesets.DataParcelNormalisationStatusEnum;
import net.fhirfactory.pegacorn.components.dataparcel.valuesets.DataParcelTypeEnum;
import net.fhirfactory.pegacorn.components.dataparcel.valuesets.DataParcelValidationStatusEnum;
import net.fhirfactory.pegacorn.components.dataparcel.valuesets.PolicyEnforcementPointApprovalStatusEnum;

@ApplicationScoped
public class SMSDataParcelManifestBuilder {

    protected DataParcelTypeDescriptor createTypeDescriptor(String recordType, String version) {
        DataParcelTypeDescriptor typeDescriptor = new DataParcelTypeDescriptor();
        typeDescriptor.setDataParcelDefiner("FHIRFactory");
        typeDescriptor.setDataParcelCategory("Collaboration");
        typeDescriptor.setDataParcelSubCategory("SMS");
        typeDescriptor.setDataParcelResource(recordType);
        typeDescriptor.setVersion(version);
        
        return (typeDescriptor);
    }

    public DataParcelManifest createManifest(Class recordType, String version) {
        return createManifest(recordType.getSimpleName(), version);
    }
    
    public DataParcelManifest createManifest(String recordType, String version) {
        DataParcelManifest manifest = new DataParcelManifest();
        manifest.setContentDescriptor(createTypeDescriptor(recordType, version));
        manifest.setDataParcelFlowDirection(DataParcelDirectionEnum.OUTBOUND_DATA_PARCEL);
        manifest.setSourceSystem(DataParcelManifest.WILDCARD_CHARACTER);
        manifest.setIntendedTargetSystem(DataParcelManifest.WILDCARD_CHARACTER);
        manifest.setDataParcelType(DataParcelTypeEnum.GENERAL_DATA_PARCEL_TYPE);
        manifest.setEnforcementPointApprovalStatus(PolicyEnforcementPointApprovalStatusEnum.POLICY_ENFORCEMENT_POINT_APPROVAL_POSITIVE);
        manifest.setNormalisationStatus(DataParcelNormalisationStatusEnum.DATA_PARCEL_CONTENT_NORMALISATION_TRUE);
        manifest.setValidationStatus(DataParcelValidationStatusEnum.DATA_PARCEL_CONTENT_VALIDATED_TRUE);
        manifest.setInterSubsystemDistributable(true);
        
        return (manifest);
    }
}
