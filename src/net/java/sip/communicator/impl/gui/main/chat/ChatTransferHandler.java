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
import java.util.*;

import javax.swing.*;
import javax.swing.text.*;

import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.swing.*;

/**
 * A TransferHandler that we use to handle copying, pasting and DnD operations
 * in our <tt>ChatPanel</tt>. The string handler is heavily inspired
 * by Sun's <tt>DefaultTransferHandler</tt> with the main difference being that
 * we only accept pasting of plain text. We do this in order to avoid html
 * support problems that appear when pasting formatted text into our editable
 * area.
 * 
 * @author Emil Ivov
 * @author Yana Stamcheva
 */
public class ChatTransferHandler
    extends ExtendedTransferHandler
{
    /**
     * This class logger.
     */
    private final static Logger logger
        = Logger.getLogger(ChatTransferHandler.class);

    /**
     * The chat panel involved in the copy/paste/DnD operation.
     */
    private final ChatPanel chatPanel;

    /**
     * Constructs the <tt>ChatTransferHandler</tt> by specifying the
     * <tt>ChatPanel</tt> we're currently dealing with.
     * 
     * @param chatPanel the <tt>ChatPanel</tt> we're currently dealing with
     */
    public ChatTransferHandler(ChatPanel chatPanel)
    {
        this.chatPanel = chatPanel;
    }

    /**
     * Handles transfers to the chat panel from the clip board or a
     * DND drop operation. The <tt>Transferable</tt> parameter contains the
     * data that needs to be imported.
     * <p>
     * @param comp  the component to receive the transfer;
     * @param t the data to import
     * @return  true if the data was inserted into the component and false
     * otherwise
     * @see #importData(TransferHandler.TransferSupport)
     */
    public boolean importData(JComponent comp, Transferable t)
    {
        if (t.isDataFlavorSupported(DataFlavor.javaFileListFlavor))
        {
            try
            {
                Object o = t.getTransferData(DataFlavor.javaFileListFlavor);

                if (o instanceof java.util.Collection)
                {
                    Collection<File> files = (Collection<File>) o;

                    for(File file: files)
                    {
                        chatPanel.sendFile(file);
                    }

                    // Otherwise fire files dropped event.
                    return true;
                }
            }
            catch (UnsupportedFlavorException e)
            {
                logger.debug("Failed to drop files.", e);
            }
            catch (IOException e)
            {
                logger.debug("Failed to drop files.", e);
            }
        }
        else if (t.isDataFlavorSupported(DataFlavor.stringFlavor))
        {
            InputContext inputContext = comp.getInputContext();
            if (inputContext != null)
            {
                inputContext.endComposition();
            }
            try
            {
                BufferedReader reader = new BufferedReader(
                    DataFlavor.stringFlavor.getReaderForText(t));

                StringBuffer buffToPaste = new StringBuffer();
                String line = reader.readLine();

                while(line != null)
                {
                    buffToPaste.append(line);

                    //read next line
                    line = reader.readLine();
                    if(line != null)
                        buffToPaste.append("\n");
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
        return false;
    }
}
