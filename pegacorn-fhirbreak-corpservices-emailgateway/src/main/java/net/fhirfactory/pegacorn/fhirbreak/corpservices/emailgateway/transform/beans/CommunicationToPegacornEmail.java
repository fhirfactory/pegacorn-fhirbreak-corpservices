/*
 * Copyright (c) 2021 ACT Health
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package net.fhirfactory.pegacorn.fhirbreak.corpservices.emailgateway.transform.beans;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.Attachment;
import org.hl7.fhir.r4.model.Communication;
import org.hl7.fhir.r4.model.Communication.CommunicationPayloadComponent;
import org.hl7.fhir.r4.model.ContactPoint;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import ca.uhn.fhir.parser.IParser;
import net.fhirfactory.pegacorn.components.dataparcel.DataParcelManifest;
import net.fhirfactory.pegacorn.fhirbreak.corpservices.emailgateway.common.EmailDataParcelManifestBuilder;
import net.fhirfactory.pegacorn.fhirbreak.corpservices.emailgateway.common.PegacornEmail;
import net.fhirfactory.pegacorn.fhirbreak.corpservices.emailgateway.common.PegacornEmailAttachment;
import net.fhirfactory.pegacorn.fhirbreak.corpservices.emailgateway.fhir.ContactPointHelper;
import net.fhirfactory.pegacorn.fhirbreak.corpservices.emailgateway.fhir.ContactPointRetrieveException;
import net.fhirfactory.pegacorn.petasos.model.uow.UoW;
import net.fhirfactory.pegacorn.petasos.model.uow.UoWPayload;
import net.fhirfactory.pegacorn.petasos.model.uow.UoWProcessingOutcomeEnum;
import net.fhirfactory.pegacorn.util.FHIRContextUtility;

@ApplicationScoped
public class CommunicationToPegacornEmail {
    
    public static final String EMAIL_SUBJECT_EXTENSION_URL = "identifier://net.fhirfactory.pegacorn.fhirbreak.corpservices.emailgateway/subject";  // will be moved to a more common petasos class

    private static final Logger LOG = LoggerFactory.getLogger(CommunicationToPegacornEmail.class);
    
    protected static final String FAILURE_MULTIPLE_CONTENT = "Found multiple contentString payload elements";
    protected static final String FAILURE_INVALID_SENDER_REFERENCE = "Could not get resource for sender";
    protected static final String FAILURE_INVALID_RECIPIENT_REFERENCE = "Could not get resource for recipient";
    protected static final String FAILURE_NO_EMAIL_FOR_SENDER = "Could not get email for sender";
    protected static final String FAILURE_NO_EMAIL_FOR_RECIPIENT = "Could not get email for recipient";
    protected static final String FAILURE_CONVERT_EMAIL_TO_JSON = "Could not convert email to JSON";
    
    private ObjectMapper jsonMapper; //TODO make common
    private IParser fhirParser;
    private boolean initialised;
    @Inject
    private FHIRContextUtility fhirContextUtility;
    @Inject
    private EmailDataParcelManifestBuilder emailManifestBuilder;
    
    
    
    public CommunicationToPegacornEmail() {
    }
    
    // just for ease of testing for now
    protected CommunicationToPegacornEmail(FHIRContextUtility fhirContextUtility, EmailDataParcelManifestBuilder emailManifestBuilder) {
        this.fhirContextUtility = fhirContextUtility;
        this.emailManifestBuilder = emailManifestBuilder;
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
    

    public UoW transformCommunicationToEmail(UoW incomingUoW) {
        LOG.debug(".transformCommunicationToEmail(): Entry");
        
        // defensive programming
        if(incomingUoW == null){
            LOG.error(".transformCommunicationToEmail(): Exit, incomingUoW is null!");
            return(null);
        }
        if(incomingUoW.getProcessingOutcome().equals(UoWProcessingOutcomeEnum.UOW_OUTCOME_FAILED)){
            LOG.warn(".transformCommunicationToEmail(): Exit, incomingUoW has already failed!");
            return(incomingUoW);
        }
        if (incomingUoW.getIngresContent() == null){
            LOG.error(".transformCommunicationToEmail(): Exit, no ingress content");
            incomingUoW.setProcessingOutcome(UoWProcessingOutcomeEnum.UOW_OUTCOME_FAILED);
            incomingUoW.setFailureDescription("UoW has no ingres content!");
            return(incomingUoW);
        }
        String incomingPayload = incomingUoW.getIngresContent().getPayload();
        if (StringUtils.isEmpty(incomingPayload)){
            LOG.warn(".transformCommunicationToEmail(): Exit, PegacornSMSMessage payload is empty!");
            incomingUoW.setProcessingOutcome(UoWProcessingOutcomeEnum.UOW_OUTCOME_FAILED);
            incomingUoW.setFailureDescription("UoW ingres payload content is empty!");
            return(incomingUoW);
        }
        
        // get the payload into a FHIR::Communication resource
        LOG.trace(".transformCommunicationToEmail(): Mapping input payload into FHIR Communication");
        Communication emailCommunication = fhirParser.parseResource(Communication.class, incomingPayload);
        
        PegacornEmail email = new PegacornEmail();
        
        // get the sender
        if (emailCommunication.hasSender()) {
            Resource sender = emailCommunication.getSenderTarget();
            if (sender == null && emailCommunication.getSender() != null) {
                Reference senderRef = emailCommunication.getSender();
                sender = (Resource) senderRef.getResource();
            }
            if (sender == null) {
                incomingUoW.setProcessingOutcome(UoWProcessingOutcomeEnum.UOW_OUTCOME_FAILED);
                incomingUoW.setFailureDescription(FAILURE_INVALID_SENDER_REFERENCE);
                LOG.warn(".transformCommunicationToEmail(): Exit, {}", FAILURE_INVALID_SENDER_REFERENCE); //TODO add reference
                return incomingUoW;
            }
            try {
                email.setFrom(ContactPointHelper.getTopRankContact(sender, ContactPoint.ContactPointSystem.EMAIL).getValue());
            } catch (ContactPointRetrieveException e) {
                incomingUoW.setProcessingOutcome(UoWProcessingOutcomeEnum.UOW_OUTCOME_FAILED);
                incomingUoW.setFailureDescription(FAILURE_NO_EMAIL_FOR_SENDER + ": " + e.getMessage());
                LOG.warn(".transformCommunicationToEmail(): Exit, {}->{}", FAILURE_NO_EMAIL_FOR_SENDER, sender, e);
                return incomingUoW;
            }
        }
        
        // get the recipients
        List<String> toEmails = new ArrayList<>();
        if (emailCommunication.hasRecipient()) {
            List<Reference> recipients = emailCommunication.getRecipient();
            for (Reference recipientRef: recipients) {
                Resource recipient = (Resource) recipientRef.getResource();
                if (recipient == null) {
                    incomingUoW.setProcessingOutcome(UoWProcessingOutcomeEnum.UOW_OUTCOME_FAILED);
                    incomingUoW.setFailureDescription(FAILURE_INVALID_RECIPIENT_REFERENCE);
                    LOG.warn(".transformCommunicationToEmail(): Exit, {}", FAILURE_INVALID_RECIPIENT_REFERENCE); //TODO add reference
                    return incomingUoW;
                }
                try {
                    toEmails.add(ContactPointHelper.getTopRankContact(recipient, ContactPoint.ContactPointSystem.EMAIL).getValue());
                } catch (ContactPointRetrieveException e) {
                    incomingUoW.setProcessingOutcome(UoWProcessingOutcomeEnum.UOW_OUTCOME_FAILED);
                    incomingUoW.setFailureDescription(FAILURE_NO_EMAIL_FOR_RECIPIENT + ": " + e.getMessage());
                    LOG.warn(".transformCommunicationToEmail(): Exit, {}->{}", FAILURE_NO_EMAIL_FOR_SENDER, recipient, e);
                    return incomingUoW;
                }
            }
        }
        email.setTo(toEmails);
        
        // get the content, subject and attachments
        if (emailCommunication.hasPayload()) {
            List<CommunicationPayloadComponent> payload = emailCommunication.getPayload();       
            boolean hasContent = false;
            int numAttachments = 0;
            
            for (CommunicationPayloadComponent payloadComponent : payload) {
                if (payloadComponent.hasContentAttachment()) {
                    numAttachments++;
                    //TODO do we have any use for language?
                    LOG.debug(".transformCommunicationToEmail(): Processing attachment {}", numAttachments);
                    Attachment communicationAttachment = payloadComponent.getContentAttachment();
                    
                    PegacornEmailAttachment emailAttachment = new PegacornEmailAttachment();
                    emailAttachment.setContentType(communicationAttachment.getContentType());
                    emailAttachment.setName(communicationAttachment.getTitle());
                    if (communicationAttachment.hasSize()) {
                        emailAttachment.setSize(Long.valueOf(communicationAttachment.getSize())); // note that the FHIR attachment returns size as an int so not sure what it does if size is larger than max in size (~2MB)
                    }
                    if (communicationAttachment.hasCreation()) {
                        //TODO check this (as not sure if time is local or GMT and what is wanted for end email)
                        emailAttachment.setCreationTime(communicationAttachment.getCreation().toString());
                    }
                    if (communicationAttachment.hasHash()) {
                        emailAttachment.setHash(communicationAttachment.getHashElement().getValueAsString());
                    }
                    if (communicationAttachment.hasData()) {
                        emailAttachment.setData(communicationAttachment.getDataElement().getValueAsString());
                    }
                    if (communicationAttachment.hasUrl()) {
                        emailAttachment.setUrl(communicationAttachment.getUrl());
                    }
                    email.getAttachments().add(emailAttachment);
                    
                } else if (payloadComponent.hasContentReference()) {
                    //TODO support this
                    // Just log a warning and ignore this
                    String referenceDisplay = payloadComponent.getContentReference().getDisplay();
                    if (referenceDisplay == null) {
                        referenceDisplay = "";
                    } else {
                        referenceDisplay = ": display->" + referenceDisplay;
                    }
                    LOG.warn(".transformCommunicationToEmail(): Ignored unsupported reference payload type{}", referenceDisplay);
                    
                } else if (payloadComponent.hasContentStringType()) {
                    if (hasContent) {
                        // multiple content - not allowed as not sure how this should be processed
                        //TODO check this.  This could make sense in some scenarios, particularly for multipart/alternative however
                        //     would need an extension element to flag this
                        incomingUoW.setProcessingOutcome(UoWProcessingOutcomeEnum.UOW_OUTCOME_FAILED);
                        incomingUoW.setFailureDescription(FAILURE_MULTIPLE_CONTENT);
                        LOG.warn(".transformCommunicationToEmail(): Exit, {} for email->{}", FAILURE_MULTIPLE_CONTENT, email);
                        return incomingUoW;
                    }
                    hasContent = true;
                    
                    // set content
                    String emailContent = payloadComponent.getContentStringType().primitiveValue();
                    email.setContent(emailContent);
                    
                    // get the subject from the extension
                    LOG.debug(".transformCommunicationToEmail(): Getting email subject from payload extension");
                    Extension subjectExtension = payloadComponent.getExtensionByUrl(EMAIL_SUBJECT_EXTENSION_URL);
                    if (subjectExtension != null) {
                        email.setSubject(subjectExtension.getValue().primitiveValue());
                    }
                }
            }
        }
        
        // convert pegacorn email to JSON string
        String egressPayloadString = null;
        try {
            egressPayloadString = jsonMapper.writeValueAsString(email);
        } catch (JsonProcessingException e) {
            LOG.error(".transformCommunicationToEmail(): Exit, Could not convert message to JSON! email->{}", email, e);
            incomingUoW.setProcessingOutcome(UoWProcessingOutcomeEnum.UOW_OUTCOME_FAILED);
            incomingUoW.setFailureDescription(FAILURE_CONVERT_EMAIL_TO_JSON + ": " + e.getMessage());
            return(incomingUoW);
        }
        
        // add the pegacorn email to the output UoW
        UoWPayload egressUoWPayload = new UoWPayload();
        egressUoWPayload.setPayload(egressPayloadString);
        
        DataParcelManifest manifest = emailManifestBuilder.createManifest(PegacornEmail.class, "1.0.0");
        List<DataParcelManifest> manifestList = new ArrayList<>();
        manifestList.add(manifest);
        
        egressUoWPayload.setPayloadManifest(manifest);
        
        incomingUoW.getEgressContent().addPayloadElement(egressUoWPayload);
        incomingUoW.setProcessingOutcome(UoWProcessingOutcomeEnum.UOW_OUTCOME_SUCCESS);
        
        LOG.debug(".transformCommunicationToEmail(): Exit");
        return(incomingUoW);
    }
}
