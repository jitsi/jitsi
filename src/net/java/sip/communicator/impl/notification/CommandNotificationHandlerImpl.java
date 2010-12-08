/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.notification;

import java.io.*;

import net.java.sip.communicator.service.notification.*;
import net.java.sip.communicator.util.*;

/**
 * An implementation of the <tt>CommandNotificationHandler</tt> interface.
 * 
 * @author Yana Stamcheva
 */
public class CommandNotificationHandlerImpl
    implements CommandNotificationHandler
{
    private Logger logger
        = Logger.getLogger(CommandNotificationHandlerImpl.class);

    private String commandDescriptor;

    private boolean isEnabled = true;

    /**
     * Creates an instance of <tt>CommandNotificationHandlerImpl</tt> by
     * specifying the <tt>commandDescriptor</tt>, which will point us to the
     * command to execute.
     * 
     * @param commandDescriptor a String that should point us to the command to
     * execute
     */
    public CommandNotificationHandlerImpl(String commandDescriptor)
    {
        this.commandDescriptor = commandDescriptor;
    }

    /**
     * Executes the <tt>command</tt>, given by the containing
     * <tt>commandDescriptor</tt>.
     */
    public void execute()
    {
        if(StringUtils.isNullOrEmpty(commandDescriptor, true))
            return;

        try
        {
            Runtime.getRuntime().exec(commandDescriptor);
        }
        catch (IOException e)
        {
            logger.error("Failed execute the following command: "
                + commandDescriptor,  e);
        }
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

    /**
     * Returns TRUE if this notification action handler is enabled and FALSE
     * otherwise. While the notification handler for the command action type
     * is disabled no programs will be executed when the
     * <tt>fireNotification</tt> method is called.
     * 
     * @return TRUE if this notification action handler is enabled and FALSE
     * otherwise
     */
    public boolean isEnabled()
    {
        return isEnabled;
    }

    /**
     * Enables or disables this notification handler. While the notification
     * handler for the command action type is disabled no programs will be
     * executed when the <tt>fireNotification</tt> method is called.
     * 
     * @param isEnabled TRUE to enable this notification handler, FALSE to
     * disable it.
     */
    public void setEnabled(boolean isEnabled)
    {
        this.isEnabled = isEnabled;
    }
}
