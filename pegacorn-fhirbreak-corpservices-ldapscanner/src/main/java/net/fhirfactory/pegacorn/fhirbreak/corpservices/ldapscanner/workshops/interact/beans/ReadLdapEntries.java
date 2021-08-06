package net.fhirfactory.pegacorn.fhirbreak.corpservices.ldapscanner.workshops.interact.beans;

import java.io.IOException;
import java.util.List;

import org.apache.directory.api.ldap.model.cursor.CursorException;
import org.apache.directory.api.ldap.model.exception.LdapException;

import net.fhirfactory.pegacorn.buildingblocks.datamodels.ldap.PractitionerLdapEntry;

public class ReadLdapEntries {
	
	public List<PractitionerLdapEntry>read() throws IOException, LdapException, CursorException {
		LdapScannerConnection ldapScannerConnection = new LdapScannerConnection();
		
		return ldapScannerConnection.search(null); //TODO get the after date from somewhere.
	}

}
