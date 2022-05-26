package net.fhirfactory.pegacorn.fhirbreak.corpservices.emailgateway.workflow;

import net.fhirfactory.pegacorn.core.interfaces.topology.WorkshopInterface;
import net.fhirfactory.pegacorn.core.model.dataparcel.DataParcelManifest;
import net.fhirfactory.pegacorn.fhirbreak.corpservices.emailgateway.workflow.beans.StartupCommunicateEmailCreator;
import net.fhirfactory.pegacorn.petasos.wup.helper.IngresActivityBeginRegistration;
import net.fhirfactory.pegacorn.workshops.WorkflowWorkshop;
import net.fhirfactory.pegacorn.wups.archetypes.petasosenabled.messageprocessingbased.MOAStandardWUP;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;


// Route to trigger sending of set email on startup.  This is done 20 seconds
// after creation using a apache camel timer.
// Note that the intention of this route is to allow confirmation that email
// sending is functioning
@ApplicationScoped
public class StartupEmailTriggerWUP extends MOAStandardWUP {
    
    private static final Logger LOG = LoggerFactory.getLogger(StartupEmailTriggerWUP.class);
    
    @Inject
    private WorkflowWorkshop workshop;

    @Override
    public void configure() throws Exception {
        fromIncludingPetasosServices("timer://startupemailtrigger?repeatCount=1&delay=20000")
            .bean(StartupCommunicateEmailCreator.class)
            .bean(IngresActivityBeginRegistration.class)
            .to(egressFeed());
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
        List<DataParcelManifest> manifestList = new ArrayList<>();
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
