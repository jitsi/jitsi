/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Copyright @ 2015 Atlassian Pty Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.java.sip.communicator.impl.notification;

import static net.java.sip.communicator.service.notification.LogMessageNotificationAction.ERROR_LOG_TYPE;
import static net.java.sip.communicator.service.notification.LogMessageNotificationAction.INFO_LOG_TYPE;
import static net.java.sip.communicator.service.notification.LogMessageNotificationAction.TRACE_LOG_TYPE;
import net.java.sip.communicator.service.notification.*;
import net.java.sip.communicator.util.*;

/**
 * An implementation of the <tt>LogMessageNotificationHandler</tt> interface.
 *
 * @author Yana Stamcheva
 */
public class LogMessageNotificationHandlerImpl
    implements LogMessageNotificationHandler
{
    /**
     * The logger that will be used to log messages.
     */
    private Logger logger
        = Logger.getLogger(LogMessageNotificationHandlerImpl.class);

    /**
     * {@inheritDoc}
     */
    public String getActionType()
    {
        return NotificationAction.ACTION_LOG_MESSAGE;
    }

    /**
     * Logs a message through the sip communicator Logger.
     *
     * @param action the action to act upon
     * @param message the message coming from the event
     */
    public void logMessage(LogMessageNotificationAction action, String message)
    {
        if (action.getLogType().equals(ERROR_LOG_TYPE))
            logger.error(message);
        else if(action.getLogType().equals(INFO_LOG_TYPE))
            logger.info(message);
        else if(action.getLogType().equals(TRACE_LOG_TYPE))
            logger.trace(message);
    }
}
