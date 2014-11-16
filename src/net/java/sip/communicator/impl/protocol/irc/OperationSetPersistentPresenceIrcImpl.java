/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.irc;

import java.util.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;

/**
 * Implementation of support for Persistent Presence for IRC.
 *
 * @author Danny van Heumen
 */
public class OperationSetPersistentPresenceIrcImpl
    extends
    AbstractOperationSetPersistentPresence<ProtocolProviderServiceIrcImpl>
{
    /**
     * Logger.
     */
    private static final Logger LOGGER = Logger
        .getLogger(OperationSetPersistentPresenceIrcImpl.class);

    /**
     * Root contact group for IRC contacts.
     */
    private final ContactGroupIrcImpl rootGroup = new ContactGroupIrcImpl(
        this.parentProvider);

    /**
     * IRC implementation for OperationSetPersistentPresence.
     *
     * @param parentProvider IRC instance of protocol provider service.
     */
    protected OperationSetPersistentPresenceIrcImpl(
        final ProtocolProviderServiceIrcImpl parentProvider)
    {
        super(parentProvider);
    }

    /**
     * Create a volatile contact.
     *
     * @param id contact id
     * @return returns instance of volatile contact
     */
    private ContactIrcImpl createVolatileContact(final String id)
    {
        // Get non-persistent group for volatile contacts.
        ContactGroupIrcImpl volatileGroup = getNonPersistentGroup();

        // Create volatile contact
        ContactIrcImpl newVolatileContact =
            new ContactIrcImpl(this.parentProvider, id, volatileGroup);
        volatileGroup.addContact(newVolatileContact);

        // Add nick to watch list of presence manager.
        final IrcConnection connection =
            this.parentProvider.getIrcStack().getConnection();
        if (connection != null)
        {
            connection.getPresenceManager().addNickWatch(id);
        }

        this.fireSubscriptionEvent(newVolatileContact, volatileGroup,
            SubscriptionEvent.SUBSCRIPTION_CREATED);

        return newVolatileContact;
    }

    /**
     * Get group for non-persistent contacts.
     *
     * @return returns group instance
     */
    private ContactGroupIrcImpl getNonPersistentGroup()
    {
        String groupName
            = IrcActivator.getResources().getI18NString(
                "service.gui.NOT_IN_CONTACT_LIST_GROUP_NAME");

        for (int i = 0; i < getRootGroup().countSubgroups(); i++)
        {
            ContactGroupIrcImpl gr =
                (ContactGroupIrcImpl) getRootGroup().getGroup(i);

            if (!gr.isPersistent() && gr.getGroupName().equals(groupName))
            {
                return gr;
            }
        }

        ContactGroupIrcImpl volatileGroup =
            new ContactGroupIrcImpl(this.parentProvider, this.rootGroup,
                groupName);

        this.rootGroup.addSubGroup(volatileGroup);

        this.fireServerStoredGroupEvent(volatileGroup,
            ServerStoredGroupEvent.GROUP_CREATED_EVENT);

        return volatileGroup;
    }

    /**
     * Get root contact group.
     *
     * @return returns root contact group
     */
    public ContactGroupIrcImpl getRootGroup()
    {
        return rootGroup;
    }

    /**
     * Subscribes to presence updates for specified contact identifier.
     *
     * @param contactIdentifier the contact's identifier
     * @throws IllegalArgumentException on bad input
     * @throws IllegalStateException if disconnected
     * @throws OperationFailedException on failed operation
     */
    @Override
    public void subscribe(final String contactIdentifier)
        throws IllegalArgumentException,
        IllegalStateException,
        OperationFailedException
    {
        if (contactIdentifier == null)
        {
            throw new IllegalArgumentException(
                "contactIdentifier cannot be null");
        }
        final IrcConnection connection =
            this.parentProvider.getIrcStack().getConnection();
        if (connection == null)
        {
            return;
        }
        connection.getPresenceManager().addNickWatch(contactIdentifier);
    }

    /**
     * Subscribes to presence updates for specified contact identifier.
     *
     * @param parent contact group
     * @param contactIdentifier contact
     * @throws OperationFailedException if not implemented
     * @throws IllegalArgumentException on bad input
     * @throws IllegalStateException if disconnected
     */
    @Override
    public void subscribe(final ContactGroup parent,
        final String contactIdentifier)
        throws IllegalArgumentException,
        IllegalStateException,
        OperationFailedException
    {
        if (contactIdentifier == null)
        {
            throw new IllegalArgumentException(
                "contactIdentifier cannot be null");
        }
        final IrcConnection connection =
            this.parentProvider.getIrcStack().getConnection();
        if (connection == null)
        {
            return;
        }
        connection.getPresenceManager().addNickWatch(contactIdentifier);
    }

    /**
     * Unsubscribe for presence change events for specified contact.
     *
     * @param contact contact instance
     * @throws IllegalArgumentException on bad input
     * @throws IllegalStateException if disconnected
     * @throws OperationFailedException if something went wrong
     */
    @Override
    public void unsubscribe(final Contact contact)
        throws IllegalArgumentException,
        IllegalStateException,
        OperationFailedException
    {
        if (contact == null)
        {
            throw new IllegalArgumentException("contact cannot be null");
        }
        final IrcConnection connection =
            this.parentProvider.getIrcStack().getConnection();
        if (connection == null)
        {
            return;
        }
        connection.getPresenceManager().removeNickWatch(contact.getAddress());
    }

    /**
     * Creating a contact group is currently not implemented.
     *
     * @param parent parent contact group
     * @param groupName new group's name
     * @throws OperationFailedException if not implemented
     */
    @Override
    public void createServerStoredContactGroup(final ContactGroup parent,
        final String groupName) throws OperationFailedException
    {
        LOGGER.trace("createServerStoredContactGroup(...) called");
        throw new OperationFailedException("Not implemented.",
            OperationFailedException.NOT_SUPPORTED_OPERATION);
    }

    /**
     * Removing a contact group is currently not implemented.
     *
     * @param group contact group to remove
     * @throws OperationFailedException if not implemented
     */
    @Override
    public void removeServerStoredContactGroup(final ContactGroup group)
        throws OperationFailedException
    {
        LOGGER.trace("removeServerStoredContactGroup called");
        throw new OperationFailedException("Not implemented.",
            OperationFailedException.NOT_SUPPORTED_OPERATION);
    }

    /**
     * Rename contact group.
     *
     * @param group contact group to rename
     * @param newName new name
     */
    @Override
    public void renameServerStoredContactGroup(final ContactGroup group,
        final String newName)
    {
        LOGGER.trace("renameServerStoredContactGroup called");
        ((ContactGroupIrcImpl) group).setGroupName(newName);
    }

    /**
     * Moving contacts to a different group is currently not implemented.
     *
     * @param contactToMove contact to move
     * @param newParent new parent group
     * @throws OperationFailedException if not implemented
     */
    @Override
    public void moveContactToGroup(final Contact contactToMove,
        final ContactGroup newParent) throws OperationFailedException
    {
        LOGGER.trace("moveContactToGroup called");
        throw new OperationFailedException("Not implemented.",
            OperationFailedException.NOT_SUPPORTED_OPERATION);
    }

    /**
     * Get group of contacts that have been discovered while using IRC.
     *
     * @return returns root contact group
     */
    @Override
    public ContactGroup getServerStoredContactListRoot()
    {
        // TODO consider using this for contacts that are registered at NickServ
        // for the IRC network. Store contacts and possibly some whois info if
        // useful for these contacts as persistent data.
        return this.rootGroup;
    }

    /**
     * Creates an unresolved contact for IRC.
     *
     * @param address contact address
     * @param persistentData persistent data for contact
     * @param parentGroup parent group to contact
     * @return returns newly created unresolved contact instance
     */
    @Override
    public ContactIrcImpl createUnresolvedContact(final String address,
        final String persistentData, final ContactGroup parentGroup)
    {
        if (!(parentGroup instanceof ContactGroupIrcImpl))
        {
            throw new IllegalArgumentException(
                "Provided contact group is not an IRC contact group instance.");
        }
        return new ContactIrcImpl(this.parentProvider, address,
            (ContactGroupIrcImpl) parentGroup);
    }

    /**
     * Creates an unresolved contact for IRC.
     *
     * @param address contact address
     * @param persistentData persistent data for contact
     * @return returns newly created unresolved contact instance
     */
    @Override
    public ContactIrcImpl createUnresolvedContact(final String address,
        final String persistentData)
    {
        return new ContactIrcImpl(this.parentProvider, address,
            this.getRootGroup());
    }

    /**
     * Create a new unresolved contact group.
     *
     * @param groupUID unique group id
     * @param persistentData persistent data is currently not supported
     * @param parentGroup the parent group for the newly created contact group
     * @return returns new unresolved contact group
     */
    @Override
    public ContactGroupIrcImpl createUnresolvedContactGroup(
        final String groupUID, final String persistentData,
        final ContactGroup parentGroup)
    {
        if (!(parentGroup instanceof ContactGroupIrcImpl))
        {
            throw new IllegalArgumentException(
                "parentGroup is not a ContactGroupIrcImpl instance");
        }
        return new ContactGroupIrcImpl(this.parentProvider,
            (ContactGroupIrcImpl) parentGroup, groupUID);
    }

    /**
     * Get current IRC presence status.
     *
     * The presence status currently is ONLINE or AWAY if we are connected or
     * OFFLINE if we aren't connected. The status is set to AWAY if an away
     * message is set.
     *
     * @return returns status ONLINE if connected and not away, or AWAY if
     *         connected and an away message is set, or OFFLINE if not connected
     *         at all
     */
    @Override
    public PresenceStatus getPresenceStatus()
    {
        final IrcConnection connection =
            this.parentProvider.getIrcStack().getConnection();
        if (connection != null && connection.isConnected())
        {
            return connection.getPresenceManager().isAway() ? IrcStatusEnum.AWAY
                : IrcStatusEnum.ONLINE;
        }
        else
        {
            return IrcStatusEnum.OFFLINE;
        }
    }

    /**
     * Set a new presence status corresponding to the provided arguments.
     *
     * @param status presence status
     * @param statusMessage message for the specified status
     */
    @Override
    public void publishPresenceStatus(final PresenceStatus status,
        final String statusMessage)
        throws IllegalArgumentException,
        IllegalStateException,
        OperationFailedException
    {
        final IrcConnection connection =
            this.parentProvider.getIrcStack().getConnection();
        String message = statusMessage;
        if (connection == null)
        {
            throw new IllegalStateException("Connection is not available.");
        }
        if (message != null && message.isEmpty())
        {
            // if we provide a message, make sure it isn't empty
            message = null;
        }

        if (status.getStatus() >= IrcStatusEnum.AVAILABLE_THRESHOLD)
        {
            connection.getPresenceManager().away(false, message);
        }
        else if (status.getStatus() >= IrcStatusEnum.AWAY_THRESHOLD)
        {
            connection.getPresenceManager().away(true, message);
        }
    }

    /**
     * Update (from IRC) containing the current presence status and message.
     *
     * @param previousStatus the previous presence status
     * @param status the current presence status
     */
    void updatePresenceStatus(final PresenceStatus previousStatus,
        final PresenceStatus status)
    {
        // Note: Currently uses general PresenceStatus type parameters because
        // EasyMock throws a java.lang.NoClassDefFoundError: Could not
        // initialize class
        // net.java.sip.communicator.impl.protocol.irc.
        // OperationSetPersistentPresenceIrcImpl$$EnhancerByCGLIB$$403085ac
        // if IrcStatusEnum is used. I'm not sure why, though ...
        fireProviderStatusChangeEvent(previousStatus, status);
    }

    /**
     * Get set of statuses supported in IRC.
     *
     * @return returns iterator for supported statuses
     */
    @Override
    public Iterator<PresenceStatus> getSupportedStatusSet()
    {
        final HashSet<PresenceStatus> statuses = new HashSet<PresenceStatus>();
        final Iterator<IrcStatusEnum> supported =
            IrcStatusEnum.supportedStatusSet();
        while (supported.hasNext())
        {
            statuses.add(supported.next());
        }
        return statuses.iterator();
    }

    /**
     * IRC currently does not implement querying presence status.
     *
     * @param contactIdentifier contact id
     * @return returns current presence status
     * @throws OperationFailedException for not supporting this feature
     */
    @Override
    public PresenceStatus queryContactStatus(final String contactIdentifier)
        throws IllegalArgumentException,
        IllegalStateException,
        OperationFailedException
    {
        // TODO implement querying presence status of contact
        throw new OperationFailedException("Not supported.",
            OperationFailedException.NOT_SUPPORTED_OPERATION);
    }

    /**
     * Find a contact by its ID.
     *
     * @param contactID ID to look up
     * @return contact instance if found or null if nothing found
     */
    @Override
    public ContactIrcImpl findContactByID(final String contactID)
    {
        if (contactID == null)
        {
            return null;
        }
        ContactIrcImpl contact = this.rootGroup.getContact(contactID);
        if (contact != null)
        {
            return contact;
        }
        Iterator<ContactGroup> groups = this.rootGroup.subgroups();
        while (groups.hasNext())
        {
            ContactGroupIrcImpl group = (ContactGroupIrcImpl) groups.next();
            contact = group.getContact(contactID);
            if (contact != null)
            {
                return contact;
            }
        }
        return null;
    }

    /**
     * IRC does not support authorization handling, so this is not supported.
     *
     * @param handler authorization handler
     */
    @Override
    public void setAuthorizationHandler(final AuthorizationHandler handler)
    {
    }

    /**
     * IRC will return the away message if AWAY status is active, or an empty
     * string if user is not away.
     *
     * @return returns empty string
     */
    @Override
    public String getCurrentStatusMessage()
    {
        final IrcConnection connection =
            this.parentProvider.getIrcStack().getConnection();
        if (connection == null)
        {
            throw new IllegalStateException("Connection is not available.");
        }
        return connection.getPresenceManager().isAway() ? connection
            .getPresenceManager().getMessage() : "";
    }

    /**
     * Find or create contact by ID.
     *
     * In IRC every chat room member is also a contact. Try to find a contact by
     * its ID. If a contact cannot be found, then create one.
     *
     * @param id id of the contact
     * @return returns instance of contact
     */
    Contact findOrCreateContactByID(final String id)
    {
        Contact contact = findContactByID(id);
        if (contact == null)
        {
            contact = createVolatileContact(id);
            LOGGER.debug("No existing contact found. Created volatile contact"
                + " for nick name '" + id + "'.");
        }
        return contact;
    }

    /**
     * Update presence based for user's nick.
     *
     * @param nick the nick
     * @param newStatus the new status
     */
    void updateNickContactPresence(final String nick,
        final PresenceStatus newStatus)
    {
        LOGGER.trace("Received presence update for nick '" + nick
            + "', status: " + newStatus.getStatus());
        final Contact contact = findContactByID(nick);
        if (contact == null)
        {
            LOGGER.trace("null contact instance found: presence will not be "
                + "processed.");
            return;
        }
        if (!(contact instanceof ContactIrcImpl))
        {
            throw new IllegalArgumentException(
                "Expected contact to be an IRC contact instance.");
        }
        final ContactIrcImpl contactIrc = (ContactIrcImpl) contact;
        final ContactGroup group = contact.getParentContactGroup();
        final PresenceStatus previous = contactIrc.getPresenceStatus();
        contactIrc.setPresenceStatus(newStatus);
        fireContactPresenceStatusChangeEvent(contact, group, previous);
    }

    /**
     * Update the nick/id for an IRC contact.
     *
     * @param oldNick the old nick
     * @param newNick the new nick
     */
    void updateNick(final String oldNick, final String newNick)
    {
        ContactIrcImpl contact = findContactByID(oldNick);
        if (contact == null)
        {
            // Nick change is not meant for any known contact. Ignoring.
            return;
        }
        contact.setAddress(newNick);
        fireContactPropertyChangeEvent(
            ContactPropertyChangeEvent.PROPERTY_DISPLAY_NAME, contact, oldNick,
            newNick);
    }
}
