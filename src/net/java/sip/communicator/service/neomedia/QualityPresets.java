/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.neomedia;

import java.awt.*;

/**
 * Predefined Quality presets used to specify some video settings during
 * call or when starting call.
 * @author Damian Minkov
 */
public class QualityPresets
    implements Comparable<QualityPresets>
{
    /**
     * 720p HD
     */
    public final static QualityPresets HD_QUALITY =
            new QualityPresets(new Dimension(1280, 720), 30);

    /**
     * SD
     */
    public final static QualityPresets SD_QUALITY =
            new QualityPresets(new Dimension(640, 480), 20);

    /**
     * Low
     */
    public final static QualityPresets LO_QUALITY =
            new QualityPresets(new Dimension(320, 240), 15);

    /**
     * The frame rate to use.
     */
    private float frameRate;

    /**
     * The resolution to use.
     */
    private Dimension resolution;

    /**
     * Creates preset with <tt>resolution</tt> and <tt>frameRate</tt>.
     * Predefined presets can be created only here.
     *
     * @param resolution the resolution.
     * @param frameRate the frame rate.
     */
    public QualityPresets(Dimension resolution, float frameRate)
    {
        this.frameRate = frameRate;
        this.resolution = resolution;
    }

    /**
     * Creates preset with <tt>resolution</tt> and <tt>frameRate</tt>.
     * Predefined presets can be created only here.
     *
     * @param resolution the resolution.
     */
    public QualityPresets(Dimension resolution)
    {
        // unspecified frame rate
        this(resolution, -1);
    }

    /**
     * Returns this preset frame rate.
     * @return the frame rate.
     */
    public float getFameRate()
    {
        return this.frameRate;
    }

    /**
     * Returns this preset resolution.
     * @return the resolution.
     */
    public Dimension getResolution()
    {
        return this.resolution;
    }

    /**
     * Compares to presets and its dimensions.
     * @param o object to compare to.
     * @return a negative integer, zero, or a positive integer as this object is
     *         less than, equal to, or greater than the specified object.
     */
    public int compareTo(QualityPresets o)
    {
        if(this.resolution.equals(o.resolution))
            return 0;
        else if(this.resolution.height < o.resolution.height &&
                this.resolution.width < o.resolution.width)
            return -1;
        else
            return 1;
    }
}
