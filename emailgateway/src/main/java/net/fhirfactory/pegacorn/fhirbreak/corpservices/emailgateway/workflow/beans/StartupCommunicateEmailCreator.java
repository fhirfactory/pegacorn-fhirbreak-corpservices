package net.fhirfactory.pegacorn.fhirbreak.corpservices.emailgateway.workflow.beans;

import java.util.ArrayList;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import net.fhirfactory.pegacorn.core.model.dataparcel.DataParcelManifest;
import net.fhirfactory.pegacorn.core.model.dataparcel.DataParcelTypeDescriptor;
import net.fhirfactory.pegacorn.core.model.dataparcel.valuesets.DataParcelDirectionEnum;
import net.fhirfactory.pegacorn.core.model.dataparcel.valuesets.DataParcelNormalisationStatusEnum;
import net.fhirfactory.pegacorn.core.model.dataparcel.valuesets.DataParcelValidationStatusEnum;
import net.fhirfactory.pegacorn.core.model.dataparcel.valuesets.PolicyEnforcementPointApprovalStatusEnum;
import net.fhirfactory.pegacorn.core.model.petasos.uow.UoW;
import net.fhirfactory.pegacorn.core.model.petasos.uow.UoWPayload;
import net.fhirfactory.pegacorn.core.model.petasos.uow.UoWProcessingOutcomeEnum;
import net.fhirfactory.pegacorn.internals.communicate.entities.message.CommunicateEmailMessage;
import net.fhirfactory.pegacorn.internals.communicate.entities.message.factories.CommunicateMessageTopicFactory;
import org.apache.camel.Exchange;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fhirfactory.pegacorn.fhirbreak.corpservices.emailgateway.common.EmailDataParcelManifestBuilder;
import net.fhirfactory.pegacorn.util.FHIRContextUtility;


// Creates a unit of work with a JSON encoding of a communication resource
// suitable for resulting in basic startup emails.
// Note that this actually created a communication resource and then encodes it.
// this could be altered to directly create the JSON string if this proves
// too resource intensive.  It could also be converted to create a PegacornEmail
// instead of a Communication resource.  A Communication resource is used at the
// moment as this confirms the entire flow through this module.
@ApplicationScoped
public class StartupCommunicateEmailCreator {
    private static final Logger LOG = LoggerFactory.getLogger(StartupCommunicateEmailCreator.class);

    private ObjectMapper jsonMapper;
    
    public static final String ENV_STARTUP_FROM_EMAIL = "MAIL_STARTUP_FROM";
    public static final String ENV_STARTUP_TO_EMAIL = "MAIL_STARTUP_TO";
    public static final String STARTUP_FROM_EMAIL_DEFAULT = "noreply@pegacorn";
    
    protected static final String FAILURE_CONVERT_TO_JSON = "Could not convert created started email Communication resource to JSON";

    @Inject
    private EmailDataParcelManifestBuilder emailManifestBuilder;

    @Inject
    private CommunicateMessageTopicFactory communicateMessageTopicFactory;

    public StartupCommunicateEmailCreator(){
        this.jsonMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
        JavaTimeModule module = new JavaTimeModule();
        this.jsonMapper.registerModule(module);
        this.jsonMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    }
    
    
    public UoW createStartupEmailCommunication(String input, Exchange camelExchange) {
        LOG.debug(".createStartupEmailCommunication(): Entry");
        
        // email addresses to send to
        List<String> emails = new ArrayList<>();
        String envTos = System.getenv(ENV_STARTUP_TO_EMAIL);
        if (!StringUtils.isEmpty(envTos)) {
            for (String to : envTos.split(";")) {
                emails.add(to.trim());
            }
        }
        
        // email address to list from
        String fromEmail = System.getenv(ENV_STARTUP_FROM_EMAIL);
        if (fromEmail == null) {
            fromEmail = STARTUP_FROM_EMAIL_DEFAULT;
        }

        //
        // Create the CommunicateEmailMessage

        CommunicateEmailMessage emailMessage = new CommunicateEmailMessage();

        emailMessage.setFrom(fromEmail);
        emailMessage.getTo().addAll(emails);
        emailMessage.setContent("This email shows that the pegacorn email subsystem is working and able to send email");

        //
        // Create the UoW
        //

        UoW uow = new UoW();
        UoWPayload payload = new UoWPayload();
        String payloadString = null;
        try{
            payloadString = getJSONMapper().writeValueAsString(emailMessage);
        } catch(Exception ex){
            String errorMessage = ExceptionUtils.getMessage(ex);
            uow.setProcessingOutcome(UoWProcessingOutcomeEnum.UOW_OUTCOME_FAILED);
            uow.setFailureDescription(errorMessage);
            getLogger().warn(".createStartupEmailCommunication(): Could not convert message to JSON, errorMessage->{}", errorMessage);
        }

        if(StringUtils.isNotEmpty(payloadString)){
            DataParcelManifest emailManifest = new DataParcelManifest();
            DataParcelTypeDescriptor emailTypeDescriptor = communicateMessageTopicFactory.createEmailTypeDescriptor();
            emailManifest.setContentDescriptor(emailTypeDescriptor);
            emailManifest.setSourceProcessingPlantParticipantName(DataParcelManifest.WILDCARD_CHARACTER);
            emailManifest.setNormalisationStatus(DataParcelNormalisationStatusEnum.DATA_PARCEL_CONTENT_NORMALISATION_TRUE);
            emailManifest.setValidationStatus(DataParcelValidationStatusEnum.DATA_PARCEL_CONTENT_VALIDATED_TRUE);
            emailManifest.setSourceSystem(DataParcelManifest.WILDCARD_CHARACTER);
            emailManifest.setEnforcementPointApprovalStatus(PolicyEnforcementPointApprovalStatusEnum.POLICY_ENFORCEMENT_POINT_APPROVAL_POSITIVE);
            emailManifest.setDataParcelFlowDirection(DataParcelDirectionEnum.INFORMATION_FLOW_OUTBOUND_DATA_PARCEL);
            emailManifest.setInterSubsystemDistributable(true);
            payload.setPayload(payloadString);
            payload.setPayloadManifest(emailManifest);
            uow.setIngresContent(payload);
            uow.getEgressContent().addPayloadElement(payload);
        }
        
        LOG.debug(".createStartupEmailCommunication(): Exit, uow->{}", uow);
        return (uow);
    }

    //
    // Getters (and Setters)
    //

    protected Logger getLogger(){
        return(LOG);
    }

    protected ObjectMapper getJSONMapper(){
        return(jsonMapper);
    }
}
