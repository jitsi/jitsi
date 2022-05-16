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
package net.java.sip.communicator.impl.sparkle;

import lombok.extern.slf4j.*;
import net.java.sip.communicator.util.osgi.*;
import org.jitsi.service.configuration.*;
import org.jitsi.service.resources.*;
import org.osgi.framework.*;

/**
 * Activates the Sparkle Framework
 *
 * @author Romain Kuntz
 */
@Slf4j
public class SparkleActivator
    extends DependentActivator
{
    /**
     * Native method declaration
     *
     * @param pathToSparkleFramework the path to the Sparkle framerok
     * @param updateAtStartup specifies whether Sparkle should be checking for
     * updates on startup.
     * @param checkInterval specifies an interval for the update checks.
     * @param downloadLink a custom download link for sparkle (i.e. the
     * SUFeedURL). If null the default URL will be choosen (the
     * SUFeedURL parameter in the .app/Contents/Info.pList).
     * @param menuItemTitle localized string to be used for the menu item title
     *                      in macosx specific menu.
     */
    public native static void initSparkle(String pathToSparkleFramework,
                                          boolean updateAtStartup,
                                          int checkInterval,
                                          String downloadLink,
                                          String menuItemTitle);

    /**
     * Whether updates are checked at startup
     */
    private final boolean updateAtStartup = true;

    /**
     * Check interval period, in seconds
     */
    private final int checkInterval = 86400;  // 1 day

    /**
     * Internal flag that we use in order to determine whether the native
     * Sparkle libs have already been loaded.
     */
    private static boolean sparkleLibLoaded = false;

    /**
     * Property name for the update link in the configuration file.
     */
    private static final String PROP_UPDATE_LINK =
        "net.java.sip.communicator.UPDATE_LINK";

    public SparkleActivator()
    {
        super(
            ResourceManagementService.class,
            ConfigurationService.class
        );
    }

    /**
     * Initialize and start Sparkle
     *
     * @param bundleContext BundleContext
     * @throws Exception if something goes wrong during sparkle initialization
     */
    @Override
    public void startWithServices(BundleContext bundleContext) throws Exception
    {
        // Dynamically loads JNI object. Will fail if non-MacOSX
        // or when libinit_sparkle.dylib is outside of the LD_LIBRARY_PATH
        try
        {
            if ( ! SparkleActivator.sparkleLibLoaded)
            {
                System.loadLibrary("sparkle_init");
                SparkleActivator.sparkleLibLoaded = true;
            }
        }
        catch(UnsatisfiedLinkError t)
        {
            logger.warn("Couldn't load sparkle library: " + t.getMessage());
            return;
        }

        String downloadLink = getService(ConfigurationService.class)
            .getString(PROP_UPDATE_LINK);

        String title = getService(ResourceManagementService.class)
                .getI18NString("plugin.updatechecker.UPDATE_MENU_ENTRY");

        // add common suffix of this menu title
        if(title != null)
            title += "...";

        // TODO: better way to get the Sparkle Framework path?
        initSparkle(System.getProperty("user.dir")
                    + "/../../Frameworks/Sparkle.framework",
                    updateAtStartup, checkInterval, downloadLink, title);

        if (logger.isInfoEnabled())
            logger.info("Sparkle Plugin ...[Started]");
    }
}
