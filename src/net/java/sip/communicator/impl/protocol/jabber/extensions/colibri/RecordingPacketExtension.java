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

import net.java.sip.communicator.impl.protocol.jabber.extensions.*;

/**
 * Implements <tt>AbstractPacketExtension</tt> for a "recording" element.
 *
 * @author Boris Grozev
 */
public class RecordingPacketExtension
        extends AbstractPacketExtension
{
    /**
     * The XML name of the <tt>recording</tt> element.
     */
    public static final String ELEMENT_NAME = "recording";

    /**
     * The XML name of the <tt>state</tt> attribute.
     */
    private static final String STATE_ATTR_NAME = "state";

    /**
     * The XML name of the <tt>token</tt> attribute.
     */
    private static final String TOKEN_ATTR_NAME = "token";

    /**
     * Initializes a new <tt>RecordingPacketExtension</tt> instance.
     */
    public RecordingPacketExtension()
    {
        super(ColibriConferenceIQ.NAMESPACE, ELEMENT_NAME);
    }

    public String getToken()
    {
        return getAttributeAsString(TOKEN_ATTR_NAME);
    }

    public void setToken(String token)
    {
        setAttribute(TOKEN_ATTR_NAME, token);
    }

    public State getState()
    {
        return State.parseString(getAttributeAsString(STATE_ATTR_NAME));
    }

    public void setState(State state)
    {
        setAttribute(STATE_ATTR_NAME, state.toString());
    }

    private enum State
    {
        ON("on"),
        OFF("off");

        private String name;
        private State(String name)
        {
            this.name = name;
        }
        public String toString()
        {
            return name;
        }
        public static State parseString(String s)
        {
            if (ON.toString().equalsIgnoreCase(s))
                return ON;
            return OFF;
        }
    }
}

