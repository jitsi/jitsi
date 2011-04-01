/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.updatechecker;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.*;

import javax.swing.*;
import javax.swing.text.*;

import net.java.sip.communicator.service.browserlauncher.*;
import net.java.sip.communicator.service.configuration.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.gui.Container; // disambiguation
import net.java.sip.communicator.service.httputil.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.resources.*;
import net.java.sip.communicator.service.shutdown.*;
import net.java.sip.communicator.service.version.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.swing.*;

import org.osgi.framework.*;

/**
 * Implements <tt>BundleActivator</tt> for the updatechecker plug-in.
 *
 * @author Damian Minkov
 * @author Lyubomir Marinov
 */
public class UpdateCheckActivator
    implements BundleActivator
{
    /**
     * The <tt>Logger</tt> used by the <tt>UpdateCheckActivator</tt> class and
     * its instances for logging output.
     */
    private static final Logger logger
        = Logger.getLogger(UpdateCheckActivator.class);

    /**
     * The bundle context.
     */
    private static BundleContext bundleContext = null;

    /**
     * Reference to the <tt>BrowserLauncherService</tt>.
     */
    private static BrowserLauncherService browserLauncher;

    /**
     * Reference to the <tt>ResourceManagementService</tt>.
     */
    private static ResourceManagementService resources;

    /**
     * Reference to the <tt>ConfigurationService</tt>.
     */
    private static ConfigurationService configuration;

    /**
     * Reference to the <tt>UIService</tt>.
     */
    private static UIService uiService = null;

    /**
     * The download link of the update.
     */
    private String downloadLink = null;

    /**
     * The last version of the software.
     */
    private String lastVersion = null;

    /**
     * The ChangeLog link.
     */
    private String changesLink = null;

    /**
     * The user credentials.
     */
    private static UserCredentials userCredentials = null;

    /**
     * The error message is any.
     */
    private static String errorMessage = null;

    /**
     * The host we are querying for updates.
     */
    private static String host = null;

    /**
     * Whether user has canceled authentication process.
     */
    private static boolean isAuthenticationCanceled = false;

    /**
     * Property name of the username used if HTTP authentication is required.
     */
    private static final String UPDATE_USERNAME_CONFIG =
        "net.java.sip.communicator.plugin.updatechecker.UPDATE_SITE_USERNAME";

    /**
     * Property name of the password used if HTTP authentication is required.
     */
    private static final String UPDATE_PASSWORD_CONFIG =
        "net.java.sip.communicator.plugin.updatechecker.UPDATE_SITE_PASSWORD";

    /**
     * Property indicating whether update check is enabled.
     */
    private static final String UPDATECHECKER_ENABLED =
        "net.java.sip.communicator.plugin.updatechecker.ENABLED";

    /**
     * Property name for the update link in the configuration file.
     */
    private static final String PROP_UPDATE_LINK =
        "net.java.sip.communicator.UPDATE_LINK";

    /**
     * The <tt>JDialog</tt>, if any, which is associated with the currently
     * executing "Check for Updates". While the "Check for Updates"
     * functionality cannot be entered, clicking the "Check for Updates" menu
     * item will bring it to the front.
     */
    private JDialog checkForUpdatesDialog;

    /**
     * The "Check for Updates" <tt>PluginComponent</tt> registered by this
     * <tt>UpdateCheckActivator</tt>.
     */
    private CheckForUpdatesMenuItemComponent checkForUpdatesMenuItemComponent;

    /**
     * The indicator/counter which determines how many methods are currently
     * executing the "Check for Updates" functionality so that it is known
     * whether it can be entered.
     */
    private int inCheckForUpdates = 0;

    /**
     * Starts this bundle
     *
     * @param bundleContext <tt>BundleContext</tt> provided by OSGi framework
     * @throws Exception if something goes wrong during start
     */
    public void start(BundleContext bundleContext) throws Exception
    {
        if (logger.isDebugEnabled())
            logger.debug("Update checker [STARTED]");

        UpdateCheckActivator.bundleContext = bundleContext;

        if (OSUtils.IS_WINDOWS)
        {
            // Register the "Check for Updates" menu item.
            checkForUpdatesMenuItemComponent
                = new CheckForUpdatesMenuItemComponent(
                        Container.CONTAINER_HELP_MENU);

            Hashtable<String, String> toolsMenuFilter
                = new Hashtable<String, String>();
            toolsMenuFilter.put(
                    Container.CONTAINER_ID,
                    Container.CONTAINER_HELP_MENU.getID());

            bundleContext.registerService(
                    PluginComponent.class.getName(),
                    checkForUpdatesMenuItemComponent,
                    toolsMenuFilter);

            // Check for software update upon startup if enabled.
            if(getConfiguration().getBoolean(UPDATECHECKER_ENABLED, true))
                checkForUpdates(false);
        }

        if (logger.isDebugEnabled())
            logger.debug("Update checker [REGISTERED]");
    }

    /**
     * Stop the bundle. Nothing to stop for now.
     * @param bundleContext <tt>BundleContext</tt> provided by OSGi framework
     * @throws Exception if something goes wrong during stop
     */
    public void stop(BundleContext bundleContext)
        throws Exception
    {
        if (logger.isDebugEnabled())
            logger.debug("Update checker [STOPPED]");
    }

    /**
     * Returns the <tt>BrowserLauncherService</tt> obtained from the bundle
     * context.
     * @return the <tt>BrowserLauncherService</tt> obtained from the bundle
     * context
     */
    private static BrowserLauncherService getBrowserLauncher()
    {
        if (browserLauncher == null)
        {
            browserLauncher
                = ServiceUtils.getService(
                        bundleContext,
                        BrowserLauncherService.class);
        }
        return browserLauncher;
    }

    /**
     * Returns the <tt>ConfigurationService</tt> obtained from the bundle
     * context.
     *
     * @return the <tt>ConfigurationService</tt> obtained from the bundle
     * context
     */
    private static ConfigurationService getConfiguration()
    {
        if (configuration == null)
        {
            configuration
                = ServiceUtils.getService(
                        bundleContext,
                        ConfigurationService.class);
        }
        return configuration;
    }

    /**
     * Gets a reference to a <code>ShutdownService</code> implementation
     * currently registered in the bundle context of the active
     * <code>UpdateCheckActivator</code> instance.
     * <p>
     * The returned reference to <code>ShutdownService</code> is not being
     * cached.
     * </p>
     *
     * @return reference to a <code>ShutdownService</code> implementation
     *         currently registered in the bundle context of the active
     *         <code>UpdateCheckActivator</code> instance
     */
    private static ShutdownService getShutdownService()
    {
        return ServiceUtils.getService(bundleContext, ShutdownService.class);
    }

    /**
     * Returns a reference to the UIService implementation currently registered
     * in the bundle context or null if no such implementation was found.
     *
     * @return a reference to a UIService implementation currently registered
     * in the bundle context or null if no such implementation was found.
     */
    private static UIService getUIService()
    {
        if(uiService == null)
            uiService = ServiceUtils.getService(bundleContext, UIService.class);
        return uiService;
    }

    /**
     * Returns resource service.
     * @return the resource service.
     */
    private static ResourceManagementService getResources()
    {
        if (resources == null)
        {
            resources
                = ServiceUtils.getService(
                        bundleContext,
                        ResourceManagementService.class);
        }
        return resources;
    }

    /**
     * Checks the first link as files on the web are sorted by date.
     *
     * @return <tt>true</tt> if we are currently running the newest version;
     * otherwise, <tt>false</tt>
     */
    private boolean isNewestVersion()
    {
        try
        {
            ServiceReference serviceReference = bundleContext
                .getServiceReference( net.java.sip.communicator.service.version.
                    VersionService.class.getName());

            VersionService verService = (VersionService) bundleContext
                    .getService(serviceReference);

            net.java.sip.communicator.service.version.Version
                ver = verService.getCurrentVersion();

            String configString
                = getConfiguration().getString(PROP_UPDATE_LINK);
            if(configString == null)
                configString = Resources.getConfigString("update_link");
            if(configString == null)
            {
                if (logger.isDebugEnabled())
                    logger.debug(
                            "Updates are disabled. Faking latest version.");
                return true;
            }

            Properties props = new Properties();
            HttpUtils.HTTPResponseResult res
                = HttpUtils.openURLConnection(configString);

            if(res == null)
                return true;

            InputStream in = res.getContent();

            props.load(in);

            in.close();

            lastVersion = props.getProperty("last_version");
            downloadLink = props.getProperty("download_link");

            changesLink =
                    configString.substring(0, configString.lastIndexOf("/") + 1)
                    + props.getProperty("changes_html");

            return lastVersion.compareTo(ver.toString()) <= 0;
        }
        catch (Exception e)
        {
            logger.warn("Cannot get and compare versions!");
            if (logger.isDebugEnabled())
                logger.debug("Error was: ", e);
            // if we get an exception this mean we were unable to compare
            // versions will return that current is newest to prevent opening
            // info dialog about new version
            return true;
        }
    }

    /**
     * Shows dialog informing about new version with button Download which
     * triggers browser launching
     */
    private void showGenericNewVersionAvailableDialog()
    {
        /*
         * Before showing the dialog, we'll enterCheckForUpdates() in order to
         * notify that it is not safe to enter "Check for Updates" again. If we
         * don't manage to show the dialog, we'll have to exitCheckForUpdates().
         * If we manage though, we'll have to exitCheckForUpdates() but only
         * once depending on its modality.
         */
        final boolean[] exitCheckForUpdates = new boolean[] { false };
        final JDialog dialog = new SIPCommDialog()
        {
            private static final long serialVersionUID = 0L;

            protected void close(boolean escaped)
            {
                synchronized (exitCheckForUpdates)
                {
                    if (exitCheckForUpdates[0])
                        exitCheckForUpdates(this);
                }
            }
        };
        dialog.setTitle(
            getResources().getI18NString(
                    "plugin.updatechecker.DIALOG_TITLE"));

        JEditorPane contentMessage = new JEditorPane();
        contentMessage.setContentType("text/html");
        contentMessage.setOpaque(false);
        contentMessage.setEditable(false);

        String dialogMsg =
            getResources().getI18NString(
                    "plugin.updatechecker.DIALOG_MESSAGE",
            new String[]{getResources()
                .getSettingsString("service.gui.APPLICATION_NAME")});

        if(lastVersion != null)
            dialogMsg +=
                getResources().getI18NString(
                "plugin.updatechecker.DIALOG_MESSAGE_2",
                new String[]{
                    getResources().getSettingsString(
                        "service.gui.APPLICATION_NAME"),
                    lastVersion});

        contentMessage.setText(dialogMsg);

        JPanel contentPane = new TransparentPanel(new BorderLayout(5,5));
        contentPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10,
                10));
        contentPane.add(contentMessage, BorderLayout.CENTER);

        JPanel buttonPanel
            = new TransparentPanel(new FlowLayout(FlowLayout.CENTER, 10,
                    10));
        final JButton closeButton = new JButton(
            getResources().getI18NString(
                    "plugin.updatechecker.BUTTON_CLOSE"));

        closeButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                dialog.dispose();
                if (exitCheckForUpdates[0])
                    exitCheckForUpdates(dialog);
            }
        });

        if(downloadLink != null)
        {
            JButton downloadButton
                = new JButton(
                        getResources().getI18NString(
                                "plugin.updatechecker.BUTTON_DOWNLOAD"));

            downloadButton.addActionListener(new ActionListener()
            {
                public void actionPerformed(ActionEvent e)
                {
                    if(OSUtils.IS_LINUX64)
                        downloadLink
                            = downloadLink.replace("i386", "amd64");

                    getBrowserLauncher().openURL(downloadLink);

                    /*
                     * Do the same as the Close button in order to not duplicate
                     * the code.
                     */
                    closeButton.doClick();
                }
            });

            buttonPanel.add(downloadButton);
        }

        buttonPanel.add(closeButton);

        contentPane.add(buttonPanel, BorderLayout.SOUTH);

        dialog.setContentPane(contentPane);

        dialog.pack();

        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        dialog.setLocation(
            screenSize.width/2 - dialog.getWidth()/2,
            screenSize.height/2 - dialog.getHeight()/2);

        synchronized (exitCheckForUpdates)
        {
            enterCheckForUpdates(dialog);
            exitCheckForUpdates[0] = true;
        }
        try
        {
            dialog.setVisible(true);
        }
        finally
        {
            synchronized (exitCheckForUpdates)
            {
                if (exitCheckForUpdates[0] && dialog.isModal())
                    exitCheckForUpdates(dialog);
            }
        }
    }

    /**
     * Shows dialog informing about new version with button Install
     * which triggers the update process.
     */
    private void showWindowsNewVersionAvailableDialog()
    {
        /*
         * Before showing the dialog, we'll enterCheckForUpdates() in order to
         * notify that it is not safe to enter "Check for Updates" again. If we
         * don't manage to show the dialog, we'll have to exitCheckForUpdates().
         * If we manage though, we'll have to exitCheckForUpdates() but only
         * once depending on its modality.
         */
        final boolean[] exitCheckForUpdates = new boolean[] { false };
        final JDialog dialog = new SIPCommDialog()
        {
            private static final long serialVersionUID = 0L;

            protected void close(boolean escaped)
            {
                synchronized (exitCheckForUpdates)
                {
                    if (exitCheckForUpdates[0])
                        exitCheckForUpdates(this);
                }
            }
        };
        ResourceManagementService resources = getResources();

        dialog.setTitle(
                resources.getI18NString("plugin.updatechecker.DIALOG_TITLE"));

        JEditorPane contentMessage = new JEditorPane();
        contentMessage.setContentType("text/html");
        contentMessage.setOpaque(false);
        contentMessage.setEditable(false);

        /*
         * Use the font of the dialog because contentMessage is just like a
         * label.
         */
        contentMessage.putClientProperty(
                JEditorPane.HONOR_DISPLAY_PROPERTIES,
                Boolean.TRUE);

        String dialogMsg
            = resources.getI18NString(
                    "plugin.updatechecker.DIALOG_MESSAGE",
                    new String[]
                            {
                                resources.getSettingsString(
                                        "service.gui.APPLICATION_NAME")
                            });

        if(lastVersion != null)
        {
            dialogMsg
                += resources.getI18NString(
                        "plugin.updatechecker.DIALOG_MESSAGE_2",
                        new String[]
                                {
                                    resources.getSettingsString(
                                            "service.gui.APPLICATION_NAME"),
                                    lastVersion
                                });
        }

        contentMessage.setText(dialogMsg);

        JPanel contentPane = new SIPCommFrame.MainContentPane();
        contentMessage.setBorder(BorderFactory.createEmptyBorder(10, 0, 20, 0));
        contentPane.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
        contentPane.add(contentMessage, BorderLayout.NORTH);

        JScrollPane scrollChanges = new JScrollPane();
        scrollChanges.setPreferredSize(new Dimension(550, 200));
        JEditorPane changesHtml = new JEditorPane();
        changesHtml.setContentType("text/html");
        changesHtml.setEditable(false);
        changesHtml.setBorder(BorderFactory.createLoweredBevelBorder());
        scrollChanges.setViewportView(changesHtml);
        contentPane.add(scrollChanges, BorderLayout.CENTER);
        try
        {
            Document changesHtmlDocument = changesHtml.getDocument();

            if (changesHtmlDocument instanceof AbstractDocument)
            {
                ((AbstractDocument) changesHtmlDocument)
                    .setAsynchronousLoadPriority(0);
            }
            changesHtml.setPage(new URL(changesLink));
        }
        catch (Exception e)
        {
            logger.error("Cannot set changes Page", e);
        }

        JPanel buttonPanel
            = new TransparentPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        final JButton closeButton
            = new JButton(
                    resources.getI18NString(
                            "plugin.updatechecker.BUTTON_CLOSE"));

        closeButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                dialog.dispose();
                if (exitCheckForUpdates[0])
                    exitCheckForUpdates(dialog);
            }
        });

        if(downloadLink != null)
        {
            JButton installButton
                = new JButton(
                        resources.getI18NString(
                                "plugin.updatechecker.BUTTON_INSTALL"));

            installButton.addActionListener(new ActionListener()
            {
                public void actionPerformed(ActionEvent e)
                {
                    if(OSUtils.IS_WINDOWS64)
                        downloadLink = downloadLink.replace("x86", "x64");

                    enterCheckForUpdates(null);
                    try
                    {
                        /*
                         * Do the same as the Close button in order to not
                         * duplicate the code.
                         */
                        closeButton.doClick();
                    }
                    finally
                    {
                        boolean windowsUpdateThreadHasStarted = false;

                        try
                        {
                            new Thread()
                            {
                                @Override
                                public void run()
                                {
                                    try
                                    {
                                        windowsUpdate();
                                    }
                                    finally
                                    {
                                        exitCheckForUpdates(null);
                                    }
                                }
                            }.start();
                            windowsUpdateThreadHasStarted = true;
                        }
                        finally
                        {
                            if (!windowsUpdateThreadHasStarted)
                                exitCheckForUpdates(null);
                        }
                    }
                }
            });

            buttonPanel.add(installButton);
        }

        buttonPanel.add(closeButton);

        contentPane.add(buttonPanel, BorderLayout.SOUTH);

        dialog.setContentPane(contentPane);

        dialog.pack();

        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize(); 
        dialog.setLocation(
                screenSize.width/2 - dialog.getWidth()/2,
                screenSize.height/2 - dialog.getHeight()/2);

        synchronized (exitCheckForUpdates)
        {
            enterCheckForUpdates(dialog);
            exitCheckForUpdates[0] = true;
        }
        try
        {
            dialog.setVisible(true);
        }
        finally
        {
            synchronized (exitCheckForUpdates)
            {
                if (exitCheckForUpdates[0] && dialog.isModal())
                    exitCheckForUpdates(dialog);
            }
        }
    }

    /**
     * The update process itself.
     * - Downloads the setup in a temporary directory.
     * - Warns that update will shut down.
     * - Triggers the setup in a separate process and shuts down.
     */
    private void windowsUpdate()
    {
        final File[] tempFile = new File[1];
        FileOutputStream tempFileOutputStream = null;
        boolean deleteTempFile = false;

        try
        {
            URL u = new URL(downloadLink);

            tempFileOutputStream = createTempFileOutputStream(u, tempFile);

            HttpUtils.HTTPResponseResult res
                = HttpUtils.openURLConnection(downloadLink);

            if(res == null)
                return;

            InputStream in = res.getContent();

            // Track the progress of the download.
            final ProgressMonitorInputStream input
                = new ProgressMonitorInputStream(
                        null,
                        downloadLink,
                        in);
            // Set the maximum value of the ProgressMonitor
            input.getProgressMonitor().setMaximum((int)res.getContentLength());

            final BufferedOutputStream output
                = new BufferedOutputStream(tempFileOutputStream);

            try
            {
                int read = -1;
                byte[] buff = new byte[1024];

                while((read = input.read(buff)) != -1)
                    output.write(buff, 0, read);
                try
                {
                    input.close();
                }
                catch (IOException ioe)
                {
                    /*
                     * Ignore it because we've already downloaded the
                     * setup and that's what matters most.
                     */ 
                }
                output.close();

                if(getUIService().getPopupDialog()
                            .showConfirmPopupDialog(
                                    getResources().getI18NString(
                                        "plugin.updatechecker.DIALOG_WARN"),
                                    getResources().getI18NString(
                                        "plugin.updatechecker.DIALOG_TITLE"),
                                    PopupDialog.YES_NO_OPTION,
                                    PopupDialog.QUESTION_MESSAGE)
                        != PopupDialog.YES_OPTION)
                    return;

                /*
                 * The setup has been downloaded. Now start it and shut
                 * down.
                 */
                new ProcessBuilder(
                        tempFile[0].getCanonicalPath(),
                        "--wait-parent",
                        "SIP_COMMUNICATOR_AUTOUPDATE_INSTALLDIR=\""
                            + System.getProperty("user.dir")
                            + "\"")
                    .start();

                getShutdownService().beginShutdown();
            }
            catch (Exception e)
            {
                logger.error("Error saving", e);
            }
            finally
            {
                try
                {
                    input.close();
                }
                catch (IOException ioe)
                {
                }
                try
                {
                    output.close();
                }
                catch (IOException ioe)
                {
                }
            }
        }
        catch(FileNotFoundException fnfe)
        {
            deleteTempFile = true;
            getUIService().getPopupDialog().showMessagePopupDialog(
                getResources().getI18NString(
                        "plugin.updatechecker.DIALOG_MISSING_UPDATE"),
                getResources().getI18NString(
                        "plugin.updatechecker.DIALOG_NOUPDATE_TITLE"),
                PopupDialog.INFORMATION_MESSAGE);
        }
        catch (Exception e)
        {
            deleteTempFile = true;
            if (logger.isInfoEnabled())
                logger.info("Error starting update process!", e);;
        }
        finally
        {
            /*
             * If we've failed, delete the temporary file into which the setup
             * was supposed to be or has already been downloaded.
             */
            if (deleteTempFile)
            {
                if (tempFileOutputStream != null)
                {
                    try
                    {
                        tempFileOutputStream.close();
                    }
                    catch (IOException ioe)
                    {
                        // Ignore it because there's nothing else we can do.
                    }
                }
                if (tempFile[0] != null)
                    tempFile[0].delete();
            }
        }
    }

    /**
     * Tries to create a new <tt>FileOutputStream</tt> for a temporary file into
     * which the setup is to be downloaded. Because temporary files generally
     * have random characters in their names and the name of the setup may be
     * shown to the user, first tries to use the name of the URL to be
     * downloaded because it likely is prettier.
     *
     * @param url the <tt>URL</tt> of the file to be downloaded
     * @param tempFile a <tt>File</tt> array of at least one element which is to
     * receive the created <tt>File</tt> instance at index zero (if successful) 
     * @return the newly created <tt>FileOutputStream</tt>
     * @throws IOException if anything goes wrong while creating the new
     * <tt>FileOutputStream</tt>
     */
    private FileOutputStream createTempFileOutputStream(
            URL url,
            File[] tempFile)
        throws IOException
    {
        /*
         * Try to use the name from the URL because it isn't a "randomly"
         * generated one.
         */
        String path = url.getPath();

        File tf = null;
        FileOutputStream tfos = null;

        if ((path != null) && (path.length() != 0))
        {
            int nameBeginIndex =path.lastIndexOf('/');
            String name;

            if (nameBeginIndex > 0)
            {
                name = path.substring(nameBeginIndex + 1);
                nameBeginIndex = name.lastIndexOf('\\');
                if (nameBeginIndex > 0)
                    name = name.substring(nameBeginIndex + 1);
            }
            else
                name = path;

            /*
             * Make sure the extension of the name is EXE so that we're able to
             * execute it later on.
             */
            int nameLength = name.length();

            if (nameLength != 0)
            {
                int baseNameEnd = name.lastIndexOf('.');

                if (baseNameEnd == -1)
                    name += ".exe";
                else if (baseNameEnd == 0)
                {
                    if (!".exe".equalsIgnoreCase(name))
                        name += ".exe";
                }
                else
                    name = name.substring(0, baseNameEnd) + ".exe";

                try
                {
                    String tempDir = System.getProperty("java.io.tmpdir");

                    if ((tempDir != null) && (tempDir.length() != 0))
                    {
                        tf = new File(tempDir, name);
                        tfos = new FileOutputStream(tf);
                    }
                }
                catch (FileNotFoundException fnfe)
                {
                    // Ignore it because we'll try File#createTempFile().
                }
                catch (SecurityException se)
                {
                    // Ignore it because we'll try File#createTempFile().
                }
            }
        }

        // Well, we couldn't use a pretty name so try File#createTempFile().
        if (tfos == null)
        {
            tf = File.createTempFile("sc-setup", ".exe");
            tfos = new FileOutputStream(tf);
        }

        tempFile[0] = tf;
        return tfos;
    }

    /**
     * Invokes "Check for Updates".
     *
     * @param notifyAboutNewestVersion <tt>true</tt> if the user is to be
     * notified if they have the newest version already; otherwise,
     * <tt>false</tt>
     */
    private synchronized void checkForUpdates(
            final boolean notifyAboutNewestVersion)
    {
        if (inCheckForUpdates > 0)
        {
            if (checkForUpdatesDialog != null)
                checkForUpdatesDialog.toFront();
            return;
        }

        Thread checkForUpdatesThread
            = new Thread()
            {
                @Override
                public void run()
                {
                    try
                    {
                        if(isNewestVersion())
                        {
                            if(!isAuthenticationCanceled
                                    && notifyAboutNewestVersion)
                            {
                                ResourceManagementService resources
                                    = getResources();

                                getUIService()
                                    .getPopupDialog()
                                        .showMessagePopupDialog(
                                                resources.getI18NString(
                                                        "plugin.updatechecker.DIALOG_NOUPDATE"),
                                                resources.getI18NString(
                                                        "plugin.updatechecker.DIALOG_NOUPDATE_TITLE"),
                                                PopupDialog.INFORMATION_MESSAGE);
                            }
                        }
                        else if (OSUtils.IS_WINDOWS)
                            showWindowsNewVersionAvailableDialog();
                        else
                            showGenericNewVersionAvailableDialog();
                    }
                    finally
                    {
                        exitCheckForUpdates(null);
                    }
                }
            };

        checkForUpdatesThread.setDaemon(true);

        enterCheckForUpdates(null);
        try
        {
            checkForUpdatesThread.start();
            checkForUpdatesThread = null;
        }
        finally
        {
            if (checkForUpdatesThread != null)
                exitCheckForUpdates(null);
        }
    }

    /**
     * Notifies this <tt>UpdateCheckActivator</tt> that a method is entering the
     * "Check for Updates" functionality and it is thus not allowed to enter it
     * again.
     *
     * @param checkForUpdatesDialog the <tt>JDialog</tt> associated with the
     * entry in the "Check for Updates" functionality if any. While "Check for
     * Updates" cannot be entered again, clicking the "Check for Updates" menu
     * item will bring the <tt>checkForUpdatesDialog</tt> to the front.
     */
    private synchronized void enterCheckForUpdates(
            JDialog checkForUpdatesDialog)
    {
        inCheckForUpdates++;
//        if (1 == inCheckForUpdates)
//            checkForUpdatesMenuItemComponent.getComponent().setEnabled(false);

        if (checkForUpdatesDialog != null)
            this.checkForUpdatesDialog = checkForUpdatesDialog;
    }

    /**
     * Notifies this <tt>UpdateCheckActivator</tt> that a method is exiting the
     * "Check for Updates" functionality and it may thus be allowed to enter it
     * again.
     *
     * @param checkForUpdatesDialog the <tt>JDialog</tt> which was associated
     * with the matching call to {@link #enterCheckForUpdates(JDialog)} if any
     */
    private synchronized void exitCheckForUpdates(JDialog checkForUpdatesDialog)
    {
        if (inCheckForUpdates == 0)
            throw new IllegalStateException("inCheckForUpdates");
        else
        {
            inCheckForUpdates--;
//            if (0 == inCheckForUpdates)
//            {
//                checkForUpdatesMenuItemComponent.getComponent().setEnabled(
//                        true);
//            }

            if ((checkForUpdatesDialog != null)
                    && (this.checkForUpdatesDialog == checkForUpdatesDialog))
                this.checkForUpdatesDialog = null;
        }
    }

    /**
     * Implements <tt>PluginComponent</tt> for the "Check for Updates" menu
     * item.
     */
    private class CheckForUpdatesMenuItemComponent
        extends AbstractPluginComponent
    {
        /**
         * The "Check for Updates" menu item.
         */
        private final JMenuItem checkForUpdatesMenuItem
            = new JMenuItem(
                    getResources().getI18NString(
                            "plugin.updatechecker.UPDATE_MENU_ENTRY"));

        /**
         * Initializes a new "Check for Updates" menu item.
         *
         * @param container the container of the update menu component
         */
        public CheckForUpdatesMenuItemComponent(Container container)
        {
            super(container);

            checkForUpdatesMenuItem.addActionListener(new ActionListener()
            {
                public void actionPerformed(ActionEvent e)
                {
                    checkForUpdates(true);
                }
            });
        }

        /**
         * Gets the UI <tt>Component</tt> of this <tt>PluginComponent</tt>.
         *
         * @return the UI <tt>Component</tt> of this <tt>PluginComponent</tt>
         * @see PluginComponent#getComponent()
         */
        public JMenuItem getComponent()
        {
            return checkForUpdatesMenuItem;
        }

        /**
         * Gets the name of this <tt>PluginComponent</tt>.
         *
         * @return the name of this <tt>PluginComponent</tt>
         * @see PluginComponent#getName()
         */
        public String getName()
        {
            return getResources().getI18NString(
                "plugin.updatechecker.UPDATE_MENU_ENTRY");
        }
    }
}
