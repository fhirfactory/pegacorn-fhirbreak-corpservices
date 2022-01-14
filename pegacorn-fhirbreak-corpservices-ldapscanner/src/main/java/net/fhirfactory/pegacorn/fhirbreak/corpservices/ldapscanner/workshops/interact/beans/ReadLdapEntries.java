package net.fhirfactory.pegacorn.fhirbreak.corpservices.ldapscanner.workshops.interact.beans;

import java.io.IOException;
import java.util.Date;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;

import org.apache.directory.api.ldap.model.cursor.CursorException;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fhirfactory.pegacorn.buildingblocks.datamodels.ldap.PractitionerLdapEntry;

@ApplicationScoped
public class ReadLdapEntries {
    private static final Logger LOG = LoggerFactory.getLogger(ReadLdapEntries.class);
	
	private Date lastScanned = new Date();
	
	/**
	 * Reads all the LDAP entries which have been modified or inserted after the supplied date/time.
	 * 
	 * @param lastScanned
	 * @return
	 * @throws IOException
	 * @throws LdapException
	 * @throws CursorException
	 */
	public List<PractitionerLdapEntry>incrementalRead() throws IOException, LdapException, CursorException {
		LOG.info("Brendan.  Incremental read.  {}", lastScanned);
		
		LdapScannerConnection ldapScannerConnection = new LdapScannerConnection();
		
		Date dateBeforeScan = new Date();
		
		List<PractitionerLdapEntry> entries = ldapScannerConnection.read(lastScanned);
		
		LOG.info("Number of changed items: {}", entries.size());
		
		lastScanned = dateBeforeScan;
		
		return entries;
	}

		
	/**
	 * Reads all the LDAP records.
	 * 
	 * @return
	 * @throws IOException
	 * @throws LdapException
	 * @throws CursorException
	 */
	public List<PractitionerLdapEntry>readAll() throws IOException, LdapException, CursorException {
		LOG.info("Brendan.  Full read");
		
		LdapScannerConnection ldapScannerConnection = new LdapScannerConnection();
		
		List<PractitionerLdapEntry> entries = ldapScannerConnection.readAll();
		
		LOG.info("Number of items read: {}", entries.size());
		
		return entries;
	}
}
