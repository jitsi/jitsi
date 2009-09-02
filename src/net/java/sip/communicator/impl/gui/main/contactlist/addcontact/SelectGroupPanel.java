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

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.customcontrols.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.util.swing.*;

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
        GuiActivator.getResources().getI18NString("service.gui.SELECT_GROUP")
            + ": ");
    
    private JComboBox groupCombo = new JComboBox();
    
    private SIPCommMsgTextArea infoLabel = new SIPCommMsgTextArea();
    
    private JLabel infoTitleLabel = new JLabel(
        GuiActivator.getResources()
            .getI18NString("service.gui.SELECT_GROUP_WIZARD"));
    
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

        Font font = infoTitleLabel.getFont();
        infoTitleLabel.setFont(font.deriveFont(Font.BOLD, font.getSize2D() + 6));

        this.labelsPanel.add(infoTitleLabel);
        this.labelsPanel.add(infoLabel);
        this.labelsPanel.add(groupPanel);

        this.rightPanel.setBorder(
            BorderFactory.createEmptyBorder(0, 10, 10, 10));

        this.rightPanel.add(labelsPanel, BorderLayout.NORTH);

        this.add(iconLabel, BorderLayout.WEST);
        this.add(rightPanel, BorderLayout.CENTER);

//      groupCombo.addItem(new GroupWrapper(
//      GuiActivator.getResources().getI18NString("service.gui.ROOT_GROUP").getText(),
//      wizard.getRootGroup()));

        String lastGroupName = ConfigurationManager.getLastContactParent();
        
        Object lastSelectedGroup = null;
        
        Iterator<MetaContactGroup> groupsList = wizard.getMainFrame().getAllGroups();

        if (groupsList.hasNext())
        {
            infoLabel.setText(
                GuiActivator.getResources().
                    getI18NString("service.gui.SELECT_GROUP_WIZARD_MSG"));

            this.groupPanel.add(groupLabel, BorderLayout.WEST);
            this.groupPanel.add(groupCombo, BorderLayout.CENTER);

            while(groupsList.hasNext())
            {
                MetaContactGroup group = groupsList.next();
                
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
            infoLabel.setText(GuiActivator.getResources()
                .getI18NString("service.gui.CREATE_FIRST_GROUP_WIZARD"));
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

    private static class GroupWrapper
    {
        private final String groupName;

        private final MetaContactGroup group;

        public GroupWrapper(MetaContactGroup group)
        {
            this(group, group.getGroupName());
        }

        public GroupWrapper(MetaContactGroup group, String groupName)
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
