/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.configuration;

import java.beans.*;

/**
 * A PropertyVetoException is thrown when a proposed change to a
 * property represents an unacceptable value.
 *
 * @author Emil Ivov
 */
public class ConfigPropertyVetoExceoption
    extends RuntimeException
{
    /**
     * A PropertyChangeEvent describing the vetoed change.
     * @serial
     */
    private final PropertyChangeEvent evt;

    /**
     * Constructs a <tt>PropertyVetoException</tt> with a
     * detailed message.
     *
     * @param message Descriptive message
     * @param evt A PropertyChangeEvent describing the vetoed change.
     */
    public ConfigPropertyVetoExceoption(String message, PropertyChangeEvent evt)
    {
        super(message);

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
