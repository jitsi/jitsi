/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber.extensions.jingle;

import java.util.*;

import org.jivesoftware.smack.packet.*;

/**
 * A utility class containing methods for creating {@link JingleIQ}
 * instances for various situations.
 *
 * @author Emil Ivov
 */
public class JinglePacketFactory
{
    /**
     * Creates a {@link JingleIQ} <tt>session-info</tt> packet carrying a
     * <tt>ringing</tt> payload.
     *
     * @param sessionInitiate the {@link JingleIQ} that established the session
     * which the response is going to belong to.
     *
     * @return a {@link JingleIQ} <tt>session-info</tt> packet carrying a
     * <tt>ringing</tt> payload.
     */
    public static JingleIQ createRinging(JingleIQ sessionInitiate)
    {
        return createSessionInfo(sessionInitiate.getTo(),
                                 sessionInitiate.getFrom(),
                                 sessionInitiate.getSID(),
                                 SessionInfoType.ringing);
    }

    /**
     * Creates a {@link JingleIQ} <tt>session-info</tt> packet carrying a
     * the specified payload type.
     *
     * @param from our full jid
     * @param to their full jid
     * @param sid the ID of the Jingle session this IQ will belong to.
     * @param type the exact type (e.g. ringing, hold, mute) of the session
     * info IQ.
     *
     * @return a {@link JingleIQ} <tt>session-info</tt> packet carrying a
     * the specified payload type.
     */
    public static JingleIQ createSessionInfo(String          from,
                                             String          to,
                                             String          sid,
                                             SessionInfoType type)
    {
        JingleIQ ringing = new JingleIQ();

        ringing.setFrom(from);
        ringing.setTo(to);
        ringing.setType(IQ.Type.SET);

        ringing.setSID(sid);
        ringing.setAction(JingleAction.SESSION_INFO);

        SessionInfoPacketExtension sessionInfoType
            = new SessionInfoPacketExtension(type);

        ringing.setSessionInfo(sessionInfoType);

        return ringing;
    }

    /**
     * Creates a {@link JingleIQ} <tt>session-terminate</tt> packet carrying a
     * {@link Reason#BUSY} payload.
     *
     * @param from our JID
     * @param to the destination JID
     * @param sid the ID of the Jingle session that this message will be
     * terminating.
     *
     * @return a {@link JingleIQ} <tt>session-terminate</tt> packet.
     */
    public static JingleIQ createBusy(String from, String to, String sid)
    {
        return createSessionTerminate(from, to, sid, Reason.BUSY, null);
    }

    /**
     * Creates a {@link JingleIQ} <tt>session-terminate</tt> packet that is
     * meant to terminate an on-going, well established session (similar to a SIP
     * BYE request).
     *
     * @param from our JID
     * @param to the destination JID
     * @param sid the ID of the Jingle session that this message will be
     * terminating.
     *
     * @return a {@link JingleIQ} <tt>session-terminate</tt> packet
     * .
     */
    public static JingleIQ createBye(String from, String to, String sid)
    {
        return createSessionTerminate(from, to, sid, Reason.SUCCESS,
                        "Nice talking to you!");
    }

    /**
     * Creates a {@link JingleIQ} <tt>session-terminate</tt> packet that is
     * meant to terminate a not yet established session.
     *
     * @param from our JID
     * @param to the destination JID
     * @param sid the ID of the Jingle session that this message will be
     * terminating.
     *
     * @return a {@link JingleIQ} <tt>session-terminate</tt> packet
     * .
     */
    public static JingleIQ createCancel(String from, String to, String sid)
    {
        return createSessionTerminate(from, to, sid, Reason.CANCEL, "Oops!");
    }

