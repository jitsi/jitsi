/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
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
