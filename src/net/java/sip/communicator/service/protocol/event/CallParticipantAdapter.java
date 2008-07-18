/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol.event;

/**
 * An abstract adapter class for receiving call participant (change) events.
 * This class exists only as a convenience for creating listener objects.
 * <p>
 * Extend this class to create a <tt>CallParticipantChangeEvent</tt> listener
 * and override the methods for the events of interest. (If you implement the
 * <tt>CallParticipantListener</tt> interface, you have to define all of the
 * methods in it. This abstract class defines null methods for them all, so you
 * only have to define methods for events you care about.)
 * </p>
 *
 * @see CallParticipantChangeEvent
 * @see CallParticipantListener
 *
 * @author Lubomir Marinov
 */
public abstract class CallParticipantAdapter
    implements CallParticipantListener
{

    /**
     * Indicates that a change has occurred in the address of the source
     * CallParticipant.
     * 
     * @param evt The <tt>CallParticipantChangeEvent</tt> instance containing
     *            the source event as well as its previous and its new address.
     */
    public void participantAddressChanged(CallParticipantChangeEvent evt)
    {
    }

    /**
     * Indicates that a change has occurred in the display name of the source
     * CallParticipant.
     * 
     * @param evt The <tt>CallParticipantChangeEvent</tt> instance containing
     *            the source event as well as its previous and its new display
     *            names.
     */
    public void participantDisplayNameChanged(CallParticipantChangeEvent evt)
    {
    }

    /**
     * Indicates that a change has occurred in the image of the source
     * CallParticipant.
     * 
     * @param evt The <tt>CallParticipantChangeEvent</tt> instance containing
     *            the source event as well as its previous and its new image.
     */
    public void participantImageChanged(CallParticipantChangeEvent evt)
    {
    }

    /**
     * Indicates that a change has occurred in the status of the source
     * CallParticipant.
     * 
     * @param evt The <tt>CallParticipantChangeEvent</tt> instance containing
     *            the source event as well as its previous and its new status.
     */
    public void participantStateChanged(CallParticipantChangeEvent evt)
    {
    }

    /**
     * Indicates that a change has occurred in the transport address that we use
     * to communicate with the participant.
     * 
     * @param evt The <tt>CallParticipantChangeEvent</tt> instance containing
     *            the source event as well as its previous and its new transport
     *            address.
     */
    public void participantTransportAddressChanged(
        CallParticipantChangeEvent evt)
    {
    }
}
