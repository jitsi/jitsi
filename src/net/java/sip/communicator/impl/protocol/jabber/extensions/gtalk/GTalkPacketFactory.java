/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber.extensions.gtalk;

import java.util.*;

import net.java.sip.communicator.impl.protocol.jabber.extensions.jingle.*;
import net.java.sip.communicator.impl.protocol.jabber.extensions.jingle.CandidateType;

import org.ice4j.*;
import org.ice4j.ice.*;
import org.jivesoftware.smack.packet.*;

/**
 * A utility class containing methods for creating {@link SessionIQ}
 * instances for various situations.
 *
 * @author Sebastien Vincent
 */
public class GTalkPacketFactory
{
    /**
     * Creates a {@link SessionIQ} <tt>reject</tt> packet.
     *
     * @param from our JID
     * @param to the destination JID
     * @param sid the ID of the Google Talk session that this message will be
     * rejecting.
     *
     * @return a {@link SessionIQ} <tt>reject</tt> packet.
     */
    public static SessionIQ createReject(String from, String to, String sid)
    {
        SessionIQ reject = new SessionIQ();

        reject.setTo(to);
        reject.setFrom(from);
        reject.setType(IQ.Type.SET);

        reject.setID(sid);
        reject.setGTalkType(GTalkType.REJECT);

        return reject;
    }

    /**
     * Creates a {@link SessionIQ} <tt>terminate</tt> packet carrying a
     * {@link Reason#BUSY} payload.
     *
     * @param from our JID
     * @param to the destination JID
     * @param sid the ID of the Google Talk session that this message will be
     * terminating.
     *
     * @return a {@link SessionIQ} <tt>terminate</tt> packet.
     */
    public static SessionIQ createBusy(String from, String to, String sid)
    {
        return createSessionTerminate(from, to, sid, Reason.BUSY, null);
    }

    /**
     * Creates a {@link SessionIQ} <tt>session-terminate</tt> packet that is
     * meant to terminate an on-going, well established session (similar to a SIP
     * BYE request).
     *
     * @param from our JID
     * @param to the destination JID
     * @param sid the ID of the Google Talk session that this message will be
     * terminating.
     *
     * @return a {@link SessionIQ} <tt>terminate</tt> packet
     * .
     */
    public static SessionIQ createBye(String from, String to, String sid)
    {
        return createSessionTerminate(from, to, sid, Reason.SUCCESS,
                        "Nice talking to you!");
    }

    /**
     * Creates a {@link SessionIQ} <tt>terminate</tt> packet that is
     * meant to terminate a not yet established session.
     *
     * @param from our JID
     * @param to the destination JID
     * @param sid the ID of the Google Talk session that this message will be
     * terminating.
     *
     * @return a {@link SessionIQ} <tt>terminate</tt> packet
     * .
     */
    public static SessionIQ createCancel(String from, String to, String sid)
    {
        return createSessionTerminate(from, to, sid, Reason.CANCEL, "Oops!");
    }

    /**
     * Creates a {@link SessionIQ} <tt>terminate</tt> packet with the
     * specified src, dst, sid, and reason.
     *
     * @param from our JID
     * @param to the destination JID
     * @param sid the ID of the Google Talk session that this message will be
     * terminating.
     * @param reason the reason for the termination
     * @param reasonText a human readable reason for the termination or
     * <tt>null</tt> for none.
     *
     * @return the newly constructed {@link SessionIQ} <tt>terminate</tt>
     * packet.
     * .
     */
    public static SessionIQ createSessionTerminate(String from,
                                                  String to,
                                                  String sid,
                                                  Reason reason,
                                                  String reasonText)
    {
        SessionIQ terminate = new SessionIQ();

        terminate.setTo(to);
        terminate.setFrom(from);
        terminate.setType(IQ.Type.SET);

        terminate.setID(sid);
        terminate.setGTalkType(GTalkType.TERMINATE);

        ReasonPacketExtension reasonPacketExt
            = new ReasonPacketExtension(reason, reasonText, null);

        terminate.setReason(reasonPacketExt);

        return terminate;
    }

    /**
     * Creates a {@link SessionIQ} <tt>accept</tt> packet with the
     * specified <tt>from</tt>, <tt>to</tt>, <tt>sid</tt>, and <tt>content</tt>.
     * Given our role in a conversation, we would assume that the <tt>from</tt>
     * value should also be used for the value of the Google Talk <tt>responder</tt>.
     *
     * @param from our JID
     * @param to the destination JID
     * @param sid the ID of the Google Talk session that this message will be
     * terminating.
     * @param description description containing payload types list
     * descriptions.
     *
     * @return the newly constructed {@link SessionIQ} <tt>accept</tt>
     * packet.
     * .
     */
    public static SessionIQ createSessionAccept(
            String                           from,
            String                           to,
            String                           sid,
            RtpDescriptionPacketExtension	 description)
    {
        SessionIQ sessionAccept = new SessionIQ();

        sessionAccept.setTo(to);
        sessionAccept.setFrom(from);
        sessionAccept.setType(IQ.Type.SET);
        sessionAccept.setInitiator(to);
        sessionAccept.setID(sid);
        sessionAccept.setGTalkType(GTalkType.ACCEPT);
        sessionAccept.addExtension(description);

        return sessionAccept;
    }

