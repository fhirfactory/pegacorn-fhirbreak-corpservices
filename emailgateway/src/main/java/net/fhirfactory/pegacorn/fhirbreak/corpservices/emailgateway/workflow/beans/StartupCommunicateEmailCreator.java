package net.fhirfactory.pegacorn.fhirbreak.corpservices.emailgateway.workflow.beans;

import java.util.ArrayList;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.camel.Exchange;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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


// Creates a Unit of Work with a JSON encoding of a CommunicateEmailMessage
// to indicate startup based off environment variables
@ApplicationScoped
public class StartupCommunicateEmailCreator {
    private static final Logger LOG = LoggerFactory.getLogger(StartupCommunicateEmailCreator.class);

    private ObjectMapper jsonMapper;
    
    public static final String ENV_STARTUP_FROM_EMAIL = "MAIL_STARTUP_FROM";
    public static final String ENV_STARTUP_TO_EMAIL = "MAIL_STARTUP_TO";
    public static final String STARTUP_FROM_EMAIL_DEFAULT = "noreply@pegacorn";
    public static final String ENV_STARTUP_SUBJECT_EMAIL = "MAIL_STARTUP_SUBJECT";
    public static final String ENV_SITENAME = "MY_POD_NAMESPACE";
    
    protected static final String FAILURE_CONVERT_TO_JSON = "Could not convert created started email Communication resource to JSON";


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
        emailMessage.setSubject(getSubject());
        emailMessage.setContent("This email shows that the pegacorn email subsystem is working and able to send email");

        //
        // Create the UoW
        //

        UoW uow = null;
        UoWPayload payload = new UoWPayload();
        String payloadString = null;
        try{
            payloadString = getJSONMapper().writeValueAsString(emailMessage);
        } catch(Exception ex){
            String errorMessage = ExceptionUtils.getMessage(ex);
            uow = new UoW();
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
            emailManifest.setValidationStatus(DataParcelValidationStatusEnum.DATA_PARCEL_CONTENT_VALIDATED_FALSE);
            emailManifest.setSourceSystem(DataParcelManifest.WILDCARD_CHARACTER);
            emailManifest.setEnforcementPointApprovalStatus(PolicyEnforcementPointApprovalStatusEnum.POLICY_ENFORCEMENT_POINT_APPROVAL_POSITIVE);
            emailManifest.setDataParcelFlowDirection(DataParcelDirectionEnum.INFORMATION_FLOW_OUTBOUND_DATA_PARCEL);
            emailManifest.setInterSubsystemDistributable(false);
            payload.setPayloadManifest(emailManifest);
            payload.setPayload(payloadString);
            uow = new UoW(payload);
            uow.getEgressContent().addPayloadElement(payload);
            uow.setProcessingOutcome(UoWProcessingOutcomeEnum.UOW_OUTCOME_SUCCESS);
        }
        
        LOG.debug(".createStartupEmailCommunication(): Exit, uow->{}", uow);
        return (uow);
    }
    
    private String getSubject() {
        String subject = "Pegacorn Email Gateway Startup";
        if (!StringUtils.isEmpty(System.getenv(ENV_STARTUP_SUBJECT_EMAIL))) {
            subject = System.getenv(ENV_STARTUP_SUBJECT_EMAIL);
        } else if (!StringUtils.isEmpty(System.getenv(ENV_SITENAME))) {
            subject = "(" + System.getenv(ENV_SITENAME) + ") " + subject;
        }
        return subject;
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
