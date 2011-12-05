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
    extends NotificationHandler
{
    /**
     * Start playing the sound pointed by <tt>getDescriotor</tt>. This
     * method should check the loopInterval value to distinguish whether to play
     * a simple sound or to play it in loop.
     * @param action the action to act upon
     * @param data Additional data for the event.
     */
    public void start(SoundNotificationAction action, NotificationData data);
    
    /**
     * Stops playing the sound pointing by <tt>getDescriptor</tt>. This method
     * is meant to be used to stop sounds that are played in loop.
     * @param data Additional data for the event.
     */
    public void stop(NotificationData data);
}