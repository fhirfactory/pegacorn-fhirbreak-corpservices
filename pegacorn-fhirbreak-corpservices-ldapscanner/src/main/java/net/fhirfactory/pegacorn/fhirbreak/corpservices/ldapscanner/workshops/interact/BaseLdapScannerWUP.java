/**
 * Copyright (c) 2020 ACT Health
 */
package net.fhirfactory.pegacorn.fhirbreak.corpservices.ldapscanner.workshops.interact;

import javax.inject.Inject;

import org.apache.camel.Exchange;

import net.fhirfactory.pegacorn.components.interfaces.topology.WorkshopInterface;
import net.fhirfactory.pegacorn.deployment.topology.model.endpoints.interact.StandardInteractClientTopologyEndpointPort;
import net.fhirfactory.pegacorn.fhirbreak.corpservices.ldapscanner.workshops.interact.beans.Ldap2UoW;
import net.fhirfactory.pegacorn.fhirbreak.corpservices.ldapscanner.workshops.interact.beans.ReadLdapEntries;
import net.fhirfactory.pegacorn.petasos.core.moa.wup.TriggerBasedWUPEndpoint;
import net.fhirfactory.pegacorn.petasos.wup.helper.IngresActivityBeginRegistration;
import net.fhirfactory.pegacorn.workshops.InteractWorkshop;
import net.fhirfactory.pegacorn.wups.archetypes.petasosenabled.messageprocessingbased.InteractIngresAPIClientGatewayWUP;

/**
 * Base class for all LDAP scanner classes.  These class will read the entries from the directory.
 * 
 * @author Brendan Douglas
 *
 */
public abstract class BaseLdapScannerWUP extends InteractIngresAPIClientGatewayWUP {
       
	
	protected abstract String getScanningCronExpression();
	protected abstract String getEndpointDiscriminator();
	
	@Inject
	private InteractWorkshop interactWorkshop;

	
    
    
	@Override
	protected WorkshopInterface specifyWorkshop() {
		return interactWorkshop;
	}
	
	
	@Override
    public void configure() throws Exception {		
        getLogger().info("{}:: ingresFeed() --> {}", getClass().getSimpleName(), ingresFeed());
        getLogger().info("{}:: egressFeed() --> {}", getClass().getSimpleName(), egressFeed());
        
        
        // Handle any exceptions which are not being handled in more specified exception handlers. //TODO 
        onException(Exception.class)
            .process(exchange -> {
            	final Exception ex = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);
            	getLogger().error("Error caught in interact WUP", ex);
            })
            .handled(true)
            .end();
        
        from("quartz://" + getEndpointDiscriminator() + "?cron=" + getScanningCronExpression())
		.routeId(getNameSet().getRouteCoreWUP() + getEndpointDiscriminator())
		.bean(ReadLdapEntries.class, "read")
        .to(ingresFeed());
        
               
       	

        // Convert the LDAP entries to a UoW.
        fromInteractIngresService(ingresFeed())
	    	.routeId(getNameSet().getRouteCoreWUP())
	    	.bean(Ldap2UoW.class,"encapsulateLdapData")
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
    
    
	@Override
	protected TriggerBasedWUPEndpoint specifyIngresEndpoint() {
		TriggerBasedWUPEndpoint endpoint = new TriggerBasedWUPEndpoint();
		
        getLogger().info("Brendan .specifyIngresEndpoint(): Entry, specifyIngresTopologyEndpointName()->{}", specifyIngresTopologyEndpointName());

        StandardInteractClientTopologyEndpointPort serverTopologyEndpoint = (StandardInteractClientTopologyEndpointPort) getTopologyEndpoint(specifyIngresTopologyEndpointName());
        
        getLogger().info("Brendan: {}", serverTopologyEndpoint);
        
        getLogger().trace(".specifyIngresEndpoint(): Retrieved serverTopologyEndpoint->{}", serverTopologyEndpoint);
        endpoint.setEndpointSpecification("xxxxxxxxxxxxxxxx");
        endpoint.setEndpointTopologyNode(serverTopologyEndpoint);
        endpoint.setFrameworkEnabled(false);
        serverTopologyEndpoint.setConnectedSystemName("ACTGOV IDAM");
        getLogger().debug(".specifyIngresEndpoint(): Exit, endpoint->{}", endpoint);
        return (endpoint);
	}
}
