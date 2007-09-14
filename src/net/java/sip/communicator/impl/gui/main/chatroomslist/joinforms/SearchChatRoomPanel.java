/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.chatroomslist.joinforms;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.i18n.*;
import net.java.sip.communicator.util.*;

public class SearchChatRoomPanel
    extends JPanel
    implements ActionListener
{
    private Logger logger = Logger.getLogger(SearchChatRoomPanel.class);
    
    private JPanel searchPanel = new JPanel(new GridLayout(0, 1));
    
    private JTextArea searchTextArea = new JTextArea(
        Messages.getI18NString("searchForChatRoomsText").getText());
    
    private JButton searchButton = new JButton(
        Messages.getI18NString("search").getText());
    
    private JPanel buttonPanel = new JPanel(
        new FlowLayout(FlowLayout.CENTER));
    
    private JoinChatRoomDialog joinChatRoomDialog;
    
    /**
     * 
     * @param joinChatRoomDialog
     */
    public SearchChatRoomPanel(JoinChatRoomDialog joinChatRoomDialog)
    {   
        super(new BorderLayout());
        
        this.joinChatRoomDialog = joinChatRoomDialog;
        
        this.buttonPanel.add(searchButton);
        
        this.searchTextArea.setOpaque(false);
        this.searchTextArea.setEditable(false);
        this.searchTextArea.setLineWrap(true);
        this.searchTextArea.setWrapStyleWord(true);

        this.searchPanel.add(searchTextArea);
        this.searchPanel.add(buttonPanel);

        this.searchPanel.setBorder(BorderFactory
                .createTitledBorder(Messages.getI18NString("search").getText()));

        this.searchButton.addActionListener(this);
        
        this.add(searchPanel);
    }

    /**
     * Invoked when the Search button is clicked.
     */
    public void actionPerformed(ActionEvent e)
    {
        new Thread()
        {
            public void run()
            {
                SwingUtilities.invokeLater(new Runnable()
                {
                    public void run()
                    {
                        joinChatRoomDialog.loadChatRoomsList();
                    }
                });
            }
        }.start();

        joinChatRoomDialog.setCursor(
            Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
    }
}
