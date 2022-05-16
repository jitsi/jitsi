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

/**
 * A listener that would gather events notifying of incoming file transfer
 * requests.
 *
 * @author Emil Ivov
 * @author Yana Stamcheva
 */
public interface FileTransferListener
    extends EventListener
{
    /**
     * Called when a new <tt>IncomingFileTransferRequest</tt> has been received.
     *
     * @param event the <tt>FileTransferRequestEvent</tt> containing the newly
     * received request and other details.
     */
    public void fileTransferRequestReceived(FileTransferRequestEvent event);

    /**
     * Called when a <tt>FileTransferCreatedEvent</tt> has been received.
     *
     * @param event the <tt>FileTransferCreatedEvent</tt> containing the newly
     * received file transfer and other details.
     */
    public void fileTransferCreated(FileTransferCreatedEvent event);

    /**
     * Called when an <tt>IncomingFileTransferRequest</tt> has been rejected.
     *
     * @param event the <tt>FileTransferRequestEvent</tt> containing the
     * received request which was rejected.
     */
    public void fileTransferRequestRejected(FileTransferRequestEvent event);

    /**
     * Called when an <tt>IncomingFileTransferRequest</tt> has been canceled
     * from the contact who sent it.
     *
     * @param event the <tt>FileTransferRequestEvent</tt> containing the
     * request which was canceled.
     */
    public void fileTransferRequestCanceled(FileTransferRequestEvent event);
}
