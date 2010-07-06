/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 * 
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.securityconfig;

import java.awt.*;

import javax.swing.*;

import net.java.sip.communicator.service.gui.*;

import org.osgi.framework.*;

/**
 * The main security configuration form panel.
 *
 * @author Yana Stamcheva
 */
public class SecurityConfigurationPanel
    extends JTabbedPane
    implements ServiceListener
{
    /**
     * Creates the <tt>SecurityConfigurationPanel</tt>.
     */
    public SecurityConfigurationPanel()
    {
        init();
        SecurityConfigActivator.bundleContext.addServiceListener(this);
    }

    /**
     * Initializes this panel.
     */
    private void init()
    {
        String osgiFilter = "("
            + ConfigurationForm.FORM_TYPE
            + "="+ConfigurationForm.SECURITY_TYPE+")";

        ServiceReference[] confFormsRefs = null;
        try
        {
            confFormsRefs = SecurityConfigActivator.bundleContext
                .getServiceReferences(  ConfigurationForm.class.getName(),
                                        osgiFilter);
        }
        catch (InvalidSyntaxException ex)
        {}

        if(confFormsRefs != null)
        {
            for (int i = 0; i < confFormsRefs.length; i++)
            {
                ConfigurationForm form
                    = (ConfigurationForm) SecurityConfigActivator.bundleContext
                        .getService(confFormsRefs[i]);

                Object formComponent = form.getForm();
                if (formComponent instanceof Component)
                    addTab(form.getTitle(), (Component) formComponent);
            }
        }
    }

    /**
     * Handles registration of a new configuration form.
     * @param event the <tt>ServiceEvent</tt> that notified us
     */
    public void serviceChanged(ServiceEvent event)
    {
        ServiceReference serviceRef = event.getServiceReference();

        Object property = serviceRef.getProperty(ConfigurationForm.FORM_TYPE);
        if (property != ConfigurationForm.SECURITY_TYPE)
            return;

        Object sService
            = SecurityConfigActivator.bundleContext
                .getService(serviceRef);

        // we don't care if the source service is not a configuration form
        if (!(sService instanceof ConfigurationForm))
            return;

        ConfigurationForm configForm = (ConfigurationForm) sService;

        if (!configForm.isAdvanced())
            return;

        Object formComponent;
        switch (event.getType())
        {
        case ServiceEvent.REGISTERED:
            formComponent = configForm.getForm();
            if (formComponent instanceof Component)
                addTab(configForm.getTitle(), (Component) formComponent);
            break;

        case ServiceEvent.UNREGISTERING:
            formComponent = configForm.getForm();
            if (formComponent instanceof Component)
                remove((Component) formComponent);
            break;
        }
    }
}