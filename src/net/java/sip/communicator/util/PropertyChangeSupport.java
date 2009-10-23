/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.util;

/**
 * Represents a mechanism to easily add to a specific <tt>Object</tt> by means
 * of composition support for firing <tt>PropertyChangeEvent</tt>s to
 * <tt>PropertyChangeListener</tt>s.
 *
 * @author Lubomir Marinov
 */
public class PropertyChangeSupport
    extends PropertyChangeNotifier
{

    /**
     * The <tt>Object</tt> to be reported as the source of the
     * <tt>PropertyChangeEvent</tt>s fired by this instance.
     */
    private final Object source;

    /**
     * Initializes a new <tt>PropertyChangeSupport</tt> which is to fire
     * <tt>PropertyChangeEvent</tt>s and to report their source as a specific
     * <tt>Object</tt>
     *
     * @param source the <tt>Object</tt> to be reported as the source of the
     * <tt>PropertyChangeEvent</tt>s fired by the new instance
     */
    public PropertyChangeSupport(Object source)
    {
        this.source = source;
    }

    /**
     * Fires a new <tt>PropertyChangeEvent</tt> to the
     * <tt>PropertyChangeListener</tt>s registered with this
     * <tt>PropertyChangeSupport</tt> in order to notify about a change in the
     * value of a specific property which had its old value modified to a
     * specific new value.
     *
     * @param property the name of the property of this
     * <tt>PropertyChangeSupport</tt> which had its value changed
     * @param oldValue the value of the property with the specified name before
     * the change
     * @param newValue the value of the property with the specified name after
     * the change
     * @see PropertyChangeNotifier#firePropertyChange(String, Object, Object)
     */
    @Override
    public void firePropertyChange(
            String property,
            Object oldValue,
            Object newValue)
    {
        super.firePropertyChange(property, oldValue, newValue);
    }

    /**
     * Gets the <tt>Object</tt> to be reported as the source of a new
     * <tt>PropertyChangeEvent</tt> which is to notify the
     * <tt>PropertyChangeListener</tt>s registered with this
     * <tt>PropertyChangeSupport</tt> about the change in the value of a
     * property with a specific name from a specific old value to a specific new
     * value.
     *
     * @param property the name of the property which had its value changed from
     * the specified old value to the specified new value
     * @param oldValue the value of the property with the specified name before
     * the change
     * @param newValue the value of the property with the specified name after
     * the change
     * @return the <tt>Object</tt> to be reported as the source of the new
     * <tt>PropertyChangeEvent</tt> which is to notify the
     * <tt>PropertyChangeListener</tt>s registered with this
     * <tt>PropertyChangeSupport</tt> about the change in the value of the
     * property with the specified name from the specified old value to the
     * specified new value
     * @see PropertyChangeNotifier#getPropertyChangeSource(String, Object, Object)
     */
    @Override
    protected Object getPropertyChangeSource(
            String property,
            Object oldValue,
            Object newValue)
    {
        return source;
    }
}
