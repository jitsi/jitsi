/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.icq.message.auth;

import java.io.*;

import net.kano.joscar.*;

/**
 * Sending authorization reply
 *
 * @author Damian Minkov
 */
public class AuthReplyCmd
    extends AbstractAuthCommand
{
    private static int FLAG_AUTH_ACCEPTED = 1;
    private static int FLAG_AUTH_DECLINED = 0;

    private String uin = null;
    private String reason = null;
    private boolean accepted = false;

    public AuthReplyCmd(String uin, String reason, boolean accepted)
    {
        super(CMD_AUTH_REPLY);

        this.uin = uin;
        this.reason = reason;
        this.accepted = accepted;
    }

    /**
     * Writes this command's SNAC data block to the given stream.
     *
     * @param out the stream to which to write the SNAC data
     * @throws IOException if an I/O error occurs
     */
    public void writeData(OutputStream out) throws IOException
    {
        byte[] uinBytes = BinaryTools.getAsciiBytes(uin);
        BinaryTools.writeUByte(out, uinBytes.length);
        out.write(uinBytes);

        if (accepted)
        {
            BinaryTools.writeUByte(out, FLAG_AUTH_ACCEPTED);
        }
        else
        {
            BinaryTools.writeUByte(out, FLAG_AUTH_DECLINED);
        }

        if (reason == null)
        {
            reason = "";
        }

        byte[] reasonBytes = BinaryTools.getAsciiBytes(reason);
        BinaryTools.writeUShort(out, reasonBytes.length);
        out.write(reasonBytes);
    }
}
