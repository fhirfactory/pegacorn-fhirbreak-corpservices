package net.fhirfactory.pegacorn.fhirbreak.ldapscanner.interact.beans;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.directory.api.ldap.model.cursor.CursorException;
import org.apache.directory.api.ldap.model.cursor.SearchCursor;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.message.Response;
import org.apache.directory.api.ldap.model.message.SearchRequest;
import org.apache.directory.api.ldap.model.message.SearchRequestImpl;
import org.apache.directory.api.ldap.model.message.SearchResultEntry;
import org.apache.directory.api.ldap.model.message.SearchScope;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.util.DateUtils;

import net.fhirfactory.pegacorn.buildingblocks.apacheds.BaseLdapConnection;
import net.fhirfactory.pegacorn.buildingblocks.datamodels.ldap.PractitionerLdapEntry;

/**
 * LDAP scanner.
 * 
 * @author Brendan Douglas
 *
 */
public class LdapScannerConnection extends BaseLdapConnection {
	
	public LdapScannerConnection() throws LdapException {		
		super(System.getenv("APACHEDS_HOST_NAME"), Integer.parseInt(System.getenv("APACHEDS_BASE_PORT")), true,  System.getenv("APACHEDS_CONNECT_NAME"), System.getenv("APACHEDS_CONNECT_CREDENTIAL"), "dc=practitioners,dc=com"); //TODO remove the dc=com from the partitition.
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
	public List<PractitionerLdapEntry> search(Date after) throws LdapException, CursorException, IOException {	
		
		List<PractitionerLdapEntry>entries = new ArrayList<>();
			
		 // Create the SearchRequest object
	    SearchRequest searchRequest = new SearchRequestImpl();
	    searchRequest.setScope(SearchScope.ONELEVEL);
	    searchRequest.addAttributes("*","+");
	    searchRequest.setTimeLimit(0);
	    searchRequest.setBase(new Dn(baseDN));
	    searchRequest.setFilter("(|(createTimestamp >=" + DateUtils.getGeneralizedTime(after) +")(modifyTimestamp >=" + DateUtils.getGeneralizedTime(after) + "))");

	    SearchCursor searchCursor = null;

		try {
			connect();
			
			searchCursor = connection.search(searchRequest);
			
		    while (searchCursor.next())
		    {
			
		        Response response = searchCursor.get();
				
				if (response instanceof SearchResultEntry) {
					Entry resultEntry = ((SearchResultEntry) response).getEntry();
					
					PractitionerLdapEntry practitionerLdapEntry = new PractitionerLdapEntry(baseDN, resultEntry);
					
					entries.add(practitionerLdapEntry);
		        }
		    }
		} finally {
			close();
			
			searchCursor.close();
		}
		
		return entries;
	}
}
