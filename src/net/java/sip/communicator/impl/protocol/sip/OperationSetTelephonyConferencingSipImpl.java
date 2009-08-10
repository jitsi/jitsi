/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.sip;

import net.java.sip.communicator.service.protocol.*;

/**
 * @author Lubomir Marinov
 */
public class OperationSetTelephonyConferencingSipImpl
    implements OperationSetTelephonyConferencing
{

    /**
     * The SIP <code>ProtocolProviderService</code> implementation which created
     * this instance and for which telephony conferencing services are being
     * provided by this instance.
     */
    private final ProtocolProviderServiceSipImpl parentProvider;

    /**
     * Initializes a new <code>OperationSetTelephonyConferencingSipImpl</code>
     * instance which is to provide telephony conferencing services for a
     * specific SIP <code>ProtocolProviderService</code> implementation.
     * 
     * @param parentProvider
     *            the SIP <code>ProtocolProviderService</code> which has
     *            requested the creation of the new instance and for which the
     *            new instance is to provide telephony conferencing services
     */
    public OperationSetTelephonyConferencingSipImpl(
        ProtocolProviderServiceSipImpl parentProvider)
    {
        this.parentProvider = parentProvider;
    }

    public Call createConfCall(String[] callees)
        throws OperationNotSupportedException
    {
        throw new OperationNotSupportedException();
    }

    public CallPeer inviteCalleeToCall(String uri, Call existingCall)
        throws OperationNotSupportedException
    {
        throw new OperationNotSupportedException();
    }
}
