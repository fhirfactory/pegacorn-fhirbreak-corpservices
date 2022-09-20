package net.fhirfactory.pegacorn.fhirbreak.corpservices.emailgateway.interact.beans;

import org.apache.camel.Exchange;
import org.apache.camel.Predicate;

import net.fhirfactory.dricats.model.petasos.uow.UoW;
import net.fhirfactory.dricats.model.petasos.uow.UoWProcessingOutcomeEnum;

public class IsSuccessfulUoW implements Predicate {

    @Override
    public boolean matches(Exchange camelExchange) {
        UoW uow = (UoW) camelExchange.getIn().getBody();
        return uow != null &&
               uow.getProcessingOutcome().equals(UoWProcessingOutcomeEnum.UOW_OUTCOME_SUCCESS);
    }
}
