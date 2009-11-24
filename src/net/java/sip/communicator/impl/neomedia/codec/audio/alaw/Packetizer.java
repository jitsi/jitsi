/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.codec.audio.alaw;

import javax.media.*;
import javax.media.format.*;

import com.ibm.media.codec.audio.*;
import net.java.sip.communicator.impl.neomedia.codec.*;

/**
 * Packetizer for ALAW codec
 * @author Damian Minkov
 */
public class Packetizer
    extends AudioPacketizer
{
    public Packetizer()
    {
        packetSize = 160;
        supportedInputFormats = new AudioFormat[]
            {
            new AudioFormat(
                AudioFormat.ALAW,
                Format.NOT_SPECIFIED,
                8,
                1,
                Format.NOT_SPECIFIED,
                Format.NOT_SPECIFIED,
                8,
                Format.NOT_SPECIFIED,
                Format.byteArray
            )
        };
        defaultOutputFormats = new AudioFormat[]
            {
            new AudioFormat(
                AudioFormat.ALAW,
                Format.NOT_SPECIFIED,
                8,
                1,
                Format.NOT_SPECIFIED,
                Format.NOT_SPECIFIED,
                8,
                Format.NOT_SPECIFIED,
                Format.byteArray
            )
        };

        PLUGIN_NAME = "ALaw Packetizer";

    }

    protected Format[] getMatchingOutputFormats(Format in)
    {

        AudioFormat af = (AudioFormat) in;

        supportedOutputFormats = new AudioFormat[]
            {
            new AudioFormat(
                Constants.ALAW_RTP,
                af.getSampleRate(),
                8,
                1,
                Format.NOT_SPECIFIED,
                Format.NOT_SPECIFIED,
                8,
                Format.NOT_SPECIFIED,
                Format.byteArray
            )
        };
        return supportedOutputFormats;
    }

    public void open() throws ResourceUnavailableException
    {
        setPacketSize(packetSize);
        reset();
    }

    public java.lang.Object[] getControls()
    {
        if (controls == null)
        {
            controls = new Control[1];
            controls[0] = new PacketSizeAdapter(this, packetSize, true);
        }
        return controls;
    }

    public synchronized void setPacketSize(int newPacketSize)
    {
        packetSize = newPacketSize;

        sample_count = packetSize;

        if (history == null)
        {
            history = new byte[packetSize];
            return;
        }

        if (packetSize > history.length)
        {
            byte[] newHistory = new byte[packetSize];
            System.arraycopy(history, 0, newHistory, 0, historyLength);
            history = newHistory;
        }
    }

}

class PacketSizeAdapter
    extends com.sun.media.controls.PacketSizeAdapter
{
    public PacketSizeAdapter(Codec newOwner, int newPacketSize,
                             boolean newIsSetable)
    {
        super(newOwner, newPacketSize, newIsSetable);
    }

    public int setPacketSize(int numBytes)
    {

        int numOfPackets = numBytes;

        if (numOfPackets < 10)
        {
            numOfPackets = 10;
        }

        if (numOfPackets > 8000)
        {
            numOfPackets = 8000;
        }
        packetSize = numOfPackets;

        ( (Packetizer) owner).setPacketSize(packetSize);

        return packetSize;
    }
}
