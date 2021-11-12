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

import java.io.IOException;
import java.util.stream.Collectors;

import org.apache.camel.Exchange;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import net.fhirfactory.pegacorn.fhirbreak.corpservices.emailgateway.common.PegacornEmail;
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
        if (email.getTo() != null) {
            exchange.getIn().setHeader("to", email.getTo().stream().collect(Collectors.joining(","))); //TODO may need to add quotation around emails or escape things
        }
        if (email.getFrom() != null) {
            exchange.getIn().setHeader("from", email.getFrom());
        }
        if (email.getSubject() != null) {
            exchange.getIn().setHeader("subject", email.getSubject());
        }
        if (email.getCc() != null) {
            exchange.getIn().setHeader("cc", email.getCc().stream().collect(Collectors.joining(","))); //TODO may need to add quotation around emails or escape things
        }
        
        // return the content
        LOG.debug(".transformPegcornEmailIntoSMTP(): Exit");
        return email.getContent();
    }
}
