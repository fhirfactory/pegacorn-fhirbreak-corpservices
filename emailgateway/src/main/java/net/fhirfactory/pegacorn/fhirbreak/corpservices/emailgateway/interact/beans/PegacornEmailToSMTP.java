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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Base64;
import java.util.stream.Collectors;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.URLDataSource;
import javax.enterprise.context.ApplicationScoped;
import javax.mail.util.ByteArrayDataSource;

import net.fhirfactory.pegacorn.internals.communicate.entities.message.CommunicateEmailMessage;
import net.fhirfactory.pegacorn.internals.communicate.entities.message.datatypes.CommunicateEmailAttachment;
import org.apache.camel.Exchange;
import org.apache.camel.attachment.Attachment;
import org.apache.camel.attachment.AttachmentMessage;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import net.fhirfactory.pegacorn.core.model.petasos.uow.UoW;
import net.fhirfactory.pegacorn.core.model.petasos.uow.UoWProcessingOutcomeEnum;

//TODO possibly change name and just allow to support IMAP and POP3 also (as they essentially use the same parameters in camel)
// Note that the size, hash and creationTime of the email are currently not passed through or used
//TODO fix setting failures on the incomingUoW.  It seems that these are not saved.  This will either need a seperate bean
//     to validate before this bean or will need some way to save out updates to the UoW to the petasos fulfillment task
@ApplicationScoped
public class PegacornEmailToSMTP {
	
	// used by SMTPToResult instead for logging
	//TODO possibly better to move earlier in the process or replace with some other type of correlation id
    public static final String EMAIL_CORRELATION_STRING_EXCHANGE_PROPERTY_NAME = "EmailCorrelationString";
    
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
        CommunicateEmailMessage email = null;
        try {
            email = jsonMapper.readValue(incomingPayload, CommunicateEmailMessage.class);
        } catch (JsonProcessingException  e) {
            incomingUoW.setProcessingOutcome(UoWProcessingOutcomeEnum.UOW_OUTCOME_FAILED);
            incomingUoW.setFailureDescription(e.getMessage());
            LOG.warn(".transformPegcornEmailIntoSMTP(): Exit, Error with Decoding JSON Payload", e);
            return null;
        }
        
        // add our email string details to the exchange for SMTPToResult to use
        exchange.setProperty(EMAIL_CORRELATION_STRING_EXCHANGE_PROPERTY_NAME, email.toString());
        
        // add to header fields
        LOG.trace(".transformPegcornEmailIntoSMTP(): Begin setting header fields");
        if (email.getFrom() != null) {
            exchange.getIn().setHeader("from", email.getFrom());
        } else {
            incomingUoW.setProcessingOutcome(UoWProcessingOutcomeEnum.UOW_OUTCOME_FAILED);
            incomingUoW.setFailureDescription("no From address for email");
            LOG.warn(".transformPegcornEmailIntoSMTP(): Exit, no From address for email->{}", email);
            return null;
        }
        if (email.getTo() != null && !email.getTo().isEmpty()) {
            exchange.getIn().setHeader("to", email.getTo().stream().collect(Collectors.joining(","))); //TODO may need to add quotation around emails or escape things
        } else {
            incomingUoW.setProcessingOutcome(UoWProcessingOutcomeEnum.UOW_OUTCOME_FAILED);
            incomingUoW.setFailureDescription("no To addresses for email");
            LOG.warn(".transformPegcornEmailIntoSMTP(): Exit, no To addresses for email->{}", email);
            return null;
        }
        if (email.getSubject() != null) {
            exchange.getIn().setHeader("subject", email.getSubject());
        } else {
            LOG.warn(".transformPegcornEmailIntoSMTP(): No subject in email, continuing: email->{}", email);
        }
        if (email.getContentType() != null) {
            exchange.getIn().setHeader("contentType", email.getContentType());
        }
        if (email.getCc() != null) {
            exchange.getIn().setHeader("cc", email.getCc().stream().collect(Collectors.joining(","))); //TODO may need to add quotation around emails or escape things
        }
        int numAttachments = 0;
        for (CommunicateEmailAttachment attachment : email.getAttachments()) {

            numAttachments++;
            LOG.trace(".transformPegcornEmailIntoSMTP(): Processing attachment {}", numAttachments);
            String attachmentName = attachment.getName();
            if (attachmentName == null) {
                // just number it
                //TODO change to use whatever else we have (url, size, creation, hash)
                attachmentName = "attachment" + numAttachments;
            }
            
            DataSource dataSource;
            if (attachment.getData() == null) {
                if (attachment.getUrl() == null) {
                    // ignore for now
                    //TODO check if this should count as a failure
                    LOG.warn(".transformPegcornEmailIntoSMTP(): Ignored attachment: empty data and url fields: attachment->{}", attachment);
                    continue;
                }
                URL url;
                try {
                    url = new URL(attachment.getUrl());
                } catch (MalformedURLException e) {
                    //TODO check if should ignore and continue instead
                    incomingUoW.setProcessingOutcome(UoWProcessingOutcomeEnum.UOW_OUTCOME_FAILED);
                    incomingUoW.setFailureDescription("Error with attachment url: " + attachment.getUrl() + ": " + e.getMessage());
                    LOG.warn(".transformPegcornEmailIntoSMTP(): Exit, Error with attachment url->{}", attachment.getUrl(), e);
                    return null;
                }
                
                // currently we don't check the url for possible security issues.  This is assuming the source is trusted. 
                //TODO add some security checks around this
                dataSource = new URLDataSource(url);
            } else {
                byte[] decodedAttachmentContent;
                try {
                    decodedAttachmentContent = Base64.getDecoder().decode(attachment.getData()); //TODO probably should change to use InputStreams
                } catch (IllegalArgumentException e) {
                    incomingUoW.setProcessingOutcome(UoWProcessingOutcomeEnum.UOW_OUTCOME_FAILED);
                    incomingUoW.setFailureDescription("Error decoding attachment " + attachmentName + ": " + e.getMessage());
                    LOG.warn(".transformPegcornEmailIntoSMTP(): Exit, Error decoding attachment {}", attachmentName, e);
                    return null;
                }
    
                dataSource = new ByteArrayDataSource(decodedAttachmentContent, attachment.getContentType());
            }
            DataHandler attachmentDataHandler = new DataHandler(dataSource);

            // add as camel attachment
            AttachmentMessage attachmentMessage = exchange.getIn(AttachmentMessage.class);
            attachmentMessage.addAttachment(attachmentName, attachmentDataHandler);

            // put in the size and creation date if present
            //TODO also add in the sha1 hash if there is somewhere to put it.  There does not seem to be a standard header for this so it is left for now
            if (attachment.getSize() != null || attachment.getCreationTime() != null) {
                Attachment justAdded = attachmentMessage.getAttachmentObject(attachmentName);
                String contentDispositionHeader = "attachment; filename=" + attachmentName;  // these values will be overwritten later by Apache Camel but set correctly to be on the safe side
                if (attachment.getCreationTime() != null) {
                    //TODO convert to RFC2822 format rather than just putting in as is (either here or in CommunicationToPegacornEmail)
                    contentDispositionHeader += "; creation-date=\"" + attachment.getCreationTime() + "\"";
                }
                if (attachment.getSize() != null) {
                    contentDispositionHeader += "; size=" + attachment.getSize();
                }
                justAdded.setHeader("Content-Disposition", contentDispositionHeader);
            }
        }
        
        // return the content
        LOG.debug(".transformPegcornEmailIntoSMTP(): Exit");
        return email.getContent();
    }
}
