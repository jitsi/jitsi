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
package net.java.sip.communicator.impl.gui.main.chat;

import java.awt.datatransfer.*;
import java.awt.im.*;
import java.io.*;
import java.net.*;
import java.util.*;

import javax.swing.*;
import javax.swing.text.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.main.contactlist.*;
import net.java.sip.communicator.impl.gui.main.contactlist.contactsource.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

/**
 * A TransferHandler that we use to handle copying, pasting and DnD operations
 * in our <tt>ChatPanel</tt>. The string handler is heavily inspired
 * by Sun's <tt>DefaultTransferHandler</tt> with the main difference being that
 * we only accept pasting of plain text. We do this in order to avoid HTML
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
     * The data flavor used when transferring <tt>UIContact</tt>s.
     */
    protected static final DataFlavor uiContactDataFlavor
        = new DataFlavor(UIContact.class, "UIContact");

    /**
     * This class logger.
     */
    private final static Logger logger
        = Logger.getLogger(ChatTransferHandler.class);

    /**
     * The data flavor used when transferring <tt>File</tt>s under Linux.
     */
    private static DataFlavor uriListFlavor;
    static
    {
         try
         {
             uriListFlavor =
                 new DataFlavor("text/uri-list;class=java.lang.String");
         } catch (ClassNotFoundException e)
         {
            // can't happen
             logger.error("", e);
         }
    }

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
        for(DataFlavor f: flavor)
        {
            if (f.equals(uiContactDataFlavor) || f.equals(uriListFlavor))
                return true;
        }

        return super.canImport(comp, flavor);
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
     */
    @Override
    @SuppressWarnings("unchecked") //the case is taken care of
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
                        chatPanel.sendFile(file);

                    // Otherwise fire files dropped event.
                    return true;
                }
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
        }

        if (t.isDataFlavorSupported(uriListFlavor))
        {
            try
            {
                Object o = t.getTransferData(uriListFlavor);
                boolean dataProcessed = false;

                StringTokenizer tokens = new StringTokenizer((String)o);
                while (tokens.hasMoreTokens())
                {
                    String urlString = tokens.nextToken();
                    URL url = new URL(urlString);
                    File file = new File(
                        URLDecoder.decode(url.getFile(), "UTF-8"));
                    chatPanel.sendFile(file);
                    dataProcessed = true;
                }

                return dataProcessed;
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
        }

        if (t.isDataFlavorSupported(uiContactDataFlavor))
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

            if (o instanceof ContactNode)
            {
                UIContact uiContact
                    = ((ContactNode) o).getContactDescriptor();

                // We only support drag&drop for MetaContacts for now.
                if (!(uiContact instanceof MetaUIContact))
                    return false;

                ChatTransport currentChatTransport
                    = chatPanel.getChatSession().getCurrentChatTransport();

                Iterator<Contact> contacts = ((MetaContact) uiContact
                    .getDescriptor()).getContactsForProvider(
                        currentChatTransport.getProtocolProvider());

                String contact = null;
                if (contacts.hasNext())
                    contact = contacts.next().getAddress();

                if (contact != null)
                {
                    List<String> inviteList = new ArrayList<String>();
                    inviteList.add(contact);
                    chatPanel.inviteContacts(   currentChatTransport,
                                                inviteList, null);

                    return true;
                }
                else
                    new ErrorDialog(
                        null,
                        GuiActivator.getResources().getI18NString(
                            "service.gui.ERROR"),
                        GuiActivator.getResources().getI18NString(
                            "service.gui.CONTACT_NOT_SUPPORTING_CHAT_CONF",
                            new String[]{uiContact.getDisplayName()}))
                    .showDialog();
            }
        }

        if (t.isDataFlavorSupported(DataFlavor.stringFlavor))
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

                StringBuilder buffToPaste = new StringBuilder();
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
                if (logger.isDebugEnabled())
                    logger.debug("Failed to drop string.", ufe);
            }
            catch (IOException ioe)
            {
                if (logger.isDebugEnabled())
                    logger.debug("Failed to drop string.", ioe);
            }
        }

        return false;
    }
}
