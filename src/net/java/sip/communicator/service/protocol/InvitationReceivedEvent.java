/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol;

/**
 * Delivered when we receive an invitation for joining an existing chat room.
 * If the corresponding chat room requires a password, then it could be
 * retreived by the getPassword() method. The method would return null if no
 * password is necessary to join the specified chat room.
 *
 * @author Emil Ivov
 */
public class InvitationReceivedEvent
    extends java.util.EventObject
{

    public InvitationReceivedEvent(ProtocolProviderService srcProvider,
                                   ChatRoom chatRoom)
    {
        super(srcProvider);
    }
}
