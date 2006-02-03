/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.contactlist;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.contactlist.event.MetaContactListListener;

/**
 * The <code>MetaContactListService</code> handles the global project contact
 * list including contacts from all implemented protocols.
 * <p>
 * An implementation of the <code>MetaContactListService</code> would take care
 * of synchronizing the local copy ot the contact list with the  versions stored
 * on the various server protocols.
 * <p>
 * All modules that would for some reason like to query or modify the contact
 * list should use this service rather than accessing protocol providers
 * directly.
 * <p>
 * The point of <code>MetaContact</code>s is being able to merge different
 * protocol specific contacts so that they represent a single person or identity.
 * Every protocol specific <code>Contact</code> would therefore automatically
 * be assigned to a corresponding <code>MetaContact</code>. A single
 * MetaContact may containg multiple contacts (e.g. a single person often
 * has accounts in different protocols) while a single protocol specific
 * Contact may only be assigned to a exactly one MetaContact.
 * <p>
 * Once created a MetaContact may be updated to contain multiple protocol
 * specific contacts. These protocol specific contacts may also be removed
 * away from a MetaContact. Whenever a MetaContact remains empty (i.e. all of
 * its protocol specific contacts are removed) it is automatically deleted.
 *
 * Note that for most of the methods defined by this interface, it is likely
 * that implementations require one or more network operations to complete
 * before returning. It is therefore strongly advised not to call these methods
 * in event dispatching threads (watch out UI implementors ;) ) as this may lead
 * to unpleasant user experience.
 *
 * @todo expections
 * @author Emil Ivov
 */
public interface MetaContactListService
{
    /**
     * Returns the root <code>MetaContactGroup</code> in this contact list.
     * All meta contacts and subgroups are children of the root meta contact
     * and references to them can only be obtained through it.
     *
     * @return the root <code>MetaContactGroup</code> for this contact list.
     */
    public MetaContactGroup getRoot();

    /**
     * Adds a listener for <code>MetaContactListChangeEvent</code>s posted after
     * the tree changes.
     *
     * @param l the listener to add
     */
    public void addContactListListener(MetaContactListListener l);

    /**
     * Removes a listener previously added with
     * <code>addContactListListener</code>.
     *
     * @param l the listener to remove
     */
    public void removeContactListListener(MetaContactListListener l);

    /**
     * Makes the specified <code>contact</code> a child of the
     * <code>newParent</code> MetaContact. If <code>contact</code> was
     * previously a child of another meta contact, it will be removed from its
     * old parent before being added to the new one. If the specified contact
     * was the only child of its previous parent, then it (the previous parent)
     * will be removed.
     *
     * @param contact the <code>Contact</code> to move to the
     * @param newParent the MetaContact where we'd like contact to be moved.
     *
     * @throws MetaContactListException with an appropriate code if the
     * operation fails for some reason.
     */
    public void moveContact(Contact contact, MetaContact newParent)
        throws MetaContactListException;

    /**
     * Deletes the specified contact from both the local contact list and (if
     * applicable) the server stored contact list if supported by the
     * corresponding protocol. If the <code>MetaContact</code> that contained
     * the given contact had no other children, it will be removed.
     * <p>
     * @param contact the contact to remove.
     * @throws MetaContactListException with an appropriate code if the
     * operation fails for some reason.
     */
    public void removeContact(Contact contact)
        throws MetaContactListException;

    /**
     * First makes the specified protocol provider create the contact as
     * indicated by <code>contactID</code>, and then associates it to the
     * _existing_ <code>metaContact</code> given as an argument.
     * <p>
     * @param provider the ProtocolProviderService that should create the
     * contact indicated by <code>contactID</code>.
     * @param metaContact the meta contact where that the newly created contact
     * should be associated to.
     * @param contactID the identifier of the contact that the specified provider
     *
     * @throws MetaContactListException with an appropriate code if the
     * operation fails for some reason.
     */
    public void addNewContactToMetaContact(ProtocolProviderService provider,
                                  MetaContact metaContact,
                                  String contactID)
        throws MetaContactListException;

    /**
     * First makes the specified protocol provider create a contact
     * corresponding to the specified <code>contactID</code>, then creates a new
     * MetaContact which will encapsulate the newly crated protocol specific
     * contact. Depending on implementations the method may sometimes need
     * time to complete as it may be necessary for an underlying protocol to
     * wait for a server to acknowledge addition of the contact.
     * <p>
     * If the specified parent MetaContactGroup did not have a corresponding
     * group on the protocol server, it will be created before the contact
     * itself.
     * <p>
     * @param provider a ref to <code>ProtocolProviderService</code> instance
     * which will create the actual protocol specific contact.
     * @param contactGroup the MetaContactGroup where the newly created meta
     * contact should be stored.
     * @param contactID a protocol specific string identifier indicating the
     * contact the prtocol provider should create.
     *
     * @throws MetaContactListException with an appropriate code if the
     * operation fails for some reason.
     */
    public void createMetaContact(ProtocolProviderService provider,
                                  MetaContactGroup contactGroup,
                                  String contactID)
        throws MetaContactListException;

    /**
     * Moves the specified <code>MetaContact</code> to <code>newGroup</code>.
     * <p>
     * @param metaContact the <code>MetaContact</code> to move.
     * @param newGroup the <code>MetaContactGroup</code> that should be the
     * new parent of <code>contact</code>.
     *
     * @throws MetaContactListException with an appropriate code if the
     * operation fails for some reason.
     */
    public void moveMetaContact(MetaContact metaContact,
                                MetaContactGroup newGroup)
        throws MetaContactListException;

    /**
     * Removes the specified <code>metaContact</code> as well as all of its
     * underlying contacts.
     * <p>
     * @param metaContact the metaContact to remove.
     * @throws MetaContactListException with an appropriate code if the
     * operation fails for some reason.
     */
    public void removeMetaContact(MetaContact metaContact)
        throws MetaContactListException;

    /**
     * Creates a <code>MetaContactGroup</code> with the specified group name.
     * Initially, the group would only be created locally. Corresponding
     * server stored groups will be created on the fly, whenever real protocol
     * specific contacts are added to the group if the protocol lying behind
     * them supports that.
     * <p>
     * @param groupName the name of the <code>MetaContactGroup</code> to create.
     *
     * @throws MetaContactListException with an appropriate code if the
     * operation fails for some reason.
     */
    public void createMetaContactGroup(String groupName)
        throws MetaContactListException;

    /**
     * Removes the specified meta contact group, all its corresponding protocol
     * specific groups and all their children. If some of the children belong to
     * server stored contact lists, they will be updated to not include the
     * child contacts any more.
     * @param groupToRemove the <code>MetaContactGroup</code> to have removed.
     *
     * @throws MetaContactListException with an appropriate code if the
     * operation fails for some reason.
     */
    public void removeMetaContactGroup(MetaContactGroup groupToRemove)
        throws MetaContactListException;
}
