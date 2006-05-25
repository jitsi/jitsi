/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.icq.message.auth;

import net.kano.joscar.snaccmd.ssi.*;

/**
 * All commands for auhtorization subclass this abstract one
 * which holds all the command codes
 *
 * @author Damian Minkov
 */
public abstract class AbstractAuthCommand
    extends SsiCommand
{
    /** A command subtype for command used
     * to send future authorization grant to client you
     * just added to your contact list.
     * And client will be able to add you in its contact list later
     * without asking permissions.  */
    public static final int CMD_AUTH_FUTURE = 0x0014;

    public static final int CMD_AUTH_FUTURE_GRANTED = 0x0015;

    /** A command subtype.
     * Use this command to send authorization request. */
    public static final int CMD_AUTH_REQUEST = 0x0018;

    public static final int CMD_AUTH_REPLY = 0x001a;

    protected AbstractAuthCommand(int command)
    {
        super(command);
    }
}
