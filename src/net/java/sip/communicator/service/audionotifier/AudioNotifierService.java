/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.audionotifier;

/**
 * The AudioNotifierService is meant to be used to control all sounds in the
 * application. An audio could be created by calling the createAudio method.
 * In order to stop all sounds in the application one could call the setMute
 * method. To check whether the sound is currently enabled the isMute method
 * could be used.
 *
 * @author Yana Stamcheva
 */
public interface AudioNotifierService
{
    /**
     * Creates an SCAudioClip and returns it.
     * @param uri the uri, which will be the source of the audio
     * @return the created SCAudioClip, that could be played.
     */
    public SCAudioClip createAudio(String uri);

    /**
     * Destroys the given audio.
     *
     * @param audio <tt>SCAudioClip</tt> to destroy
     */
    public void destroyAudio(SCAudioClip audio);

    /**
     * Stops/Restores all currently playing sounds.
     *
     * @param isMute mute or not currently playing sounds
     */
    public void setMute(boolean isMute);

    /**
     * Specifies if currently the sound is off.
     *
     * @return TRUE if currently the sound is off, FALSE otherwise
     */
    public boolean isMute();
}
