/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.configuration;

import java.util.*;
import java.beans.*;

import net.java.sip.communicator.service.configuration.*;

/**
 * This is a utility class that can be used by objects that support constrained
 * properties.  You can use an instance of this class as a member field and
 * delegate various work to it.
 *
 * @author Emil Ivov
 */
public class ChangeEventDispatcher
{

    /**
     * All property change listeners registered so far.
     */
    private List<PropertyChangeListener> propertyChangeListeners;

    /**
     * All listeners registered for vetoable change events.
     */
    private List<ConfigVetoableChangeListener> vetoableChangeListeners;

    /**
     * Hashtable for managing property change listeners registered for specific
     * properties. Maps property names to PropertyChangeSupport objects.
     */
    private Map<String, ChangeEventDispatcher> propertyChangeChildren;

    /**
     * Hashtable for managing vetoable change listeners registered for specific
     * properties. Maps property names to PropertyChangeSupport objects.
     */
    private Map<String, ChangeEventDispatcher> vetoableChangeChildren;

    /**
     * The object to be provided as the "source" for any generated events.
     */
    private Object source;

    /**
     * Constructs a <tt>VetoableChangeSupport</tt> object.
     *
     * @param sourceObject  The object to be given as the source for any events.
     */
    public ChangeEventDispatcher(Object sourceObject)
    {
        if (sourceObject == null)
        {
            throw new NullPointerException();
        }
        source = sourceObject;
    }

    /**
     * Add a PropertyChangeListener to the listener list.
     * The listener is registered for all properties.
     *
     * @param listener  The PropertyChangeChangeListener to be added
     */
    public synchronized void addPropertyChangeListener(
        PropertyChangeListener listener)
    {
        if (propertyChangeListeners == null)
        {
            propertyChangeListeners = new Vector<PropertyChangeListener>();
        }

        propertyChangeListeners.add(listener);
    }

    /**
     * Add a PropertyChangeListener for a specific property.  The listener
     * will be invoked only when a call on firePropertyChange names that
     * specific property.
     *
     * @param propertyName  The name of the property to listen on.
     * @param listener  The ConfigurationChangeListener to be added
     */

    public synchronized void addPropertyChangeListener(
        String propertyName,
        PropertyChangeListener listener)
    {
        if (propertyChangeChildren == null)
        {
            propertyChangeChildren =
                new Hashtable<String, ChangeEventDispatcher>();
        }
        ChangeEventDispatcher child = propertyChangeChildren.get(
            propertyName);
        if (child == null)
        {
            child = new ChangeEventDispatcher(source);
            propertyChangeChildren.put(propertyName, child);
        }
        child.addPropertyChangeListener(listener);
    }

    /**
     * Remove a PropertyChangeListener from the listener list.
     * This removes a ConfigurationChangeListener that was registered
     * for all properties.
     *
     * @param listener The PropertyChangeListener to be removed
     */
    public synchronized void removePropertyChangeListener(
        PropertyChangeListener listener)
    {
        if (propertyChangeListeners != null)
        {
            propertyChangeListeners.remove(listener);
        }
    }

    /**
     * Remove a PropertyChangeListener for a specific property.
     *
     * @param propertyName  The name of the property that was listened on.
     * @param listener  The VetoableChangeListener to be removed
     */
    public synchronized void removePropertyChangeListener(
        String propertyName,
        PropertyChangeListener listener)
    {
        if (propertyChangeChildren == null)
        {
            return;
        }
        ChangeEventDispatcher child = propertyChangeChildren.get(propertyName);

        if (child == null)
        {
            return;
        }
        child.removePropertyChangeListener(listener);
    }

    /**
     * Add a VetoableChangeListener to the listener list.
     * The listener is registered for all properties.
     *
     * @param listener  The VetoableChangeListener to be added
     */
    public synchronized void addVetoableChangeListener(
            ConfigVetoableChangeListener listener)
    {
        if (vetoableChangeListeners == null)
        {
            vetoableChangeListeners = new Vector<ConfigVetoableChangeListener>();
        }

        vetoableChangeListeners.add(listener);
    }

    /**
     * Remove a VetoableChangeListener from the listener list.
     * This removes a VetoableChangeListener that was registered
     * for all properties.
     *
     * @param listener  The VetoableChangeListener to be removed
     */
    public synchronized void removeVetoableChangeListener(
        ConfigVetoableChangeListener listener)
    {
        if (vetoableChangeListeners != null)
        {
            vetoableChangeListeners.remove(listener);
        }
    }

    /**
     * Add a VetoableChangeListener for a specific property.  The listener
     * will be invoked only when a call on fireVetoableChange names that
     * specific property.
     *
     * @param propertyName  The name of the property to listen on.
     * @param listener  The ConfigurationChangeListener to be added
     */

    public synchronized void addVetoableChangeListener(
        String propertyName,
        ConfigVetoableChangeListener listener)
    {
        if (vetoableChangeChildren == null)
        {
            vetoableChangeChildren = new Hashtable<String, ChangeEventDispatcher>();
        }
        ChangeEventDispatcher child = vetoableChangeChildren.get(propertyName);
        if (child == null)
        {
            child = new ChangeEventDispatcher(source);
            vetoableChangeChildren.put(propertyName, child);
        }
        child.addVetoableChangeListener(listener);
    }

    /**
     * Remove a VetoableChangeListener for a specific property.
     *
     * @param propertyName  The name of the property that was listened on.
     * @param listener  The VetoableChangeListener to be removed
     */
    public synchronized void removeVetoableChangeListener(
        String propertyName,
        ConfigVetoableChangeListener listener)
    {
        if (vetoableChangeChildren == null)
        {
            return;
        }
        ChangeEventDispatcher child = vetoableChangeChildren.get( propertyName );

        if (child == null)
        {
            return;
        }
        child.removeVetoableChangeListener(listener);
    }

