/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.notification;

import java.util.*;

/**
 * The <tt>CommandNotificationHandler</tt> interface is meant to be implemented
 * by the notification bundle in order to provide handling of command actions.
 *  
 * @author Yana Stamcheva
 */
public interface CommandNotificationHandler
    extends NotificationHandler
{
    /**
     * Executes the program pointed by the descriptor.
     * @param action the action to act upon
     * @param cmdargs arguments that are passed to the command line specified
     * in the action
     */
    public void execute(CommandNotificationAction action,
        Map<String,String> cmdargs);
}
