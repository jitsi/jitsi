/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.call;

import java.awt.event.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.utils.*;

/**
 * The hangup button shown in the call window.
 *
 * @author Yana Stamcheva
 */
public class HangupButton
    extends CallToolBarButton
{
    /**
     * Creates an instance of <tt>HangupButton</tt>, by specifying the parent
     * call panel.
     *
     * @param callPanel the parent call panel
     */
    public HangupButton(final CallPanel callPanel)
    {
        super(  ImageLoader.getImage(ImageLoader.HANGUP_BUTTON_BG),
                GuiActivator.getResources()
                    .getI18NString("service.gui.HANG_UP"));

        addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                callPanel.actionPerformedOnHangupButton(false);
            }
        });
    }
}
