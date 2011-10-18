/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.notification;

/**
 * The <tt>SoundNotificationHandler</tt> interface is meant to be
 * implemented by the notification bundle in order to provide handling of
 * sound actions.
 * 
 * @author Yana Stamcheva
 */
public interface SoundNotificationHandler
    extends NotificationActionHandler
{
    /**
     * Returns the loop interval. This is the interval of milliseconds to wait
     * before repeating the sound, when playing a sound in loop. If this method
     * returns -1 the sound should not played in loop.
     * 
     * @return the loop interval 
     */
    public int getLoopInterval();
    
    /**
     * Returns the descriptor pointing to the sound to be played.
     * 
     * @return the descriptor pointing to the sound to be played.
     */
    public String getDescriptor();
    
    /**
     * Start playing the sound pointed by <tt>getDescriotor</tt>. This
     * method should check the loopInterval value to distinguish whether to play
     * a simple sound or to play it in loop.
     */
    public void start();
    
    /**
     * Stops playing the sound pointing by <tt>getDescriptor</tt>. This method
     * is meant to be used to stop sounds that are played in loop.
     */
    public void stop();
}