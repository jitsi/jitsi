/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.media.conference;

import java.io.*;
import java.lang.reflect.*;

import javax.media.*;
import javax.media.control.*;
import javax.media.format.*;
import javax.media.protocol.*;

import net.java.sip.communicator.impl.media.*;

/**
 * Represents a <code>DataSource</code> which transcodes the tracks of a
 * specific input <code>DataSource</code> into a specific output
 * <code>Format</code>. The transcoding is attempted only for tracks which
 * actually support it for the specified output <code>Format</code>.
 * 
 * @author Lubomir Marinov
 */
public class TranscodingDataSource
    extends DataSource
{

    /**
     * The <code>DataSource</code> which has its tracks transcoded by this
     * instance.
     */
    private final DataSource inputDataSource;

    /**
     * The <code>DataSource</code> which contains the transcoded tracks of
     * <code>inputDataSource</code> and which is wrapped by this instance. It is
     * the output of <code>transcodingProcessor</code>.
     */
    private DataSource outputDataSource;

    /**
     * The <code>Format</code> in which the tracks of
     * <code>inputDataSource</code> are transcoded.
     */
    private final Format outputFormat;

    /**
     * The <code>Processor</code> which carries out the actual transcoding of
     * the tracks of <code>inputDataSource</code>.
     */
    private Processor transcodingProcessor;

    /**
     * Initializes a new <code>TranscodingDataSource</code> instance to
     * transcode the tracks of a specific <code>DataSource</code> into a
     * specific output <code>Format</code>.
     * 
     * @param inputDataSource the <code>DataSource</code> which is to have its
     *            tracks transcoded in a specific outptu <code>Format</code>
     * @param outputFormat the <code>Format</code> in which the new instance is
     *            to transcode the tracks of <code>inputDataSource</code>
     */
    public TranscodingDataSource(
        DataSource inputDataSource,
        Format outputFormat)
    {
        super(inputDataSource.getLocator());

        this.inputDataSource = inputDataSource;
        this.outputFormat = outputFormat;
    }

    /*
     * Implements DataSource#connect(). Sets up the very transcoding process and
     * just does not start it i.e. creates a Processor on the inputDataSource,
     * sets outputFormat on its tracks (which support a Format compatible with
     * outputFormat) and connects to its output DataSource.
     */
    public void connect()
        throws IOException
    {
        if (outputDataSource != null)
            return;

        Processor processor;

        try
        {
            processor = Manager.createProcessor(inputDataSource);
        }
        catch (NoProcessorException npex)
        {
            IOException ioex = new IOException();
            ioex.initCause(npex);
            throw ioex;
        }

        ProcessorUtility processorUtility = new ProcessorUtility();

        if (!processorUtility.waitForState(processor, Processor.Configured))
            throw new IOException("Couldn't configure transcoding processor.");

        TrackControl[] trackControls = processor.getTrackControls();

        if (trackControls != null)
            for (TrackControl trackControl : trackControls)
            {
                Format trackFormat = trackControl.getFormat();
    
                /*
                 * XXX We only care about AudioFormat here and we assume
                 * outputFormat is of such type because it is in our current and
                 * only use case of TranscodingDataSource 
                 */
                if ((trackFormat instanceof AudioFormat)
                        && !trackFormat.matches(outputFormat))
                {
                    Format[] supportedTrackFormats
                        = trackControl.getSupportedFormats();

                    if (supportedTrackFormats != null)
                        for (Format supportedTrackFormat
                                : supportedTrackFormats)
                            if (supportedTrackFormat.matches(outputFormat))
                            {
                                Format intersectionFormat
                                    = supportedTrackFormat.intersects(
                                            outputFormat);

                                if (intersectionFormat != null)
                                {
                                    trackControl.setFormat(intersectionFormat);
                                    break;
                                }
                            }
                }
            }

        if (!processorUtility.waitForState(processor, Processor.Realized))
            throw new IOException("Couldn't realize transcoding processor.");

        DataSource outputDataSource = processor.getDataOutput();
        outputDataSource.connect();

        transcodingProcessor = processor;
        this.outputDataSource = outputDataSource;
    }

    /*
     * Implements DataSource#disconnect(). Stops and undoes the whole setup of
     * the very transcoding process i.e. disconnects from the output DataSource
     * of the transcodingProcessor and disposes of the transcodingProcessor.
     */
    public void disconnect()
    {
        if (outputDataSource == null)
            return;

        try
        {
            stop();
        }
        catch (IOException ioex)
        {
            throw new UndeclaredThrowableException(ioex);
        }

        outputDataSource.disconnect();

        transcodingProcessor.deallocate();
        transcodingProcessor.close();
        transcodingProcessor = null;

        outputDataSource = null;
    }

    /*
     * Implements DataSource#getContentType(). Delegates to the actual output of
     * the transcoding.
     */
    public String getContentType()
    {
        return
            (outputDataSource == null)
                ? null
                : outputDataSource.getContentType();
    }

    /*
     * Implements DataSource#getControl(String). Delegates to the actual output
     * of the transcoding.
     */
    public Object getControl(String controlType)
    {
        /*
         * The Javadoc of DataSource#getControl(String) says it's an error to
         * call the method without being connected and by that time we should
         * have the outputDataSource.
         */
        return outputDataSource.getControl(controlType);
    }

    /*
     * Implements DataSource#getControls(). Delegates to the actual output of
     * the transcoding.
     */
    public Object[] getControls()
    {
        return
            (outputDataSource == null)
                ? new Object[0]
                : outputDataSource.getControls();
    }

    /*
     * Implements DataSource#getDuration(). Delegates to the actual output of
     * the transcoding.
     */
    public Time getDuration()
    {
        return
            (outputDataSource == null)
                ? DURATION_UNKNOWN
                : outputDataSource.getDuration();
    }

    /**
     * Gets the output streams that this instance provides. Some of them may be
     * the result of transcoding the tracks of the input <code>DataSource</code>
     * of this instance in the output <code>Format</code> of this instance.
     * 
     * @return an array of <code>SourceStream</code>s which represents the
     *         collection of output streams that this instance provides
     */
    public SourceStream[] getStreams()
    {
        if (outputDataSource instanceof PushBufferDataSource)
            return ((PushBufferDataSource) outputDataSource).getStreams();
        if (outputDataSource instanceof PullBufferDataSource)
            return ((PullBufferDataSource) outputDataSource).getStreams();
        if (outputDataSource instanceof PushDataSource)
            return ((PushDataSource) outputDataSource).getStreams();
        if (outputDataSource instanceof PullDataSource)
            return ((PullDataSource) outputDataSource).getStreams();
        return new SourceStream[0];
    }

    /*
     * Implements DataSource#start(). Starts the actual transcoding process
     * already set up with #connect().
     */
    public void start()
        throws IOException
    {
        /*
         * The Javadoc of DataSource#start() says it's an error to call the
         * method without being connected and by that time we should have the
         * outputDataSource.
         */
        outputDataSource.start();
        transcodingProcessor.start();
    }

    /*
     * Implements DataSource#stop(). Stops the actual transcoding process if it
     * has already been set up with #connect().
     */
    public void stop()
        throws IOException
    {
        if (outputDataSource != null)
        {
            transcodingProcessor.stop();
            outputDataSource.stop();
        }
    }
}
