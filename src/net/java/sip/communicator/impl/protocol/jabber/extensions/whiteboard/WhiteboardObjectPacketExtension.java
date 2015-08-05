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

package net.java.sip.communicator.impl.protocol.jabber.extensions.whiteboard;

import java.io.*;

import javax.xml.parsers.*;

import net.java.sip.communicator.util.*;

import org.jitsi.util.xml.*;
import org.jivesoftware.smack.packet.*;
import org.w3c.dom.*;

/**
 * WhiteboardObjectPacketExtension
 *
 * @author Julien Waechter
 */
public class WhiteboardObjectPacketExtension implements PacketExtension
{
    private static final Logger logger =
      Logger.getLogger (WhiteboardObjectPacketExtension.class);

    /**
     * The name of the XML element used for transport of white-board parameters.
     */
    public static final String ELEMENT_NAME = "xObject";

    /**
     * The names XMPP space that the white-board elements belong to.
     */
    public static final String NAMESPACE = "http://jabber.org/protocol/swb";

    /**
     * A type string constant indicating that the current object must be deleted.
     */
    public static final String ACTION_DELETE = "DELETE";

    /**
     * A type string constant indicating that the current object must be drawn.
     */
    public static final String ACTION_DRAW = "DRAW";

    /**
     * A type string constant indicating that the current object must be moved.
     */
    public static final String ACTION_MOVE = "MOVE";

    /**
     * The current WhiteboardObject to be sent.
     */
    private WhiteboardObjectJabberImpl whiteboardObject;

    /**
     * The current action associated with the WhiteboardObject.
     */
    private String action;

    /**
     *  The identifier of the WhiteboardObject to be treated
     *  When we receive a delete message,
     *   we've only the identifier of the WhiteboardObject
     */
    private String whiteboardObjectID;

    /**
     * Default WhiteboardObjectPacketExtension constructor.
     */
    public WhiteboardObjectPacketExtension ()
    {
        this.action = ACTION_DRAW;
    }

    /**
     * WhiteboardObjectPacketExtension constructor.
     *
     * @param id Identifier of the WhiteboardObject to be treated
     * @param action The current action associated with the WhiteboardObject.
     */
    public WhiteboardObjectPacketExtension (String id, String action)
    {
        this.whiteboardObjectID = id;
        this.action = action;
    }

    /**
     * Constructs and initializes a WhiteboardObjectPacketExtension.
     *
     * @param whiteboardObject The WhiteboardObject to be treated
     * @param action The current action associated with the WhiteboardObject.
     */
    public WhiteboardObjectPacketExtension (
      WhiteboardObjectJabberImpl whiteboardObject, String action)
    {
        this.whiteboardObject = whiteboardObject;
        this.action = action;
    }

