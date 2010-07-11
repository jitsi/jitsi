/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber.extensions.jingle;

import java.util.*;

import org.jivesoftware.smack.packet.*;

import net.java.sip.communicator.impl.protocol.jabber.extensions.*;

/**
 * An {@link AbstractPacketExtension} implementation for transport elements.
 *
 * @author Emil Ivov
 */
public class IceUdpTransportPacketExtension extends AbstractPacketExtension
{
    /**
     * The name of the "transport" element.
     */
    public static final String NAMESPACE
        = "urn:xmpp:jingle:transports:ice-udp:1";
    /**
     * The name of the "transport" element.
     */
    public static final String ELEMENT_NAME = "transport";

    /**
     * The name of the <tt>pwd</tt> ICE attribute.
     */
    public static final String PWD_ATTR_NAME = "pwd";

    /**
     * The name of the <tt>ufrag</tt> ICE attribute.
     */
    public static final String UFRAG_ATTR_NAME = "ufrag";

    /**
     * A list of one or more candidates representing each of the initiator's
     * higher-priority transport candidates as determined in accordance with
     * the ICE methodology.
     */
    private final List<CandidatePacketExtension> candidateList
        = new ArrayList<CandidatePacketExtension>();

    /**
     * Once the parties have connectivity and therefore the initiator has
     * completed ICE as explained in RFC 5245, the initiator MAY communicate
     * the in-use candidate pair in the signalling channel by sending a
     * transport-info message that contains a "remote-candidate" element
     */
    private RemoteCandidatePacketExtension remoteCandidate;

    /**
     * Creates a new {@link IceUdpTransportPacketExtension} instance.
     */
    public IceUdpTransportPacketExtension()
    {
        super(NAMESPACE, ELEMENT_NAME);
    }

    /**
     * Sets the ICE defined password attribute.
     *
     * @param pwd a password <tt>String</tt> as defined in RFC 5245
     */
    public void setPassword(String pwd)
    {
        super.setAttribute(PWD_ATTR_NAME, pwd);
    }

    /**
     * Returns the ICE defined password attribute.
     *
     * @return a password <tt>String</tt> as defined in RFC 5245
     */
    public String getPassword()
    {
        return super.getAttributeAsString(PWD_ATTR_NAME);
    }

    /**
     * Sets the ICE defined user fragment attribute.
     *
     * @param ufrag a user fragment <tt>String</tt> as defined in RFC 5245
     */
    public void setUfrag(String ufrag)
    {
        super.setAttribute(UFRAG_ATTR_NAME, ufrag);
    }

    /**
     * Returns the ICE defined user fragment attribute.
     *
     * @return a user fragment <tt>String</tt> as defined in RFC 5245
     */
    public String getUfrag()
    {
        return super.getAttributeAsString(UFRAG_ATTR_NAME);
    }

    /**
     * Returns this element's child (local or remote) candidate elements.
     *
     * @return this element's child (local or remote) candidate elements.
     */
    @Override
    public List<? extends PacketExtension> getChildExtensions()
    {
        if(candidateList.size() > 0)
            return candidateList;
        else if (remoteCandidate != null)
        {
            List<RemoteCandidatePacketExtension> list
                = new ArrayList<RemoteCandidatePacketExtension>();
            list.add(remoteCandidate);

            return list;
        }

        //there are apparently no child elements.
        return null;
    }



}
