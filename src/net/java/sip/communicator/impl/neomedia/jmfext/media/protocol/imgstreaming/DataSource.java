/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.jmfext.media.protocol.imgstreaming;

import javax.media.*;
import javax.media.control.*;
import javax.media.protocol.*;

import net.java.sip.communicator.impl.neomedia.jmfext.media.protocol.*;
import net.java.sip.communicator.impl.neomedia.protocol.*;

import net.sf.fmj.media.protocol.*;

/**
 * Implements <tt>CaptureDevice</tt> and <tt>DataSource</tt> for the purposes of
 * image and desktop streaming.
 *
 * @author Sebastien Vincent
 * @author Lyubomir Marinov
 * @author Damian Minkov
 */
public class DataSource
    extends AbstractVideoPullBufferCaptureDevice
    implements MuteDataSource,
            SourceCloneable
{
    /**
     * Stream created.
     */
    private ImageStream stream = null;

    /**
     * The cloneable representation of this DataSource.
     */
    private CloneableCaptureDevicePullBufferDataSource cloneableDataSource
        = null;

    /**
     * Initializes a new <tt>DataSource</tt> instance.
     */
    public DataSource()
    {
    }

    /**
     * Initializes a new <tt>DataSource</tt> instance.
     *
     * @param locator associated <tt>MediaLocator</tt>
     */
    public DataSource(MediaLocator locator)
    {
        super(locator);
    }

    /**
     * Set origin of <tt>ImageStream</tt>.
     *
     * @param streamIndex stream index
     * @param monitorIndex monitor index
     * @param x x coordinate
     * @param y y coordinate
     */
    public void setOrigin(int streamIndex, int monitorIndex, int x, int y)
    {
        if(stream != null)
        {
            stream.setOrigin(x, y);
            stream.setDisplayIndex(monitorIndex);
        }
    }

    /**
     * Creates a new <tt>PullBufferStream</tt> which is to be at a specific
     * zero-based index in the list of streams of this
     * <tt>PullBufferDataSource</tt>. The <tt>Format</tt>-related information of
     * the new instance is to be abstracted by a specific
     * <tt>FormatControl</tt>.
     *
     * @param streamIndex the zero-based index of the <tt>PullBufferStream</tt>
     * in the list of streams of this <tt>PullBufferDataSource</tt>
     * @param formatControl the <tt>FormatControl</tt> which is to abstract the
     * <tt>Format</tt>-related information of the new instance
     * @return a new <tt>PullBufferStream</tt> which is to be at the specified
     * <tt>streamIndex</tt> in the list of streams of this
     * <tt>PullBufferDataSource</tt> and which has its <tt>Format</tt>-related
     * information abstracted by the specified <tt>formatControl</tt>
     * @see AbstractPullBufferCaptureDevice#createStream(int, FormatControl)
     */
    protected AbstractPullBufferStream createStream(
            int streamIndex,
            FormatControl formatControl)
    {
        /*
         * full desktop: remainder => index
         * part of desktop: remainder => index,x,y
         */
        String remainder = getLocator().getRemainder();
        String split[] = remainder.split(",");
        int index = -1;
        int x = 0;
        int y = 0;

        if ((split != null) && (split.length > 1))
        {
            index = Integer.parseInt(split[0]);
            x = Integer.parseInt(split[1]);
            y = Integer.parseInt(split[2]);
        }
        else
        {
            index = Integer.parseInt(remainder);
        }

        ImageStream stream = new ImageStream(this, formatControl);

        stream.setDisplayIndex(index);
        stream.setOrigin(x, y);

        this.stream = stream;
        return stream;
    }

    /**
     * Determines whether this <tt>DataSource</tt> is mute.
     *
     * An image / desktop sharing does not send any
     * sounds. Thus it is always mute (returns always true).
     *
     * @return <tt>true</tt> since  this <tt>DataSource</tt> is always mute.
     */
    public boolean isMute()
    {
        return true;
    }

    /**
     * Sets the mute state of this <tt>DataSource</tt>.
     *
     * Does not change anything, since image /
     * desktop sharing does not send any sounds. Thus it is always mute.
     *
     * @param mute <tt>true</tt> to mute this <tt>DataSource</tt>; otherwise,
     *            <tt>false</tt>
     */
    public void setMute(boolean mute)
    {
    }

    /**
     * Based on JMF testing, the clone is in the same state as the original
     * (opened and connected if the original is), but at the beginning of the
     * media, not whatever position the original is.
     */
    public javax.media.protocol.DataSource createClone()
    {
        if(this.cloneableDataSource == null)
        {
            this.cloneableDataSource
                = new CloneableCaptureDevicePullBufferDataSource(this);
        }

        return this.cloneableDataSource.createClone();
    }
}
