/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.sip;

import java.net.*;
import java.text.*;
import java.util.*;
import java.io.*;

import javax.sip.*;
import javax.sip.address.*;
import javax.sip.header.*;
import javax.sip.message.*;
import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.*;

import org.w3c.dom.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.xml.*;

/**
 * Sip presence implementation (SIMPLE).
 * 
 * Compliant with rfc3261, rfc3265, rfc3856, rfc3863 and rfc3903
 *
 * @author Benoit Pradelle
 */
public class OperationSetPresenceSipImpl
    implements OperationSetPersistentPresence, SipListener
{
    private static final Logger logger =
        Logger.getLogger(OperationSetPersistentPresenceSipImpl.class);
    /**
     * A list of listeners registered for <tt>SubscriptionEvent</tt>s.
     */
    private Vector subscriptionListeners = new Vector();

    /**
     * A list of listeners registered for
     *  <tt>ProviderPresenceStatusChangeEvent</tt>s.
     */
    private Vector providerPresenceStatusListeners = new Vector();
    
    /**
     * A list of listeners registered for
     * <tt>ServerStoredGroupChangeEvent</tt>s.
     */
    private Vector serverStoredGroupListeners = new Vector();

    /**
     * A list of listeners registered for
     * <tt>ContactPresenceStatusChangeEvent</tt>s.
     */
    private Vector contactPresenceStatusListeners = new Vector();

    /**
     * The root of the SIP contact list.
     */
    private ContactGroupSipImpl contactListRoot = null;

    /**
     * The provider that created us.
     */
    private ProtocolProviderServiceSipImpl parentProvider = null;

    /**
     * The currently active status message.
     */
    private String statusMessage = "Default Status Message";

    /**
     * Our default presence status.
     */
    private PresenceStatus presenceStatus = SipStatusEnum.OFFLINE;

    /**
     * The <tt>AuthorizationHandler</tt> instance that we'd have to transmit
     * authorization requests to for approval.
     */
    private AuthorizationHandler authorizationHandler = null;
    
    /**
     * Hashtable which contains the contacts with which we want to subscribe
     * or with which we successfuly subscribed
     * Index : String, Content : ContactSipImpl
     */
    private Hashtable subscribedContacts = null;
    
    /**
     * List of all the contact interested by our presence status
     * Content : ContactSipImpl
     */
    private Vector ourWatchers = null;
    
    /**
     * List of all the CallIds to wait before unregister
     * Content : String
     */
    private Vector waitedCallIds = null;
    
    /**
     * Do we have to use a distant presence agent
     */
    private boolean useDistantPA = false;
    
    /**
     * Entity tag associated with the current communication with the distant PA
     */
    private String distantPAET = null;
    
    /**
     * the default expiration value of a PUBLISH request
     */
    private static final int PUBLISH_DEFAULT_EXPIRE = 600;
    
    /**
     * the default expiration value of a SUBSCRIBE request
     */
    private static final int SUBSCRIBE_DEFAULT_EXPIRE = 600;
    
    /**
     * The document builder factory for generating document builders
     */
    private DocumentBuilderFactory docBuilderFactory = null;
    
    /**
     * The document builder which produce xml documents
     */
    private DocumentBuilder docBuilder = null;
    
    /**
     * The transformer factory used to create transformer
     */
    private TransformerFactory transFactory = null;
    
    /**
     * The transformer used to convert XML documents
     */
    private Transformer transformer = null;
    
    /**
     * The id used in <tt><tuple></tt> elements of pidf documents.
     */
    private static long tupleid = (long) Math.random();
    
    // XML documents types
    private static final String PIDF_XML        = "pidf+xml";

    // pidf elements and attributes
    private static final String PRESENCE_ELEMENT= "presence";
    private static final String NS_ELEMENT      = "xmlns";
    private static final String NS_VALUE        = "urn:ietf:params:xml:ns:pidf";
    private static final String ENTITY_ATTRIBUTE= "entity";
    private static final String TUPLE_ELEMENT   = "tuple";
    private static final String ID_ATTRIBUTE    = "id";
    private static final String STATUS_ELEMENT  = "status";
    private static final String ONLINE_STATUS   = "open";
    private static final String OFFLINE_STATUS  = "closed";
    private static final String BASIC_ELEMENT   = "basic";
    private static final String CONTACT_ELEMENT = "contact";
    private static final String NOTE_ELEMENT    = "note";
    
    /**
     * Creates an instance of this operation set keeping a reference to the
     * specified parent <tt>provider</tt>.
     * @param provider the ProtocolProviderServiceSipImpl instance that
     * created us.
     */
    public OperationSetPresenceSipImpl(ProtocolProviderServiceSipImpl provider)
    {
        this.parentProvider = provider;
        this.contactListRoot = new ContactGroupSipImpl("RootGroup", provider);

        //add our registration listener
        this.parentProvider.addRegistrationStateChangeListener(
            new RegistrationListener());
        
        this.subscribedContacts = new Hashtable();
        this.ourWatchers = new Vector();
        this.waitedCallIds = new Vector();
        
        this.parentProvider.registerMethodProcessor(Request.SUBSCRIBE, this);
        this.parentProvider.registerMethodProcessor(Request.NOTIFY, this);
        this.parentProvider.registerMethodProcessor(Request.PUBLISH, this);
    }
    
    /**
     * Returns a PresenceStatus instance representing the state this provider is
     * currently in. Note that PresenceStatus instances returned by this method
     * MUST adequately represent all possible states that a provider might
     * enter duruing its lifecycle, includindg those that would not be visible
     * to others (e.g. Initializing, Connecting, etc ..) and those that will be
     * sent to contacts/buddies (On-Line, Eager to chat, etc.).
     * 
     * @return the PresenceStatus last published by this provider.
     */
    public PresenceStatus getPresenceStatus() {
        return this.presenceStatus;
    }

    /**
     * Return true if we use a distant presence agent
     * 
     * @return true if we use a distant presence agent
     */
    public boolean usesDistantPA() {
        return this.useDistantPA;
    }

    /**
     * Sets if we should use a distant presence agent
     * 
     * @param useDistPA true if we should use a distant presence agent
     */
    public void setDistantPA(boolean useDistPA) {
        this.useDistantPA = useDistPA;
    }
    
    /**
     * Notifies all registered listeners of the new event.
     *
     * @param source the contact that has caused the event.
     * @param eventID an identifier of the event to dispatch.
     */
    public void fireServerStoredGroupEvent(ContactGroupSipImpl source,
                                           int eventID)
    {
        ServerStoredGroupEvent evt  = new ServerStoredGroupEvent(
            source, eventID, source.getParentContactGroup(),
            this.parentProvider, this);

        Iterator listeners = null;
        synchronized (this.serverStoredGroupListeners)
        {
            listeners = new ArrayList(this.serverStoredGroupListeners)
                .iterator();
        }

        while (listeners.hasNext())
        {
            ServerStoredGroupListener listener
                = (ServerStoredGroupListener) listeners.next();

            if(eventID == ServerStoredGroupEvent.GROUP_CREATED_EVENT)
            {
                listener.groupCreated(evt);
            }
            else if(eventID == ServerStoredGroupEvent.GROUP_RENAMED_EVENT)
            {
                listener.groupNameChanged(evt);
            }
            else if(eventID == ServerStoredGroupEvent.GROUP_REMOVED_EVENT)
            {
                listener.groupRemoved(evt);
            }
        }
    }
    
    /**
     * Returns the root group of the server stored contact list.
     *
     * @return the root ContactGroup for the ContactList stored by this
     *   service.
     */
    public ContactGroup getServerStoredContactListRoot()
    {
        return this.contactListRoot;
    }
    
    /**
     * Creates a group with the specified name and parent in the server
     * stored contact list.
     *
     * @param parent the group where the new group should be created
     * @param groupName the name of the new group to create.
     */
    public void createServerStoredContactGroup(ContactGroup parent,
                                               String groupName)
    {
        ContactGroupSipImpl newGroup = new ContactGroupSipImpl(groupName,
                this.parentProvider);

        ((ContactGroupSipImpl) parent).addSubgroup(newGroup);

        this.fireServerStoredGroupEvent(newGroup,
                ServerStoredGroupEvent.GROUP_CREATED_EVENT);
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
     * @param persistentData a String returned ContactGroups's
     * getPersistentData() method during a previous run and that has been
     * persistently stored locally.
     * @param parentGroup the group under which the new group is to be created
     * or null if this is group directly underneath the root.
     * @return the unresolved <tt>ContactGroup</tt> created from the specified
     * <tt>uid</tt> and <tt>persistentData</tt>
     */
    public ContactGroup createUnresolvedContactGroup(String groupUID,
        String persistentData, ContactGroup parentGroup)
    {
        ContactGroupSipImpl newGroup = new ContactGroupSipImpl(
                ContactGroupSipImpl.createNameFromUID(groupUID),
                this.parentProvider);
        newGroup.setResolved(false);

        //if parent is null then we're adding under root.
        if(parentGroup == null) {
            parentGroup = getServerStoredContactListRoot();
        }

        ((ContactGroupSipImpl) parentGroup).addSubgroup(newGroup);

        this.fireServerStoredGroupEvent(
            newGroup, ServerStoredGroupEvent.GROUP_CREATED_EVENT);

        return newGroup;
    }
    
    /**
     * Renames the specified group from the server stored contact list.
     *
     * @param group the group to rename.
     * @param newName the new name of the group.
     */
    public void renameServerStoredContactGroup(ContactGroup group,
                                               String newName)
    {
        ((ContactGroupSipImpl) group).setGroupName(newName);

        this.fireServerStoredGroupEvent(
            (ContactGroupSipImpl) group,
            ServerStoredGroupEvent.GROUP_RENAMED_EVENT);
    }
    
    /**
     * Removes the specified contact from its current parent and places it
     * under <tt>newParent</tt>.
     *
     * @param contactToMove the <tt>Contact</tt> to move
     * @param newParent the <tt>ContactGroup</tt> where <tt>Contact</tt>
     *   would be placed.
     */
    public void moveContactToGroup(Contact contactToMove,
                                   ContactGroup newParent)
    {
        if (!(contactToMove instanceof ContactSipImpl)) {
            return;
        }
        
        ContactSipImpl sipContact
            = (ContactSipImpl)contactToMove;

        ContactGroupSipImpl parentSipGroup
            = (ContactGroupSipImpl) sipContact.getParentContactGroup();

        parentSipGroup.removeContact(sipContact);

        // if this is a volatile contact then we haven't really subscribed to
        // them so we'd need to do so here
        if(!sipContact.isPersistent())
        {
            //first tell everyone that the volatile contact was removed
            fireSubscriptionEvent(sipContact,
                                  parentSipGroup,
                                  SubscriptionEvent.SUBSCRIPTION_REMOVED);

            try
            {
                //now subscribe
                this.subscribe(newParent, contactToMove.getAddress());

                //now tell everyone that we've added the contact
                fireSubscriptionEvent(sipContact,
                                      newParent,
                                      SubscriptionEvent.SUBSCRIPTION_CREATED);
            }
            catch (Exception ex)
            {
                logger.error("Failed to move contact "
                             + sipContact.getAddress()
                             , ex);
            }
        }
        else
        {
            ((ContactGroupSipImpl) newParent).addContact(sipContact);

            fireSubscriptionMovedEvent(contactToMove,
                                       parentSipGroup,
                                       newParent);
        }
    }
    
    /**
     * Notifies all registered listeners of the new event.
     *
     * @param source the contact that has been moved..
     * @param oldParent the group where the contact was located before being
     * moved.
     * @param newParent the group where the contact has been moved.
     */
    public void fireSubscriptionMovedEvent(Contact      source,
                                           ContactGroup oldParent,
                                           ContactGroup newParent)
    {
        SubscriptionMovedEvent evt  = new SubscriptionMovedEvent(source,
            this.parentProvider,
            oldParent,
            newParent);

        Iterator listeners = null;
        synchronized (this.subscriptionListeners)
        {
            listeners = new ArrayList(this.subscriptionListeners).iterator();
        }

        while (listeners.hasNext())
        {
            SubscriptionListener listener
                = (SubscriptionListener) listeners.next();

            listener.subscriptionMoved(evt);
        }
    }
    
    /**
     * Removes the specified group from the server stored contact list.
     *
     * @param group the group to remove.
     *
     * @throws IllegalArgumentException if <tt>group</tt> was not found in this
     * protocol's contact list.
     */
    public void removeServerStoredContactGroup(ContactGroup group)
        throws IllegalArgumentException
    {
        ContactGroupSipImpl sipGroup = (ContactGroupSipImpl)group;

        ContactGroupSipImpl parent = this.contactListRoot
            .findGroupParent(sipGroup);

        if(parent == null){
            throw new IllegalArgumentException(
                "group " + group
                + " does not seem to belong to this protocol's contact list.");
        }

        parent.removeSubGroup(sipGroup);

        this.fireServerStoredGroupEvent(sipGroup,
                ServerStoredGroupEvent.GROUP_REMOVED_EVENT);
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
     * @param statusMsg the message that should be set as the reason to
     * enter that status
     * 
     * @throws IllegalArgumentException if the status requested is not a valid
     * PresenceStatus supported by this provider.
     * @throws java.lang.IllegalStateException if the provider is not currently
     * registered.
     * @throws OperationFailedException with code NETWORK_FAILURE if publishing
     * the status fails due to a network error.
     */
    public void publishPresenceStatus(PresenceStatus status,
            String statusMsg)
        throws IllegalArgumentException,
               IllegalStateException,
               OperationFailedException
    {
        PresenceStatus oldStatus = this.presenceStatus;
        this.presenceStatus = status;
        this.statusMessage = statusMsg;
        
        // inform the listener of our change in the status
        fireProviderStatusChangeEvent(oldStatus);
        
        // in the offline status, the protocol provider is already unregistered 
        if (!status.equals(SipStatusEnum.OFFLINE)) {
            assertConnected();
        }
        
        if (status.equals(SipStatusEnum.OFFLINE)) {
            unsubscribeToAllContact();
        }
        
        // now inform our distant presence agent if we have one
        if (this.useDistantPA) {
            Request req = createPublish(PUBLISH_DEFAULT_EXPIRE);
            
            if (status.equals(SipStatusEnum.OFFLINE)) {
                // remember the callid to be sure that the publish arrived
                // before unregister
                synchronized (this.waitedCallIds) {
                    this.waitedCallIds.add(((CallIdHeader) 
                        req.getHeader(CallIdHeader.NAME)).getCallId());
                }
            }
            
            ClientTransaction transac = null;
            try {
                transac = this.parentProvider
                    .getDefaultJainSipProvider().getNewClientTransaction(req);
            } catch (TransactionUnavailableException e) {
                logger.debug("can't create the client transaction", e);
                throw new OperationFailedException(
                        "can't create the client transaction",
                        OperationFailedException.NETWORK_FAILURE);
            }
            
            try {
                transac.sendRequest();
            } catch (SipException e) {
                logger.debug("can't send the PUBLISH request");
                throw new OperationFailedException(
                        "can't send the PUBLISH request",
                        OperationFailedException.NETWORK_FAILURE);
            }
            
        // no distant presence agent, send notify to every one
        } else {
            synchronized (this.ourWatchers) {   // avoid any modification during
                                                // the parsing of ourWatchers
                Iterator iter = this.ourWatchers.iterator();
                ContactSipImpl me = (ContactSipImpl) getLocalContact();
                
                while (iter.hasNext()) {
                    ContactSipImpl contact = (ContactSipImpl) iter.next();
                    
                    // let the subscription end before sending him a new status
                    if (!contact.isResolved()) {
                        continue;
                    }
    
                    ClientTransaction transac = null;
                    try {
                        if (status.equals(SipStatusEnum.OFFLINE)) {
                            transac = createNotify(contact,
                                    getPidfPresenceStatus(me),
                                    SubscriptionStateHeader.TERMINATED,
                                    SubscriptionStateHeader.PROBATION);
                            
                            // register the callid to wait it before unregister
                            synchronized (this.waitedCallIds) {
                                this.waitedCallIds.add(transac.getDialog()
                                    .getCallId().getCallId());
                            }
                        } else {
                            transac = createNotify(contact,
                                        getPidfPresenceStatus(me),
                                        SubscriptionStateHeader.ACTIVE, null);
                        }
                    } catch (OperationFailedException e) {
                        logger.debug("failed to create the new notify", e);
                        return;
                    }
                    
                    try {
                        contact.getServerDialog().sendRequest(transac);
                    } catch (Exception e) {
                        logger.debug("Can't send the request");
                        return;
                    }
                }
                
                if (status.equals(SipStatusEnum.OFFLINE)) {
                    synchronized (this.ourWatchers) {
                        this.ourWatchers.removeAllElements();
                    }
                }
            }
        }
    }
    
    /**
     * Notifies all registered listeners of the new event.
     *
     * @param oldValue the presence status we were in before the change.
     */
    private void fireProviderStatusChangeEvent(PresenceStatus oldValue)
    {
        ProviderPresenceStatusChangeEvent evt
            = new ProviderPresenceStatusChangeEvent(this.parentProvider,
                                        oldValue, this.getPresenceStatus());
        
        logger.debug("Dispatching Provider Status Change. Listeners="
                + this.providerPresenceStatusListeners.size()
                + " evt=" + evt);

        Iterator listeners = null;
        synchronized (this.providerPresenceStatusListeners)
        {
            listeners = new ArrayList(this.providerPresenceStatusListeners)
                .iterator();
        }

        while (listeners.hasNext())
        {
            ProviderPresenceStatusListener listener
                = (ProviderPresenceStatusListener) listeners.next();

            listener.providerStatusChanged(evt);
            logger.debug("reglistener: " + listener);
        }
        logger.debug("status dispatching done.");
    }
    
    /**
     * Create a valid PUBLISH request corresponding to the current presence
     * state. The request is forged to be send to the current distant presence
     * agent.
     * 
     * @param expires the expires value to send
     * 
     * @return a valid <tt>Request</tt> containing the PUBLISH
     * 
     * @throws OperationFailedException if something goes wrong
     */
    private Request createPublish(int expires)
        throws OperationFailedException
    {
        // Address
        InetAddress destinationInetAddress = null;
        try
        {
            destinationInetAddress = InetAddress.getByName(
                ((SipURI) this.parentProvider.getOurSipAddress().getURI())
                .getHost());
        }
        catch (UnknownHostException ex)
        {
            throw new OperationFailedException(
                ((SipURI) this.parentProvider.getOurSipAddress().getURI())
                        .getHost()
                + " is not a valid internet address " + ex.getMessage(),
                OperationFailedException.INTERNAL_ERROR);
        }

        // Call ID
        CallIdHeader callIdHeader = this.parentProvider
            .getDefaultJainSipProvider().getNewCallId();

        //FromHeader and ToHeader
        String localTag = ProtocolProviderServiceSipImpl.generateLocalTag();
        FromHeader fromHeader = null;
        ToHeader toHeader = null;
        try
        {
            //FromHeader
            fromHeader = this.parentProvider.getHeaderFactory()
                .createFromHeader(this.parentProvider.getOurSipAddress()
                                  , localTag);

            //ToHeader (it's ourselves)
            toHeader = this.parentProvider.getHeaderFactory()
                .createToHeader(this.parentProvider.getOurSipAddress(), null);
        }
        catch (ParseException ex)
        {
            //these two should never happen.
            logger.error(
                "An unexpected error occurred while"
                + "constructing the FromHeader or ToHeader", ex);
            throw new OperationFailedException(
                "An unexpected error occurred while"
                + "constructing the FromHeader or ToHeader"
                , OperationFailedException.INTERNAL_ERROR
                , ex);
        }

        //ViaHeaders
        ArrayList viaHeaders = this.parentProvider.getLocalViaHeaders(
            destinationInetAddress,
            this.parentProvider.getDefaultListeningPoint());

        //MaxForwards
        MaxForwardsHeader maxForwards = this.parentProvider
            .getMaxForwardsHeader();

        // Content params
        byte[] doc = getPidfPresenceStatus((ContactSipImpl) 
                this.getLocalContact());
        ContentTypeHeader contTypeHeader;
        ContentLengthHeader contLengthHeader;
        try
        {
            contTypeHeader = this.parentProvider.getHeaderFactory()
                .createContentTypeHeader("application",
                                         PIDF_XML);

            
            // IS IT NEEDED ?
            contLengthHeader = this.parentProvider.getHeaderFactory()
                .createContentLengthHeader(doc.length);
        }
        catch (ParseException ex)
        {
            //these two should never happen.
            logger.error(
                "An unexpected error occurred while"
                + "constructing the content headers", ex);
            throw new OperationFailedException(
                "An unexpected error occurred while"
                + "constructing the content headers"
                , OperationFailedException.INTERNAL_ERROR
                , ex);
        }
        catch (InvalidArgumentException exc)
        {
            //these two should never happen.
            logger.error(
                "An unexpected error occurred while"
                + "constructing the content length header", exc);
            throw new OperationFailedException(
                "An unexpected error occurred while"
                + "constructing the content length header"
                , OperationFailedException.INTERNAL_ERROR
                , exc);
        }
        
        // eventually add the entity tag
        SIPIfMatchHeader ifmHeader = null;
        try {
            if (this.distantPAET != null) {
                ifmHeader = this.parentProvider.getHeaderFactory()
                    .createSIPIfMatchHeader(this.distantPAET);
            }
        } catch (ParseException e) {
            logger.error(
                "An unexpected error occurred while"
                + "constructing the SIPIfMatch header", e);
            throw new OperationFailedException(
                "An unexpected error occurred while"
                + "constructing the SIPIfMatch header",
                OperationFailedException.INTERNAL_ERROR,
                e);
        }
        
        //CSeq
        CSeqHeader cSeqHeader = null;
        try
        {
            cSeqHeader = this.parentProvider.getHeaderFactory()
                .createCSeqHeader(1l, Request.PUBLISH);
        }
        catch (InvalidArgumentException ex)
        {
            //Shouldn't happen
            logger.error(
                "An unexpected error occurred while"
                + "constructing the CSeqHeader", ex);
            throw new OperationFailedException(
                "An unexpected error occurred while"
                + "constructing the CSeqHeader"
                , OperationFailedException.INTERNAL_ERROR
                , ex);
        }
        catch (ParseException ex)
        {
            //shouldn't happen
            logger.error(
                "An unexpected error occurred while"
                + "constructing the CSeqHeader", ex);
            throw new OperationFailedException(
                "An unexpected error occurred while"
                + "constructing the CSeqHeader"
                , OperationFailedException.INTERNAL_ERROR
                , ex);
        }
        
        // expires
        ExpiresHeader expHeader = null;
        try {
            expHeader = this.parentProvider.getHeaderFactory()
                .createExpiresHeader(expires);
        } catch (InvalidArgumentException e) {
            // will never happen
            logger.error(
                    "An unexpected error occurred while"
                    + "constructing the Expires header", e);
            throw new OperationFailedException(
                    "An unexpected error occurred while"
                    + "constructing the Expires header"
                    , OperationFailedException.INTERNAL_ERROR
                    , e);
        }
        
        // event
        EventHeader evtHeader = null;
        try {
            evtHeader = this.parentProvider.getHeaderFactory()
                .createEventHeader("presence");
        } catch (ParseException e) {
            // will never happen
            logger.error(
                    "An unexpected error occurred while"
                    + "constructing the Event header", e);
            throw new OperationFailedException(
                    "An unexpected error occurred while"
                    + "constructing the Event header"
                    , OperationFailedException.INTERNAL_ERROR
                    , e);
        }
        
        Request req = null;
        try
        {
            req = this.parentProvider.getMessageFactory().createRequest(
                toHeader.getAddress().getURI(),
                Request.PUBLISH,
                callIdHeader,
                cSeqHeader,
                fromHeader,
                toHeader,
                viaHeaders,
                maxForwards,
                contTypeHeader,
                doc);
        }
        catch (ParseException ex)
        {
            //shouldn't happen
            logger.error(
                "Failed to create message Request!", ex);
            throw new OperationFailedException(
                "Failed to create message Request!"
                , OperationFailedException.INTERNAL_ERROR
                , ex);
        }

        req.setHeader(contLengthHeader);
        req.setHeader(expHeader);
        req.setHeader(evtHeader);
        
        if (ifmHeader != null) {
            req.setHeader(ifmHeader);
        }

        return req;
    }

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
    public Iterator getSupportedStatusSet() {
        return SipStatusEnum.supportedStatusSet();
    }

    /**
     * Get the PresenceStatus for a particular contact. This method is not meant
     * to be used by the user interface (which would simply register as a
     * presence listener and always follow contact status) but rather by other
     * plugins that may for some reason need to know the status of a particular
     * contact.
     * <p>
     * @param contactIdentifier the identifier of the contact whose status we're
     * interested in.
     * @return PresenceStatus the <tt>PresenceStatus</tt> of the specified
     * <tt>contact</tt>
     *
     * @throws OperationFailedException with code NETWORK_FAILURE if retrieving
     * the status fails due to errors experienced during network communication
     * @throws IllegalArgumentException if <tt>contact</tt> is not a contact
     * known to the underlying protocol provider
     * @throws IllegalStateException if the underlying protocol provider is not
     * registered/signed on a public service.
     */
    public PresenceStatus queryContactStatus(String contactIdentifier)
        throws IllegalArgumentException,
               IllegalStateException,
               OperationFailedException
    {
        return resolveContactID(contactIdentifier).getPresenceStatus();
    }
    
    /**
     * Adds a subscription for the presence status of the contact corresponding
     * to the specified contactIdentifier. Note that apart from an exception in
     * the case of an immediate failure, the method won't return any indication
     * of success or failure. That would happen later on through a
     * SubscriptionEvent generated by one of the methods of the
     * SubscriptionListener.
     * We assume here that the user didn't specify any alternative presence URI
     * for this contact.
     * 
     * This subscription is not going to be persistent (as opposed to
     * subscriptions added from the OperationSetPersistentPresence.subscribe()
     * method)
     * @param contactIdentifier the identifier of the contact whose status
     * updates we are subscribing for.
     * 
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
        subscribe(this.contactListRoot, contactIdentifier);
    }

    /**
     * Adds a subscription for the presence status of the contact corresponding
     * to the specified contactIdentifier. Note that apart from an exception in
     * the case of an immediate failure, the method won't return any indication
     * of success or failure. That would happen later on through a
     * SubscriptionEvent generated by one of the methods of the
     * SubscriptionListener.
     * 
     * @param contactIdentifier the identifier of the contact whose status
     * updates we are subscribing for.
     * 
     * @throws OperationFailedException if subscribing fails due to errors
     * experienced during the contact creation
     * @throws IllegalArgumentException if <tt>contact</tt> is not a contact
     * known to the underlying protocol provider
     * @throws IllegalStateException if the underlying protocol provider is not
     * registered/signed on a public service.
     */
    public void subscribe(ContactGroup parentGroup, String contactIdentifier)
        throws IllegalArgumentException,
               IllegalStateException,
               OperationFailedException
    {
        logger.debug("let's subscribe " + contactIdentifier);
        
        //if the contact is already in the contact list and is resolved
        ContactSipImpl contact = (ContactSipImpl)
            findContactByID(contactIdentifier);
        
        if (contact != null && contact.isResolved()) {
            logger.debug("Contact " + contactIdentifier
                    + " already exists.");
            throw new OperationFailedException(
                "Contact " + contactIdentifier + " already exists.",
                OperationFailedException.SUBSCRIPTION_ALREADY_EXISTS);
        }
        
        assertConnected();
        
        // create the contact
        contact = new ContactSipImpl(contactIdentifier, this.parentProvider);

        //create the subscription
        Request subscription;
        try
        {
            subscription = createSubscription(contact,
                    SUBSCRIBE_DEFAULT_EXPIRE);
        }
        catch (OperationFailedException ex)
        {
            logger.error(
                "Failed to create the subcription"
                , ex);
            
            throw new OperationFailedException(
                    "Failed to create the subscription",
                    OperationFailedException.INTERNAL_ERROR);
        }

        //Transaction
        ClientTransaction subscribeTransaction;
        SipProvider jainSipProvider
            = this.parentProvider.getDefaultJainSipProvider();
        try
        {
            subscribeTransaction = jainSipProvider
                .getNewClientTransaction(subscription);
        }
        catch (TransactionUnavailableException ex)
        {
            logger.error(
                "Failed to create subscriptionTransaction.\n"
                + "This is most probably a network connection error."
                , ex);

            throw new OperationFailedException(
                    "Failed to create the subscription transaction",
                    OperationFailedException.NETWORK_FAILURE);
        }
        
        // we register the contact to find him when the OK will arrive
        CallIdHeader idheader = (CallIdHeader)
            subscription.getHeader(CallIdHeader.NAME);
        this.subscribedContacts.put(idheader.getCallId(), contact);
        
        // send the message
        try
        {
            subscribeTransaction.sendRequest();
        }
        catch (SipException ex)
        {
            logger.error(
                "Failed to send the message."
                , ex);
            
            // this contact will never been accepted or rejected
            this.subscribedContacts.remove(idheader.getCallId());

            throw new OperationFailedException(
                    "Failed to send the subscription",
                    OperationFailedException.NETWORK_FAILURE);
        }

        ((ContactGroupSipImpl) parentGroup).addContact(contact);
        
        // pretend that the contact is created
        fireSubscriptionEvent(contact,
                parentGroup,
                SubscriptionEvent.SUBSCRIPTION_CREATED);
    }
    
    /**
     * Creates a new SUBSCRIBE message with the provided parameters.
     * 
     * @param contact The contact concerned by this subscription
     * @param expires The expires value
     * 
     * @return a valid sip request reprensenting this message.
     * 
     * @throws OperationFailedException if the message can't be generated
     */
    private Request createSubscription(ContactSipImpl contact, int expires)
        throws OperationFailedException
    {
        // Address
        InetAddress destinationInetAddress = null;
        Address toAddress = null;
        try
        {
            toAddress = parseAddressStr(contact.getAddress());

            destinationInetAddress = InetAddress.getByName(
                ((SipURI) toAddress.getURI()).getHost());
        }
        catch (UnknownHostException ex)
        {
            throw new IllegalArgumentException(
                ((SipURI) toAddress.getURI()).getHost()
                + " is not a valid internet address " + ex.getMessage());
        }
        catch (ParseException ex)
        {
            //Shouldn't happen
            logger.error(
                "An unexpected error occurred while"
                + "constructing the address", ex);
            throw new OperationFailedException(
                "An unexpected error occurred while"
                + "constructing the address"
                , OperationFailedException.INTERNAL_ERROR
                , ex);
        }
        
        Request req;
        // Call ID
        CallIdHeader callIdHeader = this.parentProvider
            .getDefaultJainSipProvider().getNewCallId();
    
        //CSeq
        CSeqHeader cSeqHeader = null;
        try
        {
            cSeqHeader = this.parentProvider.getHeaderFactory()
                .createCSeqHeader(1l, Request.SUBSCRIBE);
        }
        catch (InvalidArgumentException ex)
        {
            //Shouldn't happen
            logger.error(
                "An unexpected error occurred while"
                + "constructing the CSeqHeader", ex);
            throw new OperationFailedException(
                "An unexpected error occurred while"
                + "constructing the CSeqHeader"
                , OperationFailedException.INTERNAL_ERROR
                , ex);
        }
        catch (ParseException ex)
        {
            //shouldn't happen
            logger.error(
                "An unexpected error occurred while"
                + "constructing the CSeqHeader", ex);
            throw new OperationFailedException(
                "An unexpected error occurred while"
                + "constructing the CSeqHeader"
                , OperationFailedException.INTERNAL_ERROR
                , ex);
        }
    
        //FromHeader and ToHeader
        String localTag = ProtocolProviderServiceSipImpl.generateLocalTag();
        FromHeader fromHeader = null;
        ToHeader toHeader = null;
        try
        {
            //FromHeader
            fromHeader = this.parentProvider.getHeaderFactory()
                .createFromHeader(this.parentProvider.getOurSipAddress()
                                  , localTag);

            //ToHeader
            toHeader = this.parentProvider.getHeaderFactory()
                .createToHeader(toAddress, null);
        }
        catch (ParseException ex)
        {
            //these two should never happen.
            logger.error(
                "An unexpected error occurred while"
                + "constructing the FromHeader or ToHeader", ex);
            throw new OperationFailedException(
                "An unexpected error occurred while"
                + "constructing the FromHeader or ToHeader"
                , OperationFailedException.INTERNAL_ERROR
                , ex);
        }
    
        //ViaHeaders
        ArrayList viaHeaders = this.parentProvider.getLocalViaHeaders(
            destinationInetAddress,
            this.parentProvider.getDefaultListeningPoint());

        //MaxForwards
        MaxForwardsHeader maxForwards = this.parentProvider
            .getMaxForwardsHeader();

        try
        {
            req = this.parentProvider.getMessageFactory().createRequest(
                toHeader.getAddress().getURI(),
                Request.SUBSCRIBE,
                callIdHeader,
                cSeqHeader,
                fromHeader,
                toHeader,
                viaHeaders,
                maxForwards);
        }
        catch (ParseException ex)
        {
            //shouldn't happen
            logger.error(
                "Failed to create message Request!", ex);
            throw new OperationFailedException(
                "Failed to create message Request!"
                , OperationFailedException.INTERNAL_ERROR
                , ex);
        }
        
        // Event
        EventHeader evHeader = null;
        try {
            evHeader = this.parentProvider.getHeaderFactory()
                .createEventHeader("presence");
        } catch (ParseException e) {
            //these two should never happen.
            logger.error(
                "An unexpected error occurred while"
                + "constructing the EventHeader", e);
            throw new OperationFailedException(
                "An unexpected error occurred while"
                + "constructing the EventHeader"
                , OperationFailedException.INTERNAL_ERROR
                , e);
        }
        
        // Contact
        ContactHeader contactHeader = this.parentProvider.getContactHeader();

        req.setHeader(evHeader);
        req.setHeader(contactHeader);
        
        // Accept
        AcceptHeader accept = null;
        try {
            accept = this.parentProvider.getHeaderFactory()
                .createAcceptHeader("application", PIDF_XML);
        } catch (ParseException e) {
            logger.error("wrong accept header");
            throw new OperationFailedException(
                    "An unexpected error occurred while"
                    + "constructing the AcceptHeader",
                    OperationFailedException.INTERNAL_ERROR,
                    e);
        }
        req.setHeader(accept);

        // Expires
        ExpiresHeader expHeader = null;
        try {
            expHeader = this.parentProvider.getHeaderFactory()
                .createExpiresHeader(expires);
        } catch (InvalidArgumentException e) {
            logger.debug("Invalid expires value: "  + expires, e);
            throw new OperationFailedException(
                    "An unexpected error occurred while"
                    + "constructing the ExpiresHeader",
                    OperationFailedException.INTERNAL_ERROR,
                    e);
        }
        
        req.setHeader(expHeader);
        
        return req;
    }
    
    /**
     * Creates a new SUBSCRIBE message with the provided parameters.
     * 
     * @param contact The contact concerned by this subscription
     * @param expires The expires value
     * @param dialog The dialog with which this request should be associated
     * or null if this request has to create a new dialog
     * 
     * @return a <tt>ClientTransaction</tt> which may be used with 
     * <tt>dialog.sendRequest(ClientTransaction)</tt> for send the request. 
     * 
     * @throws OperationFailedException if the message can't be generated
     */
    private ClientTransaction createSubscription(int expires, Dialog dialog)
        throws OperationFailedException
    {
        Request req = null;
        try {
            req = dialog.createRequest(Request.SUBSCRIBE);
        } catch (SipException e) {
            logger.debug("Can't create the SUBSCRIBE message");
            throw new OperationFailedException(
                    "An unexpected error occurred while"
                    + "constructing the SUBSCRIBE request"
                    , OperationFailedException.INTERNAL_ERROR
                    , e);
        }
        
        // Address
        InetAddress destinationInetAddress = null;
        Address toAddress = dialog.getRemoteTarget();
        
        // no Contact field
        if (toAddress == null) {
            toAddress = dialog.getRemoteParty();
        }
        
        try
        {
           destinationInetAddress = InetAddress.getByName(
                ((SipURI) toAddress.getURI()).getHost());
        }
        catch (UnknownHostException ex)
        {
            throw new IllegalArgumentException(
                ((SipURI) toAddress.getURI()).getHost()
                + " is not a valid internet address " + ex.getMessage());
        }

        //MaxForwards
        MaxForwardsHeader maxForwards = this.parentProvider
            .getMaxForwardsHeader();
        
        // EventHeader
        EventHeader evHeader = null;
        try {
            evHeader = this.parentProvider.getHeaderFactory()
                .createEventHeader("presence");
        } catch (ParseException e) {
            //these two should never happen.
            logger.error(
                "An unexpected error occurred while"
                + "constructing the EventHeader", e);
            throw new OperationFailedException(
                "An unexpected error occurred while"
                + "constructing the EventHeader"
                , OperationFailedException.INTERNAL_ERROR
                , e);
        }
        
        // Contact
        ContactHeader contactHeader = this.parentProvider
            .getContactHeader();
        
        // Accept
        AcceptHeader accept = null;
        try {
            accept = this.parentProvider.getHeaderFactory()
                .createAcceptHeader("application", PIDF_XML);
        } catch (ParseException e) {
            logger.error("wrong accept header");
            throw new OperationFailedException(
                    "An unexpected error occurred while"
                    + "constructing the AcceptHeader",
                    OperationFailedException.INTERNAL_ERROR,
                    e);
        }

        // Expires
        ExpiresHeader expHeader = null;
        try {
            expHeader = this.parentProvider.getHeaderFactory()
                .createExpiresHeader(expires);
        } catch (InvalidArgumentException e) {
            logger.debug("Invalid expires value: "  + expires, e);
            throw new OperationFailedException(
                    "An unexpected error occurred while"
                    + "constructing the ExpiresHeader",
                    OperationFailedException.INTERNAL_ERROR,
                    e);
        }
        
        req.setHeader(expHeader);
        req.setHeader(accept);
        req.setHeader(maxForwards);
        req.setHeader(evHeader);
        req.setHeader(contactHeader);
        
        // create the transaction (then add the via header as recommended
        // by the jain-sip documentation at:
        // http://snad.ncsl.nist.gov/proj/iptel/jain-sip-1.2
        // /javadoc/javax/sip/Dialog.html#createRequest(java.lang.String)
        ClientTransaction transac = null;
        try
        {
            transac = this.parentProvider.getDefaultJainSipProvider()
                .getNewClientTransaction(req);
        }
        catch (TransactionUnavailableException ex)
        {
            logger.error(
                "Failed to create subscriptionTransaction.\n"
                + "This is most probably a network connection error."
                , ex);

            throw new OperationFailedException(
                    "Failed to create the subscription transaction",
                    OperationFailedException.NETWORK_FAILURE);
        }
        
        //ViaHeaders
        ArrayList viaHeaders = this.parentProvider.getLocalViaHeaders(
            destinationInetAddress
            , this.parentProvider.getDefaultListeningPoint());
        req.addHeader((Header) viaHeaders.get(0));
        
        return transac;
    }
    
    /**
     * Parses the the <tt>uriStr</tt> string and returns a JAIN SIP URI.
     *
     * @param uriStr a <tt>String</tt> containing the uri to parse.
     *
     * @return a URI object corresponding to the <tt>uriStr</tt> string.
     * @throws ParseException if uriStr is not properly formatted.
     */
    private Address parseAddressStr(String uriStr)
        throws ParseException
    {
        String res = uriStr.trim();

        //Handle default domain name (i.e. transform 1234 -> 1234@sip.com)
        //assuming that if no domain name is specified then it should be the
        //same as ours.
        if (res.indexOf('@') == -1)
        {
            res = res + '@'
                + ((SipURI) this.parentProvider.getOurSipAddress().getURI())
                .getHost();
        }

        //Let's be uri fault tolerant and add the sip: scheme if there is none.
        if (!res.toLowerCase().startsWith("sip:") //no sip scheme
            && !res.toLowerCase().startsWith("pres:"))
        {
            res = "sip:" + res;    //most probably a sip uri
        }

        //Request URI
        Address uri
            = this.parentProvider.getAddressFactory().createAddress(res);

        return uri;
    }
    
    /**
     * Utility method throwing an exception if the stack is not properly
     * initialized.
     * @throws java.lang.IllegalStateException if the underlying stack is
     * not registered and initialized.
     */
    private void assertConnected()
        throws IllegalStateException
    {
        if (this.parentProvider == null)
            throw new IllegalStateException(
                "The provider must be non-null and signed on the "
                + "service before being able to communicate.");
        if (!this.parentProvider.isRegistered())
            throw new IllegalStateException(
                "The provider must be signed on the service before "
                + "being able to communicate.");
    }

    /**
     * Removes a subscription for the presence status of the specified contact.
     * @param contact the contact whose status updates we are unsubscribing
     * from.
     *
     * @throws OperationFailedException with code NETWORK_FAILURE if
     * unsubscribing fails due to errors experienced during network
     * communication
     * @throws IllegalArgumentException if <tt>contact</tt> is not a contact
     * known to the underlying protocol provider
     * @throws IllegalStateException if the underlying protocol provider is not
     * registered/signed on a public service.
     */
    public void unsubscribe(Contact contact)
        throws IllegalArgumentException,
               IllegalStateException,
               OperationFailedException
   {
        if (!(contact instanceof ContactSipImpl)) {
            throw new IllegalArgumentException("the contact is not a SIP" +
                    " contact");
        }
        
        ContactSipImpl sipcontact = (ContactSipImpl) contact;
        
        // handle the case of a distant presence agent is used
        if (this.useDistantPA) {
            // simply send a notify with an expire to 0
            Request req = createPublish(0);
            
            ClientTransaction transac = null;
            try {
                transac = this.parentProvider
                    .getDefaultJainSipProvider().getNewClientTransaction(req);
            } catch (TransactionUnavailableException e) {
                logger.debug("can't create the client transaction", e);
                throw new OperationFailedException(
                        "can't create the client transaction",
                        OperationFailedException.NETWORK_FAILURE);
            }
            
            try {
                transac.sendRequest();
            } catch (SipException e) {
                logger.debug("can't send the PUBLISH request");
                throw new OperationFailedException(
                        "can't send the PUBLISH request",
                        OperationFailedException.NETWORK_FAILURE);
            }
            
            this.distantPAET = null;
            return;
        }

        Dialog dialog = sipcontact.getClientDialog();
        
        // check if we heard about this contact
        if (this.subscribedContacts.get(dialog.getCallId().getCallId())
                == null)
        {
            throw new IllegalArgumentException("trying to unregister a not " +
                    "registered contact");
        }
        
        // we stop the subscribtion if we're subscribed to this contact
        if (!contact.getPresenceStatus().equals(SipStatusEnum.OFFLINE)
                && !contact.getPresenceStatus().equals(SipStatusEnum.UNKNOWN)
                && sipcontact.isResolvable())
        {
            assertConnected();
            
            ClientTransaction transac = null;
            try {
                transac = createSubscription(0, dialog);
            } catch (OperationFailedException e) {
                logger.debug("failed to create the unsubscription", e);
                throw e;
            }
            
            try {
                dialog.sendRequest(transac);
            } catch (Exception e) {
                logger.debug("Can't send the request");
                throw new OperationFailedException(
                        "Failed to send the subscription message",
                        OperationFailedException.NETWORK_FAILURE);
            }
        }
        
        // remove any trace of this contact
        terminateSubscription(sipcontact);
        this.subscribedContacts.remove(dialog.getCallId().getCallId());
        ((ContactGroupSipImpl) sipcontact.getParentContactGroup())
            .removeContact(sipcontact);
        
        // inform the listeners
        fireSubscriptionEvent(sipcontact,
              sipcontact.getParentContactGroup(),
              SubscriptionEvent.SUBSCRIPTION_REMOVED);
   }
    
    /**
     * Analyzes the incoming <tt>responseEvent</tt> and then forwards it to the
     * proper event handler.
     *
     * @param responseEvent the responseEvent that we received
     * ProtocolProviderService.
     */
    public void processResponse(ResponseEvent responseEvent)
    {
        ClientTransaction clientTransaction = responseEvent
            .getClientTransaction();
        Response response = responseEvent.getResponse();
        
        CSeqHeader cseq = ((CSeqHeader)response.getHeader(CSeqHeader.NAME));
        if (cseq == null)
        {
            logger.error("An incoming response did not contain a CSeq header");
            return;
        }
        String method = cseq.getMethod();
            
        SipProvider sourceProvider = (SipProvider)responseEvent.getSource();
        
        // SUBSCRIBE
        if (method.equals(Request.SUBSCRIBE)) {
            // find the contact
            CallIdHeader idheader = (CallIdHeader)
                response.getHeader(CallIdHeader.NAME);
            ContactSipImpl contact = (ContactSipImpl) this.subscribedContacts
                .get(idheader.getCallId());
            
            // if it's the response to an unsubscribe message, we just ignore it
            // whatever the response is however if we need to handle a 
            // challenge, we do it 
            ExpiresHeader expHeader = (ExpiresHeader) 
                response.getHeader(ExpiresHeader.NAME);
            if ((expHeader != null && expHeader.getExpires() == 0)
                    || contact == null)
            {
                if (response.getStatusCode() == Response.UNAUTHORIZED
                        || response.getStatusCode() == 
                            Response.PROXY_AUTHENTICATION_REQUIRED)
                {
                    try {
                        processAuthenticationChallenge(clientTransaction,
                                response, sourceProvider);
                    } catch (OperationFailedException e) {
                        logger.error("can't handle the challenge");
                    }
                } else  if (response.getStatusCode() != Response.OK
                        && response.getStatusCode() != Response.ACCEPTED)
                {
                    // this definitivly ends the subscription
                    synchronized (this.waitedCallIds) {
                        this.waitedCallIds.remove(idheader.getCallId());
                    }
                }
                // any other case (200/202) will imply a NOTIFY, so we will
                // handle the end of a subscription there

                return;
            }
            
            try {
                finalizeSubscription(contact,
                        clientTransaction.getDialog());
            } catch (NullPointerException e) {
                // should not happen
                logger.debug("failed to finalize the subscription of the" +
                        "contact", e);
                
                return;
            }
            
            // OK (200/202)
            if (response.getStatusCode() == Response.OK
                || response.getStatusCode() == Response.ACCEPTED)
            {
                // just wait the notify which will set the contact status
            // UNAUTHORIZED (401/407)
            } else if (response.getStatusCode() == Response.UNAUTHORIZED
                    || response.getStatusCode() == Response
                        .PROXY_AUTHENTICATION_REQUIRED)
            {
                try {
                    processAuthenticationChallenge(clientTransaction,
                            response, sourceProvider);
                } catch (OperationFailedException e) {
                    logger.error("can't handle the challenge");

                    // we probably won't be able to communicate with the contact
                    changePresenceStatusForContact(contact,
                            SipStatusEnum.UNKNOWN);
                }
            // 408 480 486 600 603 : non definitive reject
            } else if (response.getStatusCode() == Response.REQUEST_TIMEOUT
                    || response.getStatusCode() == Response
                        .TEMPORARILY_UNAVAILABLE
                    || response.getStatusCode() == Response.BUSY_HERE
                    || response.getStatusCode() == Response.BUSY_EVERYWHERE
                    || response.getStatusCode() == Response.DECLINE)
            {
                logger.debug("error received from the network" + response);
                
                if (response.getStatusCode() == Response
                        .TEMPORARILY_UNAVAILABLE)
                {
                    changePresenceStatusForContact(contact,
                            SipStatusEnum.OFFLINE);
                } else {
                    changePresenceStatusForContact(contact,
                            SipStatusEnum.UNKNOWN);
                }
            // definitive reject (or not implemented)
            } else {
                logger.debug("error received from the network" + response);
                
                // we'll never be able to resolve this contact
                contact.setResolvable(false);
                changePresenceStatusForContact(contact, SipStatusEnum.UNKNOWN);
            }
            
        // NOTIFY
        } else if (method.equals(Request.NOTIFY)) {
            // if it's a final response to a NOTIFY, we try to remove it from
            // the list of waited NOTIFY end
            if (response.getStatusCode() != Response.UNAUTHORIZED
                && response.getStatusCode() != Response
                    .PROXY_AUTHENTICATION_REQUIRED)
            {
                synchronized (this.waitedCallIds) {
                    this.waitedCallIds.remove(((CallIdHeader) response
                        .getHeader(CallIdHeader.NAME)).getCallId());
                }
            }
            
            // OK (200)
            if (response.getStatusCode() == Response.OK) {
                // simply nothing to do here, the contact received our NOTIFY,
                // everything is ok
            // UNAUTHORIZED (401/407)
            } else if (response.getStatusCode() == Response.UNAUTHORIZED
                    || response.getStatusCode() == Response
                        .PROXY_AUTHENTICATION_REQUIRED)
            {
                try {
                    processAuthenticationChallenge(clientTransaction,
                            response, sourceProvider);
                } catch (OperationFailedException e) {
                    logger.error("can't handle the challenge");
                    
                    // don't try to tell him anything more
                    String contactAddress = ((FromHeader) 
                            response.getHeader(FromHeader.NAME)).getAddress()
                            .getURI().toString();
                    Contact watcher = getWatcher(contactAddress);
                        
                    if (watcher != null) {
                        synchronized (this.ourWatchers) {
                            this.ourWatchers.remove(watcher);
                        }
                    }
                }
            // every error cause the subscription to be removed
            // as recommended for some cases in rfc3265
            } else {
                logger.debug("error received from the network" + response);
                
                String contactAddress = ((FromHeader) 
                        response.getHeader(FromHeader.NAME)).getAddress()
                        .getURI().toString();
                Contact watcher = getWatcher(contactAddress);
                    
                if (watcher != null) {
                    synchronized (this.ourWatchers) {
                        this.ourWatchers.remove(watcher);
                    }
                }
            }
            
        // PUBLISH 
        } else if (method.equals(Request.PUBLISH)) {
            // if it's a final response to a PUBLISH, we try to remove it from
            // the list of waited PUBLISH end
            if (response.getStatusCode() != Response.UNAUTHORIZED
                && response.getStatusCode() != Response
                    .PROXY_AUTHENTICATION_REQUIRED)
            {
                synchronized (this.waitedCallIds) {
                    this.waitedCallIds.remove(((CallIdHeader) response
                        .getHeader(CallIdHeader.NAME)).getCallId());
                }
            }
            
            // OK (200)
            if (response.getStatusCode() == Response.OK) {
                // remember the entity tag
                SIPETagHeader etHeader = (SIPETagHeader) 
                    response.getHeader(SIPETagHeader.NAME);
                
                if (etHeader == null) {
                    logger.debug("can't find the ETag header");
                    return;
                }
                
                this.distantPAET = etHeader.getETag();
                
            // UNAUTHORIZED (401/407)
            } else if (response.getStatusCode() == Response.UNAUTHORIZED
                    || response.getStatusCode() == Response
                        .PROXY_AUTHENTICATION_REQUIRED)
            {
                try {
                    processAuthenticationChallenge(clientTransaction,
                            response, sourceProvider);
                } catch (OperationFailedException e) {
                    logger.error("can't handle the challenge");
                    return;
                }
            // with every other error, we consider that we have to start a new
            // communication
            } else {
                logger.debug("error received from the network" + response);
                this.distantPAET = null;
            }
        }
    }
    
    /**
     * Finalize the subscription of a contact and transform the pending contact
     * into a real contact.
     * 
     * @param contact the contact concerned
     * @param dialog the dialog which will be used to communicate with this
     * contact for retrieving its status
     * 
     * @throws NullPointerException if dialog or contact is null
     */
    private void finalizeSubscription(ContactSipImpl contact, Dialog dialog)
        throws NullPointerException
    {
        // remember the dialog created to be able to send SUBSCRIBE
        // refresh and to unsibscribe
        if (dialog == null) {
            throw new NullPointerException("null dialog associated with a " +
                    "contact: " + contact);
        }
        if (contact == null) {
            throw new NullPointerException("null contact");
        }
        
        // set the contact client dialog
        contact.setClientDialog(dialog);
        
        contact.setResolved(true);
        
        // inform the listeners that the contact is created
        this.fireSubscriptionEvent(contact,
                contact.getParentContactGroup(),
                SubscriptionEvent.SUBSCRIPTION_RESOLVED);
        
        logger.debug("contact : " + contact + " resolved");
    }
    
    /**
     * Terminate the subscription to a contact presence status
     * 
     * @param contact the contact concerned
     */
    private void terminateSubscription(ContactSipImpl contact) {
        if (contact == null) {
            logger.debug("null contact provided, can't terminate" +
                    " subscription");
            return;
        }
        
        contact.setClientDialog(null);
        
        // we don't remove the contact as it may just be a network problem
        changePresenceStatusForContact(contact, SipStatusEnum.UNKNOWN);
        contact.setResolved(false);
    }
    
    /**
     * Creates a NOTIFY request corresponding to the provided arguments.
     * This request MUST be sent using <tt>dialog.sendRequest</tt>
     * 
     * @param contact The contact to notify
     * @param doc The presence document to send
     * @param subscriptionState The current subscription state
     * @param reason The reason of this subscription state (may be null)
     * 
     * @return a valid <tt>ClientTransaction</tt> ready to send the request
     * 
     * @throws OperationFailedException if something goes wrong during the
     * creation of the request
     */
    private ClientTransaction createNotify(ContactSipImpl contact, byte[] doc,
            String subscriptionState, String reason)
    throws OperationFailedException
    {
        Dialog dialog = contact.getServerDialog();
        
        if (dialog == null) {
            throw new OperationFailedException("the server dialog of the " +
                    "contact is null", OperationFailedException.INTERNAL_ERROR);
        }
        
        Request req = null;
        try {
            req = dialog.createRequest(Request.NOTIFY);
        } catch (SipException e) {
            logger.debug("Can't create the NOTIFY message");
            throw new OperationFailedException("Can't create the NOTIFY" +
                    " message", OperationFailedException.INTERNAL_ERROR, e);
        }
        
        // Address
        InetAddress destinationInetAddress = null;
        Address toAddress = dialog.getRemoteTarget();
        
        // no Contact field
        if (toAddress == null) {
            toAddress = dialog.getRemoteParty();
        }
        
        try
        {
           destinationInetAddress = InetAddress.getByName(
                ((SipURI) toAddress.getURI()).getHost());
        }
        catch (UnknownHostException ex)
        {
            throw new OperationFailedException(
                ((SipURI) toAddress.getURI()).getHost()
                + " is not a valid internet address ",
                OperationFailedException.INTERNAL_ERROR, ex);
        }
        
        ArrayList viaHeaders = null;
        MaxForwardsHeader maxForwards = null;
        
        try {
            //ViaHeaders
            viaHeaders = this.parentProvider.getLocalViaHeaders(
                destinationInetAddress
                , this.parentProvider.getDefaultListeningPoint());

            //MaxForwards
            maxForwards = this.parentProvider
                .getMaxForwardsHeader();
        } catch (OperationFailedException e) {
            logger.debug("cant retrive the via headers or the max forward",
                    e);
            throw new OperationFailedException("Can't create the NOTIFY" +
                    " message", OperationFailedException.INTERNAL_ERROR);
        }
        
        EventHeader evHeader = null;
        try {
            evHeader = this.parentProvider.getHeaderFactory()
                .createEventHeader("presence");
        } catch (ParseException e) {
            //these two should never happen.
            logger.error(
                "An unexpected error occurred while"
                + "constructing the EventHeader", e);
            throw new OperationFailedException("Can't create the Event" +
                    " header", OperationFailedException.INTERNAL_ERROR, e);
        }
        
        // Contact
        ContactHeader contactHeader = this.parentProvider
            .getContactHeader();
        
        // Subscription-State
        SubscriptionStateHeader sStateHeader = null;
        try {
            sStateHeader = this.parentProvider
                .getHeaderFactory().createSubscriptionStateHeader(
                        subscriptionState);
            
            if (reason != null && !reason.trim().equals("")) {
                sStateHeader.setReasonCode(reason);
            }
        } catch (ParseException e) {
            // should never happen
            logger.debug("can't create the Subscription-State header", e);
            throw new OperationFailedException("Can't create the " +
                    "Subscription-State header",
                    OperationFailedException.INTERNAL_ERROR, e);
        }
        
        // Content-type
        ContentTypeHeader cTypeHeader = null;
        try {
            cTypeHeader = this.parentProvider
                .getHeaderFactory().createContentTypeHeader("application",
                    PIDF_XML);
        } catch (ParseException e) {
            // should never happen
            logger.debug("can't create the Content-Type header", e);
            throw new OperationFailedException("Can't create the " +
                    "Content-type header",
                    OperationFailedException.INTERNAL_ERROR, e);
        }
        
        req.setHeader(maxForwards);
        req.setHeader(evHeader);
        req.setHeader(sStateHeader);
        req.setHeader(contactHeader);
        
        // create the transaction (then add the via header as recommended
        // by the jain-sip documentation at:
        // http://snad.ncsl.nist.gov/proj/iptel/jain-sip-1.2
        // /javadoc/javax/sip/Dialog.html#createRequest(java.lang.String)
        ClientTransaction transac = null;
        try
        {
            transac = this.parentProvider.getDefaultJainSipProvider()
                .getNewClientTransaction(req);
        }
        catch (TransactionUnavailableException ex)
        {
            logger.error(
                "Failed to create subscriptionTransaction.\n"
                + "This is most probably a network connection error."
                , ex);

            throw new OperationFailedException("Can't create the " +
                    "Content-length header",
                    OperationFailedException.NETWORK_FAILURE, ex);
        }
        
        req.addHeader((Header) viaHeaders.get(0));
        
        // add the content
        try {
            req.setContent(doc, cTypeHeader);
        } catch (ParseException e) {
            logger.debug("Failed to add the presence document", e);
            throw new OperationFailedException("Can't add the presence " +
                    "document to the request",
                    OperationFailedException.INTERNAL_ERROR, e);
        }
        
        return transac;
    }
    
    /**
     * Process a request from a distant contact
     *
     * @param requestEvent the <tt>RequestEvent</tt> containing the newly
     * received request.
     */
    public void processRequest(RequestEvent requestEvent)
    {
        ServerTransaction serverTransaction = requestEvent
            .getServerTransaction();
        SipProvider jainSipProvider = (SipProvider) requestEvent.getSource();
        Request request = requestEvent.getRequest();
    
        if (serverTransaction == null)
        {
            try
            {
                serverTransaction = jainSipProvider.getNewServerTransaction(
                    request);
            }
            catch (TransactionAlreadyExistsException ex)
            {
                //let's not scare the user and only log a message
                logger.error("Failed to create a new server"
                    + "transaction for an incoming request\n"
                    + "(Next message contains the request)"
                    , ex);
                return;
            }
            catch (TransactionUnavailableException ex)
            {
                //let's not scare the user and only log a message
                logger.error("Failed to create a new server"
                    + "transaction for an incoming request\n"
                    + "(Next message contains the request)"
                    , ex);
                    return;
            }
        }
        
        EventHeader eventHeader = (EventHeader) 
            request.getHeader(EventHeader.NAME);
        
        if (eventHeader == null || !eventHeader.getEventType()
                .equalsIgnoreCase("presence"))
        {
            // we are not concerned by this request, perhaps another
            // listener is ?
            return;
        }
        
        
        // NOTIFY
        if (request.getMethod().equals(Request.NOTIFY)) {
            Response response = null;
            
            logger.debug("notify received");
            
            SubscriptionStateHeader sstateHeader = (SubscriptionStateHeader)
                request.getHeader(SubscriptionStateHeader.NAME);
            
            // notify must contain one (rfc3265)
            if (sstateHeader == null) {
                logger.error("no subscription state in this request");
                return;
            }
            
            // first try to accept the contact if the contact is pending
            // it's possible if the NOTIFY arrives before the OK
            CallIdHeader idheader = (CallIdHeader) request.getHeader(
                    CallIdHeader.NAME);
            ContactSipImpl contact = (ContactSipImpl) this.subscribedContacts
                .get(idheader.getCallId());
            
            if (contact != null && !sstateHeader.getState().equalsIgnoreCase(
                    SubscriptionStateHeader.TERMINATED) && !contact
                    .isResolved())
            {
                logger.debug("contact still pending while NOTIFY received");
                try {
                    finalizeSubscription(contact,
                            serverTransaction.getDialog());
                } catch (NullPointerException e) {
                    logger.debug("failed to finalize the subscription of the" +
                            "contact", e);
                    return;
                }
            }
            
            // see if the notify correspond to an existing subscription
            if (contact == null) {
                logger.debug("contact not found for callid : " + 
                        idheader.getCallId());
                
                // try to remove the callid from the list if we were excpeting
                // this end (if it's the last notify of a subscription we just
                // stopped
                synchronized (this.waitedCallIds) {
                    this.waitedCallIds.remove(idheader.getCallId());
                }
                
                // send a 481 response (rfc3625)
                try {
                    response = this.parentProvider.getMessageFactory()
                        .createResponse(
                            Response.CALL_OR_TRANSACTION_DOES_NOT_EXIST,
                            request);
                } catch (ParseException e) {
                    logger.debug("failed to create the 481 response", e);
                    return;
                }
                
                try {
                    serverTransaction.sendResponse(response);
                } catch (SipException e) {
                    logger.debug("failed to send the response", e);
                } catch (InvalidArgumentException e) {
                    // should not happen
                    logger.debug("invalid argument provided while trying" +
                            " to send the response", e);
                }
                
                return;
            }
            
            // if we don't understand the content
            ContentTypeHeader ctheader = (ContentTypeHeader) request
                .getHeader(ContentTypeHeader.NAME);
            
            if (ctheader != null && !ctheader.getContentSubType()
                    .equalsIgnoreCase(PIDF_XML))
            {
                // send a 415 response (rfc3261)
                try {
                    response = this.parentProvider.getMessageFactory()
                        .createResponse(Response.UNSUPPORTED_MEDIA_TYPE,
                                request);
                } catch (ParseException e) {
                    logger.debug("failed to create the OK response", e);
                    return;
                }
                
                // we want PIDF
                AcceptHeader acceptHeader = null;
                try {
                    acceptHeader = this.parentProvider
                        .getHeaderFactory().createAcceptHeader(
                            "application", PIDF_XML);
                } catch (ParseException e) {
                    // should not happen
                    logger.debug("failed to create the accept header", e);
                    return;
                }
                response.setHeader(acceptHeader);
                
                try {
                    serverTransaction.sendResponse(response);
                } catch (SipException e) {
                    logger.debug("failed to send the response", e);
                } catch (InvalidArgumentException e) {
                    // should not happen
                    logger.debug("invalid argument provided while trying" +
                            " to send the response", e);
                }
            }
            
            // send an OK response
            try {
                response = this.parentProvider.getMessageFactory()
                    .createResponse(Response.OK, request);
            } catch (ParseException e) {
                logger.debug("failed to create the OK response", e);
                return;
            }
            
            try {
                serverTransaction.sendResponse(response);
            } catch (SipException e) {
                logger.debug("failed to send the response", e);
            } catch (InvalidArgumentException e) {
                // should not happen
                logger.debug("invalid argument provided while trying" +
                        " to send the response", e);
            }
            
            // if the presentity doesn't want of us anymore
            if (sstateHeader.getState().equalsIgnoreCase(
                    SubscriptionStateHeader.TERMINATED))
            {
                terminateSubscription(contact);
                this.subscribedContacts.remove(serverTransaction.getDialog()
                        .getCallId().getCallId());
                
                // try to remove the callid from the list if we were excpeting
                // this end (if it's the last notify of a subscription we just
                // stopped
                synchronized (this.waitedCallIds) {
                    this.waitedCallIds.remove(idheader.getCallId());
                }
            }
            // transform the presence document in new presence status
            if (request.getRawContent() != null) {
                setPidfPresenceStatus(new String(request.getRawContent()));
            }
            
        // SUBSCRIBE
        } else if (request.getMethod().equals(Request.SUBSCRIBE)) {
            FromHeader from = (FromHeader) request.getHeader(FromHeader.NAME);
                        
            // try to find which contact is concerned
            ContactSipImpl contact = (ContactSipImpl) resolveContactID(from
                    .getAddress().getURI().toString());
            
            // if we don't know him, create him
            if (contact == null) {
                contact = new ContactSipImpl(from.getAddress().getURI()
                        .toString(), this.parentProvider);
                
                // <tricky time>
                // this ensure that we will publish our status to this contact
                // without trying to subscribe to him
                contact.setResolved(true);
                contact.setResolvable(false);
                // </tricky time>
            }
            
            logger.debug(contact.toString() + " wants to watch your presence " +
                    "status");
            
            // remember the dialog we will use to send the NOTIFYs
            contact.setServerDialog(serverTransaction.getDialog());
            
            Dialog dialog = contact.getServerDialog();
            
            // is it a subscription end ?
            ExpiresHeader expHeader = (ExpiresHeader)
                request.getHeader(ExpiresHeader.NAME);
            if (expHeader != null && expHeader.getExpires() == 0) {
                logger.debug("contact " + contact + " isn't a watcher anymore");
                
                // remove the contact from our watcher
                synchronized (this.ourWatchers) {
                    this.ourWatchers.remove(contact);
                }
                
                // send him a OK
                Response response = null;
                try {
                    response = this.parentProvider.getMessageFactory()
                        .createResponse(Response.OK, request);
                } catch (Exception e) {
                    logger.debug("Error while creating the response 202", e);
                    return;
                }
                
                try {
                    serverTransaction.sendResponse(response);
                } catch (Exception e) {
                    logger.error("Error while sending the response 202", e);
                    return;
                }
                
                // then terminate the subscription with an ultimate NOTIFY
                ClientTransaction transac = null;
                try {
                    transac = createNotify(contact, 
                            new byte[0],
                            SubscriptionStateHeader.TERMINATED,
                            SubscriptionStateHeader.TIMEOUT);
                } catch (OperationFailedException e) {
                    logger.debug("failed to create the new notify", e);
                    return;
                }
                
                try {
                    dialog.sendRequest(transac);
                } catch (Exception e) {
                    logger.debug("Can't send the request");
                    return;
                }
                
                return;
            }
            
            // immediately send a 202/ACCEPTED
            Response response = null;
            try {
                response = this.parentProvider.getMessageFactory()
                    .createResponse(Response.ACCEPTED, request);
            } catch (Exception e) {
                logger.debug("Error while creating the response 202", e);
                return;
            }
            
            // add the expire header
            try {
                expHeader = this.parentProvider.getHeaderFactory()
                    .createExpiresHeader(SUBSCRIBE_DEFAULT_EXPIRE);
            } catch (InvalidArgumentException e) {
                logger.error("Can't create the expires header");
                return;
            }
            response.setHeader(expHeader);
            
            try {
                serverTransaction.sendResponse(response);
            } catch (Exception e) {
                logger.error("Error while sending the response 202", e);
                return;
            }
            
            // send a first NOTIFY with an empty body (to not reveal our current
            // presence status)
            ClientTransaction transac = null;
            try {
                transac = createNotify(contact, new byte[0],
                        SubscriptionStateHeader.PENDING, null);
            } catch (OperationFailedException e) {
                logger.debug("failed to create the first notify", e);
                return;
            }
            
            try {
                dialog.sendRequest(transac);
            } catch (Exception e) {
                logger.debug("Can't send the request");
                return;
            }
            
            // ask the user authorization
            AuthorizationResponse authResp = null;
            if (this.authorizationHandler != null) {
                AuthorizationRequest authReq = new AuthorizationRequest();
                authResp = this.authorizationHandler
                    .processAuthorisationRequest(authReq, contact);
            }
            
            // the user accepts
            if (authResp == null || authResp.getResponseCode().equals(
                    AuthorizationResponse.ACCEPT))
            {
                try {
                    transac = createNotify(contact, 
                            getPidfPresenceStatus((ContactSipImpl)
                                    getLocalContact()),
                            SubscriptionStateHeader.ACTIVE,
                            null);
                } catch (OperationFailedException e) {
                    logger.debug("failed to create the new notify", e);
                    return;
                }
                
                try {
                    dialog.sendRequest(transac);
                } catch (Exception e) {
                    logger.debug("Can't send the request");
                    return;
                }
                
                // add him to our watcher list
                synchronized (this.ourWatchers) {
                    this.ourWatchers.add(contact);
                }
            } else {
                // the user rejects
                try {
                    transac = createNotify(contact, 
                            new byte[0],
                            SubscriptionStateHeader.TERMINATED,
                            SubscriptionStateHeader.REJECTED);
                } catch (OperationFailedException e) {
                    logger.debug("failed to create the new notify", e);
                    return;
                }
                
                try {
                    dialog.sendRequest(transac);
                } catch (Exception e) {
                    logger.debug("Can't send the request");
                    return;
                }
            }
        }
    }
    
    /**
     * Called when a dialog is terminated
     * 
     * @param dialogTerminatedEvent DialogTerminatedEvent
     */
    public void processDialogTerminated(
            DialogTerminatedEvent dialogTerminatedEvent)
    {
        // never fired
    }

    /**
     * Called when an IO error occurs
     * 
     * @param exceptionEvent IOExceptionEvent
     */
    public void processIOException(IOExceptionEvent exceptionEvent)
    {
        // never fired
    }

    /**
     * Called when a transaction is terminated
     * 
     * @param transactionTerminatedEvent TransactionTerminatedEvent
     */
    public void processTransactionTerminated(
        TransactionTerminatedEvent transactionTerminatedEvent)
    {
        // nothing to do
    }

    /**
     * Called when a timeout occur
     *
     * @param timeoutEvent TimeoutEvent
     */
    public void processTimeout(TimeoutEvent timeoutEvent)
    {
        logger.error("timeout reached, it looks really abnormal: " +
                timeoutEvent.toString());
    }
    
    /**
     * Attempts to re-generate the corresponding request with the proper
     * credentials.
     *
     * @param clientTransaction the corresponding transaction
     * @param response the challenge
     * @param jainSipProvider the provider that received the challenge
     *
     * @throws OperationFailedException if processing the authentication
     * challenge fails.
     */
    private void processAuthenticationChallenge(
        ClientTransaction clientTransaction,
        Response response,
        SipProvider jainSipProvider)
        throws OperationFailedException
    {
        try
        {
            logger.debug("Authenticating a message request.");

            ClientTransaction retryTran
                = this.parentProvider.getSipSecurityManager().handleChallenge(
                    response
                    , clientTransaction
                    , jainSipProvider);

            retryTran.sendRequest();
            return;
        }
        catch (Exception exc)
        {
            logger.error("We failed to authenticate a message request.",
                         exc);

            throw new OperationFailedException("Failed to authenticate"
                + "a message request"
                , OperationFailedException.INTERNAL_ERROR
                , exc);
        }
    }
    
    /**
     * Notifies all registered listeners of the new event.
     *
     * @param source the contact that has caused the event.
     * @param parentGroup the group that contains the source contact.
     * @param oldValue the status that the source contact detained before
     * changing it.
     */
    public void fireContactPresenceStatusChangeEvent(ContactSipImpl  source,
                                                     ContactGroup parentGroup,
                                                     PresenceStatus oldValue)
    {
        ContactPresenceStatusChangeEvent evt
            = new ContactPresenceStatusChangeEvent(source, this.parentProvider,
                        parentGroup, oldValue, source.getPresenceStatus());

        Iterator listeners = null;
        synchronized(this.contactPresenceStatusListeners)
        {
            listeners = new ArrayList(this.contactPresenceStatusListeners)
                .iterator();
        }


        while(listeners.hasNext())
        {
            ContactPresenceStatusListener listener
                = (ContactPresenceStatusListener) listeners.next();

            listener.contactPresenceStatusChanged(evt);
        }
    }
    
    /**
     * Sets the presence status of <tt>contact</tt> to <tt>newStatus</tt>.
     *
     * @param contact the <tt>ContactSipImpl</tt> whose status we'd like
     * to set.
     * @param newStatus the new status we'd like to set to <tt>contact</tt>.
     */
    private void changePresenceStatusForContact(ContactSipImpl contact,
            PresenceStatus newStatus)
    {
        PresenceStatus oldStatus = contact.getPresenceStatus();
        contact.setPresenceStatus(newStatus);

        fireContactPresenceStatusChangeEvent(
                contact, contact.getParentContactGroup(), oldStatus);
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
        return this.contactListRoot.findContactByID(contactID);
    }

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
    public Contact getLocalContact() {
        ContactSipImpl res;
        res = new ContactSipImpl(this.parentProvider.getOurSipAddress()
                .getURI().toString(), this.parentProvider);
        
        res.setPresenceStatus(this.presenceStatus);
        
        return res;
    }

    /**
     * Handler for incoming authorization requests. An authorization request
     * notifies the user that someone is trying to add her to their contact list
     * and requires her to approve or reject authorization for that action.
     * 
     * @param handler an instance of an AuthorizationHandler for authorization
     * requests coming from other users requesting permission add us to their
     * contact list.
     */
    public void setAuthorizationHandler(AuthorizationHandler handler) {
        this.authorizationHandler = handler;
    }

    /**
     * Adds a listener that would receive events upon changes of the provider
     * presence status.
     * 
     * @param listener the listener to register for changes in our
     * PresenceStatus.
     */
    public void addProviderPresenceStatusListener(
        ProviderPresenceStatusListener listener)
    {
        synchronized(this.providerPresenceStatusListeners)
        {
            if (!this.providerPresenceStatusListeners.contains(listener))
                this.providerPresenceStatusListeners.add(listener);
        }
    }

    /**
     * Unregisters the specified listener so that it does not receive further
     * events upon changes in local presence status.
     * 
     * @param listener ProviderPresenceStatusListener
     */
    public void removeProviderPresenceStatusListener(
        ProviderPresenceStatusListener listener)
    {
        synchronized(this.providerPresenceStatusListeners)
        {
            this.providerPresenceStatusListeners.remove(listener);
        }
    }

    /**
     * Registers a listener that would receive a presence status change event
     * every time a contact, whose status we're subscribed for, changes her
     * status.
     * Note that, for reasons of simplicity and ease of implementation, there
     * is only a means of registering such "global" listeners that would 
     * receive updates for status changes for any contact and it is not
     * currently possible to register such contacts for a single contact or a
     * subset of contacts.
     *
     * @param listener the listener that would received presence status
     * updates for contacts.
     */
    public void addContactPresenceStatusListener(
        ContactPresenceStatusListener listener)
    {
        synchronized(this.contactPresenceStatusListeners)
        {
            if (!this.contactPresenceStatusListeners.contains(listener))
                this.contactPresenceStatusListeners.add(listener);
        }
    }

    /**
     * Removes the specified listener so that it won't receive any further
     * updates on contact presence status changes
     * 
     * @param listener the listener to remove.
     */
    public void removeContactPresenceStatusListener(
        ContactPresenceStatusListener listener)
    {
        synchronized(this.contactPresenceStatusListeners)
        {
            this.contactPresenceStatusListeners.remove(listener);
        }
    }

    /**
     * Registers a listener that would get notifications any time a new
     * subscription was succesfully added, has failed or was removed.
     * 
     * @param listener the SubscriptionListener to register
     */
    public void addSubsciptionListener(SubscriptionListener listener) {
        synchronized(this.subscriptionListeners)
        {
            if (!this.subscriptionListeners.contains(listener))
                this.subscriptionListeners.add(listener);
        }
    }

    /**
     * Removes the specified subscription listener.
     * 
     * @param listener the listener to remove.
     */
    public void removeSubscriptionListener(SubscriptionListener listener) {
        synchronized(this.subscriptionListeners)
        {
            this.subscriptionListeners.remove(listener);
        }
    }
    
    /**
     * Registers a listener that would receive events upon changes in server
     * stored groups.
     *
     * @param listener a ServerStoredGroupChangeListener impl that would
     *   receive events upong group changes.
     */
    public void addServerStoredGroupChangeListener(ServerStoredGroupListener
                                                        listener)
    {
        synchronized(this.serverStoredGroupListeners)
        {
            if (!this.serverStoredGroupListeners.contains(listener))
                this.serverStoredGroupListeners.add(listener);
        }
    }
    
    /**
     * Removes the specified group change listener so that it won't receive
     * any further events.
     *
     * @param listener the ServerStoredGroupChangeListener to remove
     */
    public void removeServerStoredGroupChangeListener(ServerStoredGroupListener
        listener)
    {
        synchronized(this.serverStoredGroupListeners)
        {
            this.serverStoredGroupListeners.remove(listener);
        }
    }

    /**
     * Returns the status message that was confirmed by the server
     * 
     * @return the last status message that we have requested and the server
     * has confirmed.
     */
    public String getCurrentStatusMessage() {
        return this.statusMessage;
    }
    
    /**
     * Notifies all registered listeners of the new event.
     *
     * @param source the contact that has caused the event.
     * @param parentGroup the group that contains the source contact.
     * @param eventID an identifier of the event to dispatch.
     */
    public void fireSubscriptionEvent(ContactSipImpl  source,
                                      ContactGroup parentGroup,
                                      int          eventID)
    {
        SubscriptionEvent evt  = new SubscriptionEvent(source
            , this.parentProvider
            , parentGroup
            , eventID);

        Iterator listeners = null;
        synchronized (this.subscriptionListeners)
        {
            listeners = new ArrayList(this.subscriptionListeners).iterator();
        }

        while (listeners.hasNext())
        {
            SubscriptionListener listener
                = (SubscriptionListener) listeners.next();

            if(eventID == SubscriptionEvent.SUBSCRIPTION_CREATED)
            {
                listener.subscriptionCreated(evt);
            }
            else if (eventID == SubscriptionEvent.SUBSCRIPTION_FAILED)
            {
                listener.subscriptionFailed(evt);
            }
            else if (eventID == SubscriptionEvent.SUBSCRIPTION_REMOVED)
            {
                listener.subscriptionRemoved(evt);
            }
        }
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
    public Contact createUnresolvedContact(
        String address, String persistentData)
    {
        return createUnresolvedContact(address
                , persistentData
                , getServerStoredContactListRoot());
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
    * @param parent the group where the unresolved contact is
    * supposed to belong to.
    *
    * @return the unresolved <tt>Contact</tt> created from the specified
    * <tt>address</tt> and <tt>persistentData</tt>
    */
    public Contact createUnresolvedContact(String address,
                         String persistentData,
                         ContactGroup parent)
    {
        ContactSipImpl contact = new ContactSipImpl(
            address,
            this.parentProvider);
        contact.setResolved(false);
        
        ((ContactGroupSipImpl) parent).addContact(contact);
        
        fireSubscriptionEvent(contact,
                parent,
                SubscriptionEvent.SUBSCRIPTION_CREATED);
        
        return contact;
    }
    
    /**
     * Creates a non persistent contact for the specified address. This would
     * also create (if necessary) a group for volatile contacts that would not
     * be added to the server stored contact list. This method would have no
     * effect on the server stored contact list.
     *
     * @param contactAddress the address of the volatile contact we'd like to
     * create.
     * @return the newly created volatile contact.
     */
    public ContactSipImpl createVolatileContact(String contactAddress)
    {
        // First create the new volatile contact;
        ContactSipImpl newVolatileContact
            = new ContactSipImpl(contactAddress, this.parentProvider);
        newVolatileContact.setPersistent(false);

        // Check whether a volatile group already exists and if not create one
        ContactGroupSipImpl theVolatileGroup = getNonPersistentGroup();

        // if the parent volatile group is null then we create it
        if (theVolatileGroup == null)
        {
            List emptyBuddies = new LinkedList();
            theVolatileGroup = new ContactGroupSipImpl(
                "NotInContactList",
                this.parentProvider);
            theVolatileGroup.setResolved(false);
            theVolatileGroup.setPersistent(false);

            this.contactListRoot.addSubgroup(theVolatileGroup);

            fireServerStoredGroupEvent(theVolatileGroup
                           , ServerStoredGroupEvent.GROUP_CREATED_EVENT);
        }

        //now add the volatile contact instide it
        theVolatileGroup.addContact(newVolatileContact);
        fireSubscriptionEvent(newVolatileContact
                         , theVolatileGroup
                         , SubscriptionEvent.SUBSCRIPTION_CREATED);

        return newVolatileContact;
    }
    
    /**
     * Returns the volatile group or null if this group has not yet been
     * created.
     *
     * @return a volatile group existing in our contact list or <tt>null</tt>
     * if such a group has not yet been created.
     */
    private ContactGroupSipImpl getNonPersistentGroup()
    {
        for (int i = 0;
             i < getServerStoredContactListRoot().countSubgroups();
             i++)
        {
            ContactGroupSipImpl gr = (ContactGroupSipImpl)
                getServerStoredContactListRoot().getGroup(i);

            if(!gr.isPersistent()) {
                return gr;
            }
        }

        return null;
    }

    
    /**
     * Try to find a contact registered using a string to identify him.
     *
     * @param contactID A string with which the contact may have
     *  been registered
     * @return A valid contact if it has been found, null otherwise
     */
    private Contact resolveContactID(String contactID) {
        Contact res = this.findContactByID(contactID);

        if (res == null) {
            // we try to resolve the conflict by removing "sip:" from the id
            if (contactID.startsWith("sip:")) {
                res = this.findContactByID(contactID.substring(4));
            }

            if (res == null) {
                // we try to remove the part after the '@'
                if (contactID.indexOf('@') > -1) {
                    res = this.findContactByID(
                            contactID.substring(0, contactID.indexOf('@')));

                    if (res == null) {
                        // try the same thing without sip:
                        if (contactID.startsWith("sip:")) {
                            res = this.findContactByID(
                                    contactID.substring(4,
                                            contactID.indexOf('@')));
                        }
                    }
                }
            }
        }

        return res;
    }
    
    /**
     * Returns contact if present in the watcher list, null else.
     * 
     * @param contactAddress the contact to find 
     * 
     * @return the watcher or null if the contact isn't a watcher
     */
    private ContactSipImpl getWatcher(String contactAddress) {
        String id1 = contactAddress;
        // without sip:
        String id2 = contactAddress.substring(4);
        // without the domain
        String id3 = contactAddress.substring(0, contactAddress.indexOf('@'));
        // without sip: and the domain
        String id4 = contactAddress.substring(4, contactAddress.indexOf('@'));
        
        Iterator iter = this.ourWatchers.iterator();
        while (iter.hasNext()) {
            ContactSipImpl contact = (ContactSipImpl) iter.next();
            
            // test by order of probability to be true
            // will probably save 1µs :)
            if (contact.getAddress().equals(id2)
                    || contact.getAddress().equals(id1)
                    || contact.getAddress().equals(id4)
                    || contact.getAddress().equals(id3))
            {
                return contact;
            }
        }
        
        return null;
    }
    
    /**
     * Returns a new valid xml document.
     * 
     * @return a correct xml document or null if an error occurs
     */
    private Document createDocument() {
        try {
            if (this.docBuilderFactory == null) {
                this.docBuilderFactory = DocumentBuilderFactory.newInstance();
            }
            
            if (this.docBuilder == null) {
                this.docBuilder = this.docBuilderFactory.newDocumentBuilder();
            }
        } catch (Exception e) {
            logger.error("can't create the new xml document", e);
            return null;
        }
        
        return this.docBuilder.newDocument();
    }
    
    /**
     * Convert a xml document
     * 
     * @param document the document to convert
     * 
     * @return a string representing <tt>document</tt> or null if an error
     * occur
     */
    private String convertDocument(Document document) {
        DOMSource source = new DOMSource(document);
        StringWriter stringWriter = new StringWriter();
        StreamResult result = new StreamResult(stringWriter);
        
        try {
            if (this.transFactory == null) {
                this.transFactory = TransformerFactory.newInstance();
            }
            
            if (this.transformer == null) {
                this.transformer = this.transFactory.newTransformer();
            }
            
            this.transformer.transform(source, result);
        } catch (Exception e) {
            logger.error("can't convert the xml document into a string", e);
            return null;
        }
        
        return stringWriter.toString();
    }
    
    /**
     * Convert a xml document
     * 
     * @param document the document as a String
     * 
     * @return a <tt>Document</tt> reprensenting the document or null if an
     * error occur
     */
    private Document convertDocument(String document) {
        StringReader reader = new StringReader(document);
        StreamSource source = new StreamSource(reader);
        Document doc = createDocument();
        
        if (doc == null) {
            return null;
        }
        
        DOMResult result = new DOMResult(doc);     
        
        try {
            if (this.transFactory == null) {
                this.transFactory = TransformerFactory.newInstance();
            }
            
            if (this.transformer == null) {
                this.transformer = this.transFactory.newTransformer();
            }
            
            this.transformer.transform(source, result);
        } catch (Exception e) {
            logger.error("can't convert the string into a xml document", e);
            return null;
        }
        
        return doc;
    }
    
    /**
     * Converts the <tt>PresenceStatus</tt> of <tt>contact</tt> into a PIDF
     * document.
     * 
     * @param contact The contact which interest us
     * 
     * @return a PIDF document representing the current presence status of
     * this contact or null if an error occurs.
     */
     public byte[] getPidfPresenceStatus(ContactSipImpl contact)
     {
         Document doc = this.createDocument();
         
         if (doc == null) {
             return null;
         }
         
         String contactUri = null;
         try {
             contactUri = this.parseAddressStr(contact.getAddress())
                 .getURI().toString();
         } catch (ParseException e) {
             // should not happen
             logger.debug("failed to parse the contact id", e);
             return null;
         }
         
         // <presence>
         Element presence = doc.createElement(PRESENCE_ELEMENT);
         presence.setAttribute(NS_ELEMENT, NS_VALUE);
         presence.setAttribute(ENTITY_ATTRIBUTE, contactUri);
         doc.appendChild(presence);
         
         // <tuple>
         Element tuple = doc.createElement(TUPLE_ELEMENT);
         tuple.setAttribute(ID_ATTRIBUTE, String
                 .valueOf(tupleid++));
         presence.appendChild(tuple);
         
         // <status>
         Element status = doc.createElement(STATUS_ELEMENT);
         tuple.appendChild(status);

         // <basic>
         Element basic = doc.createElement(BASIC_ELEMENT);
         if (this.getPresenceStatus().equals(SipStatusEnum.OFFLINE)) {
             basic.appendChild(doc.createTextNode(OFFLINE_STATUS));
         } else {
             basic.appendChild(doc.createTextNode(ONLINE_STATUS));
         }
         status.appendChild(basic);
         
         // <contact>
         Element contactUriEl = doc.createElement(CONTACT_ELEMENT);
         Node cValue = doc.createTextNode(contactUri);
         contactUriEl.appendChild(cValue);
         tuple.appendChild(contactUriEl);
         
         // <note> we write our real status here, this status SHOULD not be
         // used for automatic parsing but some (bad) IM clients do this...
         // we don't use xml:lang here because it's not really revelant
         Element noteNodeEl = doc.createElement(NOTE_ELEMENT);
         noteNodeEl.appendChild(doc.createTextNode(this.getPresenceStatus()
                 .getStatusName()));
         tuple.appendChild(noteNodeEl);
         
         String res = convertDocument(doc);
         if (res == null) {
             return null;
         }
         
         return res.getBytes();
     }
     
     /**
      * Sets the contact's presence status using the PIDF document provided.
      * In case of conflict (more than one status per contact) the last valid
      * status in the document is used.
      * This implementation is very tolerant to be more compatible with bad
      * implementations of SIMPLE. The limit of the tolerance is defined by
      * the CPU cost: as far as the tolerance costs nothing more in well
      * structured documents, we do it.
      * 
      * @param presenceDoc the pidf document to use
      */
     public void setPidfPresenceStatus(String presenceDoc)
     {
         Document doc = convertDocument(presenceDoc);
         
         if (doc == null) {
             return;
         }
         
         logger.debug("parsing:\n" + presenceDoc);
         
         // <presence>
         NodeList presList = doc.getElementsByTagName(PRESENCE_ELEMENT);
         if (presList.getLength() == 0) {
             logger.error("no presence element in this document");
             return;
         }
         if (presList.getLength() > 1) {
             logger.warn("more than one presence element in this document");
         }
         Node presNode = presList.item(0);
         if (presNode.getNodeType() != Node.ELEMENT_NODE) {
             logger.error("the presence node is not an element");
             return;
         }
         Element presence = (Element) presNode;
         
         // <tuple>
         NodeList tupleList = presence.getElementsByTagName(TUPLE_ELEMENT);
         for (int i = 0; i < tupleList.getLength(); i++) {
             Node tupleNode = tupleList.item(i);
             
             if (tupleNode.getNodeType() != Node.ELEMENT_NODE) {
                 continue;
             }
             
             Element tuple = (Element) tupleNode;
             
             // <contact>
             NodeList contactList = tuple.getElementsByTagName(
                     CONTACT_ELEMENT);
             
             // there should normally be only one contact per tuple (RFC3863)
             for (int j = 0; j < contactList.getLength(); j++) {
                 Node contactNode = contactList.item(j);
                 
                 if (contactNode.getNodeType() != Node.ELEMENT_NODE) {
                     continue;
                 }
                 
                 Element contact = (Element) contactNode;
                 ContactSipImpl sipcontact = (ContactSipImpl)
                     resolveContactID(getTextContent(contact));
                 
                 if (sipcontact == null) {
                     logger.debug("no contact found for id: " +
                             getTextContent(contact));
                     continue;
                 }
                 
                 // <status>
                 NodeList statusList = tuple.getElementsByTagName(
                         STATUS_ELEMENT);
                 if (statusList.getLength() == 0) {
                     logger.debug("no status in this tuple");
                     continue;
                 }
                 
                 // in case of many status, just consider the last one
                 // this is normally not permitted by RFC3863
                 int index = statusList.getLength() - 1;
                 Node statusNode = null;
                 do {
                     Node temp = statusList.item(index);
                     if (temp.getNodeType() == Node.ELEMENT_NODE) {
                         statusNode = temp;
                         break;
                     }
                     index--; 
                 } while (index >= 0);
                 
                 if (statusNode == null) {
                     logger.debug("no valid status in this tuple");
                     break;
                 }
                 
                 Element status = (Element) statusNode;
                 
                 // <basic>
                 NodeList basicList = status.getElementsByTagName(
                         BASIC_ELEMENT);
                 
                 if (basicList.getLength() == 0) {
                     logger.debug("no <basic> in this status");
                     continue;
                 }
                 
                 // in case of many basic, just consider the last one
                 // this is normally not permitted by RFC3863
                 index = basicList.getLength() - 1;
                 Node basicNode = null;
                 do {
                     Node temp = statusList.item(index);
                     if (temp.getNodeType() == Node.ELEMENT_NODE) {
                         basicNode = temp;
                         break;
                     }
                     index--; 
                 } while (index >= 0);
                 
                 if (basicNode == null) {
                     logger.debug("no valid <basic> in this status");
                     break;
                 }
                 
                 Element basic = (Element) basicNode;

                 if (getTextContent(basic).equalsIgnoreCase(ONLINE_STATUS)) {
                     // search for a <note> that can define a more precise
                     // status this is not recommended by RFC3863 but some im 
                     // clients use this.
                     NodeList noteList = tuple.getElementsByTagName(
                             NOTE_ELEMENT);

                     boolean changed = false;
                     for (int k = 0; k < noteList.getLength(); k++) {
                         Node noteNode = noteList.item(k);
                         
                         if (noteNode.getNodeType() != Node.ELEMENT_NODE) {
                             continue;
                         }
                         
                         Element note = (Element) noteNode;
                         
                         String state = getTextContent(note);
                         
                         // away ?
                         if (state.equalsIgnoreCase(SipStatusEnum.AWAY
                                 .getStatusName()))
                         {
                             changed = true;
                             changePresenceStatusForContact(sipcontact,
                                     SipStatusEnum.AWAY);
                         }
                     }
                     
                     if (changed == false) {
                         changePresenceStatusForContact(sipcontact,
                             SipStatusEnum.ONLINE);
                     }
                 } else if (getTextContent(basic).equalsIgnoreCase(
                         OFFLINE_STATUS))
                 {
                     changePresenceStatusForContact(sipcontact,
                             SipStatusEnum.OFFLINE);
                 }
             }
         }
     }
     
     /**
      * Secured call to XMLUtils.getText (no null returned but an empty string)
      * 
      * @param node the node with which call <tt>XMLUtils.getText()</tt>
      * 
      * @return the string contained in the node or an empty string if there is
      * no text information in the node.
      */
     private String getTextContent(Element node) {
         String res = XMLUtils.getText(node);
         
         if (res == null) {
             return "";
         }
         
         return res;
     }
     
     /**
      * Unsubscribe to every contact.
      */
     public void unsubscribeToAllContact() {
         logger.debug("trying to unsubscribe to every contact");
         
         // send event notifications saying that all our buddies are
         // offline. SIMPLE does not implement top level buddies
         // nor subgroups for top level groups so a simple nested loop
         // would be enough.
         Iterator groupsIter = getServerStoredContactListRoot()
             .subgroups();
         while (groupsIter.hasNext()) {
             ContactGroupSipImpl group = (ContactGroupSipImpl)
                 groupsIter.next();

             Iterator contactsIter = group.contacts();

             while (contactsIter.hasNext()) {
                 ContactSipImpl contact = (ContactSipImpl)
                     contactsIter.next();

                 PresenceStatus oldContactStatus = 
                     contact.getPresenceStatus();
                 
                 // if it's needed, we send an unsubcsription message
                 if (!oldContactStatus.equals(SipStatusEnum.OFFLINE)
                     && !oldContactStatus.equals(SipStatusEnum.UNKNOWN)
                     && contact.isResolved())
                 {
                     //assertConnected(); will fail because the parent provider
                     //                   is already unregistered at this point
                     
                     Dialog dialog = contact.getClientDialog();
                     
                     ClientTransaction transac = null;
                     try
                     {
                         transac = createSubscription(0, dialog);
                     }
                     catch (OperationFailedException e)
                     {
                         logger.error(
                             "Failed to create subscriptionTransaction.", e);

                         return;
                     }
                     
                     // we are not anymore subscribed to this contact
                     // this ensure that the response of this request will be
                     // handled as an unsubscription response
                     this.subscribedContacts.remove(
                             dialog.getCallId().getCallId());
                     
                     // remember the callId to be sure to end the subscription
                     // before unregistering
                     synchronized (this.waitedCallIds) {
                         this.waitedCallIds.add(dialog.getCallId().getCallId());
                     }
                     
                     try {
                         dialog.sendRequest(transac);
                     } catch (Exception e) {
                         logger.debug("Can't send the request");
                         return;
                     }
                     
                     logger.debug("unsubscribed to " + contact);
                 } else {
                     logger.debug("contact " + contact
                             + " doesn't insteress us");
                 }

                 terminateSubscription(contact);
             }
         }
     }
     
     
     protected class RegistrationListener
     implements RegistrationStateChangeListener
     {
         /**
          * The method is called by a ProtocolProvider implementation whenver
          * a change in the registration state of the corresponding provider had
          * occurred. The method is particularly interested in events stating
          * that the SIP provider has unregistered so that it would fire
          * status change events for all contacts in our buddy list.
          *
          * @param evt ProviderStatusChangeEvent the event describing the status
          * change.
          */
          public void registrationStateChanged(RegistrationStateChangeEvent evt)
          {
              if(evt.getNewState() == RegistrationState.UNREGISTERING)
              {
                  // this will not be called by anyone else, so call it
                  // the method will terminate every active subscription
                  try {
                      publishPresenceStatus(SipStatusEnum.OFFLINE, "");
                  } catch (OperationFailedException e) {
                      logger.error("can't set the offline mode", e);
                  }
                  
                  // we wait for every SUBSCRIBE, NOTIFY and PUBLISH transaction
                  // to finish before continuing the unsubscription
                  for (int i = 0; i < 100; i++) {   // wait 10 s. max
                      synchronized (waitedCallIds) {
                          if (waitedCallIds.size() == 0) {
                              break;
                          }
                      }
                      
                      Object o = new Object(); // don't block the 'this' monitor
                      synchronized (o) {
                          try {
                              o.wait(100);
                          } catch (InterruptedException e) {
                              logger.debug("abnormal behavior, may cause " +
                                    "unnecessary CPU use", e);
                          }
                      }
                  }
                  
                  // since we are disconnected, we won't receive any further
                  // status updates so we need to change by ourselves our own
                  // status as well as set to offline all contacts in our
                  // contact list that were online
                  PresenceStatus oldStatus = presenceStatus;
                  presenceStatus = SipStatusEnum.OFFLINE;

                  fireProviderStatusChangeEvent(oldStatus);
              } else if (evt.getNewState().equals(
                      RegistrationState.REGISTERED))
              {
                 logger.debug("enter register state"); 
                  
                  // send a subscription for every contact
                  Iterator groupsIter = getServerStoredContactListRoot()
                      .subgroups();
                  while (groupsIter.hasNext()) {
                      ContactGroupSipImpl group = (ContactGroupSipImpl)
                          groupsIter.next();
    
                      Iterator contactsIter = group.contacts();
    
                      while (contactsIter.hasNext()) {
                          ContactSipImpl contact = (ContactSipImpl)
                              contactsIter.next();
                          
                          if (contact.isResolved()) {
                              logger.debug("contact " + contact 
                                      + " already resolved");
                              continue;
                          }
                          
                          //create the subscription
                          Request subscription;
                          try
                          {
                              subscription = createSubscription(contact,
                                      SUBSCRIBE_DEFAULT_EXPIRE);
                          }
                          catch (OperationFailedException ex)
                          {
                              logger.error(
                                  "Failed to create the subcription"
                                  , ex);
                              
                              return;
                          }

                          //Transaction
                          ClientTransaction subscribeTransaction;
                          SipProvider jainSipProvider
                              = parentProvider.getDefaultJainSipProvider();
                          try
                          {
                              subscribeTransaction = jainSipProvider
                                  .getNewClientTransaction(subscription);
                          }
                          catch (TransactionUnavailableException ex)
                          {
                              logger.error(
                                  "Failed to create subscriptionTransaction.\n"
                                  + "This is most probably a network"
                                  + " connection error."
                                  , ex);

                              return;
                          }
                          
                          // we register the contact to find him when the OK
                          // will arrive
                          CallIdHeader idheader = (CallIdHeader)
                              subscription.getHeader(CallIdHeader.NAME);
                          subscribedContacts.put(idheader.getCallId(), contact);
                          logger.debug("added a contact at :" 
                                  + idheader.getCallId());
                          
                          // send the message
                          try
                          {
                              subscribeTransaction.sendRequest();
                          }
                          catch (SipException ex)
                          {
                              logger.error(
                                  "Failed to send the message.",
                                  ex);
                              
                              // this contact will never been accepted or
                              // rejected
                              subscribedContacts.remove(idheader.getCallId());

                              return;
                          }
                      }
                  }
                  
                  PresenceStatus oldStatus = getPresenceStatus();
                  presenceStatus = SipStatusEnum.ONLINE;
                  fireProviderStatusChangeEvent(oldStatus);
              }
         }
     }
}

