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
package net.java.sip.communicator.service.notification;

/**
 * An implementation of the <tt>LogMessageNotificationHandler</tt> interface.
 *
 * @author Yana Stamcheva
 */
public class LogMessageNotificationAction
    extends NotificationAction
{
    /**
     * Indicates that this log is of type trace. If this <tt>logType</tt> is set
     * the messages would be logged as trace logs.
     */
    public static final String TRACE_LOG_TYPE = "TraceLog";

    /**
     * Indicates that this log is of type info.  If this <tt>logType</tt> is set
     * the messages would be logged as info logs.
     */
    public static final String INFO_LOG_TYPE = "InfoLog";

    /**
     * Indicates that this log is of type error. If this <tt>logType</tt> is set
     * the messages would be logged as error logs.
     */
    public static final String ERROR_LOG_TYPE = "ErrorLog";

    private String logType;

    /**
     * Creates an instance of <tt>LogMessageNotificationHandlerImpl</tt> by
     * specifying the log type.
     *
     * @param logType the type of the log
     */
    public LogMessageNotificationAction(String logType)
    {
        super(NotificationAction.ACTION_LOG_MESSAGE);
        this.logType = logType;
    }

    /**
     * Returns the type of the log
     *
     * @return the type of the log
     */
    public String getLogType()
    {
        return logType;
    }
}
