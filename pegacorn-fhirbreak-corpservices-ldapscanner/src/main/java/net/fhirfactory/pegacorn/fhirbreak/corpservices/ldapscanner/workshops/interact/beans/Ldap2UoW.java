package net.fhirfactory.pegacorn.fhirbreak.corpservices.ldapscanner.workshops.interact.beans;

import java.io.IOException;

import org.apache.directory.api.ldap.model.cursor.CursorException;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fhirfactory.pegacorn.petasos.model.uow.UoW;

/**
 * Converts a list of LDAP {@link Entry} objects to a {@link UoW} 
 * 
 * @author Brendan Douglas
 *
 */
public class Ldap2UoW {
    private static final Logger LOG = LoggerFactory.getLogger(Ldap2UoW.class);
	
	 /**
	  * Read from LDAP and add the content to the unit of work.
	  * 
	 * @return
	 */
	public UoW encapsulateLdapData() throws LdapException, IOException, CursorException {
//		
//        TopicToken payloadTopicToken = new TopicToken();
//        payloadTopicToken.setIdentifier(null); //TODO set the identifier
//        payloadTopicToken.setVersion("1.0.0"); 
//	
//		LdapScannerConnection ldapScannerConnection = new LdapScannerConnection();
//		
//		List<PractitionerLdapEntry>entries = ldapScannerConnection.search(null); //TODO get the after date from somewhere.
//		
//        UoWPayload emptyPayload = new UoWPayload();
//        
//        UoW newUoW = new UoW(emptyPayload);
//        
//        
//        for (PractitionerLdapEntry entry : entries) {            
//            UoWPayload contentPayload = new UoWPayload();
//        
//            contentPayload.setPayloadTopicID(payloadTopicToken);
//            contentPayload.setPayload(""); //TODO add the entry
//            
//            newUoW.getEgressContent().getPayloadElements().add(contentPayload);
//        }
//        
//        // Now, if we've gotten to here, then all is "good" and so we should set the UoW processing status accordingly.
//        newUoW.setProcessingOutcome(UoWProcessingOutcomeEnum.UOW_OUTCOME_SUCCESS);
//		
//        return newUoW;
		
		return null;
	 }

}
