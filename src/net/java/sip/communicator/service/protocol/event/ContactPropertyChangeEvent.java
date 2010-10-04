package net.java.sip.communicator.service.protocol.event;

import net.java.sip.communicator.service.protocol.*;

/**
 * A Contact property change event is issued whenever a contact property has
 * changed. Event codes defined in this class describe properties whose changes
 * are being announced through this event.
 *
 * @author Emil Ivov
 */
public class ContactPropertyChangeEvent
    extends java.beans.PropertyChangeEvent
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * Indicates that a change has occurred in the display name of the source
     * contact.
     */
    public static final String PROPERTY_DISPLAY_NAME = "DisplayName";

     /**
     * Indicates that a change has occurred in the image of the source
     * contact.
     */
    public static final String PROPERTY_IMAGE = "Image";

    /**
     * Indicates that a change has occurred in the data that the contact is
     * storing in external sources.
     */
    public static final String PROPERTY_PERSISTENT_DATA = "PersistentData";

    /**
     * Creates a ContactPropertyChangeEvent indicating that a change has
     * occurred for property <tt>propertyName</tt> in the <tt>source</tt>
     * contact and that its value has changed from <tt>oldValue</tt> to
     * <tt>newValue</tt>.
     * <p>
     * @param source the Contact whose property has changed.
     * @param propertyName the name of the property that has changed.
     * @param oldValue the value of the property before the change occurred.
     * @param newValue the value of the property after the change occurred.
     */
    public ContactPropertyChangeEvent( Contact                 source,
                                       String                  propertyName,
                                       Object                  oldValue,
                                       Object                  newValue)
    {
        super(source, propertyName, oldValue, newValue);
    }

    /**
     * Returns a reference to the <tt>Contact</tt> whose property has changed.
     * <p>
     * @return a reference to the <tt>Contact</tt> whose reference has changed.
     */
    public Contact getSourceContact()
    {
        return (Contact)getSource();
    }

    /**
     * Returns a reference to the protocol provider where the event has
     * originated.
     * <p>
     * @return a reference to the ProtocolProviderService instance where this
     * event originated.
     */
    public ProtocolProviderService getProtocolProvider()
    {
        return getSourceContact().getProtocolProvider();
    }

    /**
     * Returns a reference to the source contact parent <tt>ContactGroup</tt>.
     * @return a reference to the <tt>ContactGroup</tt> instance that contains
     * the source <tt>Contact</tt>.
     */
    public ContactGroup getParentContactGroup()
    {
        return getSourceContact().getParentContactGroup();
    }
}
