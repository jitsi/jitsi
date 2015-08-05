/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Copyright @ 2015 Atlassian Pty Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
    private final DefaultListModel listModel = new DefaultListModel();

    private final ConfigurationFrame configFrame;

    /**
     * Creates an instance of <tt>ConfigFormList</tt>
     * @param configFrame the parent configuration frame
     */
    public ConfigFormList(ConfigurationFrame configFrame)
    {
        this.configFrame = configFrame;

        this.setOpaque(false);
        this.setLayoutOrientation(JList.HORIZONTAL_WRAP);
        this.setVisibleRowCount(1);
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
        ConfigFormDescriptor descriptor = findDescriptor(configForm);

        if (descriptor != null)
            listModel.removeElement(descriptor);
    }

    /**
     * Selects the given <tt>ConfigurationForm</tt>.
     *
     * @param configForm the <tt>ConfigurationForm</tt> to select
     */
    public void setSelected(ConfigurationForm configForm)
    {
        ConfigFormDescriptor descriptor = findDescriptor(configForm);

        if (descriptor != null)
        {
            setSelectedValue(descriptor, true);
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

    /**
     * Finds the list descriptor corresponding the given
     * <tt>ConfigurationForm</tt>.
     *
     * @param configForm the <tt>ConfigurationForm</tt>, which descriptor we're
     * looking for
     * @return the list descriptor corresponding the given
     * <tt>ConfigurationForm</tt>
     */
    private ConfigFormDescriptor findDescriptor(ConfigurationForm configForm)
    {
        for(int i = 0; i < listModel.getSize(); i++)
        {
            ConfigFormDescriptor descriptor
                = (ConfigFormDescriptor) listModel.getElementAt(i);

            if(descriptor.getConfigForm().equals(configForm))
            {
                return descriptor;
            }
        }

        return null;
    }
}
