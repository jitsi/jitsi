/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.swingnotification;

import net.java.sip.communicator.service.systray.*;

/**
 * Empty popup message handler. Used when we want to disable popup messages.
 * @author Damian Minkov
 */
public class NonePopupMessageHandlerImpl
    extends AbstractPopupMessageHandler
{
    /**
     * Implements <tt>PopupMessageHandler#showPopupMessage()</tt>
     * Doing nothing.
     *
     * @param popupMessage the message we will show
     */
    @Override
    public void showPopupMessage(PopupMessage popupMessage)
    {}

    /**
     * Implements <tt>getPreferenceIndex</tt> from
     * <tt>NonePopupMessageHandlerImpl</tt>.
     * This handler is a empty one, thus the preference index is 0.
     * @return a preference index
     */
    @Override
    public int getPreferenceIndex()
    {
        return 0;
    }

    /**
     * Implements <tt>toString</tt> from <tt>PopupMessageHandler</tt>
     * @return a description of this handler
     */
    @Override
    public String toString()
    {
        return SwingNotificationActivator.getResources()
            .getI18NString("service.gui.NONE");
    }
}
