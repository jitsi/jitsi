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

/**
 * The <tt>SelectGroupPanel</tt> is where the user should select the group,
 * in which the new contact will be added.
 * 
 * @author Yana Stamcheva
 */
public class SelectGroupPanel
    extends TransparentPanel
    implements  ItemListener
{
    private TransparentPanel groupPanel
        = new TransparentPanel(new BorderLayout()); 
    
    private JLabel groupLabel = new JLabel(
            Messages.getI18NString("selectGroup").getText() + ": ");
    
    private JComboBox groupCombo = new JComboBox();
    
    private SIPCommMsgTextArea infoLabel = new SIPCommMsgTextArea();
    
    private JLabel infoTitleLabel = new JLabel(
            Messages.getI18NString("selectGroupWizardTitle").getText());
    
    private TransparentPanel labelsPanel
        = new TransparentPanel(new GridLayout(0, 1, 10, 10));
    
    private TransparentPanel rightPanel
        = new TransparentPanel(new BorderLayout());
    
    private JLabel iconLabel = new JLabel(new ImageIcon(ImageLoader
            .getImage(ImageLoader.ADD_CONTACT_WIZARD_ICON)));
    
    private AddContactWizard parentWizard;
    
    /**
     * Creates an instance of <tt>SelectGroupPanel</tt>.
     * 
     * @param wizard the wizard where this panel is contained
     * @param newContact An object that collects all user choices through the
     * wizard.
     * @param groupsList The list of all <tt>MetaContactGroup</tt>s, from which
     * the user could select.
     */
    public SelectGroupPanel(AddContactWizard wizard,
                            NewContact newContact)
    {
        super(new BorderLayout(10, 10));

        this.parentWizard = wizard;

        this.setBorder(BorderFactory
            .createEmptyBorder(10, 10, 10, 10));

        this.setPreferredSize(new Dimension(500, 200));

        this.iconLabel.setBorder(
            BorderFactory.createEmptyBorder(0, 10, 10, 10));

        this.groupCombo.setPreferredSize(new Dimension(300, 22));
        this.groupCombo.addItemListener(this);

        this.infoLabel.setEditable(false);

        this.infoTitleLabel.setHorizontalAlignment(JLabel.CENTER);
        this.infoTitleLabel.setFont(Constants.FONT.deriveFont(Font.BOLD, 18));

        this.labelsPanel.add(infoTitleLabel);
        this.labelsPanel.add(infoLabel);
        this.labelsPanel.add(groupPanel);

        this.rightPanel.setBorder(
            BorderFactory.createEmptyBorder(0, 10, 10, 10));

        this.rightPanel.add(labelsPanel, BorderLayout.NORTH);

        this.add(iconLabel, BorderLayout.WEST);
        this.add(rightPanel, BorderLayout.CENTER);

//      groupCombo.addItem(new GroupWrapper(
//      Messages.getI18NString("rootGroup").getText(),
//      wizard.getRootGroup()));

        String lastGroupName = ConfigurationManager.getLastContactParent();
        
        Object lastSelectedGroup = null;
        
        Iterator groupsList = wizard.getMainFrame().getAllGroups();

        if (groupsList.hasNext())
        {
            infoLabel.setText(
                Messages.getI18NString("selectGroupWizard").getText());

            this.groupPanel.add(groupLabel, BorderLayout.WEST);
            this.groupPanel.add(groupCombo, BorderLayout.CENTER);

            while(groupsList.hasNext())
            {
                MetaContactGroup group
                    = (MetaContactGroup)groupsList.next();
                
                GroupWrapper gr = new GroupWrapper(group);
                
                if(lastGroupName != null && 
                    lastGroupName.equals(group.getGroupName()))
                    lastSelectedGroup = gr;
                    
                groupCombo.addItem(gr);
            }
            
            if(lastSelectedGroup != null)
                groupCombo.setSelectedItem(lastSelectedGroup);
        }
        else
        {
            infoLabel.setForeground(Color.RED);
            infoLabel.setText(
                Messages.getI18NString("createFirstGroupWizard").getText());
        }
    }
    
    /**
     * Returns the selected group.
     * @return the selected group
     */
    public MetaContactGroup getSelectedGroup()
    {
        Object selectedGroup = groupCombo.getSelectedItem();

        if (selectedGroup != null)
            return ((GroupWrapper) selectedGroup).getMetaGroup();

        return null;
    }

    /**
     * 
     */
    public void setNextButtonAccordingToComboBox()
    {
        if(groupCombo.getSelectedItem() != null)
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
        private String groupName;

        private MetaContactGroup group;

        public GroupWrapper(MetaContactGroup group)
        {
            this.group = group;
            this.groupName = group.getGroupName();
        }

        public GroupWrapper(String groupName, MetaContactGroup group)
        {
            this.group = group;
            this.groupName = groupName;
        }

        public String toString()
        {
            return groupName;
        }

        public MetaContactGroup getMetaGroup()
        {
            return this.group;
        }
    }

}
