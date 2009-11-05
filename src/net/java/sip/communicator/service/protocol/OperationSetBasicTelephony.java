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
     * The name of the property that contains the minimum port number that we'd
     * like our RTP managers to bind upon.
     */
    public static final String MIN_MEDIA_PORT_NUMBER_PROPERTY_NAME
        = "net.java.sip.communicator.service.protocol.MIN_MEDIA_PORT_NUMBER";

    /**
     * The name of the property that contains the maximum port number that we'd
     * like our RTP managers to bind upon.
     */
    public static final String MAX_MEDIA_PORT_NUMBER_PROPERTY_NAME
        = "net.java.sip.communicator.service.protocol.MAX_MEDIA_PORT_NUMBER";

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
     * Create a new call and invite the specified CallPeer to it.
     *
     * @param uri the address of the callee that we should invite to a new
     * call.
     * @return CallPeer the CallPeer that will represented by the
     * specified uri. All following state change events will be delivered
     * through that call peer. The Call that this peer is a member
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
     * Create a new call and invite the specified CallPeer to it.
     *
     * @param callee the address of the callee that we should invite to a new
     * call.
     * @return CallPeer the CallPeer that will represented by the
     * specified uri. All following state change events will be delivered
     * through that call peer. The Call that this peer is a member
     * of could be retrieved from the CallParticipatn instance with the use
     * of the corresponding method.
     * @throws OperationFailedException with the corresponding code if we fail
     * to create the call.
     */
    public Call createCall(Contact callee)
        throws OperationFailedException;

    /**
     * Indicates a user request to answer an incoming call from the specified
     * CallPeer.
     * @param peer the call peer that we'd like to answer.
     * @throws OperationFailedException with the corresponding code if we
     * encounter an error while performing this operation.
     */
    public void answerCallPeer(CallPeer peer)
        throws OperationFailedException;

    /**
     * Puts the specified CallPeer "on hold". In other words incoming
     * media flows are not played and outgoing media flows are either muted or
     * stopped, without actually interrupting the session.
     * @param peer the peer that we'd like to put on hold.
     * @throws OperationFailedException with the corresponding code if we
     * encounter an error while performing this operation.
     */
    public void putOnHold(CallPeer peer)
        throws OperationFailedException;

    /**
     * Resumes communication with a call peer previously put on hold. If
     * the specified peer is not "On Hold" at the time putOffHold is
     * called, the method has no effect.
     *
     * @param peer the call peer to put on hold.
     * @throws OperationFailedException with the corresponding code if we
     *             encounter an error while performing this operation
     */
    public void putOffHold(CallPeer peer)
        throws OperationFailedException;

    /**
     * Indicates a user request to end a call with the specified call
     * peer.
     * @param peer the peer that we'd like to hang up on.
     * @throws OperationFailedException with the corresponding code if we
     * encounter an error while performing this operation.
     */
    public void hangupCallPeer(CallPeer peer)
        throws OperationFailedException;

    /**
     * Returns an iterator over all currently active calls.
     * @return Iterator
     */
    public Iterator<? extends Call> getActiveCalls();

    /**
     * Sets the mute state of the audio stream being sent to a specific
     * <tt>CallPeer</tt>.
     * <p>
     * Muting an audio stream is implementation specific and one of the possible
     * approaches to it is sending silence.
     * </p>
     *
     * @param peer the <tt>CallPeer</tt> who receives the audio
     *            stream to have its mute state set
     * @param mute <tt>true</tt> to mute the audio stream being sent to
     *            <tt>peer</tt>; otherwise, <tt>false</tt>
     */
    public void setMute(CallPeer peer, boolean mute);

    /**
     * Adds the given <tt>LocalUserSoundLevelListener</tt> to this operation set.
     * @param l the <tt>LocalUserSoundLevelListener</tt> to add
     */
    public void addLocalUserSoundLevelListener(LocalUserSoundLevelListener l);

    /**
     * Removes the given <tt>LocalUserSoundLevelListener</tt> from this
     * operation set.
     * @param l the <tt>LocalUserSoundLevelListener</tt> to remove
     */
    public void removeLocalUserSoundLevelListener(LocalUserSoundLevelListener l);
}