    /**
     * Creates a {@link JingleIQ} <tt>session-terminate</tt> packet with the
     * specified src, dst, sid, and reason.
     *
     * @param from our JID
     * @param to the destination JID
     * @param sid the ID of the Jingle session that this message will be
     * terminating.
     * @param reason the reason for the termination
     * @param reasonText a human readable reason for the termination or
     * <tt>null</tt> for none.
     *
     * @return the newly constructed {@link JingleIQ} <tt>session-terminate</tt>
     * packet.
     * .
     */
    public static JingleIQ createSessionTerminate(String from,
                                                  String to,
                                                  String sid,
                                                  Reason reason,
                                                  String reasonText)
    {
        JingleIQ terminate = new JingleIQ();

        terminate.setTo(to);
        terminate.setFrom(from);
        terminate.setType(IQ.Type.SET);

        terminate.setSID(sid);
        terminate.setAction(JingleAction.SESSION_TERMINATE);

        ReasonPacketExtension reasonPacketExt
            = new ReasonPacketExtension(reason, reasonText, null);

        terminate.setReason(reasonPacketExt);

        return terminate;
    }

    /**
     * Creates a {@link JingleIQ} <tt>session-accept</tt> packet with the
     * specified <tt>from</tt>, <tt>to</tt>, <tt>sid</tt>, and <tt>content</tt>.
     * Given our role in a conversation, we would assume that the <tt>from</tt>
     * value should also be used for the value of the Jingle <tt>responder</tt>.
     *
     * @param from our JID
     * @param to the destination JID
     * @param sid the ID of the Jingle session that this message will be
     * terminating.
     * @param contentList the content elements containing media and transport
     * descriptions.
     *
     * @return the newly constructed {@link JingleIQ} <tt>session-terminate</tt>
     * packet.
     * .
     */
    public static JingleIQ createSessionAccept(
            String                           from,
            String                           to,
            String                           sid,
            Iterable<ContentPacketExtension> contentList)
    {
        JingleIQ sessionAccept = new JingleIQ();

        sessionAccept.setTo(to);
        sessionAccept.setFrom(from);
        sessionAccept.setResponder(from);
        sessionAccept.setType(IQ.Type.SET);

        sessionAccept.setSID(sid);
        sessionAccept.setAction(JingleAction.SESSION_ACCEPT);

        for(ContentPacketExtension content : contentList)
            sessionAccept.addContent(content);

        return sessionAccept;
    }

    /**
     * Creates a new {@link JingleIQ} with the <tt>session-initiate</tt> action.
     *
     * @param from our JID
     * @param to the destination JID
     * @param sid the ID of the Jingle session that this message will be
     * terminating.
     * @param contentList the content elements containing media and transport
     * descriptions.
     *
     * @return the newly constructed {@link JingleIQ} <tt>session-terminate</tt>
     * packet.
     */
    public static JingleIQ createSessionInitiate(
                                    String                       from,
                                    String                       to,
                                    String                       sid,
                                    List<ContentPacketExtension> contentList)
    {
        JingleIQ sessionInitiate = new JingleIQ();

        sessionInitiate.setTo(to);
        sessionInitiate.setFrom(from);
        sessionInitiate.setInitiator(from);
        sessionInitiate.setType(IQ.Type.SET);

        sessionInitiate.setSID(sid);
        sessionInitiate.setAction(JingleAction.SESSION_INITIATE);

        for(ContentPacketExtension content : contentList)
        {
            sessionInitiate.addContent(content);
        }

        return sessionInitiate;
    }

    /**
     * Creates a new {@link JingleIQ} with the <tt>content-add</tt> action.
     *
     * @param from our JID
     * @param to the destination JID
     * @param sid the ID of the Jingle session that this message will be
     * terminating.
     * @param contentList the content elements containing media and transport
     * descriptions.
     *
     * @return the newly constructed {@link JingleIQ} <tt>content-add</tt>
     * packet.
     */
    public static JingleIQ createContentAdd(
                                    String                       from,
                                    String                       to,
                                    String                       sid,
                                    List<ContentPacketExtension> contentList)
    {
        JingleIQ contentAdd = new JingleIQ();

        contentAdd.setTo(to);
        contentAdd.setFrom(from);
        contentAdd.setInitiator(from);
        contentAdd.setType(IQ.Type.SET);

        contentAdd.setSID(sid);
        contentAdd.setAction(JingleAction.CONTENT_ADD);

        for(ContentPacketExtension content : contentList)
            contentAdd.addContent(content);

        return contentAdd;
    }

