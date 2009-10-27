/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.galagonotification;

import java.awt.image.*;

import net.java.sip.communicator.service.systray.*;
import net.java.sip.communicator.util.*;

/**
 * Implements <tt>PopupMessageHandler</tt> according to the freedesktop.org
 * Desktop Notifications spec.
 *
 * @author Lubomir Marinov
 */
public class GalagoPopupMessageHandler
    extends AbstractPopupMessageHandler
{

    /**
     * The <tt>Logger</tt> used by the <tt>GalagoPopupMessageHandler</tt> class
     * and its instances for logging output.
     */
    private static final Logger logger
        = Logger.getLogger(GalagoPopupMessageHandler.class);

    /**
     * The indicator which determines whether the freedesktop.org Desktop
     * Notifications server has reported that it implements the "icon-static"
     * hint and we have to send it the icons of the <tt>PopupMessage</tt>s for
     * display.
     */
    private final boolean iconStaticIsImplemented;

    public GalagoPopupMessageHandler(boolean iconStaticIsImplemented)
    {
        this.iconStaticIsImplemented = iconStaticIsImplemented;
    }

    private BufferedImage getIcon(PopupMessage popupMessge)
    {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * Returns the preference index of this <tt>PopupMessageHandler</tt> which
     * indicates how many features it supports.
     *
     * @return the preference index of this <tt>PopupMessageHandler</tt> which
     * indicates how many features it supports
     * @see PopupMessageHandler#getPreferenceIndex()
     */
    public int getPreferenceIndex()
    {
        return 1; // using a native popup mechanism
    }

    /**
     * Shows the title and the message of the specified <tt>PopupMessage</tt>.
     *
     * @param popupMessage the <tt>PopupMessage</tt> specifying the title and
     * the message to be shown
     * @see PopupMessageHandler#showPopupMessage(PopupMessage)
     */
    public void showPopupMessage(PopupMessage popupMessage)
    {
        try
        {
            GalagoNotification
                .notify(
                    GalagoNotificationActivator.dbusConnection,
                    null,
                    0,
                    iconStaticIsImplemented ? getIcon(popupMessage) : null,
                    popupMessage.getMessageTitle(),
                    popupMessage.getMessage(),
                    -1);
        }
        catch (DBusException dbe)
        {
            logger.error("Failed to show PopupMessage " + popupMessage, dbe);
        }
    }

    /**
     * Gets the human-readable localized description of this
     * <tt>PopupMessageHandler</tt>.
     *
     * @return the human-readable localized description of this
     * <tt>PopupMessageHandler</tt>
     * @see PopupMessageHandler#toString()
     */
    @Override
    public String toString()
    {
        return
            GalagoNotificationActivator
                .getResources()
                    .getI18NString(
                        "impl.galagonotification.POPUP_MESSAGE_HANDLER");
    }
}
