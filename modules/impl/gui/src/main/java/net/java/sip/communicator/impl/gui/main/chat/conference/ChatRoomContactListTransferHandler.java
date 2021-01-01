/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.chat.conference;

import java.awt.datatransfer.*;
import java.io.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.main.chat.*;
import net.java.sip.communicator.impl.gui.main.contactlist.*;
import net.java.sip.communicator.plugin.desktoputil.ExtendedTransferHandler;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.protocol.Contact;

/**
 * A TransferHandler that we use to handle DnD operations in our
 * <tt>ContactList</tt>.
 *
 * @author Yana Stamcheva
 */
public class ChatRoomContactListTransferHandler
    extends ExtendedTransferHandler
{
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ChatRoomContactListTransferHandler.class);

    /**
     * The data flavor used when transferring <tt>MetaContact</tt>s.
     */
    private static final DataFlavor metaContactDataFlavor
        = new DataFlavor(MetaContact.class, "MetaContact");

    private final ChatRoomContactList contactList;

    /**
     * Creates an instance of <tt>ContactListTransferHandler</tt> passing the
     * <tt>contactList</tt> which uses this <tt>TransferHandler</tt>.
     * @param contactList the <tt>DefaultContactList</tt> which uses this
     * <tt>TransferHandler</tt>
     */
    public ChatRoomContactListTransferHandler(ChatRoomContactList contactList)
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
        if (component instanceof JList)
        {
            JList list = (JList) component;
            return new ContactListTransferable(
                list.getSelectedIndex(), list.getSelectedValue());
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
            if (flavor[i].equals(metaContactDataFlavor))
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
        if (t.isDataFlavorSupported(metaContactDataFlavor))
        {
            Object o = null;

            try
            {
                o = t.getTransferData(metaContactDataFlavor);
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

            if (o instanceof ChatContact
                && comp instanceof ChatRoomContactList)
            {
                ChatContact<? extends Contact> chatContact
                    = (ChatContact<? extends Contact>) o;
                Contact transferredContact = chatContact.getDescriptor();
                ChatRoomContactList list = (ChatRoomContactList) comp;

                Object dest = list.getSelectedValue();

                if (transferredContact != null)
                {
                    if (dest instanceof MetaContact)
                    {
                        MetaContact destContact = (MetaContact) dest;
                        if (transferredContact != destContact)
                        {
                            MetaContactListManager.moveContactToMetaContact(
                                transferredContact, destContact);
                        }
                        return true;
                    }
                    else if (dest instanceof MetaContactGroup)
                    {
                        MetaContactGroup destGroup = (MetaContactGroup) dest;
                        MetaContactListManager.moveContactToGroup(
                            transferredContact, destGroup);
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
        ChatContactCellRenderer renderer = null;

        if (t instanceof ContactListTransferable)
        {
            ContactListTransferable transferable = ((ContactListTransferable) t);

            try
            {
                renderer = (ChatContactCellRenderer)
                    contactList.getCellRenderer()
                        .getListCellRendererComponent(
                        contactList,
                        (ChatContact<?>) transferable
                            .getTransferData(metaContactDataFlavor),
                        transferable.getTransferIndex(), false, false);
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
            return new DataFlavor[] {   metaContactDataFlavor,
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
            return metaContactDataFlavor.equals(flavor)
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
            if (metaContactDataFlavor.equals(flavor))
            {
                return transferredObject;
            }
            else if (DataFlavor.stringFlavor.equals(flavor))
            {
                if (transferredObject instanceof ChatContact)
                    return ((ChatContact) transferredObject).getUID();
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
