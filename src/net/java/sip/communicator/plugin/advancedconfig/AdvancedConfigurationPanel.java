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
package net.java.sip.communicator.plugin.advancedconfig;

import java.awt.*;

import javax.swing.*;
import javax.swing.event.*;

import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.util.*;

import org.osgi.framework.*;

/**
 * The advanced configuration panel.
 *
 * @author Yana Stamcheva
 */
public class AdvancedConfigurationPanel
    extends TransparentPanel
    implements  ConfigurationForm,
                ConfigurationContainer,
                ServiceListener,
                ListSelectionListener
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * The <tt>Logger</tt> used by this <tt>AdvancedConfigurationPanel</tt> for
     * logging output.
     */
    private final Logger logger
        = Logger.getLogger(AdvancedConfigurationPanel.class);

    /**
     * The configuration list.
     */
    private final JList configList = new JList();

    /**
     * The center panel.
     */
    private final JPanel centerPanel = new TransparentPanel(new BorderLayout());

    /**
     * Creates an instance of the <tt>AdvancedConfigurationPanel</tt>.
     */
    public AdvancedConfigurationPanel()
    {
        super(new BorderLayout(10, 0));

        initList();

        centerPanel.setPreferredSize(new Dimension(500, 500));

        add(centerPanel, BorderLayout.CENTER);
    }

    /**
     * Initializes the config list.
     */
    private void initList()
    {
        configList.setModel(new DefaultListModel());
        configList.setCellRenderer(new ConfigListCellRenderer());
        configList.addListSelectionListener(this);
        configList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JScrollPane configScrollList = new JScrollPane();

        configScrollList.getVerticalScrollBar().setUnitIncrement(30);

        configScrollList.getViewport().add(configList);

        add(configScrollList, BorderLayout.WEST);

        String osgiFilter = "("
            + ConfigurationForm.FORM_TYPE
            + "="+ConfigurationForm.ADVANCED_TYPE+")";
        ServiceReference[] confFormsRefs = null;
        try
        {
            confFormsRefs = AdvancedConfigActivator.bundleContext
                .getServiceReferences(  ConfigurationForm.class.getName(),
                                        osgiFilter);
        }
        catch (InvalidSyntaxException ex)
        {}

        if(confFormsRefs != null)
        {
            for (int i = 0; i < confFormsRefs.length; i++)
            {
                ConfigurationForm form
                    = (ConfigurationForm) AdvancedConfigActivator.bundleContext
                        .getService(confFormsRefs[i]);

                if (form.isAdvanced())
                    this.addConfigForm(form);
            }
        }
    }

    /**
     * Shows on the right the configuration form given by the given
     * <tt>ConfigFormDescriptor</tt>.
     *
     * @param configForm the configuration form to show
     */
    private void showFormContent(ConfigurationForm configForm)
    {
        this.centerPanel.removeAll();

        JComponent configFormPanel
            = (JComponent) configForm.getForm();

        configFormPanel.setOpaque(false);

        this.centerPanel.add(configFormPanel, BorderLayout.CENTER);

        this.centerPanel.revalidate();
        this.centerPanel.repaint();
    }

    /**
     * Handles registration of a new configuration form.
     * @param event the <tt>ServiceEvent</tt> that notified us
     */
    public void serviceChanged(ServiceEvent event)
    {
        Object sService
            = AdvancedConfigActivator.bundleContext
                .getService(event.getServiceReference());

        // we don't care if the source service is not a configuration form
        if (!(sService instanceof ConfigurationForm))
            return;

        ConfigurationForm configForm = (ConfigurationForm) sService;

        /*
         * This AdvancedConfigurationPanel is an advanced ConfigurationForm so
         * don't try to add it to itself.
         */
        if ((configForm == this) || !configForm.isAdvanced())
            return;

        switch (event.getType())
        {
        case ServiceEvent.REGISTERED:
            if (logger.isInfoEnabled())
                logger.info("Handling registration of a new Configuration Form.");

            this.addConfigForm(configForm);
            break;

        case ServiceEvent.UNREGISTERING:
            this.removeConfigForm(configForm);
            break;
        }
    }

    /**
     * Adds a new <tt>ConfigurationForm</tt> to this list.
     * @param configForm The <tt>ConfigurationForm</tt> to add.
     */
    public void addConfigForm(final ConfigurationForm configForm)
    {
        if(!SwingUtilities.isEventDispatchThread())
        {
            SwingUtilities.invokeLater(new Runnable()
            {
                public void run()
                {
                    addConfigForm(configForm);
                }
            });
            return;
        }

        if (configForm == null)
            throw new IllegalArgumentException("configForm");

        DefaultListModel listModel = (DefaultListModel) configList.getModel();

        int i = 0;
        int count = listModel.getSize();
        int configFormIndex = configForm.getIndex();
        for (; i < count; i++)
        {
            ConfigurationForm form = (ConfigurationForm) listModel.get(i);

            if (configFormIndex < form.getIndex())
                break;
        }
        listModel.add(i, configForm);
    }

    /**
     * Implements <code>ApplicationWindow.show</code> method.
     *
     * @param isVisible specifies whether the frame is to be visible or not.
     */
    @Override
    public void setVisible(boolean isVisible)
    {
        if (isVisible && configList.getSelectedIndex() < 0)
        {
            this.configList.setSelectedIndex(0);
        }
        super.setVisible(isVisible);
    }

    /**
     * Removes a <tt>ConfigurationForm</tt> from this list.
     * @param configForm The <tt>ConfigurationForm</tt> to remove.
     */
    public void removeConfigForm(final ConfigurationForm configForm)
    {
        if(!SwingUtilities.isEventDispatchThread())
        {
            SwingUtilities.invokeLater(new Runnable()
            {
                public void run()
                {
                    removeConfigForm(configForm);
                }
            });
            return;
        }

        DefaultListModel listModel = (DefaultListModel) configList.getModel();

        for(int count = listModel.getSize(), i = count - 1; i >= 0; i--)
        {
            ConfigurationForm form
                = (ConfigurationForm) listModel.get(i);

            if(form.equals(configForm))
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
     * A custom cell renderer that represents a <tt>ConfigurationForm</tt>.
     */
    private class ConfigListCellRenderer extends DefaultListCellRenderer
    {
        /**
         * Serial version UID.
         */
        private static final long serialVersionUID = 0L;

        private boolean isSelected = false;

        private final Color selectedColor
            = new Color(AdvancedConfigActivator.getResources().
                getColor("service.gui.LIST_SELECTION_COLOR"));

        /**
         * Creates an instance of <tt>ConfigListCellRenderer</tt> and specifies
         * that this renderer is transparent.
         */
        public ConfigListCellRenderer()
        {
            this.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            this.setOpaque(false);
        }

        /**
         * Returns the component representing the cell given by parameters.
         * @param list the parent list
         * @param value the value of the cell
         * @param index the index of the cell
         * @param isSelected indicates if the cell is selected
         * @param cellHasFocus indicates if the cell has the focus
         * @return the component representing the cell
         */
        @Override
        public Component getListCellRendererComponent(  JList list,
                                                        Object value,
                                                        int index,
                                                        boolean isSelected,
                                                        boolean cellHasFocus)
        {
            ConfigurationForm configForm = (ConfigurationForm) value;

            this.isSelected = isSelected;
            this.setText(configForm.getTitle());

            return this;
        }

        /**
         * Paint a background for all groups and a round blue border and
         * background when a cell is selected.
         * @param g the <tt>Graphics</tt> object
         */
        @Override
        public void paintComponent(Graphics g)
        {
            Graphics g2 = g.create();
            try
            {
                internalPaintComponent(g2);
            }
            finally
            {
                g2.dispose();
            }
            super.paintComponent(g);
        }

        /**
         * Paint a background for all groups and a round blue border and
         * background when a cell is selected.
         * @param g the <tt>Graphics</tt> object
         */
        private void internalPaintComponent(Graphics g)
        {
            AntialiasingManager.activateAntialiasing(g);

            Graphics2D g2 = (Graphics2D) g;

            if (isSelected)
            {
                g2.setColor(selectedColor);
                g2.fillRect(0, 0, this.getWidth(), this.getHeight());
            }
        }
    }

    /**
     * Called when user selects a component in the list of configuration forms.
     * @param e the <tt>ListSelectionEvent</tt>
     */
    public void valueChanged(ListSelectionEvent e)
    {
        if(!e.getValueIsAdjusting())
        {
            ConfigurationForm configForm
                = (ConfigurationForm) configList.getSelectedValue();

            if(configForm != null)
                showFormContent(configForm);
        }
    }

    /**
     * Selects the given <tt>ConfigurationForm</tt>.
     *
     * @param configForm the <tt>ConfigurationForm</tt> to select
     */
    public void setSelected(ConfigurationForm configForm)
    {
        configList.setSelectedValue(configForm, true);
    }

    /**
     * Returns the title of the form.
     * @return the title of the form
     */
    public String getTitle()
    {
        return AdvancedConfigActivator.getResources()
            .getI18NString("service.gui.ADVANCED");
    }

    /**
     * Returns the icon of the form.
     * @return a byte array containing the icon of the form
     */
    public byte[] getIcon()
    {
        return AdvancedConfigActivator.getResources()
            .getImageInBytes("plugin.advancedconfig.PLUGIN_ICON");
    }

    /**
     * Returns the form component.
     * @return the form component
     */
    public Object getForm()
    {
        return this;
    }

    /**
     * Returns the index of the form in its parent container.
     * @return the index of the form in its parent container
     */
    public int getIndex()
    {
        return 300;
    }

    /**
     * Indicates if the form is an advanced form.
     * @return <tt>true</tt> to indicate that this is an advanced form,
     * otherwise returns <tt>false</tt>
     */
    public boolean isAdvanced()
    {
        return false;
    }

    /**
     * Validates the currently selected configuration form. This method is meant
     * to be used by configuration forms the re-validate when a new component
     * has been added or size has changed.
     */
    public void validateCurrentForm() {}
}
