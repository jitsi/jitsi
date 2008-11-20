/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.util;

import java.util.*;

/**
 * A "ConfigurationChange" event gets delivered whenever a someone changes a
 * configuration property. A ConfigurationEvent object is sent as an argument to
 * the ConfigurationChangeListener methods.
 * <P>
 * Normally ConfigurationChangeEvents are accompanied by the name and the old
 * and new values of the changed property. If the new value is a primitive type
 * (such as int or boolean) it must be wrapped as the corresponding java.lang.*
 * Object type (such as Integer or Boolean).
 * <P>
 * Null values may be provided for the old and the new values if their true
 * values are not known.
 * <P>
 * An event source may send a null object as the name to indicate that an
 * arbitrary set of if its properties have changed. In this case the old and new
 * values should also be null.
 * <P>
 * In the case where the event reflects the change of a constrained property, it
 * will first be dispatched to all propertyWillChange methods and only in case
 * that none of them has objected (no ChangeVetoException has been thrown) the
 * propertyChange method is called.
 * 
 * @author Emil Ivov
 */
public class PropertyChangeEvent
    extends EventObject
{

    /**
     * name of the property that changed. May be null, if not known.
     * 
     * @serial
     */
    private final String propertyName;

    /**
     * New value for property. May be null if not known.
     * 
     * @serial
     */
    private final Object newValue;

    /**
     * Previous value for property. May be null if not known.
     * 
     * @serial
     */
    private final Object oldValue;

    /**
     * Constructs a new <tt>PropertyChangeEvent</tt>.
     * 
     * @param source The bean that fired the event.
     * @param propertyName The programmatic name of the property that was
     *            changed.
     * @param oldValue The old value of the property.
     * @param newValue The new value of the property.
     */
    public PropertyChangeEvent(Object source, String propertyName,
        Object oldValue, Object newValue)
    {
        super(source);

        this.propertyName = propertyName;
        this.newValue = newValue;
        this.oldValue = oldValue;
    }

    /**
     * Gets the programmatic name of the property that was changed.
     * 
     * @return The programmatic name of the property that was changed. May be
     *         null if multiple properties have changed.
     */
    public String getPropertyName()
    {
        return propertyName;
    }

    /**
     * Sets the new value for the property, expressed as an Object.
     * 
     * @return The new value for the property, expressed as an Object. May be
     *         null if multiple properties have changed.
     */
    public Object getNewValue()
    {
        return newValue;
    }

    /**
     * Gets the old value for the property, expressed as an Object.
     * 
     * @return The old value for the property, expressed as an Object. May be
     *         null if multiple properties have changed.
     */
    public Object getOldValue()
    {
        return oldValue;
    }
}
