/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.transform.dtmf;

import net.java.sip.communicator.impl.neomedia.*;
import net.java.sip.communicator.util.*;

/**
 * <tt>DtmfRawPacket</tt> represent an RTP Packet.
 * You create your <tt>DtmfRawPacket</tt> by calling the constructor.
 * You specify the DTMF attributes : code=9, end=false, marker=true ...
 * Then you fill the packet using init( ... dtmf attributes ... );
 *
 * @author Romain Philibert
 * @author Emil Ivov
 * @author Damian Minkov
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
     * The fixed size of a DTMF packet.
     */
    public static final int DTMF_PACKET_SIZE = 16;

    /**
     * The event code to send.
     */
    private int code;

    /**
     * Is this an end packet.
     */
    private boolean end;

    /**
     * The duration of the current packet.
     */
    private int duration;

    /**
     * Creates a <tt>DtmfRawPacket</tt> using the specified buffer.
     *
     * @param buffer the <tt>byte</tt> array that we should use to store packet
     * content
     * @param offset the index where we should start using the <tt>buffer</tt>.
     * @param payload the payload that has been negotiated for telephone events
     * by our signaling modules.
     */
    public DtmfRawPacket(byte[] buffer, int offset, byte payload)
    {
        super (buffer, offset, DTMF_PACKET_SIZE);

        setPayload(payload);
    }

    /**
     * Used for incoming DTMF packets, creating <tt>DtmfRawPacket</tt>
     * from RTP one.
     * @param pkt the RTP packet.
     */
    public DtmfRawPacket(RawPacket pkt)
    {
        super(pkt.getBuffer(), pkt.getOffset(), pkt.getLength());

        int at = getHeaderLength();

        code = readByte(at++);
        end = (readByte(at++) & 0x80) != 0;

        duration = ((readByte(at++) & 0xFF) << 8) | (readByte(at++) & 0xFF);
    }

    /**
     * Initializes DTMF specific values in this packet.
     *
     * @param code the DTMF code representing the digit.
     * @param end the DTMF End flag
     * @param marker the RTP Marker flag
     * @param duration the DTMF duration
     * @param timestamp the RTP timestamp
     */
    public void init(int     code,
                     boolean end,
                     boolean marker,
                     int     duration,
                     long    timestamp)
    {
        if(logger.isTraceEnabled())
        {
            logger.trace("DTMF send on RTP, code : " + code +
                " duration = "+duration +" timestamps = "+timestamp +
                " Marker = " + marker + " End = " + end);
        }

        // Set the payload type and the marker
        setMarker(marker);

        // set the Timestamp
        setTimestamp(timestamp);

         // Create the RTP data
        setDtmfPayload(code, end, duration);
    }

    /**
     * Initializes the  a DTMF raw data using event, E and duration field.
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
     * @param end boolean used to mark the two last packets
     * @param duration int increments for each dtmf sending
     * updates, stay unchanged at the end for the 2 last packets.
     */
    private void setDtmfPayload(int code, boolean end, int duration)
    {
        this.code = code;
        this.end = end;
        this.duration = duration;

        int at = getHeaderLength();

        writeByte(at++, (byte)code);
        writeByte(at++, end ? (byte)0x80 : (byte)0);
        writeByte(at++, (byte)(duration >> 8));
        writeByte(at++, (byte)duration);
    }

    /**
     * The event code of the current packet.
     * @return the code
     */
    public int getCode()
    {
        return code;
    }

    /**
     * Is this an end packet.
     * @return the end
     */
    public boolean isEnd()
    {
        return end;
    }

    /**
     * The duration of the current event.
     * @return the duration
     */
    public int getDuration()
    {
        return duration;
    }
}
