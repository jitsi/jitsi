/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.utils;

import java.awt.datatransfer.*;
import java.io.*;
import java.util.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.main.contactlist.*;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.swing.*;

/**
 * A TransferHandler that we use to handle dropping of <tt>UIContact</tt>s or
 * simple string addresses to the conference invite dialog.
 *
 * @author Sebastien Vincent
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
     * The data flavor used when transferring <tt>UIContact</tt>s.
     */
    protected static final DataFlavor metaContactDataFlavor
        = new DataFlavor(MetaContact.class, "MetaContact");

    /**
     * The invite dialog.
     */
    private final InviteDialog dialog;

    /**
     * If the component is the selected contact list or not
     */
    private final boolean selected;

    /**
     * Constructor.
     *
     * @param dialog the invite dialog
     * @param selected if the column is the selected ones
     */
    public InviteContactTransferHandler(InviteDialog dialog, boolean selected)
    {
        this.dialog = dialog;
        this.selected = selected;
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
        if (component instanceof DefaultContactList)
        {
            MetaContact c =
                (MetaContact)((DefaultContactList)component).getSelectedValue();
            return new MetaContactTransferable(c);
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
            if (flavor[i].equals(uiContactDataFlavor) ||
                flavor[i].equals(metaContactDataFlavor))
            {
                if (comp instanceof DefaultContactList)
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

            if (o instanceof ContactNode)
            {
                UIContact uiContact
                    = ((ContactNode) o).getContactDescriptor();

                Object obj = uiContact.getDescriptor();

                if(!(obj instanceof MetaContact))
                    return false;

                Iterator<UIContactDetail> contactDetails
                    = uiContact.getContactDetailsForOperationSet(
                        OperationSetBasicTelephony.class).iterator();
                if(!contactDetails.hasNext())
                    return false;

                MetaContact metaContact =
                    (MetaContact)uiContact.getDescriptor();

                dialog.addSelectedMetaContact(metaContact);
                return true;
            }
        }
        else if(t.isDataFlavorSupported(metaContactDataFlavor))
        {
            MetaContact c = null;

            try
            {
                c = (MetaContact)t.getTransferData(metaContactDataFlavor);
            }
            catch(Exception e)
            {
                e.printStackTrace();
            }

            if(selected)
            {
                dialog.removeMetaContact(c);
                dialog.addSelectedMetaContact(c);
                return true;
            }
            else
            {
                dialog.removeSelectedMetaContact(c);
                dialog.addMetaContact(c);
            }
            return true;
        }
        return false;
    }

    /**
     * Transferable for DefaultContactList that enables drag and drop of
     * meta contacts.
     */
    public class MetaContactTransferable
        implements Transferable
    {
        /**
         * The meta contact.
         */
        private final MetaContact metaContact;

        /**
         * Creates an instance of <tt>MetaContactTransferable</tt>.
         * @param metaContact the meta contact to transfer
         */
        public MetaContactTransferable(MetaContact metaContact)
        {
            this.metaContact = metaContact;
        }

        /**
         * Returns supported flavors.
         * @return an array of supported flavors
         */
        public DataFlavor[] getTransferDataFlavors()
        {
            return new DataFlavor[] {   metaContactDataFlavor};
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
            return metaContactDataFlavor.equals(flavor);
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
            return metaContact;
        }

        /**
         * Returns meta contact.
         *
         * @return meta contact
         */
        public MetaContact getMetaContact()
        {
            return metaContact;
        }
    }
}
