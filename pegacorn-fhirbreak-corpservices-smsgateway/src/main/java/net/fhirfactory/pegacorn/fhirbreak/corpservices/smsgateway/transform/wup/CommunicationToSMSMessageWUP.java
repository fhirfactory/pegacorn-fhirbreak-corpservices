package net.fhirfactory.pegacorn.fhirbreak.corpservices.smsgateway.transform.wup;

import java.util.ArrayList;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.hl7.fhir.r4.model.Communication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fhirfactory.pegacorn.components.interfaces.topology.WorkshopInterface;
import net.fhirfactory.pegacorn.components.dataparcel.DataParcelManifest;
import net.fhirfactory.pegacorn.fhirbreak.corpservices.smsgateway.common.SMSDataParcelManifestBuilder;
import net.fhirfactory.pegacorn.fhirbreak.corpservices.smsgateway.transform.beans.CommunicationToSMSMessage;
import net.fhirfactory.pegacorn.workshops.TransformWorkshop;
import net.fhirfactory.pegacorn.wups.archetypes.petasosenabled.messageprocessingbased.MOAStandardWUP;


@ApplicationScoped
public class CommunicationToSMSMessageWUP extends MOAStandardWUP {
    
    private static final Logger LOG = LoggerFactory.getLogger(CommunicationToSMSMessageWUP.class);
    
    private static final String WUP_VERSION = "1.0.0";
    private static final String WUP_NAME = "CommunicationToSMSMessageWUP";

    
    @Inject
    private TransformWorkshop workshop;
    
    @Inject
    private SMSDataParcelManifestBuilder smsManifestBuilder;

    
    public CommunicationToSMSMessageWUP() {
    }

    @Override
    protected Logger specifyLogger() {
        return LOG;
    }

    @Override
    protected List<DataParcelManifest> specifySubscriptionTopics() {
        DataParcelManifest manifest = smsManifestBuilder.createManifest(Communication.class, "1.0.0");
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
        getLogger().info("{}:: egressFeed() --> {}", getClass().getSimpleName(), egressFeed());
        
        fromIncludingPetasosServices(ingresFeed())
            .routeId(getNameSet().getRouteCoreWUP())
            .bean(CommunicationToSMSMessage.class)
            .to(egressFeed());
    }


}
