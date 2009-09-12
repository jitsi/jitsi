/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.configforms;

import java.awt.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.customcontrols.*;
import net.java.sip.communicator.impl.gui.main.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.swing.*;

import org.osgi.framework.*;

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
    implements ServiceListener
{
    private final Logger logger = Logger.getLogger(ConfigurationFrame.class);

    private final ConfigFormList configList;

    private final TitlePanel titlePanel = new TitlePanel();

    private final JPanel centerPanel =
        new TransparentPanel(new BorderLayout(5, 5));

    /**
     * Creates an instance of <tt>ConfigurationManagerImpl</tt>.
     *
     * @param mainFrame The main application window.
     */
    public ConfigurationFrame(MainFrame mainFrame)
    {
        super(mainFrame);

        this.configList = new ConfigFormList(this);

        JScrollPane configScrollList = new JScrollPane();

        configScrollList.setHorizontalScrollBarPolicy(
            JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        configScrollList.setBorder(BorderFactory.createEmptyBorder());
        configScrollList.setOpaque(false);
        configScrollList.getViewport().setOpaque(false);
        configScrollList.getViewport().add(configList);

        this.setTitle(GuiActivator.getResources()
                .getI18NString("service.gui.SETTINGS"));

        this.getContentPane().setLayout(new BorderLayout());

        this.addDefaultForms();

        TransparentPanel mainPanel
            = new TransparentPanel(new BorderLayout(5, 5));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        mainPanel.add(centerPanel, BorderLayout.CENTER);
        mainPanel.add(configScrollList, BorderLayout.WEST);

        this.getContentPane().add(mainPanel);

        GuiActivator.bundleContext.addServiceListener(this);

        ServiceReference[] confFormsRefs = null;
        try
        {
            confFormsRefs = GuiActivator.bundleContext
                .getServiceReferences(
                    ConfigurationForm.class.getName(),
                    null);
        }
        catch (InvalidSyntaxException ex)
        {}

        if(confFormsRefs != null)
        {
            for (int i = 0; i < confFormsRefs.length; i++)
            {
                ConfigurationForm form
                    = (ConfigurationForm) GuiActivator.bundleContext
                        .getService(confFormsRefs[i]);

                this.addConfigurationForm(form);
            }
        }
    }

    /**
     * Some configuration forms constructed from the ui implementation itself
     * are added here in the configuration dialog.
     */
    public void addDefaultForms()
    {
        addConfigurationForm(
            new LazyConfigurationForm(
                "net.java.sip.communicator.impl.gui.main.account.AccountsConfigurationPanel",
                getClass().getClassLoader(),
                "service.gui.icons.ACCOUNT_ICON",
                "service.gui.ACCOUNTS",
                10));
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

        this.titlePanel.setTitleText(configFormDescriptor.getConfigFormTitle());

        this.centerPanel.add(titlePanel, BorderLayout.NORTH);

        JComponent configFormPanel
            = (JComponent) configFormDescriptor.getConfigFormPanel();

        configFormPanel.setOpaque(false);

        this.centerPanel.add(configFormPanel, BorderLayout.CENTER);

        this.centerPanel.revalidate();
        this.centerPanel.repaint();

    }

    /**
     * Implements <code>ApplicationWindow.show</code> method.
     *
     * @param isVisible specifies whether the frame is to be visible or not.
     */
    public void setVisible(boolean isVisible)
    {
        if (isVisible)
        {
            this.configList.setSelectedIndex(0);
        }
        super.setVisible(isVisible);
    }

    /**
     * Implements <tt>SIPCommFrame.close()</tt> method. Performs a click on
     * the close button.
     *
     * @param isEscaped specifies whether the close was triggered by pressing
     *            the escape key.
     */
    protected void close(boolean isEscaped)
    {
    }

    /**
     * Handles registration of a new configuration form.
     */
    public void serviceChanged(ServiceEvent event)
    {
        if(!GuiActivator.isStarted)
            return;
        Object sService =
            GuiActivator.bundleContext.getService(event.getServiceReference());

        // we don't care if the source service is not a configuration form
        if (!(sService instanceof ConfigurationForm))
        {
            return;
        }

        ConfigurationForm configForm = (ConfigurationForm) sService;

        switch (event.getType())
        {
        case ServiceEvent.REGISTERED:
            logger.info("Handling registration of a new Configuration Form.");

            this.addConfigurationForm(configForm);
            break;

        case ServiceEvent.UNREGISTERING:
            this.removeConfigurationForm(configForm);
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
     * @see ConfigurationWindow#addConfigurationForm(ConfigurationForm)
     */
    private void addConfigurationForm(ConfigurationForm configForm)
    {
        configList.addConfigForm(configForm);
    }

    /**
     * Implements <code>ConfigurationManager.removeConfigurationForm</code>
     * method. Removes the given <tt>ConfigurationForm</tt> from this dialog.
     *
     * @param configForm the form we are removing.
     *
     * @see ConfigurationWindow#removeConfigurationForm(ConfigurationForm)
     */
    private void removeConfigurationForm(ConfigurationForm configForm)
    {
        this.configList.removeConfigForm(configForm);
    }
}
