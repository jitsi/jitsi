/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.call;

import java.awt.*;
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
     * Indicates if this buttons is shown in full screen view or normal window.
     */
    private boolean isFullScreen = false;

    /**
     * Initializes a new <tt>FullScreenButton</tt> instance which is to
     * enter the full screen mode.
     *
     * @param callContainer the parent <tt>CallContainer</tt>, where this button
     * is contained
     */
    public FullScreenButton(final CallPanel callContainer,
                            final boolean isFullScreen)
    {
        this.isFullScreen = isFullScreen;

        if (isFullScreen)
            setToolTipText(GuiActivator.getResources().getI18NString(
                "service.gui.EXIT_FULL_SCREEN_TOOL_TIP"));
        else
            setToolTipText(GuiActivator.getResources().getI18NString(
                "service.gui.ENTER_FULL_SCREEN_TOOL_TIP"));

        loadSkin();

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
                if (isFullScreen)
                    callContainer.getCurrentCallRenderer().exitFullScreen();
                else
                    callContainer.getCurrentCallRenderer().enterFullScreen();
            }
        });
    }

    /**
     * Reloads icons.
     */
    public void loadSkin()
    {
        setPreferredSize(new Dimension(44, 38));

        if (isFullScreen)
            setIconImage(ImageLoader.getImage(
                ImageLoader.EXIT_FULL_SCREEN_BUTTON));
        else
            setIconImage(ImageLoader.getImage(
                ImageLoader.ENTER_FULL_SCREEN_BUTTON));
    }
}
