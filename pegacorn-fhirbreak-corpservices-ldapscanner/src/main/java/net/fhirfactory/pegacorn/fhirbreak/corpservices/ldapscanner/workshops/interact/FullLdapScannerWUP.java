/**
 * Copyright (c) 2020 ACT Health
 */
package net.fhirfactory.pegacorn.fhirbreak.corpservices.ldapscanner.workshops.interact;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A full LDAP scanner WUP.  This WUP will read all entries.
 * 
 * @author Brendan Douglas
 *
 */
public abstract class FullLdapScannerWUP extends BaseLdapScannerWUP {
    private static final Logger LOG = LoggerFactory.getLogger(FullLdapScannerWUP.class);
    
    private String WUP_VERSION="1.0.0";
    
    private String cronExpression = System.getenv("FULL_SCAN_CRON_EXPRESSION"); 

	@Override
	protected String getScanningCronExpression() {
		return cronExpression;
	}

	@Override
	protected String getEndpointDiscriminator() {
		return "full-scanning";
	}
	
	@Override
	protected Logger specifyLogger() {
		return LOG;
	}

	
    @Override
    protected String specifyIngresTopologyEndpointName() {
        return "ldapServer";
    }

    @Override
    protected String specifyIngresEndpointVersion() {
        return ("1.0.0");
    }
    
    @Override
    protected String specifyWUPInstanceVersion() {
        return (WUP_VERSION);
    }
}
