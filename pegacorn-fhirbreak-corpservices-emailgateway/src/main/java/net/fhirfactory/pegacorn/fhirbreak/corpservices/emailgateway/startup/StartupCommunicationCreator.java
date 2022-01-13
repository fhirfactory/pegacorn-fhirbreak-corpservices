package net.fhirfactory.pegacorn.fhirbreak.corpservices.emailgateway.startup;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.Communication;
import org.hl7.fhir.r4.model.Communication.CommunicationPayloadComponent;
import org.hl7.fhir.r4.model.ContactPoint;
import org.hl7.fhir.r4.model.ContactPoint.ContactPointSystem;
import org.hl7.fhir.r4.model.Person;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.StringType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.fhir.parser.DataFormatException;
import net.fhirfactory.pegacorn.core.model.dataparcel.DataParcelManifest;
import net.fhirfactory.pegacorn.fhirbreak.corpservices.emailgateway.common.EmailDataParcelManifestBuilder;
import net.fhirfactory.pegacorn.fhirbreak.corpservices.emailgateway.transform.beans.CommunicationToPegacornEmail;
import net.fhirfactory.pegacorn.core.model.petasos.uow.UoW;
import net.fhirfactory.pegacorn.core.model.petasos.uow.UoWPayload;
import net.fhirfactory.pegacorn.core.model.petasos.uow.UoWProcessingOutcomeEnum;
import net.fhirfactory.pegacorn.util.FHIRContextUtility;


// Creates a unit of work with a JSON encoding of a communication resource
// suitable for resulting in basic startup emails.
// Note that this actually created a communication resource and then encodes it.
// this could be altered to directly create the JSON string if this proves
// too resource intensive.  It could also be converted to create a PegacornEmail
// instead of a Communication resource.  A Communication resource is used at the
// moment as this confirms the entire flow through this module.
@ApplicationScoped
public class StartupCommunicationCreator {
    
    public static final String ENV_STARTUP_FROM_EMAIL = "MAIL_STARTUP_FROM";
    public static final String ENV_STARTUP_TO_EMAIL = "MAIL_STARTUP_TO";
    public static final String STARTUP_FROM_EMAIL_DEFAULT = "noreply@pegacorn";
    
    private static final Logger LOG = LoggerFactory.getLogger(StartupCommunicationCreator.class);
    
    protected static final String FAILURE_CONVERT_TO_JSON = "Could not convert created started email Communication resource to JSON";

    @Inject
    private EmailDataParcelManifestBuilder emailManifestBuilder;
    
    @Inject
    private FHIRContextUtility fhirContextUtility;
    
    
    public UoW createStartupEmailCommunication() {
        LOG.debug(".createStartupEmailCommunication(): Entry");
        
        // email addresses to send to
        List<String> emails = new ArrayList<>();
        String envTos = System.getenv(ENV_STARTUP_TO_EMAIL);
        if (!StringUtils.isEmpty(envTos)) {
            for (String to : envTos.split(";")) {
                emails.add(to.trim());
            }
        }
        
        // email address to list from
        String fromEmail = System.getenv(ENV_STARTUP_FROM_EMAIL);
        if (fromEmail == null) {
            fromEmail = STARTUP_FROM_EMAIL_DEFAULT;
        }
        
        // create the communication resource
        Communication communication = createCommunicationToEmails(fromEmail, emails);
        
        // convert to json
        UoW initialUoW = null;
        String jsonPayload = null;
        try {
            jsonPayload = fhirContextUtility.getJsonParser().encodeResourceToString(communication);
        } catch (DataFormatException e) {
            LOG.error(".getCommunicationPayload(): Exit, " + FAILURE_CONVERT_TO_JSON, e);
            initialUoW = new UoW();
            initialUoW.setProcessingOutcome(UoWProcessingOutcomeEnum.UOW_OUTCOME_FAILED);
            initialUoW.setFailureDescription(FAILURE_CONVERT_TO_JSON + ": " + e.getMessage());
        }
        
        if (initialUoW == null) {
            DataParcelManifest manifest = emailManifestBuilder.createManifest(Communication.class, "1.0.0");
            UoWPayload uowPayload = new UoWPayload(manifest, jsonPayload);
            
            initialUoW = new UoW(uowPayload);
        }
        
        LOG.debug(".createStartupEmailCommunication(): Exit");
        return initialUoW;
    }
    
    private Communication createCommunicationToEmails(String fromEmail, Collection<String> emails) {
        Communication communication = new Communication();
        communication.setId("startup_email"); //TODO possibly should make this unique with a datetime part?
        
        communication.setSender(new Reference("#from"));
        Person from = createPersonForEmail("from", fromEmail); // currently use person as this type is handled, just use as an email value

        List<Resource> containedResources = new ArrayList<>();
        containedResources.add(from);
        
        List<Reference> recipients = new ArrayList<>();
        int i = 0;
        String id;
        for (String email : emails) {
            i++;
            id = "to_" + i;
            recipients.add(new Reference("#" + id));
            containedResources.add(createPersonForEmail(id, email));
        }
        communication.setRecipient(recipients);
        
        communication.setContained(containedResources);
        
        //TODO add better content from somewhere - probably want values from the system
        StringType content = new StringType("This email shows that the pegacorn email subsystem is working and able to send email");
        CommunicationPayloadComponent payloadComponent = new CommunicationPayloadComponent(content);
        payloadComponent.addExtension(CommunicationToPegacornEmail.EMAIL_SUBJECT_EXTENSION_URL, new StringType("Pegacorn Email Gateway Startup"));
        List<CommunicationPayloadComponent> payload = new ArrayList<>();
        payload.add(payloadComponent);
        communication.setPayload(payload);
        
        return communication;
    }
    
    private Person createPersonForEmail(String id, String email) {
        Person person = new Person();
        person.setId(id);
        ContactPoint emailTelecom = new ContactPoint();
        emailTelecom.setSystem(ContactPointSystem.EMAIL);
        emailTelecom.setValue(email);
        emailTelecom.setRank(1);
        List<ContactPoint> telecomList = new ArrayList<>();
        telecomList.add(emailTelecom);
        person.setTelecom(telecomList);
        return person;
    }
}
