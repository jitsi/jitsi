/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.notification;

/**
 * An implementation of the <tt>CommandNotificationHandler</tt> interface.
 * 
 * @author Yana Stamcheva
 */
public class CommandNotificationAction
    extends NotificationAction
{
    private String commandDescriptor;

    /**
     * Creates an instance of <tt>CommandNotification</tt> by
     * specifying the <tt>commandDescriptor</tt>, which will point us to the
     * command to execute.
     * 
     * @param commandDescriptor a String that should point us to the command to
     * execute
     */
    public CommandNotificationAction(String commandDescriptor)
    {
        super(NotificationAction.ACTION_COMMAND);
        this.commandDescriptor = commandDescriptor;
    }

    /**
     * Returns the command descriptor.
     * 
     * @return the command descriptor
     */
    public String getDescriptor()
    {
        return commandDescriptor;
    }
}
