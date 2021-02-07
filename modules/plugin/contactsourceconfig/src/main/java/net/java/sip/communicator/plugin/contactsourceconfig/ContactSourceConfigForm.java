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
package net.java.sip.communicator.plugin.contactsourceconfig;

import java.awt.*;
import java.util.Collection;

import javax.swing.*;

import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.gui.*;

import org.osgi.framework.*;

/**
 *
 * @author Yana Stamcheva
 */
public class ContactSourceConfigForm
    extends TransparentPanel
    implements ServiceListener
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * The drop down list of contact sources.
     */
    private final JTabbedPane sourceTabs = new JTabbedPane();

    /**
     * OSGi filter for contact source config form services.
     */
    private static final String CONTACT_SOURCE_FORM_FILTER = "("
        + ConfigurationForm.FORM_TYPE
        + "=" + ConfigurationForm.CONTACT_SOURCE_TYPE + ")";

    /**
     * Creates the <tt>ContactSourceConfigForm</tt>.
     * @throws InvalidSyntaxException
     */
    public ContactSourceConfigForm() throws InvalidSyntaxException
    {
        // get all already running config form services
        Collection<ServiceReference<ConfigurationForm>> confFormsRefs =
            ContactSourceConfigActivator.bundleContext
                .getServiceReferences(ConfigurationForm.class,
                    CONTACT_SOURCE_FORM_FILTER);
        for (ServiceReference<ConfigurationForm> ref : confFormsRefs)
        {
            ConfigurationForm form = ContactSourceConfigActivator
                    .bundleContext.getService(ref);

            Object formComponent = form.getForm();
            if (formComponent instanceof Component)
                sourceTabs.add(form.getTitle(), (Component)formComponent);
        }

        // add those services to the tab panel
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        add(sourceTabs);

        // then listen for all future service changes
        ContactSourceConfigActivator.bundleContext.addServiceListener(this,
            CONTACT_SOURCE_FORM_FILTER);
    }

    /**
     * Handles registration of a new configuration form.
     * @param event the <tt>ServiceEvent</tt> that notified us
     */
    public void serviceChanged(ServiceEvent event)
    {
        ServiceReference<?> serviceRef = event.getServiceReference();

        Object sService
            = ContactSourceConfigActivator.bundleContext
                .getService(serviceRef);

        ConfigurationForm configForm = (ConfigurationForm) sService;

        if (!configForm.isAdvanced())
            return;

        Object formComponent;
        switch (event.getType())
        {
        case ServiceEvent.REGISTERED:
            formComponent = configForm.getForm();
            if (formComponent instanceof Component)
                sourceTabs.add(configForm.getTitle(), (Component)configForm);
            break;

        case ServiceEvent.UNREGISTERING:
            formComponent = configForm.getForm();
            if (formComponent instanceof Component)
                sourceTabs.remove((Component) formComponent);
            break;
        }
    }
}
