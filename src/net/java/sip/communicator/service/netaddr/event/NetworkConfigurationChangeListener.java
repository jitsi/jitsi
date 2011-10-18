/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.netaddr.event;

/**
 * Listens for network changes in the computer configuration.
 *
 * @author Damian Minkov
 */
public interface NetworkConfigurationChangeListener
{
    /**
     * Fired when a change has occurred in the computer network configuration.
     *
     * @param event the change event.
     */
    public void configurationChanged(ChangeEvent event);
}
