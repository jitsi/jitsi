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

import java.awt.*;
import java.util.*;
import java.util.List;

import javax.swing.*;

import org.jitsi.service.resources.*;
import org.jitsi.util.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.main.contactlist.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.contactsource.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.globalstatus.*;
import net.java.sip.communicator.util.*;

/**
 * The <tt>SourceUIContact</tt> is the implementation of the UIContact for the
 * <tt>ExternalContactSource</tt>.
 *
 * @author Yana Stamcheva
 * @author Hristo Terezov
 */
public class SourceUIContact
    extends UIContactImpl
{
    /**
     * The corresponding <tt>SourceContact</tt>, on which this abstraction is
     * based.
     */
    private final SourceContact sourceContact;

    /**
     * The corresponding <tt>ContactNode</tt> in the contact list component.
     */
    private ContactNode contactNode;

    /**
     * The parent <tt>UIGroup</tt>.
     */
    private ExternalContactSource.SourceUIGroup uiGroup;

    /**
     * The search strings for this <tt>UIContact</tt>.
     */
    private final List<String> searchStrings = new LinkedList<String>();

    /**
     * Whether we should filter all call details only to numbers.
     */
    private static final String FILTER_CALL_DETAILS_TO_NUMBERS_PROP
        = "net.java.sip.communicator.impl.gui.main.contactlist.contactsource" +
            ".FILTER_CALL_DETAILS_TO_NUMBERS";

    /**
     * Creates an instance of <tt>SourceUIContact</tt> by specifying the
     * <tt>SourceContact</tt>, on which this abstraction is based and the
     * parent <tt>UIGroup</tt>.
     *
     * @param contact the <tt>SourceContact</tt>, on which this abstraction
     * is based
     * @param parentGroup the parent <tt>UIGroup</tt>
     */
    public SourceUIContact( SourceContact contact,
                            ExternalContactSource.SourceUIGroup parentGroup)
    {
        this.sourceContact = contact;
        this.uiGroup = parentGroup;

        if(contact.getContactDetails() != null)
            for(ContactDetail detail : contact.getContactDetails())
            {
                if(detail.getDetail() != null)
                    searchStrings.add(detail.getDetail());
            }

        searchStrings.add(contact.getDisplayName());

    }

    /**
     * Returns the display name of the underlying <tt>SourceContact</tt>.
     * @return the display name
     */
    @Override
    public String getDisplayName()
    {
        return sourceContact.getDisplayName();
    }

    /**
     * Returns the parent <tt>UIGroup</tt>.
     * @return the parent <tt>UIGroup</tt>
     */
    @Override
    public UIGroup getParentGroup()
    {
        return uiGroup;
    }

    /**
     * The parent group of source contacts could not be changed.
     *
     * @param parentGroup the parent group to set
     */
    @Override
    public void setParentGroup(UIGroup parentGroup) {}

    /**
     * Returns -1 to indicate that the source index of the underlying
     * <tt>SourceContact</tt> is unknown.
     * @return -1
     */
    @Override
    public int getSourceIndex()
    {
        int contactIndex = sourceContact.getIndex();
        int groupIndex = getParentGroup().getSourceIndex();
        return ((contactIndex == -1) ? -1 : 
            ((groupIndex == -1) ? contactIndex : groupIndex + contactIndex));
    }

    /**
     * Returns null to indicate unknown status of the underlying
     * <tt>SourceContact</tt>.
     * @return null
     */
    @Override
    public ImageIcon getStatusIcon()
    {
        PresenceStatus status = sourceContact.getPresenceStatus();

        if (status != null)
            return new ImageIcon(Constants.getStatusIcon(status));

        return new ImageIcon(GlobalStatusEnum.OFFLINE.getStatusIcon());
    }

    /**
     * Gets the avatar of a specific <tt>UIContact</tt> in the form of an
     * <tt>ImageIcon</tt> value.
     *
     * @return a byte array representing the avatar of this <tt>UIContact</tt>
     */
    @Override
    public byte[] getAvatar()
    {
        return sourceContact.getImage();
    }

    /**
     * Returns the image corresponding to the underlying <tt>SourceContact</tt>.
     * @param isSelected indicates if the contact is currently selected in the
     * contact list component
     * @param width the desired image width
     * @param height the desired image height
     * @return the image
     */
    @Override
    public ImageIcon getScaledAvatar(boolean isSelected, int width, int height)
    {
        byte[] image = sourceContact.getImage();

        if ((image != null) && (image.length > 0))
        {
            ImageIcon icon = new ImageIcon(image);

            if (icon.getIconWidth() > width || icon.getIconHeight() > height)
            {
                icon
                    = ImageUtils.getScaledRoundedIcon(
                            icon.getImage(),
                            width, height);
            }
            return icon;
        }
        else
            return null;
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
        List<UIContactDetail> details
            = getContactDetailsForOperationSet(opSetClass);

        if (details != null && !details.isEmpty())
            return details.get(0);
        return null;
    }

    /**
     * Returns the underlying <tt>SourceContact</tt> this abstraction is about.
     * @return the underlying <tt>SourceContact</tt>
     */
    @Override
    public Object getDescriptor()
    {
        return sourceContact;
    }

    /**
     * Returns the display details for the underlying <tt>SourceContact</tt>.
     * @return the display details for the underlying <tt>SourceContact</tt>
     */
    @Override
    public String getDisplayDetails()
    {
        return sourceContact.getDisplayDetails();
    }

    /**
     * Returns a list of all contained <tt>UIContactDetail</tt>s.
     *
     * @return a list of all contained <tt>UIContactDetail</tt>s
     */
    @Override
    public List<UIContactDetail> getContactDetails()
    {
        List<UIContactDetail> resultList
            = new LinkedList<UIContactDetail>();

        Iterator<ContactDetail> details
            = sourceContact.getContactDetails().iterator();

        while (details.hasNext())
        {
            ContactDetail detail = details.next();

            resultList.add(new SourceContactDetail(
                        detail,
                        getInternationalizedLabel(detail.getCategory()),
                        getInternationalizedLabels(
                            detail.getSubCategories().iterator()),
                        null, sourceContact));
        }
        return resultList;
    }

    /**
     * Returns a list of <tt>UIContactDetail</tt>s supporting the given
     * <tt>OperationSet</tt> class.
     * @param opSetClass the <tt>OperationSet</tt> class we're interested in
     * @return a list of <tt>UIContactDetail</tt>s supporting the given
     * <tt>OperationSet</tt> class
     */
    @Override
    public List<UIContactDetail> getContactDetailsForOperationSet(
        Class<? extends OperationSet> opSetClass)
    {
        List<UIContactDetail> resultList
            = new LinkedList<UIContactDetail>();

        Iterator<ContactDetail> details
            = sourceContact.getContactDetails().iterator();

        PhoneNumberI18nService phoneNumberService
            = GuiActivator.getPhoneNumberI18nService();
        boolean filterToNumbers
            = GuiActivator.getConfigurationService().getBoolean(
                FILTER_CALL_DETAILS_TO_NUMBERS_PROP, false);

        while (details.hasNext())
        {
            ContactDetail detail = details.next();

            List<Class<? extends OperationSet>> supportedOperationSets
                = detail.getSupportedOperationSets();

            if ((supportedOperationSets != null)
                    && supportedOperationSets.contains(opSetClass))
            {
                if(filterToNumbers
                    && opSetClass.equals(OperationSetBasicTelephony.class)
                    && !phoneNumberService.isPhoneNumber(
                            detail.getDetail()))
                {
                    continue;
                }

                resultList.add(new SourceContactDetail(
                    detail,
                    getInternationalizedLabel(detail.getCategory()),
                    getInternationalizedLabels(
                        detail.getSubCategories().iterator()),
                    opSetClass,
                    sourceContact));
            }
        }
        return resultList;
    }

    /**
     * Returns an <tt>Iterator</tt> over a list of strings, which can be used
     * to find this contact.
     * @return an <tt>Iterator</tt> over a list of search strings
     */
    @Override
    public Iterator<String> getSearchStrings()
    {
        return searchStrings.iterator();
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
        if (contactNode == null)
            uiGroup.getParentUISource().removeUIContact(sourceContact);
    }

    /**
     * The implementation of the <tt>UIContactDetail</tt> interface for the
     * external source <tt>ContactDetail</tt>s.
     */
    protected static class SourceContactDetail
        extends UIContactDetailImpl
    {
        /**
         * Creates an instance of <tt>SourceContactDetail</tt> by specifying
         * the underlying <tt>detail</tt> and the <tt>OperationSet</tt> class
         * for it.
         * @param detail the underlying <tt>ContactDetail</tt>
         * @param category detail category string
         * @param subCategories the detail list of sub-categories
         * @param opSetClass the <tt>OperationSet</tt> class for the
         * preferred protocol provider
         * @param sourceContact the source contact
         */
        public SourceContactDetail( ContactDetail detail,
                                    String category,
                                    Collection<String> subCategories,
                                    Class<? extends OperationSet> opSetClass,
                                    SourceContact sourceContact)
        {
            super(  detail.getDetail(),
                    detail.getDetail(),
                    category,
                    subCategories,
                    null,
                    null,
                    null,
                    detail);

            ContactSourceService contactSource
                = sourceContact.getContactSource();

            if (contactSource instanceof PrefixedContactSourceService)
            {
                String prefix = ((PrefixedContactSourceService) contactSource)
                    .getPhoneNumberPrefix();

                if (prefix != null)
                    setPrefix(prefix);
            }

            addPreferredProtocolProvider(opSetClass,
                detail.getPreferredProtocolProvider(opSetClass));
            addPreferredProtocol(opSetClass,
                detail.getPreferredProtocol(opSetClass));
        }

        /**
         * Creates an instance of <tt>SourceContactDetail</tt> by specifying
         * the underlying <tt>detail</tt> and the <tt>OperationSet</tt> class
         * for it.
         *
         * @param displayName the display name
         * @param sourceContact the source contact
         */
        public SourceContactDetail(String displayName,
                                   SourceContact sourceContact)
        {
            super(  displayName,
                    displayName,
                    null,
                    null,
                    null,
                    null,
                    null,
                    sourceContact);
        }

        /**
         * Returns null to indicate that this detail doesn't support presence.
         * @return null
         */
        @Override
        public PresenceStatus getPresenceStatus()
        {
            return null;
        }
    }

    /**
     * Returns the <tt>JPopupMenu</tt> opened on a right button click over this
     * <tt>SourceUIContact</tt>.
     * @return the <tt>JPopupMenu</tt> opened on a right button click over this
     * <tt>SourceUIContact</tt>
     */
    @Override
    public JPopupMenu getRightButtonMenu()
    {
        return new SourceContactRightButtonMenu(this);
    }

    /**
     * Returns the tool tip opened on mouse over.
     * @return the tool tip opened on mouse over
     */
    @Override
    public ExtendedTooltip getToolTip()
    {
        ExtendedTooltip tip = new ExtendedTooltip(true);

        byte[] avatarImage = sourceContact.getImage();

        if (avatarImage != null && avatarImage.length > 0)
            tip.setImage(new ImageIcon(avatarImage));

        tip.setTitle(sourceContact.getDisplayName());

        String displayDetails = getDisplayDetails();

        if (displayDetails != null)
            tip.addLine(new JLabel[]{new JLabel(getDisplayDetails())});


        try
        {
            List<ContactDetail> details = sourceContact.getContactDetails(
                            ContactDetail.Category.Phone);

            if (details != null && details.size() > 0)
                addDetailsToToolTip(
                        details,
                        GuiActivator.getResources()
                            .getI18NString("service.gui.PHONES"),
                        tip);

            details = sourceContact.getContactDetails(
                ContactDetail.Category.Email);

            if (details != null && details.size() > 0)
                addDetailsToToolTip(
                        details,
                        GuiActivator.getResources()
                            .getI18NString("service.gui.EMAILS"),
                        tip);

            details = sourceContact.getContactDetails(
                ContactDetail.Category.InstantMessaging);

            if (details != null && details.size() > 0)
                addDetailsToToolTip(
                        details,
                        GuiActivator.getResources()
                            .getI18NString("service.gui.INSTANT_MESSAGINGS"),
                        tip);
        }
        catch (OperationNotSupportedException e)
        {
            List<ContactDetail> telDetails
                = sourceContact.getContactDetails(
                    OperationSetBasicTelephony.class);
            // if there is no telephony
            if(telDetails == null || telDetails.isEmpty())
                return tip;

            // Categories aren't supported. This is the case for history
            // records.
            List<ContactDetail> allDetails = sourceContact.getContactDetails();

            addDetailsToToolTip(allDetails,
                GuiActivator.getResources()
                    .getI18NString("service.gui.CALL_WITH"), tip);
        }

        return tip;
    }

    private void addDetailsToToolTip(   List<ContactDetail> details,
                                        String category,
                                        ExtendedTooltip toolTip)
    {
        ContactDetail contactDetail;

        JLabel categoryLabel = new JLabel(category, null, JLabel.LEFT);
        categoryLabel.setFont(categoryLabel.getFont().deriveFont(Font.BOLD));
        categoryLabel.setForeground(Color.DARK_GRAY);

        toolTip.addLine(null, " ");
        toolTip.addLine(new JLabel[]{categoryLabel});

        Iterator<ContactDetail> detailsIter = details.iterator();
        while (detailsIter.hasNext())
        {
            contactDetail = detailsIter.next();
            Collection<ContactDetail.SubCategory> subCategories
                = contactDetail.getSubCategories();

            JLabel[] jLabels = new JLabel[subCategories.size() + 1];
            int i = 0;
            if (subCategories != null && subCategories.size() > 0)
            {
                Iterator<ContactDetail.SubCategory> labelsIter
                    = subCategories.iterator();
                while(labelsIter.hasNext())
                {
                    JLabel label = new JLabel(
                        getInternationalizedLabel(labelsIter.next()));
                    label.setFont(label.getFont().deriveFont(Font.BOLD));
                    label.setForeground(Color.GRAY);

                    jLabels[i] = label;
                    i++;
                }
            }

            String labelText;

            if(ConfigurationUtils.isHideAddressInCallHistoryTooltipEnabled())
            {
                labelText = contactDetail.getDisplayName();
                if(StringUtils.isNullOrEmpty(labelText))
                    labelText = contactDetail.getDetail();
            }
            else
            {
                labelText = contactDetail.getDetail();
            }

            jLabels[i] = new JLabel(filterAddressDisplay(labelText));

            toolTip.addLine(jLabels);
        }
    }

    /**
     * Returns the internationalized category corresponding to the given
     * <tt>ContactDetail.Category</tt>.
     *
     * @param category the <tt>ContactDetail.SubCategory</tt>, for which we
     * would like to obtain an internationalized label
     * @return the internationalized label corresponding to the given category
     */
    protected String getInternationalizedLabel(ContactDetail.Category category)
    {
        if (category == null)
            return null;

        String categoryString = null;

        ResourceManagementService resources = GuiActivator.getResources();

        switch(category)
        {
        case Address:
            categoryString = resources.getI18NString("service.gui.ADDRESS");
            break;
        case Email:
            categoryString = resources.getI18NString("service.gui.EMAIL");
            break;
        case Personal:
            categoryString = resources.getI18NString("service.gui.PERSONAL");
            break;
        case Organization:
            categoryString = resources.getI18NString("service.gui.ORGANIZATION");
            break;
        case Phone:
            categoryString = resources.getI18NString("service.gui.PHONE");
            break;
        case InstantMessaging:
            categoryString = resources.getI18NString("service.gui.IM");
            break;
        }

        return categoryString;
    }

    /**
     * Returns a collection of internationalized string corresponding to the
     * given subCategories.
     *
     * @param subCategories an Iterator over a list of
     * <tt>ContactDetail.SubCategory</tt>s
     * @return a collection of internationalized string corresponding to the
     * given subCategories
     */
    protected Collection<String> getInternationalizedLabels(
        Iterator<ContactDetail.SubCategory> subCategories)
    {
        Collection<String> labels = new LinkedList<String>();

        while (subCategories.hasNext())
        {
            labels.add(getInternationalizedLabel(subCategories.next()));
        }

        return labels;
    }

    /**
     * Returns the internationalized label corresponding to the given category.
     *
     * @param subCategory the <tt>ContactDetail.SubCategory</tt>, for which we
     * would like to obtain an internationalized label
     * @return the internationalized label corresponding to the given category
     */
    protected String getInternationalizedLabel(
                                ContactDetail.SubCategory subCategory)
    {
        if (subCategory == null)
            return null;

        String label;
        ResourceManagementService resources = GuiActivator.getResources();

        switch(subCategory)
        {
        case City:
            label = resources.getI18NString("service.gui.CITY");
            break;
        case Country:
            label = resources.getI18NString("service.gui.COUNTRY");
            break;
        case Fax:
            label = resources.getI18NString("service.gui.FAX");
            break;
        case Home:
            label = resources.getI18NString("service.gui.HOME");
            break;
        case HomePage:
            label = resources.getI18NString("service.gui.HOME_PAGE");
            break;
        case JobTitle:
            label = resources.getI18NString("service.gui.JOB_TITLE");
            break;
        case LastName:
            label = resources.getI18NString("service.gui.LAST_NAME");
            break;
        case Mobile:
            label = resources.getI18NString("service.gui.MOBILE_PHONE");
            break;
        case Name:
            label = resources.getI18NString("service.gui.NAME");
            break;
        case Nickname:
            label = resources.getI18NString("service.gui.NICKNAME");
            break;
        case Other:
            label = resources.getI18NString("service.gui.OTHER");
            break;
        case PostalCode:
            label = resources.getI18NString("service.gui.POSTAL_CODE");
            break;
        case Street:
            label = resources.getI18NString("service.gui.STREET");
            break;
        case Work:
            label = resources.getI18NString("service.gui.WORK_PHONE");
            break;
        case AIM:
        case ICQ:
        case Jabber:
        case MSN:
        case Yahoo:
        case Skype:
        case GoogleTalk:
        case Facebook:
            label = subCategory.value();
            break;
        default:
            label = null;
            break;
        }

        return label;
    }

    /**
     * Returns all custom action buttons for this notification contact.
     *
     * @return a list of all custom action buttons for this notification contact
     */
    @Override
    public Collection<SIPCommButton> getContactCustomActionButtons()
    {
        if (sourceContact != null)
            return uiGroup.getParentUISource()
                    .getContactCustomActionButtons(sourceContact);

        return null;
    }
    
    /**
     * Returns all custom action menu items for this contact.
     *
     * @param initActions if <tt>true</tt> the actions will be reloaded.
     * @return a list of all custom action menu items for this contact.
     */
    @Override
    public Collection<JMenuItem> getContactCustomActionMenuItems(
        boolean initActions)
    {
        if (sourceContact != null)
            return uiGroup.getParentUISource()
                    .getContactCustomActionMenuItems(sourceContact, initActions);

        return null;
    }
}
