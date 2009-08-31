/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.growlnotification;

import java.util.*;
import org.osgi.framework.*;

import net.java.sip.communicator.service.systray.*;
import net.java.sip.communicator.service.systray.event.*;
import net.java.sip.communicator.util.*;
import org.growl4j.*;

// TO-DO: use a better icon in registration.

/**
 * The Growl Notification Service displays on-screen information such as
 * messages or call received, etc.
 *
 * @author Romain Kuntz
 * @author Egidijus Jankauskas
 */
public class GrowlNotificationServiceImpl
    implements PopupMessageHandler,
                GrowlCallbacksListener
{
    /**
     * The logger for this class.
     */
    private static Logger logger =
        Logger.getLogger(GrowlNotificationServiceImpl.class);
    
    /**
     * A variable that acts as a buffer to temporarily keep all PopupMessages 
     * that were sent to Growl Daemon.
     */
    private static final HashMap<Long, PopupMessage> shownPopups =  
                                        new HashMap<Long, PopupMessage>(10);
    
    private Growl growl = null;


    /** The list of all added popup listeners */
    private final List<SystrayPopupMessageListener> popupMessageListeners =
            new Vector<SystrayPopupMessageListener>();

    
    /**
     * Starts the service. Creates a Growl notifier, and check the current
     * registered protocol providers which supports BasicIM and adds message
     * listener to them.
     *
     * @param bc a currently valid bundle context
     * @throws java.lang.Exception if we fail initializing the growl notifier.
     */
    public void start(BundleContext bc)
    {
        logger.debug("Starting the Growl Notification implementation.");
        
        if(Growl.isGrowlRunning())
        {
            String[] dict = { "Default", "Welcome message" };
            byte[] sipIcon = GrowlNotificationActivator.getResources().
                                    getImageInBytes("service.gui.SIP_COMMUNICATOR_LOGO_45x45");
            growl = new Growl ("SIP Communicator", "net.sip-communicator", sipIcon, dict, dict);
            growl.addClickedNotificationsListener(this);

            growl.notifyGrowlOf("SIP Communicator", 
                                "http://www.sip-communicator.org/", 
                                "Welcome message", 
                                null, null);
        } 
    }

    /**
     * Stops the service.
     *
     * @param bc BundleContext
     */
    public void stop(BundleContext bc)
    {
        if (growl != null)
        {
            growl.doFinalCleanUp();
        }
    }
    
    /**
     * Checks if Growl is present on the system
     * @return <code>true</code> if Growl is installed and <code>false</code> otherwise
     */
    public boolean isGrowlInstalled()
    {
        return Growl.isGrowlInstalled();
    }
    
    /**
     * Checks if Growl is running
     * @return <code>true</code> if Growl is running and <code>false</code> otherwise
     */
    public boolean isGrowlRunning()
    {
        return Growl.isGrowlRunning();
    }

    public void addPopupMessageListener(SystrayPopupMessageListener listener)
    {
        synchronized (popupMessageListeners)
        {
            if (!popupMessageListeners.contains(listener))
                popupMessageListeners.add(listener);
        }
    }

    public void removePopupMessageListener(SystrayPopupMessageListener listener)
    {
        synchronized (popupMessageListeners)
        {
            popupMessageListeners.remove(listener);
        }
    }

    /**
     * Implements <tt>PopupMessageHandler#showPopupMessage()</tt>
     *
     * @param popupMessage the message we will show
     */
    public void showPopupMessage(PopupMessage popupMessage)
    {
        long timestamp = System.currentTimeMillis();
        synchronized(shownPopups) {
            shownPopups.put(timestamp, popupMessage);
        }

        String messageBody = popupMessage.getMessage();
        String messageTitle = popupMessage.getMessageTitle();
        
        // remove eventual HTML code before showing the pop-up message
        messageBody = messageBody.replaceAll("</?\\w++[^>]*+>", "");
        messageTitle = messageTitle.replaceAll("</?\\w++[^>]*+>", "");
        
        growl.notifyGrowlOf(messageTitle, 
                            messageBody, 
                            "Default", 
                            popupMessage.getIcon(), 
                            timestamp);
    }

    /**
     * Notifies all interested listeners that a <tt>SystrayPopupMessageEvent</tt>
     * occured.
     *
     * @param SystrayPopupMessageEvent the evt to send to listener.
     */
    private void firePopupMessageClicked(SystrayPopupMessageEvent evt)
    {
        logger.trace("Will dispatch the following popup event: " + evt);

        List<SystrayPopupMessageListener> listeners;
        synchronized (popupMessageListeners)
        {
            listeners =
                new ArrayList<SystrayPopupMessageListener>(
                popupMessageListeners);
        }

        for (SystrayPopupMessageListener listener : listeners)
            listener.popupMessageClicked(evt);
    }

    /**
     * This method is called by Growl when the Growl notification is not clicked
     * @param context is an object that is used to identify sent notification
     */
    public void growlNotificationTimedOut(Object context)
    {
        PopupMessage m = shownPopups.get(context);
        if (m != null) {
            synchronized(shownPopups) {
                shownPopups.remove(context);
            }
            logger.trace("Growl notification timed-out :" + 
                m.getMessageTitle() + ": " + m.getMessage());
        }
        
    }

    /**
     * This method is called by Growl when the Growl notification is clicked
     * @param context is an object that is used to identify sent notification
     */
    public void growlNotificationWasClicked(Object context)
    {
        PopupMessage m = shownPopups.get(context);
        if (m != null) {
            synchronized(shownPopups) {
                shownPopups.remove(context);
            }
            
            firePopupMessageClicked(new SystrayPopupMessageEvent(this, m.getTag()));
            logger.trace("Growl message clicked :" + 
                m.getMessageTitle() + ": " + m.getMessage());
        }
    }
    
    /**
     * Implements <tt>toString</tt> from <tt>PopupMessageHandler</tt>
     * @return a description of this handler
     */
    public String toString()
    {
        return GrowlNotificationActivator.getResources()
            .getI18NString("impl.growlnotification.POPUP_MESSAGE_HANDLER");
    }

    /**
     * Implements <tt>getPreferenceIndex</tt> from <tt>PopupMessageHandler</tt>. 
     * This handler is able to show images, detect clicks, match a click to a 
     * message, and use a native popup mechanism, thus the index is 4.
     * @return a preference index
     */
    public int getPreferenceIndex()
    {
        return 4;
    }
}
