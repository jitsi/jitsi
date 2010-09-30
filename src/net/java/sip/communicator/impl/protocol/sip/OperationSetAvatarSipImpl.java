/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 * 
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.sip;

import net.java.sip.communicator.service.protocol.*;

/**
 * A simple implementation of the <tt>OperationSetAvatar</tt> interface for the
 * jabber protocol.
 * 
 * @author Damian Minkov
 */
public class OperationSetAvatarSipImpl extends
        AbstractOperationSetAvatar<ProtocolProviderServiceSipImpl>
{

    public OperationSetAvatarSipImpl(
            ProtocolProviderServiceSipImpl parentProvider,
            OperationSetServerStoredAccountInfo accountInfoOpSet)
    {
        super(parentProvider, accountInfoOpSet, 96, 96, 0);
    }

}
