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
package net.java.sip.communicator.service.msghistory.event;

import net.java.sip.communicator.service.history.event.*;

/**
 * When searching into the message history a ProgressEvent is fired whenever
 * the progress is changed. Its fired through the search process
 * informing us about the current progress.
 *
 * @author Damian Minkov
 */
public interface MessageHistorySearchProgressListener
{
    /**
     * The minimum value for the progress change.
     * This is value indicates that the process has started.
     */
    public static int PROGRESS_MINIMUM_VALUE =
        HistorySearchProgressListener.PROGRESS_MINIMUM_VALUE;

    /**
     * The maximum value for the progress change.
     * This is value indicates that the process is finished.
     */
    public static int PROGRESS_MAXIMUM_VALUE =
        HistorySearchProgressListener.PROGRESS_MAXIMUM_VALUE;

    /**
     * This method gets called when progress changes through the search process
     * @param evt ProgressEvent the event holding the search condition and
     *              the current progress value.
     */
    void progressChanged(ProgressEvent evt);
}
