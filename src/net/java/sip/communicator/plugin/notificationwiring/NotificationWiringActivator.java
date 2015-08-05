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
package net.java.sip.communicator.plugin.notificationwiring;

import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.notification.*;
import net.java.sip.communicator.util.*;

import org.jitsi.service.neomedia.*;
import org.jitsi.service.resources.*;
import org.osgi.framework.*;

/**
 * The <tt>NotificationActivator</tt> is the activator of the notification
 * bundle.
 *
 * @author Yana Stamcheva
 */
public class NotificationWiringActivator
    implements BundleActivator
{
    private final Logger logger =
        Logger.getLogger(NotificationWiringActivator.class);

    protected static BundleContext bundleContext;

    private static NotificationService notificationService;
    private static ResourceManagementService resourcesService;
    private static UIService uiService = null;
    private static MediaService mediaService;

    /**
     * The image loader service.
     */
    private static ImageLoaderService<?> imageLoaderService;

    public void start(BundleContext bc) throws Exception
    {
        bundleContext = bc;
        try
        {
            logger.logEntry();
            logger.info("Notification wiring plugin...[  STARTED ]");


            // Get the notification service implementation
            ServiceReference notifReference = bundleContext
                .getServiceReference(NotificationService.class.getName());

            notificationService = (NotificationService) bundleContext
                .getService(notifReference);

            new NotificationManager().init();

            logger.info("Notification wiring plugin ...[REGISTERED]");
        }
        finally
        {
            logger.logExit();
        }
    }

    public void stop(BundleContext bc) throws Exception
    {
        logger.info("Notification handler Service ...[STOPPED]");
    }

    /**
     * Returns the <tt>NotificationService</tt> obtained from the bundle context.
     *
     * @return the <tt>NotificationService</tt> obtained from the bundle context
     */
    public static NotificationService getNotificationService()
    {
        return notificationService;
    }

    /**
     * Returns the <tt>ResourceManagementService</tt>, through which we will
     * access all resources.
     *
     * @return the <tt>ResourceManagementService</tt>, through which we will
     * access all resources.
     */
    public static ResourceManagementService getResources()
    {
        if (resourcesService == null)
        {
            resourcesService
                = ServiceUtils.getService(
                        bundleContext,
                        ResourceManagementService.class);
        }
        return resourcesService;
    }

    /**
     * Returns the current implementation of the <tt>UIService</tt>.
     * @return the current implementation of the <tt>UIService</tt>
     */
    public static UIService getUIService()
    {
        if (uiService == null)
        {
            uiService = ServiceUtils.getService(bundleContext, UIService.class);
        }

        return uiService;
    }

    /**
     * Returns an instance of the <tt>MediaService</tt> obtained from the
     * bundle context.
     * @return an instance of the <tt>MediaService</tt> obtained from the
     * bundle context
     */
    public static MediaService getMediaService()
    {
        if (mediaService == null)
        {
            mediaService
                = ServiceUtils.getService(bundleContext, MediaService.class);
        }
        return mediaService;
    }

    /**
     * Returns an instance of the <tt>ImageLoaderService</tt> obtained from the
     * bundle context.
     * @return an instance of the <tt>ImageLoaderService</tt> obtained from the
     * bundle context
     */
    public static ImageLoaderService<?> getImageLoaderService()
    {
        if (imageLoaderService == null)
        {
            imageLoaderService
                = ServiceUtils.getService(
                        bundleContext,
                        ImageLoaderService.class);
        }
        return imageLoaderService;
    }
}
