/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.chatroomslist;

import java.awt.*;
import java.awt.event.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.main.contactlist.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.contactsource.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.muc.*;

/**
 * A dialog that lists the existing chat rooms on the server.
 * 
 * @author Hristo Terezov
 */
public class ServerChatRoomsChoiceDialog
    extends OneChoiceInviteDialog
{

    /**
     * Generated serial id.
     */
    private static final long serialVersionUID = 428358553225114162L;
    
    /**
     * The contact source that generates the list of chat rooms.
     */
    private ContactSourceService contactSource;
    
    /**
     * Creates new instance of <tt>ServerChatRoomsChoiceDialog</tt>.
     * 
     * @param title the title of the window.
     * @param pps the protocol provider service associated with the list of chat 
     * rooms.
     */
    public ServerChatRoomsChoiceDialog(String title, 
        ChatRoomProviderWrapper pps)
    {
        super(title);
        contactList.setDefaultFilter(new SearchFilter(contactList));
        contactList.removeAllContactSources();
        contactSource = GuiActivator.getMUCService()
            .getServerChatRoomsContactSourceForProvider(pps);
        contactList.addContactSource(
            contactSource);
        
        setInfoText(GuiActivator.getResources().getI18NString(
            "service.gui.SERVER_CHAT_ROOMS_DIALOG_TEXT"));
        
        contactList.applyDefaultFilter();
        this.setMinimumSize(new Dimension(300, 300));
        addOkButtonListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                UIContact uiContact = getSelectedContact();

                if (uiContact != null)
                {
                    ChatRoomTableDialog.setChatRoomField(
                        uiContact.getDisplayName());
                }

                setVisible(false);
                dispose();
            }
        });
        addCancelButtonListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                setVisible(false);
                dispose();
            }
        });
    }
    
    /**
     * Handles provider change.
     * 
     * @param provider the provider.
     */
    public void changeProtocolProvider(ChatRoomProviderWrapper provider)
    {
        contactList.removeContactSource(contactSource);
        contactSource = GuiActivator.getMUCService()
            .getServerChatRoomsContactSourceForProvider(provider);
        contactList.addContactSource(contactSource);
        contactList.applyDefaultFilter();
    }
}
