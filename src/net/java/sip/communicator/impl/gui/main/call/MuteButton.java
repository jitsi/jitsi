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
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.swing.*;

/**
 * Represents an UI means to mute the audio stream sent to an associated
 * <tt>CallPariticant</tt>.
 *
 * @author Lubomir Marinov
 * @author Yana Stamcheva
 */
public class MuteButton
    extends SIPCommToggleButton
    implements ActionListener
{
    private static final long serialVersionUID = 0L;

    private final Call call;

    /**
     * Initializes a new <tt>MuteButton</tt> instance which is to mute the audio
     * stream to a specific <tt>CallPeer</tt>.
     *
     * @param call the <tt>Call</tt> to be associated with
     *            the new instance and to have the audio stream sent to muted
     */
    public MuteButton(Call call)
    {
        this(call, false, false);
    }

    /**
     * Initializes a new <tt>MuteButton</tt> instance which is to mute the audio
     * stream to a specific <tt>CallPeer</tt>.
     *
     * @param call  the <tt>Call</tt> to be associated with
     *              the new instance and to be put on/off hold upon performing
     *              its action.
     * @param isFullScreenMode indicates if this button will be used in a normal
     *              or full screen mode.
     * @param isSelected indicates the initial state of this toggle button -
     *              selected or not.
     */
    public MuteButton(Call call, boolean isFullScreenMode, boolean isSelected)
    {
        this.call = call;

        if (isFullScreenMode)
        {
            this.setBgImage(
                ImageLoader.getImage(ImageLoader.FULL_SCREEN_BUTTON_BG));
            this.setBgRolloverImage(
                ImageLoader.getImage(ImageLoader.FULL_SCREEN_BUTTON_BG));
            this.setIconImage(
                ImageLoader.getImage(ImageLoader.MUTE_BUTTON));
            this.setPressedImage(
                ImageLoader.getImage(ImageLoader.FULL_SCREEN_BUTTON_BG_PRESSED));
        }
        else
        {
            this.setBgImage(
                ImageLoader.getImage(ImageLoader.CALL_SETTING_BUTTON_BG));
            this.setBgRolloverImage(
                ImageLoader.getImage(ImageLoader.CALL_SETTING_BUTTON_BG));
            this.setIconImage(
                ImageLoader.getImage(ImageLoader.MUTE_BUTTON));
            this.setPressedImage(
                ImageLoader.getImage(ImageLoader.CALL_SETTING_BUTTON_PRESSED_BG));
        }

        this.addActionListener(this);
        setToolTipText(GuiActivator.getResources().getI18NString(
            "service.gui.MUTE_BUTTON_TOOL_TIP"));
        setSelected(isSelected);
    }

    /**
     * Mutes or unmutes call peers when the mute button is clicked.
     * @param evt the <tt>ActionEvent</tt> that notified us of the action
     */
    public void actionPerformed(ActionEvent evt)
    {
        if (call != null)
        {
            OperationSetBasicTelephony telephony
                = call.getProtocolProvider()
                    .getOperationSet(OperationSetBasicTelephony.class);

            // Obtain the isSelected property before invoking setMute.
            boolean isMuteSelected = isSelected();
            telephony.setMute(call, isMuteSelected);
        }
    }
}
