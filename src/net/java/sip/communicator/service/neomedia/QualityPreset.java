/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.neomedia;

import java.awt.*;

/**
 * Predefined quality preset used to specify some video settings during an
 * existing call or when starting a new call.
 *
 * @author Damian Minkov
 * @author Lyubomir Marinov
 */
public class QualityPreset
    implements Comparable<QualityPreset>
{
    /**
     * 720p HD
     */
    public static final QualityPreset HD_QUALITY
        = new QualityPreset(new Dimension(1280, 720), 30);

    /**
     * Low
     */
    public static final QualityPreset LO_QUALITY
        = new QualityPreset(new Dimension(320, 240), 15);

    /**
     * SD
     */
    public static final QualityPreset SD_QUALITY
        = new QualityPreset(new Dimension(640, 480), 20);

    /**
     * The frame rate to use.
     */
    private final float frameRate;

    /**
     * The resolution to use.
     */
    private final Dimension resolution;

    /**
     * Initializes a new quality preset with a specific <tt>resolution</tt> and
     * a specific <tt>frameRate</tt>.
     *
     * @param resolution the resolution
     * @param frameRate the frame rate
     */
    public QualityPreset(Dimension resolution, float frameRate)
    {
        this.frameRate = frameRate;
        this.resolution = resolution;
    }

    /**
     * Initializes a new quality preset with a specific <tt>resolution</tt> and
     * an unspecified <tt>frameRate</tt>.
     *
     * @param resolution the resolution
     */
    public QualityPreset(Dimension resolution)
    {
        this(resolution, -1 /* unspecified */);
    }

    /**
     * Returns this preset frame rate.
     * @return the frame rate.
     */
    public float getFameRate()
    {
        return frameRate;
    }

    /**
     * Returns this preset resolution.
     * @return the resolution.
     */
    public Dimension getResolution()
    {
        return resolution;
    }

    /**
     * Compares to presets and its dimensions.
     * @param o object to compare to.
     * @return a negative integer, zero, or a positive integer as this object is
     * less than, equal to, or greater than the specified object.
     */
    public int compareTo(QualityPreset o)
    {
        if(resolution.equals(o.resolution))
            return 0;
        else if((resolution.height < o.resolution.height)
                && (resolution.width < o.resolution.width))
            return -1;
        else
            return 1;
    }
}