    /**
     * Creates a new {@link JingleIQ} with the <tt>content-accept</tt> action.
     *
     * @param from our JID
     * @param to the destination JID
     * @param sid the ID of the Jingle session that this message will be
     * terminating.
     * @param contentList the content elements containing media and transport
     * descriptions.
     *
     * @return the newly constructed {@link JingleIQ} <tt>content-accept</tt>
     * packet.
     */
    public static JingleIQ createContentAccept(
            String                           from,
            String                           to,
            String                           sid,
            Iterable<ContentPacketExtension> contentList)
    {
        JingleIQ contentAccept = new JingleIQ();

        contentAccept.setTo(to);
        contentAccept.setFrom(from);
        contentAccept.setInitiator(from);
        contentAccept.setType(IQ.Type.SET);

        contentAccept.setSID(sid);
        contentAccept.setAction(JingleAction.CONTENT_ACCEPT);

        for(ContentPacketExtension content : contentList)
            contentAccept.addContent(content);

        return contentAccept;
    }

    /**
     * Creates a new {@link JingleIQ} with the <tt>content-reject</tt> action.
     *
     * @param from our JID
     * @param to the destination JID
     * @param sid the ID of the Jingle session that this message will be
     * terminating.
     * @param contentList the content elements containing media and transport
     * descriptions.
     *
     * @return the newly constructed {@link JingleIQ} <tt>content-reject</tt>
     * packet.
     */
    public static JingleIQ createContentReject(
            String                           from,
            String                           to,
            String                           sid,
            Iterable<ContentPacketExtension> contentList)
    {
        JingleIQ contentReject = new JingleIQ();

        contentReject.setTo(to);
        contentReject.setFrom(from);
        contentReject.setInitiator(from);
        contentReject.setType(IQ.Type.SET);

        contentReject.setSID(sid);
        contentReject.setAction(JingleAction.CONTENT_REJECT);

        if (contentList != null)
        {
            for(ContentPacketExtension content : contentList)
                contentReject.addContent(content);
        }

        return contentReject;
    }

    /**
     * Creates a new {@link JingleIQ} with the <tt>content-modify</tt> action.
     *
     * @param from our JID
     * @param to the destination JID
     * @param sid the ID of the Jingle session that this message will be
     * terminating.
     * @param content the content element containing media and transport
     * description.
     *
     * @return the newly constructed {@link JingleIQ} <tt>content-modify</tt>
     * packet.
     */
    public static JingleIQ createContentModify(
                                    String                       from,
                                    String                       to,
                                    String                       sid,
                                    ContentPacketExtension       content)
    {
        JingleIQ contentModify = new JingleIQ();

        contentModify.setTo(to);
        contentModify.setFrom(from);
        contentModify.setInitiator(from);
        contentModify.setType(IQ.Type.SET);

        contentModify.setSID(sid);
        contentModify.setAction(JingleAction.CONTENT_MODIFY);

        contentModify.addContent(content);

        return contentModify;
    }

    /**
     * Creates a new {@link JingleIQ} with the <tt>content-remove</tt> action.
     *
     * @param from our JID
     * @param to the destination JID
     * @param sid the ID of the Jingle session that this message will be
     * terminating.
     * @param contentList the content elements containing media and transport
     * descriptions.
     *
     * @return the newly constructed {@link JingleIQ} <tt>content-remove</tt>
     * packet.
     */
    public static JingleIQ createContentRemove(
                                    String                       from,
                                    String                       to,
                                    String                       sid,
                                    List<ContentPacketExtension> contentList)
    {
        JingleIQ contentRemove = new JingleIQ();

        contentRemove.setTo(to);
        contentRemove.setFrom(from);
        contentRemove.setInitiator(from);
        contentRemove.setType(IQ.Type.SET);

        contentRemove.setSID(sid);
        contentRemove.setAction(JingleAction.CONTENT_REMOVE);

        for(ContentPacketExtension content : contentList)
        {
            contentRemove.addContent(content);
        }

        return contentRemove;
    }
}
