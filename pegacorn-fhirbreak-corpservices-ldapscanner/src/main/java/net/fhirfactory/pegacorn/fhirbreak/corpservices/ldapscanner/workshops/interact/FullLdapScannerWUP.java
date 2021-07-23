/**
 * Copyright (c) 2020 ACT Health
 */
package net.fhirfactory.pegacorn.fhirbreak.corpservices.ldapscanner.workshops.interact;

import javax.enterprise.context.ApplicationScoped;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A full LDAP scanner WUP.  This WUP will read all entries.
 * 
 * @author Brendan Douglas
 *
 */
@ApplicationScoped
public class FullLdapScannerWUP extends BaseLdapScannerWUP {
    private static final Logger LOG = LoggerFactory.getLogger(FullLdapScannerWUP.class);
    
    private String WUP_VERSION="1.0.0";
    private static String WUP_INSTANCE_NAME = "FullLdapScannerHandlerWUP";

	@Override
	protected String getScanningCronExpression() {
		return "0 0/5 * 1/1 * ? *"; //TODO this will not be hardcoded
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
	protected String specifyWUPInstanceName() {
		return WUP_INSTANCE_NAME;
	}
	
    @Override
    protected String specifyIngresTopologyEndpointName() {
        return "full-scanning";
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
