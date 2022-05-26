package net.fhirfactory.pegacorn.fhirbreak.corpservices.ldapsync.workshops.interact.beans;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.directory.api.ldap.model.cursor.CursorException;
import org.apache.directory.api.ldap.model.entry.Attribute;
import org.apache.directory.api.ldap.model.entry.DefaultEntry;
import org.apache.directory.api.ldap.model.entry.DefaultModification;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.entry.Modification;
import org.apache.directory.api.ldap.model.entry.ModificationOperation;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fhirfactory.pegacorn.buildingblocks.datamodels.ldap.BaseLdapConnection;
import net.fhirfactory.pegacorn.buildingblocks.datamodels.ldap.LdapAttributeNameEnum;
import net.fhirfactory.pegacorn.buildingblocks.datamodels.ldap.PractitionerLdapEntry;

/**
 * LDAP sync
 * 
 * @author Brendan Douglas
 *
 */
public class LdapSyncConnection extends BaseLdapConnection {
	
	public LdapSyncConnection() throws LdapException {
	}

	private static final Logger LOG = LoggerFactory.getLogger(LdapSyncConnection.class);

	
	/**
	 * Add a practitioner entry.
	 * 
	 * @param newEntry
	 * @throws LdapException
	 */
	public void addOrModifyEntry(PractitionerLdapEntry newEntry) throws LdapException, IOException, CursorException {
				
		try {
			connect();
					
			// Check to see if the entry exists and if it does then modify it.
			PractitionerLdapEntry existingEntry = getEntry(newEntry.getCommonName());
			
			if (existingEntry != null) {
				LOG.info("Updating entry. dn -> {}", existingEntry.getDN());
				
				modifyEntry(newEntry, existingEntry);
				
				return;
			}
			
			// If we get here we are adding a new entry.
			LOG.info("Adding new entry. dn -> {}", newEntry.getDN());
			
			Entry entry = new DefaultEntry(newEntry.getDN(),
		            "ObjectClass: 1.3.6.1.4.1.18060.17.2.5",
		            "ObjectClass: top");
			
			for (Map.Entry<LdapAttributeNameEnum, String> attribute : newEntry.getAttributes().entrySet()) {				
		        entry.add(attribute.getKey().getName(), attribute.getValue());
			}
			
			connection.add(entry);
			
		} finally {
			close();
		}
	}

	
	/**
	 * Modifies a practitioner entry and only modify attributes which have changed.
	 * 
	 * @param updatedEntry
	 * @param changedAttributes
	 * @throws LdapException
	 * @throws IOException
	 */
	public void modifyEntry(PractitionerLdapEntry updatedEntry, PractitionerLdapEntry existingEntry) throws LdapException, IOException, CursorException {
		List<Modification>modifications = new ArrayList<>();
		
		try {
			connect();
			
			// The processing here is to create remove attribute modifications for the existing attributes and then add the attributes from the model.  By doing this the destination attributes will always 
			// match the source.
			List<Attribute> attributes = this.getAttributes(existingEntry.getCommonName());
			
			for (Attribute attribute : attributes) {
				modifications.add(new DefaultModification(ModificationOperation.REMOVE_ATTRIBUTE, attribute.getId()));
			}

			for (Map.Entry<LdapAttributeNameEnum, String> attribute : updatedEntry.getAttributes().entrySet()) {
					modifications.add(new DefaultModification(ModificationOperation.ADD_ATTRIBUTE, attribute.getKey().getName(),attribute.getValue()));
		    }
			
			Modification[]mods = new Modification[modifications.size()];
			
			for (int i = 0; i < modifications.size(); i++) {
				mods[i] = modifications.get(i);
			}

			connection.modify(updatedEntry.getDN(), mods);

		} finally {
			close();
		}		
	}
	
	
	public void deleteEntry(String dn) throws LdapException {	
		connection.delete(dn);
	}
	
	
	@Override
	public Logger getLogger() {
		return LOG;
	}	
}
