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
package net.java.sip.communicator.impl.gui.utils;

import java.awt.datatransfer.*;
import java.io.*;
import java.util.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.main.contactlist.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

/**
 * A TransferHandler that we use to handle dropping of <tt>UIContact</tt>s or
 * simple string addresses to the conference invite dialog.
 *
 * @author Sebastien Vincent
 * @author Yana Stamcheva
 */
public class InviteContactTransferHandler
    extends ExtendedTransferHandler
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * The logger.
     */
    private static final Logger logger
        = Logger.getLogger(InviteContactTransferHandler.class);

    /**
     * The data flavor used when transferring <tt>UIContact</tt>s.
     */
    protected static final DataFlavor uiContactDataFlavor
        = new DataFlavor(UIContact.class, "UIContact");

    /**
     * The destination contact list.
     */
    private final ContactList contactList;

    /**
     * The backup provider to use if no provider is specified.
     */
    private ProtocolProviderService backupProvider;

    /**
     * If the component is the selected contact list or not
     */
    private final boolean selected;

    /**
     * Indicates that this handler is of type source transfer handler.
     */
    public static final int SOURCE_TRANSFER_HANDLER = 0;

    /**
     * Indicates that this handler is of type destination transfer handler.
     */
    public static final int DEST_TRANSFER_HANDLER = 1;

    /**
     * The type of this transfer handler. Indicates if it's used for dragging
     * from the source contact list, or dropping in the destination contact
     * list.
     */
    private final int type;

    /**
     * Constructor.
     *
     * @param contactList the contact list, this transfer handler is about
     * @param type the type of this transfer handler. Indicates if it's used for
     * dragging from the source contact list, or dropping in the destination
     * contact list. One of SOURCE_TRANSFER_HANDLER or DEST_TRANSFER_HANDLER
     * @param selected if the column is the selected ones
     */
    public InviteContactTransferHandler(ContactList contactList,
                                        int type,
                                        boolean selected)
    {
        this.contactList = contactList;
        this.type = type;
        this.selected = selected;
    }

    /**
     * Creates a transferable for text pane components in order to enable drag
     * and drop of text.
     *
     * @param component the component for which to create a
     * <tt>Transferable</tt>
     * @return the created <tt>Transferable</tt>
     */
    @Override
    protected Transferable createTransferable(JComponent component)
    {
        // Dragging is only enabled in the source contact list.
        if (type != SOURCE_TRANSFER_HANDLER)
            return null;

        if (component instanceof ContactList)
        {
            List<UIContact> c = ((ContactList) component).getSelectedContacts();

            if (c != null)
                return new UIContactTransferable(c);
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
        // Dropping is only enabled in the destination contact list.
        if (type != DEST_TRANSFER_HANDLER)
            return false;

        for (int i = 0, n = flavor.length; i < n; i++)
        {
            if (flavor[i].equals(uiContactDataFlavor))
            {
                if (comp instanceof ContactList)
                {
                    return true;
                }

                return false;
            }
        }

        return false;
    }

    /**
     * Handles transfers to the invite dialog.
     *
     * @param comp  the component to receive the transfer;
     * @param t the data to import
     * @return  true if the data was inserted into the component and false
     * otherwise
     */
    @Override
    public boolean importData(JComponent comp, Transferable t)
    {
        if (t.isDataFlavorSupported(uiContactDataFlavor))
        {
            if(!selected)
                return false;

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

            if (o instanceof Collection)
            {
                Iterator<?> c = ((Collection<?>) o).iterator();

                while (c.hasNext())
                {
                    Object nextO = c.next();

                    if (nextO instanceof UIContact)
                    {
                        contactList.addContact(
                            new InviteUIContact((UIContact) nextO,
                                                backupProvider),
                            null, false, false);
                    }
                }

                return true;
            }
            else if (o instanceof UIContact)
            {
                contactList.addContact(
                    new InviteUIContact((UIContact) o,
                                        backupProvider),
                    null, false, false);

                return true;
            }
            else if (o instanceof ContactNode)
            {
                UIContact uiContact = ((ContactNode) o).getContactDescriptor();

                if (uiContact != null)
                {
                    contactList.addContact(
                        new InviteUIContact(uiContact,
                                            backupProvider),
                        null, false, false);

                    return true;
                }
            }
        }
        return false;
    }

    /**
     * The backup provider to use if no provider has been specified.
     *
     * @param backupProvider the backup provider to use if no provider has been
     * specified
     */
    public void setBackupProvider(ProtocolProviderService backupProvider)
    {
        this.backupProvider = backupProvider;
    }

    /**
     * Transferable for TreeContactList that enables drag and drop of
     * ui contacts.
     */
    public class UIContactTransferable
        implements Transferable
    {
        /**
         * The ui contact.
         */
        private final List<UIContact> uiContacts;

        /**
         * Creates an instance of <tt>UIContactTransferable</tt>.
         *
         * @param uiContacts the ui contacts to transfer
         */
        public UIContactTransferable(List<UIContact> uiContacts)
        {
            this.uiContacts = uiContacts;
        }

        /**
         * Returns supported flavors.
         *
         * @return an array of supported flavors
         */
        public DataFlavor[] getTransferDataFlavors()
        {
            return new DataFlavor[] {uiContactDataFlavor};
        }

        /**
         * Returns <tt>true</tt> if the given <tt>flavor</tt> is supported,
         * otherwise returns <tt>false</tt>.
         *
         * @param flavor the data flavor to verify
         * @return <tt>true</tt> if the given <tt>flavor</tt> is supported,
         * otherwise returns <tt>false</tt>
         */
        public boolean isDataFlavorSupported(DataFlavor flavor)
        {
            return uiContactDataFlavor.equals(flavor);
        }

        /**
         * Returns the selected text.
         *
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
            return uiContacts;
        }

        /**
         * Returns the ui contacts.
         *
         * @return the ui contacts
         */
        public List<UIContact> getUIContacts()
        {
            return uiContacts;
        }
    }
}
