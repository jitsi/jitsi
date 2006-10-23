/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol.event;

import java.util.*;


/**
 * Receives events notifying of changes that have occurred within a
 * <tt>CallParticipant</tt>. Such changes may pertain to current call
 * participant state, their display name, address, image and (possibly in the
 * future) others.
 *
 * @author Emil Ivov
 */
public interface CallParticipantListener
    extends EventListener
{

    /**
     * Indicates that a change has occurred in the status of the source
     * CallParticipant.
     *
     * @param evt The <tt>CallParticipantChangeEvent</tt> instance containing
     * the source event as well as its previous and its new status.
     */
    public void participantStateChanged(CallParticipantChangeEvent evt);

    /**
     * Indicates that a change has occurred in the display name of the source
     * CallParticipant.
     *
     * @param evt The <tt>CallParticipantChangeEvent</tt> instance containing
     * the source event as well as its previous and its new display names.
     */
    public void participantDisplayNameChanged(CallParticipantChangeEvent evt);

    /**
     * Indicates that a change has occurred in the address of the source
     * CallParticipant.
     *
     * @param evt The <tt>CallParticipantChangeEvent</tt> instance containing
     * the source event as well as its previous and its new address.
     */
    public void participantAddressChanged(CallParticipantChangeEvent evt);

    /**
     * Indicates that a change has occurred in the transport address that we
     * use to communicate with the participant.
     *
     * @param evt The <tt>CallParticipantChangeEvent</tt> instance containing
     * the source event as well as its previous and its new transport address.
     */
    public void participantTransportAddressChanged(
                                        CallParticipantChangeEvent evt);

    /**
     * Indicates that a change has occurred in the image of the source
     * CallParticipant.
     *
     * @param evt The <tt>CallParticipantChangeEvent</tt> instance containing
     * the source event as well as its previous and its new image.
     */
    public void participantImageChanged(CallParticipantChangeEvent evt);
}
