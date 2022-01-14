package net.fhirfactory.pegacorn.fhirbreak.corpservices.emailgateway.startup;

import java.util.ArrayList;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.hl7.fhir.r4.model.Communication;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fhirfactory.pegacorn.core.model.dataparcel.DataParcelManifest;
import net.fhirfactory.pegacorn.core.interfaces.topology.WorkshopInterface;
import net.fhirfactory.pegacorn.fhirbreak.corpservices.emailgateway.common.EmailDataParcelManifestBuilder;
import net.fhirfactory.pegacorn.fhirbreak.corpservices.emailgateway.transform.wup.CommunicationToPegacornEmailWUP;
import net.fhirfactory.pegacorn.petasos.wup.helper.IngresActivityBeginRegistration;
import net.fhirfactory.pegacorn.workshops.TransformWorkshop;
import net.fhirfactory.pegacorn.wups.archetypes.petasosenabled.messageprocessingbased.MOAStandardWUP;


// Route to trigger sending of set email on startup.  This is done 20 seconds
// after creation using a apache camel timer.
// Note that the intention of this route is to allow confirmation that email
// sending is functioning
@ApplicationScoped
public class StartupEmailTrigger extends MOAStandardWUP {
    
    private static final Logger LOG = LoggerFactory.getLogger(StartupEmailTrigger.class);
    
    @Inject
    private TransformWorkshop workshop; //TODO possibly should be a gateway workshop?  Not that either really fits
    
    @Inject
    private CommunicationToPegacornEmailWUP emailRoute;

    @Inject
    private EmailDataParcelManifestBuilder emailManifestBuilder;
    

    @Override
    public void configure() throws Exception {
        fromIncludingPetasosServices("timer://startupemailtrigger?repeatCount=1&delay=20000")
        .bean(StartupCommunicationCreator.class)
        .bean(IngresActivityBeginRegistration.class)
        .to(emailRoute.getIngresEndpoint().getEndpointSpecification());
    }

    @Override
    protected Logger specifyLogger() {
        return LOG;
    }

    @Override
    protected List<DataParcelManifest> specifySubscriptionTopics() {
        return new ArrayList<>();
    }
    
    @Override
    protected List<DataParcelManifest> declarePublishedTopics() {
        DataParcelManifest manifest = emailManifestBuilder.createManifest(Communication.class, "1.5.0");
        List<DataParcelManifest> manifestList = new ArrayList<>();
        manifestList.add(manifest);
        return manifestList;
    }

    @Override
    protected String specifyWUPInstanceName() {
        return "StartupEmailTrigger";
    }

    @Override
    protected String specifyWUPInstanceVersion() {
        return "1.0.0";
    }

    @Override
    protected WorkshopInterface specifyWorkshop() {
        return workshop;
    }
}
