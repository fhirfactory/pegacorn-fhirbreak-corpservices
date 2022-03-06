package net.fhirfactory.pegacorn.fhirbreak.corpservices.emailgateway.transform.beans;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Iterator;

import net.fhirfactory.pegacorn.internals.communicate.entities.message.factories.CommunicationToPegacornEmailFactory;
import org.hl7.fhir.r4.model.Communication;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

import ca.uhn.fhir.parser.DataFormatException;
import net.fhirfactory.pegacorn.fhirbreak.corpservices.emailgateway.common.EmailDataParcelManifestBuilder;
import net.fhirfactory.pegacorn.core.model.petasos.uow.UoW;
import net.fhirfactory.pegacorn.core.model.petasos.uow.UoWPayload;
import net.fhirfactory.pegacorn.core.model.petasos.uow.UoWProcessingOutcomeEnum;
import net.fhirfactory.pegacorn.util.FHIRContextUtility;

// This test class currently attempts to match exactly output JSON with returned JSON.  This will break if the
// formatting of this JSON changes and will require this class being updated in this case
@TestInstance(Lifecycle.PER_CLASS)
public class CommunicationToPegacornEmailTest {
    
    private CommunicationToPegacornEmailFactory communicationToEmail;
    private EmailDataParcelManifestBuilder manifestBuilder; //TODO remove this and replace with some sort of proxy

    /*
    @BeforeAll
    public void setup() {
        manifestBuilder = new EmailDataParcelManifestBuilder();
        communicationToEmail = new CommunicationToPegacornEmailFactory(new FHIRContextUtility(), manifestBuilder);
        communicationToEmail.initialise();
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
        UoW outputUoW = communicationToEmail.transformCommunicationToEmail(getInput(inputResourceName));
        
        Assertions.assertEquals(UoWProcessingOutcomeEnum.UOW_OUTCOME_SUCCESS, outputUoW.getProcessingOutcome(), "Expected success result for Unit of Work");

        Iterator<UoWPayload> egressPayloadIterator = outputUoW.getEgressContent().getPayloadElements().iterator();
        Assertions.assertTrue(egressPayloadIterator.hasNext(), "Egress output does not have a payload");;
        UoWPayload egressPayload = egressPayloadIterator.next();
        
        String emailJson = getResourceString(expectedOutputResourceName);
        emailJson = emailJson.replaceAll("\n", System.lineSeparator());
        Assertions.assertEquals(emailJson, egressPayload.getPayload(), "Output JSON for PegacornEmail did not match expected JSON");
        
        Assertions.assertTrue(!egressPayloadIterator.hasNext(), "Egress output has more than one payload");        
    }
    
    private void successTest(String resourceNamePart) throws IOException, URISyntaxException {
        successTest("/communication_" + resourceNamePart + ".txt", "/expected_email_" + resourceNamePart + ".txt");
    }
    
    private void failureTest(String inputResourceName, String expectedOutputResourceName, String expectedFailureDescriptionPrefix) throws IOException, URISyntaxException {
        UoW outputUoW = communicationToEmail.transformCommunicationToEmail(getInput(inputResourceName));
        Assertions.assertEquals(UoWProcessingOutcomeEnum.UOW_OUTCOME_FAILED, outputUoW.getProcessingOutcome(), "Expected failure result for Unit of Work");
        Assertions.assertTrue(outputUoW.getFailureDescription().startsWith(expectedFailureDescriptionPrefix),
                "Expected failure description starting with: " + expectedFailureDescriptionPrefix);
    }
    
    private void failureTest(String resourceNamePart, String expectedFailureDescriptionPrefix) throws IOException, URISyntaxException {
        failureTest("/communication_" + resourceNamePart + ".txt", "/expected_email_" + resourceNamePart + ".txt", expectedFailureDescriptionPrefix);
    }

    
    //
    // Success Tests
    //

    @Test
    public void testEmpty() throws IOException, URISyntaxException {
        successTest("empty");
    }
    
    @Test
    public void testComplexBase() throws IOException, URISyntaxException {
        successTest("base_1");
    }
    
    @Test
    public void testNoSender() throws IOException, URISyntaxException {
        successTest("no_sender");
    }
    
    @Test
    public void testNoRecipients() throws IOException, URISyntaxException {
        successTest("no_recipients");
    }
    
    @Test
    public void testNoSubject() throws IOException, URISyntaxException {
        successTest("no_subject");
    }
    
    @Test
    public void testNoContent() throws IOException, URISyntaxException {
        successTest("no_content");
    }

    @Test
    public void testNoSubjectOrContent() throws IOException, URISyntaxException {
        successTest("no_subject_or_content");
    }
    
    @Test
    public void testMultipleRecipients() throws IOException, URISyntaxException {
        successTest("multiple_recipients");
    }

     */
    
    //
    // Failure Tests
    //
    /*


    @Test
    public void testMultipleContent() throws IOException, URISyntaxException {
        failureTest("multiple_content", CommunicationToPegacornEmailFactory.FAILURE_MULTIPLE_CONTENT);
    }
    
    @Test
    public void testInvalidSenderReference() throws IOException, URISyntaxException {
        failureTest("invalid_sender_reference", CommunicationToPegacornEmailFactory.FAILURE_INVALID_SENDER_REFERENCE);
    }
    
    @Test
    public void testInvalidRecipientReference() throws IOException, URISyntaxException {
        failureTest("invalid_recipient_reference", CommunicationToPegacornEmailFactory.FAILURE_INVALID_RECIPIENT_REFERENCE);
    }
    
    @Test
    public void testNoValidSenderEmail() throws IOException, URISyntaxException {
        failureTest("no_valid_sender_email", CommunicationToPegacornEmailFactory.FAILURE_NO_EMAIL_FOR_SENDER);
    }
    
    @Test
    public void testNoValidRecipientEmail() throws IOException, URISyntaxException {
        failureTest("no_valid_recipient_email", CommunicationToPegacornEmailFactory.FAILURE_NO_EMAIL_FOR_RECIPIENT);
    }
    
    //
    // Exception Tests
    //
    
    @Test
    public void testInvalidInputType() throws IOException, URISyntaxException {
        Assertions.assertThrows(
                DataFormatException.class,
                () -> {
                    communicationToEmail.transformCommunicationToEmail(getInput("/patient.txt"));
                },
                "Incorrect resource type found, expected \"Communication\" but found \"Patient\"");
    }
    
    
    // tests to have
    // - gibberish input
    // - invalid subject extension url

     */
}
