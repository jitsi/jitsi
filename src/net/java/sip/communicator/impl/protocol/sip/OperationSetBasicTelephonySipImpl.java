package net.java.sip.communicator.impl.protocol.sip;

import java.util.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;

/**
 *
 * @author Emil Ivov
 */
public class OperationSetBasicTelephonySipImpl
    implements OperationSetBasicTelephony
{
    public OperationSetBasicTelephonySipImpl()
    {
    }

    /**
     * Registers the specified CallListener with this provider so that it
     * could be notified when incoming calls are received.
     *
     * @param listener the listener to register with this provider.
     */
    public void addCallListener(CallListener listener)
    {
        /** @todo implement addCallListener() */
    }

    /**
     * Indicates a user request to answer an incoming call from the specified
     * CallParticipant.
     *
     * @param participant the call participant that we'd like to anwer.
     */
    public void answerCallParticipant(CallParticipant participant)
    {
        /** @todo implement answerCallParticipant() */
    }

    /**
     * Create a new call and invite the specified CallParticipant to it.
     *
     * @param uri the address of the callee that we should invite to a new
     *   call.
     * @return CallParticipant the CallParticipant that will represented by
     *   the specified uri. All following state change events will be
     *   delivered through that call participant. The Call that this
     *   participant is a member of could be retrieved from the
     *   CallParticipatn instance with the use of the corresponding method.
     * @todo Implement this
     *   net.java.sip.communicator.service.protocol.OperationSetBasicTelephony
     *   method
     */
    public Call createCall(String uri)
    {
        /** @todo implement createCall() */
        return null;
    }

    /**
     * Create a new call and invite the specified CallParticipant to it.
     *
     * @param callee the address of the callee that we should invite to a
     *   new call.
     * @return CallParticipant the CallParticipant that will represented by
     *   the specified uri. All following state change events will be
     *   delivered through that call participant. The Call that this
     *   participant is a member of could be retrieved from the
     *   CallParticipatn instance with the use of the corresponding method.
     */
    public Call createCall(Contact callee)
    {
        /** @todo implement createCall() */
        return null;
    }

    /**
     * Returns an iterator over all currently active calls.
     *
     * @return Iterator
     */
    public Iterator getActiveCalls()
    {
        /** @todo implement getActiveCalls() */
        return null;
    }

    /**
     * Indicates a user request to end a call with the specified call
     * particiapnt.
     *
     * @param participant the participant that we'd like to hang up on.
     */
    public void hangupCallParticipant(CallParticipant participant)
    {
        /** @todo implement hangupCallParticipant() */
    }

    /**
     * Resumes communication with a call participant previously put on hold.
     *
     * @param participant the call participant to put on hold.
     */
    public void putOffHold(CallParticipant participant)
    {
        /** @todo implement putOffHold() */
    }

    /**
     * Puts the specified CallParticipant "on hold".
     *
     * @param participant the participant that we'd like to put on hold.
     */
    public void putOnHold(CallParticipant participant)
    {
        /** @todo implement putOnHold() */
    }

    /**
     * Removes the specified listener from the list of call listeners.
     *
     * @param listener the listener to unregister.
     */
    public void removeCallListener(CallListener listener)
    {
        /** @todo implement removeCallListener() */
    }
}
