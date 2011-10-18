/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.notification;

/**
 * The <tt>CommandNotificationHandler</tt> interface is meant to be implemented
 * by the notification bundle in order to provide handling of command actions.
 *  
 * @author Yana Stamcheva
 */
public interface CommandNotificationHandler
    extends NotificationActionHandler
{
    /**
     * Executes the program pointed by the descriptor. 
     */
    public void execute();
    
    /**
     * Returns the descriptor pointing to the command to be executed.
     * 
     * @return the descriptor pointing to the command to be executed.
     */
    public String getDescriptor();
}
