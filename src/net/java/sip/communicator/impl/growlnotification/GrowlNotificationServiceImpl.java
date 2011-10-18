/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.growlnotification;

import java.util.*;

import net.java.sip.communicator.service.systray.*;
import net.java.sip.communicator.service.systray.event.*;
import net.java.sip.communicator.util.*;

import org.growl4j.*;
import org.osgi.framework.*;

// TODO Use a better icon in registration.

/**
 * The Growl Notification Service displays on-screen information such as
 * messages or call received, etc.
 *
 * @author Romain Kuntz
 * @author Egidijus Jankauskas
 */
public class GrowlNotificationServiceImpl
    extends AbstractPopupMessageHandler
    implements GrowlCallbacksListener
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

    /**
     * The <tt>Growl</tt> object.
     */
    private Growl growl = null;

    /**
     * Starts the service. Creates a Growl notifier, and check the current
     * registered protocol providers which supports BasicIM and adds message
     * listener to them.
     *
     * @param bc a currently valid bundle context
     */
    public void start(BundleContext bc)
    {
        if (logger.isDebugEnabled())
            logger.debug("Starting the Growl Notification implementation.");

        if(Growl.isGrowlRunning())
        {
            String[] dict = { "Default", "Welcome message" };
            byte[] sipIcon =
                GrowlNotificationActivator.getResources().
                    getImageInBytes("service.gui.SIP_COMMUNICATOR_LOGO_45x45");
            growl = new Growl (
                GrowlNotificationActivator.getResources()
                    .getSettingsString("service.gui.APPLICATION_NAME"),
                "net.sip-communicator",
                sipIcon,
                dict,
                dict);
            growl.addClickedNotificationsListener(this);

            growl.notifyGrowlOf(
                GrowlNotificationActivator.getResources()
                    .getSettingsString("service.gui.APPLICATION_NAME"),
                GrowlNotificationActivator.getResources()
                    .getSettingsString("service.gui.APPLICATION_WEB_SITE"),
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
     * @return <tt>true</tt> if Growl is installed and <tt>false</tt>otherwise
     */
    public boolean isGrowlInstalled()
    {
        return Growl.isGrowlInstalled();
    }

    /**
     * Checks if Growl is running
     *
     * @return <tt>true</tt> if Growl is running and <tt>false</tt> otherwise
     */
    public boolean isGrowlRunning()
    {
        return Growl.isGrowlRunning();
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
     * This method is called by Growl when the Growl notification is not clicked
     *
     * @param context is an object that is used to identify sent notification
     */
    public void growlNotificationTimedOut(Object context)
    {
        PopupMessage m = shownPopups.get(context);
        if (m != null) {
            synchronized(shownPopups) {
                shownPopups.remove(context);
            }
            if (logger.isTraceEnabled())
                logger.trace("Growl notification timed-out :" +
                m.getMessageTitle() + ": " + m.getMessage());
        }
    }

    /**
     * This method is called by Growl when the Growl notification is clicked
     *
     * @param context is an object that is used to identify sent notification
     */
    public void growlNotificationWasClicked(Object context)
    {
        PopupMessage m = shownPopups.get(context);
        if (m != null) {
            synchronized(shownPopups) {
                shownPopups.remove(context);
            }

            firePopupMessageClicked(new SystrayPopupMessageEvent(this,
                    m.getTag()));
            if (logger.isTraceEnabled())
                logger.trace("Growl message clicked :" +
                m.getMessageTitle() + ": " + m.getMessage());
        }
    }

    /**
     * Implements <tt>toString</tt> from <tt>PopupMessageHandler</tt>.
     *
     * @return a description of this handler
     */
    @Override
    public String toString()
    {
        return GrowlNotificationActivator.getResources()
            .getI18NString("impl.growlnotification.POPUP_MESSAGE_HANDLER");
    }

    /**
     * Implements <tt>getPreferenceIndex</tt> from <tt>PopupMessageHandler</tt>.
     * This handler is able to show images, detect clicks, match a click to a
     * message, and use a native popup mechanism, thus the index is 4.
     *
     * @return a preference index
     */
    public int getPreferenceIndex()
    {
        return 4;
    }
}
