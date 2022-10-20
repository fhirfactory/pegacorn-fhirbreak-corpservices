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

import net.fhirfactory.pegacorn.core.constants.petasos.PetasosPropertyConstants;
import net.fhirfactory.pegacorn.core.model.dataparcel.DataParcelManifest;
import net.fhirfactory.pegacorn.core.model.petasos.task.PetasosFulfillmentTask;
import net.fhirfactory.pegacorn.core.model.petasos.uow.UoW;
import net.fhirfactory.pegacorn.core.model.petasos.uow.UoWPayload;
import net.fhirfactory.pegacorn.core.model.petasos.uow.UoWProcessingOutcomeEnum;
import org.apache.camel.Exchange;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class SMTPToResult {

    private static final Logger LOG = LoggerFactory.getLogger(SMTPToResult.class);
    
    private static final String CAMEL_MAIL_MESSAGE_ID_HEADER = "CamelMailMessageId";
    
    

    // get the results of the SMTP communication and set the UoW status
    public UoW toResult(Exchange exchange) {
        LOG.debug("toResult(): Entry");        
        
        // get uow from exchange
        PetasosFulfillmentTask fulfillmentTask = exchange.getProperty(PetasosPropertyConstants.WUP_PETASOS_FULFILLMENT_TASK_EXCHANGE_PROPERTY, PetasosFulfillmentTask.class);
        UoW uow = SerializationUtils.clone(fulfillmentTask.getTaskWorkItem());
        if (uow == null) {
            LOG.error(".toResult(): Exit, current UoW from exchange is null!");
            return(null);
        }
        if(uow.getProcessingOutcome().equals(UoWProcessingOutcomeEnum.UOW_OUTCOME_FAILED)) {
            //TODO add extra information in this case - probably from below
            LOG.error(".transformCommunicationToEmail(): Exit, UoW recorded on exchange has already failed!  This should never happen");
            return(uow);
        }
        
        // get our email payload string for logging messages
        String emailStr = exchange.getProperty(PegacornEmailToSMTP.EMAIL_CORRELATION_STRING_EXCHANGE_PROPERTY_NAME, String.class);
        
        if (exchange.isFailed()) {
            LOG.trace(".toResult(): Failure recorded on exchange, setting outcome to failure");
            uow.setProcessingOutcome(UoWProcessingOutcomeEnum.UOW_OUTCOME_FAILED);
            String failureMessage;
            Exception lastException = exchange.getException();
            if (lastException != null) {
                failureMessage = lastException.getMessage();
            } else {
                //TODO possibly use the body instead
                failureMessage = "Non-exception failure after SMTP send";
            }
            uow.setFailureDescription(failureMessage);
            LOG.info(".toResult(): Failure for sending email->{} failureMessage->{}", emailStr, failureMessage);
        } else {
            LOG.trace(".toResult(): No failure recorded on exchange, setting outcome to success");
            uow.setProcessingOutcome(UoWProcessingOutcomeEnum.UOW_OUTCOME_SUCCESS);
        }

        // set the output to the mail Message-ID if present
        String egressPayloadStr = (String) exchange.getIn().getHeader(CAMEL_MAIL_MESSAGE_ID_HEADER);
        if (StringUtils.isEmpty(egressPayloadStr)) {
            // use the body instead
            Object body = exchange.getIn().getBody(); 
            if (body != null) {
                LOG.trace(".toResult(): body->{}", body);
                egressPayloadStr = body.toString();
            } else {
                // just log and ignore - nothing should be using the output anyway
                LOG.warn(".toResult(): email->{}: Could not get Message-ID or email body for result output", emailStr);
            }
        } else {
            LOG.info(".toResult(): email->{}, mailMessageId->{}", emailStr, egressPayloadStr);
        }
        
        // add our output body from the SMTP id to the payload
        LOG.trace(".toResult(): Setting egress payload");
        UoWPayload smtpResultPayload = new UoWPayload();
        smtpResultPayload.setPayload(egressPayloadStr);
        
        LOG.trace(".toResult(): Setting egress payload manifest");
        DataParcelManifest manifest = SerializationUtils.clone(uow.getIngresContent().getPayloadManifest());
        manifest.getContentDescriptor().setDataParcelDiscriminatorType("Response");
        manifest.getContentDescriptor().setDataParcelDiscriminatorValue("ACK");
        //TODO check if a resource type should be set here
        smtpResultPayload.setPayloadManifest(manifest);
        
        LOG.trace(".toResult(): Adding egress payload to UoW");
        uow.getEgressContent().addPayloadElement(smtpResultPayload);
        
        LOG.debug("toResult(): Exit, uow->{}", uow);
        return uow;
    }
}
