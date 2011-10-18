/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol;

import java.text.*;
import java.util.*;

import net.java.sip.communicator.service.neomedia.*;
import net.java.sip.communicator.service.protocol.event.*;

/**
 * An Operation Set defining all basic telephony operations such as conducting
 * simple calls and etc. Note that video is not considered as a part of a
 * supplementary operation set and if included in the service should be available
 * behind the basic telephony set.
 *
 * @param <T> the provider extension class like for example
 * <tt>ProtocolProviderServiceSipImpl</tt> or
 * <tt>ProtocolProviderServiceJabberImpl</tt>
 *
 * @author Emil Ivov
 * @author Lyubomir Marinov
 */
public interface OperationSetBasicTelephony<T extends ProtocolProviderService>
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
     * Creates a new <tt>Call</tt> and invites a specific <tt>CallPeer</tt> to
     * it given by her <tt>String</tt> URI.
     *
     * @param uri the address of the callee who we should invite to a new
     * <tt>Call</tt>
     * @return a newly created <tt>Call</tt>. The specified <tt>callee</tt> is
     * available in the <tt>Call</tt> as a <tt>CallPeer</tt>
     * @throws OperationFailedException with the corresponding code if we fail
     * to create the call
     * @throws ParseException if <tt>callee</tt> is not a valid SIP address
     * <tt>String</tt>
     */
    public Call createCall(String uri)
        throws OperationFailedException,
               ParseException;

    /**
     * Creates a new <tt>Call</tt> and invites a specific <tt>CallPeer</tt>
     * to it given by her <tt>Contact</tt>.
     *
     * @param callee the address of the callee who we should invite to a new
     * call
     * @return a newly created <tt>Call</tt>. The specified <tt>callee</tt> is
     * available in the <tt>Call</tt> as a <tt>CallPeer</tt>
     * @throws OperationFailedException with the corresponding code if we fail
     * to create the call
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
     * Sets the mute state of the <tt>Call</tt>.
     * <p>
     * Muting audio streams sent from the call is implementation specific
     * and one of the possible approaches to it is sending silence.
     * </p>
     *
     * @param call the <tt>Call</tt> whos mute state is set
     * @param mute <tt>true</tt> to mute the call streams being sent to
     *            <tt>peers</tt>; otherwise, <tt>false</tt>
     */
    public void setMute(Call call, boolean mute);

    /**
     * Returns the protocol provider that this operation set belongs to.
     *
     * @return a reference to the <tt>ProtocolProviderService</tt> that created
     * this operation set.
     */
    public T getProtocolProvider();

    /**
     * Creates a new <tt>Recorder</tt> which is to record the specified
     * <tt>Call</tt> (into a file which is to be specified when starting the
     * returned <tt>Recorder</tt>).
     *
     * @param call the <tt>Call</tt> which is to be recorded by the returned
     * <tt>Recorder</tt> when the latter is started
     * @return a new <tt>Recorder</tt> which is to record the specified
     * <tt>call</tt> (into a file which is to be specified when starting the
     * returned <tt>Recorder</tt>)
     * @throws OperationFailedException if anything goes wrong while creating
     * the new <tt>Recorder</tt> for the specified <tt>call</tt>
     */
    public Recorder createRecorder(Call call)
        throws OperationFailedException;
}
