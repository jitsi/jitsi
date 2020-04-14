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
package net.java.sip.communicator.impl.protocol.sip;

import javax.sip.*;

/**
 * Represents a processor of events with a specific method received in
 * {@link ProtocolProviderServiceSipImpl} much like <code>SipListener</code> but
 * with the addition of signaling whether the specified event was indeed handled
 * in the processor and needs no further processing in other processors
 * registered for the same method.
 *
 * @author Lubomir Marinov
 */
public interface MethodProcessor
{

    /**
     * Process an asynchronously reported DialogTerminatedEvent. When a dialog
     * transitions to the Terminated state, the stack keeps no further records
     * of the dialog. This notification can be used by applications to clean up
     * any auxiliary data that is being maintained for the given dialog.
     *
     * @param dialogTerminatedEvent an event that indicates that the dialog has
     *            transitioned into the terminated state
     * @return <tt>true</tt> if the specified event has been handled by this
     *         processor and shouldn't be offered to other processors registered
     *         for the same method; <tt>false</tt>, otherwise
     */
    boolean processDialogTerminated(DialogTerminatedEvent dialogTerminatedEvent);

    /**
     * Process an asynchronously reported IO Exception. Asynchronous IO
     * Exceptions may occur as a result of errors during retransmission of
     * requests. The transaction state machine requires to report IO Exceptions
     * to the application immediately (according to RFC 3261). This method
     * enables an implementation to propagate the asynchronous handling of IO
     * Exceptions to the application.
     *
     * @param exceptionEvent the Exception event that is reported to the
     *            application
     * @return <tt>true</tt> if the specified event has been handled by this
     *         processor and shouldn't be offered to other processors registered
     *         for the same method; <tt>false</tt>, otherwise
     */
    boolean processIOException(IOExceptionEvent exceptionEvent);

    /**
     * Processes a Request received on a
     * <code>ProtocolProviderServiceSipImpl</code> upon which this processor is
     * registered.
     *
     * @param requestEvent requestEvent fired from the
     *            <code>ProtocolProviderServiceSipImpl</code> to the processor
     *            representing a Request received from the network
     * @return <tt>true</tt> if the specified event has been handled by this
     *         processor and shouldn't be offered to other processors registered
     *         for the same method; <tt>false</tt>, otherwise
     */
    boolean processRequest(RequestEvent requestEvent);

    /**
     * Processes a Response received on a
     * <code>ProtocolProviderServiceSipImpl</code> upon which this processor is
     * registered.
     *
     * @param responseEvent the responseEvent fired from the
     *            <code>ProtocolProviderServiceSipImpl</code> to the processor
     *            representing a Response received from the network
     * @return <tt>true</tt> if the specified event has been handled by this
     *         processor and shouldn't be offered to other processors registered
     *         for the same method; <tt>false</tt>, otherwise
     */
    boolean processResponse(ResponseEvent responseEvent);

    /**
     * Processes a retransmit or expiration Timeout of an underlying
     * {@link Transaction} handled by this SipListener. This Event notifies the
     * application that a retransmission or transaction Timer expired in the
     * SipProvider's transaction state machine. The TimeoutEvent encapsulates
     * the specific timeout type and the transaction identifier either client or
     * server upon which the timeout occurred. The type of Timeout can by
     * determined by:
     * <code>timeoutType = timeoutEvent.getTimeout().getValue();</code>
     *
     * @param timeoutEvent the timeoutEvent received indicating either the
     *            message retransmit or transaction timed out
     * @return <tt>true</tt> if the specified event has been handled by this
     *         processor and shouldn't be offered to other processors registered
     *         for the same method; <tt>false</tt>, otherwise
     */
    boolean processTimeout(TimeoutEvent timeoutEvent);

    /**
     * Process an asynchronously reported TransactionTerminatedEvent. When a
     * transaction transitions to the Terminated state, the stack keeps no
     * further records of the transaction. This notification can be used by
     * applications to clean up any auxiliary data that is being maintained for
     * the given transaction.
     *
     * @param transactionTerminatedEvent an event that indicates that the
     *            transaction has transitioned into the terminated state
     * @return <tt>true</tt> if the specified event has been handled by this
     *         processor and shouldn't be offered to other processors registered
     *         for the same method; <tt>false</tt>, otherwise
     */
    boolean processTransactionTerminated(
        TransactionTerminatedEvent transactionTerminatedEvent);
}
