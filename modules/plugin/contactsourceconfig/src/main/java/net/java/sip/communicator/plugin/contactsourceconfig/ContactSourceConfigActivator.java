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

import java.util.*;

import net.java.sip.communicator.service.gui.*;

import net.java.sip.communicator.util.osgi.*;
import org.jitsi.service.configuration.*;
import org.osgi.framework.*;

/**
 * @author Yana Stamcheva
 */
public class ContactSourceConfigActivator
    extends DependentActivator
{
     /**
     * Indicates if the contact source config form should be disabled, i.e.
     * not visible to the user.
     */
    private static final String DISABLED_PROP
        = "net.java.sip.communicator.plugin.contactsourceconfig.DISABLED";

    /**
     * The {@link BundleContext} of the {@link ContactSourceConfigActivator}.
     */
    public static BundleContext bundleContext;

    public ContactSourceConfigActivator()
    {
        super(ConfigurationService.class);
    }

    /**
     * Starts this plugin.
     * @param bc the BundleContext
     */
    @Override
    public void startWithServices(BundleContext bc)
    {
        bundleContext = bc;

        Dictionary<String, String> properties = new Hashtable<String, String>();

        // Registers the contact source panel as advanced configuration form.
        properties.put( ConfigurationForm.FORM_TYPE,
                        ConfigurationForm.ADVANCED_TYPE);


        ConfigurationService config = getService(ConfigurationService.class);
        // Checks if the context source configuration form is disabled.
        if(!config.getBoolean(DISABLED_PROP, false))
        {
            bundleContext.registerService(
                ConfigurationForm.class.getName(),
                new LazyConfigurationForm(
                    ContactSourceConfigForm.class.getName(),
                    getClass().getClassLoader(),
                    null,
                    "plugin.contactsourceconfig.CONTACT_SOURCE_TITLE",
                    101, true),
                    properties);
        }
    }
}
