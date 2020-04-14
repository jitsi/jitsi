/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Copyright @ 2015 Atlassian Pty Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.java.sip.communicator.impl.protocol.icq;

import java.util.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.service.protocol.icqconstants.*;
import net.java.sip.communicator.util.*;
import net.kano.joscar.*;
import net.kano.joscar.flapcmd.*;
import net.kano.joscar.snac.*;
import net.kano.joscar.snaccmd.*;
import net.kano.joscar.snaccmd.conn.*;
import net.kano.joscar.snaccmd.error.*;
import net.kano.joscar.snaccmd.loc.*;
import net.kano.joustsim.*;
import net.kano.joustsim.oscar.*;
import net.kano.joustsim.oscar.oscar.service.bos.*;
import net.kano.joustsim.oscar.oscar.service.buddy.*;
import net.kano.joustsim.oscar.oscar.service.icon.*;
import net.kano.joustsim.oscar.oscar.service.info.*;
import net.kano.joustsim.oscar.oscar.service.ssi.*;
import net.kano.joustsim.trust.*;

/**
 * The ICQ implementation of a Persistent Presence Operation set. This class
 * manages our own presence status as well as subscriptions for the presence
 * status of our buddies. It also offers methods for retrieving and modifying
 * the buddy contact list and adding listeners for changes in its layout.
 *
 * @author Emil Ivov
 */
