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
package net.java.sip.communicator.impl.galagonotification;

import net.java.sip.communicator.service.resources.*;
import net.java.sip.communicator.service.systray.*;
import net.java.sip.communicator.util.*;

import org.jitsi.service.resources.*;
import org.osgi.framework.*;

/**
 * Implements <tt>BundleActivator</tt> for the galagonotification bundle which
 * provides an implementation of <tt>PopupMessageHandler</tt> according to the
 * freedesktop.org Desktop Notifications spec.
 *
 * @author Lubomir Marinov
 */
public class GalagoNotificationActivator
    implements BundleActivator
{

    /**
     * The <tt>Logger</tt> used by the <tt>GalagoNotificationActivator</tt>
     * class and its instances for logging output.
     */
    private static final Logger logger
        = Logger.getLogger(GalagoNotificationActivator.class);

    /**
     * The context in which the galagonotification bundle is executing.
     */
    private static BundleContext bundleContext;

    /**
     * The <tt>DBusConnection</tt> pointer to the D-Bus connection through which
     * the freedesktop.org Desktop Notifications are being sent.
     */
    static long dbusConnection;

    /**
     * The resources such as internationalized and localized text and images
     * used by the galagonotification bundle.
     */
    private static ResourceManagementService resources;

    /**
     * Gets the resources such as internationalized and localized text and
     * images used by the galagonotification bundle.
     *
     * @return the resources such as internationalized and localized text and
     * images used by the galagonotification bundle
     */
    public static ResourceManagementService getResources()
    {
        if (resources == null)
            resources = ResourceManagementServiceUtils.getService(bundleContext);
        return resources;
    }

    /**
     * Starts the galagonotification bundle. Registers its
     * <tt>PopupMessageHandler</tt> implementation if it is supported by the
     * current operating system.
     *
     * @param bundleContext the context in which the galagonotification bundle
     * is to execute
     * @throws Exception if the <tt>PopupMessageHandler</tt> implementation of
     * the galagonotification bundle is not supported by the current operating
     * system
     * @see BundleActivator#start(BundleContext)
     */
    public void start(BundleContext bundleContext)
        throws Exception
    {
        long dbusConnection = GalagoNotification.dbus_bus_get_session();

        if (dbusConnection != 0)
        {

            /*
             * We don't much care about the very capabilities because they are
             * optional, we just want to make sure that the service exists.
             */
            String[] capabilities
                = GalagoNotification.getCapabilities(dbusConnection);

            if (capabilities != null)
            {
                if (logger.isDebugEnabled())
                {
                    logger
                        .debug(
                            "org.freedesktop.Notifications.GetCapabilities:");
                    for (String capability : capabilities)
                        if (logger.isDebugEnabled())
                            logger.debug("\t" + capability);
                }

                /*
                 * The native counterpart may return null without throwing an
                 * exception even when it fails to retrieve the capabilities. So
                 * it will not be safe to use galagonotification in this case.
                 * It is also unclear whether the server will return null when
                 * it does not support any optional capabilities. So it's not
                 * totally safe to assume that null means that
                 * galagonotification cannot be used. Anyway, displaying only
                 * the message title may be insufficient for our purposes so
                 * we'll require the optional "body" capability and solve the
                 * above questions.
                 */
                boolean bodyIsImplemented = false;
                boolean iconStaticIsImplemented = false;

                for (String capability : capabilities)
                    if ("body".equals(capability))
                    {
                        bodyIsImplemented = true;
                        if (iconStaticIsImplemented)
                            break;
                    }
                    else if ("icon-static".equals(capability))
                    {
                        iconStaticIsImplemented = true;
                        if (bodyIsImplemented)
                            break;
                    }
                if (bodyIsImplemented)
                {
                    GalagoNotificationActivator.bundleContext = bundleContext;
                    GalagoNotificationActivator.dbusConnection = dbusConnection;

                    bundleContext
                        .registerService(
                            PopupMessageHandler.class.getName(),
                            new GalagoPopupMessageHandler(
                                    iconStaticIsImplemented),
                            null);
                }
                else
                    GalagoNotification.dbus_connection_unref(dbusConnection);
            }
        }
    }

    /**
     * Stops the galagonotification bundle.
     *
     * @param bundleContext the context in which the galagonotification bundle
     * is to stop its execution
     * @throws Exception if there was an error during the stopping of the native
     * functionality supporting the <tt>PopupNotificationHandler</tt>
     * implementation of the galagonotification bundle
     */
    public void stop(BundleContext bundleContext)
        throws Exception
    {
        if (dbusConnection != 0)
        {
            GalagoNotification.dbus_connection_unref(dbusConnection);
            dbusConnection = 0;
        }
        if (bundleContext.equals(GalagoNotificationActivator.bundleContext))
            GalagoNotificationActivator.bundleContext = null;
    }
}
