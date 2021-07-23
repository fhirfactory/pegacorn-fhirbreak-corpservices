/**
 * Copyright (c) 2020 ACT Health
 */
package net.fhirfactory.pegacorn.fhirbreak.corpservices.ldapscanner.workshops.interact;

import javax.inject.Inject;

import org.apache.camel.ExchangePattern;

import net.fhirfactory.pegacorn.components.interfaces.topology.WorkshopInterface;
import net.fhirfactory.pegacorn.fhirbreak.corpservices.ldapscanner.workshops.interact.beans.Ldap2UoW;
import net.fhirfactory.pegacorn.petasos.core.moa.wup.MessageBasedWUPEndpoint;
import net.fhirfactory.pegacorn.petasos.wup.helper.IngresActivityBeginRegistration;
import net.fhirfactory.pegacorn.workshops.InteractWorkshop;
import net.fhirfactory.pegacorn.wups.archetypes.petasosenabled.messageprocessingbased.InteractIngresMessagingGatewayWUP;

/**
 * Base class for all LDAP scanner classes.  These class will read the entries from the directory.
 * 
 * @author Brendan Douglas
 *
 */
public abstract class BaseLdapScannerWUP extends InteractIngresMessagingGatewayWUP {
       
	
	protected abstract String getScanningCronExpression();
	protected abstract String getEndpointDiscriminator();
	
	@Inject
	private InteractWorkshop interactWorkshop;

	
	@Override
	protected MessageBasedWUPEndpoint specifyIngresEndpoint() {
		// TODO Auto-generated method stub
		return null;
	}
    
    
	@Override
	protected WorkshopInterface specifyWorkshop() {
		return interactWorkshop;
	}
	
	
	@Override
    public void configure() throws Exception {
        
        getLogger().info("{}:: ingresFeed() --> {}", getClass().getSimpleName(), ingresFeed());
        getLogger().info("{}:: egressFeed() --> {}", getClass().getSimpleName(), egressFeed());
        
        
        //TODO add exception handling.
        
        from("quartz://" + getEndpointDiscriminator() + "?cron=" + getScanningCronExpression())
        	.routeId(getNameSet().getRouteCoreWUP() + "LDAP-Scanner")  
        	.bean(Ldap2UoW.class,"encapsulateLdapData")
        	.to(ingresFeed());    

           
	    from(ingresFeed())
	        .routeId(getNameSet().getRouteCoreWUP())
	        .to(ExchangePattern.InOnly, getProcessingRouteIngresPoint());
    
    
	    fromInteractIngresService(ingresFeed())
	    	.routeId(getNameSet().getRouteCoreWUP())
	    	.bean(IngresActivityBeginRegistration.class, "registerActivityStart(*,  Exchange)")
	        .to(egressFeed());
    }
	
	
    @Override
    public String ingresFeed() {
        return "direct:ingresFeed-" + getEndpointDiscriminator();
    }
    
    
    @Override
    protected String specifyIngresEndpointVersion() {
        return "1.0.0";
    }

    
    protected String getProcessingRouteIngresPoint() {
        String routeEndpointName = "seda:" + getWupInstanceName() + "-process-" + getEndpointDiscriminator();
        return (routeEndpointName);
    }  
}
