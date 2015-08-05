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

import net.java.sip.communicator.service.protocol.*;

/**
 * Event that notify that remote control feature has been revoked. This is used
 * in desktop sharing related usage. After rights beeing revoked, local
 * peer must not notify keyboard and mouse events to remote peer.
 *
 * @author Sebastien Vincent
 */
public class RemoteControlRevokedEvent extends EventObject
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * Constructs a new <tt>RemoteControlGrantedEvent</tt> object.
     *
     * @param source source object
     */
    public RemoteControlRevokedEvent(Object source)
    {
        super(source);
    }

    /**
     * Get the <tt>CallPeer</tt>.
     *
     * @return the <tt>CallPeer</tt>
     */
    public CallPeer getCallPeer()
    {
        return (CallPeer)getSource();
    }
}
