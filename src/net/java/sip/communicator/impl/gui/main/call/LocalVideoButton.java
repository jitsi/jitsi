/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.call;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.neomedia.*;
import net.java.sip.communicator.service.neomedia.device.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

/**
 * The local video button is the button used to start/stop video in a
 * conversation.
 *
 * @author Lubomir Marinov
 */
public class LocalVideoButton
    extends AbstractCallToggleButton
{
    private static final Logger logger
        = Logger.getLogger(LocalVideoButton.class);

    private static final long serialVersionUID = 0L;

    /**
     * Creates a <tt>LocalVideoButton</tt> by specifying the corresponding
     * <tt>call</tt>.
     * @param call the corresponding to this button call
     */
    public LocalVideoButton(Call call)
    {
        this(call, false, false);
    }

    /**
     * Creates a <tt>LocalVideoButton</tt> by specifying the corresponding
     * <tt>call</tt>.
     *
     * @param call  the <tt>Call</tt> to be associated with the new instance and
     * to be put on/off hold upon performing its action
     * @param fullScreen <tt>true</tt> if the new instance is to be used in
     * full-screen UI; otherwise, <tt>false</tt>
     * @param selected <tt>true</tt> if the new toggle button is to be initially
     * selected; otherwise, <tt>false</tt>
     */
    public LocalVideoButton(Call call, boolean fullScreen, boolean selected)
    {
        super(  call,
            fullScreen,
            true,
            selected,
            ImageLoader.LOCAL_VIDEO_BUTTON,
            ImageLoader.LOCAL_VIDEO_BUTTON_PRESSED,
            "service.gui.LOCAL_VIDEO_BUTTON_TOOL_TIP");

        MediaDevice videoDevice
            = GuiActivator.getMediaService().getDefaultDevice(MediaType.VIDEO,
                    MediaUseCase.CALL);
        if (videoDevice == null
            || videoDevice.getDirection().equals(MediaDirection.RECVONLY))
        {
            this.setEnabled(false);
        }
    }

    /**
     * Enables or disables local video when the button is toggled/untoggled.
     */
    public void buttonPressed()
    {
        CallManager.enableLocalVideo(call,
            !CallManager.isLocalVideoEnabled(call));
    }
}
