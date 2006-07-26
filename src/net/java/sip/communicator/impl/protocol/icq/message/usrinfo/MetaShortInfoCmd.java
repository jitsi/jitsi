/*
 * Created on 20/10/2003
 */
package net.java.sip.communicator.impl.protocol.icq.message.usrinfo;

import java.io.*;

import net.java.sip.communicator.impl.protocol.icq.message.common.*;
import net.kano.joscar.*;
import net.kano.joscar.flapcmd.*;

/**
 * @author jkohen
 */
public class MetaShortInfoCmd
    extends SnacCommand
{
    private static final int NDX_NICKNAME = 0;
    private static final int NDX_FNAME = 1;
    private static final int NDX_LNAME = 2;
    private static final int NDX_EMAIL = 3;

    private String[] s = new String[4];

    public MetaShortInfoCmd(FromIcqCmd packet)
    {
        super(21, 3);

        ByteBlock block = packet.getIcqData();

        // Byte 0: unknown
        int offset = 1;
        for (int i = 0; i < s.length; i++)
        {
            final int textlen = LEBinaryTools.getUShort(block, offset) - 1; // Don't include the ending NUL.
            offset += 2;

            if (textlen > 0)
            {
                ByteBlock field = block.subBlock(offset, textlen);
                s[i] = OscarTools.getString(field, "US-ASCII");
                offset += textlen;
            }

            offset++; // Skip trailing NUL.
        }
    }

    /**
     * Returns the nick name of the user whose information is represented here,
     * or <code>null</code> if none was given.
     *
     * @return the nick name, or <code>null</code>.
     */
    public String getNickname()
    {
        return s[NDX_NICKNAME];
    }

    /**
     * Returns the first name of the user whose information is represented here,
     * or <code>null</code> if none was given.
     *
     * @return the first name, or <code>null</code>.
     */
    public String getFirstName()
    {
        return s[NDX_FNAME];
    }

    /**
     * Returns the last name of the user whose information is represented here,
     * or <code>null</code> if none was given.
     *
     * @return the last name, or <code>null</code>.
     */
    public String getLastName()
    {
        return s[NDX_LNAME];
    }

    /**
     * Returns the email address of the user whose information is represented here,
     * or <code>null</code> if none was given.
     *
     * @return the email address, or <code>null</code>.
     */
    public String getEmail()
    {
        return s[NDX_EMAIL];
    }

    public void writeData(OutputStream out) throws IOException
    {
    }
}
