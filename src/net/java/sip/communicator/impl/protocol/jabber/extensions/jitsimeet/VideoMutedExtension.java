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
package net.java.sip.communicator.impl.protocol.jabber.extensions.jitsimeet;

import net.java.sip.communicator.impl.protocol.jabber.extensions.*;

/**
 * Video muted extension that is included in users presence in Jitsi-meet
 * conferences. It does carry the info about user's video muted status.
 *
 * @author Pawel Domas
 */
public class VideoMutedExtension
    extends AbstractPacketExtension
{
    /**
     * The namespace of this packet extension.
     */
    public static final String NAMESPACE = "http://jitsi.org/jitmeet/video";

    /**
     * XML element name of this packet extension.
     */
    public static final String ELEMENT_NAME = "videomuted";

    /**
     * Creates new instance of <tt>VideoMutedExtension</tt>.
     */
    public VideoMutedExtension()
    {
        super(NAMESPACE, ELEMENT_NAME);
    }

    /**
     * Check whether or not user's video is in muted status.
     * @return <tt>true</tt> if muted, <tt>false</tt> if unmuted or
     *         <tt>null</tt> if no valid info found in the extension body.
     */
    public Boolean isVideoMuted()
    {
        return Boolean.valueOf(getText());
    }

    /**
     * Sets user's video muted status.
     *
     * @param videoMuted <tt>true</tt> or <tt>false</tt> which indicates video
     *                   muted status of the user.
     */
    public void setVideoMuted(Boolean videoMuted)
    {
        setText(
            String.valueOf(videoMuted));
    }
}
