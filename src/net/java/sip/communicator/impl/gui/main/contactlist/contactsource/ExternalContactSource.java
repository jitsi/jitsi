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
import java.awt.event.*;
import java.util.*;
import java.util.List;

import javax.swing.*;
import javax.swing.tree.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.main.contactlist.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.contactsource.*;
import net.java.sip.communicator.service.customcontactactions.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

import org.osgi.framework.*;

/**
 * The <tt>ExternalContactSource</tt> is the UI abstraction of the
 * <tt>ContactSourceService</tt>.
 *
 * @author Yana Stamcheva
 * @author Hristo Terezov
 */
public class ExternalContactSource
    implements UIContactSource
{
    /**
     * The data key of the SourceContactDescriptor object used to store a
     * reference to this object in its corresponding Sourcecontact.
     */
    public static final String UI_CONTACT_DATA_KEY
        = SourceUIContact.class.getName() + ".uiContactDescriptor";

    /**
     * The <tt>SourceUIGroup</tt> containing all contacts from this source.
     */
    private final SourceUIGroup sourceUIGroup;

    /**
     * The contact source.
     */
    private final ContactSourceService contactSource;

    /**
     * The current custom action contact.
     */
    private static SourceContact customActionContact;

    /**
     * The list of action buttons for this source contact.
     */
    private static Map<ContactAction<SourceContact>, SIPCommButton>
                                    customContactActionButtons;

    /**
     * The list of right menu action items for this source contact.
     */
    private static Map<ContactActionMenuItem<SourceContact>, JMenuItem>
                                    customContactActionMenuItems;

    /**
     * The list of action buttons for this source service.
     */
    private Map<ContactAction<ContactSourceService>, SIPCommButton>
                                    customServiceActionButtons;

    private final JTree contactListTree;

    /**
     * The index of the contact source used to order the contact sources.
     */
    private int contactSourceIndex;

    /**
     * The list of right menu action items for this group.
     */
    private Map
        <ContactActionMenuItem<ContactSourceService>, JMenuItem>
            customGroupActionMenuItems;

    /**
     * Creates an <tt>ExternalContactSource</tt> based on the given
     * <tt>ContactSourceService</tt>.
     *
     * @param contactSource the <tt>ContactSourceService</tt>, on which this
     * <tt>ExternalContactSource</tt> is based
     */
    public ExternalContactSource(   ContactSourceService contactSource,
                                    JTree contactListTree)
    {
        this.contactSource = contactSource;
        this.contactListTree = contactListTree;
        contactSourceIndex = contactSource.getIndex();

        sourceUIGroup = new SourceUIGroup(contactSource.getDisplayName(), this);
    }

    /**
     * Returns the corresponding <tt>ContactSourceService</tt>.
     *
     * @return the corresponding <tt>ContactSourceService</tt>
     */
    public ContactSourceService getContactSourceService()
    {
        return contactSource;
    }

    /**
     * Returns the UI group for this contact source. There's only one group
     * descriptor per external source.
     *
     * @return the group descriptor
     */
    public UIGroup getUIGroup()
    {
        return sourceUIGroup;
    }

    /**
     * Returns the <tt>UIContact</tt> corresponding to the given
     * <tt>sourceContact</tt>.
     *
     * @param sourceContact the <tt>SourceContact</tt>, for which we search a
     * corresponding <tt>UIContact</tt>
     * @return the <tt>UIContact</tt> corresponding to the given
     * <tt>sourceContact</tt>
     */
    public UIContact createUIContact(SourceContact sourceContact)
    {
        SourceUIContact descriptor
            = new SourceUIContact(sourceContact, sourceUIGroup);

        sourceContact.setData(UI_CONTACT_DATA_KEY, descriptor);

        return descriptor;
    }

    /**
     * Removes the <tt>UIContact</tt> from the given <tt>sourceContact</tt>.
     * @param sourceContact the <tt>SourceContact</tt>, which corresponding UI
     * contact we would like to remove
     */
    public void removeUIContact(SourceContact sourceContact)
    {
        sourceContact.setData(UI_CONTACT_DATA_KEY, null);
    }

    /**
     * Returns the <tt>UIContact</tt> corresponding to the given
     * <tt>SourceContact</tt>.
     * @param sourceContact the <tt>SourceContact</tt>, which corresponding UI
     * contact we're looking for
     * @return the <tt>UIContact</tt> corresponding to the given
     * <tt>MetaContact</tt>
     */
    public UIContact getUIContact(SourceContact sourceContact)
    {
        return (UIContact) sourceContact.getData(UI_CONTACT_DATA_KEY);
    }

    /**
     * Returns all custom action buttons for this meta contact.
     *
     * @return a list of all custom action buttons for this meta contact
     */
    public Collection<SIPCommButton> getContactCustomActionButtons(
        final SourceContact sourceContact)
    {
        customActionContact = sourceContact;

        if (customContactActionButtons == null)
            initCustomContactActionButtons();

        Iterator<ContactAction<SourceContact>> customActionsIter
            = customContactActionButtons.keySet().iterator();

        Collection<SIPCommButton> availableCustomActionButtons
            = new LinkedList<SIPCommButton>();

        while (customActionsIter.hasNext())
        {
            ContactAction<SourceContact> contactAction
                = customActionsIter.next();

            SIPCommButton actionButton =
                customContactActionButtons.get(contactAction);

            if (isContactActionVisible( contactAction, sourceContact))
            {
                availableCustomActionButtons.add(actionButton);
            }
        }

        return availableCustomActionButtons;
    }

    /**
     * Returns all custom action menu items for this contact.
     *
     * @param initActions if <tt>true</tt> the actions will be reloaded.
     * @param sourceContact the contact.
     * @return a list of all custom action menu items for this contact.
     */
    public Collection<JMenuItem> getContactCustomActionMenuItems(
        final SourceContact sourceContact, boolean initActions)
    {
        customActionContact = sourceContact;

        if (initActions || (customContactActionMenuItems == null))
            initCustomContactActionMenuItems();

        Iterator<ContactActionMenuItem<SourceContact>> customActionsIter
            = customContactActionMenuItems.keySet().iterator();

        Collection<JMenuItem> availableCustomActionMenuItems
            = new LinkedList<JMenuItem>();

        while (customActionsIter.hasNext())
        {
            ContactActionMenuItem<SourceContact> contactAction
                = customActionsIter.next();

            JMenuItem actionMenuItem =
                customContactActionMenuItems.get(contactAction);

            if (isContactActionVisible( contactAction, sourceContact))
            {
                availableCustomActionMenuItems.add(actionMenuItem);
            }
        }

        return availableCustomActionMenuItems;
    }

    /**
     * Returns all custom action menu items for the contact source.
     *
     * @param initActions if <tt>true</tt> the actions will be reloaded.
     * @return a list of all custom action menu items for the contact source.
     */
    public Collection<JMenuItem> getGroupCustomActionMenuItems(
        boolean initActions)
    {
        if (initActions || (customGroupActionMenuItems == null))
            initCustomGroupActionMenuItems();

        Iterator<ContactActionMenuItem<ContactSourceService>> customActionsIter
            = customGroupActionMenuItems.keySet().iterator();

        Collection<JMenuItem> availableCustomActionMenuItems
            = new LinkedList<JMenuItem>();

        while (customActionsIter.hasNext())
        {
            ContactActionMenuItem<ContactSourceService> contactAction
                = customActionsIter.next();

            JMenuItem actionMenuItem =
                customGroupActionMenuItems.get(contactAction);

            if (isContactActionVisible(contactAction, contactSource))
            {
                availableCustomActionMenuItems.add(actionMenuItem);
            }
        }

        return availableCustomActionMenuItems;
    }

    /**
     * Indicates if the given <tt>ContactAction</tt> should be visible for the
     * given <tt>SourceContact</tt>.
     *
     * @param contactAction the <tt>ContactAction</tt> to verify
     * if the given action should be visible
     * @return <tt>true</tt> if the given <tt>ContactAction</tt> is visible for
     * the given <tt>SourceContact</tt>, <tt>false</tt> - otherwise
     */
    private static boolean isContactActionVisible(
                            ContactAction<SourceContact> contactAction,
                            SourceContact contact)
    {
        if (contactAction.isVisible(contact))
            return true;

        return false;
    }

    /**
     * Indicates if the given <tt>ContactAction</tt> should be visible for the
     * given <tt>ContactSourceService</tt>.
     *
     * @param contactAction the <tt>ContactAction</tt> to verify
     * if the given action should be visible
     * @param contactSource the given <tt>ContactSourceService</tt>
     * @return <tt>true</tt> if the given <tt>ContactAction</tt> is visible for
     * the given <tt>ContactSourceService</tt>, <tt>false</tt> - otherwise
     */
    private static boolean isContactActionVisible(
        ContactActionMenuItem<ContactSourceService> contactAction,
        ContactSourceService contactSource)
    {
        if (contactAction.isVisible(contactSource))
            return true;

        return false;
    }

    /**
     * Indicates if the given <tt>ContactActionMenuItem</tt> should be visible
     * for the given <tt>SourceContact</tt>.
     *
     * @param contactAction the <tt>ContactActionMenuItem</tt> to verify
     * if the given action should be visible
     * @return <tt>true</tt> if the given <tt>ContactActionMenuItem</tt> is
     * visible for the given <tt>SourceContact</tt>, <tt>false</tt> - otherwise
     */
    private static boolean isContactActionVisible(
                            ContactActionMenuItem<SourceContact> contactAction,
                            SourceContact contact)
    {
        if (contactAction.isVisible(contact))
            return true;

        return false;
    }

    /**
     * Initializes custom action buttons for this contact source.
     */
    private void initCustomContactActionButtons()
    {
        customContactActionButtons
            = new LinkedHashMap<ContactAction<SourceContact>, SIPCommButton>();
        for (CustomContactActionsService<SourceContact> ccas
                : getContactActionsServices())
        {
            Iterator<ContactAction<SourceContact>> actionIterator
                = ccas.getCustomContactActions();

            if (actionIterator!= null)
            {
                while (actionIterator.hasNext())
                {
                    final ContactAction<SourceContact> ca
                        = actionIterator.next();
                    initActionButton(ca, SourceContact.class);
                }
            }
        }
    }

    /**
     * Initializes custom action menu items for this contact source.
     */
    private void initCustomContactActionMenuItems()
    {
        customContactActionMenuItems
            = new LinkedHashMap<ContactActionMenuItem<SourceContact>, JMenuItem>();
        for (CustomContactActionsService<SourceContact> ccas
                : getContactActionsServices())
        {
            Iterator<ContactActionMenuItem<SourceContact>> actionIterator
                = ccas.getCustomContactActionsMenuItems();

            if (actionIterator!= null)
            {
                while (actionIterator.hasNext())
                {
                    final ContactActionMenuItem<SourceContact> ca
                        = actionIterator.next();
                    initActionMenuItem(ca);
                }
            }
        }
    }

    /**
     * Initializes custom action menu items for this contact source.
     */
    private void initCustomGroupActionMenuItems()
    {
        customGroupActionMenuItems
            = new LinkedHashMap<ContactActionMenuItem
                <ContactSourceService>, JMenuItem>();
        for (CustomContactActionsService<ContactSourceService> ccas
                : getGroupActionsServices())
        {
            Iterator<ContactActionMenuItem<ContactSourceService>> actionIterator
                = ccas.getCustomContactActionsMenuItems();

            if (actionIterator!= null)
            {
                while (actionIterator.hasNext())
                {
                    final ContactActionMenuItem<ContactSourceService> ca
                        = actionIterator.next();
                    initGroupActionMenuItem(ca);
                }
            }
        }
    }

    /**
     * Initializes custom action buttons for this source service.
     */
    private void initCustomServiceActionButtons()
    {
        customServiceActionButtons =
            new LinkedHashMap<
                    ContactAction<ContactSourceService>, SIPCommButton>();

        for (CustomContactActionsService<ContactSourceService> ccas
                : getCustomActionsContactServices())
        {
            Iterator<ContactAction<ContactSourceService>> actionIterator
                = ccas.getCustomContactActions();

            while (actionIterator!= null && actionIterator.hasNext())
            {
                final ContactAction<ContactSourceService> ca = actionIterator.next();
                initActionButton(ca, ContactSourceService.class);
            }
        }
    }

    /**
     * Initializes an action button.
     *
     * @param ca the <tt>ContactAction</tt> corresponding to the button.
     */
    private <T> void initActionButton(
            final ContactAction<T> ca,
            final Class<T> contactSourceClass)
    {
        SIPCommButton actionButton;

        if(contactSourceClass.equals(SourceContact.class))
            actionButton = customContactActionButtons.get(ca);
        else if(contactSourceClass.equals(ContactSourceService.class))
            actionButton = customServiceActionButtons.get(ca);
        else
            return;

        if (actionButton == null)
        {
            actionButton = new SIPCommButton();

            actionButton.setToolTipText(ca.getToolTipText());

            actionButton.setIconImage(
                new ImageIcon(ca.getIcon()).getImage());
            actionButton.setRolloverIcon(
                new ImageIcon(ca.getRolloverIcon()).getImage());
            actionButton.setPressedIcon(
                new ImageIcon(ca.getPressedIcon()).getImage());

            actionButton.addActionListener(new ActionListener()
            {
                public void actionPerformed(ActionEvent event)
                {
                    try
                    {
                        JButton button = (JButton)event.getSource();
                        Point location = new Point(button.getX(),
                            button.getY() + button.getHeight());

                        SwingUtilities.convertPointToScreen(
                            location, contactListTree);

                        TreePath selectionPath
                            = contactListTree.getSelectionPath();

                        if (selectionPath != null)
                        {
                            location.y
                                += contactListTree
                                    .getPathBounds(selectionPath)
                                        .y;
                        }

                        if(contactSourceClass.equals(SourceContact.class))
                        {
                            @SuppressWarnings("unchecked")
                            T t = (T) customActionContact;

                            ca.actionPerformed(t, location.x, location.y);
                        }
                        else if(contactSourceClass.equals(
                                ContactSourceService.class))
                        {
                            @SuppressWarnings("unchecked")
                            T t = (T) contactSource;

                            ca.actionPerformed(t, location.x, location.y);
                        }
                    }
                    catch (OperationFailedException e)
                    {
                        new ErrorDialog(null,
                            GuiActivator.getResources()
                                .getI18NString("service.gui.ERROR"),
                            e.getMessage());
                    }
                }
            });

            if(contactSourceClass.equals(SourceContact.class))
            {
                @SuppressWarnings("unchecked")
                ContactAction<SourceContact> casc
                    = (ContactAction<SourceContact>) ca;

                customContactActionButtons.put(casc, actionButton);
            }
            else if(contactSourceClass.equals(ContactSourceService.class))
            {
                @SuppressWarnings("unchecked")
                ContactAction<ContactSourceService> cacss
                    = (ContactAction<ContactSourceService>) ca;

                customServiceActionButtons.put(cacss, actionButton);
            }
        }
    }

    /**
     * Initializes an action menu item.
     *
     * @param ca the <tt>ContactActionMenuItem</tt> corresponding to the item.
     */
    private void initActionMenuItem(
            final ContactActionMenuItem<SourceContact> ca)
    {
        JMenuItem actionMenuItem;

        actionMenuItem = customContactActionMenuItems.get(ca);

        if (actionMenuItem == null)
        {
            if(ca.isCheckBox())
            {
                actionMenuItem = new JCheckBoxMenuItem();
            }
            else
            {
                actionMenuItem = new JMenuItem();
            }

            actionMenuItem.setText(ca.getText(customActionContact));

            actionMenuItem.setMnemonic(ca.getMnemonics());

            byte[] icon = ca.getIcon();
            if(icon != null)
                actionMenuItem.setIcon(
                    new ImageIcon(icon));

            actionMenuItem.setSelected(ca.isSelected(customActionContact));
            actionMenuItem.setEnabled(ca.isEnabled(customActionContact));

            actionMenuItem.addActionListener(new ActionListener()
            {
                public void actionPerformed(ActionEvent event)
                {
                    try
                    {
                        ca.actionPerformed(customActionContact);
                    }
                    catch (OperationFailedException e)
                    {
                        new ErrorDialog(null,
                            GuiActivator.getResources()
                                .getI18NString("service.gui.ERROR"),
                            e.getMessage());
                    }
                }
            });

            customContactActionMenuItems.put(ca, actionMenuItem);
        }
    }

    /**
     * Initializes an action menu item.
     *
     * @param ca the <tt>ContactActionMenuItem</tt> corresponding to the item.
     */
    private void initGroupActionMenuItem(
            final ContactActionMenuItem<ContactSourceService> ca)
    {
        JMenuItem actionMenuItem;

        actionMenuItem = customGroupActionMenuItems.get(ca);

        if (actionMenuItem == null)
        {
            if(ca.isCheckBox())
            {
                actionMenuItem = new JCheckBoxMenuItem();
            }
            else
            {
                actionMenuItem = new JMenuItem();
            }

            actionMenuItem.setText(ca.getText(contactSource));

            actionMenuItem.setMnemonic(ca.getMnemonics());

            byte[] icon = ca.getIcon();
            if(icon != null)
                actionMenuItem.setIcon(
                    new ImageIcon(icon));

            actionMenuItem.setSelected(ca.isSelected(contactSource));
            actionMenuItem.setEnabled(ca.isEnabled(contactSource));

            actionMenuItem.addActionListener(new ActionListener()
            {
                public void actionPerformed(ActionEvent event)
                {
                    try
                    {
                        ca.actionPerformed(contactSource);
                    }
                    catch (OperationFailedException e)
                    {
                        new ErrorDialog(null,
                            GuiActivator.getResources()
                                .getI18NString("service.gui.ERROR"),
                            e.getMessage());
                    }
                }
            });

            customGroupActionMenuItems.put(ca, actionMenuItem);
        }
    }

    /**
     * Returns a list of all custom contact action services.
     *
     * @return a list of all custom contact action services.
     */
    @SuppressWarnings ("unchecked")
    private List<CustomContactActionsService<SourceContact>>
        getContactActionsServices()
    {
        List<CustomContactActionsService<SourceContact>>
            contactActionsServices
                = new ArrayList<CustomContactActionsService<SourceContact>>();

        ServiceReference[] serRefs = null;
        try
        {
            // get all registered provider factories
            serRefs
                = GuiActivator.bundleContext.getServiceReferences(
                    CustomContactActionsService.class.getName(), null);
        }
        catch (InvalidSyntaxException e)
        {}

        if (serRefs != null)
        {
            for (ServiceReference serRef : serRefs)
            {
                CustomContactActionsService<?> customActionService
                    = (CustomContactActionsService<?>)
                            GuiActivator.bundleContext.getService(serRef);

                if (customActionService.getContactSourceClass()
                        .equals(SourceContact.class))
                {
                    contactActionsServices.add((CustomContactActionsService<SourceContact>)customActionService);
                }
            }
        }

        GuiActivator.bundleContext.addServiceListener(
            new ContactActionsServiceListener<SourceContact>(
                    SourceContact.class));

        return contactActionsServices;
    }

    /**
     * Returns a list of all custom contact action services.
     *
     * @return a list of all custom contact action services.
     */
    @SuppressWarnings ("unchecked")
    private List<CustomContactActionsService<ContactSourceService>>
        getGroupActionsServices()
    {
        List<CustomContactActionsService<ContactSourceService>>
            contactActionsServices
                = new ArrayList<CustomContactActionsService<ContactSourceService>>();

        ServiceReference[] serRefs = null;
        try
        {
            // get all registered provider factories
            serRefs
                = GuiActivator.bundleContext.getServiceReferences(
                    CustomContactActionsService.class.getName(), null);
        }
        catch (InvalidSyntaxException e)
        {}

        if (serRefs != null)
        {
            for (ServiceReference serRef : serRefs)
            {
                CustomContactActionsService<?> customActionService
                    = (CustomContactActionsService<?>)
                            GuiActivator.bundleContext.getService(serRef);

                if (customActionService.getContactSourceClass()
                        .equals(ContactSourceService.class))
                {
                    contactActionsServices.add(
                        (CustomContactActionsService<ContactSourceService>)
                            customActionService);
                }
            }
        }

        GuiActivator.bundleContext.addServiceListener(
            new ContactActionsServiceListener<SourceContact>(
                    SourceContact.class));

        return contactActionsServices;
    }

    /**
     * Returns a list of all custom contact action services.
     *
     * @return a list of all custom contact action services.
     */
    @SuppressWarnings ("unchecked")
    private List<CustomContactActionsService<ContactSourceService>>
        getCustomActionsContactServices()
    {
        List<CustomContactActionsService<ContactSourceService>>
            contactActionsServices
                = new ArrayList<CustomContactActionsService
                                    <ContactSourceService>>();

        ServiceReference[] serRefs = null;
        try
        {
            // get all registered provider factories
            serRefs
                = GuiActivator.bundleContext.getServiceReferences(
                    CustomContactActionsService.class.getName(), null);
        }
        catch (InvalidSyntaxException e)
        {}

        if (serRefs != null)
        {
            for (ServiceReference serRef : serRefs)
            {
                CustomContactActionsService<ContactSourceService>
                    customActionService
                        = (CustomContactActionsService<ContactSourceService>)
                            GuiActivator.bundleContext.getService(serRef);

                if (customActionService.getContactSourceClass()
                        .equals(ContactSourceService.class))
                {
                    contactActionsServices.add(customActionService);
                }
            }
        }

        GuiActivator.bundleContext.addServiceListener(
            new ContactActionsServiceListener<ContactSourceService>(
                    ContactSourceService.class));

        return contactActionsServices;
    }

    /**
     * Sets the contact source index.
     *
     * @param contactSourceIndex the contact source index to set
     */
    public void setContactSourceIndex(int contactSourceIndex)
    {
        this.contactSourceIndex = contactSourceIndex;
    }

    /**
     * The <tt>SourceUIGroup</tt> is the implementation of the UIGroup for the
     * <tt>ExternalContactSource</tt>. It takes the name of the source and
     * sets it as a group name.
     */
    public class SourceUIGroup
        extends UIGroupImpl
    {
        /**
         * The display name of the group.
         */
        private final String displayName;

        /**
         * The corresponding group node.
         */
        private GroupNode groupNode;

        private ExternalContactSource parentUISource;

        /**
         * Creates an instance of <tt>SourceUIGroup</tt>.
         * @param name the name of the group
         */
        public SourceUIGroup(   String name,
                                ExternalContactSource parentUISource)
        {
            this.displayName = name;
            this.parentUISource = parentUISource;
        }

        public ExternalContactSource getParentUISource()
        {
            return parentUISource;
        }

        /**
         * Returns null to indicate that this group doesn't have a parent group
         * and can be added directly to the root group.
         * @return null
         */
        @Override
        public UIGroup getParentGroup()
        {
            return null;
        }

        /**
         * Returns -1 to indicate that this group doesn't have a source index.
         * @return -1
         */
        @Override
        public int getSourceIndex()
        {
            if (contactSourceIndex >= 0)
                return contactSourceIndex * MAX_GROUPS;

            if (contactSource.getType() == ContactSourceService.HISTORY_TYPE)
                return Integer.MAX_VALUE - MAX_GROUPS;

            return Integer.MAX_VALUE - MAX_GROUPS - 1;
        }

        /**
         * Returns <tt>false</tt> to indicate that this group is always opened.
         * @return false
         */
        @Override
        public boolean isGroupCollapsed()
        {
            return ConfigurationUtils.isContactListGroupCollapsed(getId());
        }

        /**
         * Returns the display name of this group.
         * @return the display name of this group
         */
        @Override
        public String getDisplayName()
        {
            return displayName;
        }

        /**
         * Returns -1 to indicate that the child count is unknown.
         * @return -1
         */
        @Override
        public int countChildContacts()
        {
            return -1;
        }

        /**
         * Returns -1 to indicate that the child count is unknown.
         * @return -1
         */
        @Override
        public int countOnlineChildContacts()
        {
            return -1;
        }

        /**
         * Returns the display name of the group.
         * @return the display name of the group
         */
        @Override
        public Object getDescriptor()
        {
            return contactSource;
        }

        /**
         * Returns null to indicate that this group doesn't have an identifier.
         * @return null
         */
        @Override
        public String getId()
        {
            return getDisplayName();
        }

        /**
         * Returns the corresponding <tt>GroupNode</tt>.
         * @return the corresponding <tt>GroupNode</tt>
         */
        @Override
        public GroupNode getGroupNode()
        {
            return groupNode;
        }

        /**
         * Sets the given <tt>groupNode</tt>.
         * @param groupNode the <tt>GroupNode</tt> to set
         */
        @Override
        public void setGroupNode(GroupNode groupNode)
        {
            this.groupNode = groupNode;
        }

        /**
         * Returns the right button menu for this group.
         * @return null
         */
        @Override
        public JPopupMenu getRightButtonMenu()
        {
            if(getGroupCustomActionMenuItems(false).isEmpty())
                return null;
            return new SourceGroupRightButtonMenu();
        }

        /**
         * Returns all custom action buttons for this group.
         *
         * @return a list of all custom action buttons for this group
         */
        @Override
        public Collection<SIPCommButton> getCustomActionButtons()
        {
            if (customServiceActionButtons == null)
                initCustomServiceActionButtons();

            Iterator<ContactAction<ContactSourceService>> customActionsIter
                = customServiceActionButtons.keySet().iterator();

            Collection<SIPCommButton> availableCustomActionButtons
                = new LinkedList<SIPCommButton>();

            while (customActionsIter.hasNext())
            {
                ContactAction<ContactSourceService> contactAction
                    = customActionsIter.next();

                SIPCommButton actionButton =
                    customServiceActionButtons.get(contactAction);

                if (contactAction.isVisible(contactSource))
                {
                    availableCustomActionButtons.add(actionButton);
                }
            }

            return availableCustomActionButtons;
        }
    }

    /**
     * Class for the external contact sources right button menu. It shows only
     * the defined custom actions.
     */
    private class SourceGroupRightButtonMenu
        extends SIPCommPopupMenu
    {
        /**
         * Serial version UID.
         */
        private static final long serialVersionUID = 0L;

        /**
         * Creates an instance of <tt>SourceContactRightButtonMenu</tt> by
         * specifying the <tt>SourceUIContact</tt>, for which this menu is
         * created.
         */
        public SourceGroupRightButtonMenu()
        {
            for(JMenuItem item : getGroupCustomActionMenuItems(true))
            {
                add(item);
            }
        }
    }

    /**
     * The <tt>ContactActionsServiceListener</tt> listens for service changes
     * in order to update the list of custom action buttons when a new
     * <tt>CustomContactActionsService</tt> is registered or unregistered.
     */
    private class ContactActionsServiceListener<T>
        implements ServiceListener
    {
        private final Class<T> contactSourceClass;

        ContactActionsServiceListener(Class<T> contactSourceClass)
        {
            this.contactSourceClass = contactSourceClass;
        }

        public void serviceChanged(ServiceEvent event)
        {
            ServiceReference serviceRef = event.getServiceReference();

            // if the event is caused by a bundle being stopped, we don't want to
            // know
            if (serviceRef.getBundle().getState() == Bundle.STOPPING)
                return;

            Object service = GuiActivator.bundleContext.getService(serviceRef);

            // we don't care if the source service is not a protocol provider
            if (!(service instanceof CustomContactActionsService))
                return;

            @SuppressWarnings("rawtypes")
            CustomContactActionsService cContactActionsService
                = (CustomContactActionsService) service;

            if(!cContactActionsService.getContactSourceClass().equals(
                    contactSourceClass))
                return;

            @SuppressWarnings("unchecked")
            Iterator<ContactAction<T>> actionIterator
                = cContactActionsService.getCustomContactActions();

            while (actionIterator!= null && actionIterator.hasNext())
            {
                final ContactAction<T> ca = actionIterator.next();

                switch (event.getType())
                {
                case ServiceEvent.REGISTERED:
                    initActionButton(ca, contactSourceClass);
                    break;
                case ServiceEvent.UNREGISTERING:
                    if(contactSourceClass.equals(SourceContact.class))
                    {
                        customContactActionButtons.remove(ca);
                    }
                    else if(contactSourceClass.equals(
                            ContactSourceService.class))
                    {
                        customServiceActionButtons.remove(ca);
                    }
                    break;
                }
            }
        }
    }
}
