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
package net.java.sip.communicator.impl.protocol.jabber;

import java.util.*;

import net.java.sip.communicator.impl.protocol.jabber.extensions.colibri.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.service.protocol.media.*;
import net.java.sip.communicator.util.*;

import org.jivesoftware.smack.*;
import org.jivesoftware.smack.filter.*;
import org.jivesoftware.smack.packet.*;

/**
 * Implements <tt>OperationSetVideoBridge</tt> for Jabber.
 *
 * @author Yana Stamcheva
 * @author Lyubomir Marinov
 */
public class OperationSetVideoBridgeImpl
    implements OperationSetVideoBridge,
               PacketFilter,
               PacketListener,
               RegistrationStateChangeListener
{
    /**
     * The <tt>Logger</tt> used by the <tt>OperationSetVideoBridgeImpl</tt>
     * class and its instances for logging output.
     */
    private static final Logger logger
        = Logger.getLogger(OperationSetVideoBridgeImpl.class);

    /**
     * The <tt>ProtocolProviderService</tt> implementation which initialized
     * this instance, owns it and is often referred to as its parent.
     */
    private final ProtocolProviderServiceJabberImpl protocolProvider;

    /**
     * Creates an instance of <tt>OperationSetVideoBridgeImpl</tt> by
     * specifying the parent <tt>ProtocolProviderService</tt> announcing this
     * operation set.
     *
     * @param protocolProvider the parent Jabber protocol provider
     */
    public OperationSetVideoBridgeImpl(
            ProtocolProviderServiceJabberImpl protocolProvider)
    {
        this.protocolProvider = protocolProvider;
        this.protocolProvider.addRegistrationStateChangeListener(this);
    }

    /**
     * Implements {@link PacketFilter}. Determines whether this instance is
     * interested in a specific {@link Packet}.
     * <tt>OperationSetVideoBridgeImpl</tt> returns <tt>true</tt> if the
     * specified <tt>packet</tt> is a {@link ColibriConferenceIQ}; otherwise,
     * <tt>false</tt>.
     *
     * @param packet the <tt>Packet</tt> to be determined whether this instance
     * is interested in it
     * @return <tt>true</tt> if the specified <tt>packet</tt> is a
     * <tt>ColibriConferenceIQ</tt>; otherwise, <tt>false</tt>
     */
    public boolean accept(Packet packet)
    {
        return (packet instanceof ColibriConferenceIQ);
    }

    /**
     * Creates a conference call with the specified callees as call peers via a
     * video bridge provided by the parent Jabber provider.
     *
     * @param callees the list of addresses that we should call
     * @return the newly created conference call containing all CallPeers
     * @throws OperationFailedException if establishing the conference call
     * fails
     * @throws OperationNotSupportedException if the provider does not have any
     * conferencing features.
     */
    public Call createConfCall(String[] callees)
        throws OperationFailedException,
               OperationNotSupportedException
    {
        return
            protocolProvider
                .getOperationSet(OperationSetTelephonyConferencing.class)
                    .createConfCall(
                            callees,
                            new MediaAwareCallConference(true));
    }

    /**
     * Invites the callee represented by the specified uri to an already
     * existing call using a video bridge provided by the parent Jabber provider.
     * The difference between this method and createConfCall is that
     * inviteCalleeToCall allows a user to add new peers to an already
     * established conference.
     *
     * @param uri the callee to invite to an existing conf call.
     * @param call the call that we should invite the callee to.
     * @return the CallPeer object corresponding to the callee represented by
     * the specified uri.
     * @throws OperationFailedException if inviting the specified callee to the
     * specified call fails
     * @throws OperationNotSupportedException if allowing additional callees to
     * a pre-established call is not supported.
     */
    public CallPeer inviteCalleeToCall(String uri, Call call)
        throws OperationFailedException,
        OperationNotSupportedException
    {
        return
            protocolProvider
                .getOperationSet(OperationSetTelephonyConferencing.class)
                    .inviteCalleeToCall(uri, call);
    }

    /**
     * Indicates if there's an active video bridge available at this moment. The
     * Jabber provider may announce support for video bridge, but it should not
     * be used for calling until it becomes actually active.
     *
     * @return <tt>true</tt> to indicate that there's currently an active
     * available video bridge, <tt>false</tt> - otherwise
     */
    public boolean isActive()
    {
        String jitsiVideobridge = protocolProvider.getJitsiVideobridge();

        return ((jitsiVideobridge != null) && (jitsiVideobridge.length() > 0));
    }

    /**
     * Notifies this instance that a specific <tt>ColibriConferenceIQ</tt> has
     * been received.
     *
     * @param conferenceIQ the <tt>ColibriConferenceIQ</tt> which has been
     * received
     */
    private void processColibriConferenceIQ(ColibriConferenceIQ conferenceIQ)
    {
        /*
         * The application is not a Jitsi Videobridge server, it is a client.
         * Consequently, the specified ColibriConferenceIQ is sent to it in
         * relation to the part of the application's functionality which makes
         * requests to a Jitsi Videobridge server i.e. CallJabberImpl.
         *
         * Additionally, the method processColibriConferenceIQ is presently tasked
         * with processing ColibriConferenceIQ requests only. They are SET IQs
         * sent by the Jitsi Videobridge server to notify the application about
         * updates in the states of (colibri) conferences organized by the
         * application.
         */
        if (IQ.Type.SET.equals(conferenceIQ.getType())
                && conferenceIQ.getID() != null)
        {
            OperationSetBasicTelephony<?> basicTelephony
                = protocolProvider.getOperationSet(
                        OperationSetBasicTelephony.class);

            if (basicTelephony != null)
            {
                Iterator<? extends Call> i = basicTelephony.getActiveCalls();

                while (i.hasNext())
                {
                    Call call = i.next();

                    if (call instanceof CallJabberImpl)
                    {
                        CallJabberImpl callJabberImpl = (CallJabberImpl) call;
                        MediaAwareCallConference conference
                            = callJabberImpl.getConference();

                        if ((conference != null)
                                && conference.isJitsiVideobridge())
                        {
                            /*
                             * TODO We may want to disallow rogue CallJabberImpl
                             * instances which may throw an exception to prevent
                             * the conferenceIQ from reaching the CallJabberImpl
                             * instance which it was meant for.
                             */
                            if (callJabberImpl.processColibriConferenceIQ(
                                    conferenceIQ))
                                break;
                        }
                    }
                }
            }
        }
    }

    /**
     * Implements {@link PacketListener}. Notifies this instance that a specific
     * {@link Packet} (which this instance has already expressed interest into
     * by returning <tt>true</tt> from {@link #accept(Packet)}) has been
     * received.
     *
     * @param packet the <tt>Packet</tt> which has been received and which this
     * instance is given a chance to process
     */
    public void processPacket(Packet packet)
    {
        /*
         * As we do elsewhere, acknowledge the receipt of the Packet first and
         * then go about our business with it.
         */
        IQ iq = (IQ) packet;

        if (iq.getType() == IQ.Type.SET)
            protocolProvider.getConnection().sendPacket(IQ.createResultIQ(iq));

        /*
         * Now that the acknowledging is out of the way, do go about our
         * business with the Packet.
         */
        ColibriConferenceIQ conferenceIQ = (ColibriConferenceIQ) iq;
        boolean interrupted = false;

        try
        {
            processColibriConferenceIQ(conferenceIQ);
        }
        catch (Throwable t)
        {
            logger.error(
                    "An error occurred during the processing of a "
                        + packet.getClass().getName() + " packet",
                    t);

            if (t instanceof InterruptedException)
            {
                /*
                 * We cleared the interrupted state of the current Thread by
                 * catching the InterruptedException. However, we do not really
                 * care whether the current Thread has been interrupted - we
                 * caught the InterruptedException because we want to swallow
                 * any Throwable. Consequently, we should better restore the
                 * interrupted state.
                 */
                interrupted = true;
            }
            else if (t instanceof ThreadDeath)
                throw (ThreadDeath) t;
        }
        if (interrupted)
            Thread.currentThread().interrupt();
    }

    /**
     * {@inheritDoc}
     *
     * Implements {@link RegistrationStateChangeListener}. Notifies this
     * instance that there has been a change in the <tt>RegistrationState</tt>
     * of {@link #protocolProvider}. Subscribes this instance to
     * {@link ColibriConferenceIQ}s as soon as <tt>protocolProvider</tt> is
     * registered and unsubscribes it as soon as <tt>protocolProvider</tt> is
     * unregistered.
     */
    public void registrationStateChanged(RegistrationStateChangeEvent ev)
    {
        RegistrationState registrationState = ev.getNewState();

        if (RegistrationState.REGISTERED.equals(registrationState))
        {
            protocolProvider.getConnection().addPacketListener(this, this);
        }
        else if (RegistrationState.UNREGISTERED.equals(registrationState))
        {
            XMPPConnection connection = protocolProvider.getConnection();

            if (connection != null)
                connection.removePacketListener(this);
        }
    }
}
