/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.util;

import java.util.*;

/**
 * @author Lubomir Marinov
 */
public class PropertyChangeNotifier
{
    private final List<PropertyChangeListener> listeners
            = new Vector<PropertyChangeListener>();

    public void addPropertyChangeListener(PropertyChangeListener listener)
    {
        synchronized(listeners)
        {
            if (!listeners.contains(listener))
                listeners.add(listener);
        }
    }

    public void removePropertyChangeListener(PropertyChangeListener listener)
    {
        synchronized(listeners)
        {
            listeners.remove(listener);
        }
    }

    protected void firePropertyChange(String property, Object oldValue,
            Object newValue)
    {
        PropertyChangeListener[] listeners;
        synchronized (this.listeners)
        {
            listeners
                = this.listeners.toArray(
                        new PropertyChangeListener[this.listeners.size()]);
        }

        PropertyChangeEvent event = new PropertyChangeEvent(
            getPropertyChangeSource(property, oldValue, newValue),
            property,
            oldValue,
            newValue);

        for (PropertyChangeListener listener : listeners)
            listener.propertyChange(event);
    }

    protected Object getPropertyChangeSource(String property, Object oldValue,
            Object newValue)
    {
        return this;
    }
}
