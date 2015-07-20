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
package net.java.sip.communicator.service.gui;

import java.awt.*;
import java.util.*;
import java.util.List;

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
    extends ContactQueryListener,
            MetaContactQueryListener
{
    /**
     * Returns the actual component corresponding to the contact list.
     *
     * @return the actual component corresponding to the contact list
     */
    public Component getComponent();

    /**
     * Returns the list of registered contact sources to search in.
     *
     * @return the list of registered contact sources to search in
     */
    public Collection<UIContactSource> getContactSources();

    /**
     * Returns the <tt>ExternalContactSource</tt> corresponding to the given
     * <tt>ContactSourceService</tt>.
     *
     * @param contactSource the <tt>ContactSourceService</tt>, which
     * corresponding external source implementation we're looking for
     * @return the <tt>ExternalContactSource</tt> corresponding to the given
     * <tt>ContactSourceService</tt>
     */
    public UIContactSource getContactSource(ContactSourceService contactSource);

    /**
     * Adds the given contact source to the list of available contact sources.
     *
     * @param contactSource the <tt>ContactSourceService</tt>
     */
    public void addContactSource(ContactSourceService contactSource);

    /**
     * Removes the given contact source from the list of available contact
     * sources.
     *
     * @param contactSource
     */
    public void removeContactSource(ContactSourceService contactSource);

    /**
     * Removes all stored contact sources.
     */
    public void removeAllContactSources();

    /**
     * Sets the default filter to the given <tt>filter</tt>.
     * @param filter the <tt>ContactListFilter</tt> to set as default
     */
    public void setDefaultFilter(ContactListFilter filter);

    /**
     * Gets the default filter for this contact list.
     *
     * @return the default filter for this contact list
     */
    public ContactListFilter getDefaultFilter();

    /**
     * Returns all <tt>UIContactSource</tt>s of the given type.
     *
     * @param type the type of sources we're looking for
     * @return a list of all <tt>UIContactSource</tt>s of the given type
     */
    public List<UIContactSource> getContactSources(int type);

    /**
     * Adds the given group to this list.
     *
     * @param group the <tt>UIGroup</tt> to add
     * @param isSorted indicates if the contact should be sorted regarding to
     * the <tt>GroupNode</tt> policy
     */
    public void addGroup(final UIGroup group, final boolean isSorted);

    /**
     * Removes the given group and its children from the list.
     *
     * @param group the <tt>UIGroup</tt> to remove
     */
    public void removeGroup(final UIGroup group);

    /**
     * Adds the given <tt>contact</tt> to this list.
     *
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
     *
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
     *
     * @param contact the <tt>UIContact</tt> to remove
     * @param removeEmptyGroup whether we should delete the group if is empty
     */
    public void removeContact(  final UIContact contact,
                                final boolean removeEmptyGroup);

    /**
     * Removes the node corresponding to the given <tt>MetaContact</tt> from
     * this list.
     *
     * @param contact the <tt>UIContact</tt> to remove
     */
    public void removeContact(UIContact contact);

    /**
     * Removes all entries in this contact list.
     */
    public void removeAll();

    /**
     * Returns a collection of all direct child <tt>UIContact</tt>s of the given
     * <tt>UIGroup</tt>.
     *
     * @param group the parent <tt>UIGroup</tt>
     * @return a collection of all direct child <tt>UIContact</tt>s of the given
     * <tt>UIGroup</tt>
     */
    public Collection<UIContact> getContacts(final UIGroup group);

    /**
     * Returns the currently applied filter.
     *
     * @return the currently applied filter
     */
    public ContactListFilter getCurrentFilter();

    /**
     * Returns the currently applied filter.
     *
     * @return the currently applied filter
     */
    public FilterQuery getCurrentFilterQuery();

    /**
     * Applies the given <tt>filter</tt>.
     *
     * @param filter the <tt>ContactListFilter</tt> to apply.
     * @return the filter query
     */
    public FilterQuery applyFilter(ContactListFilter filter);

    /**
     * Applies the default filter.
     *
     * @return the filter query that keeps track of the filtering results
     */
    public FilterQuery applyDefaultFilter();

    /**
     * Returns the currently selected <tt>UIContact</tt>. In case of a multiple
     * selection returns the first contact in the selection.
     *
     * @return the currently selected <tt>UIContact</tt> if there's one.
     */
    public UIContact getSelectedContact();

    /**
     * Returns the list of selected contacts.
     *
     * @return the list of selected contacts
     */
    public List<UIContact> getSelectedContacts();

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
     * Selects the first found contact node from the beginning of the contact
     * list.
     */
    public void selectFirstContact();

    /**
     * Removes the current selection.
     */
    public void removeSelection();

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

    /**
     * Indicates if this contact list is empty.
     *
     * @return <tt>true</tt> if this contact list contains no children,
     * otherwise returns <tt>false</tt>
     */
    public boolean isEmpty();

    /**
     * Shows/hides buttons shown in contact row.
     *
     * @param isVisible <tt>true</tt> to show contact buttons, <tt>false</tt> -
     * otherwise.
     */
    public void setContactButtonsVisible(boolean isVisible);

    /**
     * Shows/hides buttons shown in contact row.
     *
     * return <tt>true</tt> to indicate that contact buttons are shown,
     * <tt>false</tt> - otherwise.
     */
    public boolean isContactButtonsVisible();

    /**
     * Enables/disables multiple selection.
     *
     * @param isEnabled <tt>true</tt> to enable multiple selection,
     * <tt>false</tt> - otherwise
     */
    public void setMultipleSelectionEnabled(boolean isEnabled);

    /**
     * Enables/disables drag operations on this contact list.
     *
     * @param isEnabled <tt>true</tt> to enable drag operations, <tt>false</tt>
     * otherwise
     */
    public void setDragEnabled(boolean isEnabled);

    /**
     * Enables/disables the right mouse click menu.
     *
     * @param isEnabled <tt>true</tt> to enable right button menu,
     * <tt>false</tt> otherwise.
     */
    public void setRightButtonMenuEnabled(boolean isEnabled);
}
