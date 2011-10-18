/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.conference;

import javax.media.protocol.*;

/**
 * Describes additional information about a specific input audio
 * <tt>SourceStream</tt> of an <tt>AudioMixer</tt> so that the
 * <tt>AudioMixer</tt> can, for example, quickly discover the output
 * <tt>AudioMixingPushBufferDataSource</tt> in the mix of which the contribution
 * of the <tt>SourceStream</tt> is to not be included.
 * <p>
 * Private to <tt>AudioMixer</tt> and <tt>AudioMixerPushBufferStream</tt> but
 * extracted into its own file for the sake of clarity.
 * </p>
 *
 * @author Lubomir Marinov
 */
class InputStreamDesc
{

    /**
     * The <tt>DataSource</tt> which created the <tt>SourceStream</tt> described
     * by this instance and additional information about it.
     */
    public final InputDataSourceDesc inputDataSourceDesc;

    /**
     * The <tt>SourceStream</tt> for which additional information is described
     * by this instance.
     */
    private SourceStream inputStream;

    /**
     * The number of reads of this input stream which did not return any
     * samples.
     */
    long nonContributingReadCount;

    /**
     * Initializes a new <tt>InputStreamDesc</tt> instance which is to describe
     * additional information about a specific input audio <tt>SourceStream</tt>
     * of an <tt>AudioMixer</tt>. Associates the specified <tt>SourceStream</tt>
     * with the <tt>DataSource</tt> which created it and additional information
     * about it.
     *
     * @param inputStream a <tt>SourceStream</tt> for which additional
     * information is to be described by the new instance
     * @param inputDataSourceDesc the <tt>DataSource</tt> which created the
     * <tt>SourceStream</tt> to be described by the new instance and additional
     * information about it
     */
    public InputStreamDesc(
        SourceStream inputStream,
        InputDataSourceDesc inputDataSourceDesc)
    {
        this.inputStream = inputStream;
        this.inputDataSourceDesc = inputDataSourceDesc;
    }

    /**
     * Gets the <tt>SourceStream</tt> described by this instance.
     *
     * @return the <tt>SourceStream</tt> described by this instance
     */
    public SourceStream getInputStream()
    {
        return inputStream;
    }

    /**
     * Gets the <tt>AudioMixingPushBufferDataSource</tt> in which the mix
     * contribution of the <tt>SourceStream</tt> described by this instance is
     * to not be included.
     *
     * @return the <tt>AudioMixingPushBufferDataSource</tt> in which the mix
     * contribution of the <tt>SourceStream</tt> described by this instance is
     * to not be included
     */
    public AudioMixingPushBufferDataSource getOutputDataSource()
    {
        return inputDataSourceDesc.outputDataSource;
    }

    /**
     * Sets the <tt>SourceStream</tt> to be described by this instance.
     *
     * @param inputStream the <tt>SourceStream</tt> to be described by this
     * instance
     */
    public void setInputStream(SourceStream inputStream)
    {
        this.inputStream = inputStream;
    }
}