    /**
     * Report a vetoable property update to any registered listeners.  If
     * no one vetos the change, then fire a new ConfigurationChangeEvent
     * indicating that the change has been accepted. In the case of a
     * PropertyVetoException, end eventdispatch and rethrow the eception
     * <p>
     * No event is fired if old and new are equal and non-null.
     *
     * @param propertyName  The programmatic name of the property
     *                      that is about to change..
     * @param oldValue  The old value of the property.
     * @param newValue  The new value of the property.
     * @exception PropertyVetoException if the recipient wishes the property
     *              change to be rolled back.
     */
    public void fireVetoableChange(String propertyName,
                                   Object oldValue, Object newValue) 
        //throws PropertyVetoException
    {
        if (vetoableChangeListeners == null && vetoableChangeChildren == null)
        {
            return;
        }

        PropertyChangeEvent evt = new PropertyChangeEvent(source,
            propertyName, oldValue, newValue);
            fireVetoableChange(evt);
    }

    /**
     * Fire a vetoable property update to any registered listeners.  If
     * anyone vetos the change, then the excption will be rethrown by this
     * method.
     * <p>
     * No event is fired if old and new are equal and non-null.
     *
     * @param evt  The PropertyChangeEvent to be fired.
     * @exception PropertyVetoException if at least one of the recipients has
     * vetoed the change.
     */
    public void fireVetoableChange(PropertyChangeEvent evt) 
        // throws PropertyVetoException
    {

        Object oldValue = evt.getOldValue();
        Object newValue = evt.getNewValue();
        String propertyName = evt.getPropertyName();
        if (oldValue != null && newValue != null && oldValue.equals(newValue))
        {
            return;
        }

        ConfigVetoableChangeListener[] targets = null;
        ChangeEventDispatcher child = null;
        synchronized (this)
        {
            if (vetoableChangeListeners != null)
            {
                targets =
                    vetoableChangeListeners
                        .toArray(new ConfigVetoableChangeListener[vetoableChangeListeners
                            .size()]);
            }
            if (vetoableChangeChildren != null && propertyName != null)
            {
                child = vetoableChangeChildren.get(propertyName);
            }
        }

        if (vetoableChangeListeners != null && targets != null)
        {
            for (int i = 0; i < targets.length; i++)
            {
                ConfigVetoableChangeListener target = targets[i];
                target.vetoableChange(evt);
            }
        }

        if (child != null)
        {
            child.fireVetoableChange(evt);
        }
    }


    /**
     * Report a bound property update to any registered listeners.
     * No event is fired if old and new are equal and non-null.
     *
     * @param propertyName  The programmatic name of the property
     *                      that was changed.
     * @param oldValue  The old value of the property.
     * @param newValue  The new value of the property.
     */
    public void firePropertyChange(String propertyName,
                                   Object oldValue, Object newValue)
    {
        if (oldValue != null && newValue != null && oldValue.equals(newValue))
        {
            return;
        }
        firePropertyChange(new PropertyChangeEvent(source, propertyName,
            oldValue, newValue));
    }

    /**
     * Fire an existing PropertyChangeEvent to any registered listeners.
     * No event is fired if the given event's old and new values are
     * equal and non-null.
     * @param evt  The PropertyChangeEvent object.
     */
    public void firePropertyChange(PropertyChangeEvent evt)
    {
        Object oldValue = evt.getOldValue();
        Object newValue = evt.getNewValue();
        String propertyName = evt.getPropertyName();
        if (oldValue != null && newValue != null && oldValue.equals(newValue))
        {
            return;
        }

        if (propertyChangeListeners != null)
        {
            for (PropertyChangeListener target : propertyChangeListeners)
            {
                target.propertyChange(evt);
            }
        }

        if (propertyChangeChildren != null && propertyName != null)
        {
            ChangeEventDispatcher child =
                propertyChangeChildren.get(propertyName);
            if (child != null)
            {
                child.firePropertyChange(evt);
            }
        }
    }

    /**
     * Check if there are any listeners for a specific property. (Generic
     * listeners count as well)
     *
     * @param propertyName  the property name.
     * @return true if there are one or more listeners for the given property
     */
    public synchronized boolean hasPropertyChangeListeners(String propertyName)
    {
        if(propertyChangeListeners != null && !propertyChangeListeners.isEmpty())
        {
            // there is a generic listener
            return true;
        }
        if (propertyChangeChildren != null)
        {
            ChangeEventDispatcher child =
                propertyChangeChildren.get(propertyName);
            if (child != null && child.propertyChangeListeners != null)
            {
                return!child.propertyChangeListeners.isEmpty();
            }
        }
        return false;
    }

    /**
     * Check if there are any vetoable change listeners for a specific property.
     * (Generic vetoable change listeners count as well)
     *
     * @param propertyName  the property name.
     * @return true if there are one or more listeners for the given property
     */
    public synchronized boolean hasVetoableChangeListeners(String propertyName)
    {
        if(vetoableChangeListeners != null && !vetoableChangeListeners.isEmpty())
        {
            // there is a generic listener
            return true;
        }
        if (vetoableChangeChildren != null)
        {
            ChangeEventDispatcher child =
                vetoableChangeChildren.get(propertyName);

            if (child != null && child.vetoableChangeListeners != null)
            {
                return!child.vetoableChangeListeners.isEmpty();
            }
        }
        return false;
    }

}
