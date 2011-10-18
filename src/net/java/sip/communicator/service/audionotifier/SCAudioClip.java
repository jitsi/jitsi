/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.audionotifier;

/**
 * SCAudioClip represents an audio clip created using the AudioNotifierService.
 * Like  any audio it could be played, stopped or played in loop.
 *
 * @author Yana Stamcheva
 */
public interface SCAudioClip
{
    /**
     * Plays this audio.
     */
    public void play();

    /**
     * Plays this audio in loop.
     *
     * @param silenceInterval interval between loops
     */
    public void playInLoop(int silenceInterval);

    /**
     * Stops this audio.
     */
    public void stop();
}
