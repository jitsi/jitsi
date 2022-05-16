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
package net.java.sip.communicator.impl.gui.main.call.conference;

import java.awt.*;

import net.java.sip.communicator.impl.gui.main.call.*;
import net.java.sip.communicator.service.gui.call.*;
import net.java.sip.communicator.service.protocol.*;

/**
 * The <tt>ThumbnailConferenceCallPanel</tt> is the panel containing all video
 * conference participants as thumbnails.
 *
 * @author Yana Stamcheva
 */
public class ThumbnailConferenceCallPanel
    extends AudioConferenceCallPanel
{
    /**
     * Initializes a new <tt>ThumbnailConferenceCallPanel</tt> instance which is
     * to be used by a specific <tt>CallPanel</tt> to depict a specific
     * <tt>CallConference</tt>. The new instance will depict both the
     * audio-related and the video-related information.
     *
     * @param callPanel the <tt>CallPanel</tt> which will use the new instance
     * to depict the specified <tt>CallConference</tt>.
     * @param callConference the <tt>CallConference</tt> to be depicted by the
     * new instance
     * @param uiVideoHandler the utility which is to aid the new instance in
     * dealing with the video-related information
     */
    public ThumbnailConferenceCallPanel(CallPanel callPanel,
                                        CallConference callConference,
                                        UIVideoHandler2 uiVideoHandler)
    {
        super(callPanel, callConference);

        setBackground(Color.DARK_GRAY);
    }

    /**
     * Updates the thumbnail of the given <tt>callPeer</tt>.
     *
     * @param callPeer the <tt>CallPeer</tt>, which thumbnail
     * to update
     * @param isVideo indicates if the video is enabled for the
     * <tt>callPeer</tt>
     */
    public void updateThumbnail(CallPeer callPeer, boolean isVideo)
    {
        CallPeerRenderer peerRenderer = getCallPeerRenderer(callPeer);

        if (peerRenderer instanceof ConferencePeerPanel)
        {
            ((ConferencePeerPanel) peerRenderer)
                .enableVideoIndicator(isVideo);
        }
        
        if (peerRenderer instanceof ConferenceFocusPanel)
        {
            ((ConferenceFocusPanel) peerRenderer)
                .enableVideoIndicator(isVideo);
        }
    }

    /**
     * Updates the thumbnail of the given <tt>conferenceMember</tt>.
     *
     * @param conferenceMember the <tt>ConferenceMember</tt>, which thumbnail
     * to update
     * @param isVideo indicates if the video is enabled for the conference
     * member
     */
    public void updateThumbnail(ConferenceMember conferenceMember,
                                boolean isVideo)
    {
        CallPeerRenderer focusRenderer
            = getCallPeerRenderer(conferenceMember.getConferenceFocusCallPeer());

        if (focusRenderer instanceof ConferenceFocusPanel)
        {
            ((ConferenceFocusPanel) focusRenderer)
                .enableVideoIndicator(conferenceMember, isVideo);
        }
    }

    /**
     * Updates the local user thumbnail.
     *
     * @param isVideo indicates if the video is enabled for the local user
     */
    public void updateThumbnail(boolean isVideo)
    {
        CallPeerRenderer peerRenderer = getCallPeerRenderer(null);

        if (peerRenderer instanceof ConferencePeerPanel)
        {
            ((ConferencePeerPanel) peerRenderer)
                .enableVideoIndicator(isVideo);
        }
    }
}
