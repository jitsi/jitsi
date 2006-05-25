/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.icq.message.auth;

import java.util.*;

import net.kano.joscar.*;
import net.kano.joscar.flapcmd.*;
import net.kano.joscar.snac.*;

/**
 * Handles incoming future athorizations
 *
 * @author Damian Minkov
 */
public class AuthFutureCmdFactory
    implements SnacCmdFactory
{
    protected static final List SUPPORTED_TYPES =
        DefensiveTools.asUnmodifiableList(new CmdType[]
                                          {new CmdType(0x13, 0x15)});

    /**
     * Attempts to convert the given SNAC packet to a
     * <code>SnacCommand</code>.
     *
     * @param packet the packet to use for generation of a
     *   <code>SnacCommand</code>
     * @return an appropriate <code>SnacCommand</code> for representing the
     *   given <code>SnacPacket</code>, or <code>null</code> if no such
     *   object can be created
     */
    public SnacCommand genSnacCommand(SnacPacket packet)
    {
        return new AuthFutureCmd(packet);
    }

    /**
     * Returns a list of the SNAC command types this factory can possibly
     * convert to <code>SnacCommand</code>s.
     *
     * @return a list of command types that can be passed to
     *   <code>genSnacCommand</code>
     */
    public List getSupportedTypes()
    {
        return SUPPORTED_TYPES;
    }
}
