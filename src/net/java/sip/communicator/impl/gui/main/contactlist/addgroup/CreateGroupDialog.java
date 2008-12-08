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

import net.java.sip.communicator.impl.gui.i18n.*;
import net.java.sip.communicator.impl.gui.main.*;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.swing.*;

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
    
    private TransparentPanel buttonsPanel
        = new TransparentPanel(new FlowLayout(FlowLayout.RIGHT));
    
    private TransparentPanel mainPanel
        = new TransparentPanel(new BorderLayout());
    
    private MetaContactListService clist;
    
    private MainFrame mainFrame;
    /**
     * Creates an instance of <tt>CreateGroupDialog</tt> that represents a dialog
     * that creates a new contact group.
     * 
     * @param mainFrame The MainFrame window.
     */
    public CreateGroupDialog(MainFrame mainFrame)
    {
        super(mainFrame);

        this.mainFrame = mainFrame;
        this.clist = mainFrame.getContactList();

        this.setSize(520, 250);

        this.init();
    }
    
    /**
     * Initializes the dialog.
     */
    private void init()
    {
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
    
    public void actionPerformed(ActionEvent e)
    {
        JButton button = (JButton)e.getSource();
        String name = button.getName();
        
        if (name.equals("create"))
        {
            new CreateGroup(clist, groupPanel.getGroupName()).start();
        }
        else if(name.equals("cancel"))
        {
            dispose();
        }
    }
    
    /**
     * Creates a new meta contact group in a separate thread.
     */
    private class CreateGroup extends Thread {
        MetaContactListService mcl;
        String groupName;
        
        CreateGroup(MetaContactListService mcl, String groupName)
        {
            this.mcl = mcl;
            this.groupName = groupName;
        }
        
        public void run()
        {
            new Thread()
            {
                public void run()
                {
                    try
                    {
                        mcl.createMetaContactGroup(
                            mcl.getRoot(), groupName);

                        dispose();
                    }
                    catch (MetaContactListException ex)
                    {
                        logger.error(ex);
                        int errorCode = ex.getErrorCode();

                        if (errorCode
                                == MetaContactListException
                                    .CODE_GROUP_ALREADY_EXISTS_ERROR)
                        {
                            groupPanel.showErrorMessage(
                                    Messages.getI18NString(
                                            "addGroupExistError",
                                            new String[]{groupName}).getText());
                            
                            return;
                        }
                        else if (errorCode
                            == MetaContactListException.CODE_LOCAL_IO_ERROR)
                        {
                            groupPanel.showErrorMessage(
                                    Messages.getI18NString(
                                            "addGroupLocalError",
                                            new String[]{groupName}).getText());
                            
                            return;
                        }
                        else if (errorCode
                                == MetaContactListException.CODE_NETWORK_ERROR)
                        {
                            groupPanel.showErrorMessage(
                                    Messages.getI18NString(
                                            "addGroupNetError",
                                            new String[]{groupName}).getText());
                            
                            return;
                        }
                        else
                        {
                            groupPanel.showErrorMessage(
                                    Messages.getI18NString(
                                            "addGroupError",
                                            new String[]{groupName}).getText());

                            return;
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