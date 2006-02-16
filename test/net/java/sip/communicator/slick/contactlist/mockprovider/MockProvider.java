/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.slick.contactlist.mockprovider;

import java.util.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;

/**
 * A mock protocol provider implementation that comes with a single operation
 * set (OperationSetPersistentPresence) for use by the MetaContactListSlcik.
 *
 * @author Emil Ivov
 */
public class MockProvider
    implements ProtocolProviderService
{
    public MockProvider()
    {
    }

    /**
     * Registers the specified listener with this provider so that it would
     * receive notifications on changes of its state or other properties such
     * as its local address and display name.
     *
     * @param listener the listener to register.
     * @todo Implement this
     *   net.java.sip.communicator.service.protocol.ProtocolProviderService
     *   method
     */
    public void addRegistrationStateChangeListener(
        RegistrationStateChangeListener listener)
    {
    }

    /**
     * Returns the short name of the protocol that the implementation of this
     * provider is based upon (like SIP, Jabber, ICQ/AIM, or others for
     * example).
     *
     * @return a String containing the short name of the protocol this
     *   service is implementing (most often that would be a name in
     *   ProtocolNames).
     * @todo Implement this
     *   net.java.sip.communicator.service.protocol.ProtocolProviderService
     *   method
     */
    public String getProtocolName()
    {
        return "";
    }

    /**
     * Returns the state of the registration of this protocol provider with
     * the corresponding registration service.
     *
     * @return ProviderRegistrationState
     * @todo Implement this
     *   net.java.sip.communicator.service.protocol.ProtocolProviderService
     *   method
     */
    public RegistrationState getRegistrationState()
    {
        return null;
    }

    /**
     * Returns an array containing all operation sets supported by the
     * current implementation.
     *
     * @return a java.util.Map containing instance of all supported
     *   operation sets mapped against their class names (e.g.
     *   OperationSetPresence.class.getName()) .
     * @todo Implement this
     *   net.java.sip.communicator.service.protocol.ProtocolProviderService
     *   method
     */
    public Map getSupportedOperationSets()
    {
        return null;
    }

    /**
     * Indicates whether or not this provider is registered
     *
     * @return true if the provider is currently registered and false
     *   otherwise.
     * @todo Implement this
     *   net.java.sip.communicator.service.protocol.ProtocolProviderService
     *   method
     */
    public boolean isRegistered()
    {
        return false;
    }

    /**
     * Starts the registration process.
     *
     * @param authority the security authority that will be used for
     *   resolving any security challenges that may be returned during the
     *   registration or at any moment while wer're registered.
     * @todo Implement this
     *   net.java.sip.communicator.service.protocol.ProtocolProviderService
     *   method
     */
    public void register(SecurityAuthority authority)
    {
    }

    /**
     * Removes the specified listener.
     *
     * @param listener the listener to remove.
     * @todo Implement this
     *   net.java.sip.communicator.service.protocol.ProtocolProviderService
     *   method
     */
    public void removeRegistrationStateChangeListener(
        RegistrationStateChangeListener listener)
    {
    }

    /**
     * Makes the service implementation close all open sockets and release
     * any resources that it might have taken and prepare for
     * shutdown/garbage collection.
     *
     * @todo Implement this
     *   net.java.sip.communicator.service.protocol.ProtocolProviderService
     *   method
     */
    public void shutdown()
    {
    }

    /**
     * Ends the registration of this protocol provider with the current
     * registration service.
     *
     * @todo Implement this
     *   net.java.sip.communicator.service.protocol.ProtocolProviderService
     *   method
     */
    public void unregister()
    {
    }
}
