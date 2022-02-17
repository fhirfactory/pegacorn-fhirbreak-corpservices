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
package net.fhirfactory.pegacorn.identityservices.ldapgateway.transform.directory2fhir.beans;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.fhirfactory.pegacorn.datasets.fhir.r4.base.entities.practitioner.PractitionerResourceHelpers;
import net.fhirfactory.pegacorn.datasets.fhir.r4.internal.topics.FHIRElementTopicIDBuilder;
import net.fhirfactory.pegacorn.datasets.PegacornReferenceProperties;
import net.fhirfactory.pegacorn.identityservices.ldapgateway.common.LDAPUserRecord;
import net.fhirfactory.pegacorn.petasos.model.topics.TopicToken;
import net.fhirfactory.pegacorn.petasos.model.uow.UoW;
import net.fhirfactory.pegacorn.petasos.model.uow.UoWPayload;
import net.fhirfactory.pegacorn.petasos.model.uow.UoWProcessingOutcomeEnum;
import net.fhirfactory.pegacorn.util.FHIRContextUtility;
import org.hl7.fhir.r4.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class UserRecord2PractitionerBean {
    private static final Logger LOG = LoggerFactory.getLogger(UserRecord2PractitionerBean.class);

    @Inject
    private PractitionerResourceHelpers practitionerHelpers;

    @Inject
    private FHIRContextUtility fhirContextUtility;

    @Inject
    private FHIRElementTopicIDBuilder fhirElementTopicIDBuilder;

    @Inject
    private PegacornReferenceProperties systemWideProperties;

    public UoW transformUserRecord2Practitioner(UoW theUoW){
        LOG.debug(".transformUserRecord2Practitioner(): Entry, theUoW --> {}", theUoW);
        try {
            String incomingPayloadString = theUoW.getIngresContent().getPayload();
            ObjectMapper jsonMapper = new ObjectMapper();
            LDAPUserRecord userRecord = jsonMapper.readValue(incomingPayloadString, LDAPUserRecord.class);
            Practitioner practitioner = new Practitioner();
            Identifier practitionerIdentifier = practitionerHelpers.constructIdentifierFromEmail(userRecord.getEmailAddress1());
            practitioner.addIdentifier(practitionerIdentifier);
            if(userRecord.getEmailAddress2() != null){
                Identifier practitionerIdentifier2 = practitionerHelpers.constructIdentifierFromEmail(userRecord.getEmailAddress2());
                practitioner.addIdentifier(practitionerIdentifier2);
            }
            HumanName name = practitionerHelpers.constructHumanName(userRecord.getFirstName(), userRecord.getFirstName(), userRecord.getMiddleName(), userRecord.getPrefix(), userRecord.getSuffix(), HumanName.NameUse.OFFICIAL);
            practitioner.addName(name);
            ContactPoint emailContact = practitionerHelpers.constructContactPoint(userRecord.getEmailAddress1(), ContactPoint.ContactPointUse.WORK, ContactPoint.ContactPointSystem.EMAIL, 1);
            practitioner.addTelecom(emailContact);
            if(userRecord.getEmailAddress2() != null){
                ContactPoint emailContact2 = practitionerHelpers.constructContactPoint(userRecord.getEmailAddress2(), ContactPoint.ContactPointUse.WORK, ContactPoint.ContactPointSystem.EMAIL, 6);
                practitioner.addTelecom(emailContact2);
            }
            if(userRecord.getPhone1() != null){
                ContactPoint phoneContact1 = practitionerHelpers.constructContactPoint(userRecord.getPhone1(), ContactPoint.ContactPointUse.WORK, ContactPoint.ContactPointSystem.PHONE, 2);
                practitioner.addTelecom(phoneContact1);
            }
            if(userRecord.getPhone2() != null){
                ContactPoint phoneContact2 = practitionerHelpers.constructContactPoint(userRecord.getPhone2(), ContactPoint.ContactPointUse.WORK, ContactPoint.ContactPointSystem.PHONE, 7);
                practitioner.addTelecom(phoneContact2);
            }
            if(userRecord.getMobilePhone1() != null){
                ContactPoint mobileContact1A = practitionerHelpers.constructContactPoint(userRecord.getMobilePhone1(), ContactPoint.ContactPointUse.MOBILE, ContactPoint.ContactPointSystem.PHONE, 8);
                ContactPoint mobileContact1B = practitionerHelpers.constructContactPoint(userRecord.getMobilePhone1(), ContactPoint.ContactPointUse.MOBILE, ContactPoint.ContactPointSystem.SMS, 3);
                practitioner.addTelecom(mobileContact1A);
                practitioner.addTelecom(mobileContact1B);
            }
            if(userRecord.getMobilePhone2() != null){
                ContactPoint mobileContact2A = practitionerHelpers.constructContactPoint(userRecord.getMobilePhone2(), ContactPoint.ContactPointUse.MOBILE, ContactPoint.ContactPointSystem.PHONE, 9);
                ContactPoint mobileContact2B = practitionerHelpers.constructContactPoint(userRecord.getMobilePhone2(), ContactPoint.ContactPointUse.MOBILE, ContactPoint.ContactPointSystem.SMS, 4);
                practitioner.addTelecom(mobileContact2A);
                practitioner.addTelecom(mobileContact2B);
            }
            if(userRecord.getPager() != null){
                ContactPoint pagerContact = practitionerHelpers.constructContactPoint(userRecord.getPager(), ContactPoint.ContactPointUse.WORK, ContactPoint.ContactPointSystem.PAGER, 5);
                practitioner.addTelecom(pagerContact);
            }
            String practitionerString = fhirContextUtility.getJsonParser().encodeResourceToString(practitioner);
            TopicToken token = fhirElementTopicIDBuilder.createTopicToken(ResourceType.Practitioner.name(), systemWideProperties.getPegacornDefaultFHIRVersion());
            UoWPayload outputPayload = new UoWPayload(token,practitionerString);
            theUoW.setProcessingOutcome(UoWProcessingOutcomeEnum.UOW_OUTCOME_SUCCESS);
            theUoW.getEgressContent().addPayloadElement(outputPayload);
        } catch (Exception ex){
            theUoW.setProcessingOutcome(UoWProcessingOutcomeEnum.UOW_OUTCOME_FAILED);
            theUoW.setFailureDescription(ex.getMessage());
        }
        LOG.debug(".transformUserRecord2Practitioner(): Exit, theUoW --> {}", theUoW);
        return(theUoW);
    }
}
