/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.neomedia;

import net.java.sip.communicator.util.*;

/**
 * Abstract base implementation of <tt>MediaStream</tt> to ease the
 * implementation of the interface.
 *
 * @author Lubomir Marinov
 */
public abstract class AbstractMediaStream
    implements MediaStream
{

    /**
     * The delegate of this instance which implements support for property
     * change notifications for its
     * {@link #addPropertyChangeListener(PropertyChangeListener)} and
     * {@link #removePropertyChangeListener(PropertyChangeListener)}.
     */
    private final PropertyChangeSupport propertyChangeSupport
        = new PropertyChangeSupport(this);

    /**
     * Adds a <tt>PropertyChangelistener</tt> to this stream which is to be
     * notified upon property changes such as a SSRC ID which becomes known.
     *
     * @param listener the <tt>PropertyChangeListener</tt> to register for
     * <tt>PropertyChangeEvent</tt>s
     * @see MediaStream#addPropertyChangeListener(PropertyChangeListener)
     */
    public void addPropertyChangeListener(PropertyChangeListener listener)
    {
        propertyChangeSupport.addPropertyChangeListener(listener);
    }

    /**
     * Fires a new <tt>PropertyChangeEvent</tt> to the
     * <tt>PropertyChangeListener</tt>s registered with this instance in order
     * to notify about a change in the value of a specific property which had
     * its old value modified to a specific new value.
     * 
     * @param property the name of the property of this instance which had its
     * value changed
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
        propertyChangeSupport.firePropertyChange(property, oldValue, newValue);
    }

    /**
     * Removes the specified <tt>PropertyChangeListener</tt> from this stream so
     * that it won't receive further property change events.
     *
     * @param listener the <tt>PropertyChangeListener</tt> to remove
     * @see MediaStream#removePropertyChangeListener(PropertyChangeListener)
     */
    public void removePropertyChangeListener(PropertyChangeListener listener)
    {
        propertyChangeSupport.removePropertyChangeListener(listener);
    }
}
