/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol;

import net.java.sip.communicator.service.protocol.event.*;
import java.util.*;


/**
 * The ProtocolProvider interface should be implemented by bundles that wrap telephony
 * protocol stacks. It gives the user interface a way to plug into those stacks
 * and receive notifications on status change and incoming calls, as well as
 * deliver user requests for establishing or ending calls, putting participants
 * on hold and etc.
 *
 * @author Emil Ivov
 */
public interface ProtocolProviderService
{


    /**
     * Returns a String containing a human readable string representation of the
     * provider. Such names would be shown by a telephony user interface so that
     * users may chose the protocol they'd like to use to make a specific call.
     * Most often this would be the name of the protocol the provider
     * implements, and/or some combination of the server it is attached to and
     * the user name used. It is up to providers to make sure that th.
     * @return a String representation of this provider.
     */
    public String getProviderName();

    /**
     * Returns the short name of the protocol that the implementation of this
     * provider is based upon (like SIP, Jabber, ICQ/AIM,  or others for
     * example).
     * @return a String containing the short name of the protocol this service
     * is taking care of.
     */
    public String getProtocolName();

    /**
     * Many communications protocols have well known logos that users are
     * familiar with. The image returned by this method should have a 32x32 size
     * and if this is not the case the user interface will try to resize it
     * (results are not guaranteed). In case the Provider does not wish to use
     * this feature, this method should return null. In that case the user
     * interface may try to show an image representation that it finds suitable
     * (or just a common protocol logo).
     * @return byte[] a 32x32 protocol logo or representative image.
     */
    public byte[] getProviderImage();

    /**
     * Registers the specified listener with this provider so that it would
     * receive notifications on changes of its state or other properties such
     * as its local address and display name.
     * @param listener the listener to register.
     */
    public void addProviderChangeListener(ProviderChangeListener listener);

    /**
     * Removes the specified listener.
     * @param listener the listener to remove.
     */
    public void removeProviderChangeListener(ProviderChangeListener listener);

    /**
     * Returns the protocol specific contact instance representing the local
     * user. In the case of SIP this would be your local sip address or in the
     * case of an IM protocol such as ICQ - your own uin. No set method should
     * be provided in implementations of this class. The getLocalContact()
     * method is only used for giving information to the user on their currently
     * used addressed a different service (ConfigurationService) should be used
     * for changing that kind of settings.
     * @return the Contact (address, phone number, or uin) that the Provider
     * implementation is communicating on behalf of.
     */
    public Contact getLocalContact();

    /**
     * Returns a PresenceStatus instance representing the state this provider is
     * currently in. Note that PresenceStatus instances returned by this method
     * MUST adequately represent all possible states that a provider might
     * enter duruing its lifecycle, includindg those that would not be visible
     * to others (e.g. Initializing, Connecting, etc ..) and those that will be
     * sent to contacts/buddies (On-Line, Eager to chat, etc.).
     * @return PresenceStatus
     */
    public PresenceStatus getStatus();

    /**
     * Requests the provider to enter into a status corresponding to the
     * specified paramters. Note that calling this method does not necessarily
     * imply that the requested status would be entered. This method would
     * return right after being called and the caller should add itself as
     * a listener to this class in order to get notified when the state has
     * actually changed.
     *
     * @param status the PresenceStatus as returned by getRequestableStatusSet
     * @param statusMessage the message that should be set as the reason to
     * enter that status
     * @throws IllegalArgumentException if the status requested is not a valid
     * PresenceStatus supported by this provider.
     */
    public void enterStatus(PresenceStatus status, String statusMessage)
        throws IllegalArgumentException;

    /**
     * Returns the set of PresenceStatus objects that a user of this service
     * may request the provider to enter. Note that the provider would most
     * probaby enter more states than those returned by this method as they
     * only depict instances that users may request to enter. (e.g. a user
     * may not request a "Connecting..." state - it is a temporary state
     * that the provider enters while trying to enter the "Connected" state).
     *
     * @return Iterator a PresenceStatus array containing "enterable"
     * status instances.
     */
    public Iterator getRequestableStatusSet();

    /**
     * Returns a string representation of the registration service that is
     * used by this provider or null if none is used. The string returned by
     * this method is used by the user interface so that it could give (if
     * necessary) information to the user on its point of registration. It is
     * therefore not necessary to return a valid URL but rather a human readable
     * descriptive string.
     * @return a string representing (the address of) the service being used.
     */
    public String getRegistrationServer();

    /**
     * Allows the user interface to plugin an object that would handle incoming
     * authentication challenges.
     * @param authority SecurityAuthority
     */
    public void setSecurityAuthority(SecurityAuthority authority);

    /**
     * Returns an array containing all operation sets supported by the current
     * implementation. When querying this method users must be prepared to
     * receive any sybset of the OperationSet-s defined by this service. They
     * MUST ignore any OperationSet-s that they are not aware of and that may be
     * defined by future version of this service. Such "unknown" OperationSet-s
     * though not encouraged, may also be defined by service implementors.
     *
     * @return an array of OperationSet-s supported by this protocol provider
     * implementation.
     */
    public OperationSet[] getSupportedOperationSets();

    /**
     * Initialized the service implementation, and puts it in a sate where it
     * could interoperate with other services.
     */
    public void initialize();

    /**
     * Returns true if the provider service implementation is initialized and ready
     * for use by other services, and false otherwise. Note that this should
     * remain as a separate method and not be one of the presence statuses
     * since, in theorry, an implementation might very well be unaware of
     * its presence status while uninitialized.
     */
    public boolean isInitialized();

    /**
     * Makes the service implementation close all open sockets and release
     * any resources that it might have taken and prepare for shutdown/garbage
     * collection.
     */
    public void shutdown();
}
