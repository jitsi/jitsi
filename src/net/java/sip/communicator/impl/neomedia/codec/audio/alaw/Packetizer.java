/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.codec.audio.alaw;

import javax.media.*;
import javax.media.format.*;

import net.java.sip.communicator.impl.neomedia.codec.*;

/**
 * Implements an RTP packetizer for the A-law codec.
 *
 * @author Damian Minkov
 * @author Lyubomir Marinov
 */
public class Packetizer
    extends com.ibm.media.codec.audio.AudioPacketizer
{
    /**
     * Initializes a new <tt>Packetizer</tt> instance.
     */
    public Packetizer()
    {
        defaultOutputFormats
            = new AudioFormat[]
                    {
                        new AudioFormat(
                                Constants.ALAW_RTP,
                                Format.NOT_SPECIFIED,
                                8,
                                1,
                                Format.NOT_SPECIFIED,
                                Format.NOT_SPECIFIED,
                                8,
                                Format.NOT_SPECIFIED,
                                Format.byteArray)
                    };
        packetSize = 160;
        PLUGIN_NAME = "A-law Packetizer";
        supportedInputFormats
            = new AudioFormat[]
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
                                Format.byteArray)
                    };
    }

    public Object[] getControls()
    {
        if (controls == null)
        {
            controls
                = new Control[]
                        {
                            new PacketSizeAdapter(this, packetSize, true)
                        };
        }
        return controls;
    }

    protected Format[] getMatchingOutputFormats(Format in)
    {
        AudioFormat af = (AudioFormat) in;
        double sampleRate = af.getSampleRate();

        supportedOutputFormats
            = new AudioFormat[]
                    {
                        new AudioFormat(
                                Constants.ALAW_RTP,
                                sampleRate,
                                8,
                                1,
                                Format.NOT_SPECIFIED,
                                Format.NOT_SPECIFIED,
                                8,
                                sampleRate,
                                Format.byteArray)
                    };
        return supportedOutputFormats;
    }

    public void open()
        throws ResourceUnavailableException
    {
        setPacketSize(packetSize);
        reset();
    }

    /**
     * Sets the packet size to be used by this <tt>Packetizer</tt>.
     *
     * @param newPacketSize the new packet size to be used by this
     * <tt>Packetizer</tt>
     */
    private synchronized void setPacketSize(int newPacketSize)
    {
        packetSize = newPacketSize;

        sample_count = packetSize;

        if (history == null)
        {
            history = new byte[packetSize];
        }
        else if (packetSize > history.length)
        {
            byte[] newHistory = new byte[packetSize];

            System.arraycopy(history, 0, newHistory, 0, historyLength);
            history = newHistory;
        }
    }

    private static class PacketSizeAdapter
        extends com.sun.media.controls.PacketSizeAdapter
    {
        public PacketSizeAdapter(Codec owner, int packetSize, boolean settable)
        {
            super(owner, packetSize, settable);
        }

        @Override
        public int setPacketSize(int numBytes)
        {
            if (numBytes < 10)
                numBytes = 10;
            if (numBytes > 8000)
                numBytes = 8000;

            packetSize = numBytes;

            ((Packetizer)owner).setPacketSize(packetSize);

            return packetSize;
        }
    }
}
