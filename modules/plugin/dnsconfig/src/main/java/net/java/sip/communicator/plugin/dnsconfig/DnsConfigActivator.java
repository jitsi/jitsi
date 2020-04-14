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
package net.java.sip.communicator.plugin.dnsconfig;

import java.util.*;

import net.java.sip.communicator.service.gui.*;

import net.java.sip.communicator.util.osgi.*;
import org.jitsi.service.configuration.*;
import org.jitsi.service.fileaccess.*;
import org.osgi.framework.*;

/**
 * OSGi bundle activator for the parallel DNS configuration form.
 *
 * @author Ingo Bauersachs
 */
public class DnsConfigActivator
    extends DependentActivator
{
     /**
     * Indicates if the DNS configuration form should be disabled, i.e.
     * not visible to the user.
     */
    private static final String DISABLED_PROP
        = "net.java.sip.communicator.plugin.dnsconfig.DISABLED";

    static BundleContext bundleContext;
    private static FileAccessService fileAccessService;
    private ServiceRegistration configForm = null;

    /**
     * The <tt>ConfigurationService</tt> registered in {@link #bundleContext}
     * and used by the <tt>SecurityConfigActivator</tt> instance to read and
     * write configuration properties.
     */
    private static ConfigurationService configurationService;

    public DnsConfigActivator()
    {
        super(
            ConfigurationService.class,
            FileAccessService.class
        );
    }

    /**
     * Starts this bundle.
     * @param bc the bundle context
     */
    @Override
    public void startWithServices(BundleContext bc)
    {
        bundleContext = bc;
        Dictionary<String, String> properties = new Hashtable<String, String>();
        properties.put(ConfigurationForm.FORM_TYPE,
                       ConfigurationForm.ADVANCED_TYPE);

        ConfigurationService config = getService(ConfigurationService.class);
        // Checks if the dns configuration form is disabled.
        if(!config.getBoolean(DISABLED_PROP, false))
        {
            configForm = bc.registerService(
                ConfigurationForm.class.getName(),
                new LazyConfigurationForm(
                    DnsContainerPanel.class.getName(),
                    getClass().getClassLoader(),
                    "plugin.dnsconfig.ICON",
                    "plugin.dnsconfig.TITLE",
                    2000, true),
                properties);
        }
    }

    /**
     * Stops this bundle.
     * @param bc the bundle context
     * @throws Exception if something goes wrong
     */
    public void stop(BundleContext bc) throws Exception
    {
        super.stop(bc);
        if(configForm != null)
        {
            configForm.unregister();
        }
    }

    /**
     * Returns the <tt>FileAccessService</tt> obtained from the bundle context.
     *
     * @return the <tt>FileAccessService</tt> obtained from the bundle context
     */
    public static FileAccessService getFileAccessService()
    {
        if (fileAccessService == null)
        {
            fileAccessService
                = ServiceUtils.getService(
                        bundleContext,
                        FileAccessService.class);
        }
        return fileAccessService;
    }
}
