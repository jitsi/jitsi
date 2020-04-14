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

import java.awt.*;
import java.util.*;

import javax.swing.*;
import javax.swing.border.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.main.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.Logger;

import org.jitsi.util.*;
import org.osgi.framework.*;

import com.explodingpixels.macwidgets.*;

/**
 * The <tt>ConfigurationFrame</tt> is the dialog opened when the "Options" menu
 * is selected. It contains different basic configuration forms, like General,
 * Accounts, Notifications, etc. and also allows plugin configuration forms to
 * be added.
 *
 * @author Yana Stamcheva
 */
public class ConfigurationFrame
    extends SIPCommDialog
    implements  ConfigurationContainer,
                ServiceListener
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * The <tt>Logger</tt> used by the <tt>ConfigurationFrame</tt> class and its
     * instances for logging output.
     */
    private static final Logger logger
        = Logger.getLogger(ConfigurationFrame.class);

    private static final int BORDER_SIZE = 20;

    private final ConfigFormList configList;

    private final JPanel centerPanel
        = new TransparentPanel(new BorderLayout(5, 5));

    /**
     * Indicates if the account config form should be shown.
     */
    public static final String SHOW_ACCOUNT_CONFIG_PROPERTY
        = "net.java.sip.communicator.impl.gui.main."
            + "configforms.SHOW_ACCOUNT_CONFIG";

    /**
     * Indicates if the configuration window should be shown.
     */
    public static final String SHOW_OPTIONS_WINDOW_PROPERTY
        = "net.java.sip.communicator.impl.gui.main."
            + "configforms.SHOW_OPTIONS_WINDOW";

    /**
     * Initializes a new <tt>ConfigurationFrame</tt> instance.
     *
     * @param mainFrame The main application window.
     */
    public ConfigurationFrame(MainFrame mainFrame)
    {
        super(mainFrame, false);

        this.configList = new ConfigFormList(this);

        JScrollPane configScrollList = new JScrollPane();

        configScrollList.setHorizontalScrollBarPolicy(
            JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        configScrollList.setBorder(null);
        configScrollList.setOpaque(false);
        configScrollList.getViewport().setOpaque(false);
        configScrollList.getViewport().add(configList);

        this.setTitle(GuiActivator.getResources()
                .getI18NString("service.gui.SETTINGS"));

        this.getContentPane().setLayout(new BorderLayout());

        this.addDefaultForms();

        TransparentPanel mainPanel
            = new TransparentPanel(new BorderLayout());

        centerPanel.setMinimumSize(new Dimension(600, 100));
        centerPanel.setMaximumSize(
            new Dimension(600, Integer.MAX_VALUE));

        mainPanel.add(centerPanel, BorderLayout.CENTER);

        JComponent topComponent = createTopComponent();
        topComponent.add(configScrollList);
        mainPanel.add(topComponent, BorderLayout.NORTH);

        centerPanel.setBorder(BorderFactory.createEmptyBorder(  BORDER_SIZE,
                                                                BORDER_SIZE,
                                                                BORDER_SIZE,
                                                                BORDER_SIZE));

        this.getContentPane().add(mainPanel);

        GuiActivator.bundleContext.addServiceListener(this);

        // General configuration forms only.
        Collection<ServiceReference<ConfigurationForm>> cfgFormRefs;
        String osgiFilter
            = "(" + ConfigurationForm.FORM_TYPE + "="
                + ConfigurationForm.GENERAL_TYPE + ")";

        try
        {
            cfgFormRefs
                = GuiActivator.bundleContext.getServiceReferences(
                        ConfigurationForm.class,
                        osgiFilter);
        }
        catch (InvalidSyntaxException ex)
        {
            cfgFormRefs = null;
        }

        if ((cfgFormRefs != null) && !cfgFormRefs.isEmpty())
        {
            for (ServiceReference<ConfigurationForm> cfgFormRef : cfgFormRefs)
            {
                ConfigurationForm form
                    = GuiActivator.bundleContext.getService(cfgFormRef);

                addConfigurationForm(form);
            }
        }
    }

    /**
     * Creates the toolbar panel for this chat window, depending on the current
     * operating system.
     *
     * @return the created toolbar
     */
    private JComponent createTopComponent()
    {
        JComponent topComponent = null;

        if (OSUtils.IS_MAC)
        {
            UnifiedToolBar macToolbarPanel = new UnifiedToolBar();

            MacUtils.makeWindowLeopardStyle(getRootPane());

            macToolbarPanel.getComponent().setLayout(new BorderLayout());
            macToolbarPanel.disableBackgroundPainter();
            macToolbarPanel.installWindowDraggerOnWindow(this);
            centerPanel.setOpaque(true);
            centerPanel.setBackground(
                new Color(GuiActivator.getResources()
                    .getColor("service.gui.MAC_PANEL_BACKGROUND")));

            topComponent = macToolbarPanel.getComponent();
        }
        else
        {
            topComponent = new TransparentPanel(new BorderLayout());
            topComponent.setBorder(
                new EmptyBorder(BORDER_SIZE / 2, BORDER_SIZE, 0, 0));
        }

        return topComponent;
    }

    /**
     * Some configuration forms constructed from the ui implementation itself
     * are added here in the configuration dialog.
     */
    public void addDefaultForms()
    {
        if (ConfigurationUtils.isShowAccountConfig())
            addConfigurationForm(
                new LazyConfigurationForm(
                    "net.java.sip.communicator.impl.gui.main."
                    + "account.AccountsConfigurationPanel",
                    getClass().getClassLoader(),
                    "service.gui.icons.ACCOUNT_ICON",
                    "service.gui.ACCOUNTS",
                    0));
    }

    /**
     * Shows on the right the configuration form given by the given
     * <tt>ConfigFormDescriptor</tt>.
     *
     * @param configFormDescriptor the descriptor of the for we will be showing.
     */
    public void showFormContent(ConfigFormDescriptor configFormDescriptor)
    {
        this.centerPanel.removeAll();

        final JComponent configFormPanel
            = (JComponent) configFormDescriptor.getConfigFormPanel();

        configFormPanel.setOpaque(false);

        centerPanel.add(configFormPanel, BorderLayout.CENTER);

        centerPanel.revalidate();

        // Set the height of the center panel to be equal to the height of the
        // currently contained panel + all borders.
        centerPanel.setPreferredSize(
            new Dimension(550,
                configFormPanel.getPreferredSize().height + 2*BORDER_SIZE));

        pack();
    }

    /**
     * Implements <code>ApplicationWindow.show</code> method.
     *
     * @param isVisible specifies whether the frame is to be visible or not.
     */
    @Override
    public void setVisible(final boolean isVisible)
    {
        if(!SwingUtilities.isEventDispatchThread())
        {
            SwingUtilities.invokeLater(new Runnable()
            {
                public void run()
                {
                    setVisible(isVisible);
                }
            });
            return;
        }

        if (isVisible && configList.getSelectedIndex() < 0)
        {
            this.configList.setSelectedIndex(0);
        }
        super.setVisible(isVisible);
        super.toFront();
    }

    /**
     * Implements <tt>SIPCommFrame.close()</tt> method. Performs a click on
     * the close button.
     *
     * @param isEscaped specifies whether the close was triggered by pressing
     *            the escape key.
     */
    @Override
    protected void close(boolean isEscaped)
    {
        this.setVisible(false);
    }

    /**
     * Handles registration of a new configuration form.
     * @param event the <tt>ServiceEvent</tt> that notified us
     */
    @Override
    public void serviceChanged(ServiceEvent event)
    {
        if(!GuiActivator.isStarted)
            return;

        ServiceReference<?> serRef = event.getServiceReference();

        Object property = serRef.getProperty(ConfigurationForm.FORM_TYPE);

        if (property != ConfigurationForm.GENERAL_TYPE)
            return;

        Object service = GuiActivator.bundleContext.getService(serRef);

        // we don't care if the source service is not a configuration form
        if (!(service instanceof ConfigurationForm))
            return;

        ConfigurationForm cfgForm = (ConfigurationForm) service;

        if (cfgForm.isAdvanced())
            return;

        switch (event.getType())
        {
        case ServiceEvent.REGISTERED:
            if (logger.isInfoEnabled())
            {
                logger.info(
                        "Handling registration of a new Configuration Form.");
            }
            addConfigurationForm(cfgForm);
            break;

        case ServiceEvent.UNREGISTERING:
            removeConfigurationForm(cfgForm);
            break;
        }
    }

    /**
     * Implements the <code>ConfigurationManager.addConfigurationForm</code>
     * method. Checks if the form contained in the <tt>ConfigurationForm</tt>
     * is an instance of java.awt.Component and if so adds the form in this
     * dialog, otherwise throws a ClassCastException.
     *
     * @param configForm the form we are adding
     *
     * @see ConfigFormList#addConfigForm(ConfigurationForm)
     */
    private void addConfigurationForm(final ConfigurationForm configForm)
    {
        if(!SwingUtilities.isEventDispatchThread())
        {
            SwingUtilities.invokeLater(new Runnable()
            {
                public void run()
                {
                    addConfigurationForm(configForm);
                }
            });
            return;
        }

        configList.addConfigForm(configForm);
    }

    /**
     * Implements <code>ConfigurationManager.removeConfigurationForm</code>
     * method. Removes the given <tt>ConfigurationForm</tt> from this dialog.
     *
     * @param configForm the form we are removing.
     *
     * @see ConfigFormList#removeConfigForm(ConfigurationForm)
     */
    private void removeConfigurationForm(final ConfigurationForm configForm)
    {
        if(!SwingUtilities.isEventDispatchThread())
        {
            SwingUtilities.invokeLater(new Runnable()
            {
                public void run()
                {
                    removeConfigurationForm(configForm);
                }
            });
            return;
        }

        configList.removeConfigForm(configForm);
    }

    public void setSelected(final ConfigurationForm configForm)
    {
        if(!SwingUtilities.isEventDispatchThread())
        {
            SwingUtilities.invokeLater(new Runnable()
            {
                public void run()
                {
                    setSelected(configForm);
                }
            });
            return;
        }

        configList.setSelected(configForm);
    }

    /**
     * Validates the currently selected configuration form. This method is meant
     * to be used by configuration forms the re-validate when a new component
     * has been added or size has changed.
     */
    public void validateCurrentForm()
    {
        centerPanel.revalidate();

        centerPanel.setPreferredSize(null);

        validate();

        // Set the height of the center panel to be equal to the height of the
        // currently contained panel + all borders.
        centerPanel.setPreferredSize(
            new Dimension(550, centerPanel.getHeight()));

        pack();
    }
}
