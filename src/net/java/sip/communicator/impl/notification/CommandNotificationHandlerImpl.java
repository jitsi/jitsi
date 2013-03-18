/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.notification;

import java.io.*;
import java.util.*;

import net.java.sip.communicator.service.notification.*;
import net.java.sip.communicator.util.Logger;

import org.jitsi.util.*;

/**
 * An implementation of the <tt>CommandNotificationHandler</tt> interface.
 *
 * @author Yana Stamcheva
 */
public class CommandNotificationHandlerImpl
    implements CommandNotificationHandler
{
    /**
     * The <tt>Logger</tt> used by this <tt>CommandNotificationHandlerImpl</tt>
     * instance to log debugging information.
     */
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
     * Executes the command, given by the <tt>descriptor</tt> of a specific
     * <tt>CommandNotificationAction</tt>.
     *
     * @param action the action to act upon.
     * @param cmdargs command-line arguments.
     */
    public void execute(
            CommandNotificationAction action,
            Map<String,String> cmdargs)
    {
        String actionDescriptor = action.getDescriptor();

        if(StringUtils.isNullOrEmpty(actionDescriptor, true))
            return;

        if (cmdargs != null)
        {
            for (Map.Entry<String, String> cmdarg : cmdargs.entrySet())
            {
                actionDescriptor
                    = actionDescriptor.replace(
                            "${" + cmdarg.getKey() + "}",
                            cmdarg.getValue());
            }
        }

        try
        {
            Runtime.getRuntime().exec(actionDescriptor);
        }
        catch (IOException ioe)
        {
            logger.error(
                    "Failed to execute the following command: "
                        + action.getDescriptor(),
                    ioe);
        }
    }
}
