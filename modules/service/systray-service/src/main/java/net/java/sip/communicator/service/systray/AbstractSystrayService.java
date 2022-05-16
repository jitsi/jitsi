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
package net.java.sip.communicator.service.systray;

import lombok.extern.slf4j.*;
import net.java.sip.communicator.service.systray.event.*;
import net.java.sip.communicator.util.osgi.*;
import org.jitsi.service.configuration.*;
import org.osgi.framework.*;

import java.util.*;

/**
 * Base implementation of {@link SystrayService}. Manages
 * <tt>PopupMessageHandler</tt>s and <tt>SystrayPopupMessageListener</tt>s.
 *
 * @author Nicolas Chamouard
 * @author Yana Stamcheva
 * @author Lyubomir Marinov
 * @author Symphorien Wanko
 * @author Pawel Domas
 */
@Slf4j
public abstract class AbstractSystrayService
    implements SystrayService, ServiceListener
{
    /**
     * OSGI bundle context
     */
    protected final BundleContext bundleContext;

    /**
     * The popup handler currently used to show popup messages
     */
    private PopupMessageHandler activePopupHandler;

    /**
     * A set of usable <tt>PopupMessageHandler</tt>
     */
    private final Map<ServiceReference<PopupMessageHandler>, PopupMessageHandler>
        popupHandlerSet = new HashMap<>();

    /**
     * List of listeners from early received calls to addPopupMessageListener.
     * Calls to addPopupMessageListener before the UIService is registered.
     */
    private final List<SystrayPopupMessageListener> earlyAddedListeners
        = new ArrayList<>();

    private final ConfigurationService configService;

    /**
     * Creates new instance of <tt>AbstractSystrayService</tt>.
     */
    public AbstractSystrayService(BundleContext bundleContext,
        ConfigurationService configService)
    {
        this.bundleContext = bundleContext;
        this.configService = configService;
    }

    /**
     * Registers given <tt>PopupMessageHandler</tt>.
     *
     * @param handler the <tt>PopupMessageHandler</tt> to be registered.
     * @param ref     OSGi ServiceReference of the <tt>PopupMessageHandler</tt>
     *                to be registered.
     */
    protected boolean addPopupHandler(PopupMessageHandler handler,
        ServiceReference<PopupMessageHandler> ref)
    {
        if (!popupHandlerSet.containsValue(handler))
        {
            popupHandlerSet.put(ref, handler);
            logger.info("adding popup handler {}", handler);
            return true;
        }

        logger.warn(
            "the following popup handler has not been added since it is already known: {}",
            handler);
        return false;
    }

    /**
     * Removes given <tt>PopupMessageHandler</tt>.
     *
     * @param ref the <tt>PopupMessageHandler</tt> to be removed.
     */
    protected PopupMessageHandler removePopupHandler(
        ServiceReference<PopupMessageHandler> ref)
    {
        PopupMessageHandler handler = popupHandlerSet.get(ref);
        popupHandlerSet.remove(ref);
        return handler;
    }

    /**
     * Checks if given <tt>handlerClass</tt> is registered as a handler.
     *
     * @param handler the class name to be checked.
     * @return <tt>true</tt> if given <tt>handlerClass</tt> is already
     * registered as a handler.
     */
    protected boolean containsHandler(PopupMessageHandler handler)
    {
        return popupHandlerSet.containsValue(handler);
    }

    /**
     * Returns active <tt>PopupMessageHandler</tt>.
     *
     * @return active <tt>PopupMessageHandler</tt>.
     */
    protected PopupMessageHandler getActivePopupHandler()
    {
        return activePopupHandler;
    }

    /**
     * Implements <tt>SystraService#showPopupMessage()</tt>
     *
     * @param popupMessage the message we will show
     */
    public void showPopupMessage(PopupMessage popupMessage)
    {
        // since popup handler could be loaded and unloader on the fly,
        // we have to check if we currently have a valid one.
        if (activePopupHandler != null)
        {
            activePopupHandler.showPopupMessage(popupMessage);
        }
    }

    /**
     * Stub method that does nothing.
     *
     * @param count ignored
     */
    @Override
    public void setNotificationCount(int count)
    {
    }

    /**
     * Implements the <tt>SystrayService.addPopupMessageListener</tt> method. If
     * <tt>activePopupHandler</tt> is still not available record the listener so
     * we can add him later.
     *
     * @param listener the listener to add
     */
    public void addPopupMessageListener(SystrayPopupMessageListener listener)
    {
        if (activePopupHandler != null)
        {
            activePopupHandler.addPopupMessageListener(listener);
        }
        else
        {
            earlyAddedListeners.add(listener);
        }
    }

    /**
     * Implements the <tt>SystrayService.removePopupMessageListener</tt>
     * method.
     *
     * @param listener the listener to remove
     */
    public void removePopupMessageListener(SystrayPopupMessageListener listener)
    {
        if (activePopupHandler != null)
        {
            activePopupHandler.removePopupMessageListener(listener);
        }
    }

    /**
     * Set the handler which will be used for popup message
     *
     * @param newHandler the handler to set. providing a null handler is like
     *                   disabling popup.
     * @return the previously used popup handler
     */
    public PopupMessageHandler setActivePopupMessageHandler(
        PopupMessageHandler newHandler)
    {
        PopupMessageHandler oldHandler = activePopupHandler;

        logger.info("setting the following popup handler as active: {}",
            newHandler);

        activePopupHandler = newHandler;
        // if we have received calls to addPopupMessageListener before
        // the UIService is registered we should add those listeners
        if (earlyAddedListeners != null)
        {
            for (SystrayPopupMessageListener l : earlyAddedListeners)
            {
                activePopupHandler.addPopupMessageListener(l);
            }

            earlyAddedListeners.clear();
        }

        return oldHandler;
    }

    /**
     * Get the handler currently used by this implementation to popup message
     *
     * @return the current handler
     */
    public PopupMessageHandler getActivePopupMessageHandler()
    {
        return activePopupHandler;
    }

    /**
     * Sets activePopupHandler to be the one with the highest preference index.
     */
    public void selectBestPopupMessageHandler()
    {
        popupHandlerSet.values().stream()
            .max(Comparator
                .comparingInt(PopupMessageHandler::getPreferenceIndex))
            .ifPresent(this::setActivePopupMessageHandler);
    }

    /**
     * Initializes popup handler by searching registered services for class
     * <tt>PopupMessageHandler</tt>.
     */
    protected void initHandlers()
    {
        // Listens for new popup handlers
        try
        {
            bundleContext.addServiceListener(
                this,
                "(objectclass=" + PopupMessageHandler.class.getName()
                    + ")");
        }
        catch (Exception e)
        {
            logger.warn("could add service listeners", e);
        }

        // now we look if some handler has been registered before we start
        // to listen
        Collection<ServiceReference<PopupMessageHandler>> handlerRefs
            = ServiceUtils.getServiceReferences(
            bundleContext,
            PopupMessageHandler.class);

        if (!handlerRefs.isEmpty())
        {
            String configuredHandler
                = configService.getString("systray.POPUP_HANDLER");

            for (ServiceReference<PopupMessageHandler> handlerRef : handlerRefs)
            {
                PopupMessageHandler handler
                    = bundleContext.getService(handlerRef);
                if (addPopupHandler(handler, handlerRef))
                {
                    logger
                        .info("added the following popup handler: {}", handler);
                    if (handler.getClass().getName().equals(configuredHandler))
                    {
                        setActivePopupMessageHandler(handler);
                    }
                }
            }

            if (configuredHandler == null)
            {
                selectBestPopupMessageHandler();
            }
        }
    }

    public void serviceChanged(ServiceEvent serviceEvent)
    {
        try
        {
            ServiceReference<PopupMessageHandler> ref
                = (ServiceReference<PopupMessageHandler>) serviceEvent
                .getServiceReference();
            if (serviceEvent.getType() == ServiceEvent.REGISTERED)
            {
                PopupMessageHandler handler = bundleContext.getService(ref);
                addPopupHandler(handler, ref);

                String configuredHandler
                    = configService.getString("systray.POPUP_HANDLER");

                if ((configuredHandler == null)
                    && ((getActivePopupHandler() == null)
                    || (handler.getPreferenceIndex()
                    > getActivePopupHandler().getPreferenceIndex())))
                {
                    // The user doesn't have a preferred handler set and new
                    // handler with better preference index has arrived,
                    // thus setting it as active.
                    setActivePopupMessageHandler(handler);
                }
                if ((configuredHandler != null)
                    && configuredHandler.equals(
                    handler.getClass().getName()))
                {
                    // The user has a preferred handler set and it just
                    // became available, thus setting it as active
                    setActivePopupMessageHandler(handler);
                }
            }
            else if (serviceEvent.getType() == ServiceEvent.UNREGISTERING)
            {
                PopupMessageHandler handler = removePopupHandler(ref);
                PopupMessageHandler activeHandler = getActivePopupHandler();
                if (activeHandler == handler)
                {
                    setActivePopupMessageHandler(null);

                    // We just lost our default handler, so we replace it
                    // with the one that has the highest preference index.
                    selectBestPopupMessageHandler();
                }
            }
        }
        catch (IllegalStateException e)
        {
            logger.debug("could not handle {} service changed event",
                serviceEvent.getType(), e);
        }
    }
}
