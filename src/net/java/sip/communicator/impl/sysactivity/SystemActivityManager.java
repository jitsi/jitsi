/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.sysactivity;

/**
 * Common interface shared between different OS implementations.
 *
 * @author Damian Minkov
 */
public interface SystemActivityManager
{
    /**
     * Starts the manager.
     */
    public void start();

    /**
     * Stops the manager.
     */
    public void stop();

    /**
     * Whether the underlying implementation is currently connected and
     * working.
     * @return whether we are connected and working.
     */
    public boolean isConnected();
}
