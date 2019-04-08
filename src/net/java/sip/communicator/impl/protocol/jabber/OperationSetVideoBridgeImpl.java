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

import org.jitsi.xmpp.extensions.colibri.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.service.protocol.media.*;
import org.jivesoftware.smack.*;
import org.jivesoftware.smack.iqrequest.*;
import org.jivesoftware.smack.packet.*;
import org.jxmpp.jid.*;

/**
 * Implements <tt>OperationSetVideoBridge</tt> for Jabber.
 *
 * @author Yana Stamcheva
 * @author Lyubomir Marinov
 */
public class OperationSetVideoBridgeImpl
    extends AbstractIqRequestHandler
    implements OperationSetVideoBridge,
               RegistrationStateChangeListener
{
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
        super(
            ColibriConferenceIQ.ELEMENT_NAME,
            ColibriConferenceIQ.NAMESPACE,
            IQ.Type.set,
            Mode.async);
        this.protocolProvider = protocolProvider;
        this.protocolProvider.addRegistrationStateChangeListener(this);
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
        Jid jitsiVideobridge = protocolProvider.getJitsiVideobridge();

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
        if (IQ.Type.set.equals(conferenceIQ.getType())
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

    @Override
    public IQ handleIQRequest(IQ iqRequest)
    {
        ColibriConferenceIQ conferenceIQ = (ColibriConferenceIQ) iqRequest;
        processColibriConferenceIQ(conferenceIQ);
        return IQ.createResultIQ(iqRequest);
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
            protocolProvider.getConnection().registerIQRequestHandler(this);
        }
        else if (RegistrationState.UNREGISTERED.equals(registrationState))
        {
            XMPPConnection connection = protocolProvider.getConnection();

            if (connection != null)
                connection.unregisterIQRequestHandler(this);
        }
    }
}
