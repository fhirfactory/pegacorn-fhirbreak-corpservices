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
package net.fhirfactory.pegacorn.fhirbreak.corpservices.emailgateway.interact.wup;

import java.util.ArrayList;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fhirfactory.pegacorn.core.model.dataparcel.DataParcelManifest;
import net.fhirfactory.pegacorn.core.interfaces.topology.WorkshopInterface;
import net.fhirfactory.pegacorn.fhirbreak.corpservices.emailgateway.common.EmailDataParcelManifestBuilder;
import net.fhirfactory.pegacorn.fhirbreak.corpservices.emailgateway.common.PegacornEmail;
import net.fhirfactory.pegacorn.fhirbreak.corpservices.emailgateway.interact.beans.PegacornEmailToSMTP;
import net.fhirfactory.pegacorn.fhirbreak.corpservices.emailgateway.interact.beans.SMTPToResult;
import net.fhirfactory.pegacorn.workshops.InteractWorkshop;
import net.fhirfactory.pegacorn.wups.archetypes.petasosenabled.messageprocessingbased.MOAStandardWUP;

@ApplicationScoped
public class SMTPEgressWUP extends MOAStandardWUP {
    
    // mail properties - same names as javax.mail, which is used under the hood, except for mail.smtp.debug which would just
    // be mail.debug
    public static final String PROP_SMTP_HOST = "mail.smtp.host";
    public static final String PROP_SMTP_PORT = "mail.smtp.port";
    public static final String PROP_SMTP_DEBUG = "mail.smtp.debug";
    public static final String PROP_SMTP_CONNECTION_TIMEOUT = "mail.smtp.connectiontimeout";
    
    // environment variables are only used if properties do not exist
    public static final String ENV_SMTP_HOST = "MAIL_SMTP_HOST";
    public static final String ENV_SMTP_PORT = "MAIL_SMTP_PORT";
    public static final String ENV_SMTP_DEBUG = "MAIL_SMTP_DEBUG";
    public static final String ENV_SMTP_CONNECTION_TIMEOUT = "MAIL_SMTP_TIMEOUT";
    
    // this can be altered to use optional property placeholders after upgrading to Apache Camel 3.9 or later
    private static final String SMTP_CAMEL_ENDPOINT =
            "smtp://{{" + PROP_SMTP_HOST + ":{{env:" + ENV_SMTP_HOST + "}}}}" +
            ":{{" + PROP_SMTP_PORT + ":{{env:" + ENV_SMTP_PORT + ":25}}}}" +
            "?debugMode={{" + PROP_SMTP_DEBUG + ":{{env:" + ENV_SMTP_DEBUG + ":false}}}}" +
            "&connectionTimeout={{" + PROP_SMTP_CONNECTION_TIMEOUT + ":{{env:" + ENV_SMTP_CONNECTION_TIMEOUT + ":30000}}}}";
    
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
        DataParcelManifest manifest = emailManifestBuilder.createManifest(PegacornEmail.class, "1.5.0"); //TODO fix up hardcoded values
        List<DataParcelManifest> manifestList = new ArrayList<>();
        manifestList.add(manifest);
        return manifestList;
    }
    
    @Override
    protected List<DataParcelManifest> declarePublishedTopics() {
        DataParcelManifest manifest = emailManifestBuilder.createManifest(String.class, "1.5.0"); //TODO fix up hardcoded values
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
            .bean(PegacornEmailToSMTP.class)
            .choice()
                .when().simple("${body} != null")
                .to(SMTP_CAMEL_ENDPOINT)
            .end()
            .bean(SMTPToResult.class)
            .to(egressFeed());
    }
}
