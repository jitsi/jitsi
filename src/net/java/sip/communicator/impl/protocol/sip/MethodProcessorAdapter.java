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
 * Provides a default implementation of <code>MethodProcessor</code> which does
 * no processing and exists only as a convenience to extenders so that they can
 * override the methods of interest.
 *
 * @author Lyubomir Marinov
 */
public class MethodProcessorAdapter
    implements MethodProcessor
{

    /*
     * Implements
     * MethodProcessor#processDialogTerminated(DialogTerminatedEvent).
     */
    public boolean processDialogTerminated(
        DialogTerminatedEvent dialogTerminatedEvent)
    {
        return false;
    }

    /*
     * Implements MethodProcessor#processIOException(IOExceptionEvent).
     */
    public boolean processIOException(IOExceptionEvent exceptionEvent)
    {
        return false;
    }

    /*
     * Implements MethodProcessor#processRequest(RequestEvent).
     */
    public boolean processRequest(RequestEvent requestEvent)
    {
        return false;
    }

    /*
     * Implements MethodProcessor#processResponse(ResponseEvent).
     */
    public boolean processResponse(ResponseEvent responseEvent)
    {
        return false;
    }

    /*
     * Implements MethodProcessor#processTimeout(TimeoutEvent).
     */
    public boolean processTimeout(TimeoutEvent timeoutEvent)
    {
        return false;
    }

    /*
     * Implements
     * MethodProcessor#processTransactionTerminated(TransactionTerminatedEvent).
     */
    public boolean processTransactionTerminated(
        TransactionTerminatedEvent transactionTerminatedEvent)
    {
        return false;
    }
}
