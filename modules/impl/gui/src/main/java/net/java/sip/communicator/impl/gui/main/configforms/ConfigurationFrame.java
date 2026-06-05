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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;

import javax.swing.*;
import javax.swing.border.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.main.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.diagnostics.DiagnosticReportGenerator;
import net.java.sip.communicator.util.diagnostics.DiagnosticReportOptions;
import org.osgi.framework.*;
import net.java.sip.communicator.service.protocol.ProtocolProviderFactory;
import net.java.sip.communicator.service.protocol.AccountID;

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
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ConfigurationFrame.class);

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

        centerPanel.setMinimumSize(new Dimension(1000, 100));
        centerPanel.setMaximumSize(
            new Dimension(1000, Integer.MAX_VALUE));

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
        JComponent topComponent = new TransparentPanel(new BorderLayout());
        topComponent.setBorder(
            new EmptyBorder(BORDER_SIZE / 2, BORDER_SIZE, 0, 0));
        // Add a small panel on the right to host auxiliary actions
        JPanel rightPanel = new TransparentPanel(new FlowLayout(FlowLayout.RIGHT));

        JButton diagBtn = new JButton(GuiActivator.getResources()
            .getI18NString("plugin.diagnostics.GENERATE_DIAGNOSTIC_REPORT"));
        diagBtn.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                // Ask user for options
                JCheckBox includeLogs = new JCheckBox(GuiActivator.getResources().getI18NString("plugin.diagnostics.INCLUDE_LOGS"), true);
                JCheckBox includeConfig = new JCheckBox(GuiActivator.getResources().getI18NString("plugin.diagnostics.INCLUDE_CONFIG"), true);
                JCheckBox includeThreads = new JCheckBox(GuiActivator.getResources().getI18NString("plugin.diagnostics.INCLUDE_THREAD_DUMP"), true);
                JCheckBox includeScreen = new JCheckBox(GuiActivator.getResources().getI18NString("plugin.diagnostics.INCLUDE_SCREENSHOT"), false);
                JCheckBox redact = new JCheckBox(GuiActivator.getResources().getI18NString("plugin.diagnostics.REDACT_SENSITIVE"), true);

                JPanel panel = new JPanel(new GridLayout(0, 1));
                panel.add(includeLogs);
                panel.add(includeConfig);
                panel.add(includeThreads);
                panel.add(includeScreen);
                panel.add(redact);

                int res = JOptionPane.showConfirmDialog(ConfigurationFrame.this,
                    panel,
                    GuiActivator.getResources().getI18NString("plugin.diagnostics.DIAGNOSTIC_OPTIONS_TITLE"),
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.PLAIN_MESSAGE);

                if (res != JOptionPane.OK_OPTION)
                    return;

                // Ask for destination
                JFileChooser chooser = new JFileChooser();
                chooser.setDialogTitle(GuiActivator.getResources().getI18NString("plugin.diagnostics.SELECT_DESTINATION"));
                chooser.setDialogType(JFileChooser.SAVE_DIALOG);
                chooser.setSelectedFile(new java.io.File("jitsi-diagnostic-" +
                    new java.text.SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date()) + ".zip"));

                int fc = chooser.showSaveDialog(ConfigurationFrame.this);
                if (fc != JFileChooser.APPROVE_OPTION)
                    return;

                final java.io.File dest = chooser.getSelectedFile();

                // Prepare options
                final DiagnosticReportOptions options = new DiagnosticReportOptions();
                options.setIncludeLogs(includeLogs.isSelected());
                options.setIncludeConfig(includeConfig.isSelected());
                options.setIncludeThreadDump(includeThreads.isSelected());
                options.setIncludeScreenshot(includeScreen.isSelected());
                options.setRedactSensitive(redact.isSelected());

                // Collect a user-friendly environment summary
                StringBuilder bundlesInfo = new StringBuilder();
                try
                {
                    Bundle[] bundles = GuiActivator.bundleContext.getBundles();
                    if (bundles == null || bundles.length == 0)
                    {
                        bundlesInfo.append("No se detectaron módulos (bundles) cargados.\n");
                    }
                    else
                    {
                        int total = bundles.length;
                        int active = 0;
                        for (Bundle b : bundles)
                        {
                            if (b.getState() == Bundle.ACTIVE) active++;
                        }
                        bundlesInfo.append("Módulos instalados: ").append(total).append(" (activos: ").append(active).append(")\n");
                        bundlesInfo.append("Lista (hasta 20):\n");
                        int shown = 0;
                        for (Bundle b : bundles)
                        {
                            bundlesInfo.append(" - ");
                            String name = b.getSymbolicName();
                            if (name == null || name.length() == 0)
                                name = b.getLocation();
                            bundlesInfo.append(name == null ? "<sin-nombre>" : name);
                            bundlesInfo.append(" (estado=").append(b.getState()).append(")\n");
                            if (++shown >= 20) break;
                        }
                    }

                    // Account summary: number of configured accounts per protocol
                    try {
                        java.util.Map<Object, ProtocolProviderFactory> pf = GuiActivator.getProtocolProviderFactories();
                        int accountCount = 0;
                        Map<String, Integer> perProtocol = new HashMap<>();
                        if (pf != null)
                        {
                            for (ProtocolProviderFactory factory : pf.values())
                            {
                                try
                                {
                                    java.util.List<AccountID> regs = factory.getRegisteredAccounts();
                                    int cnt = (regs == null) ? 0 : regs.size();
                                    accountCount += cnt;
                                    String proto = (String)factory.getClass().getSimpleName();
                                    perProtocol.put(proto, perProtocol.getOrDefault(proto, 0) + cnt);
                                }
                                catch (Throwable ignore) { /* ignore per-factory errors */ }
                            }
                        }
                        bundlesInfo.append("Cuentas configuradas: ").append(accountCount).append("\n");
                        if (!perProtocol.isEmpty())
                        {
                            bundlesInfo.append("Por proveedor/protocolo:\n");
                            for (Map.Entry<String,Integer> entry : perProtocol.entrySet())
                                bundlesInfo.append("  - ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
                        }
                    } catch (Throwable ignore) {}
                }
                catch (Throwable t)
                {
                    bundlesInfo.append("<bundles-info-unavailable>\n");
                }

                // Run generation in background
                JDialog progressDlg = new JDialog(ConfigurationFrame.this,
                    GuiActivator.getResources().getI18NString("plugin.diagnostics.GENERATING"), true);
                JProgressBar bar = new JProgressBar();
                bar.setIndeterminate(true);
                progressDlg.getContentPane().add(bar);
                progressDlg.setSize(300, 80);
                progressDlg.setLocationRelativeTo(ConfigurationFrame.this);

                javax.swing.SwingWorker<Void, Void> worker = new javax.swing.SwingWorker<Void, Void>() {
                    @Override
                    protected Void doInBackground() throws Exception {
                            DiagnosticReportGenerator gen = new DiagnosticReportGenerator();
                            gen.addTextEntry("report/environment.txt", bundlesInfo.toString());

                        // capture only the Jitsi main window if available
                        try
                        {
                            java.awt.Window mainWindow = null;
                            try {
                                Object ui = net.java.sip.communicator.impl.gui.GuiActivator.getUIService();
                                if (ui != null) {
                                    // UIServiceImpl exposes main frame via getMainFrame
                                    try {
                                        java.lang.reflect.Method m = ui.getClass().getMethod("getMainFrame");
                                        Object mf = m.invoke(ui);
                                        if (mf instanceof java.awt.Window)
                                            mainWindow = (java.awt.Window) mf;
                                    } catch (NoSuchMethodException ignore) {
                                        // ignore if method not present
                                    }
                                }
                            } catch (Throwable ignore) {
                                // ignore
                            }

                            if (mainWindow != null && mainWindow.isVisible())
                            {
                                try {
                                    // Try to bring application window to front briefly so the capture contains it
                                    if (mainWindow instanceof java.awt.Frame) {
                                        java.awt.Frame f = (java.awt.Frame) mainWindow;
                                        f.toFront();
                                        f.requestFocus();
                                    } else {
                                        mainWindow.toFront();
                                        mainWindow.requestFocus();
                                    }
                                    // small pause to let the OS bring the window forward
                                    try { Thread.sleep(200); } catch (InterruptedException ie) { /* ignore */ }
                                } catch (Throwable t) {
                                    // ignore focus/bring-to-front failures
                                }

                                java.awt.Rectangle r = mainWindow.getBounds();
                                java.awt.Robot robot = new java.awt.Robot();
                                java.awt.image.BufferedImage img = robot.createScreenCapture(r);
                                java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
                                javax.imageio.ImageIO.write(img, "png", baos);
                                gen.addBinaryEntry("screenshot/jitsi-window.png", baos.toByteArray());
                            }
                            else
                            {
                                // fallback: full screen capture
                                try {
                                    java.awt.Robot r = new java.awt.Robot();
                                    java.awt.Rectangle screen = new java.awt.Rectangle(java.awt.Toolkit.getDefaultToolkit().getScreenSize());
                                    java.awt.image.BufferedImage img = r.createScreenCapture(screen);
                                    java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
                                    javax.imageio.ImageIO.write(img, "png", baos);
                                    gen.addBinaryEntry("screenshot/jitsi-window.png", baos.toByteArray());
                                } catch (Throwable ignore) {
                                    // ignore screenshot failures
                                }
                            }
                        }
                        catch (Exception ex)
                        {
                            // proceed without screenshot
                        }

                        gen.generate(dest, options);
                        return null;
                    }

                    @Override
                    protected void done()
                    {
                        progressDlg.dispose();
                        try
                        {
                            get();
                            JOptionPane.showMessageDialog(ConfigurationFrame.this,
                                GuiActivator.getResources().getI18NString("plugin.diagnostics.SUCCESS") + "\n" + dest.getAbsolutePath());
                        }
                        catch (Exception ex)
                        {
                            JOptionPane.showMessageDialog(ConfigurationFrame.this,
                                GuiActivator.getResources().getI18NString("plugin.diagnostics.FAILED") + "\n" + ex.getMessage(),
                                GuiActivator.getResources().getI18NString("plugin.diagnostics.ERROR"),
                                JOptionPane.ERROR_MESSAGE);
                        }
                    }
                };

                worker.execute();
                progressDlg.setVisible(true);
            }
        });

        rightPanel.add(diagBtn);
        topComponent.add(rightPanel, BorderLayout.EAST);

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
            new Dimension(
                configFormPanel.getPreferredSize().width + 2*BORDER_SIZE,
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
