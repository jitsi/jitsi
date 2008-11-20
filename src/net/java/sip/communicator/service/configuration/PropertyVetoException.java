/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.configuration;

import net.java.sip.communicator.util.*;

/**
 * A PropertyVetoException is thrown when a proposed change to a
 * property represents an unacceptable value.
 *
 * @author Emil Ivov
 */
public class PropertyVetoException
    extends RuntimeException
{
    /**
     * A PropertyChangeEvent describing the vetoed change.
     * @serial
     */
    private PropertyChangeEvent evt;

    /**
     * Constructs a <tt>PropertyVetoException</tt> with a
     * detailed message.
     *
     * @param mess Descriptive message
     * @param evt A PropertyChangeEvent describing the vetoed change.
     */
    public PropertyVetoException(String mess, PropertyChangeEvent evt)
    {
        super(mess);
        this.evt = evt;
    }

    /**
     * Gets the vetoed <tt>PropertyChangeEvent</tt>.
     *
     * @return A PropertyChangeEvent describing the vetoed change.
     */
    public PropertyChangeEvent getPropertyChangeEvent()
    {
        return evt;
    }
}
