/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.icq.message.usrinfo;

import java.io.*;

import net.java.sip.communicator.impl.protocol.icq.message.common.*;
import net.kano.joscar.*;

/**
 * Tlv set in command for changis user account info stored on server
 * @author Damian Minkov
 */
public class DetailTlv
    implements Writable
{
    private byte[] data = new byte[0];
    private int type;

    public DetailTlv(int type)
    {
        this.type = type;
    }

    public void write(OutputStream out)
        throws IOException
    {
        LEBinaryTools.writeUShort(out, type);
        LEBinaryTools.writeUShort(out, data.length);
        out.write(data);
    }

    public long getWritableLength()
    {
        return 4 + data.length;
    }

    public void writeUInt(long number)
    {
        byte[] tmp = LEBinaryTools.getUInt(number);
        byte[] newData = new byte[data.length + tmp.length];

        System.arraycopy(data, 0, newData, 0, data.length);
        System.arraycopy(tmp, 0, newData, data.length, tmp.length);

        data = newData;
    }

    public void writeUShort(int number)
    {
        byte[] tmp = LEBinaryTools.getUShort(number);
        byte[] newData = new byte[data.length + tmp.length];

        System.arraycopy(data, 0, newData, 0, data.length);
        System.arraycopy(tmp, 0, newData, data.length, tmp.length);

        data = newData;
    }

    public void writeUByte(int number)
    {
        byte[] tmp = LEBinaryTools.getUByte(number);
        byte[] newData = new byte[data.length + tmp.length];

        System.arraycopy(data, 0, newData, 0, data.length);
        System.arraycopy(tmp, 0, newData, data.length, tmp.length);

        data = newData;
    }

    public void writeString(String str)
    {
        if(str == null)
            str = "";// empty string so length will be 0 and nothing to be writen

        byte[] tmp = BinaryTools.getAsciiBytes(str);

        // save the string length before we process the string bytes
        writeUShort(tmp.length);

        byte[] newData = new byte[data.length + tmp.length];

        System.arraycopy(data, 0, newData, 0, data.length);
        System.arraycopy(tmp, 0, newData, data.length, tmp.length);

        data = newData;
    }

    public String toString()
    {
        StringBuffer result = new StringBuffer();
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try
        {
            write(out);
        }
        catch(IOException ex)
        {
            ex.printStackTrace();
            return null;
        }

        byte[] arrOut = out.toByteArray();
        for(int i = 0; i < arrOut.length; i++)
        {
            byte temp = arrOut[i];
            result.append(Integer.toHexString(temp&0xFF)).append(' ');
        }

        return result.toString();
    }
}
