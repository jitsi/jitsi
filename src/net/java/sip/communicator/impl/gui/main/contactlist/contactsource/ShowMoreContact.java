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
package net.java.sip.communicator.impl.gui.main.contactlist.contactsource;

import java.util.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.main.contactlist.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.contactsource.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.gui.event.*;
import net.java.sip.communicator.service.protocol.*;

/**
 *
 * @author Yana Stamcheva
 */
public class ShowMoreContact
    extends UIContactImpl
    implements ContactListListener
{
    /**
     * The string associated with this contact.
     */
    private final String showMoreString
        = GuiActivator.getResources().getI18NString("service.gui.SHOW_MORE");

    /**
     * The parent group.
     */
    private UIGroup parentGroup;

    /**
     * The contact node corresponding to this contact.
     */
    private ContactNode contactNode;

    /**
     * The parent contact query, which added the contact.
     */
    private final ContactQuery contactQuery;

    /**
     * The query results.
     */
    private final List<SourceContact> queryResults;

    /**
     * The count of shown contacts corresponding to the underlying query.
     */
    private int shownResultsCount;

    /**
     * The maximum result count by show.
     */
    private int maxResultCount;

    /**
     * Creates an instance of <tt>MoreInfoContact</tt>.
     *
     * @param contactQuery the contact query
     * @param queryResults the result list
     * @param maxResultCount the maximum result count
     */
    public ShowMoreContact( ContactQuery contactQuery,
                            List<SourceContact> queryResults,
                            int maxResultCount)
    {
        this.contactQuery = contactQuery;
        this.queryResults = queryResults;
        this.maxResultCount = maxResultCount;

        // The contact list is already showing a number of results.
        this.shownResultsCount = maxResultCount;

        GuiActivator.getContactList().addContactListListener(this);
    }

    /**
     * Returns the descriptor of this contact.
     *
     * @return the descriptor of this contact
     */
    @Override
    public Object getDescriptor()
    {
        return showMoreString;
    }

    /**
     * Returns an empty string to indicate that this contact has no display
     * name.
     *
     * @return an empty string
     */
    @Override
    public String getDisplayName()
    {
        return "";
    }

    /**
     * Returns null to indicate that there are no display details.
     *
     * @return null
     */
    @Override
    public String getDisplayDetails()
    {
        return null;
    }

    /**
     * Returns Integer.MAX_VALUE to indicate that this contact should be placed
     * at the end of its parent group.
     *
     * @return Integer.MAX_VALUE
     */
    @Override
    public int getSourceIndex()
    {
        return Integer.MAX_VALUE;
    }

    /**
     * Returns null to indicate that this contact has no avatar.
     *
     * @param isSelected indicates if the contact is selected
     * @param width avatar width
     * @param height avatar height
     * @return null
     */
    @Override
    public ImageIcon getScaledAvatar(boolean isSelected, int width, int height)
    {
        return null;
    }

    /**
     * Returns null to indicate that this contact has no status icon.
     *
     * @return null
     */
    @Override
    public ImageIcon getStatusIcon()
    {
        return null;
    }

    /**
     * Returns an extended tooltip for this contact.
     *
     * @return the created tooltip
     */
    @Override
    public ExtendedTooltip getToolTip()
    {
        ExtendedTooltip tooltip = new ExtendedTooltip(false);

        tooltip.setTitle(GuiActivator.getResources()
            .getI18NString("service.gui.SHOW_MORE_TOOLTIP"));

        return tooltip;
    }

    /**
     * Returns null to indicate that this contact has no right button menu.
     *
     * @return null
     */
    @Override
    public JPopupMenu getRightButtonMenu()
    {
        return null;
    }

    /**
     * Returns the parent group of this contact.
     *
     * @return the parent group of this contact
     */
    @Override
    public UIGroup getParentGroup()
    {
        return parentGroup;
    }

    /**
     * Sets the parent group of this contact
     *
     * @param parentGroup the parent group of this contact
     */
    @Override
    public void setParentGroup(UIGroup parentGroup)
    {
        this.parentGroup = parentGroup;
    }

    /**
     * Returns null to indicate that this contact cannot be searched.
     *
     * @return null
     */
    @Override
    public Iterator<String> getSearchStrings()
    {
        return null;
    }

    /**
     * Returns the corresponding contact node.
     *
     * @return the corresponding contact node
     */
    @Override
    public ContactNode getContactNode()
    {
        return contactNode;
    }

    /**
     * Sets the corresponding contact node.
     *
     * @param contactNode the contact node to set
     */
    @Override
    public void setContactNode(ContactNode contactNode)
    {
        this.contactNode = contactNode;

        // contactNode is null, when the the ui contact is removed/cleared
        // we must free resources
        if(contactNode == null)
        {
            GuiActivator.getContactList().removeContactListListener(this);
        }
    }

    /**
     * Returns null to indicate that this contact has no contact details.
     *
     * @param opSetClass the <tt>OperationSet</tt> class, which details we're
     * looking for
     * @return null
     */
    @Override
    public UIContactDetail getDefaultContactDetail(
        Class<? extends OperationSet> opSetClass)
    {
        return null;
    }

    /**
     * Returns null to indicate that this contact has no contact details.
     *
     * @return null
     */
    @Override
    public List<UIContactDetail> getContactDetails()
    {
        return null;
    }

    /**
     * Returns null to indicate that this contact has no contact details.
     *
     * @param opSetClass the <tt>OperationSet</tt> class, which details we're
     * looking for
     * @return null
     */
    @Override
    public List<UIContactDetail> getContactDetailsForOperationSet(
        Class<? extends OperationSet> opSetClass)
    {
        return null;
    }

    /**
     * Indicates that a contact has been clicked in the contact list. Show some
     * more contacts after the "show more" has been clicked
     *
     * @param evt the <tt>ContactListEvent</tt> that notified us
     */
    public void contactClicked(ContactListEvent evt)
    {
        if (evt.getSourceContact().equals(this))
        {
            List<SourceContact> contacts
                = new ArrayList<SourceContact>(queryResults);

            int newCount
                = shownResultsCount + maxResultCount;

            int resultSize = contacts.size();

            int maxCount = (resultSize > newCount) ? newCount : resultSize;

            GuiActivator.getContactList().removeContact(this);

            for (int i = shownResultsCount; i < maxCount; i++)
            {
                GuiActivator.getContactList().contactReceived(
                    new ContactReceivedEvent(contactQuery, contacts.get(i)));
            }

            shownResultsCount = maxCount;

            if (shownResultsCount < resultSize
                || (contactQuery.getStatus() != ContactQuery.QUERY_COMPLETED
                && contactQuery.getStatus() != ContactQuery.QUERY_ERROR))
            {
                GuiActivator.getContactList().addContact(
                    contactQuery,
                    this,
                    GuiActivator.getContactList().getContactSource(
                        contactQuery.getContactSource()).getUIGroup(),
                    false);

                // The ContactListListener was removed when the ShowMoreContact
                // was removed from the contact list, so we need to add it
                // again.
                GuiActivator.getContactList().addContactListListener(this);
            }
        }
    }

    public void groupClicked(ContactListEvent evt) {}

    /**
     * We're not interested in group selection events here.
     */
    public void groupSelected(ContactListEvent evt) {}

    /**
     * We're not interested in contact selection events here.
     */
    public void contactSelected(ContactListEvent evt) {}

    /**
     * Returns all custom action buttons for this meta contact.
     *
     * @return a list of all custom action buttons for this meta contact
     */
    @Override
    public Collection<SIPCommButton> getContactCustomActionButtons()
    {
        return null;
    }
}
