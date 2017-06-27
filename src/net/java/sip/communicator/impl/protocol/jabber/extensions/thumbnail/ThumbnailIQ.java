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

import org.jivesoftware.smack.packet.*;
import org.jivesoftware.smack.provider.*;
import org.jivesoftware.smack.util.stringencoder.Base64;
import org.jxmpp.jid.Jid;
import org.xmlpull.v1.*;

/**
 * The <tt>ThumbnailIQ</tt> is an IQ packet that is meant to be used for
 * thumbnail requests and responses.
 *
 * @author Yana Stamcheva
 */
public class ThumbnailIQ
    extends IQ
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
    public ThumbnailIQ()
    {
        super(ELEMENT_NAME, NAMESPACE);
    }

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
    public ThumbnailIQ(Jid from, Jid to, String cid, IQ.Type type)
    {
        this();
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
    public ThumbnailIQ( Jid from, Jid to, String cid,
                        String mimeType, byte[] data, IQ.Type type)
    {
        this(from, to, cid, type);

        this.data = data;
        this.mimeType = mimeType;
    }

    /**
     * Creates a <tt>ThumbnailIQ</tt> packet, by specifying the content-ID,
     * the type of data and the data of the thumbnail.
     *
     * @param cid the content-ID used to identify this packet in the destination
     * @param mimeType the type of the data passed
     * @param data the data of the thumbnail
     */
    public ThumbnailIQ(String cid, String mimeType, byte[] data)
    {
        this();
        this.cid = cid;
        this.mimeType = mimeType;
        this.data = data;
    }

    /**
     * Returns the xml representing the data element in this <tt>IQ</tt> packet.
     */
    @Override
    protected IQ.IQChildElementXmlStringBuilder getIQChildElementBuilder(IQ.IQChildElementXmlStringBuilder buf)
    {
        buf.attribute(CID, cid)
            .optAttribute(TYPE, mimeType);

        if (data != null)
        {
            buf.rightAngleBracket();
            buf.append(Base64.encodeToString(data));
        }
        else
        {
            buf.setEmptyElement();
        }

        return buf;
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
