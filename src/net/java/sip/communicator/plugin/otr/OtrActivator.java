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
package net.java.sip.communicator.plugin.otr;

import java.util.*;

import net.java.sip.communicator.plugin.otr.authdialog.*;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.msghistory.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.resources.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.Logger;

import org.jitsi.service.configuration.*;
import org.jitsi.service.resources.*;
import org.jitsi.util.*;
import org.osgi.framework.*;

/**
 *
 * @author George Politis
 * @author Pawel Domas
 */
public class OtrActivator
    extends AbstractServiceDependentActivator
    implements ServiceListener
{

    /**
     * The {@link BundleContext} of the {@link OtrActivator}.
     */
    public static BundleContext bundleContext;

    /**
     * The {@link ConfigurationService} of the {@link OtrActivator}. Can also be
     * obtained from the {@link OtrActivator#bundleContext} on demand, but we
     * add it here for convenience.
     */
    public static ConfigurationService configService;

    /**
     * The <tt>Logger</tt> used by the <tt>OtrActivator</tt> class and its
     * instances for logging output.
     */
    private static final Logger logger = Logger.getLogger(OtrActivator.class);

    /**
     * Indicates if the security/chat config form should be disabled, i.e.
     * not visible to the user.
     */
    private static final String OTR_CHAT_CONFIG_DISABLED_PROP
        = "net.java.sip.communicator.plugin.otr.otrchatconfig.DISABLED";

    /**
     * A property used in configuration to disable the OTR plugin.
     */
    public static final String OTR_DISABLED_PROP =
        "net.java.sip.communicator.plugin.otr.DISABLED";

    /**
     * A property specifying whether private messaging should be made mandatory.
     */
    public static final String OTR_MANDATORY_PROP =
        "net.java.sip.communicator.plugin.otr.PRIVATE_MESSAGING_MANDATORY";

    /**
     * The {@link ResourceManagementService} of the {@link OtrActivator}. Can
     * also be obtained from the {@link OtrActivator#bundleContext} on demand,
     * but we add it here for convenience.
     */
    public static ResourceManagementService resourceService;

    /**
     * The {@link ScOtrEngine} of the {@link OtrActivator}.
     */
    public static ScOtrEngineImpl scOtrEngine;

    /**
     * The {@link ScOtrKeyManager} of the {@link OtrActivator}.
     */
    public static ScOtrKeyManager scOtrKeyManager = new ScOtrKeyManagerImpl();

    /**
     * The {@link UIService} of the {@link OtrActivator}. Can also be obtained
     * from the {@link OtrActivator#bundleContext} on demand, but we add it here
     * for convenience.
     */
    public static UIService uiService;

    /**
     * The <tt>MetaContactListService</tt> reference.
     */
    private static MetaContactListService metaCListService;
    
    /**
     * The message history service.
     */
    private static MessageHistoryService messageHistoryService;

    /**
     * The {@link OtrContactManager} of the {@link OtrActivator}.
     */
    private static OtrContactManager otrContactManager;
    
    /**
     * Gets an {@link AccountID} by its UID.
     *
     * @param uid The {@link AccountID} UID.
     * @return The {@link AccountID} with the requested UID or null.
     */
    public static AccountID getAccountIDByUID(String uid)
    {
        if (uid == null || uid.length() < 1)
            return null;

        Map<Object, ProtocolProviderFactory> providerFactoriesMap =
            OtrActivator.getProtocolProviderFactories();

        if (providerFactoriesMap == null)
            return null;

        for (ProtocolProviderFactory providerFactory
                : providerFactoriesMap.values())
        {
            for (AccountID accountID : providerFactory.getRegisteredAccounts())
            {
                if (accountID.getAccountUniqueID().equals(uid))
                    return accountID;
            }
        }

        return null;
    }

    /**
     * Gets all the available accounts in SIP Communicator.
     *
     * @return a {@link List} of {@link AccountID}.
     */
    public static List<AccountID> getAllAccountIDs()
    {
        Map<Object, ProtocolProviderFactory> providerFactoriesMap =
            OtrActivator.getProtocolProviderFactories();

        if (providerFactoriesMap == null)
            return null;

        List<AccountID> accountIDs = new Vector<AccountID>();

        for (ProtocolProviderFactory providerFactory
                : providerFactoriesMap.values())
        {
            for (AccountID accountID : providerFactory.getRegisteredAccounts())
                accountIDs.add(accountID);
        }

        return accountIDs;
    }

    private static Map<Object, ProtocolProviderFactory>
        getProtocolProviderFactories()
    {
        Collection<ServiceReference<ProtocolProviderFactory>> serRefs
            = ServiceUtils.getServiceReferences(
                    bundleContext,
                    ProtocolProviderFactory.class);
        Map<Object, ProtocolProviderFactory> providerFactoriesMap
            = new Hashtable<Object, ProtocolProviderFactory>();

        if (!serRefs.isEmpty())
        {
            for (ServiceReference<ProtocolProviderFactory> serRef : serRefs)
            {
                ProtocolProviderFactory providerFactory
                    = bundleContext.getService(serRef);

                providerFactoriesMap.put(
                        serRef.getProperty(ProtocolProviderFactory.PROTOCOL),
                        providerFactory);
            }
        }
        return providerFactoriesMap;
    }

    private OtrTransformLayer otrTransformLayer;

    /**
     * The dependent class. We are waiting for the ui service.
     * @return the ui service class.
     */
    @Override
    public Class<?> getDependentServiceClass()
    {
        return UIService.class;
    }

    private void handleProviderAdded(ProtocolProviderService provider)
    {
        OperationSetInstantMessageTransform opSetMessageTransform
            = provider.getOperationSet(
                    OperationSetInstantMessageTransform.class);

        if (opSetMessageTransform != null)
            opSetMessageTransform.addTransformLayer(this.otrTransformLayer);
        else if (logger.isTraceEnabled())
            logger.trace("Service did not have a transform op. set.");
    }

    private void handleProviderRemoved(ProtocolProviderService provider)
    {
        // check whether the provider has a basic im operation set
        OperationSetInstantMessageTransform opSetMessageTransform
            = provider.getOperationSet(
                    OperationSetInstantMessageTransform.class);

        if (opSetMessageTransform != null)
            opSetMessageTransform.removeTransformLayer(this.otrTransformLayer);
    }

    /*
     * Implements ServiceListener#serviceChanged(ServiceEvent).
     */
    @Override
    public void serviceChanged(ServiceEvent serviceEvent)
    {
        Object sService =
            bundleContext.getService(serviceEvent.getServiceReference());

        if (logger.isTraceEnabled())
        {
            logger.trace(
                    "Received a service event for: "
                        + sService.getClass().getName());
        }

        // we don't care if the source service is not a protocol provider
        if (!(sService instanceof ProtocolProviderService))
            return;

        if (logger.isDebugEnabled())
            logger.debug("Service is a protocol provider.");
        if (serviceEvent.getType() == ServiceEvent.REGISTERED)
        {
            if (logger.isDebugEnabled())
            {
                logger.debug(
                        "Handling registration of a new Protocol Provider.");
            }
            this.handleProviderAdded((ProtocolProviderService) sService);
        }
        else if (serviceEvent.getType() == ServiceEvent.UNREGISTERING)
        {
            this.handleProviderRemoved((ProtocolProviderService) sService);
        }
    }

    /**
     * The bundle context to use.
     * @param context the context to set.
     */
    @Override
    public void setBundleContext(BundleContext context)
    {
        bundleContext = context;
    }

    /*
     * Implements AbstractServiceDependentActivator#start(UIService).
     */
    @Override
    public void start(Object dependentService)
    {
        configService
            = ServiceUtils.getService(
                    bundleContext,
                    ConfigurationService.class);
        // Check whether someone has disabled this plug-in.
        if(configService.getBoolean(OTR_DISABLED_PROP, false))
        {
            configService = null;
            return;
        }

        resourceService
            = ResourceManagementServiceUtils.getService(bundleContext);
        if (resourceService == null)
        {
            configService = null;
            return;
        }

        uiService = (UIService) dependentService;

        // Init static variables, don't proceed without them.
        scOtrEngine = new ScOtrEngineImpl();
        otrContactManager = new OtrContactManager();
        otrTransformLayer = new OtrTransformLayer();

        // Register Transformation Layer
        bundleContext.addServiceListener(this);
        bundleContext.addServiceListener(scOtrEngine);
        bundleContext.addServiceListener(otrContactManager);

        Collection<ServiceReference<ProtocolProviderService>> protocolProviderRefs
            = ServiceUtils.getServiceReferences(
                    bundleContext,
                    ProtocolProviderService.class);

        if (!protocolProviderRefs.isEmpty())
        {
            if (logger.isDebugEnabled())
            {
                logger.debug(
                        "Found " + protocolProviderRefs.size()
                            + " already installed providers.");
            }
            for (ServiceReference<ProtocolProviderService> protocolProviderRef
                    : protocolProviderRefs)
            {
                ProtocolProviderService provider
                    = bundleContext.getService(protocolProviderRef);

                handleProviderAdded(provider);
            }
        }

        if(!OSUtils.IS_ANDROID)
        {
            Hashtable<String, String> containerFilter
                = new Hashtable<String, String>();

            // Register the right-click menu item.
            containerFilter.put(Container.CONTAINER_ID,
                Container.CONTAINER_CONTACT_RIGHT_BUTTON_MENU.getID());

            bundleContext.registerService(
                PluginComponentFactory.class.getName(),
                new OtrPluginComponentFactory(
                        Container.CONTAINER_CONTACT_RIGHT_BUTTON_MENU),
                containerFilter);

            // Register the chat window menu bar item.
            containerFilter.put(Container.CONTAINER_ID,
                                Container.CONTAINER_CHAT_MENU_BAR.getID());

            bundleContext.registerService(
                PluginComponentFactory.class.getName(),
                new OtrPluginComponentFactory(
                        Container.CONTAINER_CHAT_MENU_BAR),
                containerFilter);

            // Register the chat button bar default-action-button.
            containerFilter.put(Container.CONTAINER_ID,
                                Container.CONTAINER_CHAT_TOOL_BAR.getID());

            bundleContext.registerService(
                PluginComponentFactory.class.getName(),
                new OtrPluginComponentFactory(
                        Container.CONTAINER_CHAT_TOOL_BAR),
                containerFilter);

            // Register Swing OTR action handler
            bundleContext.registerService(
                OtrActionHandler.class.getName(),
                new SwingOtrActionHandler(), null);

            containerFilter.put(Container.CONTAINER_ID,
                                Container.CONTAINER_CHAT_WRITE_PANEL.getID());
            bundleContext.registerService(
                PluginComponentFactory.class.getName(),
                new PluginComponentFactory(Container.CONTAINER_CHAT_WRITE_PANEL)
                {
                    @Override
                    protected PluginComponent getPluginInstance()
                    {
                        return
                            new OTRv3OutgoingSessionSwitcher(
                                    getContainer(),
                                    this);
                    }
                },
                containerFilter);
        }

        // If the general configuration form is disabled don't register it.
        if (!configService.getBoolean(OTR_CHAT_CONFIG_DISABLED_PROP, false)
                && !OSUtils.IS_ANDROID)
        {
            Dictionary<String, String> properties
                = new Hashtable<String, String>();

            properties.put( ConfigurationForm.FORM_TYPE,
                            ConfigurationForm.SECURITY_TYPE);
            // Register the configuration form.
            bundleContext.registerService(ConfigurationForm.class.getName(),
                    new LazyConfigurationForm(
                            "net.java.sip.communicator.plugin.otr.authdialog."
                                + "OtrConfigurationPanel",
                            getClass().getClassLoader(),
                            "plugin.otr.configform.ICON",
                            "service.gui.CHAT", 1),
                            properties);
        }
    }

    /*
     * Implements BundleActivator#stop(BundleContext).
     */
    @Override
    public void stop(BundleContext bc) throws Exception
    {
        // Unregister transformation layer.
        // start listening for newly register or removed protocol providers
        bundleContext.removeServiceListener(this);

        if(scOtrEngine != null)
            bundleContext.removeServiceListener(scOtrEngine);

        if(otrContactManager != null)
            bundleContext.removeServiceListener(otrContactManager);

        Collection<ServiceReference<ProtocolProviderService>> protocolProviderRefs
            = ServiceUtils.getServiceReferences(
                    bundleContext,
                    ProtocolProviderService.class);

        if (!protocolProviderRefs.isEmpty())
        {
            // in case we found any
            for (ServiceReference<ProtocolProviderService> protocolProviderRef
                    : protocolProviderRefs)
            {
                ProtocolProviderService provider
                    = bundleContext.getService(protocolProviderRef);

                handleProviderRemoved(provider);
            }
        }
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
     * Gets the service giving access to message history.
     *
     * @return the service giving access to message history.
     */
    public static MessageHistoryService getMessageHistoryService()
    {
        if (messageHistoryService == null)
        {
            messageHistoryService
                = ServiceUtils.getService(
                        bundleContext,
                        MessageHistoryService.class);
        }
        return messageHistoryService;
    }
    
    /**
     * The factory that will be registered in OSGi and will create OTR menu
     * instances.
     */
    private static class OtrPluginComponentFactory
        extends PluginComponentFactory
    {
        OtrPluginComponentFactory(Container c)
        {
            super(c);
        }

        @Override
        protected PluginComponent getPluginInstance()
        {
            Container container = getContainer();
            if(container.equals(Container.CONTAINER_CHAT_TOOL_BAR))
                return new OtrMetaContactButton(container, this);
            else
                return new OtrMetaContactMenu(container, this);
        }
    }
}
