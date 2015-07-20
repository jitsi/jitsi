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
package net.java.sip.communicator.impl.protocol.jabber.extensions.jingle;

import org.jivesoftware.smack.packet.*;

/**
 * The <tt>reason</tt> element provides human or machine-readable information
 * explaining what prompted the <tt>action</tt> of the encapsulating
 * <tt>jingle</tt> element.
 *
 * @author Emil Ivov
 * @author Lyubomir Marinov
 */
public class ReasonPacketExtension
    implements PacketExtension
{
    /**
     * The name space (or rather lack thereof ) that the reason element
     * belongs to.
     */
    public static final String NAMESPACE = "";

    /**
     * The name of the "content" element.
     */
    public static final String ELEMENT_NAME = "reason";

    /**
     * The name of the text element.
     */
    public static final String TEXT_ELEMENT_NAME = "text";

    /**
     * The reason that this packet extension is transporting.
     */
    private final Reason reason;

    /**
     * The content of the text element (if any) providing human-readable
     * information about the reason for the action.
     */
    private String text;

    /**
     * XEP-0166 mentions that the "reason" element MAY contain an element
     * qualified by some other namespace that provides more detailed machine-
     * readable information about the reason for the action.
     */
    private PacketExtension otherExtension;

    /**
     * Creates a new <tt>ReasonPacketExtension</tt> instance with the specified
     * reason String.
     *
     * @param reason the reason string that we'd like to transport in this
     * packet extension, which may or may not be one of the static strings
     * defined here.
     * @param text an element providing human-readable information about the
     * reason for the action or <tt>null</tt> if no such information is
     * currently available.
     * @param packetExtension any other element that MAY be providing further
     * information or <tt>null</tt> if no such element has been specified.
     */
    public ReasonPacketExtension(Reason          reason,
                                 String          text,
                                 PacketExtension packetExtension)
    {
        this.reason = reason;
        this.text = text;
        this.otherExtension = packetExtension;
    }

    /**
     * Returns the reason string that this packet extension is transporting.
     *
     * @return the reason string that this packet extension is transporting.
     */
    public Reason getReason()
    {
        return reason;
    }

    /**
     * Returns human-readable information about the reason for the action or
     * <tt>null</tt> if no such information is currently available.
     *
     * @return human-readable information about the reason for the action or
     * <tt>null</tt> if no such information is currently available.
     */
    public String getText()
    {
        return text;
    }

    /**
     * Sets the human-readable information about the reason for the action or
     * <tt>null</tt> if no such information is currently available
     *
     * @param text the human-readable information about the reason for the
     * action or <tt>null</tt> if no such information is currently available
     */
    public void setText(String text)
    {
        this.text = text;
    }

    /**
     * Returns an extra extension containing further info about this action or
     * <tt>null</tt> if no such extension has been specified. This method
     * returns the extension that XEP-0166 refers to the following way:
     * the "reason" element MAY contain an element qualified by some other
     * namespace that provides more detailed machine-readable information about
     * the reason for the action.
     *
     * @return an extra extension containing further info about this action or
     * <tt>null</tt> if no such extension has been specified.
     */
    public PacketExtension getOtherExtension()
    {
        return otherExtension;
    }

    /**
     * Sets the extra extension containing further info about this action or
     * <tt>null</tt> if no such extension has been specified.
     *
     * @param otherExtension the extra extension containing further info about
     * this action or <tt>null</tt> if no such extension has been specified
     */
    public void setOtherExtension(PacketExtension otherExtension)
    {
        this.otherExtension = otherExtension;
    }

    /**
     * Returns the root element name.
     *
     * @return the element name.
     */
    public String getElementName()
    {
        return ELEMENT_NAME;
    }

    /**
     * Returns the root element XML namespace.
     *
     * @return the namespace.
     */
    public String getNamespace()
    {
        return NAMESPACE;
    }

    /**
     * Returns the XML representation of the PacketExtension.
     *
     * @return the packet extension as XML.
     */
    public String toXML()
    {
        StringBuilder bldr = new StringBuilder("<" + getElementName() + ">");

        bldr.append("<" + getReason().toString() + "/>");

        //add reason "text" if we have it
        if(getText() != null)
        {
            bldr.append("<text>");
            bldr.append(getText());
            bldr.append("</text>");
        }

        //add the extra element if it has been specified.
        if(getOtherExtension() != null)
        {
            bldr.append(getOtherExtension().toXML());
        }

        bldr.append("</" + getElementName() + ">");
        return bldr.toString();
    }
}
