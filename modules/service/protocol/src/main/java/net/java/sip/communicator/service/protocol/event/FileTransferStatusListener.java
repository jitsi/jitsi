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

/**
 * The <tt>FileTransferStatusListener</tt> listens for
 * <tt>FileTransferStatusChangeEvent</tt> in order to indicate a change in the
 * current status of a file transfer.
 *
 * @author Yana Stamcheva
 */
public interface FileTransferStatusListener
{
    /**
     * Indicates a change in the file transfer status.
     *
     * @param event the event containing information about the change
     */
    public void statusChanged(FileTransferStatusChangeEvent event);
}
