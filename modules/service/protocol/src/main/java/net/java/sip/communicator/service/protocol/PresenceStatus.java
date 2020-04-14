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
package net.java.sip.communicator.service.protocol;

/**
 * The class is used to represent the state of the connection of a given
 * ProtocolProvider or Contact. It is up to the implementation to determine the
 * exact states that an object might go through. An IM provider for example
 * might go through states like, CONNECTING, ON-LINE, AWAY, etc, A status
 * instance is represented by an integer varying from 0 to 100, a Status Name
 * and a Status Description.
 *
 * The integer status variable is used so that the users of the service get the
 * notion of whether or not a given Status instance represents a state that
 * allows communication (above 20) and so that it could compare instances
 * between themselves (e.g. for sorting a ContactList for example).
 *
 * A state may not be created by the user. User may request a status change
 * giving parameters requested by the ProtocolProvider. Once a statue is
 * successfully entered by the provider, a ConnectivityStatus instance is
 * conveyed to the user through a notification event.
 *
 * @author Emil Ivov
 */
public class PresenceStatus
    implements Comparable<PresenceStatus>
{
    /**
     * An integer above which all values of the status coefficient indicate that
     * a status with connectivity (communication is possible).
     */
    public static final int ONLINE_THRESHOLD = 20;

    /**
     * An integer above which all values of the status coefficient indicate both
     * connectivity and availability but the person is away from the computer.
     * This value has special meaning, it is the border between dnd and away and
     * statuses with this value are normally considered on the phone.
     */
    public static final int EXTENDED_AWAY_THRESHOLD = 31;

    /**
     * An integer above which all values of the status coefficient indicate both
     * connectivity and availability but the person is away from the computer.
     */
    public static final int AWAY_THRESHOLD = 36;

    /**
     * An integer above which all values of the status coefficient indicate both
     * connectivity and availability.
     */
    public static final int AVAILABLE_THRESHOLD = 50;

    /**
     * An integer above which all values of the status coefficient indicate
     * eagerness to communicate
     */
    public static final int EAGER_TO_COMMUNICATE_THRESHOLD = 80;

    /**
     * An integer indicating the maximum possible value of the status field.
     */
    public static final int MAX_STATUS_VALUE = 100;

    /**
     * An image that graphically represents the status.
     */
    protected final byte[] statusIcon;

    /**
     * Represents the connectivity status on a scale from 0 to 100 with 0
     * indicating complete disability for communication and 100 maximum ability
     * and user willingness. Implementors of this service should respect the
     * following indications for status values. 0 - complete disability 1:10 -
     * initializing. 1:20 - trying to enter a state where communication is
     * possible (Connecting ..) 20:50 - communication is possible but might be
     * unwanted, inefficient or delayed(e.g. Away state in IM clients) 50:80 -
     * communication is possible (On - line) 80:100 - communication is possible
     * and user is eager to communicate. (Free for chat! Talk to me, etc.)
     */
    protected final int status;

    /**
     * The name of this status instance (e.g. Away, On-line, Invisible, etc.)
     */
    protected final String statusName;

    /**
     * Creates an instance of this class using the specified parameters.
     *
     * @param status the status variable representing the new instance
     * @param statusName the name of this PresenceStatus
     */
    protected PresenceStatus(int status, String statusName)
    {
        this(status, statusName, null);
    }

    /**
     * Creates an instance of this class using the specified parameters.
     *
     * @param status the status variable representing the new instance
     * @param statusName the name of this PresenceStatus
     * @param statusIcon an image that graphically represents the status or null
     *            if no such image is available.
     */
    protected PresenceStatus(int status, String statusName, byte[] statusIcon)
    {
        this.status = status;
        this.statusName = statusName;
        this.statusIcon = statusIcon;
    }

    /**
     * Returns an integer representing the presence status on a scale from 0 to
     * 100.
     *
     * @return a short indicating the level of availability corresponding to
     *         this status object.
     */
    public int getStatus()
    {
        return status;
    }

    /**
     * Returns the name of this status (such as Away, On-line, Invisible, etc).
     *
     * @return a String variable containing the name of this status instance.
     */
    public String getStatusName()
    {
        return statusName;
    }

    /**
     * Returns a string representation of this provider status. Strings returned
     * by this method have the following format: PresenceStatus:<STATUS_STRING>:
     * <STATUS_MESSAGE> and are meant to be used for logging/debugging purposes.
     *
     * @return a string representation of this object.
     */
    @Override
    public String toString()
    {
        return "PresenceStatus:" + getStatusName();
    }

    /**
     * Indicates whether the user is Online (can be reached) or not.
     *
     * @return true if the the status coefficient is higher than the
     *         ONLINE_THRESHOLD and false otherwise
     */
    public boolean isOnline()
    {
        return getStatus() >= ONLINE_THRESHOLD;
    }

    /**
     * Indicates whether the user is both Online and avaliable (can be reached
     * and is likely to respond) or not.
     *
     * @return true if the the status coefficient is higher than the
     *         AVAILABLE_THRESHOLD and false otherwise
     */
    public boolean isAvailable()
    {
        return getStatus() >= AVAILABLE_THRESHOLD;
    }

    /**
     * Indicates whether the user is Online, available and eager to communicate
     * (can be reached and is likely to become annoyingly talkative if
     * contacted).
     *
     * @return true if the the status coefficient is higher than the
     *         EAGER_TO_COMMUNICATE_THRESHOLD and false otherwise
     */
    public boolean isEagerToCommunicate()
    {
        return getStatus() >= EAGER_TO_COMMUNICATE_THRESHOLD;
    }

    /**
     * Compares this instance with the specified object for order. Returns a
     * negative integer, zero, or a positive integer as this status instance is
     * considered to represent less, as much, or more availability than the one
     * specified by the parameter.
     * <p>
     *
     * @param target the <code>PresenceStatus</code> to be compared.
     * @return a negative integer, zero, or a positive integer as this object is
     *         less than, equal to, or greater than the specified object.
     *
     * @throws ClassCastException if the specified object's type prevents it
     *             from being compared to this Object.
     * @throws NullPointerException if o is null
     */
    public int compareTo(PresenceStatus target)
        throws ClassCastException,
               NullPointerException
    {
        return (getStatus() - target.getStatus());
    }

    /**
     * Indicates whether some other object is "equal to" this one. To
     * PresenceStatus instances are considered equal if and only if both their
     * connectivity coefficient and their name are equal.
     * <p>
     *
     * @param obj the reference object with which to compare.
     * @return <tt>true</tt> if this presence status instance is equal to the
     *         <code>obj</code> argument; <tt>false</tt> otherwise.
     */
    @Override
    public boolean equals(Object obj)
    {
        if (obj == null || !(obj instanceof PresenceStatus))
            return false;

        PresenceStatus status = (PresenceStatus) obj;

        return status.getStatus() == getStatus()
            && status.getStatusName().equals(getStatusName());
    }

    /**
     * Returns a hash code value for the object. This method is supported for
     * the benefit of hashtables such as those provided by
     * <tt>java.util.Hashtable</tt>.
     * <p>
     *
     * @return a hash code value for this object (which is actually the result
     *         of the getStatusName().hashCode()).
     */
    @Override
    public int hashCode()
    {
        return getStatusName().hashCode();
    }

    /**
     * Returns an image that graphically represents the status.
     *
     * @return a byte array containing the image that graphically represents the
     *         status or null if no such image is available.
     */
    public byte[] getStatusIcon()
    {
        return statusIcon;
    }
}
