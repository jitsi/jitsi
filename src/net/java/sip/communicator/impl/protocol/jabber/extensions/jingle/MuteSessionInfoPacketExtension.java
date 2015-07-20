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
package net.java.sip.communicator.impl.protocol.jabber.extensions.jingle;

/**
 * Represents the <tt>mute</tt> and <tt>unmute</tt> session info types.
 *
 * @author Emil Ivov
 */
public class MuteSessionInfoPacketExtension extends SessionInfoPacketExtension
{
    /**
     * The name of the <tt>name</tt> mute attribute.
     */
    public static final String NAME_ATTR_VALUE = "name";

    /**
     * Creates a <tt>SessionInfoPacketExtension</tt> instance corresponding to
     * either the {@link SessionInfoType#mute} or {@link SessionInfoType#unmute}
     * types according to the value of the <tt>mute</tt> parameter.
     *
     * @param mute <tt>true</tt> if the new instance is to be of the
     * {@link SessionInfoType#mute} type and <tt>false</tt> for a {@link
     * SessionInfoType#unmute}
     * @param name the name of the session to be muted or <tt>null</tt> if the
     * element pertains to all active sessions
     */
    public MuteSessionInfoPacketExtension(boolean mute, String name)
    {
        super(mute ? SessionInfoType.mute : SessionInfoType.unmute);
        setAttribute(NAME_ATTR_VALUE, name);

    }

    /**
     * Determines if this session info packet represents a mute.
     *
     * @return <tt>true</tt> if this packet represents a {@link
     * SessionInfoType#mute} and <tt>false</tt> otherwise.
     */
    public boolean isMute()
    {
        return getType() == SessionInfoType.mute;
    }

    /**
     * Returns the name of the session that this extension is pertaining to or
     * <tt>null</tt> if it is referring to all active sessions.
     *
     * @return the name of the session that this extension is pertaining to or
     * <tt>null</tt> if it is referring to all active sessions.
     */
    public String getName()
    {
        return getAttributeAsString(NAME_ATTR_VALUE);
    }
}
