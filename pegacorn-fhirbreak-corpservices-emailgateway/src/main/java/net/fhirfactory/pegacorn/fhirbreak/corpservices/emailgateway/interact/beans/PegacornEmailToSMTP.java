package net.fhirfactory.pegacorn.fhirbreak.corpservices.emailgateway.interact.beans;

import org.apache.camel.Exchange;

// possibly change name and just allow to support SMTP, IMAP and POP3
public class PegacornEmailToSMTP {

    public PegacornEmailToSMTP() {
    }
    
    // take our email UoW and convert to camel output for SMTP route
    // this consists of straight email body content and headers for recipient,
    // subject, etc.
    // currently just hardcoded test values
    public String toSMTP(Exchange exchange) {
        // these would be changed to come from our input as a PegacornEmail
        exchange.getIn().setHeader("to", "<bob@nowhere.act.gov.au>");
        exchange.getIn().setHeader("from", "<alice@nowhere.act.gov.au>");
        exchange.getIn().setHeader("subject", "<alice@nowhere.act.gov.au>");
        return "This is the message content";
    }
}