public class OperationSetPersistentPresenceIcqImpl
    extends AbstractOperationSetPersistentPresence<ProtocolProviderServiceIcqImpl>
{
    private static final Logger logger =
        Logger.getLogger(OperationSetPersistentPresenceIcqImpl.class);

    /**
     * This one should actually be in joscar. But since it isn't we might as
     * well define it here.
     */
    private static final long ICQ_ONLINE_MASK = 0x01000000L;

    /**
     * The IcqContact representing the local protocol provider.
     */
    private ContactIcqImpl localContact = null;

    /**
     * The listener that would react upon changes of the registration state of
     * our provider
     */
    private RegistrationStateListener registrationStateListener
        = new RegistrationStateListener();

    /**
     * The listener that would receive joust sim status updates for budddies in
     * our contact list
     */
    private JoustSimBuddyServiceListener joustSimBuddySerListener
        = new JoustSimBuddyServiceListener();

    /**
     * emcho: I think Bos stands for Buddy Online-status Service ... or at least
     * it seems like a plausible translation. This listener follows changes
     * in our own presence status and translates them in the corresponding
     * protocol provider events.
     */
    private JoustSimBosListener joustSimBosListener = new JoustSimBosListener();

    /**
     * Contains our current status message. Note that this field would only
     * be changed once the server has confirmed the new status message and
     * not immediately upon setting a new one..
     */
    private String currentStatusMessage = "";

    /**
     * The presence status that we were last notified of etnering.
     */
    private long currentIcqStatus = -1;

    private AuthorizationHandler authorizationHandler = null;
    private AuthListener authListener = new AuthListener();

    /**
     * The timer scheduling task that will query awaiting authorization
     * contacts for their status
     */
    private Timer presenceQueryTimer = null;

    /**
     *  Interval between queries for awaiting authorization
     *  contact statuses
     */
    private long PRESENCE_QUERY_INTERVAL = 120000l;

    /**
     *  Used to request authorization when a user comes online
     *  and haven't granted one
     */
    private OperationSetExtendedAuthorizationsIcqImpl opSetExtendedAuthorizations = null;

    /**
     *  Buddies seen availabel
     */
    private final List<String> buddiesSeenAvailable = new Vector<String>();

    /**
     * The array list we use when returning from the getSupportedStatusSet()
     * method.
     */
    private final List<PresenceStatus> supportedPresenceStatusSet = new ArrayList<PresenceStatus>();

    /**
     * A map containing bindings between SIP Communicator's icq presence status
     * instances and ICQ status codes
     */
    private static final Map<PresenceStatus, Long> scToIcqStatusMappings
        = new Hashtable<PresenceStatus, Long>();
    static{
        scToIcqStatusMappings.put(IcqStatusEnum.AWAY,
                                  FullUserInfo.ICQSTATUS_AWAY);
        scToIcqStatusMappings.put(IcqStatusEnum.DO_NOT_DISTURB,
                                  FullUserInfo.ICQSTATUS_DND);
        scToIcqStatusMappings.put(IcqStatusEnum.FREE_FOR_CHAT,
                                  FullUserInfo.ICQSTATUS_FFC);
        scToIcqStatusMappings.put(IcqStatusEnum.INVISIBLE,
                                  FullUserInfo.ICQSTATUS_INVISIBLE);
        scToIcqStatusMappings.put(IcqStatusEnum.NOT_AVAILABLE,
                                  FullUserInfo.ICQSTATUS_NA);
        scToIcqStatusMappings.put(IcqStatusEnum.OCCUPIED,
                                  FullUserInfo.ICQSTATUS_OCCUPIED);
        scToIcqStatusMappings.put(IcqStatusEnum.ONLINE,
                                  ICQ_ONLINE_MASK);
    }

    private static final Map<PresenceStatus, Long> scToAimStatusMappings
        = new Hashtable<PresenceStatus, Long>();
    static{
        scToAimStatusMappings.put(AimStatusEnum.AWAY,
                                  FullUserInfo.ICQSTATUS_AWAY);
        scToAimStatusMappings.put(AimStatusEnum.INVISIBLE,
                                  FullUserInfo.ICQSTATUS_INVISIBLE);
        scToAimStatusMappings.put(AimStatusEnum.ONLINE,
                                  ICQ_ONLINE_MASK);
    }

    /**
     * The server stored contact list that will be encapsulating joustsim's
     * buddy list.
     */
    private ServerStoredContactListIcqImpl ssContactList = null;

    /**
     * Creates a new Presence OperationSet over the specified icq provider.
     * @param icqProvider IcqProtocolProviderServiceImpl
     * @param uin the UIN of our account.
     */
    protected OperationSetPersistentPresenceIcqImpl(
                    ProtocolProviderServiceIcqImpl icqProvider,
                    String uin)
    {
        super(icqProvider);

        ssContactList = new ServerStoredContactListIcqImpl( this , icqProvider);

        //add a listener that'll follow the provider's state.
        parentProvider.addRegistrationStateChangeListener(
            registrationStateListener);
    }

    /**
     * Get the PresenceStatus for a particular contact. This method is not meant
     * to be used by the user interface (which would simply register as a
     * presence listener and always follow contact status) but rather by other
     * plugins that may for some reason need to know the status of a particular
     * contact.
     * <p>
     * @param contactIdentifier the dientifier of the contact whose status we're
     * interested in.
     * @return PresenceStatus the <tt>PresenceStatus</tt> of the specified
     * <tt>contact</tt>
     * @throws java.lang.IllegalStateException if the provider is not signed
     * on ICQ
     * @throws java.lang.IllegalArgumentException if <tt>contact</tt> is not
     * a valid <tt>IcqContact</tt>
     */
    public PresenceStatus queryContactStatus(String contactIdentifier)
        throws IllegalStateException, IllegalArgumentException
    {
        assertConnected();

        //these are commented since we now use identifiers.
        //        if (!(contact instanceof ContactIcqImpl))
        //            throw new IllegalArgumentException(
        //                "Cannont get status for a non-ICQ contact! ("
        //                + contact + ")");
        //
        //        ContactIcqImpl contactImpl = (ContactIcqImpl)contact;

        StatusResponseRetriever responseRetriever =
            new StatusResponseRetriever();

        GetInfoCmd getInfoCmd =
            new GetInfoCmd(GetInfoCmd.CMD_USER_INFO, contactIdentifier);

        parentProvider.getAimConnection().getInfoService().getOscarConnection()
            .sendSnacRequest(getInfoCmd, responseRetriever);

        synchronized(responseRetriever)
        {
            try{
                responseRetriever.wait(10000);
            }
            catch (InterruptedException ex){
                //we don't care
            }
        }

        return statusLongToPresenceStatus(responseRetriever.status);
    }

    /**
     * Converts the specified icqstatus to one of the status fields of the
     * IcqStatusEnum class.
     *
     * @param icqStatus the icqStatus as retured in FullUserInfo by the joscar
     *        stack
     * @return a PresenceStatus instance representation of the "long" icqStatus
     * parameter. The returned result is one of the IcqStatusEnum fields.
     */
    private PresenceStatus statusLongToPresenceStatus(long icqStatus)
    {
        if(parentProvider.USING_ICQ)
        {
            // Fixed order of status checking
            // The order does matter, as the icqStatus consists of more than one
            // status for example DND = OCCUPIED | DND | AWAY
            if(icqStatus == -1)
            {
                return IcqStatusEnum.OFFLINE;
            }
            else if ( (icqStatus & FullUserInfo.ICQSTATUS_INVISIBLE ) != 0)
            {
                return IcqStatusEnum.INVISIBLE;
            }
            else if ( (icqStatus & FullUserInfo.ICQSTATUS_DND ) != 0)
            {
                return IcqStatusEnum.DO_NOT_DISTURB;
            }
            else if ( (icqStatus & FullUserInfo.ICQSTATUS_OCCUPIED ) != 0)
            {
                return IcqStatusEnum.OCCUPIED;
            }
            else if ( (icqStatus & FullUserInfo.ICQSTATUS_NA ) != 0)
            {
                return IcqStatusEnum.NOT_AVAILABLE;
            }
            else if ( (icqStatus & FullUserInfo.ICQSTATUS_AWAY ) != 0)
            {
                return IcqStatusEnum.AWAY;
            }
            else if ( (icqStatus & FullUserInfo.ICQSTATUS_FFC ) != 0)
            {
                return IcqStatusEnum.FREE_FOR_CHAT;
            }

            // FIXED:  Issue 70
            // Incomplete status information in ICQ

            // if none of the statuses is satisfied
            // then the default is Online
            // there is no such status send from the server as Offline
            // when received error from server, after a query
            // the status is -1 so Offline
//            else if ((icqStatus & ICQ_ONLINE_MASK) == 0 )
//            {
//                return IcqStatusEnum.OFFLINE;
//            }

            return IcqStatusEnum.ONLINE;
        }
        else
        {
            if(icqStatus == -1)
            {
                return AimStatusEnum.OFFLINE;
            }
            else if ( (icqStatus & FullUserInfo.ICQSTATUS_INVISIBLE ) != 0)
            {
                return AimStatusEnum.INVISIBLE;
            }
            else if ( (icqStatus & FullUserInfo.ICQSTATUS_AWAY ) != 0)
            {
                return AimStatusEnum.AWAY;
            }

            return AimStatusEnum.ONLINE;
        }
    }

    /**
     * Converts the specified IcqStatusEnum member to the corresponding ICQ
     * flag.
     *
     * @param status the icqStatus as retured in FullUserInfo by the joscar
     *        stack
     * @return a PresenceStatus instance representation of the "long" icqStatus
     * parameter. The returned result is one of the IcqStatusEnum fields.
     */
    private long presenceStatusToStatusLong(PresenceStatus status)
    {
        if(parentProvider.USING_ICQ)
            return scToIcqStatusMappings.get(status).longValue();
        else
            return scToAimStatusMappings.get(status).longValue();
    }

    /**
     * Adds a subscription for the presence status of the contact corresponding
     * to the specified contactIdentifier. Apart from an exception in the case
     * of an immediate failure, the method won't return any indication of
     * success or failure. That would happen later on through a
     * SubscriptionEvent generated by one of the methods of the
     * SubscriptionListener.
     * <p>
     * This subscription is not going to be persistent (as opposed to
     * subscriptions added from the OperationSetPersistentPresence.subscribe()
     * method)
     * <p>
     * @param contactIdentifier the identifier of the contact whose status
     * updates we are subscribing for.
     * <p>
     * @throws OperationFailedException with code NETWORK_FAILURE if subscribing
     * fails due to errors experienced during network communication
     * @throws IllegalArgumentException if <tt>contact</tt> is not a contact
     * known to the underlying protocol provider
     * @throws IllegalStateException if the underlying protocol provider is not
     * registered/signed on a public service.
     */
    public void subscribe(String contactIdentifier)
        throws IllegalArgumentException,
               IllegalStateException,
               OperationFailedException
    {
        assertConnected();

        ssContactList.addContact(contactIdentifier);
    }

    /**
     * Creates a non persistent contact for the specified address. This would
     * also create (if necessary) a group for volatile contacts that would not
     * be added to the server stored contact list. The volatile contact would
     * remain in the list until it is really added to the contact list or
     * until the application is terminated.
     * @param uin the UIN/Screenname of the contact to create.
     * @return the newly created volatile <tt>ContactIcqImpl</tt>
     */
    public ContactIcqImpl createVolatileContact(String uin)
    {
        return ssContactList.createVolatileContact(new Screenname(uin));
    }

    /**
     * Creates and returns a unresolved contact from the specified
     * <tt>address</tt> and <tt>persistentData</tt>. The method will not try
     * to establish a network connection and resolve the newly created Contact
     * against the server. The protocol provider may will later try and resolve
     * the contact. When this happens the corresponding event would notify
     * interested subscription listeners.
     *
     * @param address an identifier of the contact that we'll be creating.
     * @param persistentData a String returned Contact's getPersistentData()
     * method during a previous run and that has been persistently stored
     * locally.
     * @param parentGroup the group that the unresolved contact should belong to.
     * @return the unresolved <tt>Contact</tt> created from the specified
     * <tt>address</tt> and <tt>persistentData</tt>
     *
     * @throws java.lang.IllegalArgumentException if <tt>parentGroup</tt> is not
     * an instance of ContactGroupIcqImpl
     */
    public Contact createUnresolvedContact(String address,
                                           String persistentData,
                                           ContactGroup parentGroup)
        throws IllegalArgumentException
    {
        if(! (parentGroup instanceof ContactGroupIcqImpl) )
            throw new IllegalArgumentException(
                "Argument is not an icq contact group (group="
                + parentGroup + ")");

        ContactIcqImpl contact =
            ssContactList.createUnresolvedContact(
            (ContactGroupIcqImpl)parentGroup, new Screenname(address));

        contact.setPersistentData(persistentData);

        return contact;
    }

    /**
     * Creates and returns a unresolved contact from the specified
     * <tt>address</tt> and <tt>persistentData</tt>. The method will not try
     * to establish a network connection and resolve the newly created Contact
     * against the server. The protocol provider may will later try and resolve
     * the contact. When this happens the corresponding event would notify
     * interested subscription listeners.
     *
     * @param address an identifier of the contact that we'll be creating.
     * @param persistentData a String returned Contact's getPersistentData()
     * method during a previous run and that has been persistently stored
     * locally.
     *
     * @return the unresolved <tt>Contact</tt> created from the specified
     * <tt>address</tt> and <tt>persistentData</tt>
     */
    public Contact createUnresolvedContact(String address,
                                           String persistentData)
    {
        return createUnresolvedContact(  address
                                       , persistentData
                                       , getServerStoredContactListRoot());
    }

    /**
     * Creates and returns a unresolved contact group from the specified
     * <tt>address</tt> and <tt>persistentData</tt>. The method will not try
     * to establish a network connection and resolve the newly created
     * <tt>ContactGroup</tt> against the server or the contact itself. The
     * protocol provider will later resolve the contact group. When this happens
     * the corresponding event would notify interested subscription listeners.
     *
     * @param groupUID an identifier, returned by ContactGroup's getGroupUID,
     * that the protocol provider may use in order to create the group.
     * @param persistentData a String returned ContactGroups's getPersistentData()
     * method during a previous run and that has been persistently stored
     * locally.
     * @param parentGroup the group under which the new group is to be created
     * or null if this is group directly underneath the root.
     * @return the unresolved <tt>ContactGroup</tt> created from the specified
     * <tt>uid</tt> and <tt>persistentData</tt>
     */
    public ContactGroup createUnresolvedContactGroup(String groupUID,
        String persistentData, ContactGroup parentGroup)
    {
        //silently ignore the parent group. ICQ does not support subgroups so
        //parentGroup is supposed to be root. if it is not however we're not
        //going to complain because we're cool :).
        return ssContactList.createUnresolvedContactGroup(groupUID);
    }

    /**
     * Persistently adds a subscription for the presence status of the  contact
     * corresponding to the specified contactIdentifier and indicates that it
     * should be added to the specified group of the server stored contact list.
     * Note that apart from an exception in the case of an immediate failure,
     * the method won't return any indication of success or failure. That would
     * happen later on through a SubscriptionEvent generated by one of the
     * methods of the SubscriptionListener.
     * <p>
     * @param contactIdentifier the contact whose status updates we are subscribing
     *   for.
     * @param parent the parent group of the server stored contact list where
     * the contact should be added.
     * <p>
     * <p>
     * @throws OperationFailedException with code NETWORK_FAILURE if subscribing
     * fails due to errors experienced during network communication
     * @throws IllegalArgumentException if <tt>contact</tt> or
     * <tt>parent</tt> are not a contact known to the underlying protocol
     * provider.
     * @throws IllegalStateException if the underlying protocol provider is not
     * registered/signed on a public service.
     */
    public void subscribe(ContactGroup parent, String contactIdentifier)
        throws IllegalArgumentException,
               IllegalStateException,
               OperationFailedException
    {
        assertConnected();

        if(! (parent instanceof ContactGroupIcqImpl) )
            throw new IllegalArgumentException(
                "Argument is not an icq contact group (group=" + parent + ")");

        ssContactList.addContact((ContactGroupIcqImpl)parent, contactIdentifier);
    }

    /**
     * Removes a subscription for the presence status of the specified contact.
     * @param contact the contact whose status updates we are unsubscribing from.
     *
     * @throws OperationFailedException with code NETWORK_FAILURE if unsubscribing
     * fails due to errors experienced during network communication
     * @throws IllegalArgumentException if <tt>contact</tt> is not a contact
     * known to this protocol provider or is not an ICQ contact
     * @throws IllegalStateException if the underlying protocol provider is not
     * registered/signed on a public service.
     */
    public void unsubscribe(Contact contact) throws IllegalArgumentException,
        IllegalStateException, OperationFailedException
    {
        assertConnected();

        if(! (contact instanceof ContactIcqImpl) )
            throw new IllegalArgumentException(
                "Argument is not an icq contact (contact=" + contact + ")");

        ContactIcqImpl contactIcqImpl = (ContactIcqImpl)contact;

        ContactGroupIcqImpl contactGroup
            = ssContactList.findContactGroup(contactIcqImpl);

        if (contactGroup == null)
            throw new IllegalArgumentException(
              "The specified contact was not found on the local "
              +"contact/subscription list: " + contact);

        if(!contactIcqImpl.isPersistent())
        {
            contactGroup.removeContact(contactIcqImpl);
            fireSubscriptionEvent(contactIcqImpl,
                                  contactGroup,
                                  SubscriptionEvent.SUBSCRIPTION_REMOVED);

            return;
        }

        if (logger.isTraceEnabled())
            logger.trace("Going to remove contact from ss-list : " + contact);

        if( !contactGroup.isPersistent()
            && contactIcqImpl.getJoustSimBuddy().isAwaitingAuthorization())
        {
            // this is contact in AwaitingAuthorization group
            // we must find the original parent and remove it from there
            ContactGroupIcqImpl origParent =
                ssContactList.findGroup(contactIcqImpl.getJoustSimBuddy());

            if(origParent != null)
            {
                origParent.getJoustSimSourceGroup().
                    deleteBuddy(contactIcqImpl.getJoustSimBuddy());
            }
        }
        else
        {
            MutableGroup joustSimContactGroup = contactGroup.getJoustSimSourceGroup();

            joustSimContactGroup.deleteBuddy(contactIcqImpl.getJoustSimBuddy());
        }
    }

    /**
     * Returns a reference to the contact with the specified ID in case we have
     * a subscription for it and null otherwise/
     * @param contactID a String identifier of the contact which we're seeking a
     * reference of.
     * @return a reference to the Contact with the specified
     * <tt>contactID</tt> or null if we don't have a subscription for the
     * that identifier.
     */
    public Contact findContactByID(String contactID)
    {
        return ssContactList.findContactByScreenName(contactID);
    }

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
     * @throws java.lang.IllegalStateException if the provider is not currently
     * registered.
     * @throws OperationFailedException with code NETWORK_FAILURE if publishing
     * the status fails due to a network error.
     */
    public void publishPresenceStatus(PresenceStatus status,
                                      String statusMessage) throws
        IllegalArgumentException, IllegalStateException,
        OperationFailedException
    {
        assertConnected();

        if (!(status instanceof IcqStatusEnum || status instanceof AimStatusEnum))
            throw new IllegalArgumentException(
                            status + " is not a valid ICQ/AIM status");

        if (logger.isDebugEnabled())
            logger.debug("Will set status: " + status);

        MainBosService bosService
            = parentProvider.getAimConnection().getBosService();

        if(!parentProvider.USING_ICQ)
        {
            if(status.equals(AimStatusEnum.AWAY))
            {
                if(getPresenceStatus().equals(AimStatusEnum.INVISIBLE))
                    bosService.setVisibleStatus(true);

                bosService.getOscarConnection().sendSnac(new SetInfoCmd(
                    new InfoData(null, "I'm away!", null, null)));
            }
            else if(status.equals(AimStatusEnum.INVISIBLE))
            {
                if(getPresenceStatus().equals(AimStatusEnum.AWAY))
                    bosService.getOscarConnection().sendSnac(new SetInfoCmd(
                        new InfoData(null, InfoData.NOT_AWAY, null, null)));

                bosService.setVisibleStatus(false);
            }
            else if(status.equals(AimStatusEnum.ONLINE))
            {
                if(getPresenceStatus().equals(AimStatusEnum.INVISIBLE))
                    bosService.setVisibleStatus(true);
                else if(getPresenceStatus().equals(AimStatusEnum.AWAY))
                {
                    bosService.getOscarConnection().sendSnac(new SetInfoCmd(
                        new InfoData(null, InfoData.NOT_AWAY, null, null)));
                }
            }
        }
        else
        {
            long icqStatus = presenceStatusToStatusLong(status);

            if (logger.isDebugEnabled())
                logger.debug("Will set status: " + status + " long=" + icqStatus);

            bosService.getOscarConnection().sendSnac(new SetExtraInfoCmd(icqStatus));

            if(status.equals(IcqStatusEnum.AWAY))
                parentProvider.getAimConnection().getInfoService().
                    setAwayMessage(statusMessage);
            else
                bosService.setStatusMessage(statusMessage);
        }

        //so that everyone sees the change.
        queryContactStatus(
            parentProvider.getAimConnection().getScreenname().getFormatted());
    }

    /**
     * Returns the status message that was confirmed by the serfver
     * @return the last status message that we have requested and the aim server
     * has confirmed.
     */
    public String getCurrentStatusMessage()
    {
        return this.currentStatusMessage;
    }

    /**
     * Returns the protocol specific contact instance representing the local
     * user.
     *
     * @return the Contact (address, phone number, or uin) that the Provider
     *   implementation is communicating on behalf of.
     */
    public Contact getLocalContact()
    {
        return localContact;
    }

    /**
     * Creates a group with the specified name and parent in the server stored
     * contact list.
     * @param groupName the name of the new group to create.
     * @param parent the group where the new group should be created
     *
     * @throws OperationFailedException with code NETWORK_FAILURE if unsubscribing
     * fails due to errors experienced during network communication
     * @throws IllegalArgumentException if <tt>contact</tt> is not a contact
     * known to the underlying protocol provider
     * @throws IllegalStateException if the underlying protocol provider is not
     * registered/signed on a public service.
     */
    public void createServerStoredContactGroup(ContactGroup parent,
        String groupName)
    {
        assertConnected();

        if (!parent.canContainSubgroups())
            throw new IllegalArgumentException(
                "The specified contact group cannot contain child groups. Group:"
                + parent );

        ssContactList.createGroup(groupName);
    }

    /**
     * Removes the specified group from the server stored contact list.
     * @param group the group to remove.
     *
     * @throws OperationFailedException with code NETWORK_FAILURE if deleting
     * the group fails because of a network error.
     * @throws IllegalArgumentException if <tt>parent</tt> is not a contact
     * known to the underlying protocol provider.
     * @throws IllegalStateException if the underlying protocol provider is not
     * registered/signed on a public service.
     */
    public void removeServerStoredContactGroup(ContactGroup group)
    {
        assertConnected();

        if( !(group instanceof ContactGroupIcqImpl) )
            throw new IllegalArgumentException(
                "The specified group is not an icq contact group: " + group);

        ssContactList.removeGroup((ContactGroupIcqImpl)group);
    }

    /**
     * Renames the specified group from the server stored contact list. This
     * method would return before the group has actually been renamed. A
     * <tt>ServerStoredGroupEvent</tt> would be dispatched once new name
     * has been acknowledged by the server.
     *
     * @param group the group to rename.
     * @param newName the new name of the group.
     *
     * @throws OperationFailedException with code NETWORK_FAILURE if deleting
     * the group fails because of a network error.
     * @throws IllegalArgumentException if <tt>parent</tt> is not a contact
     * known to the underlying protocol provider.
     * @throws IllegalStateException if the underlying protocol provider is not
     * registered/signed on a public service.
     */
    public void renameServerStoredContactGroup(
                    ContactGroup group, String newName)
    {
        assertConnected();

        if( !(group instanceof ContactGroupIcqImpl) )
            throw new IllegalArgumentException(
                "The specified group is not an icq contact group: " + group);

        ssContactList.renameGroup((ContactGroupIcqImpl)group, newName);
    }

    /**
     * Removes the specified contact from its current parent and places it
     * under <tt>newParent</tt>.
     * @param contactToMove the <tt>Contact</tt> to move
     * @param newParent the <tt>ContactGroup</tt> where <tt>Contact</tt>
     * would be placed.
     */
    public void moveContactToGroup(Contact contactToMove,
                                   ContactGroup newParent)
    {
        assertConnected();

        if( !(contactToMove instanceof ContactIcqImpl) )
            throw new IllegalArgumentException(
                "The specified contact is not an icq contact." + contactToMove);
        if( !(newParent instanceof ContactGroupIcqImpl) )
            throw new IllegalArgumentException(
                "The specified group is not an icq contact group."
                + newParent);

        ssContactList.moveContact((ContactIcqImpl)contactToMove,
                                  (ContactGroupIcqImpl)newParent);
    }

    /**
     * Returns a snapshot ieves a server stored list of subscriptions/contacts that have been
     * made previously. Note that the contact list returned by this method may
     * be incomplete as it is only a snapshot of what has been retrieved through
     * the network up to the moment when the method is called.
     * @return a ConactGroup containing all previously made subscriptions stored
     * on the server.
     */
    ServerStoredContactListIcqImpl getServerStoredContactList()
    {
        return ssContactList;
    }

    /**
     * Returns a PresenceStatus instance representing the state this provider
     * is currently in.
     *
     * @return PresenceStatus
     */
    public PresenceStatus getPresenceStatus()
    {
        return statusLongToPresenceStatus(currentIcqStatus);
    }

    /**
     * Returns the set of PresenceStatus objects that a user of this service
     * may request the provider to enter.
     *
     * @return Iterator a PresenceStatus array containing "enterable" status
     *   instances.
     */
    public Iterator<PresenceStatus> getSupportedStatusSet()
    {
        if(supportedPresenceStatusSet.size() == 0)
        {
            if(parentProvider.USING_ICQ)
            {
                supportedPresenceStatusSet.add(IcqStatusEnum.ONLINE);
                supportedPresenceStatusSet.add(IcqStatusEnum.DO_NOT_DISTURB);
                supportedPresenceStatusSet.add(IcqStatusEnum.FREE_FOR_CHAT);
                supportedPresenceStatusSet.add(IcqStatusEnum.NOT_AVAILABLE);
                supportedPresenceStatusSet.add(IcqStatusEnum.OCCUPIED);
                supportedPresenceStatusSet.add(IcqStatusEnum.AWAY);
                supportedPresenceStatusSet.add(IcqStatusEnum.INVISIBLE);
                supportedPresenceStatusSet.add(IcqStatusEnum.OFFLINE);
            }
            else
            {
                supportedPresenceStatusSet.add(AimStatusEnum.ONLINE);
                supportedPresenceStatusSet.add(AimStatusEnum.AWAY);
                supportedPresenceStatusSet.add(AimStatusEnum.INVISIBLE);
                supportedPresenceStatusSet.add(AimStatusEnum.OFFLINE);
            }
        }

        return supportedPresenceStatusSet.iterator();
    }

    /**
     * Handler for incoming authorization requests. An authorization request
     * notifies the user that someone is trying to add her to their contact list
     * and requires her to approve or reject authorization for that action.
     * @param handler an instance of an AuthorizationHandler for authorization
     * requests coming from other users requesting permission add us to their
     * contact list.
     */
    public void setAuthorizationHandler(AuthorizationHandler handler)
    {
        /** @todo
         * method to be removed and AuthorizationHandler to be set
         * set upon creation of the
         * provider so that there could be only one.
         *
         **/
        this.authorizationHandler = handler;

        parentProvider.getAimConnection().getSsiService().
            addBuddyAuthorizationListener(authListener);
    }

    /**
     * The StatusResponseRetriever is used as a one time handler for responses
     * to requests sent through the sendSnacRequest method of one of joustsim's
     * Services. The StatusResponseRetriever would ignore everything apart from
     * the first response, which will be stored in the status field. In the
     * case of a timeout, the status would remain on -1. Both a response and
     * a timeout would make the StatusResponseRetriever call its notifyAll
     * method so that those that are waiting on it be notified.
     */
    private static class StatusResponseRetriever extends SnacRequestAdapter
    {
            private boolean ran = false;
            private long status = -1;

            @Override
            public void handleResponse(SnacResponseEvent e) {
                SnacCommand snac = e.getSnacCommand();

                synchronized(this) {
                    if (ran) return;
                    ran = true;
                }

                if (snac instanceof UserInfoCmd)
                {
                    UserInfoCmd uic = (UserInfoCmd) snac;

                    FullUserInfo userInfo = uic.getUserInfo();
                    if (userInfo != null)
                    {
                        this.status = userInfo.getIcqStatus();

                        // StatusResponseRetriever is used when query for
                        // user status if status is not set (is -1)
                        // this means user is offline.
                        //if (this.status == -1)
                        //    status = ICQ_ONLINE_MASK;

                        synchronized(this){
                            this.notifyAll();
                        }
                    }
                }
                else if( snac instanceof SnacError)
                {
                    //this is most probably a CODE_USER_UNAVAILABLE, but
                    //whatever it is it means that to us the buddy in question
                    //is as good as offline so leave status at -1 and notify.

                    if (logger.isDebugEnabled())
                        logger.debug("status is" + status);
                    synchronized(this){
                        this.notifyAll();
                    }
                }

            }

            @Override
            public void handleTimeout(SnacRequestTimeoutEvent event) {
                synchronized(this) {
                    if (ran) return;
                    ran = true;
                    notifyAll();
                }
            }
    }

    /**
     * Utility method throwing an exception if the icq stack is not properly
     * initialized.
     * @throws java.lang.IllegalStateException if the underlying ICQ stack is
     * not registered and initialized.
     */
    private void assertConnected() throws IllegalStateException
    {
        if (parentProvider == null)
            throw new IllegalStateException(
                "The icq provider must be non-null and signed on the ICQ "
                +"service before being able to communicate.");
        if (!parentProvider.isRegistered())
            throw new IllegalStateException(
                "The icq provider must be signed on the ICQ service before "
                +"being able to communicate.");
    }

    /**
     * Returns the root group of the server stored contact list. Most often this
     * would be a dummy group that user interface implementations may better not
     * show.
     *
     * @return the root ContactGroup for the ContactList stored by this service.
     */
    public ContactGroup getServerStoredContactListRoot()
    {
        return ssContactList.getRootGroup();
    }

    /**
     * Registers a listener that would receive events upong changes in server
     * stored groups.
     * @param listener a ServerStoredGroupChangeListener impl that would receive
     * events upong group changes.
     */
    @Override
    public void addServerStoredGroupChangeListener(
        ServerStoredGroupListener listener)
    {
        ssContactList.addGroupListener(listener);
    }

    /**
     * Removes the specified group change listener so that it won't receive
     * any further events.
     * @param listener the ServerStoredGroupChangeListener to remove
     */
    @Override
    public void removeServerStoredGroupChangeListener(
        ServerStoredGroupListener listener)
    {
        ssContactList.removeGroupListener(listener);
    }

    /**
     * Notify all provider presence listeners of the corresponding event change
     *
     * @param oldStatusL
     *            the status our icq stack had so far
     * @param newStatusL
     *            the status we have from now on
     */
    void fireProviderPresenceStatusChangeEvent(long oldStatusL, long newStatusL)
    {
        PresenceStatus oldStatus = statusLongToPresenceStatus(oldStatusL);
        PresenceStatus newStatus = statusLongToPresenceStatus(newStatusL);

        if (oldStatus.equals(newStatus))
            if (logger.isDebugEnabled())
                logger.debug(
                "Ignored prov stat. change evt. old==new = "
                    + oldStatus);
        else
            fireProviderStatusChangeEvent(oldStatus, newStatus);
    }

    /**
     * Our listener that will tell us when we're registered to icq and joust
     * sim is ready to accept us as a listener.
     */
    private class RegistrationStateListener
        implements RegistrationStateChangeListener
    {
        /**
         * The method is called by a ProtocolProvider implementation whenver
         * a change in the registration state of the corresponding provider had
         * occurred.
         * @param evt ProviderStatusChangeEvent the event describing the status
         * change.
         */
        public void registrationStateChanged(RegistrationStateChangeEvent evt)
        {
            if (logger.isDebugEnabled())
                logger.debug("The ICQ provider changed state from: "
                         + evt.getOldState()
                         + " to: " + evt.getNewState());
            if(evt.getNewState() == RegistrationState.FINALIZING_REGISTRATION)
            {
                if (logger.isDebugEnabled())
                    logger.debug("adding a Bos Service Listener");
                parentProvider.getAimConnection().getBosService()
                    .addMainBosServiceListener(joustSimBosListener);

                ssContactList.init(
                    parentProvider.getAimConnection().getSsiService());

//                /**@todo implement the following
                parentProvider.getAimConnection().getBuddyService()
                     .addBuddyListener(joustSimBuddySerListener);

//                  @todo we really need this for following the status of our
//                 contacts and we really need it here ...*/
                parentProvider.getAimConnection().getBuddyInfoManager()
                    .addGlobalBuddyInfoListener(new GlobalBuddyInfoListener());

                parentProvider.getAimConnection().getExternalServiceManager().
                    getIconServiceArbiter().addIconRequestListener(
                        new IconUpdateListener());

                parentProvider.getAimConnection().getInfoService().
                    addInfoListener(new AwayMessageListener());
            }
            else if(evt.getNewState() == RegistrationState.REGISTERED)
            {
                if(parentProvider.USING_ICQ)
                {
                    opSetExtendedAuthorizations =
                        (OperationSetExtendedAuthorizationsIcqImpl) parentProvider
                            .getOperationSet(OperationSetExtendedAuthorizations.class);

                    if(presenceQueryTimer == null)
                        presenceQueryTimer = new Timer();
                    else
                    {
                        // cancel any previous jobs and create new timer
                        presenceQueryTimer.cancel();
                        presenceQueryTimer = new Timer();
                    }

                    AwaitingAuthorizationContactsPresenceTimer
                        queryTask = new AwaitingAuthorizationContactsPresenceTimer();

                    // start after 15 seconds. wait for login to be completed and
                    // list and statuses to be gathered
                    presenceQueryTimer.scheduleAtFixedRate(
                            queryTask, 15000, PRESENCE_QUERY_INTERVAL);
                }
            }
            else if(evt.getNewState() == RegistrationState.UNREGISTERED
                 || evt.getNewState() == RegistrationState.AUTHENTICATION_FAILED
                 || evt.getNewState() == RegistrationState.CONNECTION_FAILED)
            {
                if(presenceQueryTimer != null)
                {
                    presenceQueryTimer.cancel();
                    presenceQueryTimer = null;
                }

                //since we are disconnected, we won't receive any further status
                //updates so we need to change by ourselves our own status as
                //well as set to offline all contacts in our contact list that
                //were online

                //start by notifying that our own status has changed
                long oldStatus  = currentIcqStatus;
                currentIcqStatus = -1;

                //only notify of an event change if there was really one.
                if( oldStatus != -1 )
                    fireProviderPresenceStatusChangeEvent(oldStatus,
                                                          currentIcqStatus);

                //send event notifications saying that all our buddies are
                //offline. The icq protocol does not implement top level buddies
                //nor subgroups for top level groups so a simple nested loop
                //would be enough.
                Iterator<ContactGroup> groupsIter
                    = getServerStoredContactListRoot().subgroups();

                while(groupsIter.hasNext())
                {
                    ContactGroup group = groupsIter.next();
                    Iterator<Contact> contactsIter = group.contacts();

                    while(contactsIter.hasNext())
                    {
                        ContactIcqImpl contact
                            = (ContactIcqImpl)contactsIter.next();

                        PresenceStatus oldContactStatus
                            = contact.getPresenceStatus();

                        if(!oldContactStatus.isOnline())
                            continue;

                        if(parentProvider.USING_ICQ)
                        {
                            contact.updatePresenceStatus(IcqStatusEnum.OFFLINE);

                            fireContactPresenceStatusChangeEvent(
                                  contact
                                , contact.getParentContactGroup()
                                , oldContactStatus, IcqStatusEnum.OFFLINE);
                        }
                        else
                        {
                            contact.updatePresenceStatus(AimStatusEnum.OFFLINE);

                            fireContactPresenceStatusChangeEvent(
                                  contact
                                , contact.getParentContactGroup()
                                , oldContactStatus, AimStatusEnum.OFFLINE);
                        }
                    }
                }
            }
        }
    }

    /**
     * The listeners that would monitor the joust sim stack for changes in our
     * own presence status.
     */
    private class JoustSimBosListener implements MainBosServiceListener
    {
        /**
         * Notifications of exta information such as avail message, icon hash
         * or certificate.
         * @param extraInfos List
         */
        public void handleYourExtraInfo(List<ExtraInfoBlock> extraInfos)
        {
            if (logger.isDebugEnabled())
                logger.debug("Got extra info: " + extraInfos);
            // @xxx we should one day probably do something here, like check
            // whether the status message has been changed for example.
            for (ExtraInfoBlock block : extraInfos)
            {
                if (block.getType() == ExtraInfoBlock.TYPE_AVAILMSG){
                    String statusMessage = ExtraInfoData.readAvailableMessage(
                                                    block.getExtraData());
                    if (logger.isDebugEnabled())
                        logger.debug("Received a status message:" + statusMessage);

                    if ( getCurrentStatusMessage().equals(statusMessage)){
                        if (logger.isDebugEnabled())
                            logger.debug("Status message is same as old. Ignoring");
                        return;
                    }

                    String oldStatusMessage = getCurrentStatusMessage();
                    currentStatusMessage = statusMessage;

                    fireProviderStatusMessageChangeEvent(
                        oldStatusMessage, getCurrentStatusMessage());
                }
            }
        }

        /**
         * Fires the corresponding presence status chagne event. Note that this
         * method will be called once per sendSnac packet. When setting a new
         * status we generally send three packets - 1 for the status and 2 for
         * the status message. Make sure that only one event goes outside of
         * this package.
         *
         * @param service the source MainBosService instance
         * @param userInfo our own info
         */
        public void handleYourInfo(MainBosService service,
                                   FullUserInfo userInfo)
        {
            if (logger.isDebugEnabled())
                logger.debug("Received our own user info: " + userInfo);
            if (logger.isDebugEnabled())
                logger.debug("previous status was: " + currentIcqStatus);
            if (logger.isDebugEnabled())
                logger.debug("new status is: " + userInfo.getIcqStatus());

            //update the last received field.
            long oldStatus  = currentIcqStatus;

            if(parentProvider.USING_ICQ)
            {
                currentIcqStatus = userInfo.getIcqStatus();

                //it might happen that the info here is -1 (in case we're going back
                //to online). Yet the fact that we're getting the event means
                //that we're very much online so make sure we change accordingly
                if (currentIcqStatus == -1 )
                    currentIcqStatus = ICQ_ONLINE_MASK;

                //only notify of an event change if there was really one.
                if( oldStatus !=  currentIcqStatus)
                    fireProviderPresenceStatusChangeEvent(oldStatus,
                                                        currentIcqStatus);
            }
            else
            {
                if(userInfo.getAwayStatus() != null
                    && userInfo.getAwayStatus().equals(Boolean.TRUE))
                {
                    currentIcqStatus = presenceStatusToStatusLong(AimStatusEnum.AWAY);
                }
                else if(userInfo.getIcqStatus() != -1)
                {
                    currentIcqStatus = userInfo.getIcqStatus();
                }
                else // online status
                    currentIcqStatus = ICQ_ONLINE_MASK;

                if( oldStatus != currentIcqStatus )
                    fireProviderPresenceStatusChangeEvent(oldStatus,
                                                        currentIcqStatus);
            }
        }
    }

    /**
     * Listens for status updates coming from the joust sim statck and generates
     * the corresponding sip-communicator events.
     * @author Emil Ivov
     */
    private class JoustSimBuddyServiceListener implements BuddyServiceListener
    {

        /**
         * Updates the last received status in the corresponding contact
         * and fires a contact presence status change event.
         * @param service the BuddyService that generated the exception
         * @param buddy the Screenname of the buddy whose status update we're
         * receiving
         * @param info the FullUserInfo containing the new status of the
         * corresponding contact
         */
        public void gotBuddyStatus(BuddyService service, Screenname buddy,
                                   FullUserInfo info)
        {
            if (logger.isDebugEnabled())
                logger.debug("Received a status update for buddy=" + buddy);
            if (logger.isDebugEnabled())
                logger.debug("Updated user info is " + info);

            ContactIcqImpl sourceContact
                = ssContactList.findContactByScreenName(buddy.getFormatted());

            if(sourceContact == null){
                logger.warn("No source contact found for screenname=" + buddy);
                return;
            }
            PresenceStatus oldStatus
                = sourceContact.getPresenceStatus();

            PresenceStatus newStatus = null;

            if(!parentProvider.USING_ICQ)
            {
                Boolean awayStatus = info.getAwayStatus();
                if(awayStatus == null || awayStatus.equals(Boolean.FALSE))
                    newStatus = AimStatusEnum.ONLINE;
                else
                    newStatus = AimStatusEnum.AWAY;
            }
            else
                newStatus = statusLongToPresenceStatus(info.getIcqStatus());

            sourceContact.updatePresenceStatus(newStatus);

            ContactGroupIcqImpl parent
                = ssContactList.findContactGroup(sourceContact);

            Iterable<ExtraInfoBlock> extraInfoBlocks
                = info.getExtraInfoBlocks();
            if(extraInfoBlocks != null){
                for (ExtraInfoBlock block : extraInfoBlocks)
                {
                    if (block.getType() == ExtraInfoBlock.TYPE_AVAILMSG)
                    {
                        String status = ExtraInfoData.readAvailableMessage(
                            block.getExtraData());
                        if (logger.isInfoEnabled())
                            logger.info("Status Message is: " + status + ".");
                        sourceContact.setStatusMessage(status);
                    }
                }
            }

            if (logger.isDebugEnabled())
                logger.debug("Will Dispatch the contact status event.");
            fireContactPresenceStatusChangeEvent(sourceContact, parent,
                                                 oldStatus, newStatus);
        }

        /**
         * Updates the last received status in the corresponding contact
         * and fires a contact presence status change event.
         *
         * @param service the BuddyService that generated the exception
         * @param buddy the Screenname of the buddy whose status update we're
         * receiving
         */
        public void buddyOffline(BuddyService service, Screenname buddy)
        {
            if (logger.isDebugEnabled())
                logger.debug("Received a status update for buddy=" + buddy);

            ContactIcqImpl sourceContact
                = ssContactList.findContactByScreenName(buddy.getFormatted());

            if(sourceContact == null)
                return;

            PresenceStatus oldStatus
                = sourceContact.getPresenceStatus();
            PresenceStatus newStatus = null;

            if(parentProvider.USING_ICQ)
                newStatus = IcqStatusEnum.OFFLINE;
            else
                newStatus = AimStatusEnum.OFFLINE;

            sourceContact.updatePresenceStatus(newStatus);

            ContactGroupIcqImpl parent
                = ssContactList.findContactGroup(sourceContact);

            fireContactPresenceStatusChangeEvent(sourceContact, parent,
                                                 oldStatus, newStatus);
        }
    }

    /**
     * Apart from login - does nothing so far.
     */
    private class GlobalBuddyInfoListener extends GlobalBuddyInfoAdapter{
        @Override
        public void receivedStatusUpdate(BuddyInfoManager manager,
                                         Screenname buddy, BuddyInfo info)
        {
            String statusMessage = info.getStatusMessage();
            if (logger.isDebugEnabled())
                logger.debug("buddy=" + buddy);
            if (logger.isDebugEnabled())
                logger.debug("info.getAwayMessage()=" + info.getAwayMessage());
            if (logger.isDebugEnabled())
                logger.debug("info.getOnlineSince()=" + info.getOnlineSince());
            if (logger.isDebugEnabled())
                logger.debug("info.getStatusMessage()=" + statusMessage);

            ContactIcqImpl sourceContact
                = ssContactList.findContactByScreenName(buddy.getFormatted());

            if(sourceContact != null)
            {
                sourceContact.setStatusMessage(statusMessage);
            }

        }

    }

    private class AuthListener
        implements BuddyAuthorizationListener
    {
        public void authorizationDenied(Screenname screenname, String reason)
        {
            if (logger.isTraceEnabled())
                logger.trace("authorizationDenied from " + screenname);
            Contact srcContact = findContactByID(screenname.getFormatted());

            authorizationHandler.processAuthorizationResponse(
                new AuthorizationResponse(AuthorizationResponse.REJECT, reason)
                , srcContact);
            try
            {
                unsubscribe(srcContact);
            } catch (OperationFailedException ex)
            {
                logger.error("cannot remove denied contact : " + srcContact, ex);
            }
        }

        public void authorizationAccepted(Screenname screenname, String reason)
        {
            if (logger.isTraceEnabled())
                logger.trace("authorizationAccepted from " + screenname);
            Contact srcContact = findContactByID(screenname.getFormatted());

            authorizationHandler.processAuthorizationResponse(
                new AuthorizationResponse(AuthorizationResponse.ACCEPT, reason)
                , srcContact);
        }

        public void authorizationRequestReceived(Screenname screenname,
                                                 String reason)
        {
            if (logger.isTraceEnabled())
                logger.trace("authorizationRequestReceived from " + screenname);
            Contact srcContact = findContactByID(screenname.getFormatted());

            if(srcContact == null)
                srcContact = createVolatileContact(screenname.getFormatted());

            AuthorizationRequest authRequest = new AuthorizationRequest();
                authRequest.setReason(reason);

            AuthorizationResponse authResponse =
                authorizationHandler.processAuthorisationRequest(
                    authRequest, srcContact);


            if (authResponse.getResponseCode() == AuthorizationResponse.IGNORE)
                return;

            parentProvider.getAimConnection().getSsiService().
                replyBuddyAuthorization(
                    screenname,
                    authResponse.getResponseCode() == AuthorizationResponse.ACCEPT,
                    authResponse.getReason());
        }

        public boolean authorizationRequired(Screenname screenname, Group parentGroup)
        {
            if (logger.isTraceEnabled())
                logger.trace("authorizationRequired from " + screenname);

            if (logger.isTraceEnabled())
                logger.trace("finding buddy : " + screenname);
            ContactIcqImpl srcContact =
                ssContactList.findContactByScreenName(screenname.getFormatted());

            if(srcContact == null)
            {
                ContactGroupIcqImpl parent =
                    ssContactList.findContactGroup(parentGroup);
                srcContact = ssContactList.
                    createUnresolvedContact(parent, screenname);

                Buddy buddy = srcContact.getJoustSimBuddy();

                if(buddy instanceof VolatileBuddy)
                    ((VolatileBuddy)buddy).setAwaitingAuthorization(true);
            }

            AuthorizationRequest authRequest =
                authorizationHandler.createAuthorizationRequest(
                srcContact);

            if(authRequest == null)
                return false;

            parentProvider.getAimConnection().getSsiService().
                sendFutureBuddyAuthorization(screenname, authRequest.getReason());

            parentProvider.getAimConnection().getSsiService().
                requestBuddyAuthorization(screenname, authRequest.getReason());

            return true;
        }

        public void futureAuthorizationGranted(Screenname screenname,
                                               String reason)
        {
            if (logger.isTraceEnabled())
                logger.trace("futureAuthorizationGranted from " + screenname);
        }

        public void youWereAdded(Screenname screenname)
        {
            if (logger.isTraceEnabled())
                logger.trace("youWereAdded from " + screenname);
        }
    }

    /**
     * Notified if buddy icon is changed
     */
    private class IconUpdateListener
        implements IconRequestListener
    {
        public void buddyIconCleared(IconService iconService,
                                     Screenname screenname,
                                     ExtraInfoData extraInfoData)
        {
            updateBuddyIcon(screenname, null);
        }

        public void buddyIconUpdated(IconService iconService,
                                     Screenname screenname,
                                     ExtraInfoData extraInfoData,
                                     ByteBlock byteBlock)
        {
            if(byteBlock != null)
            {
                updateBuddyIcon(screenname, byteBlock.toByteArray());
            }
        }

        /**
         * Changes the Contact image
         * @param screenname the contact screenname
         * @param icon byte array representing the image
         */
        private void updateBuddyIcon(Screenname screenname, byte[] icon)
        {
            ContactIcqImpl contact = ssContactList.findContactByScreenName(
                            screenname.getFormatted());

            if(contact != null)
            {
                byte[] oldIcon = contact.getImage();

                contact.setImage(icon);

                fireContactPropertyChangeEvent(
                    ContactPropertyChangeEvent.PROPERTY_IMAGE,
                    contact,
                    oldIcon,
                    icon);
            }
        }
    }

    private class AwaitingAuthorizationContactsPresenceTimer
        extends TimerTask
    {
        @Override
        public void run()
        {
            if (logger.isTraceEnabled())
                logger.trace("Running status retreiver for AwaitingAuthorizationContacts");

            Iterator<ContactGroup> groupsIter
                    = getServerStoredContactListRoot().subgroups();

            while(groupsIter.hasNext())
            {
                ContactGroup group = groupsIter.next();
                Iterator<Contact> contactsIter = group.contacts();

                while(contactsIter.hasNext())
                {
                    ContactIcqImpl sourceContact
                        = (ContactIcqImpl)contactsIter.next();

                    if(!sourceContact.getJoustSimBuddy()
                        .isAwaitingAuthorization())
                        continue;

                    String sourceContactAddress = sourceContact.getAddress();
                    PresenceStatus newStatus
                        = queryContactStatus(sourceContactAddress);
                    PresenceStatus oldStatus = sourceContact.getPresenceStatus();

                    if(newStatus.equals(oldStatus))
                       continue;

                    sourceContact.updatePresenceStatus(newStatus);

                    fireContactPresenceStatusChangeEvent(
                        sourceContact,
                        sourceContact.getParentContactGroup(),
                        oldStatus,
                        newStatus);

                    if (!newStatus.equals(IcqStatusEnum.OFFLINE)
                        && !buddiesSeenAvailable.contains(sourceContactAddress))
                    {
                        buddiesSeenAvailable.add(sourceContactAddress);
                        try
                        {
                            AuthorizationRequest req =
                                new AuthorizationRequest();
                            req.setReason("I'm resending my request. " +
                                "Please authorize me!");

                            opSetExtendedAuthorizations
                                .reRequestAuthorization(req, sourceContact);
                        } catch (OperationFailedException ex)
                        {
                            logger.error("failed to reRequestAuthorization", ex);
                        }
                    }
                }
            }
        }
    }

    /**
     * Notifies for changes in away message
     */
    private class AwayMessageListener
        implements InfoServiceListener
    {
        public void handleAwayMessage(
            InfoService service, Screenname buddy, String awayMessage)
        {
            ContactIcqImpl sourceContact
                = ssContactList.findContactByScreenName(buddy.getFormatted());

            if(sourceContact != null)
            {
                sourceContact.setStatusMessage(awayMessage);
            }
        }
        public void handleUserProfile(InfoService service, Screenname buddy,
            String userInfo){}
        public void handleCertificateInfo(InfoService service, Screenname buddy,
            BuddyCertificateInfo certInfo){}
        public void handleInvalidCertificates(InfoService service, Screenname buddy,
            CertificateInfo origCertInfo, Throwable ex){}
        public void handleDirectoryInfo(InfoService service, Screenname buddy,
            DirInfo dirInfo){}
    }
}
