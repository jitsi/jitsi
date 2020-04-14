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
package net.java.sip.communicator.service.protocol;

/**
 * An <tt>OperationSet</tt> defining operations that allow transferring calls to
 * a new location.
 *
 * @author Emil Ivov
 */
public interface OperationSetCallTransfer
    extends OperationSet
{
    /**
     * Indicates a user request to transfer the specified call participant to a
     * new (target) uri.
     *
     * @param peer the call peer we'd like to transfer
     * @param targetURI the uri that we'd like this call peer to be
     * transferred to.
     */
    public void transferCallPeer(CallPeer peer,
                                 String   targetURI);
}
