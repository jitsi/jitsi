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

import net.java.sip.communicator.impl.protocol.jabber.*;
import net.java.sip.communicator.util.*;

import org.jitsi.util.xml.*;
import org.jivesoftware.smack.packet.*;
import org.w3c.dom.*;

/**
 * Whiteboard session packet extension.
 */
public class WhiteboardSessionPacketExtension
    implements PacketExtension
{
    private Logger logger
        = Logger.getLogger(WhiteboardSessionPacketExtension.class);

    /**
     * A type string constant indicating that the user would like to leave the
     * current white board session.
     */
    public static final String ACTION_LEAVE = "LEAVE";

    /**
     * The name of the XML element used for transport of white-board parameters.
     */
    public static final String ELEMENT_NAME = "xSession";

    /**
     * The names XMPP space that the white-board elements belong to.
     */
    public static final String NAMESPACE = "http://jabber.org/protocol/swb";

    /**
     * The current action associated with the WhiteboardObject.
     */
    private String action;

    /**
     * The white board session for which the action is about.
     */
    private WhiteboardSessionJabberImpl whiteboardSession;

    /**
     * The address of the contact associated with this packet extension.
     */
    private String contactAddress;

    private String whiteboardSessionId;

    /**
     * Constructs and initializes a WhiteboardObjectPacketExtension.
     *
     * @param session The WhiteboardSession to be treated
     * @param contactAddress The address of the contact associated with this
     * packet extension
     * @param action The current action associated with the WhiteboardSession.
     */
    public WhiteboardSessionPacketExtension (
        WhiteboardSessionJabberImpl session,
        String contactAddress,
        String action)
    {
        this.whiteboardSession = session;
        this.whiteboardSessionId = session.getWhiteboardID();
        this.contactAddress = contactAddress;
        this.action = action;
    }

    /**
     * WhiteboardSessionPacketExtension constructor with a XML-SVG String.
     *
     * @param xml XML-SVG String
     */
    public  WhiteboardSessionPacketExtension (String xml)
    {
        try
        {
            DocumentBuilder builder
                    = XMLUtils.newDocumentBuilderFactory().newDocumentBuilder();
            InputStream in = new ByteArrayInputStream (xml.getBytes ());
            Document doc = builder.parse (in);

            Element e = doc.getDocumentElement ();
            String elementName = e.getNodeName ();

            if (elementName.equals (ACTION_LEAVE))
            {
                this.setWhiteboardSessionId(e.getAttribute ("id"));
                this.setContactAddress(e.getAttribute ("userId"));
                this.action = WhiteboardSessionPacketExtension.ACTION_LEAVE;
            }
            else
                if (logger.isDebugEnabled())
                    logger.debug ("Element name unknown!");
        }
        catch (ParserConfigurationException ex)
        {
            if (logger.isDebugEnabled())
                logger.debug ("Problem WhiteboardSession : " + xml, ex);
        }
        catch (IOException ex)
        {
            if (logger.isDebugEnabled())
                logger.debug ("Problem WhiteboardSession : " + xml, ex);
        }
        catch (Exception ex)
        {
            if (logger.isDebugEnabled())
                logger.debug ("Problem WhiteboardSession : " + xml, ex);
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

    public String toXML()
    {
        String s = "";

        if(action.equals (
          WhiteboardSessionPacketExtension.ACTION_LEAVE))
        {
            s = "<LEAVE id=\"#sessionId\" userId=\"#userId\"/>";
            s = s.replaceAll ("#sessionId", whiteboardSession.getWhiteboardID());
            s = s.replaceAll ("#userId", contactAddress);
        }

        return "<" + WhiteboardSessionPacketExtension.ELEMENT_NAME +
          " xmlns=\"" + WhiteboardSessionPacketExtension.NAMESPACE +
          "\">"+s+"</" + WhiteboardSessionPacketExtension.ELEMENT_NAME + ">";
    }

    /**
     * Returns the white board session identifier.
     *
     * @return the white board session identifier
     */
    public String getWhiteboardSessionId()
    {
        return whiteboardSessionId;
    }

    /**
     * Sets the white board session identifier.
     *
     * @param whiteboardSessionId the identifier of the session
     */
    public void setWhiteboardSessionId(String whiteboardSessionId)
    {
        this.whiteboardSessionId = whiteboardSessionId;
    }

    /**
     * Returns the action associated with this session packet extension.
     *
     * @return the action associated with this session packet extension.
     */
    public String getAction()
    {
        return action;
    }

    /**
     * Sets the action associated with this session packet extension.
     *
     * @param action the action associated with this session packet extension.
     */
    public void setAction(String action)
    {
        this.action = action;
    }

    /**
     * Returns the address of the contact associated with this packet extension
     *
     * @return the address of the contact associated with this packet extension
     */
    public String getContactAddress()
    {
        return contactAddress;
    }

    /**
     * Sets the address of the contact associated with this packet extension
     *
     * @param contactAddress the address of the contact associated with this
     * packet extension
     */
    public void setContactAddress(String contactAddress)
    {
        this.contactAddress = contactAddress;
    }
}
