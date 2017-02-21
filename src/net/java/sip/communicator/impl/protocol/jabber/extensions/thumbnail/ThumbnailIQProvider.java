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
package net.java.sip.communicator.impl.protocol.jabber.extensions.thumbnail;

import net.java.sip.communicator.util.Base64;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.provider.IQProvider;
import org.jxmpp.jid.Jid;
import org.xmlpull.v1.XmlPullParser;

/**
 * The <tt>ThumbnailIQ</tt> is an IQ packet that is meant to be used for
 * thumbnail requests and responses.
 *
 * @author Yana Stamcheva
 */
public class ThumbnailIQProvider
    extends IQProvider
{
    /**
     * Parses the given <tt>XmlPullParser</tt> into a ThumbnailIQ packet and
     * returns it.
     */
    @Override
    public IQ parse(XmlPullParser parser, int depth) throws Exception
    {
        String elementName = parser.getName();
        String namespace = parser.getNamespace();

        if (elementName.equals(ThumbnailIQ.ELEMENT_NAME)
            && namespace.equals(ThumbnailIQ.NAMESPACE))
        {
            String cid = parser.getAttributeValue("", ThumbnailIQ.CID);
            String mimeType = parser.getAttributeValue("", ThumbnailIQ.TYPE);
            int eventType = parser.next();
            if (eventType == XmlPullParser.TEXT)
            {
                byte[] data = Base64.decode(parser.getText());
                return new ThumbnailIQ(cid, mimeType, data);
            }
        }

        return null;
    }
}
