/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
 package net.java.sip.communicator.service.gui.event;

import net.java.sip.communicator.service.protocol.event.*;

/**
  * The UserActionListener allows interested parties (such as a telephony
  * protocol implementation) to register for notifications upon user requests
  * relating to conversation management (such as establishing or ending a call
  * with a specified call participant). The reason that we use such an
  * EventListener rather that putting all these methods in a Provider and
  * letting the Provider implementation handle them is that the user interface
  * is not supposed to show any intelligence concerning telephony. It is not,
  * for example, supposed to know that sip:emcho@sipphone.com is a sip URI and
  * not a phone number. Listening telephony providers on the other hand would be
  * able to recognize URIs they know how to handle and handle the event as well
  * as call its consume() method so that it is not dispatched to other
  * listeners.
  * @author Emil Ivov
  */
public interface UserActionListener
    extends java.util.EventListener
{
    /**
     *
     * @param evt CalleeInvitationEvent
     */
    public void handleHangupRequest(CallParticipantControlEvent evt);

    public void handleAnswerRequest(CallParticipantControlEvent evt);

    /**
     * @todo add some a target url to the call control listener
     * @param evt UserCallControlEvent
     */
    public void handleTransferRequest(CallParticipantControlEvent evt);

    public void handleExitRequest();
}
