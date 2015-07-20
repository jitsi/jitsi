/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Copyright @ 2015 Atlassian Pty Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.java.sip.communicator.plugin.securityconfig;

import java.awt.*;
import java.util.*;

import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.gui.*;

import org.osgi.framework.*;

/**
 * The main security configuration form panel.
 *
 * @author Yana Stamcheva
 */
public class SecurityConfigurationPanel
    extends SIPCommTabbedPane
    implements ServiceListener
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

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
        Collection<ServiceReference<ConfigurationForm>> confFormsRefs;
        String osgiFilter
            = "(" + ConfigurationForm.FORM_TYPE + "="
                + ConfigurationForm.SECURITY_TYPE + ")";

        try
        {
            confFormsRefs
                = SecurityConfigActivator.bundleContext.getServiceReferences(
                        ConfigurationForm.class,
                        osgiFilter);
        }
        catch (InvalidSyntaxException ex)
        {
            confFormsRefs = null;
        }

        if ((confFormsRefs != null) && !confFormsRefs.isEmpty())
        {
            for (ServiceReference<ConfigurationForm> sr : confFormsRefs)
            {
                ConfigurationForm form
                    = SecurityConfigActivator.bundleContext.getService(sr);
                Object formComponent = form.getForm();

                if (formComponent instanceof Component)
                    addConfigForm(form);
            }
        }
    }

    /**
     * Handles registration of a new configuration form.
     *
     * @param event the <tt>ServiceEvent</tt> that notified us
     */
    public void serviceChanged(ServiceEvent event)
    {
        ServiceReference<?> ref = event.getServiceReference();
        Object property = ref.getProperty(ConfigurationForm.FORM_TYPE);

        if (!ConfigurationForm.SECURITY_TYPE.equals(property))
            return;

        // SecurityConfigActivator registers a ConfigurationForm with FORM_TYPE
        // SECURITY_TYPE so when, SecurityConfigActivator.stop is invoked, an
        // IllegalStateException will be thrown here.
        Object service;

        try
        {
            service = SecurityConfigActivator.bundleContext.getService(ref);
        }
        catch (IllegalStateException ex)
        {
            // SecurityConfigActivator.bundleContext is no longer valid.
            return;
        }

        // we don't care if the source service is not a configuration form
        if (!(service instanceof ConfigurationForm))
            return;

        ConfigurationForm cfgForm = (ConfigurationForm) service;

        if (!cfgForm.isAdvanced())
            return;

        Object formComponent;

        switch (event.getType())
        {
        case ServiceEvent.REGISTERED:
            formComponent = cfgForm.getForm();
            if (formComponent instanceof Component)
                addConfigForm(cfgForm);
            break;

        case ServiceEvent.UNREGISTERING:
            formComponent = cfgForm.getForm();
            if (formComponent instanceof Component)
                remove((Component) formComponent);
            break;
        }
    }

    /**
     * Adds the given form to this configuration panel.
     *
     * @param form the <tt>ConfigurationForm</tt> to add
     */
    private void addConfigForm(ConfigurationForm form)
    {
        int index = form.getIndex();
        String title = form.getTitle();
        Component component = (Component) form.getForm();

        if (index >= getTabCount())
            addTab(title, component);
        else
            insertTab(title, null, component, title, index);
    }
}
