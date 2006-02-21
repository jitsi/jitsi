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
     * Mock implementation of the corresponding ProtocolProviderService method.
     *
     * @param listener a param.
     */
    public void addRegistrationStateChangeListener(
        RegistrationStateChangeListener listener)
    {
    }

    /**
     * Mock implementation of the corresponding ProtocolProviderService method.
     *
     * @return an empty.
     */
    public String getProtocolName()
    {
        return "";
    }

    /**
     * Mock implementation of the corresponding ProtocolProviderService method.
     *
     * @return a null ProviderRegistrationState
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
     */
    public Map getSupportedOperationSets()
    {
        return null;
    }

    /**
     * Mock implementation of the corresponding ProtocolProviderService method.
     *
     * @return always true.
     */
    public boolean isRegistered()
    {
        return true;
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
