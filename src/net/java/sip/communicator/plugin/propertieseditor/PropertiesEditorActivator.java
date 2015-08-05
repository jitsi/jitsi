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
package net.java.sip.communicator.plugin.propertieseditor;

import org.jitsi.service.configuration.*;
import org.jitsi.service.resources.ResourceManagementService;

import java.util.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.util.*;
import org.osgi.framework.*;

/**
 * The <tt>BundleActivator</tt> of the PropertiesEditor plugin.
 *
 * @author Marin Dzhigarov
 * @author Pawel Domas
 */
public class PropertiesEditorActivator 
    implements BundleActivator 
{
    /**
     * The bundle context.
     */
    private static BundleContext bundleContext;

    /**
     * The configuration service.
     */
    private static ConfigurationService configService;

    /**
     * The resource management service.
     */
    private static ResourceManagementService resourceManagementService;

    /**
     * The ui service
     */
    private static UIService uiService;

    /**
     * Returns the <tt>ConfigurationService</tt> obtained from the
     * <tt>BundleContext</tt>.
     *
     * @return the <tt>ConfigurationService</tt> obtained from the
     * <tt>BundleContext</tt>.
     */
    public static ConfigurationService getConfigurationService() 
    {
        if (configService == null) 
        {
            configService
                = ServiceUtils.getService(
                        bundleContext,
                        ConfigurationService.class);
        }
        return configService;
    }

    /**
     * Returns the <tt>ResourceManagementService</tt> obtained from the
     * <tt>BundleContext</tt>.
     *
     * @return the <tt>ResourceManagementService</tt> obtained from the
     * <tt>BundleContext</tt>.
     */
    public static ResourceManagementService getResourceManagementService()
    {
        if (resourceManagementService == null)
        {
            resourceManagementService
                = ServiceUtils.getService(
                        bundleContext,
                        ResourceManagementService.class);
        }
        return resourceManagementService;
    }

    /**
     * Returns the <tt>UIService</tt> obtained from the <tt>BundleContext</tt>.
     *
     * @return the <tt>UIService</tt> obtained from the <tt>BundleContext</tt>.
     */
    public static UIService getUIService() 
    {
        if (uiService == null)
            uiService = ServiceUtils.getService(bundleContext, UIService.class);
        return uiService;
    }

    /**
     * Starts this bundle and adds the <td>PropertiesEditorPanel</tt> contained
     * in it to the configuration window obtained from the <tt>UIService</tt>.
     *
     * @param bc the <tt>BundleContext</tt>
     * @throws Exception if one of the operation executed in the start method
     * fails
     */
    public void start(BundleContext bc) throws Exception 
    {
        bundleContext = bc;

        Dictionary<String, String> properties = new Hashtable<String, String>();

        properties.put(
                ConfigurationForm.FORM_TYPE,
                ConfigurationForm.ADVANCED_TYPE);
        bundleContext.registerService(
                ConfigurationForm.class.getName(),
                new LazyConfigurationForm(
                        StartingPanel.class.getName(),
                        getClass().getClassLoader(),
                        "",
                        "plugin.propertieseditor.TITLE",
                        1002, true),
                properties);
    }

    /**
     * Stops this bundle.
     *
     * @param bc the <tt>BundleContext</tt>
     * @throws Exception if one of the operation executed in the stop method
     * fails
     */
    public void stop(BundleContext bc) throws Exception {}
}
