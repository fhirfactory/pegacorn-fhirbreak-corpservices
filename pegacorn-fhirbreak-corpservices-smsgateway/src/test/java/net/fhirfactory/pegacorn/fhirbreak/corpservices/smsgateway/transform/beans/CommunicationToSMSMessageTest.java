package net.fhirfactory.pegacorn.fhirbreak.corpservices.smsgateway.transform.beans;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Iterator;

import org.hl7.fhir.r4.model.Communication;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

import net.fhirfactory.pegacorn.fhirbreak.corpservices.smsgateway.common.SMSDataParcelManifestBuilder;
import net.fhirfactory.pegacorn.petasos.model.uow.UoW;
import net.fhirfactory.pegacorn.petasos.model.uow.UoWPayload;
import net.fhirfactory.pegacorn.petasos.model.uow.UoWProcessingOutcomeEnum;
import net.fhirfactory.pegacorn.util.FHIRContextUtility;

//This test class currently attempts to match exactly output JSON with returned JSON.  This will break if the
//formatting of this JSON changes and will require this class being updated in this case
@TestInstance(Lifecycle.PER_CLASS)
public class CommunicationToSMSMessageTest {
    
    private CommunicationToSMSMessage communicationToSMSMessage;
    private SMSDataParcelManifestBuilder manifestBuilder; //TODO remove this and replace with some sort of proxy

    @BeforeAll
    public void setup() {
        manifestBuilder = new SMSDataParcelManifestBuilder();
        communicationToSMSMessage = new CommunicationToSMSMessage(new FHIRContextUtility(), manifestBuilder);
        communicationToSMSMessage.initialise();
    }
    
    private String getResourceString(String resourceName) throws IOException, URISyntaxException {
        URL resource = getClass().getResource(resourceName);
        return Files.readString(Paths.get(resource.toURI()));        
    }
    
    private UoW getInput(String inputResourceName) throws IOException, URISyntaxException {
        // get our input communication resource
        String communicationJson = getResourceString(inputResourceName);
        
        // create our input Unit of Work
        UoWPayload communicationPayload = new UoWPayload(
                manifestBuilder.createManifest(Communication.class, "1.0.0"),
                communicationJson);
        return new UoW(communicationPayload);
    }
    
    private void successTest(String inputResourceName, String expectedOutputResourceName) throws IOException, URISyntaxException {
        UoW outputUoW = communicationToSMSMessage.transformCommunicationToSMS(getInput(inputResourceName));
        
        Assertions.assertEquals(UoWProcessingOutcomeEnum.UOW_OUTCOME_SUCCESS, outputUoW.getProcessingOutcome(), "Expected success result for Unit of Work");

        Iterator<UoWPayload> egressPayloadIterator = outputUoW.getEgressContent().getPayloadElements().iterator();
        Assertions.assertTrue(egressPayloadIterator.hasNext(), "Egress output does not have a payload");;
        UoWPayload egressPayload = egressPayloadIterator.next();
        
        String emailJson = getResourceString(expectedOutputResourceName);
        Assertions.assertEquals(emailJson, egressPayload.getPayload(), "Output JSON for SMSMessageBase did not match expected JSON");
        
        Assertions.assertTrue(!egressPayloadIterator.hasNext(), "Egress output has more than one payload");        
    }
    
    private void successTest(String resourceNamePart) throws IOException, URISyntaxException {
        successTest("/communication_" + resourceNamePart + ".txt", "/expected_sms_" + resourceNamePart + ".txt");
    }
    
    //
    // Success Tests
    //
    
    @Test
    public void successSimple() throws IOException, URISyntaxException {
        successTest("simple");
    }
    
    // add tests for:
    // - multiple recipients
    // - no recipient
    // - no message
    // - no recipient and no message
    // - complex contacts
    // - multiple payload types
    
    //
    // Failure Tests
    //
    
    // add tests for
    // - empty input
    // - gibberish input
    // - invalid recipient reference
    // - recipient without contact
    // - recipient with no valid contact
    // - multiple recipients, one without contact
    // - multiple recipients, one with no valid contact
    // - multiple payload.contentString elements
    
}
