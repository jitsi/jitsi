/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber.extensions.jingle;

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
        JingleIQ ringing = new JingleIQ();

        ringing.setTo(sessionInitiate.getFrom());
        ringing.setFrom(sessionInitiate.getTo());
        ringing.setType(IQ.Type.SET);

        ringing.setSID(sessionInitiate.getSID());
        ringing.setAction(JingleAction.SESSION_INFO);

        SessionInfoPacketExtension ringingPayload
            = new SessionInfoPacketExtension(SessionInfoType.ringing);

        ringing.setSessionInfo(ringingPayload);

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
        return createSessionTerminate(from, to, sid,
                        Reason.BUSY, null);
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
        return createSessionTerminate(from, to, sid, Reason.CANCEL,
                        "Oops!");
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
}
