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
 * Provides additional information on the transport on which Basic Instant
 * Messaging communication is built. Note that this refers to the
 * characteristics of the instant messaging protocol, not to the underlying TCP
 * or UDP transport layer.
 *
 * This interface defines methods that provide information on the transport
 * facilities that are used by the Basic Instant Messaging protocol
 * implementation. Methods can be used to query the transport channel for
 * information such as maximum message sizes and allowed number of consecutive
 * messages.
 *
 * @author Danny van Heumen
 */
public interface OperationSetBasicInstantMessagingTransport extends OperationSet
{
    /**
     * Constant value indicating unlimited size or number.
     */
    int UNLIMITED = -1;

    /**
     * Compute the maximum message size for a messaging being sent to the
     * provided contact.
     *
     * <p>
     * If there is no limit to the message size, please use constant
     * {@link #UNLIMITED}.
     * </p>
     *
     * @param contact the contact to which the message will be sent
     * @return returns the maximum size of the message or UNLIMITED if there is
     *         no limit
     */
    int getMaxMessageSize(Contact contact);

    /**
     * Compute the maximum number of consecutive messages allowed to be sent to
     * this contact.
     *
     * <p>
     * If there is no limit to the number of messages, please use constant
     * {@link #UNLIMITED}.
     * </p>
     *
     * @param contact the contact to which the messages are sent
     * @return returns the maximum number of messages to send
     */
    int getMaxNumberOfMessages(Contact contact);
}
