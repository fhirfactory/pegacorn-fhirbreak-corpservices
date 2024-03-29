package net.fhirfactory.pegacorn.fhirbreak.corpservices.ldapscanner.workshops.interact.beans;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.directory.api.ldap.model.cursor.CursorException;
import org.apache.directory.api.ldap.model.cursor.SearchCursor;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.filter.FilterEncoder;
import org.apache.directory.api.ldap.model.message.Response;
import org.apache.directory.api.ldap.model.message.SearchRequest;
import org.apache.directory.api.ldap.model.message.SearchRequestImpl;
import org.apache.directory.api.ldap.model.message.SearchResultEntry;
import org.apache.directory.api.ldap.model.message.SearchScope;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.util.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fhirfactory.pegacorn.buildingblocks.datamodels.ldap.BaseLdapConnection;
import net.fhirfactory.pegacorn.buildingblocks.datamodels.ldap.PractitionerLdapEntry;

/**
 * LDAP scanner.
 * 
 * @author Brendan Douglas
 *
 */
public class LdapScannerConnection extends BaseLdapConnection {
    private static final Logger LOG = LoggerFactory.getLogger(LdapScannerConnection.class);
	
	public LdapScannerConnection() throws LdapException {		

	}

	
	/**
	 * Returns all LDAP entries.
	 * 
	 * @return
	 * @throws LdapException
	 * @throws CursorException
	 * @throws IOException
	 */
	public List<PractitionerLdapEntry> getAll()  throws LdapException, CursorException, IOException {
		return read(null);
	}
	
	
	/**
	 * Returns a list of LDAP entries where the createTimestamp or the modifyTimestamp is >= the date supplied.
	 * 
	 * @param after
	 * @param baseDN
	 * @return
	 * @throws LdapException
	 * @throws CursorException
	 * @throws IOException
	 */
	public List<PractitionerLdapEntry> read(Date after) throws LdapException, CursorException, IOException {	
	
		List<PractitionerLdapEntry>entries = new ArrayList<>();
			
		 // Create the SearchRequest object
	    SearchRequest searchRequest = new SearchRequestImpl();
	    searchRequest.setScope(SearchScope.ONELEVEL);
	    searchRequest.addAttributes("*","+");
	    searchRequest.setTimeLimit(0);
	    searchRequest.setBase(new Dn(baseDN));
	    
	    if (after == null) {
	    	searchRequest.setFilter("(&(objectCategory=Organizationalperson)(objectClass=person))");
	    } else {
	    	searchRequest.setFilter(FilterEncoder.format("(&(&(objectCategory=Organizationalperson)(objectClass=person))(|(createTimestamp>={0})(modifyTimestamp>={1})))",DateUtils.getGeneralizedTime(after), DateUtils.getGeneralizedTime(after)));
	    }
		
	    SearchCursor searchCursor = null;
	    
		try {
			connect();

			searchCursor = connection.search(searchRequest);
			
		    while (searchCursor.next()) {
		        Response response = searchCursor.get();
				
				if (response instanceof SearchResultEntry) {
					
					Entry resultEntry = ((SearchResultEntry) response).getEntry();
					
					PractitionerLdapEntry practitionerLdapEntry = new PractitionerLdapEntry(resultEntry, baseDN);
					
					entries.add(practitionerLdapEntry);
		        }
		    }
		} finally {
			close();
			
			searchCursor.close();
		}
		
		return entries;
	}
	
	
	public List<PractitionerLdapEntry> readAll() throws LdapException, CursorException, IOException {
		return read(null);
	}


	@Override
	public Logger getLogger() {
		return LOG;
	}	
}
