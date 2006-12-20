/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.exampleplugin;

import net.java.sip.communicator.service.gui.*;

import org.osgi.framework.*;

public class ExamplePluginActivator implements BundleActivator
{
    public void start(BundleContext bc) throws Exception
    {   
        ServiceReference uiServiceRef
            = bc.getServiceReference(UIService.class.getName());
        
        UIService uiService
            = (UIService) bc.getService(uiServiceRef);        
        
        if(uiService.isContainerSupported(
            UIService.CONTAINER_CONTACT_RIGHT_BUTTON_MENU))
        {   
            ExamplePluginMenuItem examplePlugin = new ExamplePluginMenuItem();
            
            uiService.addComponent(
                UIService.CONTAINER_CONTACT_RIGHT_BUTTON_MENU,
                examplePlugin);
                
        }
        
    }

    public void stop(BundleContext bc) throws Exception
    {   
    }    
}
