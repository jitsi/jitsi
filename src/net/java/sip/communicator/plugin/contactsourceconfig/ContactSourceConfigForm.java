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
package net.java.sip.communicator.plugin.contactsourceconfig;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.gui.*;

import org.osgi.framework.*;

/**
 *
 * @author Yana Stamcheva
 */
public class ContactSourceConfigForm
    extends TransparentPanel
    implements ServiceListener
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * The drop down list of contact sources.
     */
    private final JComboBox contactSourceComboBox = new JComboBox();

    /**
     * Creates the <tt>ContactSourceConfigForm</tt>.
     */
    public ContactSourceConfigForm()
    {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        contactSourceComboBox.setRenderer(new ContactSourceRenderer());

        final JPanel centerPanel
            = new TransparentPanel(new BorderLayout(10, 10));
        centerPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        centerPanel.setPreferredSize(new Dimension(450, 300));

        contactSourceComboBox.addItemListener(new ItemListener()
        {
            public void itemStateChanged(ItemEvent event)
            {
                ConfigurationForm form
                    = (ConfigurationForm) contactSourceComboBox
                        .getSelectedItem();

                centerPanel.removeAll();
                JComponent c = (JComponent) form.getForm();
                c.setOpaque(false);

                centerPanel.add(c, BorderLayout.CENTER);

                centerPanel.revalidate();
                centerPanel.repaint();
            }
        });

        init();

        add(contactSourceComboBox);
        add(Box.createVerticalStrut(10));
        add(centerPanel);

        ContactSourceConfigActivator.bundleContext.addServiceListener(this);
    }

    /**
     * Initializes this panel.
     */
    private void init()
    {
        String osgiFilter = "("
            + ConfigurationForm.FORM_TYPE
            + "="+ConfigurationForm.CONTACT_SOURCE_TYPE+")";

        ServiceReference[] confFormsRefs = null;
        try
        {
            confFormsRefs = ContactSourceConfigActivator.bundleContext
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
                    = (ConfigurationForm) ContactSourceConfigActivator
                        .bundleContext.getService(confFormsRefs[i]);

                Object formComponent = form.getForm();
                if (formComponent instanceof Component)
                    addConfigForm(form);
            }
        }
    }

    /**
     * Handles registration of a new configuration form.
     * @param event the <tt>ServiceEvent</tt> that notified us
     */
    public void serviceChanged(ServiceEvent event)
    {
        ServiceReference serviceRef = event.getServiceReference();

        Object property = serviceRef.getProperty(ConfigurationForm.FORM_TYPE);
        if (property != ConfigurationForm.CONTACT_SOURCE_TYPE)
            return;

        Object sService
            = ContactSourceConfigActivator.bundleContext
                .getService(serviceRef);

        // we don't care if the source service is not a configuration form
        if (!(sService instanceof ConfigurationForm))
            return;

        ConfigurationForm configForm = (ConfigurationForm) sService;

        if (!configForm.isAdvanced())
            return;

        Object formComponent;
        switch (event.getType())
        {
        case ServiceEvent.REGISTERED:
            formComponent = configForm.getForm();
            if (formComponent instanceof Component)
                addConfigForm(configForm);
            break;

        case ServiceEvent.UNREGISTERING:
            formComponent = configForm.getForm();
            if (formComponent instanceof Component)
                remove((Component) formComponent);
            break;
        }
    }

    /**
     * Adds the given form to this configuration panel.
     *
     * @param form the <tt>ConfigurationForm</tt> to add
     */
    private void addConfigForm(ConfigurationForm form)
    {
        int cIndex = form.getIndex();

        if (cIndex >= contactSourceComboBox.getItemCount() || cIndex == -1)
            contactSourceComboBox.addItem(form);
        else
            contactSourceComboBox.insertItemAt(form, cIndex);
    }

    /**
     * The contact source combo box custom renderer.
     */
    private class ContactSourceRenderer extends DefaultListCellRenderer
    {
        /**
         * Serial version UID.
         */
        private static final long serialVersionUID = 0L;

        @Override
        public Component getListCellRendererComponent(
            JList list, Object value, int index,
                boolean isSelected, boolean hasFocus)
        {
            JLabel renderer
                = (JLabel) super.getListCellRendererComponent(
                    list, value, index, isSelected, hasFocus);

            if (value != null)
            {
                ConfigurationForm form = (ConfigurationForm) value;

                renderer.setText(form.getTitle());
            }

            return renderer;
        }
    }
}
