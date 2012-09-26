/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.call;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

import org.jitsi.service.neomedia.*;
import org.jitsi.service.neomedia.device.*;

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
     * Whether video is enabled. If this is <tt>false</tt>, calls to
     * <tt>setEnabled(true)</tt> will NOT enable the button.
     */
    private boolean videoAvailable;

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


        OperationSetVideoTelephony videoTelephony
                = call.getProtocolProvider().getOperationSet(
                OperationSetVideoTelephony.class);

        MediaDevice videoDevice = GuiActivator.getMediaService()
                .getDefaultDevice(MediaType.VIDEO, MediaUseCase.CALL);

        /* Check whether we can send video and set the appropriate tooltip */
        if(videoDevice == null ||
                videoDevice.getDirection().equals(MediaDirection.RECVONLY))
        {
            setToolTipText(GuiActivator.getResources()
                    .getI18NString("service.gui.NO_CAMERA_AVAILABLE"));
            videoAvailable = false;
        }
        else if (videoTelephony == null)
        {
            setToolTipText(GuiActivator.getResources()
                    .getI18NString("service.gui.NO_VIDEO_FOR_PROTOCOL"));
            videoAvailable = false;
        }
        else if(!ConfigurationManager.hasEnabledVideoFormat(
                call.getProtocolProvider()))
        {
            setToolTipText(GuiActivator.getResources()
                    .getI18NString("service.gui.NO_VIDEO_ENCODINGS"));
            videoAvailable = false;
        }
        else
        {
            setToolTipText(GuiActivator.getResources()
                    .getI18NString("service.gui.LOCAL_VIDEO_BUTTON_TOOL_TIP"));
            videoAvailable = true;
        }

        super.setEnabled(videoAvailable);
    }

    /**
     * Enables or disables local video when the button is toggled/untoggled.
     */
    public void buttonPressed()
    {
        CallManager.enableLocalVideo(call,
            !CallManager.isLocalVideoEnabled(call));
    }

    /**
     * Enables/disables the button. If <tt>this.videoAvailable</tt> is false,
     * keeps the button as it is (i.e. disabled).
     *
     * @param enable <tt>true</tt> to enable the button, <tt>false</tt> to
     * disable it.
     */
    @Override
    public void setEnabled(boolean enable)
    {
        if(videoAvailable)
            super.setEnabled(enable);
    }
}
