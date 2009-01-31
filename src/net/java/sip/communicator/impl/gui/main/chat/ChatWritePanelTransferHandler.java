/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.chat;

import java.awt.datatransfer.*;
import java.awt.im.*;
import java.io.*;

import javax.swing.*;
import javax.swing.text.*;

/**
 * A TransferHandler that we use to handle copying and pasting in our chat write
 * panel. The handler is heavily inspired by Sun's
 * <tt>DefaultTransferHandler</tt> with the main difference being that we only
 * accept pasting of plain text. We do this in order to avoid html support
 * problems that appear when pasting formatted text into our editable area.
 */
public class ChatWritePanelTransferHandler
    extends TransferHandler
{

    /**
     * Handles transport (cut and copy) from the chat panel to
     * <tt>clipboard</tt>. This method will only transfer plain text and would
     * explicitly ignore any formatting.
     * <p>
     * @param comp  the component holding the data to be transferred;
     *              provided to enable sharing of <code>TransferHandler</code>s
     * @param clip  the clipboard to transfer the data into
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
     * Handles transfers to the chat panel write area from the clip board or a
     * DND drop operation. The <tt>Transferable</tt> parameter contains the
     * data that needs to be imported.
     * <p>
     * @param comp  the component to receive the transfer;
     * @param transferable     the data to import
     * @return  true if the data was inserted into the component and false
     * otherwise
     * @see #importData(TransferHandler.TransferSupport)
     */
    public boolean importData(JComponent comp, Transferable transferable)
    {
        if (comp instanceof JTextComponent)
        {
            DataFlavor flavor
                = getFlavor(transferable.getTransferDataFlavors());

            if (flavor != null)
            {
                InputContext inputContext = comp.getInputContext();
                if (inputContext != null)
                {
                    inputContext.endComposition();
                }
                try
                {
                    BufferedReader reader = new BufferedReader(
                                    flavor.getReaderForText(transferable));

                    StringBuffer buffToPaste = new StringBuffer();
                    String line = null;

                    while((line = reader.readLine() ) != null)
                    {
                        buffToPaste.append(line).append("\n");
                    }

                    ((JTextComponent)comp)
                        .replaceSelection(buffToPaste.toString());
                    return true;
                }
                catch (UnsupportedFlavorException ufe)
                {
                    //ignore
                }
                catch (IOException ioe)
                {
                    //ignore
                }
            }
        }
        return false;
    }

    /** Indicates whether a component will accept an import of the given
     * set of data flavors prior to actually attempting to import it.  We return
     * <tt>true</tt> to indicate that the transfer with at least one of the
     * given flavors would work and <tt>false</tt> to reject the transfer.
     * <p>
     * @param support the object containing the details of
     *        the transfer, not <code>null</code>.
     * @param transferFlavors the data formats available
     * @return  true if the data can be inserted into the component, false otherwise
     * @throws NullPointerException if <code>support</code> is {@code null}
     */
    public boolean canImport(JComponent comp,
                             DataFlavor[] transferFlavors)
    {
        JTextComponent c = (JTextComponent)comp;

        if (!(c.isEditable() && c.isEnabled()))
        {
            return false;
        }
        return (getFlavor(transferFlavors) != null);
    }

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
        //we always return null here since we don't need to do anything with
        //the source component at this stage/
        return NONE;
    }

    /**
     * Locates and returns a string flavor.
     *
     * @param flavors the haystack containing all supported flavors.
     *
     * @return a string flavor if one exists in the <tt>flavors</tt> array and
     * false otherwise.
     */
    private DataFlavor getFlavor(DataFlavor[] flavors)
    {
        if (flavors != null)
        {
            for (int counter = 0; counter < flavors.length; counter++)
            {
                if (flavors[counter].equals(DataFlavor.stringFlavor))
                {
                    return flavors[counter];
                }
            }
        }

        return null;
    }
}