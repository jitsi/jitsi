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
 * Interacts with user for received transfer request for unknown calls.
 *
 * @author Damian Minkov
 */
public interface TransferAuthority
{
    /**
     * Checks with user for unknown transfer. Returns <tt>true</tt> if user
     * accepts and we must process the transfer, <tt>false</tt> otherwise.
     *
     * @param fromContact the contact initiating the transfer.
     * @param transferTo the address we will be transferred to.
     * @return <tt>true</tt> if transfer is allowed to process, <tt>false</tt>
     * otherwise.
     */
    public boolean processTransfer(Contact fromContact, String transferTo);

    /**
     * Checks with user for unknown transfer. Returns <tt>true</tt> if user
     * accepts and we must process the transfer, <tt>false</tt> otherwise.
     *
     * @param fromAddress the address initiating the transfer.
     * @param transferTo the address we will be transferred to.
     * @return <tt>true</tt> if transfer is allowed to process, <tt>false</tt>
     * otherwise.
     */
    public boolean processTransfer(String fromAddress, String transferTo);
}
