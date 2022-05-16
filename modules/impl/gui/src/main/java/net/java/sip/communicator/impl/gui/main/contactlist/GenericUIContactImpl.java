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
package net.java.sip.communicator.impl.gui.main.contactlist;

import java.awt.*;
import java.util.*;
import java.util.List;

import javax.swing.*;

import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.protocol.*;

/**
 * A generic full implementation of the <tt>UIContact</tt> interface.
 *
 * @author Yana Stamcheva
 */
public class GenericUIContactImpl
    extends UIContactImpl
{
    /**
     * The descriptor of the contact.
     */
    private final Object descriptor;

    /**
     * The parent group.
     */
    private UIGroup parentGroup;

    /**
     * The display name of this contact.
     */
    private final String displayName;

    /**
     * The display details of this contact.
     */
    private String displayDetails;

    /**
     * The index of the contact in its original source.
     */
    private int sourceIndex;

    /**
     * The list of string that correspond to this contact matches.
     */
    private List<String> searchStrings;

    /**
     * A map of this contact details.
     */
    private Map<Class<? extends OperationSet>, List<UIContactDetail>>
        contactDetails;

    private Collection<SIPCommButton> customActionButtons;

    /**
     * The corresponding <tt>ContactNode</tt> in the contact list component.
     */
    private ContactNode contactNode;

    /**
     * The status icon of this contact.
     */
    private ImageIcon statusIcon;

    /**
     * The avatar icon of this contact.
     */
    private ImageIcon avatarIcon;

    /**
     * Creates an instance of <tt>GenericUIContactImpl</tt>.
     *
     * @param descriptor the descriptor of the contact
     * @param parentGroup the parent group
     * @param displayName the display name of the contact
     */
    public GenericUIContactImpl(Object descriptor,
                                UIGroup parentGroup,
                                String displayName)
    {
        this.descriptor = descriptor;
        this.parentGroup = parentGroup;
        this.displayName = displayName;
    }

    /**
     * Returns the descriptor of this contact.
     *
     * @return the descriptor of this contact
     */
    @Override
    public Object getDescriptor()
    {
        return descriptor;
    }

    /**
     * Returns the display name of this contact.
     *
     * @return the display name of this contact
     */
    @Override
    public String getDisplayName()
    {
        return displayName;
    }

    /**
     * Returns the display details of this contact. These would be shown
     * whenever the contact is selected.
     *
     * @return the display details of this contact
     */
    @Override
    public String getDisplayDetails()
    {
        return displayDetails;
    }

    /**
     * Sets the display details of this contact. These would be shown
     * whenever the contact is selected.
     *
     * @param the display details of this contact
     */
    public void setDisplayDetails(String displayDetails)
    {
        this.displayDetails = displayDetails;
    }

    /**
     * Returns the index of this contact in its source.
     *
     * @return the source index
     */
    @Override
    public int getSourceIndex()
    {
        return sourceIndex;
    }

    /**
     * Sets the index of this contact.
     *
     * @param the source index
     */
    public void setSourceIndex(int index)
    {
        this.sourceIndex = index;
    }

    /**
     * Creates a tool tip for this contact. If such tooltip is
     * provided it would be shown on mouse over over this <tt>UIContact</tt>.
     *
     * @return the tool tip for this contact descriptor
     */
    @Override
    public ExtendedTooltip getToolTip()
    {
        return null;
    }

    /**
     * Returns the right button menu component.
     *
     * @return the right button menu component
     */
    @Override
    public Component getRightButtonMenu()
    {
        return null;
    }

    /**
     * Returns the parent group.
     *
     * @return the parent group
     */
    @Override
    public UIGroup getParentGroup()
    {
        return parentGroup;
    }

    /**
     * Sets the parent group.
     *
     * @param parentGroup the parent group
     */
    @Override
    public void setParentGroup(UIGroup parentGroup)
    {
        this.parentGroup = parentGroup;
    }

    /**
     * Returns an <tt>Iterator</tt> over a list of the search strings of this
     * contact.
     *
     * @return an <tt>Iterator</tt> over a list of the search strings of this
     * contact
     */
    @Override
    public Iterator<String> getSearchStrings()
    {
        return searchStrings.iterator();
    }

    /**
     * Sets the list of the search strings of this contact.
     *
     * @param strings the list of search strings of this contact
     */
    public void setSearchStrings(List<String> strings)
    {
        this.searchStrings = strings;
    }

    /**
     * Returns the default <tt>ContactDetail</tt> to use for any operations
     * depending to the given <tt>OperationSet</tt> class.
     *
     * @param opSetClass the <tt>OperationSet</tt> class we're interested in
     * @return the default <tt>ContactDetail</tt> to use for any operations
     * depending to the given <tt>OperationSet</tt> class
     */
    @Override
    public UIContactDetail getDefaultContactDetail(
        Class<? extends OperationSet> opSetClass)
    {
        List<UIContactDetail> opSetDetails = contactDetails.get(opSetClass);

        if (opSetDetails != null && opSetDetails.size() > 0)
            return opSetDetails.get(0);

        return null;
    }

    /**
     * Returns a list of all <tt>UIContactDetail</tt>s corresponding to the
     * given <tt>OperationSet</tt> class.
     *
     * @param opSetClass the <tt>OperationSet</tt> class we're looking for
     * @return a list of all <tt>UIContactDetail</tt>s corresponding to the
     * given <tt>OperationSet</tt> class
     */
    @Override
    public List<UIContactDetail> getContactDetailsForOperationSet(
        Class<? extends OperationSet> opSetClass)
    {
        return contactDetails.get(opSetClass);
    }

    /**
     * Returns a list of all <tt>UIContactDetail</tt>s within this
     * <tt>UIContact</tt>.
     *
     * @return a list of all <tt>UIContactDetail</tt>s within this
     * <tt>UIContact</tt>
     */
    @Override
    public List<UIContactDetail> getContactDetails()
    {
        List<UIContactDetail> details = new ArrayList<UIContactDetail>();

        Iterator<List<UIContactDetail>> listsIter
            = contactDetails.values().iterator();

        while (listsIter.hasNext())
        {
            details.addAll(listsIter.next());
        }

        return details;
    }

    /**
     * Returns all custom action buttons for this notification contact.
     *
     * @return a list of all custom action buttons for this notification contact
     */
    @Override
    public Collection<SIPCommButton> getContactCustomActionButtons()
    {
        return customActionButtons;
    }

    /**
     * Sets all custom action buttons for this notification contact.
     *
     * @param buttonsList a list of all custom action buttons for this
     * notification contact
     */
    public void setContactCustomActionButtons(
        Collection<SIPCommButton> buttonsList)
    {
        this.customActionButtons = buttonsList;
    }

    /**
     * Adds the given <tt>detailsList</tt> for the given <tt>opSetClass</tt>.
     *
     * @param opSetClass the class of the OperationSet
     * @param detailsList the list of contact details supported for the given
     * operation set
     */
    public void addContactDetails(  Class<? extends OperationSet> opSetClass,
                                    List<UIContactDetail> detailsList)
    {
        if (contactDetails == null)
            contactDetails = new HashMap<   Class<? extends OperationSet>,
                                            List<UIContactDetail>>();

        contactDetails.put(opSetClass, detailsList);
    }

    /**
     * Sets the contact details map.
     *
     * @param contactDetailsMap the map of contact details and corresponding
     * supported operation set
     */
    public void setContactDetails(  Map <Class<? extends OperationSet>,
                                    List<UIContactDetail>> contactDetailsMap)
    {
        contactDetails = contactDetailsMap;
    }

    /**
     * Returns the corresponding <tt>ContactNode</tt> from the contact list
     * component.
     * @return the corresponding <tt>ContactNode</tt>
     */
    @Override
    public ContactNode getContactNode()
    {
        return contactNode;
    }

    /**
     * Sets the corresponding <tt>ContactNode</tt>.
     * @param contactNode the corresponding <tt>ContactNode</tt>
     */
    @Override
    public void setContactNode(ContactNode contactNode)
    {
        this.contactNode = contactNode;
    }

    /**
     * Returns the general status icon of the given UIContact.
     *
     * @return PresenceStatus the most "available" status from all
     * sub-contact statuses.
     */
    @Override
    public ImageIcon getStatusIcon()
    {
        return statusIcon;
    }

    /**
     * Sets the general status icon of this contact.
     *
     * @return PresenceStatus The most "available" status from all
     * sub-contact statuses.
     */
    public void setStatusIcon(ImageIcon statusIcon)
    {
        this.statusIcon = statusIcon;
    }

    /**
     * Gets the avatar of a specific <tt>UIContact</tt> in the form of an
     * <tt>ImageIcon</tt> value.
     *
     * @param isSelected indicates if the contact is selected
     * @param width the desired icon width
     * @param height the desired icon height
     * @return an <tt>ImageIcon</tt> which represents the avatar of the
     * specified <tt>MetaContact</tt>
     */
    @Override
    public ImageIcon getScaledAvatar(boolean isSelected, int width, int height)
    {
        if (avatarIcon != null
            && (avatarIcon.getIconWidth() > width
                || avatarIcon.getIconHeight() > height))
        {
            avatarIcon = ImageUtils.getScaledRoundedIcon(
                avatarIcon.getImage(), width, height);
        }
        return avatarIcon;
    }

    /**
     * Sets the avatar of this <tt>UIContact</tt> in the form of an
     * <tt>ImageIcon</tt> value.
     *
     * @param avatarIcon the avatar icon of this contact
     */
    public void setAvatar(ImageIcon avatarIcon)
    {
        this.avatarIcon = avatarIcon;
    }
}
