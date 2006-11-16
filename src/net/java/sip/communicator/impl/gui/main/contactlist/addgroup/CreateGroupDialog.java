/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.contactlist.addgroup;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.customcontrols.*;
import net.java.sip.communicator.impl.gui.i18n.*;
import net.java.sip.communicator.impl.gui.main.*;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.util.*;

/**
 * The <tt>CreateGroupDialog</tt> is the dialog containing the form for creating
 * a group.
 * @author Yana Stamcheva
 */
public class CreateGroupDialog
    extends SIPCommDialog
    implements ActionListener {
    
    private Logger logger = Logger.getLogger(CreateGroupDialog.class.getName());
    
    private CreateGroupPanel groupPanel = new CreateGroupPanel();
    
    private JButton addButton = new JButton(Messages.getString("create"));
    
    private JButton cancelButton = new JButton(Messages.getString("cancel"));
    
    private JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    
    private JPanel mainPanel = new JPanel(new BorderLayout());
    
    private MetaContactListService clist;
    
    private MainFrame mainFrame;
    /**
     * Creates an instance of <tt>CreateGroupDialog</tt> that represents a dialog
     * that creates a new contact group.
     * 
     * @param mainFrame The MainFrame window.
     */
    public CreateGroupDialog(MainFrame mainFrame) {
        
        super(mainFrame);
        
        this.mainFrame = mainFrame;
        this.clist = mainFrame.getContactList();
        
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        
        this.setSize(520, 250);
        
        this.setLocation((int)screenSize.getWidth()/2 - getWidth()/2,
                (int)screenSize.getHeight()/2 - getHeight()/2);
                
        this.init();
    }
    
    /**
     * Initializes the dialog.
     */
    private void init() {
        this.setTitle(Messages.getString("addGroup"));
        
        this.getRootPane().setDefaultButton(addButton);
        this.addButton.setName("create");
        this.cancelButton.setName("cancel");
        
        this.addButton.addActionListener(this);
        this.cancelButton.addActionListener(this);
        
        this.buttonsPanel.add(addButton);
        this.buttonsPanel.add(cancelButton);
        
        this.mainPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 0, 10));
        
        this.mainPanel.add(groupPanel, BorderLayout.CENTER);
        this.mainPanel.add(buttonsPanel, BorderLayout.SOUTH);
        
        this.getContentPane().add(mainPanel);
    }
    
    public void actionPerformed(ActionEvent e) {
        JButton button = (JButton)e.getSource();
        String name = button.getName();
        
        if (name.equals("create")) {
            new CreateGroup(clist, groupPanel.getGroupName()).start();
        }
        
        this.dispose();
    }
    
    /**
     * Creates a new meta contact group in a separate thread.
     */
    private class CreateGroup extends Thread {
        MetaContactListService mcl;
        String groupName;
        
        CreateGroup(MetaContactListService mcl,
                String groupName) {
            this.mcl = mcl;
            this.groupName = groupName;
        }
        
        public void run() {
            new Thread() {
                public void run() {
                    try {
                        mcl.createMetaContactGroup(
                            mcl.getRoot(), groupName);
                    }
                    catch (MetaContactListException ex) {
                        logger.error(ex);
                        int errorCode = ex.getErrorCode();
                        
                        if (errorCode
                                == MetaContactListException
                                    .CODE_CONTACT_ALREADY_EXISTS_ERROR) {
                                
                                JOptionPane.showMessageDialog(mainFrame,
                                    Messages.getString(
                                            "addGroupExistError",
                                            groupName),
                                    Messages.getString(
                                            "addGroupErrorTitle"),
                                    JOptionPane.WARNING_MESSAGE);
                        }
                        else if (errorCode
                            == MetaContactListException.CODE_LOCAL_IO_ERROR) {
                            
                            JOptionPane.showMessageDialog(mainFrame,
                                Messages.getString(
                                        "addGroupLocalError",
                                        groupName),
                                Messages.getString(
                                        "addGroupErrorTitle"),
                                JOptionPane.WARNING_MESSAGE);
                        }
                        else if (errorCode
                                == MetaContactListException.CODE_NETWORK_ERROR) {
                            
                            JOptionPane.showMessageDialog(mainFrame,
                                    Messages.getString(
                                            "addGroupNetError",
                                            groupName),
                                    Messages.getString(
                                            "addGroupErrorTitle"),
                                    JOptionPane.WARNING_MESSAGE);
                        }
                        else {
                            
                            JOptionPane.showMessageDialog(mainFrame,
                                    Messages.getString(
                                            "addGroupError",
                                            groupName),
                                    Messages.getString(
                                            "addGroupErrorTitle"),
                                    JOptionPane.WARNING_MESSAGE);
                        }
                    }
                }
            }.start();
        }
    }

    protected void close(boolean isEscaped)
    {
        this.cancelButton.doClick();
    }
}
