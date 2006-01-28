/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.sip;

import java.util.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;

/**
 *
 * @author Emil Ivov
 */
public class SipProtocolProviderServiceImpl
//emil: we're changing pp a lot. so this is a way to silence javac errors from
//here until we're done
//  implements ProtocolProviderService
{
    public static final String SIP_PROTOCOL_NAME = "SIP";
    /**
     * Returns the short name of the protocol that the implementation of this
     * provider is based upon, or in other words - SIP.
     *
     * @return a String containing the name of our protocol - SIP.
     */
    public String getProtocolName()
    {
        return SIP_PROTOCOL_NAME;
    }

    /**
     * Ends the registration of this protocol provider with the current
     * registration service.
     */
    public void unregister()
    {

    }

    /**
     * Requests the provider to enter into a status corresponding to the
     * specified paramters.
     *
     * @param status the PresenceStatus as returned by
     *   getRequestableStatusSet
     * @param statusMessage the message that should be set as the reason to
     *   enter that status
     * @throws IllegalArgumentException if the status requested is not a
     *   valid PresenceStatus supported by this provider.
     * @todo Implement this
     *   net.java.sip.communicator.service.protocol.ProtocolProviderService
     *   method
     */
    public void enterStatus(PresenceStatus status, String statusMessage) throws
        IllegalArgumentException
    {
    }

    /**
     * Returns the protocol specific contact instance representing the local
     * user.
     *
     * @return the Contact (address, phone number, or uin) that the Provider
     *   implementation is communicating on behalf of.
     * @todo Implement this
     *   net.java.sip.communicator.service.protocol.ProtocolProviderService
     *   method
     */
    public Contact getLocalContact()
    {
        return null;
    }

    /**
     * Returns the state of the registration of this protocol provider with the
     * corresponding registration service.
     * @return ProviderRegistrationState
     */
    public RegistrationState getRegistrationState()
    {

        return null;
    }

    /**
     * Starts the registration process. Connection details such as
     * registration server, user name/number are provided through the
     * configuration service through implementation specific properties.
     *
     * @param authority the security authority that will be used for resolving
     *        any security challenges that may be returned during the
     *        registration or at any moment while wer're registered.
     *
     */
    public void register(SecurityAuthority authority)
    {

    }

    /**
     * Many communications protocols have well known logos that users are
     * familiar with.
     *
     * @return byte[] a 32x32 protocol logo or representative image.
     * @todo Implement this
     *   net.java.sip.communicator.service.protocol.ProtocolProviderService
     *   method
     */
    public byte[] getProviderImage()
    {
        return null;
    }

    /**
     * Returns a String containing a human readable string representation of
     * the provider.
     *
     * @return a String representation of this provider.
     * @todo Implement this
     *   net.java.sip.communicator.service.protocol.ProtocolProviderService
     *   method
     */
    public String getProviderName()
    {
        return "";
    }

    /**
     * Returns a string representation of the registration service that is
     * used by this provider or null if none is used.
     *
     * @return a string representing (the address of) the service being used.
     * @todo Implement this
     *   net.java.sip.communicator.service.protocol.ProtocolProviderService
     *   method
     */
    public String getRegistrationServer()
    {
        return "";
    }

    /**
     * Returns the set of PresenceStatus objects that a user of this service
     * may request the provider to enter.
     *
     * @return Iterator a PresenceStatus array containing "enterable" status
     *   instances.
     * @todo Implement this
     *   net.java.sip.communicator.service.protocol.ProtocolProviderService
     *   method
     */
    public Iterator getRequestableStatusSet()
    {
        return null;
    }

    /**
     * Returns a PresenceStatus instance representing the state this provider
     * is currently in.
     *
     * @return PresenceStatus
     * @todo Implement this
     *   net.java.sip.communicator.service.protocol.ProtocolProviderService
     *   method
     */
    public PresenceStatus getPresenceStatus()
    {
        return null;
    }

    /**
     * Returns an array containing all operation sets supported by the
     * current implementation.
     *
     * @return an array of OperationSet-s supported by this protocol
     *   provider implementation.
     * @todo Implement this
     *   net.java.sip.communicator.service.protocol.ProtocolProviderService
     *   method
     */
    public Map getSupportedOperationSets()
    {
        return null;
    }

    /**
     * Initialized the service implementation, and puts it in a sate where it
     * could interoperate with other services.
     * @param accountID the account id string identifying the account that this
     * ProtocolProvider is serving.
     * @param initializationProperties any properties that a custom protocol
     * provider implementation might be needing for initizalization.
     */
//we don't need it not that we have install account
//    public void initialize(String accountID,
//                           Map initializationProperties)
//    {
//
//    }

    /**
     * Returns true if the provider service implementation is initialized and
     * ready for use by other services, and false otherwise.
     *
     * @return boolean
     * @todo Implement this
     *   net.java.sip.communicator.service.protocol.ProtocolProviderService
     *   method
     */
    public boolean isInitialized()
    {
        return false;
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
    public void addRegistrationStateChangeListener(RegistrationStateChangeListener listener)
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
    public void removeRegistrationStateChangeListener(RegistrationStateChangeListener listener)
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
}
