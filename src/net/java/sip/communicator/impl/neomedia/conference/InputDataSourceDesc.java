/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.conference;

import java.io.*;

import javax.media.protocol.*;

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
     * The <tt>DataSource</tt> for which additional information is described by
     * this instance.
     */
    public final DataSource inputDataSource;

    /**
     * The <tt>AudioMixingPushBufferDataSource</tt> in which the mix
     * contributions of {@link inputDataSource} are to not be included.
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

    void disconnect()
    {
        getEffectiveInputDataSource().disconnect();
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
    public DataSource getEffectiveInputDataSource()
    {
        return
            (transcodingDataSource == null)
                ? inputDataSource
                : transcodingDataSource;
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
    public void setTranscodingDataSource(DataSource transcodingDataSource)
    {
        this.transcodingDataSource = transcodingDataSource;
    }

    void start()
        throws IOException
    {
        getEffectiveInputDataSource().start();
    }

    void stop()
        throws IOException
    {
        getEffectiveInputDataSource().stop();
    }
}
