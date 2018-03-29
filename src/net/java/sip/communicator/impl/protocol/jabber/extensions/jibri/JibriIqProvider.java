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
package net.java.sip.communicator.impl.protocol.jabber.extensions.jibri;

import org.jitsi.util.*;

import org.jivesoftware.smack.packet.*;
import org.jivesoftware.smack.provider.*;
import org.jivesoftware.smack.util.PacketParserUtils;
import org.jxmpp.jid.*;
import org.jxmpp.jid.impl.*;
import org.xmlpull.v1.*;

/**
 * Parses {@link JibriIq}.
 */
public class JibriIqProvider
    extends IQProvider<JibriIq>
{
    /**
     * {@inheritDoc}
     */
    @Override
    public JibriIq parse(XmlPullParser parser, int depth)
        throws Exception
    {
        String namespace = parser.getNamespace();

        // Check the namespace
        if (!JibriIq.NAMESPACE.equals(namespace))
        {
            return null;
        }

        String rootElement = parser.getName();

        JibriIq iq;

        if (JibriIq.ELEMENT_NAME.equals(rootElement))
        {
            iq = new JibriIq();

            String action
                = parser.getAttributeValue("", JibriIq.ACTION_ATTR_NAME);
            iq.setAction(JibriIq.Action.parse(action));

            String status
                = parser.getAttributeValue("", JibriIq.STATUS_ATTR_NAME);
            iq.setStatus(JibriIq.Status.parse(status));

            String recordingMode
                = parser.getAttributeValue(
                        "", JibriIq.RECORDING_MODE_ATTR_NAME);
            if (!StringUtils.isNullOrEmpty(recordingMode))
                iq.setRecordingMode(
                        JibriIq.RecordingMode.parse(recordingMode));

            String room
                = parser.getAttributeValue("", JibriIq.ROOM_ATTR_NAME);
            if (!StringUtils.isNullOrEmpty(room))
            {
                EntityBareJid roomJid = JidCreate.entityBareFrom(room);
                iq.setRoom(roomJid);
            }

            String streamId
                = parser.getAttributeValue("", JibriIq.STREAM_ID_ATTR_NAME);
            if (!StringUtils.isNullOrEmpty(streamId))
                iq.setStreamId(streamId);

            String displayName
                = parser.getAttributeValue("", JibriIq.DISPLAY_NAME_ATTR_NAME);
            if (!StringUtils.isNullOrEmpty(displayName))
                iq.setDisplayName(displayName);

            String sipAddress
                = parser.getAttributeValue("", JibriIq.SIP_ADDRESS_ATTR_NAME);
            if (!StringUtils.isNullOrEmpty(sipAddress))
                iq.setSipAddress(sipAddress);
        }
        else
        {
            return null;
        }

        boolean done = false;

        while (!done)
        {
            switch (parser.next())
            {
                case XmlPullParser.START_TAG:
                {
                    String name = parser.getName();

                    if ("error".equals(name))
                    {
                        XMPPError error = PacketParserUtils.parseError(parser).build();
                        iq.setXMPPError(error);
                    }
                    break;
                }
                case XmlPullParser.END_TAG:
                {
                    String name = parser.getName();

                    if (rootElement.equals(name))
                    {
                        done = true;
                    }
                    break;
                }
            }
        }

        return iq;
    }
}
