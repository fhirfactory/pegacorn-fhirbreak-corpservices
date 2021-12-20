package net.fhirfactory.pegacorn.fhirbreak.corpservices.smsgateway.transform.beans;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import org.hl7.fhir.r4.model.Communication;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

import ca.uhn.fhir.parser.DataFormatException;
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
    
    private List<String> getResourceStrings(String resourceNamePrefix) throws IOException, URISyntaxException {
        List<String> resourceStrings = new ArrayList<>();
        int resourceIndex = 1;
        String resourceName = resourceNamePrefix + "_" + resourceIndex + ".txt";
        URL resource = getClass().getResource(resourceName);
        Path resourcePath;
        while (resource != null) {
            resourcePath = Paths.get(resource.toURI());
            resourceStrings.add(Files.readString(resourcePath));

            resourceIndex++;
            resourceName = resourceNamePrefix + "_" + resourceIndex + ".txt";
            resource = getClass().getResource(resourceName);
        }
        return resourceStrings;
    }
    
    private void successTest(String inputResourceName, String expectedOutputResourceName) throws IOException, URISyntaxException {
        UoW outputUoW = communicationToSMSMessage.transformCommunicationToSMS(getInput(inputResourceName));
        
        Assertions.assertEquals(UoWProcessingOutcomeEnum.UOW_OUTCOME_SUCCESS, outputUoW.getProcessingOutcome(), "Expected success result for Unit of Work");

        Iterator<UoWPayload> egressPayloadIterator = outputUoW.getEgressContent().getPayloadElements().iterator();
        Assertions.assertTrue(egressPayloadIterator.hasNext(), "Egress output does not have a payload");
        UoWPayload egressPayload = egressPayloadIterator.next();
        
        String smsJson = getResourceString(expectedOutputResourceName);
        Assertions.assertEquals(smsJson, egressPayload.getPayload(), "Output JSON for SMSMessageBase did not match expected JSON");
        
        Assertions.assertTrue(!egressPayloadIterator.hasNext(), "Egress output has more than one payload");        
    }
    
    private void successTest(String resourceNamePart) throws IOException, URISyntaxException {
        successTest("/communication_" + resourceNamePart + ".txt", "/expected_sms_" + resourceNamePart + ".txt");
    }
    
    private void successTestMultiOutput(String inputResourceName, String expectedOutputResourceNamePrefix) throws IOException, URISyntaxException {
        UoW outputUoW = communicationToSMSMessage.transformCommunicationToSMS(getInput(inputResourceName));
        
        Assertions.assertEquals(UoWProcessingOutcomeEnum.UOW_OUTCOME_SUCCESS, outputUoW.getProcessingOutcome(), "Expected success result for Unit of Work");

        Iterator<UoWPayload> egressPayloadIterator = outputUoW.getEgressContent().getPayloadElements().iterator();
        Assertions.assertTrue(egressPayloadIterator.hasNext(), "Egress output does not have a payload");
        
        List<String> expectedOutputs = getResourceStrings(expectedOutputResourceNamePrefix);
        Assertions.assertFalse(expectedOutputs.isEmpty(), "No expected output files found.  Should at minimum include " + expectedOutputResourceNamePrefix + "_1.txt");
        int actualOutputNum = outputUoW.getEgressContent().getPayloadElements().size();
        int expectedOutputNum = expectedOutputs.size();
        Assertions.assertEquals(expectedOutputNum, actualOutputNum, "Number of actual and expected outputs (SMSMessageBase) do not match");
        
        String smsJson;
        Iterator<String> expectedOutputIterator;
        boolean matched;
        List<String> unmatchedSmsJson = new ArrayList<>();
        do {
            smsJson = egressPayloadIterator.next().getPayload();
            expectedOutputIterator = expectedOutputs.iterator();
            matched = false;
            do {
                if (smsJson.equals(expectedOutputIterator.next())) {
                    expectedOutputIterator.remove();
                    matched = true;
                }
            } while (!matched && expectedOutputIterator.hasNext());
            if (!matched) {
                unmatchedSmsJson.add(smsJson);
            }
        } while (egressPayloadIterator.hasNext());
        
        if (!unmatchedSmsJson.isEmpty()) {
            Assertions.fail(
                    "Unmatched output SMS.  Expected:\n" + unmatchedSmsJson.stream().collect(Collectors.joining("\n"))
                    + "\nActual:\n" + expectedOutputs.stream().collect(Collectors.joining("\n")));
        }
    }

    private void successTestMultiOutput(String resourceNamePart) throws IOException, URISyntaxException {
        successTestMultiOutput("/communication_" + resourceNamePart + ".txt", "/expected_sms_" + resourceNamePart);
    }
    
    private void failureTest(String resourceNamePart, String expectedFailureDescriptionPrefix) throws IOException, URISyntaxException {
        String inputResourceName = "/communication_" + resourceNamePart + ".txt";
        UoW outputUoW = communicationToSMSMessage.transformCommunicationToSMS(getInput(inputResourceName));
        Assertions.assertEquals(UoWProcessingOutcomeEnum.UOW_OUTCOME_FAILED, outputUoW.getProcessingOutcome(), "Expected failure result for Unit of Work");
        Assertions.assertTrue(outputUoW.getFailureDescription().startsWith(expectedFailureDescriptionPrefix),
                "Expected failure description starting with: " + expectedFailureDescriptionPrefix);
    }


    
    //
    // Success Tests
    //
    
    @Test
    public void successSimple() throws IOException, URISyntaxException {
        successTest("simple");
    }
    
    @Test
    public void multipleRecipients() throws IOException, URISyntaxException {
        successTestMultiOutput("multiple_recipients");
    }
    
    @Test
    public void noRecipient() throws IOException, URISyntaxException {
        successTest("no_recipient");
    }
    
    @Test
    public void noMessage() throws IOException, URISyntaxException {
        successTest("no_message");
    }
    
    @Test
    public void noRecipientOrContent() throws IOException, URISyntaxException {
        successTest("no_recipient_or_content");
    }
    
    //
    // Failure Tests
    //
    
    @Test
    public void failMultipleMessages() throws IOException, URISyntaxException {
        failureTest("multiple_message", CommunicationToSMSMessage.FAILURE_MULTIPLE_SMS_CONTENT);
    }
    
    @Test
    public void failRecipientPhoneNumber() throws IOException, URISyntaxException {
        failureTest("no_recipient_phone_number", CommunicationToSMSMessage.FAILURE_NO_PHONE_NUMBER_FOR_RECIPIENT);
    }
    
    @Test
    public void failRecipientNoValidContact() throws IOException, URISyntaxException {
        failureTest("no_recipient_valid_contact", CommunicationToSMSMessage.FAILURE_NO_PHONE_NUMBER_FOR_RECIPIENT);
    }
    
    @Test
    public void failInvalidRecipientReference() throws IOException, URISyntaxException {
        failureTest("invalid_recipient_reference", CommunicationToSMSMessage.FAILURE_INVALID_RECIPIENT_REFERENCE);
    }
    
    //
    // Exception Tests
    //
    
    @Test
    public void testInvalidInputType() throws IOException, URISyntaxException {
        Assertions.assertThrows(
                DataFormatException.class,
                () -> {
                    communicationToSMSMessage.transformCommunicationToSMS(getInput("/patient.txt"));
                },
                "Incorrect resource type found, expected \"Communication\" but found \"Patient\"");
    }
    
    // add tests for
    // - empty input
    // - gibberish input
}
