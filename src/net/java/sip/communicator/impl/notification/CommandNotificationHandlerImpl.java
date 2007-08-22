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
}
