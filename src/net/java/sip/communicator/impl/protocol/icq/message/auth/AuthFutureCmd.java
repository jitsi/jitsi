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
 * When sending authorization request
 * we can send also future athorization so
 * the other party has no need to request our athorization
 *
 * @author Damian Minkov
 */
public class AuthFutureCmd
    extends AbstractAuthCommand
{
    private String uin;
    private String reason;

    public AuthFutureCmd(String uin, String reason)
    {
        super(CMD_AUTH_FUTURE);
        this.uin = uin;
        this.reason = reason;
    }

    public AuthFutureCmd(SnacPacket packet)
    {
        super(CMD_AUTH_FUTURE_GRANTED);

        ByteBlock data = packet.getData();

        int offset = 0;
        short strLen = LEBinaryTools.getUByte(data, offset);
        offset++;

        ByteBlock field = data.subBlock(offset, strLen);
        uin = OscarTools.getString(field, "US-ASCII");
        offset += strLen;

        strLen = LEBinaryTools.getUByte(data, offset);
        offset++;

        field = data.subBlock(offset, strLen);
        reason = OscarTools.getString(field, "US-ASCII");
    }

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

    public String getUin()
    {
        return uin;
    }
}
