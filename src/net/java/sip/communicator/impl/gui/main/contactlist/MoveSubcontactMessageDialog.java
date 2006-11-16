/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.contactlist;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.customcontrols.*;
import net.java.sip.communicator.impl.gui.i18n.*;
import net.java.sip.communicator.impl.gui.main.*;
import net.java.sip.communicator.impl.gui.utils.*;

/**
 * The <tt>MoveSubcontactMessageDialog</tt> is the the dialog shown when user
 * tries to move a subcontact in the contact list. It is meant to inform the
 * user that she should select another meta contact, where the previously
 * choosen contact will be moved.
 * 
 * @author Yana Stamcheva
 */
public class MoveSubcontactMessageDialog
    extends SIPCommDialog
{
    private SIPCommMsgTextArea infoArea 
        = new SIPCommMsgTextArea(Messages.getString("moveSubcontactMsg"));
    
    private JLabel infoTitleLabel 
        = new JLabel(Messages.getString("moveSubcontact"));
    
    private JLabel iconLabel = new JLabel(
            new ImageIcon(ImageLoader.getImage(ImageLoader.WARNING_ICON)));
    
    private JButton cancelButton = new JButton(Messages.getString("cancel"));
    
    private JPanel labelsPanel = new JPanel(new GridLayout(0, 1));
    private JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
    private JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    
    private int dialogWidth = 300;
    private int dialogHeight = 100;
    
    private MainFrame mainFrame;
    private ContactListListener clistListener;
    
    /**
     * Creates an instance of MoveSubcontactMessageDialog and constructs
     * all panels contained in this dialog.
     * @param mainFrame the main application window
     */
    public MoveSubcontactMessageDialog(MainFrame parentWindow,
            ContactListListener listener)
    {
        super(parentWindow);
        
        this.mainFrame = parentWindow;
        this.clistListener = listener;
        
        this.mainPanel.setPreferredSize(
                new Dimension(dialogWidth, dialogHeight));
        
        this.computeDialogLocation(mainFrame);
        
        this.cancelButton.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e) {
                dispose();
                
                ContactList clist
                    = mainFrame.getContactListPanel().getContactList();
                
                clist.removeExcContactListListener(clistListener);
                
                clist.setCursor(
                        Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            }
        });
        
        this.infoTitleLabel.setHorizontalAlignment(JLabel.CENTER);
        this.infoTitleLabel.setFont(Constants.FONT.deriveFont(Font.BOLD, 18));
        
        this.labelsPanel.add(infoTitleLabel);
        this.labelsPanel.add(infoArea);
        
        this.mainPanel.setBorder(
                BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        this.buttonsPanel.add(cancelButton);
        
        this.mainPanel.add(labelsPanel, BorderLayout.CENTER);
        this.mainPanel.add(iconLabel, BorderLayout.WEST);
        this.mainPanel.add(buttonsPanel, BorderLayout.SOUTH);
        
        this.getContentPane().add(mainPanel);
        this.pack();
    }

    /**
     * Computes the location of this dialog in order to show it on the left
     * or the right side of the main application window.
     * @param parentWindow the main application window
     */
    private void computeDialogLocation(JFrame parentWindow)
    {
        int dialogY = (int) Toolkit.getDefaultToolkit()
            .getScreenSize().getHeight()/2 - dialogHeight/2;
        
        int parentX = parentWindow.getLocation().x;
        
        if ((parentX - dialogWidth) > 0) {
            this.setLocation(parentX - dialogWidth,
                dialogY);
        }
        else {
            this.setLocation(parentX + parentWindow.getWidth(),
                    dialogY);
        }
    }

    protected void close(boolean isEscaped)
    {
        this.cancelButton.doClick();
    }
}
