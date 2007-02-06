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
    
    private I18NString createString = Messages.getI18NString("create");
    
    private I18NString cancelString = Messages.getI18NString("cancel");
    
    private JButton addButton = new JButton(createString.getText());
    
    private JButton cancelButton = new JButton(cancelString.getText());
    
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
                
        this.init();
    }
    
    /**
     * Initializes the dialog.
     */
    private void init() {
        this.setTitle(Messages.getI18NString("addGroup").getText());
        
        this.getRootPane().setDefaultButton(addButton);
        this.addButton.setName("create");
        this.cancelButton.setName("cancel");
        
        this.addButton.addActionListener(this);
        this.cancelButton.addActionListener(this);
        
        this.addButton.setMnemonic(createString.getMnemonic());
        this.cancelButton.setMnemonic(cancelString.getMnemonic());
        
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
                                
                            new ErrorDialog(mainFrame,
                                    Messages.getI18NString(
                                            "addGroupExistError",
                                            groupName).getText(),
                                            ex,
                                    Messages.getI18NString(
                                            "addGroupErrorTitle").getText())
                                            .showDialog();
                        }
                        else if (errorCode
                            == MetaContactListException.CODE_LOCAL_IO_ERROR) {
                            
                            new ErrorDialog(mainFrame,
                                Messages.getI18NString(
                                        "addGroupLocalError",
                                        groupName).getText(),
                                        ex,
                                Messages.getI18NString(
                                        "addGroupErrorTitle").getText())
                                        .showDialog();
                        }
                        else if (errorCode
                                == MetaContactListException.CODE_NETWORK_ERROR) {
                            
                            new ErrorDialog(mainFrame,
                                    Messages.getI18NString(
                                            "addGroupNetError",
                                            groupName).getText(),
                                            ex,
                                    Messages.getI18NString(
                                            "addGroupErrorTitle").getText())
                                            .showDialog();
                        }
                        else {
                            
                            new ErrorDialog(mainFrame,
                                    Messages.getI18NString(
                                            "addGroupError",
                                            groupName).getText(),
                                            ex,
                                    Messages.getI18NString(
                                            "addGroupErrorTitle").getText())
                                            .showDialog();
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
