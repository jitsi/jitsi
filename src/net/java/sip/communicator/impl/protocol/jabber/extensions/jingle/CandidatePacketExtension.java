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
import org.ice4j.ice.*;

/**
 * @author Emil Ivov
 */
public class CandidatePacketExtension extends AbstractPacketExtension
    implements Comparable<CandidatePacketExtension>
{
    /**
     * The name of the "candidate" element.
     */
    public static final String ELEMENT_NAME = "candidate";

    /**
     * The name of the "component" element.
     */
    public static final String COMPONENT_ATTR_NAME = "component";

    /**
     * The "component" ID for RTP components.
     */
    public static final int RTP_COMPONENT_ID = 1;

    /**
     * The "component" ID for RTCP components.
     */
    public static final int RTCP_COMPONENT_ID = 2;

    /**
     * The name of the "foundation" element.
     */
    public static final String FOUNDATION_ATTR_NAME = "foundation";

    /**
     * The name of the "generation" element.
     */
    public static final String GENERATION_ATTR_NAME = "generation";

    /**
     * The name of the "id" element.
     */
    public static final String ID_ATTR_NAME = "id";

    /**
     * The name of the "ip" element.
     */
    public static final String IP_ATTR_NAME = "ip";

    /**
     * The name of the "network" element.
     */
    public static final String NETWORK_ATTR_NAME = "network";

    /**
     * The name of the "port" element.
     */
    public static final String PORT_ATTR_NAME = "port";

    /**
     * The name of the "priority" element.
     */
    public static final String PRIORITY_ATTR_NAME = "priority";

    /**
     * The name of the "protocol" element.
     */
    public static final String PROTOCOL_ATTR_NAME = "protocol";

    /**
     * The name of the "rel-addr" element.
     */
    public static final String REL_ADDR_ATTR_NAME = "rel-addr";

    /**
     * The name of the "rel-port" element.
     */
    public static final String REL_PORT_ATTR_NAME = "rel-port";

    /**
     * The name of the "type" element.
     */
    public static final String TYPE_ATTR_NAME = "type";

    /**
     * The name of the "tcptype" element.
     */
    public static final String TCPTYPE_ATTR_NAME = "tcptype";

    /**
     * Creates a new {@link CandidatePacketExtension}
     */
    public CandidatePacketExtension()
    {
        super(null, ELEMENT_NAME);
    }

    /**
     * Creates a new {@link CandidatePacketExtension} with the specified
     * <tt>elementName</tt> so that this class would be usable as a
     * <tt>RemoteCandidatePacketExtension</tt> parent.
     *
     * @param elementName the element name that this instance should be using.
     */
    protected CandidatePacketExtension(String elementName)
    {
        super(null, elementName);
    }

    /**
     * Sets a component ID as defined in ICE-CORE.
     *
     * @param component a component ID as defined in ICE-CORE.
     */
    public void setComponent(int component)
    {
        super.setAttribute(COMPONENT_ATTR_NAME, component);
    }

    /**
     * Returns a component ID as defined in ICE-CORE.
     *
     * @return a component ID as defined in ICE-CORE.
     */
    public int getComponent()
    {
        return super.getAttributeAsInt(COMPONENT_ATTR_NAME);
    }

    /**
     * Sets the candidate foundation as defined in ICE-CORE.
     *
     * @param foundation the candidate foundation as defined in ICE-CORE.
     */
    public void setFoundation(String foundation)
    {
        super.setAttribute(FOUNDATION_ATTR_NAME, foundation);
    }

    /**
     * Returns the candidate foundation as defined in ICE-CORE.
     *
     * @return the candidate foundation as defined in ICE-CORE.
     */
    public String getFoundation()
    {
        return super.getAttributeAsString(FOUNDATION_ATTR_NAME);
    }

    /**
     * Sets this candidate's generation index. A generation is an index,
     * starting at 0, that enables the parties to keep track of updates to the
     * candidate throughout the life of the session. For details, see the ICE
     * Restarts section of XEP-0176.
     *
     * @param generation this candidate's generation index.
     */
    public void setGeneration(int generation)
    {
        super.setAttribute(GENERATION_ATTR_NAME, generation);
    }

    /**
     * Returns this candidate's generation. A generation is an index, starting at
     * 0, that enables the parties to keep track of updates to the candidate
     * throughout the life of the session. For details, see the ICE Restarts
     * section of XEP-0176.
     *
     * @return this candidate's generation index.
     */
    public int getGeneration()
    {
        return super.getAttributeAsInt(GENERATION_ATTR_NAME);
    }

    /**
     * Sets this candidates's unique identifier <tt>String</tt>.
     *
     * @param id this candidates's unique identifier <tt>String</tt>
     */
    public void setID(String id)
    {
        super.setAttribute(ID_ATTR_NAME, id);
    }

    /**
     * Returns this candidates's unique identifier <tt>String</tt>.
     *
     * @return this candidates's unique identifier <tt>String</tt>
     */
    public String getID()
    {
        return super.getAttributeAsString(ID_ATTR_NAME);
    }

    /**
     * Sets this candidate's Internet Protocol (IP) address; this can be either
     * an IPv4 address or an IPv6 address.
     *
     * @param ip this candidate's IPv4 or IPv6 address.
     */
    public void setIP(String ip)
    {
        super.setAttribute(IP_ATTR_NAME, ip);
    }

    /**
     * Returns this candidate's Internet Protocol (IP) address; this can be
     * either an IPv4 address or an IPv6 address.
     *
     * @return this candidate's IPv4 or IPv6 address.
     */
    public String getIP()
    {
        return super.getAttributeAsString(IP_ATTR_NAME);
    }

    /**
     * The network index indicating the interface that the candidate belongs to.
     * The network ID is used for diagnostic purposes only in cases where the
     * calling hardware has more than one Network Interface Card.
     *
     * @param network the network index indicating the interface that the
     * candidate belongs to.
     */
    public void setNetwork(int network)
    {
        super.setAttribute(NETWORK_ATTR_NAME, network);
    }

    /**
     * Returns the network index indicating the interface that the candidate
     * belongs to. The network ID is used for diagnostic purposes only in cases
     * where the calling hardware has more than one Network Interface Card.
     *
     * @return the network index indicating the interface that the candidate
     * belongs to.
     */
    public int getNetwork()
    {
        return super.getAttributeAsInt(NETWORK_ATTR_NAME);
    }

    /**
     * Sets this candidate's port number.
     *
     * @param port this candidate's port number.
     */
    public void setPort(int port)
    {
        super.setAttribute(PORT_ATTR_NAME, port);
    }

    /**
     * Returns this candidate's port number.
     *
     * @return this candidate's port number.
     */
    public int getPort()
    {
        return super.getAttributeAsInt(PORT_ATTR_NAME);
    }

    /**
     * This candidate's priority as defined in ICE's RFC 5245
     *
     * @param priority this candidate's priority
     */
    public void setPriority(long priority)
    {
        super.setAttribute(PRIORITY_ATTR_NAME, priority);
    }

    /**
     * This candidate's priority as defined in ICE's RFC 5245
     *
     * @return this candidate's priority
     */
    public int getPriority()
    {
        return super.getAttributeAsInt(PRIORITY_ATTR_NAME);
    }

    /**
     * Sets this candidate's transport protocol.
     *
     * @param protocol this candidate's transport protocol.
     */
    public void setProtocol(String protocol)
    {
        super.setAttribute(PROTOCOL_ATTR_NAME, protocol);
    }

    /**
     * Sets this candidate's transport protocol.
     *
     * @return this candidate's transport protocol.
     */
    public String getProtocol()
    {
        return super.getAttributeAsString(PROTOCOL_ATTR_NAME);
    }

    /**
     * Sets this candidate's related address as described by ICE's RFC 5245.
     *
     * @param relAddr this candidate's related address as described by ICE's
     * RFC 5245.
     */
    public void setRelAddr(String relAddr)
    {
        super.setAttribute(REL_ADDR_ATTR_NAME, relAddr);
    }

    /**
     * Returns this candidate's related address as described by ICE's RFC 5245.
     *
     * @return this candidate's related address as described by ICE's RFC 5245.
     */
    public String getRelAddr()
    {
        return super.getAttributeAsString(REL_ADDR_ATTR_NAME);
    }

    /**
     * Sets this candidate's related port as described by ICE's RFC 5245.
     *
     * @param relPort this candidate's related port as described by ICE's
     * RFC 5245.
     */
    public void setRelPort(int relPort)
    {
        super.setAttribute(REL_PORT_ATTR_NAME, relPort);
    }

    /**
     * Returns this candidate's related port as described by ICE's RFC 5245.
     *
     * @return this candidate's related port as described by ICE's RFC 5245.
     */
    public int getRelPort()
    {
        return super.getAttributeAsInt(REL_PORT_ATTR_NAME);
    }

    /**
     * Sets a Candidate Type as defined in ICE-CORE. The allowable values are
     * "host" for host candidates, "prflx" for peer reflexive candidates,
     * "relay" for relayed candidates, and "srflx" for server reflexive
     * candidates. All allowable values are enumerated in the {@link
     * CandidateType} enum.
     *
     * @param type this candidates' type as per ICE's RFC 5245.
     */
    public void setType(CandidateType type)
    {
        super.setAttribute(TYPE_ATTR_NAME, type);
    }

    /**
     * Returns a Candidate Type as defined in ICE-CORE. The allowable values are
     * "host" for host candidates, "prflx" for peer reflexive candidates,
     * "relay" for relayed candidates, and "srflx" for server reflexive
     * candidates. All allowable values are enumerated in the {@link
     * CandidateType} enum.
     *
     * @return this candidates' type as per ICE's RFC 5245.
     */
    public CandidateType getType()
    {
        return CandidateType.valueOf(getAttributeAsString(TYPE_ATTR_NAME));
    }

    /**
     * Compares this instance with another CandidatePacketExtension by
     * preference of type: host < local < prflx < srflx < stun < relay.
     *
     * @return 0 if the type are equal. -1 if this instance type is preferred.
     * Otherwise 1.
     */
    public int compareTo(CandidatePacketExtension candidatePacketExtension)
    {
        // If the types are different.
        if(this.getType() != candidatePacketExtension.getType())
        {
            CandidateType[] types = {
                CandidateType.host,
                CandidateType.local,
                CandidateType.prflx,
                CandidateType.srflx,
                CandidateType.stun,
                CandidateType.relay
            };
            for(int i = 0; i < types.length; ++i)
            {
                // this object is preferred.
                if(types[i] == this.getType())
                {
                    return -1;
                }
                // the candidatePacketExtension is preferred.
                else if(types[i] == candidatePacketExtension.getType())
                {
                    return 1;
                }
            }
        }
        // If the types are equal.
        return 0;
    }

    /**
     * Gets the TCP type for this <tt>CandidatePacketExtension</tt>.
     */
    public CandidateTcpType getTcpType()
    {
        String tcpTypeString = getAttributeAsString(TCPTYPE_ATTR_NAME);
        try
        {
            return CandidateTcpType.parse(tcpTypeString);
        }
        catch (IllegalArgumentException iae)
        {
            return null;
        }
    }

    /**
     * Sets the TCP type for this <tt>CandidatePacketExtension</tt>.
     * @param tcpType
     */
    public void setTcpType(CandidateTcpType tcpType)
    {
        setAttribute(TCPTYPE_ATTR_NAME, tcpType.toString());
    }
}
