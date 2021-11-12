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
package net.fhirfactory.pegacorn.fhirbreak.corpservices.emailgateway.fhir;

import java.util.List;

import org.hl7.fhir.r4.model.ContactPoint;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Practitioner;
import org.hl7.fhir.r4.model.PractitionerRole;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ResourceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ContactPointHelper {
    
    private static final Logger LOG = LoggerFactory.getLogger(ContactPointHelper.class);

    public static String getTopRankContact(Resource contactableEntity, ContactPoint.ContactPointSystem contactPointType)
            throws ContactPointRetrieveException
    {
        if (contactableEntity == null || contactPointType == null) {
            throw new IllegalArgumentException("Must specify contactableEntity and contactPointType");
        }
        
        List<ContactPoint> contactPoints;
        ResourceType entityType = contactableEntity.getResourceType();
        switch (entityType) {
            case Practitioner:
                contactPoints = ((Practitioner) contactableEntity).getTelecom();
                break;
            case PractitionerRole:
                contactPoints = ((PractitionerRole) contactableEntity).getTelecom();
                break;
            case Patient:
                contactPoints = ((Patient) contactableEntity).getTelecom();
                break;
            //TODO could do other references but not sure if they make sense
            default:
                throw new UnsupportedContactResourceTypeException("Unsupported resource type " + entityType);
        }
        
        // find the correct email contact point (if two of equal rank then use the first)
        ContactPoint topRankContact = null;
        for (ContactPoint contactPoint : contactPoints) {
            if (contactPointType.equals(contactPoint.getSystem())) {
                if (topRankContact == null || (contactPoint.getRank() < topRankContact.getRank())) {
                    //TODO only replace here if the contact point is still valid
                    //     i.e. check the period
                    topRankContact = contactPoint;
                }
            } else {
                LOG.trace(".transformCommunicationToEmail(): Ignored contact point of type {}", contactPoint.getSystem());
            }
        }
        if (topRankContact == null) {
            throw new NoMatchingContactException(
                    "No contact of type " + contactPointType +
                    " found for " + entityType + " " + contactableEntity);
        }
        
        return topRankContact.getValue();
    }
}
