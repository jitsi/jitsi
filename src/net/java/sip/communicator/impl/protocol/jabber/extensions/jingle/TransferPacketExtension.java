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

import net.java.sip.communicator.impl.protocol.jabber.extensions.*;

/**
 * Implements <tt>AbstractPacketExtension</tt> for the "transfer" element
 * defined by XEP-0251: Jingle Session Transfer.
 *
 * @author Lyubomir Marinov
 */
public class TransferPacketExtension
    extends AbstractPacketExtension
{
    /**
     * The name of the "transfer" element.
     */
    public static final String ELEMENT_NAME = "transfer";

    /**
     * The name of the "from" attribute of the "transfer" element.
     */
    public static final String FROM_ATTR_NAME = "from";

    /**
     * The namespace of the "transfer" element.
     */
    public static final String NAMESPACE = "urn:xmpp:jingle:transfer:0";

    /**
     * The name of the "sid" attribute of the "transfer" element.
     */
    public static final String SID_ATTR_NAME = "sid";

    /**
     * The name of the "to" attribute of the "transfer" element.
     */
    public static final String TO_ATTR_NAME = "to";

    /**
     * Initializes a new <tt>TransferPacketExtension</tt> instance.
     */
    public TransferPacketExtension()
    {
        super(NAMESPACE, ELEMENT_NAME);
    }

    /**
     * Gets the value of the "from" attribute of this "transfer" element.
     *
     * @return the value of the "from" attribute of this "transfer" element
     */
    public String getFrom()
    {
        return getAttributeAsString(FROM_ATTR_NAME);
    }

    /**
     * Sets the value of the "from" attribute of this "transfer" element.
     *
     * @param from the value of the "from" attribute of this "transfer" element
     */
    public void setFrom(String from)
    {
        setAttribute(FROM_ATTR_NAME, from);
    }

    /**
     * Gets the value of the "sid" attribute of this "transfer" element.
     *
     * @return the value of the "sid" attribute of this "transfer" element
     */
    public String getSID()
    {
        return getAttributeAsString(SID_ATTR_NAME);
    }

    /**
     * Sets the value of the "sid" attribute of this "transfer" element.
     *
     * @param sid the value of the "sid" attribute of this "transfer" element
     */
    public void setSID(String sid)
    {
        setAttribute(SID_ATTR_NAME, sid);
    }

    /**
     * Gets the value of the "to" attribute of this "transfer" element.
     *
     * @return the value of the "to" attribute of this "transfer" element
     */
    public String getTo()
    {
        return getAttributeAsString(TO_ATTR_NAME);
    }

    /**
     * Sets the value of the "to" attribute of this "transfer" element.
     *
     * @param to the value of the "to" attribute of this "transfer" element
     */
    public void setTo(String to)
    {
        setAttribute(TO_ATTR_NAME, to);
    }
}
