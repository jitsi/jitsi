/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol;

/**
 * The class is used to represent the state of the connection
 * of a given ProtocolProvider or Contact. It is up to the implementation to
 * determine the exact states that an object might go through. An IM provider
 * for example might go through states like, CONNECTING, ON-LINE, AWAY, etc, A
 * status instance is represented by an integer varying from 0 to
 * 100, a Status Name and a Status Description.
 *
 * The integer status variable is used so that the users of the service get the
 * notion of whether or not a given Status instance represents a state that
 * allows communication (above 20) and so that it could compare instances
 * between themselves (e.g. for sorting a ContactList for example).
 *
 * A state may not be created by the user. User may request a status change
 * giving parameters requested by the ProtocolProvider. Once a statue is
 * successfully entered by the provider, a ConnectivityStatus instacne is
 * conveyed to the user through a notification event.
 *
 * @author Emil Ivov
 */
public class PresenceStatus
        implements Comparable
{

    /**
     * Represents the connectivity status on a scale from
     * 0 to 100  with 0 indicating complete disabiilty for communication and 100
     * maximum ability and user willingness. Implementors of this service should
     * respect the following indications for status values.
     * 0 - complete disability
     * 1:10 - initializing.
     * 1:20 - trying to enter a state where communication is possible (Connecting ..)
     * 20:50 - communication is possible but might be unwanted, inefficient or delayed(e.g. Away state in IM clients)
     * 50:80 - communication is possible (On - line)
     * 80:100 - communication is possible and user is eager to communicate. (Free for chat! Talk to me, etc.)
     */
    protected short status = 0;

    /**
     * The name of this status instance (e.g. Away, On-line, Invisible, etc.)
     */
    protected String statusName = null;

    /**
     * A message describing this status instance (e.g. I am busy right now, and
     * I'll get back to you later).
     */
    protected String statusMessage = null;

    /**
     * Creates an instance of this class using the specified parameters.
     * @param status the status variable representing the new instance
     * @param statusName the name of this PresenceStatus
     * @param statusMessage a message describing the user's status.
     */
    protected PresenceStatus(short status, String statusName, String statusMessage)
    {
        this.status = status;
        this.statusName = statusName;
        this.statusMessage = statusMessage;
    }


    /**
     * Returns an integer representing the presence status on a scale from
     * 0 to 100.
     * @return a short indicating the level of availability corresponding to
     * this status object.
     */
    public short getStatus()
    {
        return status;
    }

    /**
     * Returns the name of this status (such as Away, On-line, Invisible, etc).
     * @return a String variable containing the name of this status instance.
     */
    public String getStatusName()
    {
        return statusName;
    }

    /**
     * Returns a description of the status (like for example a note giving
     * details on that status like "out for a piss" for example).
     * @return a String variable detailing the status.
     */
    public String getStatusMessage()
    {
        return statusMessage;
    }

    /**
     * Returns a string represenation of this provider status. Strings returned
     * by this method have the following format: PresenceStatus:<STATUS_STRING>:
     * <STATUS_MESSAGE> and are meant to be used for loggin/debugging purposes.
     * @return a string representation of this object.
     */
    public String toString()
    {
        return getClass().getName()
            + ":" + getStatusName()
            + ":" + getStatusMessage();
    }

    /**
     * Compares this inatance with the specified object for order.  Returns a
     * negative integer, zero, or a positive integer as this status instance is
     * considered to represent less, as much, or more availabilite than the one
     * specified by the parameter.<p>
     *
     * @param   o the Object to be compared.
     * @return  a negative integer, zero, or a positive integer as this object
     *		is less than, equal to, or greater than the specified object.
     *
     * @throws ClassCastException if the specified object's type prevents it
     *         from being compared to this Object.
     * @throws NullPointerException if o is null
     */
    public int compareTo(Object o)
        throws ClassCastException, NullPointerException
    {
        PresenceStatus target = (PresenceStatus)o;
        return (getStatus() - target.getStatus());
    }
}
