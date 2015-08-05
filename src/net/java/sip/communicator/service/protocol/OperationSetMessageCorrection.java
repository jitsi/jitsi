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
 * Provides functionality for correcting instant messages.
 *
 * @author Ivan Vergiliev
 */
public interface OperationSetMessageCorrection
    extends OperationSetBasicInstantMessaging
{
    /**
     * Replaces the message with ID <tt>correctedMessageUID</tt> sent to
     * the contact <tt>to</tt> with the message <tt>message</tt>
     *
     * @param to The contact to send the message to.
     * @param resource The ContactResource to send the message to.
     * @param message The new message.
     * @param correctedMessageUID The ID of the message being replaced.
     */
    public void correctMessage(Contact to, ContactResource resource,
        Message message, String correctedMessageUID);
}
