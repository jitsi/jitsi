/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 * 
 * Distributable under LGPL license. See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main.configforms;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.customcontrols.*;
import net.java.sip.communicator.impl.gui.i18n.*;
import net.java.sip.communicator.impl.gui.main.*;
import net.java.sip.communicator.impl.gui.utils.Constants;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.util.*;

import org.osgi.framework.*;

/**
 * The implementation of the <tt>ConfigurationManager</tt> interface.
 * 
 * @author Yana Stamcheva
 */
public class ConfigurationFrame
    extends SIPCommDialog
    implements ExportedWindow, ServiceListener
{
    private Logger logger = Logger.getLogger(ConfigurationFrame.class);

    private ConfigFormList configList;

    private TitlePanel titlePanel = new TitlePanel();

    private JPanel mainPanel = new JPanel(new BorderLayout(5, 5));

    private JPanel centerPanel = new JPanel(new BorderLayout(5, 5));

    private JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

    private I18NString closeString = Messages.getI18NString("close");

    private JButton closeButton = new JButton(closeString.getText());

    private MainFrame mainFrame;

    /**
     * Creates an instance of <tt>ConfigurationManagerImpl</tt>.
     * 
     * @param mainFrame The main application window.
     */
    public ConfigurationFrame(MainFrame mainFrame)
    {
        super(mainFrame);

        this.mainFrame = mainFrame;

        this.configList = new ConfigFormList(this);

        this.setTitle(Messages.getI18NString("configuration").getText());

        this.getContentPane().setLayout(new BorderLayout());

        this.addDefaultForms();

        this.mainPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        this.mainPanel.add(centerPanel, BorderLayout.CENTER);

        this.mainPanel.add(configList, BorderLayout.WEST);

        this.buttonsPanel.add(closeButton);

        this.closeButton.setMnemonic(closeString.getMnemonic());

        this.closeButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                dispose();
            }
        });

        buttonsPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0,
            Constants.BORDER_COLOR));

        this.mainPanel.add(buttonsPanel, BorderLayout.SOUTH);

        this.getContentPane().add(mainPanel);

        GuiActivator.bundleContext.addServiceListener(this);
    }

    /**
     * Some configuration forms constructed from the ui implementation itself
     * are added here in the configuration dialog.
     */
    public void addDefaultForms()
    {
        this.addConfigurationForm(new AccountsConfigurationForm(mainFrame));
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

        this.centerPanel.add(configFormDescriptor.getConfigFormPanel(),
            BorderLayout.CENTER);

        this.centerPanel.revalidate();
        this.centerPanel.repaint();

    }

    /**
     * Implements <code>ApplicationWindow.show</code> method.
     * 
     * @see net.java.sip.communicator.service.gui.ExportedWindow#setVisible(boolean)
     * 
     * @param isVisible specifies whether the frame is to be visible or not.
     */
    public void setVisible(boolean isVisible)
    {
        if (isVisible)
        {
            this.configList.setSelectedIndex(0);

            super.setVisible(true);

            this.closeButton.requestFocus();
        }
        else
            super.setVisible(false);
    }

    /**
     * Implements <code>ApplicationWindow.minimizeWindow</code> method.
     * 
     * @see net.java.sip.communicator.service.gui.ExportedWindow#minimize()
     */
    public void minimize()
    {
    }

    /**
     * Implements <code>ApplicationWindow.maximizeWindow</code> method.
     * 
     * @see net.java.sip.communicator.service.gui.ExportedWindow#maximize()
     */
    public void maximize()
    {
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
        this.closeButton.doClick();
    }

    /**
     * Returns the identifier of this <tt>ExportedWindow</tt>.
     * 
     * @return a reference to the <tt>WindowID</tt> instance representing this
     *         frame.
     */
    public WindowID getIdentifier()
    {
        return ExportedWindow.CONFIGURATION_WINDOW;
    }

    /**
     * Implements the <tt>ExportedWindow.bringToFront</tt> method. Brings this
     * window to front.
     */
    public void bringToFront()
    {
        this.toFront();
    }

    /**
     * The source of the window
     * 
     * @return the source of the window
     */
    public Object getSource()
    {
        return this;
    }

    /**
     * Handles registration of a new configuration form.
     */
    public void serviceChanged(ServiceEvent event)
    {
        Object sService =
            GuiActivator.bundleContext.getService(event.getServiceReference());

        // we don't care if the source service is not a configuration form
        if (!(sService instanceof ConfigurationForm))
        {
            return;
        }

        ConfigurationForm configForm = (ConfigurationForm) sService;

        if (event.getType() == ServiceEvent.REGISTERED)
        {
            logger.info("Handling registration of a new Configuration Form.");

            this.addConfigurationForm(configForm);
        }
        else if (event.getType() == ServiceEvent.UNREGISTERING)
        {
            this.removeConfigurationForm(configForm);
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
        ConfigFormDescriptor descriptor = new ConfigFormDescriptor(configForm);

        if (descriptor != null)
        {
            if (configForm.getIndex() > -1)
                configList.addConfigForm(descriptor, configForm.getIndex());
            else
                configList.addConfigForm(descriptor);
        }
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
