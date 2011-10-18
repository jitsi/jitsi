/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.conference;

import java.io.*;

import javax.media.*;
import javax.media.protocol.*;

import net.java.sip.communicator.impl.neomedia.*;
import net.java.sip.communicator.impl.neomedia.protocol.*;
import net.java.sip.communicator.util.*;

/**
 * Describes additional information about a specific input <tt>DataSource</tt>
 * of an <tt>AudioMixer</tt> so that the <tt>AudioMixer</tt> can, for example,
 * quickly discover the output <tt>AudioMixingPushBufferDataSource</tt> in the
 * mix of which the contribution of the <tt>DataSource</tt> is to not be
 * included.
 * <p>
 * Private to <tt>AudioMixer</tt> and <tt>AudioMixerPushBufferStream</tt> but
 * extracted into its own file for the sake of clarity.
 * </p>
 *
 * @author Lubomir Marinov
 */
class InputDataSourceDesc
{

    /**
     * The <tt>Logger</tt> used by the <tt>InputDataSourceDesc</tt> class and
     * its instances for logging output.
     */
    private static final Logger logger
        = Logger.getLogger(InputDataSourceDesc.class);

    /**
     * The constant which represents an empty array with <tt>SourceStream</tt>
     * element type. Explicitly defined in order to avoid unnecessary allocations.
     */
    private static final SourceStream[] EMPTY_STREAMS = new SourceStream[0];

    /**
     * The indicator which determines whether the effective input
     * <tt>DataSource</tt> described by this instance is currently connected.
     */
    private boolean connected;

    /**
     * The <tt>Thread</tt> which currently executes {@link DataSource#connect()}
     * on the effective input <tt>DataSource</tt> described by this instance.
     */
    private Thread connectThread;

    /**
     * The <tt>DataSource</tt> for which additional information is described by
     * this instance.
     */
    public final DataSource inputDataSource;

    /**
     * The <tt>AudioMixingPushBufferDataSource</tt> in which the mix
     * contributions of {@link #inputDataSource} are to not be included.
     */
    public final AudioMixingPushBufferDataSource outputDataSource;

    /**
     * The <tt>DataSource</tt>, if any, which transcodes the tracks of
     * {@link #inputDataSource} in the output <tt>Format</tt> of the associated
     * <tt>AudioMixer</tt>.
     */
    private DataSource transcodingDataSource;

    /**
     * Initializes a new <tt>InputDataSourceDesc</tt> instance which is to
     * describe additional information about a specific input
     * <tt>DataSource</tt> of an <tt>AudioMixer</tt>. Associates the specified
     * <tt>DataSource</tt> with the <tt>AudioMixingPushBufferDataSource</tt> in
     * which the mix contributions of the specified input <tt>DataSource</tt>
     * are to not be included.
     *
     * @param inputDataSource a <tt>DataSourc</tt> for which additional
     * information is to be described by the new instance
     * @param outputDataSource the <tt>AudioMixingPushBufferDataSource</tt> in
     * which the mix contributions of <tt>inputDataSource</tt> are to not be
     * included
     */
    public InputDataSourceDesc(
        DataSource inputDataSource,
        AudioMixingPushBufferDataSource outputDataSource)
    {
        this.inputDataSource = inputDataSource;
        this.outputDataSource = outputDataSource;
    }

    /**
     * Connects the effective input <tt>DataSource</tt> described by this
     * instance upon request from a specific <tt>AudioMixer</tt>. If the
     * effective input <tt>DataSource</tt> is to be asynchronously connected,
     * the completion of the connect procedure will be reported to the specified
     * <tt>AudioMixer</tt> by calling its
     * {@link AudioMixer#connected(InputDataSourceDesc)}.
     *
     * @param audioMixer the <tt>AudioMixer</tt> requesting the effective input
     * <tt>DataSource</tt> described by this instance to be connected
     * @throws IOException if anything wrong happens while connecting the
     * effective input <tt>DataSource</tt> described by this instance
     */
    synchronized void connect(final AudioMixer audioMixer)
        throws IOException
    {
        final DataSource effectiveInputDataSource
            = (transcodingDataSource == null)
                ? inputDataSource
                : transcodingDataSource;

        if (effectiveInputDataSource instanceof TranscodingDataSource)
        {
            if (connectThread == null)
            {
                connectThread = new Thread()
                {
                    @Override
                    public void run()
                    {
                        try
                        {
                            audioMixer
                                .connect(
                                    effectiveInputDataSource,
                                    inputDataSource);
                            synchronized (InputDataSourceDesc.this)
                            {
                                connected = true;
                            }
                            audioMixer.connected(InputDataSourceDesc.this);
                        }
                        catch (IOException ioex)
                        {
                            logger
                                .error(
                                    "Failed to connect to inputDataSource "
                                        + MediaStreamImpl
                                            .toString(inputDataSource),
                                    ioex);
                        }
                        finally
                        {
                            synchronized (InputDataSourceDesc.this)
                            {
                                if (connectThread == Thread.currentThread())
                                    connectThread = null;
                            }
                        }
                    }
                };
                connectThread.setDaemon(true);
                connectThread.start();
            }
        }
        else
        {
            audioMixer.connect(effectiveInputDataSource, inputDataSource);
            connected = true;
        }
    }

