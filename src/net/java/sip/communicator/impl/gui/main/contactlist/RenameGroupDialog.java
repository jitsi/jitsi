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
import net.java.sip.communicator.service.contactlist.*;

/**
 * The <tt>RenameGroupDialog</tt> is the dialog containing the form for
 * renaming a group. 
 * 
 * @author Yana Stamcheva
 */
public class RenameGroupDialog
    extends SIPCommDialog
    implements ActionListener {

    private RenameGroupPanel renameGroupPanel;
    
    private I18NString renameString = Messages.getI18NString("rename");
    
    private I18NString cancelString = Messages.getI18NString("cancel");
    
    private JButton renameButton = new JButton(renameString.getText());
    
    private JButton cancelButton = new JButton(cancelString.getText());
    
    private TransparentPanel buttonsPanel
        = new TransparentPanel(new FlowLayout(FlowLayout.RIGHT));
    
    private TransparentPanel mainPanel
        = new TransparentPanel(new BorderLayout());
    
    private MetaContactListService clist;
    
    private MetaContactGroup metaGroup;
        
    /**
     * Creates an instance of <tt>RenameGroupDialog</tt>.
     * 
     * @param mainFrame The main application window.
     * @param metaGroup The <tt>MetaContactGroup</tt> to rename.
     */
    public RenameGroupDialog(MainFrame mainFrame,
            MetaContactGroup metaGroup) {
        super(mainFrame);
        
        this.setSize(new Dimension(520, 270));
        
        this.clist = mainFrame.getContactList();
        this.metaGroup = metaGroup;
        
        this.renameGroupPanel = new RenameGroupPanel(metaGroup.getGroupName());
        
        this.init();
    }
    
    /**
     * Initializes the <tt>RenameGroupDialog</tt> by adding the buttons,
     * fields, etc.
     */
    private void init() {
        this.setTitle(Messages.getI18NString("renameGroup").getText());
        
        this.getRootPane().setDefaultButton(renameButton);
        this.renameButton.setName("rename");
        this.cancelButton.setName("cancel");
        
        this.renameButton.setMnemonic(renameString.getMnemonic());
        this.cancelButton.setMnemonic(cancelString.getMnemonic());
        
        this.renameButton.addActionListener(this);
        this.cancelButton.addActionListener(this);
        
        this.buttonsPanel.add(renameButton);
        this.buttonsPanel.add(cancelButton);
        
        this.mainPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 0, 10));
        
        this.mainPanel.add(renameGroupPanel, BorderLayout.NORTH);
        this.mainPanel.add(buttonsPanel, BorderLayout.SOUTH);
        
        this.getContentPane().add(mainPanel);
    }
    
    /**
     * Handles the <tt>ActionEvent</tt>. In order to rename the group invokes
     * the <code>renameMetaContactGroup</code> method of the current
     * <tt>MetaContactListService</tt>.
     */
    public void actionPerformed(ActionEvent e) {
        JButton button = (JButton)e.getSource();
        String name = button.getName();
        
        if (name.equals("rename")) {
            if (metaGroup != null) {
                new Thread() {
                    public void run() {
                        clist.renameMetaContactGroup(
                                metaGroup, renameGroupPanel.getNewName());
                    }
                }.start();
            }
            this.dispose();
        }
        else {
            this.dispose();
        }
    }
    
    /**
     * Requests the focus in the text field contained in this
     * dialog.
     */
    public void requestFocusInFiled() {
        this.renameGroupPanel.requestFocusInField();
    }

    protected void close(boolean isEscaped)
    {
        this.cancelButton.doClick();
    }
}
