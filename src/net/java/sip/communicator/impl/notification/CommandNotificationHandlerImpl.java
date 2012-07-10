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
     * @param cmdargs command-line arguments.
     */
    public void execute(CommandNotificationAction action,
        Map<String,String> cmdargs)
    {
        if(StringUtils.isNullOrEmpty(action.getDescriptor(), true))
            return;

        String actionDescriptor = action.getDescriptor();
        if (cmdargs != null)
        {
            for (Map.Entry<String, String> entry : cmdargs.entrySet())
            {
                if(actionDescriptor.indexOf("${" + entry.getKey() + "}") != -1)
                {
                    actionDescriptor = actionDescriptor.replace(
                        "${" + entry.getKey() + "}",
                        entry.getValue()
                    );
                }
            }
        }

        try
        {
            Runtime.getRuntime().exec(actionDescriptor);
        }
        catch (IOException e)
        {
            logger.error("Failed execute the following command: "
                + action.getDescriptor(),  e);
        }
    }
}
