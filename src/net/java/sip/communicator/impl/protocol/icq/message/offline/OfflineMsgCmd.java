/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.icq.message.offline;

import java.io.*;
import java.util.*;

import net.java.sip.communicator.impl.protocol.icq.message.common.*;
import net.kano.joscar.*;
import net.kano.joscar.flapcmd.*;

/**
 * Command received frin server. Parses incoming offline messages
 * There is byte in one message indicating that this the last message.
 *
 * @author Damian Minkov
 */
public class OfflineMsgCmd
    extends SnacCommand
{
    private long uin;
    private Calendar cal;
    private int msgType;
    private String contents;

    // used to indicate that this is the last message of the sequence
    private boolean endOfOfflineMessages = false;

    public OfflineMsgCmd(FromIcqCmd packet)
    {
        super(21, 3);

        if (packet.getType().equals(AbstractIcqCmd.CMD_OFFLINE_MSG_DONE))
        {
            endOfOfflineMessages = true;
        }
        else
        {
            ByteBlock block = packet.getIcqData();

            uin = LEBinaryTools.getUInt(block, 0);
            cal = new GregorianCalendar(
                LEBinaryTools.getUShort(block, 4),
                LEBinaryTools.getUByte(block, 6),
                LEBinaryTools.getUByte(block, 7),
                LEBinaryTools.getUByte(block, 8),
                LEBinaryTools.getUByte(block, 9));
            msgType = LEBinaryTools.getUShort(block, 10);
            final int textlen = LEBinaryTools.getUShort(block, 12) - 1; // Don't include the ending NUL.
            block = block.subBlock(14, textlen);
            contents = OscarTools.getString(block, "US-ASCII");
        }
    }

    /**
     * Writes this command's SNAC data block to the given stream.
     *
     * @param out the stream to which to write the SNAC data
     * @throws IOException if an I/O error occurs
     */
    public void writeData(OutputStream out) throws IOException
    {
        // noting to write as it is only for receiving
    }

    /**
     * Returns the time the message was sent.
     *
     * @return the time this message was sent.
     */
    public Date getDate()
    {
        return cal.getTime();
    }

    /**
     * Returns the content of this message. The meaning varies accordingly
     * with the {@link #getMsgType() "message type"}.
     *
     * @return the content of this message.
     */
    public String getContents()
    {
        return contents;
    }

    /**
     * Returns the UIN of the sender of this message.
     *
     * @return the UIN of the sender.
     */
    public long getUin()
    {
        return uin;
    }

    /**
     * Returns the type of this message.
     *
     * @return the type of this message.
     */
    public int getMsgType()
    {
        return msgType;
    }

    /**
     * Indicates whether all offline Messages are received
     * The last packet of the sequence does not contain any message data
     *
     * It doesn't contain message - it is only end_of_sequence marker.
     *
     * @return boolean are all ofline messages received
     */
    public boolean isEndOfOfflineMessages()
    {
        return endOfOfflineMessages;
    }

}
