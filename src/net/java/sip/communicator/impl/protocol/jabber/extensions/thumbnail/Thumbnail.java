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

import org.jivesoftware.smack.util.*;
import org.jivesoftware.smackx.bob.*;
import org.xmlpull.v1.*;

/**
 * The <tt>ThumbnailElement</tt> represents a "thumbnail" XML element, that is
 * contained in the file element, we're sending to notify for a file transfer.
 * The <tt>ThumbnailElement</tt>'s role is to advertise a thumbnail.
 *
 * @author Yana Stamcheva
 */
public class Thumbnail
{
    /**
     * The name of the XML element used for transport of thumbnail parameters.
     */
    public static final String ELEMENT_NAME = "thumbnail";

    /**
     * The names XMPP space that the thumbnail elements belong to.
     */
    public static final String NAMESPACE = "urn:xmpp:thumbs:0";

    /**
     * The name of the thumbnail attribute "cid".
     */
    public final static String CID = "cid";

    /**
     * The name of the thumbnail attribute "mime-type".
     */
    public final static String MIME_TYPE = "mime-type";

    /**
     * The name of the thumbnail attribute "width".
     */
    public final static String WIDTH = "width";

    /**
     * The name of the thumbnail attribute "height".
     */
    public final static String HEIGHT = "height";

    private BoBHash cid;

    private String mimeType;

    private int width;

    private int height;

    /**
     * Creates a <tt>ThumbnailPacketExtension</tt> by specifying all extension
     * attributes.
     *
     * @param thumbnailData the byte array containing the thumbnail data
     * @param mimeType the mime type attribute
     * @param width the width of the thumbnail
     * @param height the height of the thumbnail
     */
    public Thumbnail(       byte[] thumbnailData,
                            String mimeType,
                            int width,
                            int height)
    {
        this.cid = createCid(thumbnailData);
        this.mimeType = mimeType;
        this.width = width;
        this.height = height;
    }

    /**
     * Creates a new empty <tt>ThumbnailElement</tt>.
     * @param parser 
     */
    public Thumbnail(XmlPullParser parser)
    {
        cid = parseCid(parser.getAttributeValue("", CID));
        mimeType = parser.getAttributeValue("", MIME_TYPE);
        String parserWidth = parser.getAttributeValue("", WIDTH);
        String parserHeight = parser.getAttributeValue("", HEIGHT);
        try
        {
            width = Integer.parseInt(parserWidth);
            height = Integer.parseInt(parserHeight);
        }
        catch (NumberFormatException nfe)
        {
            // ignore, width and height are optional
        }
    }

    private BoBHash parseCid(String cid)
    {
        // previous Jitsi versions used to send <hashType>-<hash>@<server>
        if (!cid.endsWith("@bob.xmpp.org"))
        {
            cid = cid.substring(0, cid.indexOf('@')) + "@bob.xmpp.org";
        }

        return BoBHash.fromCid(cid);
    }

    /**
     * Returns the XML representation of this ExtensionElement.
     *
     * @return the packet extension as XML.
     */
    public String toXML()
    {
        StringBuffer buf = new StringBuffer();

        // open element
        buf.append("<").append(ELEMENT_NAME).
            append(" xmlns=\"").append(NAMESPACE).append("\"");

        // adding thumbnail parameters
        buf = addXmlAttribute(buf, CID, this.getCid().getCid());
        buf = addXmlAttribute(buf, MIME_TYPE, this.getMimeType());
        buf = addXmlIntAttribute(buf, WIDTH, this.getWidth());
        buf = addXmlIntAttribute(buf, HEIGHT, this.getWidth());

        // close element
        buf.append("/>");

        return buf.toString();
    }

    /**
     * Returns the Content-ID, corresponding to this <tt>ThumbnailElement</tt>.
     * @return the Content-ID, corresponding to this <tt>ThumbnailElement</tt>
     */
    public BoBHash getCid()
    {
        return cid;
    }

    /**
     * Returns the mime type of this <tt>ThumbnailElement</tt>.
     * @return the mime type of this <tt>ThumbnailElement</tt>
     */
    public String getMimeType()
    {
        return mimeType;
    }

    /**
     * Returns the width of this <tt>ThumbnailElement</tt>.
     * @return the width of this <tt>ThumbnailElement</tt>
     */
    public int getWidth()
    {
        return width;
    }

    /**
     * Returns the height of this <tt>ThumbnailElement</tt>.
     * @return the height of this <tt>ThumbnailElement</tt>
     */
    public int getHeight()
    {
        return height;
    }

    /**
     * Sets the content-ID of this <tt>ThumbnailElement</tt>.
     * @param cid the content-ID to set
     */
    public void setCid(BoBHash cid)
    {
        this.cid = cid;
    }

    /**
     * Sets the mime type of the thumbnail.
     * @param mimeType the mime type of the thumbnail
     */
    public void setMimeType(String mimeType)
    {
        this.mimeType = mimeType;
    }

    /**
     * Sets the width of the thumbnail
     * @param width the width of the thumbnail
     */
    public void setWidth(int width)
    {
        this.width = width;
    }

    /**
     * Sets the height of the thumbnail
     * @param height the height of the thumbnail
     */
    public void setHeight(int height)
    {
        this.height = height;
    }

    /**
     * Creates the XML <tt>String</tt> corresponding to the specified attribute
     * and value and adds them to the <tt>buff</tt> StringBuffer.
     *
     * @param buff the <tt>StringBuffer</tt> to add the attribute and value to.
     * @param attrName the name of the thumbnail attribute that we're adding.
     * @param attrValue the value of the attribute we're adding to the XML
     * buffer.
     * @return the <tt>StringBuffer</tt> that we've added the attribute and its
     * value to.
     */
    private StringBuffer addXmlAttribute(   StringBuffer buff,
                                            String attrName,
                                            String attrValue)
    {
        buff.append(" " + attrName + "=\"").append(attrValue).append("\"");

        return buff;
    }

    /**
     * Creates the XML <tt>String</tt> corresponding to the specified attribute
     * and value and adds them to the <tt>buff</tt> StringBuffer.
     *
     * @param buff the <tt>StringBuffer</tt> to add the attribute and value to.
     * @param attrName the name of the thumbnail attribute that we're adding.
     * @param attrValue the value of the attribute we're adding to the XML
     * buffer.
     * @return the <tt>StringBuffer</tt> that we've added the attribute and its
     * value to.
     */
    private StringBuffer addXmlIntAttribute(StringBuffer buff,
                                            String attrName,
                                            int attrValue)
    {

        return addXmlAttribute(buff, attrName, String.valueOf(attrValue));
    }

    /**
     * Creates the cid attribute value for the given <tt>thumbnailData</tt>.
     *
     * @param thumbnailData the byte array containing the data
     * @return the cid attribute value for the thumbnail extension
     */
    private BoBHash createCid(byte[] thumbnailData)
    {
        return new BoBHash(SHA1.hex(thumbnailData), "sha1");
    }
}
