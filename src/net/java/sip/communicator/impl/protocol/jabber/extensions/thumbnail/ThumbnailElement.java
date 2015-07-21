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

import java.io.*;
import java.security.*;

import javax.xml.parsers.*;

import net.java.sip.communicator.util.*;

import org.jitsi.util.xml.*;
import org.w3c.dom.*;

/**
 * The <tt>ThumbnailElement</tt> represents a "thumbnail" XML element, that is
 * contained in the file element, we're sending to notify for a file transfer.
 * The <tt>ThumbnailElement</tt>'s role is to advertise a thumbnail.
 *
 * @author Yana Stamcheva
 */
public class ThumbnailElement
{
    private static final Logger logger
        = Logger.getLogger(ThumbnailElement.class);

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

    private String cid;

    private String mimeType;

    private int width;

    private int height;

    /**
     * Creates a <tt>ThumbnailPacketExtension</tt> by specifying all extension
     * attributes.
     *
     * @param serverAddress the Jabber address of the destination contact
     * @param thumbnailData the byte array containing the thumbnail data
     * @param mimeType the mime type attribute
     * @param width the width of the thumbnail
     * @param height the height of the thumbnail
     */
    public ThumbnailElement(String serverAddress,
                            byte[] thumbnailData,
                            String mimeType,
                            int width,
                            int height)
    {
        this.cid = createCid(serverAddress, thumbnailData);
        this.mimeType = mimeType;
        this.width = width;
        this.height = height;
    }

    /**
     * Creates a <tt>ThumbnailElement</tt> by parsing the given <tt>xml</tt>.
     *
     * @param xml the XML from which we obtain the needed information to create
     * this <tt>ThumbnailElement</tt>
     */
    public ThumbnailElement(String xml)
    {
          DocumentBuilder builder;
          try
          {
              builder
                  = XMLUtils.newDocumentBuilderFactory().newDocumentBuilder();
              InputStream in = new ByteArrayInputStream (xml.getBytes());
              Document doc = builder.parse(in);

              Element e = doc.getDocumentElement();
              String elementName = e.getNodeName();

              if (elementName.equals (ELEMENT_NAME))
              {
                  this.setCid(e.getAttribute (CID));
                  this.setMimeType(e.getAttribute(MIME_TYPE));
                  this.setHeight(Integer.parseInt(e.getAttribute(HEIGHT)));
                  this.setHeight(Integer.parseInt(e.getAttribute(WIDTH)));
              }
              else
                  if (logger.isDebugEnabled())
                      logger.debug ("Element name unknown!");
          }
          catch (ParserConfigurationException ex)
          {
              if (logger.isDebugEnabled())
                  logger.debug ("Problem parsing Thumbnail Element : " + xml, ex);
          }
          catch (IOException ex)
          {
              if (logger.isDebugEnabled())
                  logger.debug ("Problem parsing Thumbnail Element : " + xml, ex);
          }
          catch (Exception ex)
          {
              if (logger.isDebugEnabled())
                  logger.debug ("Problem parsing Thumbnail Element : " + xml, ex);
          }
    }

    /**
     * Returns the XML representation of this PacketExtension.
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
        buf = addXmlAttribute(buf, CID, this.getCid());
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
    public String getCid()
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
    public void setCid(String cid)
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
     * Creates the cid attrubte value for the given <tt>contactJabberAddress</tt>
     * and <tt>thumbnailData</tt>.
     *
     * @param serverAddress the Jabber server address
     * @param thumbnailData the byte array containing the data
     * @return the cid attrubte value for the thumbnail extension
     */
    private String createCid(   String serverAddress,
                                byte[] thumbnailData)
    {
        try
        {
            return "sha1+" + Sha1Crypto.encode(thumbnailData)
                + "@" + serverAddress;
        }
        catch (NoSuchAlgorithmException e)
        {
            if (logger.isDebugEnabled())
                logger.debug("Failed to encode the thumbnail in SHA-1.", e);
        }
        catch (UnsupportedEncodingException e)
        {
            if (logger.isDebugEnabled())
                logger.debug("Failed to encode the thumbnail in SHA-1.", e);
        }

        return null;
    }
}
