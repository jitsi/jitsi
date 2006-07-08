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
     * Indicates that a change has occurred in the display name of the source
     * contact.
     */
    public static final String PROPERTY_DISPLAY_NAME = "DisplayName";

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
    public ContactPropertyChangeEvent( Contact source,
                                       String  propertyName,
                                       Object  oldValue,
                                       Object  newValue)
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
}
