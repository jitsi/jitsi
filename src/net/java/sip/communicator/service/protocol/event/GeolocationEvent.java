/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol.event;

import java.util.*;

import net.java.sip.communicator.service.protocol.*;

/**
 * Instances of this class represent a change geographic location of a contact.
 *
 * @author Guillaume Schreiner
 */
public class GeolocationEvent
    extends EventObject
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * The provider that has generated the event.
     */
    private ProtocolProviderService sourceProvider = null;

    /**
     * The contact that this event is pertaining to.
     */
    private Contact sourceContact = null;

    /**
     * The new location of the contact that has caused this event.
     */
    private Map<String, String> newLocation = null;

    /**
     * Creates an event instance indicating that the specified source contact
     * has changed its geographic location to <tt>newLocation</tt>.
     *
     * @param sourceContact the contact associated with this event.
     * @param sourceProvider the protocol provider that the contact belongs to.
     * @param newLocation the geolocation where the sourceCountact currently is.
     * @param geolocationOpSet the operation set that generated this event
     */
    public GeolocationEvent(Contact                 sourceContact,
                            ProtocolProviderService sourceProvider,
                            Map<String, String>     newLocation,
                            OperationSetGeolocation geolocationOpSet)
    {
        super(geolocationOpSet);
        this.sourceContact = sourceContact;
        this.sourceProvider = sourceProvider;
        this.newLocation = newLocation;
    }

    /**
     * Returns the provider that the source contact belongs to.
     *
     * @return the provider that the source contact belongs to.
     */
    public ProtocolProviderService getSourceProvider()
    {
        return sourceProvider;
    }

    /**
     * Returns the source contact associated with the event.
     *
     * @return the source contact associated with the event.
     */
    public Contact getSourceContact()
    {
        return this.sourceContact;
    }

    /**
     * Returns the Geolocation of the contact after this event took place.
     * (i.e. at the time the event is being dispatched).
     *
     * @return geolocation stored into a Map indicating the current location
     * of the source Contact at the moment the event was dispatched.
     */
    public Map<String, String> getNewLocation()
    {
        return newLocation;
    }

    /**
     * Returns the <tt>GeolocationOperationSet</tt> instance that is the source
     * of this event.
     *
     * @return the <tt>OperationSetGeolocation</tt> instance that is the source
     * of this event.
     */
    public OperationSetGeolocation getSourceGeolocationOperationSet()
    {
        return (OperationSetGeolocation)getSource();
    }

    /**
     * Returns a String representation of this GeolocationContactChangeEvent
     *
     * @return A a <tt>java.lang.String</tt> representation of this
     * ContactPresenceStatusChangeEvent.
     */
    public String toString()
    {
        StringBuffer buff = new StringBuffer
            ("ContactGeolocationPresenceChangeEvent-[ ContactID=");
        buff.append(getSourceContact().getAddress());
        return buff.append(", NewLocation=").append(getNewLocation())
            .append("]").toString();
    }

}
