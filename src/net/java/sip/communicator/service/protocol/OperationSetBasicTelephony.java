/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol;

import net.java.sip.communicator.service.protocol.event.*;
import java.util.*;
import java.text.*;

/**
 * An Operation Set defining all basic telephony operations such as conducting
 * simple calls and etc. Note that video is not considered as a part of a
 * supplementary operation set and if included in the service should be available
 * behind the basic telephony set.
 *
 * @author Emil Ivov
 * @author Lubomir Marinov
 */
public interface OperationSetBasicTelephony
    extends OperationSet
{
    /**
     * Registers the specified CallListener with this provider so that it could
     * be notified when incoming calls are received. This method is called
     * by the implementation of the PhoneUI service.
     * @param listener the listener to register with this provider.
     *
     */
    public void addCallListener(CallListener listener);

    /**
     * Removes the specified listener from the list of call listeners.
     * @param listener the listener to unregister.
     */
    public void removeCallListener(CallListener listener);

    /**
     * Create a new call and invite the specified CallParticipant to it.
     *
     * @param uri the address of the callee that we should invite to a new
     * call.
     * @return CallParticipant the CallParticipant that will represented by the
     * specified uri. All following state change events will be delivered
     * through that call participant. The Call that this participant is a member
     * of could be retrieved from the CallParticipatn instance with the use
     * of the corresponding method.
     * @throws OperationFailedException with the corresponding code if we fail
     * to create the call.
     * @throws ParseException if <tt>callee</tt> is not a valid sip address
     * string.
     */
    public Call createCall(String uri)
        throws OperationFailedException
        , ParseException;

    /**
     * Create a new call and invite the specified CallParticipant to it.
     *
     * @param callee the address of the callee that we should invite to a new
     * call.
     * @return CallParticipant the CallParticipant that will represented by the
     * specified uri. All following state change events will be delivered
     * through that call participant. The Call that this participant is a member
     * of could be retrieved from the CallParticipatn instance with the use
     * of the corresponding method.
     * @throws OperationFailedException with the corresponding code if we fail
     * to create the call.
     */
    public Call createCall(Contact callee)
        throws OperationFailedException;

    /**
     * Indicates a user request to answer an incoming call from the specified
     * CallParticipant.
     * @param participant the call participant that we'd like to answer.
     * @throws OperationFailedException with the corresponding code if we
     * encounter an error while performing this operation.
     */
    public void answerCallParticipant(CallParticipant participant)
        throws OperationFailedException;

    /**
     * Puts the specified CallParticipant "on hold". In other words incoming
     * media flows are not played and outgoing media flows are either muted or
     * stopped, without actually interrupting the session.
     * @param participant the participant that we'd like to put on hold.
     * @throws OperationFailedException with the corresponding code if we
     * encounter an error while performing this operation.
     */
    public void putOnHold(CallParticipant participant)
        throws OperationFailedException;

    /**
     * Resumes communication with a call participant previously put on hold. If
     * the specified participant is not "On Hold" at the time putOffHold is
     * called, the method has no effect.
     * 
     * @param participant the call participant to put on hold.
     * @throws OperationFailedException with the corresponding code if we
     *             encounter an error while performing this operation
     */
    public void putOffHold(CallParticipant participant)
        throws OperationFailedException;

    /**
     * Indicates a user request to end a call with the specified call
     * participant.
     * @param participant the participant that we'd like to hang up on.
     * @throws OperationFailedException with the corresponding code if we
     * encounter an error while performing this operation.
     */
    public void hangupCallParticipant(CallParticipant participant)
        throws OperationFailedException;

    /**
     * Returns an iterator over all currently active calls.
     * @return Iterator
     */
    public Iterator getActiveCalls();

    /**
     * Sets the mute state of the audio stream being sent to a specific
     * <tt>CallParticipant</tt>.
     * <p>
     * Muting an audio stream is implementation specific and one of the possible
     * approaches to it is sending silence.
     * </p>
     * 
     * @param participant the <tt>CallParticipant</tt> who receives the audio
     *            stream to have its mute state set
     * @param mute <tt>true</tt> to mute the audio stream being sent to
     *            <tt>participant</tt>; otherwise, <tt>false</tt>
     */
    public void setMute(CallParticipant participant, boolean mute);
}
