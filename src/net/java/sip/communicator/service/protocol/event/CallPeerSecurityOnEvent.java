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
package net.java.sip.communicator.service.protocol.event;

import net.java.sip.communicator.service.protocol.*;

import org.jitsi.service.neomedia.*;
import org.jitsi.service.protocol.event.*;

/**
 * The <tt>CallPeerSecurityOnEvent</tt> is triggered whenever a
 * communication with a given peer is going secure.
 *
 * @author Werner Dittmann
 * @author Yana Stamcheva
 */
public class CallPeerSecurityOnEvent
    extends CallPeerSecurityStatusEvent
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    private final String cipher;

    private final SrtpControl srtpControl;

    /**
     * The event constructor
     *
     * @param callPeer the call peer associated with this event
     * @param sessionType the type of the session, either
     *            {@link CallPeerSecurityStatusEvent#AUDIO_SESSION} or
     *            {@link CallPeerSecurityStatusEvent#VIDEO_SESSION}
     * @param cipher the cipher used for the encryption
     * @param srtpControl the security controller that caused this event
     */
    public CallPeerSecurityOnEvent( CallPeer callPeer,
                                    int sessionType,
                                    String cipher,
                                    SrtpControl srtpControl)
    {
        super(callPeer, sessionType);
        this.srtpControl = srtpControl;
        this.cipher = cipher;
    }

    /**
     * Returns the cipher used for the encryption.
     *
     * @return the cipher used for the encryption.
     */
    public String getCipher()
    {
        return cipher;
    }

    /**
     * Gets the security controller that caused this event.
     *
     * @return the security controller that caused this event.
     */
    public SrtpControl getSecurityController()
    {
        return srtpControl;
    }
}
