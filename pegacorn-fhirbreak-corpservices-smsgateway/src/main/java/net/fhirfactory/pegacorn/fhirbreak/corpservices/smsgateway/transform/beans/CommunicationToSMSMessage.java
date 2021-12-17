package net.fhirfactory.pegacorn.fhirbreak.corpservices.smsgateway.transform.beans;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.Communication;
import org.hl7.fhir.r4.model.Communication.CommunicationPayloadComponent;
import org.hl7.fhir.r4.model.ContactPoint;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import ca.uhn.fhir.parser.IParser;
import net.fhirfactory.pegacorn.components.dataparcel.DataParcelManifest;
import net.fhirfactory.pegacorn.fhir.helpers.ContactPointHelper;
import net.fhirfactory.pegacorn.fhir.helpers.exception.ContactPointRetrieveException;
import net.fhirfactory.pegacorn.fhirbreak.corpservices.smsgateway.common.SMSDataParcelManifestBuilder;
import net.fhirfactory.pegacorn.fhirbreak.corpservices.smsgateway.common.SMSMessageBase;
import net.fhirfactory.pegacorn.petasos.model.uow.UoW;
import net.fhirfactory.pegacorn.petasos.model.uow.UoWPayload;
import net.fhirfactory.pegacorn.petasos.model.uow.UoWProcessingOutcomeEnum;
import net.fhirfactory.pegacorn.util.FHIRContextUtility;

public class CommunicationToSMSMessage {

    private static final Logger LOG = LoggerFactory.getLogger(CommunicationToSMSMessage.class);
    
    protected static final String FAILURE_INVALID_RECIPIENT_REFERENCE = "Could not get resource for recipient";
    protected static final String FAILURE_NO_PHONE_NUMBER_FOR_RECIPIENT = "Could not get phone number for recipient";
    protected static final String FAILURE_CONVERT_SMS_TO_JSON = "Could not convert SMSMessageBase to JSON";
    protected static final String FAILURE_MULTIPLE_SMS_CONTENT = "Multiple payload.contentString values found";
    
    private ObjectMapper jsonMapper; //TODO make common
    private IParser fhirParser;
    private boolean initialised;
    @Inject
    private FHIRContextUtility fhirContextUtility;
    @Inject
    private SMSDataParcelManifestBuilder smsManifestBuilder;
    
    
    public CommunicationToSMSMessage() {
    }
    
    // just for ease of testing for now
    protected CommunicationToSMSMessage(FHIRContextUtility fhirContextUtility, SMSDataParcelManifestBuilder smsManifestBuilder) {
        this.fhirContextUtility = fhirContextUtility;
        this.smsManifestBuilder = smsManifestBuilder;
    }
    
