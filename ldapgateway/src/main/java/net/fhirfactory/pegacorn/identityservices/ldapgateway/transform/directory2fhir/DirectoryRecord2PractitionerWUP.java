/*
 * Copyright (c) 2021 Mark Hunter
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
package net.fhirfactory.pegacorn.identityservices.ldapgateway.transform.directory2fhir;

import net.fhirfactory.pegacorn.identityservices.ldapgateway.common.LDAPRecordTopicBuilder;
import net.fhirfactory.pegacorn.identityservices.ldapgateway.common.LDAPRecordTypeEnum;
import net.fhirfactory.pegacorn.petasos.model.processingplant.DefaultWorkshopSetEnum;
import net.fhirfactory.pegacorn.petasos.model.topics.TopicToken;
import net.fhirfactory.pegacorn.petasos.wup.archetypes.MOAStandardWUP;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.HashSet;
import java.util.Set;

@ApplicationScoped
public class DirectoryRecord2PractitionerWUP extends MOAStandardWUP {
    private static final Logger LOG = LoggerFactory.getLogger(DirectoryRecord2PractitionerWUP.class);
    private String WUP_VERSION = "1.0.0";
    private String WUP_NAME = "DirectoryRecord2PractitionerWUP";

    @Inject
    private LDAPRecordTopicBuilder ldapRecordTopicBuilder;

    @Override
    protected Logger getLogger() {
        return (LOG);
    }

    @Override
    protected Set<TopicToken> specifySubscriptionTopics() {
        HashSet<TopicToken> subscriptionSet = new HashSet<>();
        TopicToken userRecordToken = ldapRecordTopicBuilder.createTopicToken(LDAPRecordTypeEnum.LDAP_RECORD_USER_RECORD, "1.0.0");
        subscriptionSet.add(userRecordToken);
        return(subscriptionSet);
    }

    @Override
    protected String specifyWUPInstanceName() {
        return (WUP_NAME);
    }

    @Override
    protected String specifyWUPVersion() {
        return (WUP_VERSION);
    }

    @Override
    protected String specifyWUPWorkshop() {
        return (DefaultWorkshopSetEnum.TRANSFORM_WORKSHOP.getWorkshop());
    }

    @Override
    public void configure() throws Exception {

    }
}