    /**
     * Creates a new {@link SessionIQ} with the <tt>initiate</tt> type.
     *
     * @param from our JID
     * @param to the destination JID
     * @param sid the ID of the Google Talk session that this message will be
     * terminating.
     * @param description description containing payload types list.
     *
     * @return the newly constructed {@link SessionIQ} <tt>terminate</tt>
     * packet.
     */
    public static SessionIQ createSessionInitiate(
            String  	                     from,
            String      	                 to,
            String          	             sid,
            RtpDescriptionPacketExtension	 description)
    {
        SessionIQ sessionInitiate = new SessionIQ();

        sessionInitiate.setTo(to);
        sessionInitiate.setFrom(from);
        sessionInitiate.setInitiator(from);
        sessionInitiate.setType(IQ.Type.SET);

        sessionInitiate.setID(sid);
        sessionInitiate.setGTalkType(GTalkType.INITIATE);
        sessionInitiate.addExtension(description);

        return sessionInitiate;
    }

    /**
     * Creates a new {@link SessionIQ} with the <tt>candidates</tt> type.
     *
     * @param from our JID
     * @param to the destination JID
     * @param sid the ID of the Google Talk session.
     * @param candidate a <tt>GTalkCandidatePacketExtension</tt>.
     *
     * @return the newly constructed {@link SessionIQ} <tt>terminate</tt>
     * packet.
     */
    public static SessionIQ createSessionCandidates(
            String  	                     from,
            String      	                 to,
            String          	             sid,
            GTalkCandidatePacketExtension	 candidate)
    {
        SessionIQ sessionInitiate = new SessionIQ();

        sessionInitiate.setTo(to);
        sessionInitiate.setFrom(from);
        sessionInitiate.setInitiator(from);
        sessionInitiate.setType(IQ.Type.SET);

        sessionInitiate.setID(sid);
        sessionInitiate.setGTalkType(GTalkType.CANDIDATES);
        sessionInitiate.addExtension(candidate);

        return sessionInitiate;
    }

    /**
     * Converts the ICE media <tt>stream</tt> and its local candidates into a
     * list of Google Talk candidates.
     *
     * @param name of the stream
     * @param stream the {@link IceMediaStream} that we'd like to describe in
     * XML.
     *
     * @return the list of Google Talk candidates
     */
    public static List<GTalkCandidatePacketExtension> createCandidates(
                                                        String name,
                                                        IceMediaStream stream)
    {
        List<GTalkCandidatePacketExtension> exts = new
            ArrayList<GTalkCandidatePacketExtension>();

        for(Component component : stream.getComponents())
        {
            String mediaName = null;
            if(name.equals("rtp"))
            {
                if(component.getComponentID() == 1)
                {
                    mediaName = name;
                }
                else
                {
                    mediaName = "rtcp";
                    // Audio RTCP is never used in Google Talk and it is also
                    // never transmitted by Gmail client
                }
            }
            else if(name.equals("video_rtp"))
            {
                if(component.getComponentID() == 1)
                {
                    mediaName = name;
                }
                else
                {
                    mediaName = "video_rtcp";
                }
            }

            for(Candidate candidate : component.getLocalCandidates())
            {
                exts.add(createCandidate(candidate, mediaName, stream));
            }
        }

        return exts;
    }

    /**
     * Creates a {@link GTalkCandidatePacketExtension} and initializes it so
     * that it would describe the state of <tt>candidate</tt>
     *
     * @param candidate the ICE4J {@link Candidate} that we'd like to convert
     * into an Google Talk packet extension.
     * @param name name of the candidate extension
     * @param stream ICE stream
     *
     * @return a new {@link GTalkCandidatePacketExtension} corresponding to the
     * state of the <tt>candidate</tt> candidate.
     */
    private static GTalkCandidatePacketExtension createCandidate(
            Candidate candidate, String name, IceMediaStream stream)
    {
        GTalkCandidatePacketExtension packet =
            new GTalkCandidatePacketExtension();

        Component component = candidate.getParentComponent();

        packet.setName(name);
        packet.setGeneration(
                component.getParentStream().getParentAgent().getGeneration());

        TransportAddress transportAddress = candidate.getTransportAddress();

        // different username/password for each candidate ?
        packet.setUsername(((LocalCandidate)candidate).getUfrag());
        packet.setPassword("");
        packet.setAddress(transportAddress.getHostAddress());
        packet.setPort(transportAddress.getPort());
        if(transportAddress.getPort() != 443)
        {
            packet.setProtocol(candidate.getTransport().toString());
        }
        else
        {
            packet.setProtocol("ssltcp");
        }
        packet.setNetwork(0);
        packet.setFoundation(0);
        packet.setComponent(component.getComponentID());

        CandidateType candType = CandidateType.valueOf(
                candidate.getType().toString());

        if(candType == CandidateType.srflx)
        {
            candType = CandidateType.stun;
        }
        else if(candType == CandidateType.host)
        {
            candType = CandidateType.local;
        }

        packet.setType(candType);
        double priority = candidate.getPriority();
        packet.setPreference((priority / 1000));

        return packet;
    }
}