    @PostConstruct
    public void initialise() {
        LOG.debug(".initialise(): Entry");
        if(!initialised) {
            LOG.info(".initialise(): initialising....");
            fhirParser = fhirContextUtility.getJsonParser();
            jsonMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT); // sets pretty printing
            this.initialised = true;
            LOG.info(".initialise(): Done.");
        }
        else {
            LOG.debug(".initialise(): Already initialised, nothing to do...");
        }
        LOG.debug(".initialise(): Exit");
    }

    
    public UoW transformCommunicationToSMS(UoW incomingUoW) {
        LOG.debug(".transformCommunicationToSMS(): Entry");
        
        // defensive programming
        if(incomingUoW == null){
            LOG.error(".transformCommunicationToSMS(): Exit, incomingUoW is null!");
            return(null);
        }
        if(incomingUoW.getProcessingOutcome().equals(UoWProcessingOutcomeEnum.UOW_OUTCOME_FAILED)){
            LOG.warn(".transformCommunicationToSMS(): Exit, incomingUoW has already failed!");
            return(incomingUoW);
        }
        if (incomingUoW.getIngresContent() == null){
            LOG.error(".transformCommunicationToSMS(): Exit, no ingress content");
            incomingUoW.setProcessingOutcome(UoWProcessingOutcomeEnum.UOW_OUTCOME_FAILED);
            incomingUoW.setFailureDescription("UoW has no ingres content!");
            return(incomingUoW);
        }
        String incomingPayload = incomingUoW.getIngresContent().getPayload();
        if (StringUtils.isEmpty(incomingPayload)){
            LOG.warn(".transformCommunicationToSMS(): Exit, Communication payload is empty!");
            incomingUoW.setProcessingOutcome(UoWProcessingOutcomeEnum.UOW_OUTCOME_FAILED);
            incomingUoW.setFailureDescription("UoW ingres payload content is empty!");
            return(incomingUoW);
        }
        
        // get the payload into a FHIR::Communication resource
        LOG.trace(".transformCommunicationToSMS(): Mapping input payload into FHIR Communication");
        Communication smsCommunication = fhirParser.parseResource(Communication.class, incomingPayload);
        
        List<SMSMessageBase> smsMessages = new ArrayList<>();
        
        // get the phone recipients
        if (smsCommunication.hasRecipient()) {
            // get the phone numbers from the recipients
            SMSMessageBase smsMessage;
            List<Reference> recipients = smsCommunication.getRecipient();
            for (Reference recipientRef: recipients) {
                Resource recipient = (Resource) recipientRef.getResource();
                if (recipient == null) {
                    incomingUoW.setProcessingOutcome(UoWProcessingOutcomeEnum.UOW_OUTCOME_FAILED);
                    incomingUoW.setFailureDescription(FAILURE_INVALID_RECIPIENT_REFERENCE);
                    LOG.warn(".transformCommunicationToSMS(): Exit, {}", FAILURE_INVALID_RECIPIENT_REFERENCE); //TODO add reference
                    return incomingUoW;
                }
                try {
                    smsMessage = new SMSMessageBase();
                    smsMessage.setPhoneNumber(ContactPointHelper.getTopRankContact(recipient, ContactPoint.ContactPointSystem.PHONE).getValue());
                    smsMessages.add(smsMessage);
                } catch (ContactPointRetrieveException e) {
                    incomingUoW.setProcessingOutcome(UoWProcessingOutcomeEnum.UOW_OUTCOME_FAILED);
                    incomingUoW.setFailureDescription(FAILURE_NO_PHONE_NUMBER_FOR_RECIPIENT + ": " + e.getMessage());
                    LOG.warn(".transformCommunicationToSMS(): Exit, {}->{}", FAILURE_NO_PHONE_NUMBER_FOR_RECIPIENT, recipient, e);
                    return incomingUoW;
                }
            }
        } else {
            // allow a single SMS Message without a recipient at this point but log a warning
            // note that default functionality will cause this to fail in the Interact workshop
            LOG.warn("transformCommunicationToSMS(): No Communication recipient to provided for SMS recipient phone number");
            smsMessages.add(new SMSMessageBase());
        }
        
        // get the message from the payload
        String message = null;
        if (smsCommunication.hasPayload()) {
            List<CommunicationPayloadComponent> payload = smsCommunication.getPayload();       
            
            for (CommunicationPayloadComponent payloadComponent : payload) {
                if (payloadComponent.hasContentStringType()) {
                    if (message != null) {
                        // cannot have more than one payload.contentString
                        LOG.error(".transformCommunicationToSMS(): Exit, {}: First message->{}", FAILURE_MULTIPLE_SMS_CONTENT, message);
                        incomingUoW.setProcessingOutcome(UoWProcessingOutcomeEnum.UOW_OUTCOME_FAILED);
                        incomingUoW.setFailureDescription(FAILURE_MULTIPLE_SMS_CONTENT);
                        return(incomingUoW);
                    }
                    message = payloadComponent.getContentStringType().getValue();
                    for (SMSMessageBase smsMessage : smsMessages) {
                        smsMessage.setMessage(message);
                    }
                } else if (LOG.isDebugEnabled()) {
                    LOG.debug("transformCommunicationToSMS(): skipped {} content payload: id->{}", payloadComponent.getContent().fhirType(), payloadComponent.getId());
                }
            }
        }
        if (message == null) {
            // allow SMS Messages without actual message content at this point in the process but log a warning.
            // note that default functionality will cause this to fail in the Interact workshop
            LOG.warn("transformCommunicationToSMS(): No Communication payload to set as SMS content");
        }
        
        // convert SMS Message to JSON string
        for (SMSMessageBase smsMessage : smsMessages) {
            String egressPayloadString = null;
            try {
                egressPayloadString = jsonMapper.writeValueAsString(smsMessage);
            } catch (JsonProcessingException e) {
                LOG.error(".transformCommunicationToSMS(): Exit, Could not convert message to JSON! smsMessage->{}", smsMessage, e);
                incomingUoW.setProcessingOutcome(UoWProcessingOutcomeEnum.UOW_OUTCOME_FAILED);
                incomingUoW.setFailureDescription(FAILURE_CONVERT_SMS_TO_JSON + ": " + e.getMessage());
                return(incomingUoW);
            }
            
            // add the SMS message to the output UoW
            UoWPayload egressUoWPayload = new UoWPayload();
            egressUoWPayload.setPayload(egressPayloadString);
            
            DataParcelManifest manifest = smsManifestBuilder.createManifest(SMSMessageBase.class, "1.0.0");
            List<DataParcelManifest> manifestList = new ArrayList<>();
            manifestList.add(manifest);
            
            egressUoWPayload.setPayloadManifest(manifest);
        
            incomingUoW.getEgressContent().addPayloadElement(egressUoWPayload);
        }
        
        incomingUoW.setProcessingOutcome(UoWProcessingOutcomeEnum.UOW_OUTCOME_SUCCESS);
        
        LOG.debug(".transformCommunicationToSMS(): Exit");
        return(incomingUoW);
    }
}
