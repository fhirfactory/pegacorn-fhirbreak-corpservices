/*
 * Copyright (c) 2021 ACT Health
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package net.fhirfactory.pegacorn.fhirbreak.corpservices.emailgateway.transform.wup;

import java.util.ArrayList;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fhirfactory.pegacorn.components.dataparcel.DataParcelManifest;
import net.fhirfactory.pegacorn.components.interfaces.topology.WorkshopInterface;
import net.fhirfactory.pegacorn.fhirbreak.corpservices.emailgateway.common.EmailDataParcelManifestBuilder;
import net.fhirfactory.pegacorn.fhirbreak.corpservices.emailgateway.transform.beans.CommunicationToPegacornEmail;
import net.fhirfactory.pegacorn.workshops.TransformWorkshop;
import net.fhirfactory.pegacorn.wups.archetypes.petasosenabled.messageprocessingbased.MOAStandardWUP;

@ApplicationScoped
public class CommunicationToPegacornEmailWUP extends MOAStandardWUP {
    
    private static final Logger LOG = LoggerFactory.getLogger(CommunicationToPegacornEmailWUP.class);
    
    private static final String WUP_VERSION = "1.0.0";
    private static final String WUP_NAME = "CommunicationToPegacornEmailWUP";

    
    @Inject
    private TransformWorkshop workshop;
    
    @Inject
    private EmailDataParcelManifestBuilder emailManifestBuilder;

    
    public CommunicationToPegacornEmailWUP() {
    }

    @Override
    protected Logger specifyLogger() {
        return LOG;
    }

    @Override
    protected List<DataParcelManifest> specifySubscriptionTopics() {
        DataParcelManifest manifest = emailManifestBuilder.createManifest(EmailDataParcelManifestBuilder.TYPE_COMMUNICATION, "1.0.0");
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
            .bean(CommunicationToPegacornEmail.class)
            .to(egressFeed());
    }
}
