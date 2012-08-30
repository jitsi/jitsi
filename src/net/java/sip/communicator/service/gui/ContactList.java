/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.gui;

import java.awt.*;
import java.util.*;

import net.java.sip.communicator.service.contactsource.*;
import net.java.sip.communicator.service.gui.event.*;

/**
 * The <tt>ContactList</tt> interface represents a contact list. All contact
 * list components that need to be available as a service could implement
 * this interface.
 *
 * @author Yana Stamcheva
 */
public interface ContactList
{
    /**
     * Returns the actual component corresponding to the contact list.
     *
     * @return the actual component corresponding to the contact list
     */
    public Component getComponent();

    /**
     * Returns the list of registered contact sources to search in.
     * @return the list of registered contact sources to search in
     */
    public Collection<UIContactSource> getContactSources();

    /**
     * Returns the <tt>ExternalContactSource</tt> corresponding to the given
     * <tt>ContactSourceService</tt>.
     * @param contactSource the <tt>ContactSourceService</tt>, which
     * corresponding external source implementation we're looking for
     * @return the <tt>ExternalContactSource</tt> corresponding to the given
     * <tt>ContactSourceService</tt>
     */
    public UIContactSource getContactSource(ContactSourceService contactSource);

    /**
     * Adds the given group to this list.
     * @param group the <tt>UIGroup</tt> to add
     * @param isSorted indicates if the contact should be sorted regarding to
     * the <tt>GroupNode</tt> policy
     */
    public void addGroup(final UIGroup group, final boolean isSorted);

    /**
     * Removes the given group and its children from the list.
     * @param group the <tt>UIGroup</tt> to remove
     */
    public void removeGroup(final UIGroup group);

    /**
     * Adds the given <tt>contact</tt> to this list.
     * @param contact the <tt>UIContact</tt> to add
     * @param group the <tt>UIGroup</tt> to add to
     * @param isContactSorted indicates if the contact should be sorted
     * regarding to the <tt>GroupNode</tt> policy
     * @param isGroupSorted indicates if the group should be sorted regarding to
     * the <tt>GroupNode</tt> policy in case it doesn't exist and should be
     * added
     */
    public void addContact( final UIContact contact,
                            final UIGroup group,
                            final boolean isContactSorted,
                            final boolean isGroupSorted);

    /**
     * Adds the given <tt>contact</tt> to this list.
     * @param query the <tt>ContactQuery</tt> that adds the given contact
     * @param contact the <tt>UIContact</tt> to add
     * @param group the <tt>UIGroup</tt> to add to
     * @param isSorted indicates if the contact should be sorted regarding to
     * the <tt>GroupNode</tt> policy
     */
    public void addContact(final ContactQuery query,
                            final UIContact contact,
                            final UIGroup group,
                            final boolean isSorted);

    /**
     * Removes the node corresponding to the given <tt>MetaContact</tt> from
     * this list.
     * @param contact the <tt>UIContact</tt> to remove
     * @param removeEmptyGroup whether we should delete the group if is empty
     */
    public void removeContact(  final UIContact contact,
                                final boolean removeEmptyGroup);

    /**
     * Removes the node corresponding to the given <tt>MetaContact</tt> from
     * this list.
     * @param contact the <tt>UIContact</tt> to remove
     */
    public void removeContact(UIContact contact);

    /**
     * Returns the currently applied filter.
     * @return the currently applied filter
     */
    public ContactListFilter getCurrentFilter();

    /**
     * Applies the given <tt>filter</tt>.
     * @param filter the <tt>ContactListFilter</tt> to apply.
     * @return the filter query
     */
    public FilterQuery applyFilter(ContactListFilter filter);

    /**
     * Applies the default filter.
     * @return the filter query that keeps track of the filtering results
     */
    public FilterQuery applyDefaultFilter();

    /**
     * Returns the currently selected <tt>UIContact</tt> if there's one.
     *
     * @return the currently selected <tt>UIContact</tt> if there's one.
     */
    public UIContact getSelectedContact();

    /**
     * Returns the currently selected <tt>UIGroup</tt> if there's one.
     *
     * @return the currently selected <tt>UIGroup</tt> if there's one.
     */
    public UIGroup getSelectedGroup();

    /**
     * Selects the given <tt>UIContact</tt> in the contact list.
     *
     * @param uiContact the contact to select
     */
    public void setSelectedContact(UIContact uiContact);

    /**
     * Selects the given <tt>UIGroup</tt> in the contact list.
     *
     * @param uiGroup the group to select
     */
    public void setSelectedGroup(UIGroup uiGroup);

    /**
     * Adds a listener for <tt>ContactListEvent</tt>s.
     *
     * @param listener the listener to add
     */
    public void addContactListListener(ContactListListener listener);

    /**
     * Removes a listener previously added with <tt>addContactListListener</tt>.
     *
     * @param listener the listener to remove
     */
    public void removeContactListListener(ContactListListener listener);

    /**
     * Refreshes the given <tt>UIContact</tt>.
     *
     * @param uiContact the contact to refresh
     */
    public void refreshContact(UIContact uiContact);
}
