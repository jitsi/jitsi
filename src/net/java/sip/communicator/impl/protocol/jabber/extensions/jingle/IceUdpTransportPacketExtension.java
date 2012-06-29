/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber.extensions.jingle;

import java.util.*;

import net.java.sip.communicator.impl.protocol.jabber.extensions.*;

import org.jivesoftware.smack.packet.*;

/**
 * An {@link AbstractPacketExtension} implementation for transport elements.
 *
 * @author Emil Ivov
 * @author Lyubomir Marinov
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
     * Creates a new {@link IceUdpTransportPacketExtension} instance with the
     * specified <tt>namespace</tt> and <tt>elementName</tt>. The purpose of
     * this method is to allow {@link RawUdpTransportPacketExtension} to
     * extend this class.
     *
     * @param namespace the XML namespace that the instance should belong to.
     * @param elementName the name of the element that we would be representing.
     */
    protected IceUdpTransportPacketExtension(String namespace,
                                             String elementName)
    {
        super(namespace, elementName);
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
        synchronized (candidateList)
        {
            if(candidateList.size() > 0)
            {
                return candidateList;
            }
            else if (remoteCandidate != null)
            {
                List<RemoteCandidatePacketExtension> list
                    = new ArrayList<RemoteCandidatePacketExtension>();
                list.add(remoteCandidate);

                return list;
            }
        }

        //there are apparently no child elements.
        return null;
    }

    /**
     * Adds <tt>candidate</tt> to the list of {@link CandidatePacketExtension}s
     * registered with this transport.
     *
     * @param candidate the new {@link CandidatePacketExtension} to add to this
     * transport element.
     */
    public void addCandidate(CandidatePacketExtension candidate)
    {
        synchronized(candidateList)
        {
            candidateList.add(candidate);
        }
    }

    /**
     * Removes <tt>candidate</tt> from the list of
     * {@link CandidatePacketExtension}s registered with this transport.
     *
     * @param candidate the <tt>CandidatePacketExtension</tt> to remove from
     * this transport element
     * @return <tt>true</tt> if the list of <tt>CandidatePacketExtension</tt>s
     * registered with this transport contained the specified <tt>candidate</tt>
     */
    public boolean removeCandidate(CandidatePacketExtension candidate)
    {
        synchronized (candidateList)
        {
            return candidateList.remove(candidate);
        }
    }

    /**
     * Returns the list of {@link CandidatePacketExtension}s currently
     * registered with this transport.
     *
     * @return the list of {@link CandidatePacketExtension}s currently
     * registered with this transport.
     */
    public List<CandidatePacketExtension> getCandidateList()
    {
        synchronized(candidateList)
        {
            return new ArrayList<CandidatePacketExtension>(this.candidateList);
        }
    }

    /**
     * Sets <tt>candidate</tt> as the in-use candidate after ICE has terminated.
     *
     * @param candidate the new {@link CandidatePacketExtension} to set as an
     * in-use candidate for this session.
     */
    public void setRemoteCandidate(RemoteCandidatePacketExtension candidate)
    {
        this.remoteCandidate = candidate;
    }

    /**
     * Returns the in-use <tt>candidate</tt> for this session.
     *
     * @return Returns the in-use <tt>candidate</tt> for this session.
     */
    public RemoteCandidatePacketExtension getRemoteCandidate()
    {
        return remoteCandidate;
    }

    /**
     * Tries to determine whether  <tt>childExtension</tt> is a {@link
     * CandidatePacketExtension}, a {@link RemoteCandidatePacketExtension} or
     * something else and then adds it as such.
     *
     *  @param childExtension the extension we'd like to add here.
     */
    @Override
    public void addChildExtension(PacketExtension childExtension)
    {
        //first check for RemoteCandidate because they extend Candidate.
        if(childExtension instanceof RemoteCandidatePacketExtension)
            setRemoteCandidate((RemoteCandidatePacketExtension) childExtension);

        else if(childExtension instanceof CandidatePacketExtension)
            addCandidate((CandidatePacketExtension) childExtension);
    }
}
