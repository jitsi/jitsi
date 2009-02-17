/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.growlnotification;

import java.lang.reflect.*;
import java.util.*;

import org.osgi.framework.*;

import com.growl.*;

import net.java.sip.communicator.service.systray.*;
import net.java.sip.communicator.service.systray.event.*;
import net.java.sip.communicator.util.*;

/**
 * The Growl Notification Service displays on-screen information such as
 * messages or call received, etc.
 *
 * @author Romain Kuntz
 */
public class GrowlNotificationServiceImpl
    implements PopupMessageHandler
{
    /**
     * The logger for this class.
     */
    private static Logger logger =
        Logger.getLogger(GrowlNotificationServiceImpl.class);

    /**
     * The Growl notifier
     */
    private Growl notifier;

    /**
     * The notifyGrowlOf/setAllowedNotifications/setDefaultNotifications 
     * methods of the growl class. We use reflection to access them
     * in order to avoid compilation errors on non mac platforms.
     */
    private Method notifyMethod = null;
    private Method setAllowedNotifMethod = null;
    private Method setDefaultNotifMethod = null;

    /* All Growl Notifications and the default ones */
    private String [] allNotif =
        new String[] { "SIP Communicator Started",
                       "Protocol events",
                       "Message Received",
                       "Message Sent"};

    private String [] defaultNotif =
        new String[] { "SIP Communicator Started",
                       "Message Received" };

    /** 
     * The path to the SIP Communicator icon used in Growl's configuration 
     * menu and protocol events messages
     */
    private String sipIconPath = "resources/images/logo/sc_logo_128x128.icns";

    /** The list of all added popup listeners */
    private final List<SystrayPopupMessageListener> popupMessageListeners =
            new Vector<SystrayPopupMessageListener>();

    /**
     * starts the service. Creates a Growl notifier, and check the current
     * registerd protocol providers which supports BasicIM and adds message
     * listener to them.
     *
     * @param bc a currently valid bundle context
     * @throws java.lang.Exception if we fail initializing the growl notifier.
     */
    public void start(BundleContext bc)
        throws Exception
    {
        logger.debug("Starting the Growl Notification implementation.");

        /* Register to Growl */
        try
        {
            Constructor<Growl> constructor = Growl.class.getConstructor(
                    new Class[] { String.class, String.class });
            notifier = constructor.newInstance(
                    new Object[]{"SIP Communicator", sipIconPath});

            //init the setAllowedNotifications method
            setAllowedNotifMethod = Growl.class.getMethod(
                    "setAllowedNotifications"
                    , new Class[]{String[].class});

            //init the setDefaultNotifications method
            setDefaultNotifMethod = Growl.class.getMethod(
                    "setDefaultNotifications"
                    , new Class[]{String[].class});

            //init the notifyGrowlOf method
            notifyMethod = Growl.class.getMethod(
                    "notifyGrowlOf"
                    , new Class[]{String.class, String.class, 
                                  String.class, String.class});

            setAllowedNotifications(allNotif);
            setDefaultNotifications(defaultNotif);
            notifier.register();

            notifyGrowlOf("SIP Communicator Started"
                          , sipIconPath
                          , "Welcome to SIP Communicator"
                          , "http://www.sip-communicator.org");
        }
        catch (Exception ex)
        {
            logger.error("Could not send the message to Growl", ex);
            throw ex;
        }

        bc.registerService(PopupMessageHandler.class.getName(), this, null);
    }

    /**
     * stops the service.
     *
     * @param bc BundleContext
     */
    public void stop(BundleContext bc)
    {
    }

    /**
     * Convenience method that defers to notifier.notifyGrowlOf() using
     * reflection without referencing it directly. The purpose of this method
     * is to allow the class to compile on non-mac systems.
     *
     * @param inNotificationName The name of one of the notifications we told
     * growl about.
     * @param inTitle The Title of our Notification as Growl will show it
     * @param inDescription The Description of our Notification as Growl will
     * display it
     *
     * @throws Exception When a notification is not known
     */
    public void notifyGrowlOf(String inNotificationName,
                              String inImagePath,
                              String inTitle,
                              String inDescription)
        throws Exception
    {
        // remove eventual html code before showing the popup message
        inDescription = inDescription.replaceAll("</?\\w++[^>]*+>", "");
        
        notifyMethod.invoke(
            notifier, new Object[]{inNotificationName, inImagePath, 
                                   inTitle, inDescription});
    }
    
    /**
     * Convenience method that defers to notifier.setAllowedNotifications() 
     * using reflection without referencing it directly. The purpose of this 
     * method is to allow the class to compile on non-mac systems.
     *
     * @param inAllNotes The list of allowed Notifications
     */
    public void setAllowedNotifications(String [] inAllNotes)
        throws Exception
    {
        setAllowedNotifMethod.invoke(notifier, new Object[]{inAllNotes});
    }

    /**
     * Convenience method that defers to notifier.setDefaultNotifications() 
     * using reflection without referencing it directly. The purpose of this 
     * method is to allow the class to compile on non-mac systems.
     *
     * @param inDefNotes The list of default Notifications
     */
    public void setDefaultNotifications(String [] inDefNotes)
        throws Exception
    {
        setDefaultNotifMethod.invoke(notifier, new Object[]{inDefNotes});
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
        try
        {
            notifyGrowlOf("Message Received"
                          , sipIconPath
                          , popupMessage.getMessageTitle()
                          , popupMessage.getMessage());
        }
        catch (Exception ex)
        {
            logger.error("Could not notify the received message to Growl", ex);
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
}
