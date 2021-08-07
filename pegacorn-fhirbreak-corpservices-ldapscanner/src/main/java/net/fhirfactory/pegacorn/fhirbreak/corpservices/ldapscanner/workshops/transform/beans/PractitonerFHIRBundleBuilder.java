package net.fhirfactory.pegacorn.fhirbreak.corpservices.ldapscanner.workshops.transform.beans;

import java.util.ArrayList;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.hl7.fhir.r4.model.ContactPoint;
import org.hl7.fhir.r4.model.ContactPoint.ContactPointSystem;
import org.hl7.fhir.r4.model.ContactPoint.ContactPointUse;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.HumanName.NameUse;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Practitioner;
import org.hl7.fhir.r4.model.PractitionerRole;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fhirfactory.pegacorn.buildingblocks.datamodels.ldap.PractitionerLdapEntry;
import net.fhirfactory.pegacorn.internals.fhir.r4.resources.organization.factories.OrganizationFactory;
import net.fhirfactory.pegacorn.internals.fhir.r4.resources.practitioner.factories.PractitionerFactory;
import net.fhirfactory.pegacorn.internals.fhir.r4.resources.practitioner.factories.PractitionerResourceHelpers;
import net.fhirfactory.pegacorn.internals.fhir.r4.resources.resource.datatypes.ContactPointFactory;
import net.fhirfactory.pegacorn.petasos.model.uow.UoW;

@ApplicationScoped
public class PractitonerFHIRBundleBuilder {
	
    private static final Logger LOG = LoggerFactory.getLogger(PractitonerFHIRBundleBuilder.class);
    
    @Inject
    private PractitionerResourceHelpers practitionerResourceHelper;
    
    @Inject
    private PractitionerFactory practitionerFactory;
    
    @Inject
    private ContactPointFactory contactPointFactory;
    
    @Inject
    private OrganizationFactory organizationFactory;
    
    public void buildFHIRBundle(UoW incomingUoW) {
		
		// The UoW contains a JSON representation of the LDAP entry.  Retrieve this and convert.
        JSONObject jsonLdapEntry = new JSONObject(incomingUoW.getIngresContent().getPayload());
        PractitionerLdapEntry ldapEntry = new PractitionerLdapEntry("dc=practitioners,dc=com", jsonLdapEntry); 
       
        Practitioner practitioner = createPractitioner(ldapEntry);
        List<ContactPoint> contactPoints = createContactPoints(ldapEntry);
        Organization organisation = createOrganization(ldapEntry); 
        PractitionerRole practitionerRole = createPractitionerRole(ldapEntry, practitioner);
        
        LOG.info("Brendan:.  Practitioner name: {}", practitioner.getNameFirstRep().getGivenAsSingleString());
	}

    
    private Practitioner createPractitioner(PractitionerLdapEntry ldapEntry) {
    	HumanName humanName = practitionerResourceHelper.constructHumanName(ldapEntry.getGivenName().getValue(), ldapEntry.getSurname().getValue(), null, ldapEntry.getPersonalTitle().getValue(), ldapEntry.getSuffix().getValue(), NameUse.OFFICIAL);
    	Identifier emailIdentifier = practitionerResourceHelper.buildPractitionerIdentifierFromEmail(ldapEntry.getEmailAddress().getValue());
    	
    	return practitionerFactory.buildPractitioner(humanName, emailIdentifier);
    }
    
    
    public List<ContactPoint> createContactPoints(PractitionerLdapEntry ldapEntry) {
    	List<ContactPoint> contactPoints = new ArrayList<>();
    	
    	contactPoints.add(contactPointFactory.buildContactPoint(ldapEntry.getMobileNumber().getValue(), ContactPointUse.WORK , ContactPointSystem.PHONE, 0));
    	contactPoints.add(contactPointFactory.buildContactPoint(ldapEntry.getPhoneNumber().getValue(), ContactPointUse.WORK , ContactPointSystem.PHONE, 1));
    	contactPoints.add(contactPointFactory.buildContactPoint(ldapEntry.getPager().getValue(), ContactPointUse.WORK , ContactPointSystem.PAGER, 2));
    	
    	return null;
    }
    
    
    public Organization createOrganization(PractitionerLdapEntry ldapEntry) {    	
    	return null;
    }
    
    
    public PractitionerRole createPractitionerRole(PractitionerLdapEntry ldapEntry, Practitioner practitioner) {
    	PractitionerRole practitionerRole = new PractitionerRole();
    	
    	practitionerRole.setPractitionerTarget(practitioner);
    	practitionerRole.addCode().addCoding().setDisplay(ldapEntry.getJobTitle().getValue());
    	
    	return practitionerRole;
    	
    	
    }
}
