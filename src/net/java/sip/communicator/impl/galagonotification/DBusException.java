/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.galagonotification;

/**
 * Implements <tt>Exception</tt> for D-Bus errors reported through the native
 * <tt>DBusError</tt> structure.
 *
 * @author Lubomir Marinov
 */
public class DBusException
    extends Exception
{

    /**
     * Initializes a new <tt>DBusException</tt> instance with the specified
     * detail message.
     *
     * @param message the detail message to later be reported by the new
     * instance through its {@link #getMessage()}
     */
    public DBusException(String message)
    {
        super(message);
    }
}
