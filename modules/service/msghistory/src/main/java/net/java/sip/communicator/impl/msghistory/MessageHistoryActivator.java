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
package net.java.sip.communicator.impl.msghistory;

import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.history.*;
import net.java.sip.communicator.service.msghistory.*;

import net.java.sip.communicator.util.osgi.*;
import org.jitsi.service.configuration.*;
import org.jitsi.service.resources.*;
import org.osgi.framework.*;

/**
 * Activates the MessageHistoryService
 *
 * @author Damian Minkov
 */
public class MessageHistoryActivator
    extends DependentActivator
{
    /**
     * The <tt>Logger</tt> instance used by the
     * <tt>MessageHistoryActivator</tt> class and its instances for logging
     * output.
     */
    private static org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(MessageHistoryActivator.class);

    /**
     * The <tt>MessageHistoryService</tt> reference.
     */
    private static MessageHistoryServiceImpl msgHistoryService = null;

    /**
     * The <tt>ResourceManagementService</tt> reference.
     */
    private static ResourceManagementService resourcesService;

    /**
     * The <tt>MetaContactListService</tt> reference.
     */
    private static MetaContactListService metaCListService;

    /**
     * The <tt>ConfigurationService</tt> reference.
     */
    private static ConfigurationService configService;

    /**
     * The <tt>BundleContext</tt> of the service.
     */
    static BundleContext bundleContext;

    public MessageHistoryActivator()
    {
        super(
            ResourceManagementService.class,
            MetaContactListService.class,
            ConfigurationService.class,
            HistoryService.class
        );
    }

    /**
     * Initialize and start message history
     *
     * @param bc the BundleContext
     * @throws Exception if initializing and starting message history service
     * fails
     */
    @Override
    public void startWithServices(BundleContext bc) throws Exception
    {
        bundleContext = bc;
        HistoryService historyService = getService(HistoryService.class);

        //Create and start the message history service.
        msgHistoryService =
            new MessageHistoryServiceImpl();

        msgHistoryService.setHistoryService(historyService);

        msgHistoryService.start(bundleContext);

        bundleContext.registerService(
            MessageHistoryService.class.getName(), msgHistoryService, null);

        logger.info("Message History Service ...[REGISTERED]");
    }

    /**
     * Returns the <tt>MetaContactListService</tt> obtained from the bundle
     * context.
     * @return the <tt>MetaContactListService</tt> obtained from the bundle
     * context
     */
    public static MetaContactListService getContactListService()
    {
        if (metaCListService == null)
        {
            metaCListService
                = ServiceUtils.getService(
                        bundleContext,
                        MetaContactListService.class);
        }
        return metaCListService;
    }

    /**
     * Returns the <tt>MessageHistoryService</tt> registered to the bundle
     * context.
     * @return the <tt>MessageHistoryService</tt> registered to the bundle
     * context
     */
    public static MessageHistoryServiceImpl getMessageHistoryService()
    {
        return msgHistoryService;
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
     * Returns the <tt>ConfigurationService</tt> obtained from the bundle
     * context.
     * @return the <tt>ConfigurationService</tt> obtained from the bundle
     * context
     */
    public static ConfigurationService getConfigurationService()
    {
        if(configService == null)
        {
            configService
                = ServiceUtils.getService(
                bundleContext,
                ConfigurationService.class);
        }
        return configService;
    }
}
