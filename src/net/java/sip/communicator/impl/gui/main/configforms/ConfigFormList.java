/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.configforms;

import java.awt.*;

import javax.swing.*;
import javax.swing.event.*;

import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.util.swing.*;

/**
 * The list containing all <tt>ConfigurationForm</tt>s.
 * 
 * @author Yana Stamcheva
 */
public class ConfigFormList
    extends JList
    implements ListSelectionListener
{
    private final DefaultListModel listModel = new DefaultListModel();

    private final ConfigurationFrame configFrame;

    private static final Color gradientStartColor
        = new Color(255, 255, 255, 200);

    private static final Color gradientEndColor
        = new Color(255, 255, 255, 200);

    /**
     * Creates an instance of <tt>ConfigFormList</tt>
     */
    public ConfigFormList(ConfigurationFrame configFrame)
    {
        this.configFrame = configFrame;

        this.setOpaque(false);
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
        if (configForm == null)
            throw new IllegalArgumentException("configForm");

        int i = 0;
        int count = listModel.size();
        int configFormIndex = configForm.getIndex();
        for (; i < count; i++)
        {
            ConfigFormDescriptor descriptor
                = (ConfigFormDescriptor) listModel.get(i);

            if (configFormIndex < descriptor.getConfigForm().getIndex())
                break;
        }
        listModel.add(i, new ConfigFormDescriptor(configForm));
    }

    /**
     * Removes a <tt>ConfigurationForm</tt> from this list. 
     * @param configForm The <tt>ConfigurationForm</tt> to remove.
     */
    public void removeConfigForm(ConfigurationForm configForm)
    {
        for(int count = listModel.getSize(), i = count - 1; i >= 0; i--)
        {
            ConfigFormDescriptor descriptor
                = (ConfigFormDescriptor) listModel.get(i);

            if(descriptor.getConfigForm().equals(configForm))
            {
                listModel.remove(i);
                /*
                 * TODO We may just consider not allowing duplicates on addition
                 * and then break here.
                 */
            }
        }
    }

    /**
     * Called when user selects a component in the list of configuration forms.
     */
    public void valueChanged(ListSelectionEvent e)
    {
        if(!e.getValueIsAdjusting())
        {
            ConfigFormDescriptor configFormDescriptor
                = (ConfigFormDescriptor) this.getSelectedValue();

            if(configFormDescriptor != null)
                configFrame.showFormContent(configFormDescriptor);
        }
    }
    
    public void paintComponent(Graphics g)
    {
        Graphics2D g2 = (Graphics2D) g;

        AntialiasingManager.activateAntialiasing(g2);

        int width = getWidth();
        int height = getHeight();
        GradientPaint p =
            new GradientPaint(width / 2,
                              0,
                              gradientStartColor,
                              width / 2,
                              height,
                              gradientEndColor);

        g2.setPaint(p);
        g2.fillRoundRect(0, 0, width, height, 10, 10);

        super.paintComponent(g2);
    }
}
