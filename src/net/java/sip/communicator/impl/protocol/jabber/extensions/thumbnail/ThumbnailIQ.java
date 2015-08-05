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

import net.java.sip.communicator.util.*;

import org.jivesoftware.smack.packet.*;
import org.jivesoftware.smack.provider.*;
import org.xmlpull.v1.*;

/**
 * The <tt>ThumbnailIQ</tt> is an IQ packet that is meant to be used for
 * thumbnail requests and responses.
 *
 * @author Yana Stamcheva
 */
public class ThumbnailIQ
    extends IQ
    implements IQProvider
{
    /**
     * The names XMPP space that the thumbnail elements belong to.
     */
    public static final String NAMESPACE = "urn:xmpp:bob";

    /**
     * The name of the "data" element.
     */
    public static final String ELEMENT_NAME = "data";

    /**
     * The name of the thumbnail attribute "cid".
     */
    public final static String CID = "cid";

    /**
     * The name of the thumbnail attribute "mime-type".
     */
    public final static String TYPE = "type";

    private String cid;

    private String mimeType;

    private byte[] data;

    /**
     * An empty constructor used to initialize this class as an
     * <tt>IQProvier</tt>.
     */
    public ThumbnailIQ() {}

    /**
     * Creates a <tt>ThumbnailIQ</tt> packet, by specifying the source, the
     * destination, the content-ID and the type of this packet. The type could
     * be one of the types defined in <tt>IQ.Type</tt>.
     *
     * @param from the source of the packet
     * @param to the destination of the packet
     * @param cid the content-ID used to identify this packet in the destination
     * @param type the of the packet, which could be one of the types defined
     * in <tt>IQ.Type</tt>
     */
    public ThumbnailIQ(String from, String to, String cid, Type type)
    {
        this.cid = cid;

        this.setFrom(from);
        this.setTo(to);
        this.setType(type);
    }

    /**
     * Creates a <tt>ThumbnailIQ</tt> packet, by specifying the source, the
     * destination, the content-ID, the type of data and the data of the
     * thumbnail. We also precise the type of the packet to create.
     *
     * @param from the source of the packet
     * @param to the destination of the packet
     * @param cid the content-ID used to identify this packet in the destination
     * @param mimeType the type of the data passed
     * @param data the data of the thumbnail
     * @param type the of the packet, which could be one of the types defined
     * in <tt>IQ.Type</tt>
     */
    public ThumbnailIQ( String from, String to, String cid,
                        String mimeType, byte[] data, Type type)
    {
        this(from, to, cid, type);

        this.data = data;
        this.mimeType = mimeType;
    }

    /**
     * Parses the given <tt>XmlPullParser</tt> into a ThumbnailIQ packet and
     * returns it.
     * @see IQProvider#parseIQ(XmlPullParser)
     */
    public IQ parseIQ(XmlPullParser parser) throws Exception
    {
        String elementName = parser.getName();
        String namespace = parser.getNamespace();

        if (elementName.equals(ELEMENT_NAME)
            && namespace.equals(NAMESPACE))
        {
            this.cid = parser.getAttributeValue("", CID);
            this.mimeType = parser.getAttributeValue("", TYPE);
        }

        int eventType = parser.next();

        if (eventType == XmlPullParser.TEXT)
        {
            this.data = Base64.decode(parser.getText());
        }

        return this;
    }

    /**
     * Returns the xml representing the data element in this <tt>IQ</tt> packet.
     */
    @Override
    public String getChildElementXML()
    {
        StringBuffer buf = new StringBuffer();

        // open extension
        buf.append("<").append(ELEMENT_NAME)
            .append(" xmlns=\"").append(NAMESPACE).append("\"")
            .append(" " + CID).append("=\"").append(cid).append("\"");

        if (mimeType != null)
            buf.append(" " + TYPE).append("=\"").append(mimeType).append("\">");
        else
            buf.append(">");

        if (data != null)
        {
            byte[] encodedData = Base64.encode(data);
            buf.append(new String(encodedData));
        }

        buf.append("</data>");

        return buf.toString();
    }

    /**
     * Returns the content-ID of this thumbnail packet.
     * @return the content-ID of this thumbnail packet
     */
    public String getCid()
    {
        return cid;
    }

    /**
     * Returns the data of the thumbnail.
     * @return the data of the thumbnail
     */
    public byte[] getData()
    {
        return data;
    }
}
