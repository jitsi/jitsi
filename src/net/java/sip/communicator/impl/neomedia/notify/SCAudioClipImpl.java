/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.notify;

import net.java.sip.communicator.service.audionotifier.*;

/**
 * Common properties impl for SCAudioClip.
 *
 * @author Damian Minkov
 */
public abstract class SCAudioClipImpl
    implements SCAudioClip
{
    private boolean isLooping;

    private int loopInterval;

    private boolean isInvalid;

    /**
     * Returns TRUE if this audio is invalid, FALSE otherwise.
     *
     * @return TRUE if this audio is invalid, FALSE otherwise
     */
    public boolean isInvalid()
    {
        return isInvalid;
    }

    /**
     * Marks this audio as invalid or not.
     *
     * @param isInvalid TRUE to mark this audio as invalid, FALSE otherwise
     */
    public void setInvalid(boolean isInvalid)
    {
        this.setIsInvalid(isInvalid);
    }

    /**
     * Returns TRUE if this audio is currently playing in loop, FALSE otherwise.
     * @return TRUE if this audio is currently playing in loop, FALSE otherwise.
     */
    public boolean isLooping()
    {
        return isLooping;
    }

    /**
     * Returns the loop interval if this audio is looping.
     * @return the loop interval if this audio is looping
     */
    public int getLoopInterval()
    {
        return loopInterval;
    }

    /**
     * @param isLooping the isLooping to set
     */
    public void setIsLooping(boolean isLooping)
    {
        this.isLooping = isLooping;
    }

    /**
     * @param loopInterval the loopInterval to set
     */
    public void setLoopInterval(int loopInterval)
    {
        this.loopInterval = loopInterval;
    }

    /**
     * @param isInvalid the isInvalid to set
     */
    public void setIsInvalid(boolean isInvalid)
    {
        this.isInvalid = isInvalid;
    }

    /**
     * Stops this audio without setting the isLooping property in the case of
     * a looping audio. The AudioNotifier uses this method to stop the audio
     * when setMute(true) is invoked. This allows us to restore all looping
     * audios when the sound is restored by calling setMute(false).
     */
    public abstract void internalStop();
}