    /**
     * Creates a <tt>DataSource</tt> which attempts to transcode the tracks of
     * the input <tt>DataSource</tt> described by this instance into a specific
     * output <tt>Format</tt>.
     *
     * @param outputFormat the <tt>Format</tt> in which the tracks of the input
     * <tt>DataSource</tt> described by this instance are to be transcoded
     * @return <tt>true</tt> if a new transcoding <tt>DataSource</tt> has been
     * created for the input <tt>DataSource</tt> described by this instance;
     * otherwise, <tt>false</tt>
     */
    synchronized boolean createTranscodingDataSource(Format outputFormat)
    {
        if (transcodingDataSource == null)
        {
            setTranscodingDataSource(
                new TranscodingDataSource(inputDataSource, outputFormat));
            return true;
        }
        else
            return false;
    }

    /**
     * Disconnects the effective input <tt>DataSource</tt> described by this
     * instance if it is already connected.
     */
    synchronized void disconnect()
    {
        if (connected)
        {
            getEffectiveInputDataSource().disconnect();
            connected = false;
        }
    }

    /**
     * Gets the control available for the effective input <tt>DataSource</tt>
     * described by this instance with a specific type.
     *
     * @param controlType a <tt>String</tt> value which specifies the type of
     * the control to be retrieved
     * @return an <tt>Object</tt> which represents the control available for the
     * effective input <tt>DataSource</tt> described by this instance with the
     * specified <tt>controlType</tt> if such a control exists; otherwise,
     * <tt>null</tt>
     */
    public synchronized Object getControl(String controlType)
    {
        DataSource effectiveInputDataSource = getEffectiveInputDataSource();

        return
            (effectiveInputDataSource == null)
                ? null
                : effectiveInputDataSource.getControl(controlType);
    }

    /**
     * Gets the actual <tt>DataSource</tt> from which the associated
     * <tt>AudioMixer</tt> directly reads in order to retrieve the mix
     * contribution of the <tt>DataSource</tt> described by this instance.
     *
     * @return the actual <tt>DataSource</tt> from which the associated
     * <tt>AudioMixer</tt> directly reads in order to retrieve the mix
     * contribution of the <tt>DataSource</tt> described by this instance
     */
    public synchronized DataSource getEffectiveInputDataSource()
    {
        return
            (transcodingDataSource == null)
                ? inputDataSource
                : (connected ? transcodingDataSource : null);
    }

    /**
     * Gets the <tt>SourceStream</tt>s of the effective input
     * <tt>DataSource</tt> described by this instance.
     *
     * @return an array of the <tt>SourceStream</tt>s of the effective input
     * <tt>DataSource</tt> described by this instance
     */
    public synchronized SourceStream[] getStreams()
    {
        if (!connected)
            return EMPTY_STREAMS;

        DataSource inputDataSource = getEffectiveInputDataSource();

        if (inputDataSource instanceof PushBufferDataSource)
            return ((PushBufferDataSource) inputDataSource).getStreams();
        else if (inputDataSource instanceof PullBufferDataSource)
            return ((PullBufferDataSource) inputDataSource).getStreams();
        else if (inputDataSource instanceof TranscodingDataSource)
            return ((TranscodingDataSource) inputDataSource).getStreams();
        else
            return null;
    }

    /**
     * Sets the <tt>DataSource</tt>, if any, which transcodes the tracks of the
     * input <tt>DataSource</tt> described by this instance in the output
     * <tt>Format</tt> of the associated <tt>AudioMixer</tt>.
     *
     * @param transcodingDataSource the <tt>DataSource</tt> which transcodes
     * the tracks of the input <tt>DataSource</tt> described by this instance in
     * the output <tt>Format</tt> of the associated <tt>AudioMixer</tt>
     */
    private synchronized void setTranscodingDataSource(
            DataSource transcodingDataSource)
    {
        this.transcodingDataSource = transcodingDataSource;
        connected = false;
    }

    /**
     * Starts the effective input <tt>DataSource</tt> described by this instance
     * if it is connected.
     *
     * @throws IOException if starting the effective input <tt>DataSource</tt>
     * described by this instance fails
     */
    synchronized void start()
        throws IOException
    {
        if (connected)
            getEffectiveInputDataSource().start();
    }

    /**
     * Stops the effective input <tt>DataSource</tt> described by this instance
     * if it is connected.
     *
     * @throws IOException if stopping the effective input <tt>DataSource</tt>
     * described by this instance fails
     */
    synchronized void stop()
        throws IOException
    {
        if (connected)
            getEffectiveInputDataSource().stop();
    }
}
