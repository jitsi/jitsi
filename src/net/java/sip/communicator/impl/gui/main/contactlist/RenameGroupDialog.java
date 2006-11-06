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

    private RenameGroupPanel renameGroupPanel 
        = new RenameGroupPanel();
    
    private JButton renameButton = new JButton(Messages.getString("rename"));
    
    private JButton cancelButton = new JButton(Messages.getString("cancel"));
    
    private JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    
    private JPanel mainPanel = new JPanel(new BorderLayout());
    
    private MetaContactListService clist;
    
    private MetaContactGroup metaGroup;
        
    /**
     * Creates an instance of <tt>RenameGroupDialog</tt>.
     * 
     * @param clist The <tt>MetaContactListService</tt>.
     * @param metaGroup The <tt>MetaContactGroup</tt> to rename.
     */
    public RenameGroupDialog(MetaContactListService clist,
            MetaContactGroup metaGroup) {
        
        this.setSize(new Dimension(520, 270));
        
        this.clist = clist;
        this.metaGroup = metaGroup;
        
        this.init();
    }
    
    /**
     * Initializes the <tt>RenameGroupDialog</tt> by adding the buttons,
     * fields, etc.
     */
    private void init() {
        this.setTitle(Messages.getString("renameContact"));
        
        this.renameButton.setName("rename");
        this.cancelButton.setName("cancel");
        
        this.renameButton.setMnemonic(
                Messages.getString("mnemonic.renameContactButton").charAt(0));
        this.cancelButton.setMnemonic(
                Messages.getString("mnemonic.cancel").charAt(0));
        
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

    protected void close()
    {
        this.cancelButton.doClick();
    }
}
