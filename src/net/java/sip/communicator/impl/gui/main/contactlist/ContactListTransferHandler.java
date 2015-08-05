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

import java.awt.datatransfer.*;
import java.io.*;
import java.util.*;

import javax.swing.*;
import javax.swing.tree.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.main.chat.*;
import net.java.sip.communicator.impl.gui.main.contactlist.contactsource.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

/**
 * A TransferHandler that we use to handle DnD operations in our
 * <tt>ContactList</tt>.
 *
 * @author Yana Stamcheva
 */
public class ContactListTransferHandler
    extends ExtendedTransferHandler
{
    /**
     * The data flavor used when transferring <tt>UIContact</tt>s.
     */
    protected static final DataFlavor uiContactDataFlavor
        = new DataFlavor(UIContact.class, "UIContact");

    /**
     * The logger.
     */
    private static final Logger logger
        = Logger.getLogger(ContactListTransferHandler.class);

    /**
     * The contact list, where this transfer happens.
     */
    private final DefaultTreeContactList contactList;
    
    /**
     * The transferred object.
     */
    private Object transferredObject = null;

    /**
     * Creates an instance of <tt>ContactListTransferHandler</tt> passing the
     * <tt>contactList</tt> which uses this <tt>TransferHandler</tt>.
     * @param contactList the <tt>DefaultContactList</tt> which uses this
     * <tt>TransferHandler</tt>
     */
    public ContactListTransferHandler(DefaultTreeContactList contactList)
    {
        this.contactList = contactList;
    }

    /**
     * Creates a transferable for text pane components in order to enable drag
     * and drop of text.
     * @param component the component for which to create a
     * <tt>Transferable</tt>
     * @return the created <tt>Transferable</tt>
     */
    @Override
    protected Transferable createTransferable(JComponent component)
    {
        if(ConfigurationUtils.isAddContactDisabled()
            && (ConfigurationUtils.isContactMoveDisabled()
                || ConfigurationUtils.isCreateGroupDisabled()))
            return null;

        if (component instanceof JTree)
        {
            JTree tree = (JTree) component;
            TreePath selectionPath = tree.getSelectionPath();
            transferredObject = selectionPath.getLastPathComponent();
            return new ContactListTransferable(
                tree.getRowForPath(selectionPath),
                selectionPath.getLastPathComponent());
            
        }

        return super.createTransferable(component);
    }

    /**
     * Indicates whether a component will accept an import of the given
     * set of data flavors prior to actually attempting to import it. We return
     * <tt>true</tt> to indicate that the transfer with at least one of the
     * given flavors would work and <tt>false</tt> to reject the transfer.
     * <p>
     * @param comp component
     * @param flavor the data formats available
     * @return  true if the data can be inserted into the component, false
     * otherwise
     * @throws NullPointerException if <code>support</code> is {@code null}
     */
    @Override
    public boolean canImport(JComponent comp, DataFlavor flavor[])
    {
        if(!(transferredObject instanceof ContactNode))
            return false;

        Object selectedObject = (((ContactNode)transferredObject)
            .getContactDescriptor()).getDescriptor();
        if (selectedObject instanceof MetaContact)
        {
            MetaContact metaContact = (MetaContact) selectedObject;
            Iterator<Contact> contacts = metaContact.getContacts();
            while(contacts.hasNext())
            {
                Contact contact = contacts.next();
                if(contact.getPersistableAddress() == null)
                {
                    return false;
                }
            }
        }
        
        for (int i = 0, n = flavor.length; i < n; i++)
        {
            if (flavor[i].equals(uiContactDataFlavor))
            {
                return true;
            }
        }

        return super.canImport(comp, flavor);
    }

    /**
     * Handles transfers to the contact list from the clip board or a
     * DND drop operation. The <tt>Transferable</tt> parameter contains the
     * data that needs to be imported.
     *
     * @param comp the component to receive the transfer
     * @param t the data to import
     * @return <tt>true</tt> if the data was inserted into the component;
     * <tt>false</tt>, otherwise
     * @see TransferHandler#importData(JComponent, Transferable)
     */
    @Override
    @SuppressWarnings("unchecked") //taken care of
    public boolean importData(JComponent comp, Transferable t)
    {
        if (t.isDataFlavorSupported(DataFlavor.javaFileListFlavor))
        {
            Object o = null;

            try
            {
                o = t.getTransferData(DataFlavor.javaFileListFlavor);
            }
            catch (UnsupportedFlavorException e)
            {
                if (logger.isDebugEnabled())
                    logger.debug("Failed to drop files.", e);
            }
            catch (IOException e)
            {
                if (logger.isDebugEnabled())
                    logger.debug("Failed to drop files.", e);
            }

            if (o instanceof Collection)
            {
                ChatPanel chatPanel = getChatPanel();

                if (chatPanel != null)
                {
                    Collection<File> files = (Collection<File>) o;

                    for (File file : files)
                    {
                        chatPanel.sendFile(file);

                        GuiActivator.getUIService().getChatWindowManager()
                            .openChat(chatPanel, false);
                    }
                    return true;
                }
            }
        }
        else if (t.isDataFlavorSupported(uiContactDataFlavor))
        {
            Object o = null;

            try
            {
                o = t.getTransferData(uiContactDataFlavor);
            }
            catch (UnsupportedFlavorException e)
            {
                if (logger.isDebugEnabled())
                    logger.debug("Failed to drop meta contact.", e);
            }
            catch (IOException e)
            {
                if (logger.isDebugEnabled())
                    logger.debug("Failed to drop meta contact.", e);
            }

            if (o instanceof ContactNode
                && comp instanceof TreeContactList)
            {
                UIContact transferredContact
                    = ((ContactNode) o).getContactDescriptor();

                // We support darg&drop for MetaContacts only.
                if (!(transferredContact instanceof MetaUIContact))
                    return false;

                TreeContactList list = (TreeContactList) comp;

                Object dest = list.getSelectedValue();

                if (transferredContact == null)
                    return false;

                if(transferredContact.getDescriptor() instanceof MetaContact)
                {
                    // check if underlying account is readonly
                    MetaContact metaContact =
                        (MetaContact)transferredContact.getDescriptor();
                    boolean onlyReadonlyContacts = true;
                    Iterator<Contact> iter = metaContact.getContacts();
                    while(iter.hasNext())
                    {
                        Contact c = iter.next();
                        ProtocolProviderService pp = c.getProtocolProvider();
                        OperationSetPersistentPresencePermissions
                            opsetPermissions = pp.getOperationSet(
                                OperationSetPersistentPresencePermissions.class);

                        if( opsetPermissions == null
                            || !opsetPermissions.isReadOnly(c))
                        {
                            onlyReadonlyContacts = false;
                            break;
                        }
                    }
                    if(onlyReadonlyContacts)
                        return false;
                }

                if (dest instanceof ContactNode)
                {
                    UIContact destContact
                        = ((ContactNode) dest).getContactDescriptor();

                    // We support darg&drop for MetaContacts only for now.
                    if (!(destContact instanceof MetaUIContact))
                        return false;

                    if (transferredContact != destContact)
                    {
                        String mergeEnabledStr =
                            GuiActivator.getResources().getSettingsString(
                                "impl.gui.dnd.MERGE_ENABLED");

                        // by default merging contacts is enabled
                        if(mergeEnabledStr != null
                            && !Boolean.parseBoolean(mergeEnabledStr))
                        {
                            UIGroup destGroup =
                                destContact.getParentGroup();

                            if (destGroup == null
                                || !(destGroup instanceof MetaUIGroup)
                                || (ConfigurationUtils
                                        .isContactMoveDisabled()
                                    || ConfigurationUtils
                                        .isCreateGroupDisabled()))
                                return false;

                            MetaContactListManager.moveMetaContactToGroup(
                                (MetaContact) transferredContact
                                    .getDescriptor(),
                                (MetaContactGroup) destGroup
                                    .getDescriptor());
                        }
                        else
                        {
                            if(!ConfigurationUtils.isAddContactDisabled())
                            {
                                MetaContactListManager
                                    .moveMetaContactToMetaContact(
                                        (MetaContact) transferredContact
                                            .getDescriptor(),
                                        (MetaContact) destContact
                                            .getDescriptor());
                            }
                        }
                    }
                    return true;
                }
                else if (dest instanceof GroupNode)
                {
                    UIGroup destGroup
                        = ((GroupNode) dest).getGroupDescriptor();

                    // We support darg&drop for MetaContacts only for now.
                    if (!(destGroup instanceof MetaUIGroup)
                        && !destGroup.equals(list.getRootUIGroup()))
                    {
                        return false;
                    }

                    if (destGroup instanceof MetaUIGroup
                        && !transferredContact
                            .getParentGroup().equals(destGroup)
                        && !(ConfigurationUtils.isContactMoveDisabled()
                            && ConfigurationUtils.isCreateGroupDisabled()))
                    {
                        MetaContactListManager.moveMetaContactToGroup(
                            (MetaContact) transferredContact.getDescriptor(),
                            (MetaContactGroup) destGroup.getDescriptor());
                    }
                    else if(destGroup.equals(list.getRootUIGroup()))
                    {
                        MetaContactListManager.moveMetaContactToGroup(
                            (MetaContact) transferredContact.getDescriptor(),
                            GuiActivator.getContactListService().getRoot());
                    }

                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Overrides <tt>TransferHandler.getVisualRepresentation(Transferable t)</tt>
     * in order to return a custom drag icon.
     * <p>
     * The default parent implementation of this method returns null.
     *
     * @param t  the data to be transferred; this value is expected to have been
     * created by the <code>createTransferable</code> method
     * @return the icon to show when dragging
     */
    @Override
    public Icon getVisualRepresentation(Transferable t)
    {
        Icon icon = null;

        if (t instanceof ContactListTransferable)
        {
            ContactListTransferable transferable = ((ContactListTransferable) t);

            try
            {
                icon = ((ContactListTreeCellRenderer)
                    contactList.getCellRenderer()).getDragIcon(
                        contactList,
                        transferable.getTransferData(uiContactDataFlavor),
                        transferable.getTransferIndex());
            }
            catch (UnsupportedFlavorException e)
            {
                if (logger.isDebugEnabled())
                    logger.debug(
                        "Unsupported flavor while" +
                        " obtaining transfer data.", e);
            }
            catch (IOException e)
            {
                if (logger.isDebugEnabled())
                    logger.debug(
                        "The data for the request flavor" +
                        " is no longer available.", e);
            }
        }

        return icon;
    }

    /**
     * Returns the <tt>ChatPanel</tt> corresponding to the currently selected
     * contact.
     *
     * @return the <tt>ChatPanel</tt> corresponding to the currently selected
     * contact.
     */
    private ChatPanel getChatPanel()
    {
        ChatPanel chatPanel = null;
        Object selectedObject = contactList.getSelectedValue();

        if (selectedObject instanceof MetaContact)
        {
            MetaContact metaContact = (MetaContact) selectedObject;

            // Obtain the corresponding chat panel.
            chatPanel = GuiActivator.getUIService().getChatWindowManager()
                            .getContactChat(metaContact, true);
        }

        return chatPanel;
    }

    /**
     * Transferable for JList that enables drag and drop of contacts.
     */
    public class ContactListTransferable implements Transferable
    {
        private int transferredIndex;

        private Object transferredObject;

        /**
         * Creates an instance of <tt>ContactListTransferable</tt>.
         * @param index the index of the transferred object in the list
         * @param o the transferred list object
         */
        public ContactListTransferable(int index, Object o)
        {
            this.transferredIndex = index;
            this.transferredObject = o;
        }

        /**
         * Returns supported flavors.
         * @return an array of supported flavors
         */
        public DataFlavor[] getTransferDataFlavors()
        {
            return new DataFlavor[] {   uiContactDataFlavor,
                                        DataFlavor.stringFlavor};
        }

        /**
         * Returns <tt>true</tt> if the given <tt>flavor</tt> is supported,
         * otherwise returns <tt>false</tt>.
         * @param flavor the data flavor to verify
         * @return <tt>true</tt> if the given <tt>flavor</tt> is supported,
         * otherwise returns <tt>false</tt>
         */
        public boolean isDataFlavorSupported(DataFlavor flavor)
        {
            return uiContactDataFlavor.equals(flavor)
                    || DataFlavor.stringFlavor.equals(flavor);
        }

        /**
         * Returns the selected text.
         * @param flavor the flavor
         * @return the selected text
         * @exception UnsupportedFlavorException if the requested data flavor
         * is not supported.
         * @exception IOException if the data is no longer available in the
         * requested flavor.
         */
        public Object getTransferData(DataFlavor flavor)
            throws  UnsupportedFlavorException,
                    IOException
        {
            if (uiContactDataFlavor.equals(flavor))
            {
                return transferredObject;
            }
            else if (DataFlavor.stringFlavor.equals(flavor))
            {
                if (transferredObject instanceof ContactNode)
                    return ((ContactNode) transferredObject)
                        .getContactDescriptor().getDisplayName();
            }
            else
                throw new UnsupportedFlavorException(flavor);

            return null;
        }

        /**
         * Returns the index of the transferred list cell.
         * @return the index of the transferred list cell
         */
        public int getTransferIndex()
        {
            return transferredIndex;
        }
    }
}
