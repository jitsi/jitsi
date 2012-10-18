/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber;

import java.util.*;

import org.jivesoftware.smack.*;
import org.jivesoftware.smackx.packet.*;

import net.java.sip.communicator.impl.protocol.jabber.extensions.cobri.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.media.*;

/**
 * Implements <tt>OperationSetVideoBridge</tt> for Jabber.
 *
 * @author Yana Stamcheva
 */
public class OperationSetVideoBridgeImpl
    implements OperationSetVideoBridge
{
    /**
     * The parent protocol provider.
     */
    private final ProtocolProviderServiceJabberImpl protocolProvider;

    /**
     * Creates an instance of <tt>OperationSetVideoBridgeImpl</tt> by
     * specifying the parent <tt>ProtocolProviderService</tt> announcing this
     * operation set.
     *
     * @param protocolProvider the parent Jabber protocol provider
     */
    public OperationSetVideoBridgeImpl(ProtocolProviderServiceJabberImpl
                                                            protocolProvider)
    {
        this.protocolProvider = protocolProvider;
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
        return protocolProvider
                .getOperationSet(OperationSetTelephonyConferencing.class)
                    .createConfCall(callees,
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
        return protocolProvider.getOperationSet(
                OperationSetTelephonyConferencing.class).inviteCalleeToCall(
                                                        uri,
                                                        call);
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
        String jitsiVideoBridge = protocolProvider.getJitsiVideoBridge();

        return (jitsiVideoBridge != null
                && jitsiVideoBridge.length() > 0);
    }
}
