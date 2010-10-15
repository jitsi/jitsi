/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.call;

import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.protocol.*;

/**
 * Represents an UI means to mute the audio stream sent in an associated
 * <tt>Call</tt>.
 *
 * @author Lubomir Marinov
 * @author Yana Stamcheva
 * @author Dmitri Melnikov
 */
public class MuteButton
    extends AbstractCallToggleButton
{
    /**
     * The serialization-related version of the <tt>MuteButton</tt> class
     * explicitly defined to silence a related warning (e.g. in Eclipse IDE)
     * since the <tt>MuteButton</tt> class does not add instance fields.
     */
    private static final long serialVersionUID = 0L;

    /**
     * Initializes a new <tt>MuteButton</tt> instance which is to mute the audio
     * stream to a specific <tt>CallPeer</tt>.
     *
     * @param call the <tt>Call</tt> to be associated with the new instance and
     * to have the audio stream sent to muted
     */
    public MuteButton(Call call)
    {
        this(call, false, false);
    }

    /**
     * Initializes a new <tt>MuteButton</tt> instance which is to mute the audio
     * stream to a specific <tt>Call</tt>.
     *
     * @param call  the <tt>Call</tt> to be associated with the new instance and
     * whose audio stream is to be muted upon performing its action
     * @param fullScreen <tt>true</tt> if the new instance is to be used in
     * full-screen UI; otherwise, <tt>false</tt>
     * @param selected <tt>true</tt> if the new toggle button is to be initially
     * selected; otherwise, <tt>false</tt>
     */
    public MuteButton(Call call, boolean fullScreen, boolean selected)
    {
        super(
                call,
                fullScreen,
                selected,
                ImageLoader.MUTE_BUTTON,
                "service.gui.MUTE_BUTTON_TOOL_TIP");
    }

    /**
     * Mutes or unmutes the associated <tt>Call</tt> upon clicking this button.
     */
    public void buttonPressed()
    {
        if (call != null)
        {
            OperationSetBasicTelephony<?> telephony
                = call.getProtocolProvider().getOperationSet(
                        OperationSetBasicTelephony.class);

            telephony.setMute(call, isSelected());
        }
    }
}
