/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.call;

import java.awt.event.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.util.skin.*;
import net.java.sip.communicator.util.swing.*;

/**
 * Represents the button, which is used to expand the video in full screen mode.
 *
 * @author Yana Stamcheva
 * @author Adam Netocny
 */
public class FullScreenButton
    extends SIPCommButton
    implements Skinnable
{
    /**
     * Initializes a new <tt>FullScreenButton</tt> instance which is to
     * enter the full screen mode.
     *
     * @param callDialog the parent <tt>CallDialog</tt>, where this button is
     * contained
     */
    public FullScreenButton(final CallDialog callDialog)
    {
        super(  ImageLoader.getImage(ImageLoader.CALL_SETTING_BUTTON_BG),
                ImageLoader.getImage(ImageLoader.ENTER_FULL_SCREEN_BUTTON));

        setToolTipText(GuiActivator.getResources().getI18NString(
            "service.gui.ENTER_FULL_SCREEN_TOOL_TIP"));

        addActionListener(new ActionListener()
        {
            /**
             * Invoked when an action occurs.
             *
             * @param evt the <tt>ActionEvent</tt> instance containing the
             *            data associated with the action and the act of its
             *            performing
             */
            public void actionPerformed(ActionEvent evt)
            {
                callDialog.getCurrentCallRenderer().enterFullScreen();
            }
        });
    }

    /**
     * Reloads icons.
     */
    public void loadSkin()
    {
        setBackgroundImage(ImageLoader.getImage(
                ImageLoader.CALL_SETTING_BUTTON_BG));

        setIconImage(ImageLoader.getImage(
                ImageLoader.ENTER_FULL_SCREEN_BUTTON));
    }
}
