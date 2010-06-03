/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
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
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.swing.*;

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
    public Transferable createTransferable(JComponent component)
    {
        if (component instanceof JTree)
        {
            JTree tree = (JTree) component;
            TreePath selectionPath = tree.getSelectionPath();
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
    public boolean canImport(JComponent comp, DataFlavor flavor[])
    {
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

                Object dest
                    = list.getSelectionPath().getLastPathComponent();

                if (transferredContact != null)
                {
                    if (dest instanceof ContactNode)
                    {
                        UIContact destContact
                            = ((ContactNode) dest).getContactDescriptor();

                        // We support darg&drop for MetaContacts only for now.
                        if (!(destContact instanceof MetaUIContact))
                            return false;

                        if (transferredContact != destContact)
                        {
                            MetaContactListManager.moveMetaContactToMetaContact(
                                (MetaContact) transferredContact.getDescriptor(),
                                (MetaContact) destContact.getDescriptor());
                        }
                        return true;
                    }
                    else if (dest instanceof GroupNode)
                    {
                        UIGroup destGroup
                            = ((GroupNode) dest).getGroupDescriptor();

                        // We support darg&drop for MetaContacts only for now.
                        if (!(destGroup instanceof MetaUIGroup))
                            return false;

                        if (!transferredContact
                                .getParentGroup().equals(destGroup))
                        {
                            MetaContactListManager.moveMetaContactToGroup(
                                (MetaContact) transferredContact.getDescriptor(),
                                (MetaContactGroup) destGroup.getDescriptor());
                        }
                        return true;
                    }
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
    public Icon getVisualRepresentation(Transferable t)
    {
        ContactListTreeCellRenderer renderer = null;

        if (t instanceof ContactListTransferable)
        {
            ContactListTransferable transferable = ((ContactListTransferable) t);

            try
            {
                renderer = (ContactListTreeCellRenderer)
                    contactList.getCellRenderer()
                        .getTreeCellRendererComponent(
                        contactList,
                        transferable.getTransferData(uiContactDataFlavor),
                        true, // is selected
                        false, // is expanded
                        true, // is leaf
                        transferable.getTransferIndex(),
                        true // has focus
                        );
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

        return renderer;
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
        Object selectedObject
            = contactList.getSelectionPath().getLastPathComponent();

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
