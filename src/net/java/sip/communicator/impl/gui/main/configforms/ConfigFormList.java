/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main.configforms;

import javax.swing.*;
import javax.swing.event.*;

import net.java.sip.communicator.service.gui.*;

/**
 * The list containing all <tt>ConfigurationForm</tt>s.
 * 
 * @author Yana Stamcheva
 */
public class ConfigFormList
    extends JList
    implements ListSelectionListener
{
    private DefaultListModel listModel = new DefaultListModel();
    
    private ConfigurationFrame configFrame;
    
    /**
     * Creates an instance of <tt>ConfigFormList</tt>
     */
    public ConfigFormList(ConfigurationFrame configFrame)
    {
        this.configFrame = configFrame;
        
        this.setCellRenderer(new ConfigFormListCellRenderer());
        this.setModel(listModel);
        
        this.addListSelectionListener(this);
    }

    /**
     * Adds a new <tt>ConfigurationForm</tt> to this list. 
     * @param configForm The <tt>ConfigurationForm</tt> to add.
     */
    public void addConfigForm(ConfigurationForm configForm)
    {
        listModel.addElement(configForm);
    }

    /**
     * Removes a <tt>ConfigurationForm</tt> from this list. 
     * @param configForm The <tt>ConfigurationForm</tt> to remove.
     */
    public void removeConfigForm(ConfigurationForm configForm)
    {
        listModel.removeElement(configForm);
    }

    public void valueChanged(ListSelectionEvent e)
    {
        if(!e.getValueIsAdjusting())
        {
            ConfigurationForm configForm
                = (ConfigurationForm) this.getSelectedValue();
            
            configFrame.showFormContent(configForm);
        }
    }
}
