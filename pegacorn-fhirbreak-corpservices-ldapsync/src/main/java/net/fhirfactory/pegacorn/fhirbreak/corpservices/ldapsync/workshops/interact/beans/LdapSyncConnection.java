package net.fhirfactory.pegacorn.fhirbreak.corpservices.ldapsync.workshops.interact.beans;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.directory.api.ldap.model.entry.DefaultEntry;
import org.apache.directory.api.ldap.model.entry.DefaultModification;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.entry.Modification;
import org.apache.directory.api.ldap.model.entry.ModificationOperation;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fhirfactory.pegacorn.buildingblocks.apacheds.BaseLdapConnection;
import net.fhirfactory.pegacorn.buildingblocks.datamodels.ldap.LdapAttribute;
import net.fhirfactory.pegacorn.buildingblocks.datamodels.ldap.LdapAttributeNameEnum;
import net.fhirfactory.pegacorn.buildingblocks.datamodels.ldap.PractitionerLdapEntry;

/**
 * LDAP sync
 * 
 * @author Brendan Douglas
 *
 */
public class LdapSyncConnection extends BaseLdapConnection {
	private static final Logger LOG = LoggerFactory.getLogger(LdapSyncConnection.class);

	public LdapSyncConnection() throws LdapException {
		super(System.getenv("APACHEDS_HOST_NAME"), Integer.parseInt(System.getenv("APACHEDS_BASE_PORT")), true,  System.getenv("APACHEDS_CONNECT_NAME"), System.getenv("APACHEDS_CONNECT_CREDENTIAL"), "dc=practitioners,dc=com"); //TODO remove the dc=com from the partitition.
	}

	
	/**
	 * Add a practitioner entry.
	 * 
	 * @param newEntry
	 * @throws LdapException
	 */
	public void addEntry(PractitionerLdapEntry newEntry) throws LdapException, IOException {
				
		try {
			Entry entry = new DefaultEntry(newEntry.getDN(),
		            "ObjectClass: 1.3.6.1.4.1.18060.17.2.5",
		            "ObjectClass: top");
			
			for (Map.Entry<LdapAttributeNameEnum, LdapAttribute> attribute : newEntry.getAttributes().entrySet()) {
		        entry.add(attribute.getKey().getName(), attribute.getValue().getValue());
			}
			
			entry.add("cn", newEntry.getCommonName());
			
			connect();
			
			connection.add(entry);
			
		} finally {
			close();
		}
	}

	
	/**
	 * Modify a practitioner entry.
	 * 
	 * @param updatedEntry
	 * @throws LdapException
	 */
	public void modifyEntry(PractitionerLdapEntry updatedEntry) throws LdapException, IOException {
		List<Modification>modifications = new ArrayList<>();
		
		try {
			
			for (Map.Entry<LdapAttributeNameEnum, LdapAttribute> attribute : updatedEntry.getAttributes().entrySet()) {
				modifications.add(new DefaultModification(ModificationOperation.REPLACE_ATTRIBUTE, attribute.getKey().getName(),attribute.getValue().getValue()));
		    }
			
			connect();
			
			for (Modification modification : modifications) {
				connection.modify(updatedEntry.getDN(), modification);
			}

		} finally {
			close();
		}
	}


	@Override
	public Logger getLogger() {
		return LOG;
	}	
}
