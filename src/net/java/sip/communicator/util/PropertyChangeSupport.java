/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.util;

/**
 * @author Lubomir Marinov
 */
public class PropertyChangeSupport
    extends PropertyChangeNotifier
{
    private final Object source;

    public PropertyChangeSupport(Object source)
    {
        this.source = source;
    }

    public void firePropertyChange(String property, Object oldValue,
            Object newValue)
    {
        super.firePropertyChange(property, oldValue, newValue);
    }

    protected Object getPropertyChangeSource(String property, Object oldValue,
            Object newValue)
    {
        return source;
    }
}
