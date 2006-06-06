/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.icq.message.auth;

import java.io.*;

import net.java.sip.communicator.impl.protocol.icq.message.common.*;
import net.kano.joscar.*;
import net.kano.joscar.flapcmd.*;

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
     * Incoming Command Requesting our authorization
     * @param packet SnacPacket incoming packet
     */
    public RequestAuthCmd(SnacPacket packet)
    {
        super(CMD_AUTH_REQUEST_RECV);

        ByteBlock messageData = packet.getData();
        // parse data
        int offset = 0;
        short uinLen = BinaryTools.getUByte(messageData, offset++);
        uin = OscarTools.getString(messageData.subBlock(offset, uinLen), "US-ASCII");
        offset += uinLen;

        int reasonLen = BinaryTools.getUShort(messageData, offset);
        offset += 2;
        reason = OscarTools.getString(messageData.subBlock(offset, reasonLen), "US-ASCII");
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

    public String getReason()
    {
        return reason;
    }

    public String getSender()
    {
        return uin;
    }

}
