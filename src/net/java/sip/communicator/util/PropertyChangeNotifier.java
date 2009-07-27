/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.util;

import java.util.*;

/**
 * Represents a source of <code>PropertyChangeEvent</code>s which notifies
 * <code>PropertyChangeListener</code>s about changes in the values of
 * properties.
 * 
 * @author Lubomir Marinov
 */
public class PropertyChangeNotifier
{

    /**
     * The list of <code>PropertyChangeListener</code>s interested in and
     * notified about changes in the values of the properties of this
     * <code>PropertyChangeNotifier</code>.
     */
    private final List<PropertyChangeListener> listeners
        = new Vector<PropertyChangeListener>();

    /**
     * Adds a specific <code>PropertyChangeListener</code> to the list of
     * listeners interested in and notified about changes in the values of the
     * properties of this <code>PropertyChangeNotifier</code>.
     * 
     * @param listener
     *            a <code>PropertyChangeListener</code> to be notified about
     *            changes in the values of the properties of this
     *            <code>PropertyChangeNotifier</code>. If the specified listener
     *            is already in the list of interested listeners (i.e. it has
     *            been previously added), it is not added again.
     */
    public void addPropertyChangeListener(PropertyChangeListener listener)
    {
        if (listener != null)
            synchronized (listeners)
            {
                if (!listeners.contains(listener))
                    listeners.add(listener);
            }
    }

    /**
     * Removes a specific <code>PropertyChangeListener</code> from the list of
     * listeners interested in and notified about changes in the values of the
     * properties of this <code>PropertyChangeNotifer</code>.
     * 
     * @param listener
     *            a <code>PropertyChangeListener</code> to no longer be notified
     *            about changes in the values of the properties of this
     *            <code>PropertyChangeNotifier</code>
     */
    public void removePropertyChangeListener(PropertyChangeListener listener)
    {
        if (listener != null)
            synchronized (listeners)
            {
                listeners.remove(listener);
            }
    }

    /**
     * Fires a new <code>PropertyChangeEvent</code> to the
     * <code>PropertyChangeListener</code>s registered with this
     * <code>PropertyChangeNotifier</code> in order to notify about a change in
     * the value of a specific property which had its old value modified to a
     * specific new value.
     * 
     * @param property
     *            the name of the property of this
     *            <code>PropertyChangeNotifier</code> which had its value
     *            changed
     * @param oldValue
     *            the value of the property with the specified name before the
     *            change
     * @param newValue
     *            the value of the property with the specified name after the
     *            change
     */
    protected void firePropertyChange(
        String property,
        Object oldValue,
        Object newValue)
    {
        PropertyChangeListener[] listeners;
        synchronized (this.listeners)
        {
            listeners
                = this.listeners
                        .toArray(
                            new PropertyChangeListener[this.listeners.size()]);
        }

        PropertyChangeEvent event
            = new PropertyChangeEvent(
                    getPropertyChangeSource(property, oldValue, newValue),
                    property,
                    oldValue,
                    newValue);

        for (PropertyChangeListener listener : listeners)
            listener.propertyChange(event);
    }

    /**
     * Gets the <code>Object</code> to be reported as the source of a new
     * <code>PropertyChangeEvent</code> which is to notify the
     * <code>PropertyChangeListener</code>s registered with this
     * <code>PropertyChangeNotifier</code> about the change in the value of a
     * property with a specific name from a specific old value to a specific new
     * value.
     * 
     * @param property
     *            the name of the property which had its value changed from the
     *            specified old value to the specified new value
     * @param oldValue
     *            the value of the property with the specified name before the
     *            change
     * @param newValue
     *            the value of the property with the specified name after the
     *            change
     * @return the <code>Object</code> to be reported as the source of the new
     *         <code>PropertyChangeEvent</code> which is to notify the
     *         <code>PropertyChangeListener</code>s registered with this
     *         <code>PropertyChangeNotifier</code> about the change in the value
     *         of the property with the specified name from the specified old
     *         value to the specified new value
     */
    protected Object getPropertyChangeSource(
        String property,
        Object oldValue,
        Object newValue)
    {
        return this;
    }
}
