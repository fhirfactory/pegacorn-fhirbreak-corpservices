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
package net.fhirfactory.pegacorn.fhirbreak.corpservices.emailgateway.interact.beans;

import java.util.Base64;
import java.util.stream.Collectors;

import javax.activation.DataHandler;
import javax.mail.util.ByteArrayDataSource;

import org.apache.camel.Exchange;
import org.apache.camel.attachment.AttachmentMessage;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import net.fhirfactory.pegacorn.fhirbreak.corpservices.emailgateway.common.PegacornEmail;
import net.fhirfactory.pegacorn.fhirbreak.corpservices.emailgateway.common.PegacornEmailAttachment;
import net.fhirfactory.pegacorn.petasos.model.uow.UoW;
import net.fhirfactory.pegacorn.petasos.model.uow.UoWProcessingOutcomeEnum;

//TODO possibly change name and just allow to support IMAP and POP3 also (as they essentially use the same parameters in camel)
public class PegacornEmailToSMTP {
    
    private static final Logger LOG = LoggerFactory.getLogger(PegacornEmailToSMTP.class);

    private ObjectMapper jsonMapper;
    
    public PegacornEmailToSMTP() {
        jsonMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT); // sets pretty printing;
    }
    
    // take our email UoW and convert to camel output for SMTP route
    // this consists of straight email body content and headers for recipient,
    // subject, etc.
    //TODO check if should throw an exception or something else or record the UoW explicitly here)
    public String transformPegcornEmailIntoSMTP(UoW incomingUoW, Exchange exchange) {
        LOG.debug(".transformPegcornEmailIntoSMTP(): Entry, uow->{}", incomingUoW);
        
        // defensive programming
        // just return null for the Email message string
        if(incomingUoW == null){
            LOG.error(".transformPegcornEmailIntoSMTP(): Exit, incomingUoW is null!");
            return(null);
        }
        if(incomingUoW.getProcessingOutcome().equals(UoWProcessingOutcomeEnum.UOW_OUTCOME_FAILED)){
            LOG.warn(".transformPegcornEmailIntoSMTP(): Exit, incomingUoW has already failed!");
            return(null);
        }
        if (incomingUoW.getIngresContent() == null){
            LOG.error(".transformPegcornEmailIntoSMTP(): Exit, no ingress content");
            incomingUoW.setProcessingOutcome(UoWProcessingOutcomeEnum.UOW_OUTCOME_FAILED);
            incomingUoW.setFailureDescription("UoW has no ingres content!");
            return(null);
        }
        String incomingPayload = incomingUoW.getIngresContent().getPayload();
        if (StringUtils.isEmpty(incomingPayload)){
            LOG.warn(".transformPegcornEmailIntoSMTP(): Exit, PegacornSMSMessage payload is empty!");
            incomingUoW.setProcessingOutcome(UoWProcessingOutcomeEnum.UOW_OUTCOME_FAILED);
            incomingUoW.setFailureDescription("UoW ingres payload content is empty!");
            return(null);
        }
        
        // pull out the payload
        LOG.trace(".transformPegcornEmailIntoSMTP(): Mapping input payload into PegacornEmail");
        PegacornEmail email;
        try {
            email = jsonMapper.readValue(incomingPayload, PegacornEmail.class);
        } catch (JsonProcessingException  e) {
            incomingUoW.setProcessingOutcome(UoWProcessingOutcomeEnum.UOW_OUTCOME_FAILED);
            incomingUoW.setFailureDescription(e.getMessage());
            LOG.warn(".transformPegcornEmailIntoSMTP(): Exit, Error with Decoding JSON Payload", e);
            return null;
        }
        
        // add to header fields
        LOG.trace(".transformPegcornEmailIntoSMTP(): Begin setting header fields");
        if (email.getFrom() != null) {
            exchange.getIn().setHeader("from", email.getFrom());
        } else {
            incomingUoW.setProcessingOutcome(UoWProcessingOutcomeEnum.UOW_OUTCOME_FAILED);
            incomingUoW.setFailureDescription("no From address for email");
            LOG.warn(".transformPegcornEmailIntoSMTP(): Exit, no From address for email");
            return null;
        }
        if (email.getTo() != null && !email.getTo().isEmpty()) {
            exchange.getIn().setHeader("to", email.getTo().stream().collect(Collectors.joining(","))); //TODO may need to add quotation around emails or escape things
        } else {
            incomingUoW.setProcessingOutcome(UoWProcessingOutcomeEnum.UOW_OUTCOME_FAILED);
            incomingUoW.setFailureDescription("no To addresses for email");
            LOG.warn(".transformPegcornEmailIntoSMTP(): Exit, no To addresses for email");
            return null;
        }
        if (email.getSubject() != null) {
            exchange.getIn().setHeader("subject", email.getSubject());
        } else {
            LOG.warn(".transformPegcornEmailIntoSMTP(): No subject in email, continuing");
        }
        if (email.getCc() != null) {
            exchange.getIn().setHeader("cc", email.getCc().stream().collect(Collectors.joining(","))); //TODO may need to add quotation around emails or escape things
        }
        int numAttachments = 0;
        for (PegacornEmailAttachment attachment : email.getAttachments()) {
            //TODO if no data get attachment from url if possible
            //TODO if size and or hash then check this (either immediately if small or when streaming if larger)
            //TODO try to retain creation metadata on file is given as in the data field
            numAttachments++;
            LOG.trace(".transformPegcornEmailIntoSMTP(): Processing attachment {}", numAttachments);
            String attachmentName = attachment.getName();
            if (attachmentName == null) {
                // just number it
                //TODO change to use whatever else we have (url, size, creation, hash)
                attachmentName = "attachment" + numAttachments;
            }
            if (attachment.getData() == null) {
                // ignore for now
                LOG.warn(".transformPegcornEmailIntoSMTP(): Ignored attachment {} ({}): empty data field; url processing not supported");
                continue;
            }

            byte[] decodedAttachmentContent;
            try {
                decodedAttachmentContent = Base64.getDecoder().decode(attachment.getData()); //TODO probably should change to use InputStreams
            } catch (IllegalArgumentException e) {
                incomingUoW.setProcessingOutcome(UoWProcessingOutcomeEnum.UOW_OUTCOME_FAILED);
                incomingUoW.setFailureDescription("Error decoding attachment " + attachmentName + ": " + e.getMessage());
                LOG.warn(".transformPegcornEmailIntoSMTP(): Exit, Error decoding attachment {}", attachmentName, e);
                return null;
            }

            ByteArrayDataSource dataSource = new ByteArrayDataSource(decodedAttachmentContent, attachment.getContentType());
            DataHandler attachmentDataHandler = new DataHandler(dataSource);

            // add as camel attachment
            //TODO should be possible to add the size and creationTime (to Content-Disposition and Content-Length).
            AttachmentMessage attachmentMessage = exchange.getIn(AttachmentMessage.class);
//            AttachmentMessage attachmentMessage = exchange.getMessage(AttachmentMessage.class);
            attachmentMessage.addAttachment(attachmentName, attachmentDataHandler);
        }
        
        // return the content
        LOG.debug(".transformPegcornEmailIntoSMTP(): Exit");
        return email.getContent();
    }
}
