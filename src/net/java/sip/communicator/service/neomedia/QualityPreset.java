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
public class QualityPreset
{
    /**
     * 720p HD
     */
    public final static QualityPreset HD_QUALITY =
            new QualityPreset(new Dimension(1280, 720), 30);

    /**
     * SD
     */
    public final static QualityPreset SD_QUALITY =
            new QualityPreset(new Dimension(640, 480), 20);

    /**
     * Low
     */
    public final static QualityPreset LO_QUALITY =
            new QualityPreset(new Dimension(320, 240), 15);

    /**
     * The frame rate to use.
     */
    private int frameRate;

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
    private QualityPreset(Dimension resolution, int frameRate)
    {
        this.frameRate = frameRate;
        this.resolution = resolution;
    }

    /**
     * Returns this preset frame rate.
     * @return the frame rate.
     */
    public int getFameRate()
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
}
