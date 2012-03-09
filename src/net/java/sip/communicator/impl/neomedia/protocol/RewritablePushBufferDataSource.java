/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.protocol;

import java.io.*;
import java.nio.ByteBuffer; // disambiguation.
import java.nio.ByteOrder; // disambiguation.
import java.nio.IntBuffer; // disambiguation.
import java.util.*;

import javax.media.*;
import javax.media.format.*;
import javax.media.protocol.*;

import net.java.sip.communicator.service.neomedia.*;

/**
 * Implements a <tt>PushBufferDataSource</tt> wrapper which provides mute
 * support for the wrapped instance.
 * <p>
 * Because the class wouldn't work for our use case without it,
 * <tt>CaptureDevice</tt> is implemented and is being delegated to the wrapped
 * <tt>DataSource</tt> (if it supports the interface in question).
 * </p>
 *
 * @author Lyubomir Marinov
 */
public class RewritablePushBufferDataSource
    extends PushBufferDataSourceDelegate<PushBufferDataSource>
    implements MuteDataSource,
               InbandDTMFDataSource
{

    /**
     * The indicator which determines whether this <tt>DataSource</tt> is mute.
     */
    private boolean mute;

    /**
     * The tones to send via inband DTMF, if not empty.
     */
    private LinkedList<DTMFInbandTone> tones = new LinkedList<DTMFInbandTone>();

    /**
     * Initializes a new <tt>RewritablePushBufferDataSource</tt> instance which
     * is to provide mute support for a specific <tt>PushBufferDataSource</tt>.
     *
     * @param dataSource the <tt>PushBufferDataSource</tt> the new instance is
     *            to provide mute support for
     */
    public RewritablePushBufferDataSource(PushBufferDataSource dataSource)
    {
        super(dataSource);
    }

    /**
     * Implements {@link PushBufferDataSource#getStreams()}. Wraps the streams
     * of the wrapped <tt>PushBufferDataSource</tt> into
     * <tt>MutePushBufferStream</tt> instances in order to provide mute support
     * to them.
     *
     * @return an array of <tt>PushBufferStream</tt> instances with enabled mute
     * support
     */
    public PushBufferStream[] getStreams()
    {
        PushBufferStream[] streams = dataSource.getStreams();

        if (streams != null)
        {
            for (int streamIndex = 0;
                    streamIndex < streams.length;
                    streamIndex++)
            {
                PushBufferStream stream = streams[streamIndex];

                if (stream != null)
                    streams[streamIndex] = new MutePushBufferStream(stream);
            }
        }
        return streams;
    }

    /**
     * Determines whether this <tt>DataSource</tt> is mute.
     *
     * @return <tt>true</tt> if this <tt>DataSource</tt> is mute; otherwise,
     *         <tt>false</tt>
     */
    public synchronized boolean isMute()
    {
        return mute;
    }

    /**
     * Replaces the media data contained in a specific <tt>Buffer</tt> with a
     * compatible representation of silence.
     *
     * @param buffer the <tt>Buffer</tt> the data contained in which is to be
     * replaced with silence
     */
    public static void mute(Buffer buffer)
    {
        Object data = buffer.getData();

        if (data != null)
        {
            Class<?> dataClass = data.getClass();
            final int fromIndex = buffer.getOffset();
            final int toIndex = fromIndex + buffer.getLength();

            if (Format.byteArray.equals(dataClass))
                Arrays.fill((byte[]) data, fromIndex, toIndex, (byte) 0);
            else if (Format.intArray.equals(dataClass))
                Arrays.fill((int[]) data, fromIndex, toIndex, 0);
            else if (Format.shortArray.equals(dataClass))
                Arrays.fill((short[]) data, fromIndex, toIndex, (short) 0);

            buffer.setData(data);
        }
    }

    /**
     * Sets the mute state of this <tt>DataSource</tt>.
     *
     * @param mute <tt>true</tt> to mute this <tt>DataSource</tt>; otherwise,
     *            <tt>false</tt>
     */
    public synchronized void setMute(boolean mute)
    {
        this.mute = mute;
    }

    /**
     * Adds a new inband DTMF tone to send.
     *
     * @param tone the DTMF tone to send.
     */
    public void addDTMF(DTMFInbandTone tone)
    {
        this.tones.add(tone);
    }

    /**
     * Determines whether this <tt>DataSource</tt> sends a DTMF tone.
     *
     * @return <tt>true</tt> if this <tt>DataSource</tt> is sending a DTMF tone;
     * otherwise, <tt>false</tt>.
     */
    public boolean isSendingDTMF()
    {
        return !this.tones.isEmpty();
    }

    /**
     * Replaces the media data contained in a specific <tt>Buffer</tt> with an
     * inband DTMF tone signal.
     *
     * @param buffer the <tt>Buffer</tt> the data contained in which is to be
     * replaced with the DTMF tone
     * @param tone the <tt>DMFTTone</tt> to send via inband DTMF signal.
     */
    public static void sendDTMF(
            Buffer buffer,
            DTMFInbandTone tone)
    {
        Object data = buffer.getData();

        // Send the inband DTMF tone only if the buffer contains audio data.
        if (data != null && (buffer.getFormat() instanceof AudioFormat))
        {
            Class<?> dataClass = data.getClass();
            int fromIndex = buffer.getOffset();

            AudioFormat audioFormat = (AudioFormat) buffer.getFormat();
            double samplingFrequency = audioFormat.getSampleRate();
            int sampleSizeInBits = audioFormat.getSampleSizeInBits();

            // Generates the inband DTMF signal.
            int[] sampleData = tone.getAudioSamples(
                    samplingFrequency,
                    sampleSizeInBits);
            IntBuffer.wrap(sampleData);

            int toIndex = fromIndex +
                sampleData.length * (sampleSizeInBits / 8);
            ByteBuffer newData = ByteBuffer.allocate(toIndex);

            // Prepares newData to be endian compliant with original buffer
            // data.
            if(audioFormat.getEndian() == AudioFormat.BIG_ENDIAN)
            {
                newData.order(ByteOrder.BIG_ENDIAN);
            }
            else
            {
                newData.order(ByteOrder.LITTLE_ENDIAN);
            }

            // Keeps data unchanged if storeed before the original buffer offset
            // index.
            // Takes care of original data array type (byte, short or int).
            if (Format.byteArray.equals(dataClass))
            {
                newData.put(((byte[]) data), 0, fromIndex);
            }
            else if (Format.shortArray.equals(dataClass))
            {
                for(int i = 0; i < fromIndex; ++i)
                {
                    newData.putShort(((short[]) data)[i]);
                }
            }
            else if (Format.intArray.equals(dataClass))
            {
                for(int i = 0; i < fromIndex; ++i)
                {
                    newData.putInt(((int[]) data)[i]);
                }
            }

            // Copies inband DTMF singal into newData.
            // Takes care of audio format encryption data type (byte, short or
            // int).
            switch (sampleSizeInBits)
            {
                case 8:
                    for(int i = 0; i < sampleData.length; ++i)
                    {
                        newData.put(((byte) sampleData[i]));
                    }
                    break;
                case 16:
                    for(int i = 0; i < sampleData.length; ++i)
                    {
                        newData.putShort(((short) sampleData[i]));
                    }
                    break;
                case 32:
                    for(int i = 0; i < sampleData.length; ++i)
                    {
                        newData.putInt(sampleData[i]);
                    }
                    break;
            }

            // Copies newData up to date into the original buffer.
            // Takes care of original data array type (byte, short or int).
            if (Format.byteArray.equals(dataClass))
            {
                buffer.setData(newData.array());
            }
            else if (Format.shortArray.equals(dataClass))
            {
                buffer.setData(newData.asShortBuffer().array());
            }
            else if (Format.intArray.equals(dataClass))
            {
                buffer.setData(newData.asIntBuffer().array());
            }

            // Updates the buffer length.
            buffer.setLength(toIndex - fromIndex);
        }
    }

    /**
     * Implements a <tt>PushBufferStream</tt> wrapper which provides mute
     * support for the wrapped instance.
     */
    private class MutePushBufferStream
        extends SourceStreamDelegate<PushBufferStream>
        implements PushBufferStream
    {

        /**
         * Initializes a new <tt>MutePushBufferStream</tt> instance which is to
         * provide mute support to a specific <tt>PushBufferStream</tt>.
         *
         * @param stream the <tt>PushBufferStream</tt> the new instance is to
         * provide mute support to
         */
        public MutePushBufferStream(PushBufferStream stream)
        {
            super(stream);
        }

        /**
         * Implements {@link PushBufferStream#getFormat()}. Delegates to the
         * wrapped <tt>PushBufferStream</tt>.
         *
         * @return the <tt>Format</tt> of the wrapped <tt>PushBufferStream</tt>
         */
        public Format getFormat()
        {
            return stream.getFormat();
        }

        /**
         * Implements {@link PushBufferStream#read(Buffer)}. If this instance is
         * muted (through its owning <tt>RewritablePushBufferDataSource</tt>),
         * overwrites the data read from the wrapped <tt>PushBufferStream</tt>
         * with silence data.
         *
         * @param buffer a <tt>Buffer</tt> in which the read data is to be
         * returned to the caller
         * @throws IOException if reading from the wrapped
         * <tt>PushBufferStream</tt> fails
         */
        public void read(Buffer buffer)
            throws IOException
        {
            stream.read(buffer);

            if (isSendingDTMF())
            {
                sendDTMF(buffer, tones.poll());
            }
            else if (isMute())
            {
                mute(buffer);
            }
        }

        /**
         * Implements
         * {@link PushBufferStream#setTransferHandler(BufferTransferHandler)}.
         * Sets up the hiding of the wrapped <tt>PushBufferStream</tt> from the
         * specified <tt>transferHandler</tt> and thus gives this
         * <tt>MutePushBufferStream</tt> full control when the
         * <tt>transferHandler</tt> in question starts calling to the stream
         * given to it in
         * <tt>BufferTransferHandler#transferData(PushBufferStream)</tt>.
         *
         * @param transferHandler a <tt>BufferTransferHandler</tt> to be
         * notified by this instance when data is available for reading from it
         */
        public void setTransferHandler(BufferTransferHandler transferHandler)
        {
            stream.setTransferHandler(
                (transferHandler == null)
                    ? null
                    : new StreamSubstituteBufferTransferHandler(
                            transferHandler,
                            stream,
                            this));
        }
    }
}
