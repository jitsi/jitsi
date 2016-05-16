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

import java.util.*;

/**
 * Parent class for SecurityOn and SecurityOff events.
 *
 * @author Yana Stamcheva
 */
public abstract class CallPeerSecurityStatusEvent
    extends EventObject
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * Constant value defining that security is enabled.
     */
    public static final int AUDIO_SESSION = 1;

    /**
     * Constant value defining that security is disabled.
     */
    public static final int VIDEO_SESSION = 2;

    /**
     * Session type of the event {@link #AUDIO_SESSION} or
     * {@link #VIDEO_SESSION}.
     */
    private final int sessionType;

    /**
     * Constructor required by the EventObject.
     *
     * @param source the source object for this event.
     * @param sessionType either <code>AUDIO_SESSION</code> or
     *                    <code>VIDEO_SESSION</code> to indicate the type of the
     *                    session
     */
    public CallPeerSecurityStatusEvent(Object source, int sessionType)
    {
        super(source);

        this.sessionType = sessionType;
    }

    /**
     * Returns the type of the session, either AUDIO_SESSION or VIDEO_SESSION.
     *
     * @return the type of the session, either AUDIO_SESSION or VIDEO_SESSION.
     */
    public int getSessionType()
    {
        return sessionType;
    }
}
