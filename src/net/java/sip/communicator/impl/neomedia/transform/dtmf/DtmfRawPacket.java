/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.transform.dtmf;

import net.java.sip.communicator.impl.media.transform.*;
import net.java.sip.communicator.impl.neomedia.*;
import net.java.sip.communicator.util.*;

/**
 * <tt>DtmfRawPacket</tt> represent an RTP Packet.
 * You create your <tt>DtmfRawPacket</tt> by calling the constructor.
 * You specify the DTMF attributes : code=9, end=false, marker=truen ...
 * Then you fill the packet using fillRawPacket( ... dtmf attributes ... );
 *
 * @author Romain Philibert
 * @author Emil Ivov
 */
public class DtmfRawPacket
        extends RawPacket
{
    /**
     * Our class logger.
     */
    private static final Logger logger
        = Logger.getLogger(DtmfRawPacket.class);

    /**
     * Creates a <tt>DtmfRawPacket</tt> using the specified buffer.
     *
     * @param buffer Byte array holding the content of this Packet
     */
    public DtmfRawPacket(byte[] buffer)
    {
        //DTMF buffer length = 16.
        super (buffer, 0, 16);
    }

    /**
     * Fill the RTP packet with DTMF fields.
     *
     * @param code the DTMF code representing the digit.
     * @param end the DTMF End flag
     * @param marker the RTP Marker flag
     * @param duration the DTMF duration
     * @param timestamp the RTP timestamp
     */
    public void fillRawPacket(int code, boolean end, boolean marker, int duration, long timestamp)
    {
        logger.trace("DTMF send on RTP, code : " + code + " " +
                "dur = "+duration +" "+
                "ts = "+timestamp +" "+
                (marker?"Marker":"") +
                (end?"End":""));

        // Set the payload type and the marker
        setMarker(marker);

        // set the Timestamp
        setTimestamp(timestamp);

         // Create the RTP data
        setData(code, end, duration);
    }

    /**
     * Create a DTMF raw data using event, E and duration field.
     * Event : the digits to transmit (0-15).
     * E : End field, used to mark the two last packets.
     * R always = 0.
     * Volume always = 0.
     * Duration : duration increments for each dtmf sending updates,
     * stay unchanged at the end for the 3 last packets.
     * <pre>
     *  0                   1                   2                   3
     *  0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
     *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     *  |     event     |E R| volume    |          duration             |
     *  |       ?       |? 0|    0      |              ?                |
     *  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * </pre>
     *
     * @param code the digit to transmit 0-15
     * @param end : boolean used to mark the two last packets
     * @param duration : int increments for each dtmf sending
     *          updates, stay unchanged at the end for the 2 last packets.
     * @return the DTMF raw data
     */
    private void setData(int code, boolean end, int duration)
    {
        byte[] data = new byte[4];
        data[0]=(byte)code;
        data[1]= end ? (byte)0x80 : (byte)0;
        data[2]=(byte)(duration >> 8);
        data[3]=(byte) duration;
        System.arraycopy(data, 0, this.buffer, 12, 4);
    }

    /**
     * Read the timestamp value of the RTP Packet;
     * @return the timestamp value
     */
    public long getTimestamp()
    {
        return readInt(4);
    }

    /**
     * Set the timestamp value of the RTP Packet
     * @param timestamp : the RTP Timestamp
     */
    public void setTimestamp(long timestamp)
    {
        this.buffer[4] = (byte) ((timestamp >> 24) & 0xFF);
        this.buffer[5] = (byte) ((timestamp >> 16) & 0xFF);
        this.buffer[6] = (byte) ((timestamp >> 8) & 0xFF);
        this.buffer[7] = (byte) (timestamp & 0xFF);
    }

    /**
     * Set the marker of the RTP Packet and the PayloadType to
     * <tt>DtmfConstants.DtmfSDP</tt>;
     * @param marker : the RTP Marker
     */
    private void setMarker(boolean marker)
    {
        if(marker)
        {
             this.buffer[1] = (byte) (DtmfConstants.DtmfSDP | 0x80);
        }
        else
        {
             this.buffer[1] = (byte) DtmfConstants.DtmfSDP;
        }
    }

}
