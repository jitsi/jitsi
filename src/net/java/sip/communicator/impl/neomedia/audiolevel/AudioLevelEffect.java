/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.audiolevel;

import javax.media.*;
import javax.media.format.*;

import net.java.sip.communicator.impl.neomedia.control.*;
import net.java.sip.communicator.service.neomedia.event.*;

/**
 * An effect that would pass data to the <tt>AudioLevelEventDispatcher</tt>
 * so that it would calculate levels and dispatch changes to interested parties.
 *
 * @author Damian Minkov
 * @author Emil Ivov
 * @author Lyubomir Marinov
 */
public class AudioLevelEffect
    extends ControlsAdapter
    implements Effect
{
    /**
     * The indicator which determines whether <tt>AudioLevelEffect</tt>
     * instances are to perform the copying of the data from input
     * <tt>Buffer</tt>s to output <tt>Buffer</tt>s themselves (e.g. using
     * {@link System#arraycopy(Object, int, Object, int, int)}).
     */
    private static final boolean COPY_DATA_FROM_INPUT_TO_OUTPUT = true;

    /**
     * The <tt>SimpleAudioLevelListener</tt> which this instance associates with
     * its {@link #eventDispatcher}.
     */
    private SimpleAudioLevelListener audioLevelListener = null;

    /**
     * The dispatcher of the events which handles the calculation and the event
     * firing in different thread in order to now slow down the JMF codec chain.
     */
    private final AudioLevelEventDispatcher eventDispatcher
        = new AudioLevelEventDispatcher("AudioLevelEffect Dispatcher");

    /**
     * The indicator which determines whether {@link #open()} has been called on
     * this instance without an intervening {@link #close()}.
     */
    private boolean open = false;

    /**
     * The supported audio formats by this effect.
     */
    private Format[] supportedAudioFormats;

    /**
     * The minimum and maximum values of the scale
     */
    public AudioLevelEffect()
    {
        supportedAudioFormats
            = new Format[]
                    {
                        new AudioFormat(
                                AudioFormat.LINEAR,
                                Format.NOT_SPECIFIED,
                                16,
                                1,
                                AudioFormat.LITTLE_ENDIAN,
                                AudioFormat.SIGNED,
                                16,
                                Format.NOT_SPECIFIED,
                                Format.byteArray)
                    };
    }

    /**
     * Sets (or unsets if <tt>listener</tt> is <tt>null</tt>), the listener that
     * is going to be notified of audio level changes detected by this effect.
     * Given the semantics of the {@link AudioLevelEventDispatcher} this effect
     * would do no real work if no listener is set or if it is set to
     * <tt>null</tt>.
     *
     * @param listener the <tt>SimplAudioLevelListener</tt> that we'd like to
     * receive level changes or <tt>null</tt> if we'd like level measurements
     * to stop.
     */
    public void setAudioLevelListener(SimpleAudioLevelListener listener)
    {
        synchronized (eventDispatcher)
        {
            audioLevelListener = listener;
            if (open)
                eventDispatcher.setAudioLevelListener(audioLevelListener);
        }
    }
    
    /**
     * Returns audio level listener.
     * 
     * @return audio level listener or <tt>null</tt> if not exist
     */
    public SimpleAudioLevelListener getAudioLevelListener()
    {
        synchronized (eventDispatcher)
        {
            return audioLevelListener;
        }
    }

    /**
     * Lists all of the input formats that this codec accepts.
     *
     * @return An array that contains the supported input <tt>Formats</tt>.
     */
    public Format[] getSupportedInputFormats()
    {
        return supportedAudioFormats;
    }

    /**
     * Lists the output formats that this codec can generate.
     *
     * @param input The <tt>Format</tt> of the data to be used as input to the
     * plug-in.
     * @return An array that contains the supported output <tt>Formats</tt>.
     */
    public Format[] getSupportedOutputFormats(Format input)
    {
        return
            new Format[]
                    {
                        new AudioFormat(
                                AudioFormat.LINEAR,
                                ((AudioFormat)input).getSampleRate(),
                                16,
                                1,
                                AudioFormat.LITTLE_ENDIAN,
                                AudioFormat.SIGNED,
                                16,
                                Format.NOT_SPECIFIED,
                                Format.byteArray)
                    };
    }

    /**
     * Sets the format of the data to be input to this codec.
     *
     * @param format The <tt>Format</tt> to be set.
     * @return The <tt>Format</tt> that was set.
     */
    public Format setInputFormat(Format format)
    {
        return (format instanceof AudioFormat) ? (AudioFormat) format : null;
    }

    /**
     * Sets the format for the data this codec outputs.
     *
     * @param format The <tt>Format</tt> to be set.
     * @return The <tt>Format</tt> that was set.
     */
    public Format setOutputFormat(Format format)
    {
        return (format instanceof AudioFormat) ? (AudioFormat) format : null;
    }

    /**
     * Performs the media processing defined by this codec.
     *
     * @param inputBuffer The <tt>Buffer</tt> that contains the media data to be
     * processed.
     * @param outputBuffer The <tt>Buffer</tt> in which to store the processed
     * media data.
     * @return <tt>BUFFER_PROCESSED_OK</tt> if the processing is successful.
     * @see PlugIn
     */
    public int process(Buffer inputBuffer, Buffer outputBuffer)
    {
        /*
         * In accord with what an Effect is generally supposed to do, copy the
         * data from the inputBuffer into outputBuffer.
         */
        if (COPY_DATA_FROM_INPUT_TO_OUTPUT)
        {
            // Copy the actual data from the input to the output.
            Object data = outputBuffer.getData();
            int inputBufferLength = inputBuffer.getLength();
            byte[] bufferData;

            if ((data instanceof byte[]) &&
                    (((byte[])data).length >= inputBufferLength))
            {
                bufferData = (byte[])data;
            }
            else
            {
                bufferData = new byte[inputBufferLength];
                outputBuffer.setData(bufferData);
            }
            outputBuffer.setLength(inputBufferLength);
            outputBuffer.setOffset(0);

            System.arraycopy(
                inputBuffer.getData(), inputBuffer.getOffset(),
                bufferData, 0,
                inputBufferLength);

            // Now copy the remaining attributes.
            outputBuffer.setFormat(inputBuffer.getFormat());
            outputBuffer.setHeader(inputBuffer.getHeader());
            outputBuffer.setSequenceNumber(inputBuffer.getSequenceNumber());
            outputBuffer.setTimeStamp(inputBuffer.getTimeStamp());
            outputBuffer.setFlags(inputBuffer.getFlags());
            outputBuffer.setDiscard(inputBuffer.isDiscard());
            outputBuffer.setEOM(inputBuffer.isEOM());
            outputBuffer.setDuration(inputBuffer.getDuration());
        }
        else
        {
            outputBuffer.copy(inputBuffer);
        }

        /*
         * At long last, do the job which this AudioLevelEffect exists for i.e.
         * deliver the data to eventDispatcher so that its audio level gets
         * calculated and delivered to audioEventListener. 
         */
        eventDispatcher.addData(outputBuffer);

        return BUFFER_PROCESSED_OK;
    }

    /**
     * Gets the name of this plug-in as a human-readable string.
     *
     * @return A <tt>String</tt> that contains the descriptive name of the
     * plug-in.
     */
    public String getName()
    {
        return "Audio Level Effect";
    }

    /**
     * Opens this effect.
     *
     * @throws ResourceUnavailableException If all of the required resources
     * cannot be acquired.
     */
    public void open()
        throws ResourceUnavailableException
    {
        synchronized (eventDispatcher)
        {
            if (!open)
            {
                open = true;
                eventDispatcher.setAudioLevelListener(audioLevelListener);
            }
        }
    }

    /**
     * Closes this effect.
     */
    public void close()
    {
        synchronized (eventDispatcher)
        {
            if (open)
            {
                open = false;
                eventDispatcher.setAudioLevelListener(null);
            }
        }
    }

    /**
     * Resets its state.
     */
    public void reset()
    {
    }
}
