/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.galagonotification;

import java.awt.image.*;
import java.io.*;

import javax.imageio.*;

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

    /**
     * Initializes a new <tt>GalagoPopupMessageHandler</tt> instance with an
     * indicator which determines whether the freedesktop.org Desktop
     * Notifications server has reported that it implements the "icon-static"
     * hint and the new instance has to send it the icons of the
     * <tt>PopupMessage</tt>s for display.
     *
     * @param iconStaticIsImplemented <tt>true</tt> if the freedesktop.org
     * Desktop Notifications server has reported that it implements the
     * "icon-static" hint and the new instance has to send it the icons of the
     * <tt>PopupMessage</tt>s for display; otherwise, <tt>false</tt> and the
     * new instance will not send the icons of the <tt>PopupMessage</tt>s for
     * display
     */
    public GalagoPopupMessageHandler(boolean iconStaticIsImplemented)
    {
        this.iconStaticIsImplemented = iconStaticIsImplemented;
    }

    /**
     * Gets the icon of the specified <tt>PopupMessage</tt> as a
     * <tt>BufferedImage</tt> instance so that it can be sent to the
     * freedesktop.org Desktop Notifications server for display.
     *
     * @param popupMessage the <tt>PopupMessage</tt> to get the icon of
     * @return a <tt>BufferedIImage</tt> instance which represents the icon of
     * <tt>popupMessage</tt> as reported by its {@link PopupMessage#getIcon()};
     * <tt>null</tt> if it did not provide an icon or there was an error during
     * the loading of the icon <tt>byte[]</tt> into a <tt>BufferedImage</tt>
     * instance
     */
    private BufferedImage getIcon(PopupMessage popupMessage)
    {
        byte[] iconBytes = popupMessage.getIcon();
        BufferedImage icon = null;

        if ((iconBytes != null) && (iconBytes.length > 0))
            try
            {
                icon = ImageIO.read(new ByteArrayInputStream(iconBytes));
            }
            catch (IOException ioe)
            {
                logger
                    .error("Failed to create BufferedImage from byte[].", ioe);
            }

        /*
         * TODO If icon is null, we may want to provide another icon. For
         * example, the Swing notifications show the application icon in the
         * case.
         */
        return icon;
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
        int preferenceIndex = 1; // using a native popup mechanism

        if (iconStaticIsImplemented)
            ++preferenceIndex; // showing images
        return preferenceIndex;
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
