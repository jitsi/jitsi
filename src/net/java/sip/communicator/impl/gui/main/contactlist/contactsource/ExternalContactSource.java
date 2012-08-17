/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.contactlist.contactsource;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.main.contactlist.*;
import net.java.sip.communicator.service.contactsource.*;
import net.java.sip.communicator.service.customcontactactions.*;
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

    /**
     * Creates an <tt>ExternalContactSource</tt> based on the given
     * <tt>ContactSourceService</tt>.
     *
     * @param contactSource the <tt>ContactSourceService</tt>, on which this
     * <tt>ExternalContactSource</tt> is based
     */
    public ExternalContactSource(ContactSourceService contactSource)
    {
        this.contactSource = contactSource;

        sourceUIGroup = new SourceUIGroup(contactSource.getDisplayName());
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
    public static void removeUIContact(SourceContact sourceContact)
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
    public static UIContact getUIContact(SourceContact sourceContact)
    {
        return (UIContact) sourceContact.getData(UI_CONTACT_DATA_KEY);
    }

    /**
     * Returns all custom action buttons for this meta contact.
     *
     * @return a list of all custom action buttons for this meta contact
     */
    public static Collection<SIPCommButton> getContactCustomActionButtons(
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
    private static void initCustomActionButtons()
    {
        customActionButtons = new LinkedHashMap
            <ContactAction<SourceContact>, SIPCommButton>();

        CustomContactActionsChangeListener changeListener
                    = new CustomContactActionsChangeListener();

        for (CustomContactActionsService<SourceContact> ccas
                : getContactActionsServices())
        {
            ccas.addCustomContactActionsListener(changeListener);

            Iterator<ContactAction<SourceContact>> actionIterator
                = ccas.getCustomContactActions();

            while (actionIterator!= null && actionIterator.hasNext())
            {
                final ContactAction<SourceContact> ca = actionIterator.next();

                SIPCommButton actionButton = customActionButtons.get(ca);

                if (actionButton == null)
                {
                    actionButton = new SIPCommButton(
                        new ImageIcon(ca.getIcon()).getImage(),
                        new ImageIcon(ca.getPressedIcon()).getImage(),
                        null);

                    actionButton.addActionListener(new ActionListener()
                    {
                        public void actionPerformed(ActionEvent e)
                        {
                            List<UIContactDetail> contactDetails
                                = SourceUIContact.getContactDetails(
                                    customActionContact);
                            contactDetails.add(
                                new SourceUIContact.SourceContactDetail(
                                        customActionContact.getDisplayName(),
                                        customActionContact));

                            UIContactDetailCustomAction contactAction
                                = new UIContactDetailCustomAction(ca);

                            if (contactDetails.size() > 1)
                            {
                                ChooseUIContactDetailPopupMenu
                                detailsPopupMenu
                                    = new ChooseUIContactDetailPopupMenu(
                                        (JButton) e.getSource(),
                                        contactDetails,
                                        contactAction);

                                detailsPopupMenu.showPopupMenu();
                            }
                            else if (contactDetails.size() == 1)
                            {
                                JButton button = (JButton) e.getSource();
                                Point location = new Point(button.getX(),
                                    button.getY() + button.getHeight());

                                SwingUtilities.convertPointToScreen(
                                    location, GuiActivator.getContactList());

                                location.y = location.y
                                    + GuiActivator.getContactList()
                                        .getPathBounds(
                                            GuiActivator.getContactList()
                                            .getSelectionPath()).y;

                                contactAction.actionPerformed(
                                    contactDetails.get(0),
                                    location.x,
                                    location.y);
                            }
                        }
                    });

                    customActionButtons.put(ca, actionButton);
                }
            }
        }
    }

    /**
     * Returns a list of all custom contact action services.
     *
     * @return a list of all custom contact action services.
     */
    @SuppressWarnings ("unchecked")
    private static List<CustomContactActionsService<SourceContact>>
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
        return contactActionsServices;
    }

    /**
     * The <tt>SourceUIGroup</tt> is the implementation of the UIGroup for the
     * <tt>ExternalContactSource</tt>. It takes the name of the source and
     * sets it as a group name.
     */
    private class SourceUIGroup
        implements UIGroup
    {
        /**
         * The display name of the group.
         */
        private final String displayName;

        /**
         * The corresponding group node.
         */
        private GroupNode groupNode;

        /**
         * Creates an instance of <tt>SourceUIGroup</tt>.
         * @param name the name of the group
         */
        public SourceUIGroup(String name)
        {
            this.displayName = name;
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
            if (contactSource.getIdentifier()
                .equals(ContactSourceService.CALL_HISTORY))
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
            return displayName;
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
     * Listens for updates on actions and when received update the source contact.
     */
    private static class CustomContactActionsChangeListener
        implements CustomContactActionsListener
    {
        /**
         * Update for custom action has occurred.
         * @param event the event containing the source which was updated.
         */
        public void updated(CustomContactActionsEvent event)
        {
            if(!(event.getSource() instanceof SourceContact))
                return;

            ContactNode contactNode
                = getUIContact((SourceContact)event.getSource()).getContactNode();

            if (contactNode != null)
                GuiActivator.getContactList().nodeChanged(contactNode);
        }
    }

    /**
     * An implementation of <tt>UIContactDetail</tt> for a custom action.
     */
    private static class UIContactDetailCustomAction
        implements UIContactDetailAction
    {
        /**
         * The contact action.
         */
        private final ContactAction<SourceContact> contactAction;

        /**
         * Creates an instance of <tt>UIContactDetailCustomAction</tt>.
         */
        public UIContactDetailCustomAction(
            ContactAction<SourceContact> contactAction)
        {
            this.contactAction = contactAction;
        }

        /**
         * Performs the action on button click.
         */
        public void actionPerformed(UIContactDetail contactDetail, int x, int y)
        {
            try
            {
                contactAction.actionPerformed(
                    (SourceContact) contactDetail.getDescriptor(), x, y);
            }
            catch (OperationFailedException e)
            {
                new ErrorDialog(null,
                    GuiActivator.getResources()
                        .getI18NString("service.gui.ERROR"),
                    e.getMessage());
            }
        }
    }
}
