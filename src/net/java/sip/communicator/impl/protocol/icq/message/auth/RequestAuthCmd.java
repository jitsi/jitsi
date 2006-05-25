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
 * Command requesting authorization
 * @author Damian Minkov
 */
public class RequestAuthCmd
    extends AbstractAuthCommand
{
    private String uin;
    private String reason;

    public RequestAuthCmd(String uin, String reason)
    {
        super(CMD_AUTH_REQUEST);

        this.uin = uin;
        this.reason = reason;
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

        if (reason == null)
        {
            reason = "";
        }

        byte[] reasonBytes = BinaryTools.getAsciiBytes(reason);
        BinaryTools.writeUShort(out, reasonBytes.length);
        out.write(reasonBytes);
    }
}
