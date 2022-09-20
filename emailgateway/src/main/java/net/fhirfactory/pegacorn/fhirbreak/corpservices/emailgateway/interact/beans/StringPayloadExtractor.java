package net.fhirfactory.pegacorn.fhirbreak.corpservices.emailgateway.interact.beans;

import java.util.Iterator;

import javax.enterprise.context.ApplicationScoped;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fhirfactory.dricats.model.petasos.uow.UoW;
import net.fhirfactory.dricats.model.petasos.uow.UoWPayload;
import net.fhirfactory.dricats.model.petasos.uow.UoWProcessingOutcomeEnum;

// bean that simply extracts the string payload and returns it.  This is used
// by the SMTP route as the successful case expects the route body to be the
// email content
@ApplicationScoped
public class StringPayloadExtractor {
    private static final Logger LOG = LoggerFactory.getLogger(StringPayloadExtractor.class);

    // note that we extract from the egress payload as the way this is called means
    // that petasos does not do its thing in between
    public String extractEgressStringPayload(UoW uow) {
        LOG.debug(".extractStringPayload(): Entry, uow->{}", uow);
        
        // defensive programming
        // just return null for the Email message string and log as error.
        // These will result in exceptions as none of these should ever occur as they should
        // be checked prior to this bean
        if(uow == null){
            LOG.error(".extractStringPayload(): Exit, incomingUoW is null!");
            return null;
        }
        if (uow.getProcessingOutcome().equals(UoWProcessingOutcomeEnum.UOW_OUTCOME_FAILED)) {
        	// this should never happen as it should be checked prior to this bean
            LOG.error(".extractStringPayload(): Exit, incomingUoW has already failed!");
            return null;
        }
        if (uow.getEgressContent() == null) {
        	// this should never happen as it should be checked prior to this bean
            LOG.error(".extractStringPayload(): Exit, no egress content");
            return null;
        }
        
        Iterator<UoWPayload> payloadIterator = uow.getEgressContent().getPayloadElements().iterator();
        if (!payloadIterator.hasNext()) {
        	// this should never happen as it should be checked prior to this bean
            LOG.error(".extractStringPayload(): Exit, Egress payload is empty!");
            return null;
        }
        
        String payload = payloadIterator.next().getPayload();

        if (payloadIterator.hasNext()) {
            LOG.error(".extractStringPayload(): Exit, More than one egress payload to extract from!");
            return null;
        }

        LOG.debug(".extractStringPayload(): Exit, payload->{}", payload);
        return payload;
    }
}
