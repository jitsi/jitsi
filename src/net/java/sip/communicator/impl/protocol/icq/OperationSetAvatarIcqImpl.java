/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.icq;

import net.java.sip.communicator.service.protocol.*;

/**
 * A simple implementation of the <tt>OperationSetAvatar</tt> interface for the
 * icq protocol.
 *
 * @author Damian Minkov
 */
public class OperationSetAvatarIcqImpl
    extends AbstractOperationSetAvatar<ProtocolProviderServiceIcqImpl>
{
    public OperationSetAvatarIcqImpl(
            ProtocolProviderServiceIcqImpl parentProvider,
            OperationSetServerStoredAccountInfo accountInfoOpSet)
    {
        super(parentProvider, accountInfoOpSet, 0, 0, 0);
    }
}
