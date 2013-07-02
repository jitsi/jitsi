/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.systray;

import net.java.sip.communicator.service.systray.event.*;
import net.java.sip.communicator.util.*;

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
public abstract class AbstractSystrayService
    implements SystrayService
{

    /**
     * The logger
     */
    private final Logger logger
            = Logger.getLogger(AbstractSystrayService.class);

    /**
     * The popup handler currently used to show popup messages
     */
    private PopupMessageHandler activePopupHandler;

    /**
     * A set of usable <tt>PopupMessageHandler</tt>
     */
    private final Hashtable<String, PopupMessageHandler> popupHandlerSet
            = new Hashtable<String, PopupMessageHandler>();

    /**
     * List of listeners from early received calls to addPopupMessageListener.
     * Calls to addPopupMessageListener before the UIService is registered.
     */
    private List<SystrayPopupMessageListener> earlyAddedListeners = null;

    /**
     * Registers given <tt>PopupMessageHandler</tt>.
     * @param handler the <tt>PopupMessageHandler</tt> to be registered.
     */
    protected void addPopupHandler(PopupMessageHandler handler)
    {
        popupHandlerSet.put(handler.getClass().getName(), handler);
    }

    /**
     * Removes given <tt>PopupMessageHandler</tt>.
     * @param handler the <tt>PopupMessageHandler</tt> to be removed.
     */
    protected void removePopupHandler(PopupMessageHandler handler)
    {
        popupHandlerSet.remove(handler.getClass().getName());
    }

    /**
     * Checks if given <tt>handlerClass</tt> is registered as a handler.
     * @param handlerClass the class name to be checked.
     * @return <tt>true</tt> if given <tt>handlerClass</tt> is already
     *         registered as a handler.
     */
    protected boolean containsHandler(String handlerClass)
    {
        return popupHandlerSet.contains(handlerClass);
    }

    /**
     * Returns active <tt>PopupMessageHandler</tt>.
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
            activePopupHandler.showPopupMessage(popupMessage);
    }

    /**
     * Implements the <tt>SystrayService.addPopupMessageListener</tt> method.
     * If <tt>activePopupHandler</tt> is still not available record the listener
     * so we can add him later.
     *
     * @param listener the listener to add
     */
    public void addPopupMessageListener(SystrayPopupMessageListener listener)
    {
        if (activePopupHandler != null)
            activePopupHandler.addPopupMessageListener(listener);
        else
        {
            if(earlyAddedListeners == null)
                earlyAddedListeners =
                        new ArrayList<SystrayPopupMessageListener>();

            earlyAddedListeners.add(listener);
        }
    }

    /**
     * Implements the <tt>SystrayService.removePopupMessageListener</tt> method.
     *
     * @param listener the listener to remove
     */
    public void removePopupMessageListener(SystrayPopupMessageListener listener)
    {
        if (activePopupHandler != null)
            activePopupHandler.removePopupMessageListener(listener);
    }

    /**
     * Set the handler which will be used for popup message
     * @param newHandler the handler to set. providing a null handler is like
     * disabling popup.
     * @return the previously used popup handler
     */
    public PopupMessageHandler setActivePopupMessageHandler(
            PopupMessageHandler newHandler)
    {
        PopupMessageHandler oldHandler = activePopupHandler;

        if (logger.isInfoEnabled())
        {
            logger.info(
                    "setting the following popup handler as active: "
                            + newHandler);
        }
        activePopupHandler = newHandler;
        // if we have received calls to addPopupMessageListener before
        // the UIService is registered we should add those listeners
        if(earlyAddedListeners != null)
        {
            for(SystrayPopupMessageListener l : earlyAddedListeners)
                activePopupHandler.addPopupMessageListener(l);

            earlyAddedListeners.clear();
            earlyAddedListeners = null;
        }

        return oldHandler;
    }

    /**
     * Get the handler currently used by this implementation to popup message
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
        PopupMessageHandler preferredHandler = null;
        int highestPrefIndex = 0;

        if (!popupHandlerSet.isEmpty())
        {
            Enumeration<String> keys = popupHandlerSet.keys();

            while (keys.hasMoreElements())
            {
                String handlerName = keys.nextElement();
                PopupMessageHandler h = popupHandlerSet.get(handlerName);

                if (h.getPreferenceIndex() > highestPrefIndex)
                {
                    highestPrefIndex = h.getPreferenceIndex();
                    preferredHandler = h;
                }
            }
            setActivePopupMessageHandler(preferredHandler);
        }
    }
}
