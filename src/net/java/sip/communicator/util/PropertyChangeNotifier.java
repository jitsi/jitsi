/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.util;

import java.util.*;
import java.beans.*;

/**
 * Represents a source of <tt>PropertyChangeEvent</tt>s which notifies
 * <tt>PropertyChangeListener</tt>s about changes in the values of properties.
 *
 * @author Lubomir Marinov
 */
public class PropertyChangeNotifier
{

    /**
     * The list of <tt>PropertyChangeListener</tt>s interested in and notified
     * about changes in the values of the properties of this
     * <tt>PropertyChangeNotifier</tt>.
     */
    private final List<PropertyChangeListener> listeners
        = new Vector<PropertyChangeListener>();

    /**
     * Adds a specific <tt>PropertyChangeListener</tt> to the list of listeners
     * interested in and notified about changes in the values of the properties
     * of this <tt>PropertyChangeNotifier</tt>.
     * 
     * @param listener a <tt>PropertyChangeListener</tt> to be notified about
     * changes in the values of the properties of this
     * <tt>PropertyChangeNotifier</tt>. If the specified listener is already in
     * the list of interested listeners (i.e. it has been previously added), it
     * is not added again.
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
     * Removes a specific <tt>PropertyChangeListener</tt> from the list of
     * listeners interested in and notified about changes in the values of the
     * properties of this <tt>PropertyChangeNotifer</tt>.
     * 
     * @param listener a <tt>PropertyChangeListener</tt> to no longer be
     * notified about changes in the values of the properties of this
     * <tt>PropertyChangeNotifier</tt>
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
     * Fires a new <tt>PropertyChangeEvent</tt> to the
     * <tt>PropertyChangeListener</tt>s registered with this
     * <tt>PropertyChangeNotifier</tt> in order to notify about a change in the
     * value of a specific property which had its old value modified to a
     * specific new value.
     * 
     * @param property the name of the property of this
     * <tt>PropertyChangeNotifier</tt> which had its value changed
     * @param oldValue the value of the property with the specified name before
     * the change
     * @param newValue the value of the property with the specified name after
     * the change
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
     * Gets the <tt>Object</tt> to be reported as the source of a new
     * <tt>PropertyChangeEvent</tt> which is to notify the
     * <tt>PropertyChangeListener</tt>s registered with this
     * <tt>PropertyChangeNotifier</tt> about the change in the value of a
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
     * <tt>PropertyChangeNotifier</tt> about the change in the value of the
     * property with the specified name from the specified old value to the
     * specified new value
     */
    protected Object getPropertyChangeSource(
        String property,
        Object oldValue,
        Object newValue)
    {
        return this;
    }
}
