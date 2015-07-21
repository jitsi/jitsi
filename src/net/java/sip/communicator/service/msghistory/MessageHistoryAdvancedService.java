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
package net.java.sip.communicator.service.msghistory;

import net.java.sip.communicator.service.protocol.*;

import java.util.*;

/**
 * Adds advanced operation to the message service like inserting/editing
 * messages. Can be used to insert messages when synchronizing history with
 * external source.
 * @author Damian Minkov
 */
public interface MessageHistoryAdvancedService
{
    /**
     * Inserts message to the history. Allows to update the already saved
     * history.
     * @param direction String direction of the message in or out.
     * @param source The source Contact
     * @param destination The destination Contact
     * @param message Message message to be written
     * @param messageTimestamp Date this is the timestamp when was message
     * received that came from the protocol provider
     * @param isSmsSubtype whether message to write is an sms
     */
    public void insertMessage(
        String direction,
        Contact source,
        Contact destination,
        Message message,
        Date messageTimestamp,
        boolean isSmsSubtype);
}
