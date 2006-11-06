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
 * The <tt>RenameContactDialog</tt> is the dialog containing the form for
 * renaming a contact. 
 * 
 * @author Yana Stamcheva
 */
public class RenameContactDialog
    extends SIPCommDialog
    implements ActionListener {

    private RenameContactPanel renameContactPanel 
        = new RenameContactPanel();
    
    private JButton renameButton = new JButton(Messages.getString("rename"));
    
    private JButton cancelButton = new JButton(Messages.getString("cancel"));
    
    private JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    
    private JPanel mainPanel = new JPanel(new BorderLayout());
    
    private MetaContactListService clist;
    
    private MetaContact metaContact;
        
    /**
     * Creates an instance of <tt>RenameContactDialog</tt>.
     * 
     * @param clist The <tt>MetaContactListService</tt>.
     * @param metaContact The <tt>MetaContact</tt> to rename.
     */
    public RenameContactDialog(MetaContactListService clist,
            MetaContact metaContact) {
        
        this.setSize(new Dimension(520, 270));
        
        this.clist = clist;
        this.metaContact = metaContact;
        
        this.init();
    }
    
    /**
     * Initializes the <tt>RenameContactDialog</tt> by adding the buttons,
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
        
        this.mainPanel.add(renameContactPanel, BorderLayout.NORTH);
        this.mainPanel.add(buttonsPanel, BorderLayout.SOUTH);
        
        this.getContentPane().add(mainPanel);
    }
    
    /**
     * Handles the <tt>ActionEvent</tt>. In order to rename the contact invokes
     * the <code>renameMetaContact</code> method of the current
     * <tt>MetaContactListService</tt>.
     */
    public void actionPerformed(ActionEvent e) {
        JButton button = (JButton)e.getSource();
        String name = button.getName();
        
        if (name.equals("rename")) {
            if (metaContact != null) {
                new Thread() {
                    public void run() {
                        clist.renameMetaContact(
                            metaContact, renameContactPanel.getNewName());
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
        this.renameContactPanel.requestFocusInField();
    }

    protected void close()
    {
        this.cancelButton.doClick();
    }
}
