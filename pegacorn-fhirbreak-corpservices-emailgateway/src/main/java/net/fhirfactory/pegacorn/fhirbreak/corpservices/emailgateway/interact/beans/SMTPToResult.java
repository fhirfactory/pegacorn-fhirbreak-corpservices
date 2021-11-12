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

import org.apache.camel.Exchange;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fhirfactory.pegacorn.components.dataparcel.DataParcelManifest;
import net.fhirfactory.pegacorn.petasos.model.configuration.PetasosPropertyConstants;
import net.fhirfactory.pegacorn.petasos.model.uow.UoW;
import net.fhirfactory.pegacorn.petasos.model.uow.UoWPayload;
import net.fhirfactory.pegacorn.petasos.model.uow.UoWProcessingOutcomeEnum;

public class SMTPToResult {

    private static final Logger LOG = LoggerFactory.getLogger(SMTPToResult.class);

    // get the results of the SMTP communication and set the UoW status
    public UoW toResult(Exchange exchange) {
        LOG.debug("toResult(): Entry");        
        
        // get uow from exchange
        UoW uow = (UoW) exchange.getProperty(PetasosPropertyConstants.WUP_CURRENT_UOW_EXCHANGE_PROPERTY_NAME);
        if (uow == null) {
            LOG.error(".toResult(): Exit, current UoW from exchange is null!");
            return(null);
        }
        if(uow.getProcessingOutcome().equals(UoWProcessingOutcomeEnum.UOW_OUTCOME_FAILED)){
            LOG.warn(".transformCommunicationToEmail(): Exit, UoW recorded on exchange has already failed!");
            return(uow);
        }
        
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
        } else {
            LOG.trace(".toResult(): No failure recorded on exchange, setting outcome to success");
            uow.setProcessingOutcome(UoWProcessingOutcomeEnum.UOW_OUTCOME_SUCCESS);
        }
        
        Object body = exchange.getIn().getBody(); 
        if (body != null) {
            LOG.trace(".toResult(): body->{}", body);
            
            // get as string
            String bodyStr = body.toString();
            
            // add our output body from the SMTP id to the payload
            //TODO work out what this could be and udpate
            LOG.trace(".toResult(): Setting egress payload");
            UoWPayload smtpResultPayload = new UoWPayload();
            if (!StringUtils.isEmpty(bodyStr)) {
                smtpResultPayload.setPayload(bodyStr);
            }
            
            LOG.trace(".toResult(): Setting egress payload manifest");
            DataParcelManifest manifest = SerializationUtils.clone(uow.getIngresContent().getPayloadManifest());
            manifest.getContentDescriptor().setDataParcelDiscriminatorType("Response");
            manifest.getContentDescriptor().setDataParcelDiscriminatorValue("ACK");
            smtpResultPayload.setPayloadManifest(manifest);
            
            LOG.trace(".toResult(): Adding egress payload to UoW");
            uow.getEgressContent().addPayloadElement(smtpResultPayload);
        }
        
        
        LOG.warn("toResult(): Exit, uow->{}", uow);  //TODO change to debug
        return uow;
    }
}