    /**
     * WhiteboardObjectPacketExtension constructor with a XML-SVG String.
     *
     * @param xml XML-SVG String
     */
    public  WhiteboardObjectPacketExtension (String xml)
    {
        try
        {
            DocumentBuilder builder
                    = XMLUtils.newDocumentBuilderFactory().newDocumentBuilder();
            InputStream in = new ByteArrayInputStream (xml.getBytes ());
            Document doc = builder.parse (in);

            Element e = doc.getDocumentElement ();
            String elementName = e.getNodeName ();
            this.action = WhiteboardObjectPacketExtension.ACTION_DRAW;

            if (elementName.equals ("rect"))
            {
                //we have a rectangle
                whiteboardObject = new WhiteboardObjectRectJabberImpl (xml);
            }
            else if (elementName.equals ("circle"))
            {
                //we have a circle
                whiteboardObject = new WhiteboardObjectCircleJabberImpl (xml);
            }
            else if (elementName.equals ("path"))
            {
                //we have a path
                whiteboardObject = new WhiteboardObjectPathJabberImpl (xml);
            }
            else if (elementName.equals ("polyline"))
            {
                //we have polyline
                whiteboardObject = new WhiteboardObjectPolyLineJabberImpl (xml);
            }
            else if (elementName.equals ("polygon"))
            {
                //we have a polygon
                whiteboardObject = new WhiteboardObjectPolygonJabberImpl (xml);
            }
            else if (elementName.equals ("line"))
            {
                //we have a line
                whiteboardObject = new WhiteboardObjectLineJabberImpl (xml);
            }
            else if (elementName.equals ("text"))
            {
                //we have a text
                whiteboardObject = new WhiteboardObjectTextJabberImpl (xml);
            }
            else if (elementName.equals ("image"))
            {
                //we have an image
                whiteboardObject = new WhiteboardObjectImageJabberImpl (xml);
            }
            else if (elementName.equals ("delete"))
            {
                //we have a delete action
                this.setWhiteboardObjectID (e.getAttribute ("id"));
                this.action = WhiteboardObjectPacketExtension.ACTION_DELETE;
            }
            else //we have a problem :p
                if (logger.isDebugEnabled())
                    logger.debug ("elementName unknow\n");
        }
        catch (ParserConfigurationException ex)
        {
            if (logger.isDebugEnabled())
                logger.debug ("Problem WhiteboardObject : " + xml, ex);
        }
        catch (IOException ex)
        {
            if (logger.isDebugEnabled())
                logger.debug ("Problem WhiteboardObject : " + xml, ex);
        }
        catch (Exception ex)
        {
            if (logger.isDebugEnabled())
                logger.debug ("Problem WhiteboardObject : " + xml, ex);
        }

    }

    /**
     * Returns the root element name.
     *
     * @return the element name.
     */
    public String getElementName ()
    {
        return ELEMENT_NAME;
    }

    /**
     * Returns the root element XML namespace.
     *
     * @return the namespace.
     */
    public String getNamespace ()
    {
        return NAMESPACE;
    }

    /**
     * Returns the XML representation of the WhiteboardObject
     *
     * @return the WhiteboardObject as XML.
     */
    public String toXML ()
    {
        String s="";
        if(getAction ().equals (
          WhiteboardObjectPacketExtension.ACTION_DELETE))
        {
            s = "<delete id=\"#i\"/>";
            s = s.replaceAll ("#i", getWhiteboardObjectID());
        }
        else
            s = getWhiteboardObject ().toXML ();

        return "<" + WhiteboardObjectPacketExtension.ELEMENT_NAME +
          " xmlns=\"" + WhiteboardObjectPacketExtension.NAMESPACE +
          "\">"+s+"</" + WhiteboardObjectPacketExtension.ELEMENT_NAME + ">";
    }

    /**
     * Returns the current action associated with the WhiteboardObject to send.
     * (DELETE - DRAW - MOVE)
     *
     * @return current action.
     */
    public String getAction ()
    {
        return action;
    }

    /**
     * Sets the action associated with the WhiteboardObject to send.
     * (DELETE - DRAW - MOVE)
     *
     * @param action the action associated with the WhiteboardObject to send.
     */
    public void setAction (String action)
    {
        this.action = action;
    }

    /**
     * Returns the current WhiteboardObject to be sent.
     *
     * @return WhiteboardObject to be sent
     */
    public WhiteboardObjectJabberImpl getWhiteboardObject ()
    {
        return whiteboardObject;
    }

    /**
     * Returns the current WhiteboardObject's identifier to be sent.
     * (For a delete WhiteboardObject message)
     *
     * @return WhiteboardObject's identifier
     */
    public String getWhiteboardObjectID ()
    {
        return whiteboardObjectID;
    }

    /**
     * Sets the current WhiteboardObject's identifier to be sent.
     * (For a delete WhiteboardObject message)
     *
     * @param objectID WhiteboardObject's identifier
     */
    public void setWhiteboardObjectID (String objectID)
    {
        this.whiteboardObjectID = objectID;
    }
}
