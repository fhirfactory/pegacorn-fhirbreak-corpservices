package net.fhirfactory.pegacorn.fhirbreak.corpservices.ldapsync.workshops.interact.beans;

import java.io.IOException;

import org.apache.directory.api.ldap.model.cursor.CursorException;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fhirfactory.pegacorn.buildingblocks.datamodels.ldap.PractitionerLdapEntry;
import net.fhirfactory.pegacorn.petasos.model.uow.UoW;
import net.fhirfactory.pegacorn.petasos.model.uow.UoWPayload;
import net.fhirfactory.pegacorn.petasos.model.uow.UoWProcessingOutcomeEnum;

/**
 * Writes an LDAP entry.
 * 
 * @author brendan_douglas_a
 *
 */
public class LdapEntryWriter {
	private static final Logger LOG = LoggerFactory.getLogger(LdapEntryWriter.class);
	
	public UoW writerLdapEntry(UoW incomingUoW) throws IOException, LdapException, CursorException{
		
		JSONObject ldapEntryJson = new JSONObject(incomingUoW.getIngresContent().getPayload());
		
		LOG.info("Brendan.  The json: {}", ldapEntryJson.toString());
		
		PractitionerLdapEntry ldapEntry = new PractitionerLdapEntry(System.getenv("LDAP_SERVER_BASE_DN"), ldapEntryJson);
		
		LOG.info("Brendan.  In writer email address: {}", ldapEntry.getEmailAddress());
		
		LdapSyncConnection connection = new LdapSyncConnection();
		
		connection.addOrModifyEntry(ldapEntry);
		
        UoWPayload uowPayload = new UoWPayload();
        uowPayload.setPayload("LDAP entry stored successfully");
        incomingUoW.getEgressContent().addPayloadElement(uowPayload);
        incomingUoW.setProcessingOutcome(UoWProcessingOutcomeEnum.UOW_OUTCOME_SUCCESS);
        
		LOG.info("Brendan.  end of writer");
         
        return incomingUoW;
		
	}

}
