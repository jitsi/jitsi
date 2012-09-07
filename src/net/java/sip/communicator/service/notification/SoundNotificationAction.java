/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.notification;

/**
 * An implementation of the <tt>SoundNotificationHandlerImpl</tt> interface.
 * 
 * @author Yana Stamcheva
 */
public class SoundNotificationAction
    extends NotificationAction
{
    /**
     * Interval of milliseconds to wait before repeating the sound. -1 means no
     * repetition.
     */
    private int loopInterval;

    /**
     * the descriptor pointing to the sound to be played.
     */
    private String soundFileDescriptor;

    /**
     * The boolean telling if this sound is activated or not.
     */
    private boolean isSoundEnabled;

    /**
     * Creates an instance of <tt>SoundNotification</tt> by
     * specifying the sound file descriptor. The sound is played once.
     * 
     * @param soundDescriptor the sound file descriptor
     */
    public SoundNotificationAction(String soundDescriptor)
    {
        this(soundDescriptor, -1, (soundDescriptor != null));
    }

    /**
     * Creates an instance of <tt>SoundNotification</tt> by
     * specifying the sound file descriptor and the loop interval.
     *
     * @param soundDescriptor the sound file descriptor
     * @param loopInterval the loop interval
     */
    public SoundNotificationAction( String soundDescriptor,
                                    int loopInterval)
    {
        this(soundDescriptor, loopInterval, (soundDescriptor != null));
    }

    /**
     * Creates an instance of <tt>SoundNotification</tt> by
     * specifying the sound file descriptor and the loop interval.
     * 
     * @param soundDescriptor the sound file descriptor
     * @param loopInterval the loop interval
     * @param isSoundEnabled True if this sound is activated. False Otherwise.
     */
    public SoundNotificationAction( String soundDescriptor,
                                            int loopInterval,
                                            boolean isSoundEnabled)
    {
        super(NotificationAction.ACTION_SOUND);
        this.soundFileDescriptor = soundDescriptor;
        this.loopInterval = loopInterval;
        this.isSoundEnabled = isSoundEnabled;
    }

    /**
     * Returns the loop interval. This is the interval of milliseconds to wait
     * before repeating the sound, when playing a sound in loop. By default this
     * method returns -1.
     * 
     * @return the loop interval 
     */
    public int getLoopInterval()
    {
        return loopInterval;
    }

    /**
     * Returns the descriptor pointing to the sound to be played.
     * 
     * @return the descriptor pointing to the sound to be played.
     */
    public String getDescriptor()
    {
        return soundFileDescriptor;
    }

    /**
     * Returns if this sound is activated or not.
     *
     * @return True if this sound is activated. False Otherwise.
     */
    public boolean isSoundEnabled()
    {
        return isSoundEnabled;
    }

    /**
     * Enables or disables this sound notification.
     *
     * @param isSoundEnabled True if this sound is activated. False Otherwise.
     */
    public void setSoundEnabled(boolean isSoundEnabled)
    {
        this.isSoundEnabled = isSoundEnabled;
    }
}
