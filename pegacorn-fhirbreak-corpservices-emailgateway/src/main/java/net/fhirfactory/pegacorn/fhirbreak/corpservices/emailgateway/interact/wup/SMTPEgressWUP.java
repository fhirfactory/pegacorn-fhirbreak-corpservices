package net.fhirfactory.pegacorn.fhirbreak.corpservices.emailgateway.interact.wup;

import java.util.ArrayList;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fhirfactory.pegacorn.components.dataparcel.DataParcelManifest;
import net.fhirfactory.pegacorn.components.interfaces.topology.WorkshopInterface;
import net.fhirfactory.pegacorn.fhirbreak.corpservices.emailgateway.common.EmailDataParcelManifestBuilder;
import net.fhirfactory.pegacorn.fhirbreak.corpservices.emailgateway.interact.beans.PegacornEmailToSMTP;
import net.fhirfactory.pegacorn.workshops.InteractWorkshop;
import net.fhirfactory.pegacorn.wups.archetypes.petasosenabled.messageprocessingbased.MOAStandardWUP;

@ApplicationScoped
public class SMTPEgressWUP extends MOAStandardWUP {
    
    public static final String PROP_SMTP_HOST = "smtp.hostname";
    public static final String PROP_SMTP_PORT = "smtp.port";
    
    private static final Logger LOG = LoggerFactory.getLogger(SMTPEgressWUP.class);
    private static final String WUP_VERSION = "1.0.0";
    private static final String WUP_NAME = "SMTPEgressWUP";

    
    @Inject
    private InteractWorkshop workshop;
    
    @Inject
    private EmailDataParcelManifestBuilder emailManifestBuilder;

    
    public SMTPEgressWUP() {
    }

    @Override
    protected Logger specifyLogger() {
        return LOG;
    }

    @Override
    protected List<DataParcelManifest> specifySubscriptionTopics() {
        DataParcelManifest manifest = emailManifestBuilder.createManifest("PegacornEmail", "1.0.0"); //TODO fix up hardcoded values
        List<DataParcelManifest> manifestList = new ArrayList<>();
        manifestList.add(manifest);
        return manifestList;
    }

    @Override
    protected String specifyWUPInstanceName() {
        return WUP_NAME;
    }

    @Override
    protected String specifyWUPInstanceVersion() {
        return WUP_VERSION;
    }

    @Override
    protected WorkshopInterface specifyWorkshop() {
        return workshop;
    }

    @Override
    public void configure() throws Exception {
        getLogger().info("{}:: ingresFeed() --> {}", getClass().getSimpleName(), ingresFeed());
//        getLogger().info("{}:: egressFeed() --> {}", getClass().getSimpleName(), egressFeed());
        
        fromIncludingPetasosServices(ingresFeed())
            .routeId(getNameSet().getRouteCoreWUP())
            .bean(PegacornEmailToSMTP.class)
//            .to(egressFeed());
            .to("smtp://{{" + PROP_SMTP_HOST + "}}:{{" + PROP_SMTP_PORT + "}}?debugMode=true");
    }
}
