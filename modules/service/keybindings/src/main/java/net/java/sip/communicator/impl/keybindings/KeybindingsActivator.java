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
package net.java.sip.communicator.impl.keybindings;

import net.java.sip.communicator.service.keybindings.*;

import net.java.sip.communicator.util.osgi.*;
import org.jitsi.service.configuration.*;
import org.jitsi.service.resources.*;
import org.osgi.framework.*;

/**
 * Enabling and disabling osgi functionality for keybindings.
 *
 * @author Damian Johnson
 */
public class KeybindingsActivator
    extends DependentActivator
{
    /**
     * The <tt>Logger</tt> instance used by the
     * <tt>KeybindingsActivator</tt> class and its instances for logging
     * output.
     */
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(KeybindingsActivator.class);

    /**
     * The <tt>KeybindingsService</tt> reference.
     */
    private KeybindingsServiceImpl keybindingsService = null;

    /**
     * Reference to the configuration service
     */
    private static ConfigurationService configService = null;

    /**
     * The resource service.
     */
    private static ResourceManagementService resourceService = null;

    public KeybindingsActivator()
    {
        super(
            ResourceManagementService.class,
            ConfigurationService.class
        );
    }

    /**
     * Called when this bundle is started.
     *
     * @param context The execution context of the bundle being started.
     */
    @Override
    public void startWithServices(BundleContext context)
    {
        configService = getService(ConfigurationService.class);
        resourceService = getService(ResourceManagementService.class);

        if (logger.isDebugEnabled())
            logger.debug("Service Impl: " + getClass().getName()
            + " [  STARTED ]");
        this.keybindingsService = new KeybindingsServiceImpl();
        this.keybindingsService.start(context);
        context.registerService(KeybindingsService.class.getName(),
            this.keybindingsService, null);
    }

    /**
     * Called when this bundle is stopped so the Framework can perform the
     * bundle-specific activities necessary to stop the bundle.
     *
     * @param context The execution context of the bundle being stopped.
     */
    public void stop(BundleContext context) throws Exception
    {
        super.stop(context);
        if (this.keybindingsService != null)
        {
            this.keybindingsService.stop();
            this.keybindingsService = null;
        }
    }

    /**
     * Returns a reference to a ConfigurationService implementation currently
     * registered in the bundle context or null if no such implementation was
     * found.
     *
     * @return a currently valid implementation of the ConfigurationService.
     */
    public static ConfigurationService getConfigService()
    {
        return configService;
    }

    /**
     * Returns the <tt>ResourceManagementService</tt> obtained from the
     * bundle context.
     *
     * @return the <tt>ResourceManagementService</tt> obtained from the
     * bundle context
     */
    public static ResourceManagementService getResourceService()
    {
        return resourceService;
    }
}
