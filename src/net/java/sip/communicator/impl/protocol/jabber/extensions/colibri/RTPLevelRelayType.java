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
package net.java.sip.communicator.impl.protocol.jabber.extensions.colibri;

/**
 * Defines the RTP-level relay types as specified by RFC 3550 "RTP: A Transport
 * Protocol for Real-Time Applications" in section 2.3 "Mixers and Translators".
 *
 * @author Lyubomir Marinov
 */
public enum RTPLevelRelayType
{
    /**
     * The type of RTP-level relay which performs content mixing on the received
     * media. In order to mix the received content, the relay will usually
     * decode the received RTP and RTCP packets into raw media and will
     * subsequently generate new RTP and RTCP packets to send the new media
     * which represents the mix of the received content.
     */
    MIXER,

    /**
     * The type of RTP-level relay which does not perform content mixing on the
     * received media and rather forwards the received RTP and RTCP packets. The
     * relay will usually not decode the received RTP and RTCP into raw media.
     */
    TRANSLATOR;

    /**
     * Parses a <tt>String</tt> into an <tt>RTPLevelRelayType</tt> enum value.
     * The specified <tt>String</tt> to parse must be in a format as produced by
     * {@link #toString()}; otherwise, the method will throw an exception.
     *
     * @param s the <tt>String</tt> to parse into an <tt>RTPLevelRelayType</tt>
     * enum value
     * @return an <tt>RTPLevelRelayType</tt> enum value on which
     * <tt>toString()</tt> produces the specified <tt>s</tt>
     * @throws IllegalArgumentException if none of the
     * <tt>RTPLevelRelayType</tt> enum values produce the specified <tt>s</tt>
     * when <tt>toString()</tt> is invoked on them
     * @throws NullPointerException if <tt>s</tt> is <tt>null</tt>
     */
    public static RTPLevelRelayType parseRTPLevelRelayType(String s)
    {
        if (s == null)
            throw new NullPointerException("s");
        for (RTPLevelRelayType v : values())
        {
            if (v.toString().equalsIgnoreCase(s))
                return v;
        }
        throw new IllegalArgumentException(s);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString()
    {
        return name().toLowerCase();
    }
}
