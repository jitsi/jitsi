/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.contactlist.contactsource;

import javax.swing.*;
import javax.swing.tree.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.main.contactlist.*;
import net.java.sip.communicator.service.contactsource.*;
import net.java.sip.communicator.service.customcontactactions.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.swing.*;
import org.osgi.framework.*;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

/**
 * The <tt>ExternalContactSource</tt> is the UI abstraction of the
 * <tt>ContactSourceService</tt>.
 *
 * @author Yana Stamcheva
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
                                    customActionButtons;

    private final JTree contactListTree;

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

        if (customActionButtons == null)
            initCustomActionButtons();

        Iterator<ContactAction<SourceContact>> customActionsIter
            = customActionButtons.keySet().iterator();

        Collection<SIPCommButton> availableCustomActionButtons
            = new LinkedList<SIPCommButton>();

        while (customActionsIter.hasNext())
        {
            ContactAction<SourceContact> contactAction
                = customActionsIter.next();

            SIPCommButton actionButton = customActionButtons.get(contactAction);

            if (isContactActionVisible( contactAction,
                                        sourceContact))
            {
                availableCustomActionButtons.add(actionButton);
            }
        }

        return availableCustomActionButtons;
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
     * Initializes custom action buttons for this contact source.
     */
    private void initCustomActionButtons()
    {
        customActionButtons = new LinkedHashMap
            <ContactAction<SourceContact>, SIPCommButton>();

        for (CustomContactActionsService<SourceContact> ccas
                : getContactActionsServices())
        {
            Iterator<ContactAction<SourceContact>> actionIterator
                = ccas.getCustomContactActions();

            while (actionIterator!= null && actionIterator.hasNext())
            {
                final ContactAction<SourceContact> ca = actionIterator.next();

                initActionButton(ca);
            }
        }
    }

    /**
     * Initializes an action button.
     *
     * @param ca the <tt>ContactAction</tt> corresponding to the button.
     */
    private void initActionButton(final ContactAction<SourceContact> ca)
    {
        SIPCommButton actionButton = customActionButtons.get(ca);

        if (actionButton == null)
        {
            actionButton = new SIPCommButton(
                new ImageIcon(ca.getIcon()).getImage(),
                new ImageIcon(ca.getPressedIcon()).getImage(),
                null);

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
                            location.y = location.y
                                + contactListTree.getPathBounds(
                                    selectionPath).y;

                        ca.actionPerformed(
                            customActionContact,
                            location.x,
                            location.y);
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

            customActionButtons.put(ca, actionButton);
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
                = new ArrayList<CustomContactActionsService
                                    <SourceContact>>();

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
                    contactActionsServices.add(
                        (CustomContactActionsService<SourceContact>)
                            customActionService);
                }
            }
        }

        GuiActivator.bundleContext.addServiceListener(
            new ContactActionsServiceListener());

        return contactActionsServices;
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
        public UIGroup getParentGroup()
        {
            return null;
        }

        /**
         * Returns -1 to indicate that this group doesn't have a source index.
         * @return -1
         */
        public int getSourceIndex()
        {
            int sourceIndex = contactSource.getIndex();

            if (sourceIndex >= 0)
                return sourceIndex;

            if (contactSource.getType() == ContactSourceService.HISTORY_TYPE)
                return Integer.MAX_VALUE;

            return Integer.MAX_VALUE - 1;
        }

        /**
         * Returns <tt>false</tt> to indicate that this group is always opened.
         * @return false
         */
        public boolean isGroupCollapsed()
        {
            return false;
        }

        /**
         * Returns the display name of this group.
         * @return the display name of this group
         */
        public String getDisplayName()
        {
            return displayName;
        }

        /**
         * Returns -1 to indicate that the child count is unknown.
         * @return -1
         */
        public int countChildContacts()
        {
            return -1;
        }

        /**
         * Returns -1 to indicate that the child count is unknown.
         * @return -1
         */
        public int countOnlineChildContacts()
        {
            return -1;
        }

        /**
         * Returns the display name of the group.
         * @return the display name of the group
         */
        public Object getDescriptor()
        {
            return contactSource;
        }

        /**
         * Returns null to indicate that this group doesn't have an identifier.
         * @return null
         */
        public String getId()
        {
            return null;
        }

        /**
         * Returns the corresponding <tt>GroupNode</tt>.
         * @return the corresponding <tt>GroupNode</tt>
         */
        public GroupNode getGroupNode()
        {
            return groupNode;
        }

        /**
         * Sets the given <tt>groupNode</tt>.
         * @param groupNode the <tt>GroupNode</tt> to set
         */
        public void setGroupNode(GroupNode groupNode)
        {
            this.groupNode = groupNode;
        }

        /**
         * Returns the right button menu for this group.
         * @return null
         */
        public JPopupMenu getRightButtonMenu()
        {
            return null;
        }
    }

    /**
     * The <tt>ContactActionsServiceListener</tt> listens for service changes
     * in order to update the list of custom action buttons when a new
     * <tt>CustomContactActionsService</tt> is registered or unregistered.
     */
    private class ContactActionsServiceListener
        implements ServiceListener
    {
        public void serviceChanged(ServiceEvent event)
        {
            ServiceReference serviceRef = event.getServiceReference();

            // if the event is caused by a bundle being stopped, we don't want to
            // know
            if (serviceRef.getBundle().getState() == Bundle.STOPPING)
            {
                return;
            }

            Object service = GuiActivator.bundleContext.getService(serviceRef);

            // we don't care if the source service is not a protocol provider
            if (!(service instanceof CustomContactActionsService))
            {
                return;
            }

            @SuppressWarnings("unchecked")
            Iterator<ContactAction<SourceContact>> actionIterator
                = ((CustomContactActionsService<SourceContact>) service)
                    .getCustomContactActions();
            while (actionIterator!= null && actionIterator.hasNext())
            {
                final ContactAction<SourceContact> ca = actionIterator.next();

                switch (event.getType())
                {
                    case ServiceEvent.REGISTERED:
                        initActionButton(ca);
                        break;
                    case ServiceEvent.UNREGISTERING:
                        customActionButtons.remove(ca);
                        break;
                }
            }
        }
    }
}
