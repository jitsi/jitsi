/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.contactlist.contactsource;

import java.util.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.main.contactlist.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.contactsource.*;
import net.java.sip.communicator.service.protocol.*;

/**
 * 
 * @author Yana Stamcheva
 */
public class ShowMoreContact
    implements  UIContact,
                ContactListListener
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
    private int shownResultsCount = FilterQuery.MAX_EXTERNAL_RESULT_COUNT;

    /**
     * Creates an instance of <tt>MoreInfoContact</tt>.
     *
     * @param contactQuery
     * @param queryResults
     */
    public ShowMoreContact( ContactQuery contactQuery,
                            List<SourceContact> queryResults)
    {
        this.contactQuery = contactQuery;
        this.queryResults = queryResults;

        GuiActivator.getContactList().addContactListListener(this);
    }

    /**
     * Returns the descriptor of this contact.
     *
     * @return the descriptor of this contact
     */
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
    public String getDisplayName()
    {
        return "";
    }

    /**
     * Returns null to indicate that there are no display details.
     *
     * @return null
     */
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
    public ImageIcon getAvatar(boolean isSelected, int width, int height)
    {
        return null;
    }

    /**
     * Returns null to indicate that this contact has no status icon.
     *
     * @return null
     */
    public ImageIcon getStatusIcon()
    {
        return null;
    }

    /**
     * Returns an extended tooltip for this contact.
     *
     * @return the created tooltip
     */
    public ExtendedTooltip getToolTip()
    {
        ExtendedTooltip tooltip = new ExtendedTooltip(
            GuiActivator.getUIService().getMainFrame(), false);

        tooltip.setTitle(GuiActivator.getResources()
            .getI18NString("service.gui.SHOW_MORE_TOOLTIP"));

        return tooltip;
    }

    /**
     * Returns null to indicate that this contact has no right button menu.
     *
     * @return null
     */
    public JPopupMenu getRightButtonMenu()
    {
        return null;
    }

    /**
     * Returns the parent group of this contact.
     *
     * @return the parent group of this contact
     */
    public UIGroup getParentGroup()
    {
        return parentGroup;
    }

    /**
     * Sets the parent group of this contact
     *
     * @param parentGroup the parent group of this contact
     */
    public void setParentGroup(UIGroup parentGroup)
    {
        this.parentGroup = parentGroup;
    }

    /**
     * Returns null to indicate that this contact cannot be searched.
     *
     * @return null
     */
    public Iterator<String> getSearchStrings()
    {
        return null;
    }

    /**
     * Returns the corresponding contact node.
     *
     * @return the corresponding contact node
     */
    public ContactNode getContactNode()
    {
        return contactNode;
    }

    /**
     * Sets the corresponding contact node.
     *
     * @param contactNode the contact node to set
     */
    public void setContactNode(ContactNode contactNode)
    {
        this.contactNode = contactNode;
    }

    /**
     * Returns null to indicate that this contact has no contact details.
     *
     * @param opSetClass the <tt>OperationSet</tt> class, which details we're
     * looking for
     * @return null
     */
    public UIContactDetail getDefaultContactDetail(
        Class<? extends OperationSet> opSetClass)
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
                = shownResultsCount + FilterQuery.MAX_EXTERNAL_RESULT_COUNT;

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
                || contactQuery.getStatus() != ContactQuery.QUERY_COMPLETED)
                GuiActivator.getContactList().addContact(
                    contactQuery,
                    this,
                    TreeContactList.getContactSource(
                        contactQuery.getContactSource()).getUIGroup(),
                    false);

        }
    }

    public void groupClicked(ContactListEvent evt) {}
}
