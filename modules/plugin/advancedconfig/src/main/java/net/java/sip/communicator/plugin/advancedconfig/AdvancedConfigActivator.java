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
package net.java.sip.communicator.plugin.advancedconfig;

import java.util.*;

import net.java.sip.communicator.service.gui.*;

import net.java.sip.communicator.util.osgi.*;
import org.jitsi.service.configuration.*;
import org.jitsi.service.resources.*;
import org.osgi.framework.*;

import javax.swing.*;

/**
 *
 * @author Yana Stamcheva
 */
public class AdvancedConfigActivator
    extends DependentActivator
{
    /**
     * The logger.
     */
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(AdvancedConfigActivator.class);

    /**
     * The bundle context.
     */
    protected static BundleContext bundleContext;

    /**
     * The resource management service.
     */
    private static ResourceManagementService resourceService;

    /**
     * Indicates if the advanced configuration form should be disabled, i.e.
     * not visible to the user.
     */
    private static final String DISABLED_PROP
        = "net.java.sip.communicator.plugin.advancedconfig.DISABLED";

    /**
     * The advanced configuration panel registered by this bundle.
     */
    private static AdvancedConfigurationPanel panel;

    /**
     * The OSGi service registration of the panel.
     */
    private static ServiceRegistration panelRegistration;

    public AdvancedConfigActivator()
    {
        super(
            ConfigurationService.class,
            ResourceManagementService.class
        );
    }

    /**
     * Starts this bundle.
     * @param bc the bundle context
     * @throws Exception if something goes wrong
     */
    @Override
    public void startWithServices(BundleContext bc)
        throws Exception
    {
        bundleContext = bc;

        // If the notification configuration form is disabled don't continue.
        if (getService(ConfigurationService.class)
            .getBoolean(DISABLED_PROP, false))
        {
            return;
        }

        SwingUtilities.invokeLater(new Runnable()
        {
            public void run()
            {
                final Dictionary<String, String> properties = new Hashtable<String, String>();
                properties.put( ConfigurationForm.FORM_TYPE,
                                ConfigurationForm.GENERAL_TYPE);
                panel = new AdvancedConfigurationPanel();
                bundleContext.addServiceListener(panel);

                // do not block swing thread
                new Thread(new Runnable()
                {
                    public void run()
                    {
                        bundleContext.registerService(
                            ConfigurationForm.class.getName(),
                            panel,
                            properties);
                    }
                }).start();
            }
        });

        if (logger.isInfoEnabled())
            logger.info("ADVANCED CONFIG PLUGIN... [REGISTERED]");
    }

    /**
     * Stops this bundle.
     * @param bc the bundle context
     * @throws Exception if something goes wrong
     */
    @Override
    public void stop(BundleContext bc) throws Exception
    {
        super.stop(bc);
        if(panel != null)
            bc.removeServiceListener(panel);

        if(panelRegistration != null)
            panelRegistration.unregister();
    }

    /**
     * Returns the <tt>ResourceManagementService</tt> implementation.
     * @return the <tt>ResourceManagementService</tt> implementation
     */
    public static ResourceManagementService getResources()
    {
        if (resourceService == null)
            resourceService
                = ServiceUtils.getService(
                bundleContext,
                ResourceManagementService.class);
        return resourceService;
    }
}
