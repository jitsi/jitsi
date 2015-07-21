/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Copyright @ 2015 Atlassian Pty Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.java.sip.communicator.impl.gui.main.call;

import java.util.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

import org.jitsi.service.neomedia.*;
import org.jitsi.service.neomedia.device.*;

/**
 * Implements a button which starts/stops the streaming of the local video in a
 * <tt>Call</tt> to the remote peer(s).
 *
 * @author Lyubomir Marinov
 */
public class LocalVideoButton
    extends AbstractCallToggleButton
{
    private static final long serialVersionUID = 0L;

    /**
     * The telephony conference to be depicted by this instance.
     */
    private final CallConference callConference;

    /**
     * Initializes a new <tt>LocalVideoButton</tt> instance which is to
     * start/stop the streaming of the local video to the remote peers
     * participating in a specific telephony conference.
     *
     * @param call the <tt>Call</tt> which participates in the telephony
     * conference to start/stop the streaming of the local video to the remote
     * peers in
     */
    public LocalVideoButton(Call call)
    {
        this(call, false);
    }

    /**
     * Initializes a new <tt>LocalVideoButton</tt> instance which is to
     * start/stop the streaming of the local video to the remote peers
     * participating in a specific telephony conference.
     *
     * @param call the <tt>Call</tt> which participates in the telephony
     * conference to start/stop the streaming of the local video to the remote
     * peers in
     * @param selected <tt>true</tt> if the new instance is to be initially
     * selected; otherwise, <tt>false</tt>
     */
    public LocalVideoButton(Call call, boolean selected)
    {
        super(
                null,
                true,
                selected,
                ImageLoader.LOCAL_VIDEO_BUTTON,
                ImageLoader.LOCAL_VIDEO_BUTTON_PRESSED,
                "service.gui.LOCAL_VIDEO_BUTTON_TOOL_TIP");

        this.callConference = call.getConference();
    }

    /**
     * Enables or disables local video when the button is toggled/untoggled.
     */
    @Override
    public void buttonPressed()
    {
        /*
         * CallManager actually enables/disables the local video for the
         * telephony conference associated with the Call so pick up a Call
         * participating in callConference and it should do.
         */
        List<Call> calls = callConference.getCalls();

        if (!calls.isEmpty())
        {
            Call call = calls.get(0);

            CallManager.enableLocalVideo(
                    call,
                    !CallManager.isLocalVideoEnabled(call));
        }
    }

    /**
     * Enables/disables the button. If <tt>this.videoAvailable</tt> is false,
     * keeps the button as it is (i.e. disabled).
     *
     * @param enabled <tt>true</tt> to enable the button, <tt>false</tt> to
     * disable it.
     */
    @Override
    public void setEnabled(boolean enabled)
    {
        /*
         * Regardless of what CallPanel tells us about the enabled state of
         * this LocalVideoButton, we have to analyze the state ourselves because
         * we have to update the tool tip and take into account the global state
         * of the application.
         */

        MediaDevice videoDevice
            = GuiActivator.getMediaService().getDefaultDevice(
                    MediaType.VIDEO,
                    MediaUseCase.CALL);
        String toolTipTextKey;
        boolean videoAvailable;

        /* Check whether we can send video and set the appropriate tool tip. */
        if((videoDevice == null)
                || !videoDevice.getDirection().allowsSending())
        {
            toolTipTextKey = "service.gui.NO_CAMERA_AVAILABLE";
            videoAvailable = false;
        }
        else
        {
            boolean hasVideoTelephony = false;
            boolean hasEnabledVideoFormat = false;

            for (Call conferenceCall : callConference.getCalls())
            {
                ProtocolProviderService protocolProvider
                    = conferenceCall.getProtocolProvider();

                if (!hasVideoTelephony)
                {
                    OperationSetVideoTelephony videoTelephony
                        = protocolProvider.getOperationSet(
                                OperationSetVideoTelephony.class);

                    if (videoTelephony != null)
                        hasVideoTelephony = true;
                }
                if (!hasEnabledVideoFormat
                        && ConfigurationUtils.hasEnabledVideoFormat(
                                protocolProvider))
                {
                    hasEnabledVideoFormat = true;
                }

                if (hasVideoTelephony && hasEnabledVideoFormat)
                    break;
            }

            if (!hasVideoTelephony)
            {
                toolTipTextKey = "service.gui.NO_VIDEO_FOR_PROTOCOL";
                videoAvailable = false;
            }
            else if(!hasEnabledVideoFormat)
            {
                toolTipTextKey = "service.gui.NO_VIDEO_ENCODINGS";
                videoAvailable = false;
            }
            else
            {
                toolTipTextKey = "service.gui.LOCAL_VIDEO_BUTTON_TOOL_TIP";
                videoAvailable = true;
            }
        }
        setToolTipText(
                GuiActivator.getResources().getI18NString(toolTipTextKey));

        super.setEnabled(videoAvailable && enabled);
    }
}
