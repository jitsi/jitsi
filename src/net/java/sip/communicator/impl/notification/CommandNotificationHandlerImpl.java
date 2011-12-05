/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
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

    /**
     * {@inheritDoc}
     */
    public String getActionType()
    {
        return NotificationAction.ACTION_COMMAND;
    }

    /**
     * Executes the command, given by the <tt>commandDescriptor</tt> of the
     * action.
     * 
     * @param action the action to act upon.
     */
    public void execute(CommandNotificationAction action)
    {
        if(StringUtils.isNullOrEmpty(action.getDescriptor(), true))
            return;

        try
        {
            Runtime.getRuntime().exec(action.getDescriptor());
        }
        catch (IOException e)
        {
            logger.error("Failed execute the following command: "
                + action.getDescriptor(),  e);
        }
    }
}
