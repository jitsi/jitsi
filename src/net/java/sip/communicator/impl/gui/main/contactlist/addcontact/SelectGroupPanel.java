/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.contactlist.addcontact;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.customcontrols.*;
import net.java.sip.communicator.impl.gui.i18n.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.gui.*;

/**
 * The <tt>SelectGroupPanel</tt> is where the user should select the group,
 * in which the new contact will be added.
 * 
 * @author Yana Stamcheva
 */
public class SelectGroupPanel
    extends JPanel
    implements  ItemListener
{

    private JPanel groupPanel = new JPanel(new BorderLayout()); 
    
    private JLabel groupLabel = new JLabel(
            Messages.getI18NString("selectGroup").getText() + ": ");
    
    private JComboBox groupCombo = new JComboBox();
    
    private SIPCommMsgTextArea infoLabel = new SIPCommMsgTextArea(
            Messages.getI18NString("selectGroupWizard").getText());
    
    private JLabel infoTitleLabel = new JLabel(
            Messages.getI18NString("selectGroupWizardTitle").getText());
    
    private JPanel labelsPanel = new JPanel(new GridLayout(0, 1));
    
    private JPanel rightPanel = new JPanel(new BorderLayout(10, 10));
   
    private JPanel rightNorthPanel = new JPanel();
    
    private JLabel iconLabel = new JLabel(new ImageIcon(ImageLoader
            .getImage(ImageLoader.ADD_CONTACT_WIZARD_ICON)));
    
    private NewContact newContact;
    
    private WizardContainer parentWizard;
    
    /**
     * Creates an instance of <tt>SelectGroupPanel</tt>.
     * 
     * @param newContact An object that collects all user choices through the
     * wizard.
     * @param groupsList The list of all <tt>MetaContactGroup</tt>s, from which
     * the user could select.
     */
    public SelectGroupPanel(WizardContainer wizard, NewContact newContact, 
            Iterator groupsList) {
        super(new BorderLayout(10, 10));
    
        this.setPreferredSize(new Dimension(500, 200));
        
        this.iconLabel.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 10));
        
        this.parentWizard = wizard;
        
        this.newContact = newContact;
        
        this.groupCombo.setPreferredSize(new Dimension(300, 22));
        this.groupCombo.setEditable(true);
        this.groupCombo.addItemListener(this);
        
        while(groupsList.hasNext())
        {   
            MetaContactGroup group
                = (MetaContactGroup)groupsList.next();
            
            groupCombo.addItem(new GroupWrapper(group));
        }
        
        this.infoLabel.setEditable(false);
        
        this.infoTitleLabel.setHorizontalAlignment(JLabel.CENTER);
        this.infoTitleLabel.setFont(Constants.FONT.deriveFont(Font.BOLD, 18));
        
        this.labelsPanel.add(infoTitleLabel);
        this.labelsPanel.add(infoLabel);
        
        this.groupPanel.add(groupLabel, BorderLayout.WEST);
        this.groupPanel.add(groupCombo, BorderLayout.CENTER);
                
        this.rightNorthPanel.setLayout(
            new BoxLayout(rightNorthPanel, BoxLayout.Y_AXIS));
        
        this.rightNorthPanel.add(labelsPanel);
        this.rightNorthPanel.add(groupPanel);
     
        this.rightPanel.setBorder(BorderFactory.createEmptyBorder(10, 5, 10, 5));
        
        this.rightPanel.add(rightNorthPanel, BorderLayout.NORTH);
        
        this.add(iconLabel, BorderLayout.WEST);
        this.add(rightPanel, BorderLayout.CENTER);
    } 
    
    /**
     * Adds all selected from user contact groups in the new contact.
     */
    public void addNewContactGroup()
    {
        Object group = groupCombo.getSelectedItem();
        
        if (group instanceof GroupWrapper)
            newContact.addGroup(((GroupWrapper)group).getMetaGroup());
        else            
            newContact.setNewGroup(group.toString());
    }
    
    /**
     * 
     */
    public void setNextButtonAccordingToComboBox()
    {
        if(groupCombo.getSelectedItem() != null
            || groupCombo.getSelectedItem() != "")
        {
            parentWizard.setNextFinishButtonEnabled(true);
        }
        else
        {
            parentWizard.setNextFinishButtonEnabled(false);
        }
    }

    /**
     * Implements <tt>ItemListener.itemStateChanged</tt>.
     */
    public void itemStateChanged(ItemEvent e)
    {
        this.setNextButtonAccordingToComboBox();
    }
    
    private class GroupWrapper
    {
        private MetaContactGroup group;
        
        public GroupWrapper(MetaContactGroup group)
        {
            this.group = group;
        }
        
        public String toString()
        {
            return group.getGroupName();
        }
        
        public MetaContactGroup getMetaGroup()
        {
            return this.group;
        }
    }
}
