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
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.Narrative;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Period;
import org.hl7.fhir.r4.model.Person;
import org.hl7.fhir.r4.model.Practitioner;
import org.hl7.fhir.r4.model.PractitionerRole;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ResourceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ContactPointHelper {
    
    private static final Logger LOG = LoggerFactory.getLogger(ContactPointHelper.class);

    /**
     * @return true if the contactPoint has no period or if the current time is within the its period
     */
    public static boolean isActive(ContactPoint contactPoint) {
        if (!contactPoint.hasPeriod()) {
            return true;
        }
        Period validPeriod = contactPoint.getPeriod();
        DateTimeType now = DateTimeType.now();
        return (!validPeriod.hasStart() || now.after(validPeriod.getStartElement())) &&
               (!validPeriod.hasEnd() || now.before(validPeriod.getEndElement()));
    }
    
    /**
     * @return The top ranked contact point.  Throws a NoMatchingContactPointException if none found 
     */
    public static ContactPoint getTopRankContact(Resource contactableEntity, ContactPoint.ContactPointSystem contactPointType)
            throws ContactPointRetrieveException
    {
        LOG.debug(".getTopRankContact(): Entry");
        
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
            case Person:
                contactPoints = ((Person) contactableEntity).getTelecom();
                break;
            //TODO could do other references but not sure if they make sense
            default:
                throw new UnsupportedContactResourceTypeException("Unsupported resource type " + entityType);
        }
        
        // find the correct email contact point (if two of equal rank then use the first)
        DateTimeType now = DateTimeType.now();
        ContactPoint topRankContact = null;
        for (ContactPoint contactPoint : contactPoints) {
            LOG.trace(".getTopRankContact(): Checking contact point: type->{}, value->{}", contactPoint.getSystem(), contactPoint.getValue());
            if (contactPointType.equals(contactPoint.getSystem())) {
                if (topRankContact == null || (contactPoint.getRank() < topRankContact.getRank())) {
                    if (isActive(contactPoint)) {
                        LOG.trace(".getTopRankContact(): Updating top rank contact point: id->{}, value->{}",
                                contactPoint.getId(), contactPoint.getValue());
                        topRankContact = contactPoint;
                    } else {
                        LOG.trace(".getTopRankContact(): Contact is not active - skipped");
                    }
                } else {
                    LOG.trace(".getTopRankContact(): Contact is lower (or equal) rank - skipped");
                }
            } else {
                LOG.trace(".getTopRankContact(): Ignored contact point of type {}", contactPoint.getSystem());
            }
        }
        if (topRankContact == null) {
            LOG.debug(".topRankContact(): No matching contact");
            String contactableEntityDisplay;
            if (contactableEntity.hasId()) {
                contactableEntityDisplay = contactableEntity.getId();
            } else {
                Narrative narrative = null;
                switch (entityType) {
                    case Practitioner:
                        narrative = ((Practitioner) contactableEntity).getText();
                        break;
                    case PractitionerRole:
                        narrative = ((PractitionerRole) contactableEntity).getText();
                        break;
                    case Patient:
                        narrative = ((Patient) contactableEntity).getText();
                        break;
                    case Person:
                        narrative = ((Person) contactableEntity).getText();
                        break;
                    default:
                        // no handling for other types
                }
                if (narrative != null) {
                    contactableEntityDisplay = narrative.primitiveValue();
                } else {
                    // likely just reference text but at least it has the class name
                    contactableEntityDisplay = contactableEntity.toString();
                }
            }
            
            throw new NoMatchingContactException(
                    "No contact of type " + contactPointType +
                    " found for " + entityType + " " + contactableEntityDisplay);
        }
        
        LOG.debug(".getTopRankContact(): Exit, topRankContact.value->{}", topRankContact.getValue());
        return topRankContact;
    }
}
