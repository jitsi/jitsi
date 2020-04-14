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
package net.java.sip.communicator.service.protocol.event;

import java.util.*;

import net.java.sip.communicator.service.protocol.*;

/**
 * The <tt>FileTransferProgressEvent</tt> indicates the progress of a file
 * transfer.
 *
 * @author Yana Stamcheva
 */
public class FileTransferProgressEvent
    extends EventObject
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * Indicates the progress of a file transfer in bytes.
     */
    private long progress;

    /**
     * Indicates when this event occured.
     */
    private long timestamp;

    /**
     * Creates a <tt>FileTransferProgressEvent</tt> by specifying the source
     * file transfer object, that triggered the event and the new progress
     * value.
     *
     * @param fileTransfer the source file transfer object, that triggered the
     * event
     * @param timestamp when this event occured
     * @param progress the new progress value
     */
    public FileTransferProgressEvent(   FileTransfer fileTransfer,
                                        long timestamp,
                                        long progress)
    {
        super(fileTransfer);

        this.timestamp = timestamp;
        this.progress = progress;
    }

    /**
     * Returns the source <tt>FileTransfer</tt> that triggered this event.
     *
     * @return the source <tt>FileTransfer</tt> that triggered this event
     */
    public FileTransfer getFileTransfer()
    {
        return (FileTransfer) source;
    }

    /**
     * Returns the progress of the file transfer in transferred bytes.
     *
     * @return the progress of the file transfer
     */
    public long getProgress()
    {
        return progress;
    }

    /**
     * Returns the timestamp when this event initially occured.
     *
     * @return the timestamp when this event initially occured
     */
    public long getTimestamp()
    {
        return timestamp;
    }
}
