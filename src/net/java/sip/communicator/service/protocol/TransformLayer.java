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

import net.java.sip.communicator.service.protocol.event.*;

/**
 * An instance of the <tt>TransformLayer</tt>, when registered with
 * <tt>OperationSetInstantMessageTransform</tt> would be passed all message
 * events. The class looks a lot like a <tt>MessageListener</tt> with the major
 * difference being that all the methods are defined with a return value. The
 * events we return would contain all message details after their transformation
 * from by the layer implementation. All methods return <tt>null</tt> in case
 * the <tt>TransformLayer</tt> implementation determines that the message event
 * should not be determined to the upper layers.
 * <p/>
 * Important Notice: As of May 5 2009, this operation set is still a work in
 * progress and may change significantly in the following months. Any work based
 * on this interface is therefore likely to require frequent updates to keep
 * compatibility.
 *
 * @author Emil Ivov
 *
 */
public interface TransformLayer
{
    /**
     * Called when a new incoming <tt>Message</tt> has been received. The method
     * returns an instance of <tt>MessageReceivedEvent</tt> which in many cases
     * would be different from the <tt>evt</tt> instance that was passed as
     * param. The param and the return instances could very well (and will
     * often) be instances of different implementations so users of this
     * interface (i.e. protocol implementors) should make no assumptions
     * for the class of the return type and copy the returned instance into
     * a new one if necessary.
     *
     * @param evt the <tt>MessageReceivedEvent</tt> containing the newly
     * received message, its sender and other details.
     *
     * @return an instance of a (possibly new) <tt>MessageReceivedEvent</tt>
     * instance containing the transformed message or <tt>null</tt> if the
     * <tt>TransportLayer</tt> has determined that this message event should not
     * be delivered to the upper layers.
     */
    public MessageReceivedEvent messageReceived(MessageReceivedEvent evt);

    /**
     * Called when the underlying implementation has just been asked by other
     * bundles to send an outgoing message. The method returns an instance of
     * <tt>MessageDeliveredEvent</tt> which in many cases would be different
     * from the <tt>evt</tt> instance that was passed as a parameter. The param
     * and the return instances could very well (and will often) be instances of
     * different implementations so users of this interface (i.e. protocol
     * implementors) should make no assumptions for the class of the return type
     * and copy the returned instance into a new one if necessary.
     *
     * @param evt the MessageDeliveredEvent containing the id of the message
     * that has caused the event.
     *
     * @return a number of instances of (possibly new)
     * <tt>MessageDeliveredEvent</tt> instances containing the transformed
     * message(s) or an empty array if the <tt>TransportLayer</tt> has
     * determined that there are no message event that should be delivered to
     * the upper layers.
     */
    public MessageDeliveredEvent[] messageDeliveryPending(MessageDeliveredEvent evt);

    /**
     * Called when the underlying implementation has received an indication
     * that a message, sent earlier has been successfully received by the
     * destination. The method returns an instance of
     * <tt>MessageDeliveredEvent</tt> which in many cases would be different
     * from the <tt>evt</tt> instance that was passed as a parameter. The param
     * and the return instances could very well (and will often) be instances of
     * different implementations so users of this interface (i.e. protocol
     * implementors) should make no assumptions for the class of the return type
     * and copy the returned instance into a new one if necessary.
     *
     * @param evt the MessageDeliveredEvent containing the id of the message
     * that has caused the event.
     *
     * @return an instance of a (possibly new) <tt>MessageDeliveredEvent</tt>
     * instance containing the transformed message or <tt>null</tt> if the
     * <tt>TransportLayer</tt> has determined that this message event should not
     * be delivered to the upper layers.
     */
    public MessageDeliveredEvent messageDelivered(MessageDeliveredEvent evt);

    /**
     * Called to indicated that delivery of a message sent earlier has failed.
     * Reason code and phrase are contained by the <tt>MessageFailedEvent</tt>
     * The method returns an instance of
     * <tt>MessageDeliveredEvent</tt> which in many cases would be different
     * from the <tt>evt</tt> instance that was passed as a parameter. The param
     * and the return instances could very well (and will often) be instances of
     * different implementations so users of this interface (i.e. protocol
     * implementors) should make no assumptions for the class of the return type
     * and copy the returned instance into a new one if necessary.
     *
     * @param evt the <tt>MessageFailedEvent</tt> containing the ID of the
     * message whose delivery has failed.
     *
     * @return an instance of a (possibly new) <tt>MessageDeliveredEvent</tt>
     * instance containing the transformed message or <tt>null</tt> if the
     * <tt>TransportLayer</tt> has determined that this message event should not
     * be delivered to the upper layers.
     */
    public MessageDeliveryFailedEvent
                        messageDeliveryFailed(MessageDeliveryFailedEvent evt);
}
