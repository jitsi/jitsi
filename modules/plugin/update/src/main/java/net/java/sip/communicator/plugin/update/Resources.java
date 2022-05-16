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
package net.java.sip.communicator.plugin.update;

import java.io.*;
import java.util.*;
import net.java.sip.communicator.util.osgi.*;
import org.jitsi.service.resources.*;

/**
 * Implements methods to facilitate dealing with resources in the update
 * plug-in.
 *
 * @author Yana Stamcheva
 * @author Lyubomir Marinov
 */
public class Resources
{
    /**
     * The <tt>Logger</tt> used by the <tt>Resources</tt> class for logging
     * output.
     */
    private static org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Resources.class);

    /**
     * The name of the configuration file of the update plug-in.
     */
    private static final String UPDATE_CONFIGURATION_FILE
        = "update-location.properties";

    /**
     * The <tt>ResourceManagementService</tt> registered in the
     * <tt>BundleContext</tt> of the update plug-in.
     */
    private static ResourceManagementService resources;

    /**
     * The properties found in the configuration file of the update plug-in.
     */
    private static Properties updateConfiguration;

    /**
     * Gets the <tt>ResourceManagementService</tt> registered in the
     * <tt>BundleContext</tt> of the update plug-in.
     *
     * @return the <tt>ResourceManagementService</tt> (if any) registered in the
     * <tt>BundleContext</tt> of the update plug-in
     */
    public static ResourceManagementService getResources()
    {
        if (resources == null)
        {
            resources
                = ServiceUtils.getService(
                        UpdateActivator.bundleContext,
                        ResourceManagementService.class);
        }
        return resources;
    }

    /**
     * Gets a <tt>String</tt> value associated with a specific key in the
     * configuration file of the update plug-in.
     *
     * @param key the key to get the associated <tt>String</tt> value of
     * @return the <tt>String</tt> value (if any) associated with the specified
     * <tt>key</tt> in the configuration file of the update plug-in
     */
    public static String getUpdateConfigurationString(String key)
    {
        if (updateConfiguration == null)
        {
            updateConfiguration = new Properties();

            File configFile = new File(UPDATE_CONFIGURATION_FILE);
            try (InputStream is = configFile.exists()
                ? new FileInputStream(configFile)
                : ClassLoader
                    .getSystemResourceAsStream(UPDATE_CONFIGURATION_FILE)
            )
            {
                if (is != null)
                {
                    updateConfiguration.load(is);
                }
                else
                {
                    logger.info(
                        "No configuration file specified for the update plug-in");
                }
            }
            catch (IOException ioe)
            {
                logger.error(
                    "Could not load the configuration file of the update plug-in",
                    ioe);
            }
        }

        return updateConfiguration.getProperty(key);
    }
}
