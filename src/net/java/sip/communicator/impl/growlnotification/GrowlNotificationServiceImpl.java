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
package net.java.sip.communicator.impl.growlnotification;

import net.java.sip.communicator.service.systray.*;
import net.java.sip.communicator.service.systray.event.*;
import net.java.sip.communicator.util.*;

import org.apache.commons.lang3.*;
import org.growl4j.*;
import org.jitsi.service.resources.*;
import org.osgi.framework.*;

/**
 * The Growl Notification Service displays on-screen information such as
 * messages or call received, etc.
 *
 * @author Romain Kuntz
 * @author Egidijus Jankauskas
 * @author Lyubomir Marinov
 */
public class GrowlNotificationServiceImpl
    extends AbstractPopupMessageHandler
    implements GrowlCallbacksListener
{
    /**
     * The <tt>Logger</tt> used by the <tt>GrowlNotificationServiceImpl</tt>
     * class and its instance for logging output.
     */
    private static final Logger logger
        = Logger.getLogger(GrowlNotificationServiceImpl.class);

    /**
     * The notification type (in Growl terms) to be specified to
     * {@link Growl#notifyGrowlOf(String, String, String, byte[], Object)}
     * when called by {@link #showPopupMessage(PopupMessage)}.
     */
    private static final String SHOW_POPUP_MESSAGE_TYPE = "Default";

    /**
     * The <tt>Growl</tt> object.
     */
    private Growl growl;

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

        ResourceManagementService resources
            = GrowlNotificationActivator.getResources();
        byte[] sipIcon
            = resources.getImageInBytes(
                    "service.gui.SIP_COMMUNICATOR_LOGO_45x45");
        String[] dict = { SHOW_POPUP_MESSAGE_TYPE };

        growl
            = new Growl(
                    resources.getSettingsString("service.gui.APPLICATION_NAME"),
                    "net.sip-communicator",
                    sipIcon,
                    dict,
                    dict);
        growl.addClickedNotificationsListener(this);
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
            growl = null;
        }
    }

    /**
     * Implements <tt>PopupMessageHandler#showPopupMessage()</tt>
     *
     * @param popupMessage the message we will show
     */
    public void showPopupMessage(PopupMessage popupMessage)
    {
        String messageBody = popupMessage.getMessage();
        String messageTitle = popupMessage.getMessageTitle();

        // remove eventual HTML code before showing the pop-up message
        messageBody = messageBody.replaceAll("</?\\w++[^>]*+>", "");
        messageTitle = messageTitle.replaceAll("</?\\w++[^>]*+>", "");

        // unescape any chars that can be escaped inside the text
        messageBody = StringEscapeUtils.unescapeHtml4(messageBody);
        messageTitle = StringEscapeUtils.unescapeHtml4(messageTitle);

        growl.notifyGrowlOf(
                messageTitle,
                messageBody,
                SHOW_POPUP_MESSAGE_TYPE,
                popupMessage.getIcon(),
                popupMessage.getTag());
    }

    /**
     * This method is called by Growl when the Growl notification is not clicked
     *
     * @param context an object identifying the notification
     */
    public void growlNotificationTimedOut(Object context)
    {
        if (logger.isTraceEnabled())
            logger.trace("Growl notification timed out: " + context);
    }

    /**
     * This method is called by Growl when the Growl notification is clicked
     *
     * @param context an object identifying the notification
     */
    public void growlNotificationWasClicked(final Object context)
    {
        // release the native thread
        new Thread(new Runnable()
        {
            public void run()
            {
                firePopupMessageClicked(
                    new SystrayPopupMessageEvent(this, context));
                if (logger.isTraceEnabled())
                    logger.trace("Growl notification clicked: " + context);
            }
        }).start();
    }

    /**
     * Implements <tt>toString</tt> from <tt>PopupMessageHandler</tt>.
     *
     * @return a description of this handler
     */
    @Override
    public String toString()
    {
        return
            GrowlNotificationActivator.getResources().getI18NString(
                    "impl.growlnotification.POPUP_MESSAGE_HANDLER");
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
