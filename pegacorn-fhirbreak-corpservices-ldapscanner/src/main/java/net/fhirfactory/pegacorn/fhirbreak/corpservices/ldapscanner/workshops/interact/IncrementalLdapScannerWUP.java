/**
 * Copyright (c) 2020 ACT Health
 */
package net.fhirfactory.pegacorn.fhirbreak.corpservices.ldapscanner.workshops.interact;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A partial LDAP scanner WUP.  This WUP will read only the entries updated/creates since the last read.
 * 
 * @author Brendan Douglas
 *
 */
public abstract class IncrementalLdapScannerWUP extends BaseLdapScannerWUP {
    private static final Logger LOG = LoggerFactory.getLogger(IncrementalLdapScannerWUP.class);
    
    private String WUP_VERSION="1.0.0";
    private static String WUP_INSTANCE_NAME = "PartialLdapScannerHandlerWUP";

	@Override
	protected String getScanningCronExpression() {
		return "0 0/1 * 1/1 * ? *"; //TODO this will not be hardcoded
	}

	@Override
	protected String getEndpointDiscriminator() {
		return "partial-scanning";
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
        return "partial-scanning";
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
