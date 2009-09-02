/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.util.swing;

import java.awt.datatransfer.*;
import java.io.*;

import javax.swing.*;
import javax.swing.text.*;

/**
 * A TransferHandler that we use to handle copying, pasting and DnD operations.
 * The string handler is heavily inspired by Sun's
 * <tt>DefaultTransferHandler</tt> with the main difference being that
 * we only accept pasting of plain text. We do this in order to avoid html
 * support problems that appear when pasting formatted text into our editable
 * area.
 * 
 * @author Emil Ivov
 * @author Yana Stamcheva
 */
public class ExtendedTransferHandler
    extends TransferHandler
{
    /**
     * Returns the type of transfer actions supported by the source;
     * any bitwise-OR combination of <tt>COPY</tt>, <tt>MOVE</tt>
     * and <tt>LINK</tt>.
     * <p>
     * Some models are not mutable, so a transfer operation of <tt>MOVE</tt>
     * should not be advertised in that case. Returning <tt>NONE</tt>
     * disables transfers from the component.
     *
     * @param c  the component holding the data to be transferred;
     *           provided to enable sharing of <code>TransferHandler</code>s
     * @return {@code COPY} if the transfer property can be found,
     *          otherwise returns <code>NONE</code>
     */
    public int getSourceActions(JComponent c)
    {
        return TransferHandler.COPY_OR_MOVE;
    }

    /** Indicates whether a component will accept an import of the given
     * set of data flavors prior to actually attempting to import it.  We return
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
            if (flavor[i].equals(DataFlavor.javaFileListFlavor))
            {
                return true;
            }
            else if (flavor[i].equals(DataFlavor.stringFlavor))
            {
                if (comp instanceof JTextComponent)
                {
                    JTextComponent c = (JTextComponent)comp;

                    if (c.isEditable() && c.isEnabled())
                    {
                        return true;
                    }
                }

                return false;
            }
        }

        return false;
    }

    /**
     * Creates a transferable for text pane components in order to enable drag
     * and drop of text.
     */
    public Transferable createTransferable(JComponent comp)
    {
        if (comp instanceof JTextPane)
        {
            return new SelectedTextTransferable((JTextPane) comp);
        }

        return super.createTransferable(comp);
    }

    /**
     * Handles transport (cut and copy) from the chat panel to
     * <tt>clipboard</tt>. This method will only transfer plain text and would
     * explicitly ignore any formatting.
     * <p>
     * @param comp  the component holding the data to be transferred;
     *              provided to enable sharing of <code>TransferHandler</code>s
     * @param clipboard the clipboard to transfer the data into
     * @param action the transfer action requested; this should
     *  be a value of either <code>COPY</code> or <code>MOVE</code>;
     *  the operation performed is the intersection  of the transfer
     *  capabilities given by getSourceActions and the requested action;
     *  the intersection may result in an action of <code>NONE</code>
     *  if the requested action isn't supported
     * @throws IllegalStateException if the clipboard is currently unavailable
     * @see Clipboard#setContents(Transferable, ClipboardOwner)
     */
    public void exportToClipboard(JComponent comp,
                                  Clipboard clipboard,
                                  int action)
        throws IllegalStateException
    {
        if (comp instanceof JTextComponent)
        {
            JTextComponent textComponent = (JTextComponent)comp;
            int startIndex = textComponent.getSelectionStart();
            int endIndex = textComponent.getSelectionEnd();
            if (startIndex != endIndex)
            {
                try
                {
                    Document doc = textComponent.getDocument();
                    String srcData = doc.getText(startIndex,
                                                 endIndex - startIndex);
                    StringSelection contents =new StringSelection(srcData);

                    // this may throw an IllegalStateException,
                    // but it will be caught and handled in the
                    // action that invoked this method
                    clipboard.setContents(contents, null);

                    if (action == TransferHandler.MOVE)
                    {
                        doc.remove(startIndex, endIndex - startIndex);
                    }
                }
                catch (BadLocationException ble)
                {
                    //we simply ignore
                }
            }
        }
    }

    /**
     * Transferable for text pane components that enables drag and drop of text.
     */
    public class SelectedTextTransferable implements Transferable
    {
        private JTextPane textPane;

        public SelectedTextTransferable(JTextPane textPane)
        {
            this.textPane = textPane;
        }

        // Returns supported flavors
        public DataFlavor[] getTransferDataFlavors()
        {
            return new DataFlavor[]{DataFlavor.stringFlavor};
        }

        // Returns true if flavor is supported
        public boolean isDataFlavorSupported(DataFlavor flavor)
        {
            return DataFlavor.stringFlavor.equals(flavor);
        }

        // Returns Selected Text
        public Object getTransferData(DataFlavor flavor)
            throws UnsupportedFlavorException, IOException
        {
            if (!DataFlavor.stringFlavor.equals(flavor))
            {
                throw new UnsupportedFlavorException(flavor);
            }

            return textPane.getSelectedText();
        }
    }
}
