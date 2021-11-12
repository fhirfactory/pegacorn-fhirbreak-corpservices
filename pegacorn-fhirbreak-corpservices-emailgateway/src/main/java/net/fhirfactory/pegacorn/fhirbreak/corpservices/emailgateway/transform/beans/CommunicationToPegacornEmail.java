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

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.hl7.fhir.r4.model.Communication;
import org.hl7.fhir.r4.model.ContactPoint;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import net.fhirfactory.pegacorn.components.dataparcel.DataParcelManifest;
import net.fhirfactory.pegacorn.fhirbreak.corpservices.emailgateway.common.EmailDataParcelManifestBuilder;
import net.fhirfactory.pegacorn.fhirbreak.corpservices.emailgateway.common.PegacornEmail;
import net.fhirfactory.pegacorn.fhirbreak.corpservices.emailgateway.fhir.ContactPointHelper;
import net.fhirfactory.pegacorn.fhirbreak.corpservices.emailgateway.fhir.ContactPointRetrieveException;
import net.fhirfactory.pegacorn.petasos.model.uow.UoW;
import net.fhirfactory.pegacorn.petasos.model.uow.UoWPayload;
import net.fhirfactory.pegacorn.petasos.model.uow.UoWProcessingOutcomeEnum;

public class CommunicationToPegacornEmail {

    private static final Logger LOG = LoggerFactory.getLogger(CommunicationToPegacornEmail.class);
    
    private ObjectMapper jsonMapper; //TODO make common
    
    @Inject
    private EmailDataParcelManifestBuilder emailManifestBuilder;
    
    public CommunicationToPegacornEmail() {
        jsonMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT); // sets pretty printing;
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
        Communication emailCommunication;
        try {
            emailCommunication = jsonMapper.readValue(incomingPayload, Communication.class);
        } catch (JsonProcessingException  e) {
            incomingUoW.setProcessingOutcome(UoWProcessingOutcomeEnum.UOW_OUTCOME_FAILED);
            incomingUoW.setFailureDescription(e.getMessage());
            LOG.warn(".transformCommunicationToEmail(): Exit, Error with Decoding JSON Payload", e);
            return incomingUoW;
        }
        
        PegacornEmail email = new PegacornEmail();
        
        // get the sender
        Resource sender = emailCommunication.getSenderTarget();
        if (sender == null) {
            incomingUoW.setProcessingOutcome(UoWProcessingOutcomeEnum.UOW_OUTCOME_FAILED);
            incomingUoW.setFailureDescription("No Sender in Communication");
            LOG.warn(".transformCommunicationToEmail(): Exit, No Sender in Communication->{}", emailCommunication);
            return incomingUoW;
        }
        try {
            email.setFrom(ContactPointHelper.getTopRankContact(sender, ContactPoint.ContactPointSystem.EMAIL));
        } catch (ContactPointRetrieveException e) {
            incomingUoW.setProcessingOutcome(UoWProcessingOutcomeEnum.UOW_OUTCOME_FAILED);
            incomingUoW.setFailureDescription(e.getMessage());
            LOG.warn(".transformCommunicationToEmail(): Exit, Error getting top ranked contact point for sender->{}", sender, e);
            return incomingUoW;
        }
        
        // get the recipients
        List<String> toEmails = new ArrayList<>();
        List<Reference> recipients = emailCommunication.getRecipient();
        if (recipients == null || recipients.isEmpty()) {
            incomingUoW.setProcessingOutcome(UoWProcessingOutcomeEnum.UOW_OUTCOME_FAILED);
            incomingUoW.setFailureDescription("No Recipients in Communication");
            LOG.warn(".transformCommunicationToEmail(): Exit, No Recipients in Communication->{}", emailCommunication);
            return incomingUoW;
        }
        for (Reference recipientRef: recipients) {
            Resource recipient = (Resource) recipientRef.getResource();
            //TODO handle cannot get from reference
            try {
                toEmails.add(ContactPointHelper.getTopRankContact(recipient, ContactPoint.ContactPointSystem.EMAIL));
            } catch (ContactPointRetrieveException e) {
                incomingUoW.setProcessingOutcome(UoWProcessingOutcomeEnum.UOW_OUTCOME_FAILED);
                incomingUoW.setFailureDescription(e.getMessage());
                LOG.warn(".transformCommunicationToEmail(): Exit, Error getting top ranked contact point for recipient->{}", recipient, e);
                return incomingUoW;
            }
        }
        
        // get the subject
        //TODO get from extension to payload
        email.setSubject("temporary subject");
        
        // get the content
        //TODO get from payload
        email.setContent("Temporary message content");
        
        // get the attachments
        //TODO get from payload
        
        // convert pegacorn email to JSON string
        String egressPayloadString = null;
        try {
            egressPayloadString = jsonMapper.writeValueAsString(email);
        } catch (JsonProcessingException e) {
            LOG.error(".transformCommunicationToEmail(): Exit, Could not convert message to JSON! email->{}", email, e);
            incomingUoW.setProcessingOutcome(UoWProcessingOutcomeEnum.UOW_OUTCOME_FAILED);
            incomingUoW.setFailureDescription(ExceptionUtils.getStackTrace(e));
            return(incomingUoW);
        }
        
        // add the pegacorn email to the output UoW
        UoWPayload egressUoWPayload = new UoWPayload();
        egressUoWPayload.setPayload(egressPayloadString);
        
        DataParcelManifest manifest = emailManifestBuilder.createManifest("PegacornEmail", "1.0.0"); //TODO fix up hardcoded values
        List<DataParcelManifest> manifestList = new ArrayList<>();
        manifestList.add(manifest);
        
        egressUoWPayload.setPayloadManifest(manifest);
        
        incomingUoW.getEgressContent().addPayloadElement(egressUoWPayload);
        
        LOG.debug(".transformCommunicationToEmail(): Exit");
        return(incomingUoW);
    }
}
