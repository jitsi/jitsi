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

import javax.net.ssl.*;
import javax.swing.*;

import net.java.sip.communicator.service.browserlauncher.*;
import net.java.sip.communicator.service.configuration.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.gui.Container; // disambiguation
import net.java.sip.communicator.service.certificate.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.resources.*;
import net.java.sip.communicator.service.shutdown.*;
import net.java.sip.communicator.service.version.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.swing.*;

import org.osgi.framework.*;

/**
 * Activates the UpdateCheck plugin
 *
 * @author Damian Minkov
 * @author Lubomir Marinov
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
    private static BrowserLauncherService browserLauncherService;

    /**
     * Reference to the <tt>ResourceManagementService</tt>.
     */
    private static ResourceManagementService resourcesService;

    /**
     * Reference to the <tt>ConfigurationService</tt>.
     */
    private static ConfigurationService configService;

    /**
     * Reference to the <tt>UIService</tt>.
     */
    private static UIService uiService = null;

    /**
     * Reference to the <tt>CertificateVerificationService</tt>.
     */
    private static CertificateVerificationService certificateService = null;

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

    static
    {
        removeDownloadRestrictions();
    }

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

        try
        {
            logger.logEntry();
            UpdateCheckActivator.bundleContext = bundleContext;
        }
        finally
        {
            logger.logExit();
        }

        Thread updateThread = new Thread(new UpdateCheckThread());
        updateThread.setDaemon(true);
        updateThread.start();

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
        if (browserLauncherService == null)
        {
            ServiceReference serviceReference = bundleContext
                .getServiceReference(BrowserLauncherService.class.getName());

            browserLauncherService = (BrowserLauncherService) bundleContext
                .getService(serviceReference);
        }

        return browserLauncherService;
    }

    /**
     * Returns the <tt>ConfigurationService</tt> obtained from the bundle
     * context.
     *
     * @return the <tt>ConfigurationService</tt> obtained from the bundle
     *         context
     */
    private static ConfigurationService getConfigurationService()
    {
        if (configService == null)
        {
            ServiceReference configReference =
                bundleContext.getServiceReference(ConfigurationService.class
                    .getName());

            configService =
                (ConfigurationService) bundleContext
                    .getService(configReference);
        }

        return configService;
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
        return
            (ShutdownService)
                bundleContext.getService(
                    bundleContext.getServiceReference(
                        ShutdownService.class.getName()));
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
        {
            ServiceReference uiServiceReference
                = bundleContext.getServiceReference(
                    UIService.class.getName());
            uiService = (UIService)bundleContext
                .getService(uiServiceReference);
        }
        return uiService;
    }

    /**
     * Returns resource service.
     * @return the resource service.
     */
    private static ResourceManagementService getResources()
    {
        if (resourcesService == null)
        {
            ServiceReference serviceReference = bundleContext
                .getServiceReference(ResourceManagementService.class.getName());

            if(serviceReference == null)
                return null;

            resourcesService = (ResourceManagementService) bundleContext
                .getService(serviceReference);
        }

        return resourcesService;
    }

    /**
     * Return the certificate verification service impl.
     * @return the CertificateVerification service.
     */
    private static CertificateVerificationService
        getCertificateVerificationService()
    {
        if(certificateService == null)
        {
            ServiceReference certVerifyReference
                = bundleContext.getServiceReference(
                    CertificateVerificationService.class.getName());
            if(certVerifyReference != null)
                certificateService
                = (CertificateVerificationService)bundleContext.getService(
                        certVerifyReference);
        }

        return certificateService;
    }

    /**
     * Check the first link as files on the web are sorted by date
     * @return whether we are using the latest version or not.
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

            String configString = null;

            configString = getConfigurationService().getString(
                    PROP_UPDATE_LINK);

            if(configString == null)
            {
                configString = Resources.getConfigString("update_link");
            }

            if(configString == null)
            {
                if (logger.isDebugEnabled())
                    logger.debug(
                            "Updates are disabled. Faking latest version.");
                return true;
            }

            URL url = new URL(configString);
            URLConnection conn = url.openConnection();

            if (conn instanceof HttpURLConnection)
            {
                while(((HttpURLConnection)conn).getResponseCode() ==
                            HttpURLConnection.HTTP_UNAUTHORIZED
                        && !isAuthenticationCanceled)
                {
                    if(userCredentials.getUserName() != null)
                    {
                        errorMessage = getResources().getI18NString(
                            "service.gui.AUTHENTICATION_FAILED",
                            new String[]{
                                userCredentials.getUserName(),
                                host});

                        userCredentials.setUserName(null);
                        userCredentials.setPasswordPersistent(false);
                        userCredentials = null;

                        getConfigurationService().removeProperty(
                            UPDATE_USERNAME_CONFIG);
                        getConfigurationService().removeProperty(
                            UPDATE_PASSWORD_CONFIG);                        

                        conn = url.openConnection();
                    }
                    else
                        break;
                }
            }
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);

            Properties props = new Properties();
            props.load(conn.getInputStream());

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
     * Show dialog informing about new version with button Download which
     * triggers browser launching
     */
    private void UpdaterShow()
    {
        final JDialog dialog = new SIPCommDialog()
        {
            private static final long serialVersionUID = 0L;

            protected void close(boolean isEscaped)
            {
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
        JButton closeButton = new JButton(
            getResources().getI18NString(
                    "plugin.updatechecker.BUTTON_CLOSE"));

        closeButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e)
            {
                dialog.setVisible(false);
            }
        });

        if(downloadLink != null)
        {
            JButton downloadButton = new JButton(getResources().
                    getI18NString("plugin.updatechecker.BUTTON_DOWNLOAD"));

            downloadButton.addActionListener(new ActionListener()
            {
                public void actionPerformed(ActionEvent e)
                {
                    if(OSUtils.IS_LINUX64)
                        downloadLink
                            = downloadLink.replace("i386", "amd64");

                    getBrowserLauncher().openURL(downloadLink);
                    dialog.dispose();
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
            screenSize.height/2 - dialog.getHeight()/2
        );

        dialog.setVisible(true);
    }

    /**
     * Shows dialog informing about new version with button Install
     * which triggers the update process.
     */
    private void windowsUpdaterShow()
    {
        final JDialog dialog = new SIPCommDialog()
        {
            private static final long serialVersionUID = 0L;

            protected void close(boolean isEscaped)
            {
            }
        };

        dialog.setTitle(
            getResources().getI18NString("plugin.updatechecker.DIALOG_TITLE"));

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

        String dialogMsg =
            getResources().getI18NString("plugin.updatechecker.DIALOG_MESSAGE",
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
            changesHtml.setPage(new URL(changesLink));
        } catch (Exception e)
        {
            logger.error("Cannot set changes Page", e);
        }

        JPanel buttonPanel
            = new TransparentPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        JButton closeButton = new JButton(
            getResources().getI18NString("plugin.updatechecker.BUTTON_CLOSE"));

        closeButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e)
            {
                dialog.setVisible(false);
            }
        });

        if(downloadLink != null)
        {
            JButton installButton = new JButton(getResources().getI18NString(
                "plugin.updatechecker.BUTTON_INSTALL"));

            installButton.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e)
                {
                    if(OSUtils.IS_WINDOWS64)
                        downloadLink = downloadLink.replace("x86", "x64");

                    dialog.dispose();
                    windowsUpdate();
                }
            });

            buttonPanel.add(installButton);
        }

        buttonPanel.add(closeButton);

        contentPane.add(buttonPanel, BorderLayout.SOUTH);

        dialog.setContentPane(contentPane);

        dialog.pack();

        dialog.setLocation(
            Toolkit.getDefaultToolkit().getScreenSize().width/2
                - dialog.getWidth()/2,
            Toolkit.getDefaultToolkit().getScreenSize().height/2
                - dialog.getHeight()/2
        );

        dialog.setVisible(true);
    }

    /**
     * The update process itself.
     * - Downloads the installer in a temp directory.
     * - Warns that update will shutdown.
     * - Triggers update (installer) in separate process with the help
     * of update.exe and shutdowns.
     */
    private void windowsUpdate()
    {
        File tempF = null;
        try
        {
            final File temp = File.createTempFile("sc-install", ".exe");
            tempF = temp;

            URL u = new URL(downloadLink);
            URLConnection uc = u.openConnection();

            if (uc instanceof HttpURLConnection)
            {
                if(uc instanceof HttpsURLConnection)
                {
                    CertificateVerificationService vs =
                        getCertificateVerificationService();

                    int port = u.getPort();

                    /* if we do not specify port in the URL
                     * (http://domain.org:port) we have to set up the default
                     * port of HTTP (80) or
                     * HTTPS (443).
                     */
                    if(port == -1)
                    {
                        if(u.getProtocol().equals("http"))
                        {
                            port = 80;
                        }
                        else if(u.getProtocol().equals("https"))
                        {
                            port = 443;
                        }
                    }

                    ((HttpsURLConnection)uc).setSSLSocketFactory(
                            vs.getSSLContext(
                            u.getHost(), port).getSocketFactory());
                }

                // we don't handle here authentication fails cause
                // still we gone to downloading file we have gone through
                // successful authentication
            }

            InputStream in = uc.getInputStream();

            // Chain a ProgressMonitorInputStream to the
            // URLConnection's InputStream
            final ProgressMonitorInputStream pin
             = new ProgressMonitorInputStream(null, u.toString(), in);

            // Set the maximum value of the ProgressMonitor
            ProgressMonitor pm = pin.getProgressMonitor();
            pm.setMaximum(uc.getContentLength());

            final BufferedOutputStream out =
                    new BufferedOutputStream(new FileOutputStream(temp));
            new Thread(new Runnable()
            {
                public void run()
                {
                    try
                    {
                        int read = -1;
                        byte[] buff = new byte[1024];
                        while((read = pin.read(buff)) != -1)
                        {
                            out.write(buff, 0, read);
                        }
                        pin.close();
                        out.flush();
                        out.close();

                        if(getUIService().getPopupDialog().
                            showConfirmPopupDialog(
                                    getResources().getI18NString(
                                    "plugin.updatechecker.DIALOG_WARN"),
                                    getResources().getI18NString(
                                    "plugin.updatechecker.DIALOG_TITLE"),
                                    PopupDialog.YES_NO_OPTION,
                                    PopupDialog.QUESTION_MESSAGE
                                ) != PopupDialog.YES_OPTION)
                        {
                            return;
                        }

                        String packageName = getResources().getSettingsString(
                                "plugin.updatechecker.package.name");

                        // file saved. Now start updater and shutdown.
                        String workingDir = System.getProperty("user.dir");

                        String updateFileLocation = workingDir + File.separator;
                        if(packageName != null)
                            updateFileLocation += packageName + "-up2date.exe";
                        else
                            updateFileLocation += "up2date.exe";

                        ProcessBuilder processBuilder
                            = new ProcessBuilder(
                                new String[]
                                {
                                    updateFileLocation,
                                    "--wait-parent",
                                    "--allow-elevation",
                                    temp.getCanonicalPath(),
                                    workingDir
                                });
                        processBuilder.start();

                        getShutdownService().beginShutdown();

                    } catch (Exception e)
                    {
                        logger.error("Error saving", e);
                        try
                        {
                            pin.close();
                            out.close();
                        } catch (Exception e1)
                        {}
                    }
                }
            }).start();

        }
        catch(FileNotFoundException e)
        {
            getUIService().getPopupDialog().showMessagePopupDialog(
                getResources().getI18NString(
                        "plugin.updatechecker.DIALOG_MISSING_UPDATE"),
                getResources().getI18NString(
                        "plugin.updatechecker.DIALOG_NOUPDATE_TITLE"),
                PopupDialog.INFORMATION_MESSAGE);
            tempF.delete();
        }
        catch (Exception e)
        {
            if (logger.isInfoEnabled())
                logger.info("Error starting update process!", e);
            tempF.delete();
        }
    }

    /**
     * Invokes action for checking for updates.
     */
    private void checkForUpdate()
    {
        if(isNewestVersion())
        {
            if(isAuthenticationCanceled)
                return;

            getUIService().getPopupDialog().showMessagePopupDialog(
                getResources().getI18NString(
                        "plugin.updatechecker.DIALOG_NOUPDATE"),
                getResources().getI18NString(
                        "plugin.updatechecker.DIALOG_NOUPDATE_TITLE"),
                PopupDialog.INFORMATION_MESSAGE);
        }
        else
            windowsUpdaterShow();
    }

    /**
     * Installs Dummy TrustManager will not try to validate self-signed certs.
     * Fix some problems with not proper use of certs.
     */
    private static void removeDownloadRestrictions()
    {
        HostnameVerifier hv = new HostnameVerifier()
        {
            public boolean verify(String urlHostName, SSLSession session)
            {
                logger.warn("Warning: URL Host: " + urlHostName +
                        " vs. " + session.getPeerHost());
                return true;
            }
        };
        HttpsURLConnection.setDefaultHostnameVerifier(hv);

        Authenticator.setDefault(new Authenticator()
        {
            protected PasswordAuthentication getPasswordAuthentication()
            {
                if(userCredentials == null)
                {
                    // if there is something save return it
                    ConfigurationService config = getConfigurationService();
                    String uName
                        = (String) config.getProperty(UPDATE_USERNAME_CONFIG);
                    if(uName != null)
                    {
                        String pass
                            = (String) config.getProperty(UPDATE_PASSWORD_CONFIG);

                        if(pass != null)
                        {
                            userCredentials = new UserCredentials();
                            userCredentials.setUserName(uName);
                            userCredentials.setPassword(new String(
                                    Base64.decode(pass)).toCharArray());
                            userCredentials.setPasswordPersistent(true);
                        }
                    }
                }

                if(userCredentials != null)
                {
                    return new PasswordAuthentication(
                        userCredentials.getUserName(),
                        userCredentials.getPassword());
                }
                else
                {
                    host = getRequestingHost();

                    AuthenticationWindow authWindow = null;
                    if(errorMessage == null)
                    {
                        authWindow = new AuthenticationWindow(host, true, null);
                    }
                    else
                    {
                        authWindow = new AuthenticationWindow(
                                null, null, host, true, null, errorMessage);
                        // we showed the message, remove it
                        errorMessage = null;
                    }

                    userCredentials = new UserCredentials();

                    authWindow.setVisible(true);

                    if (!authWindow.isCanceled())
                    {
                        isAuthenticationCanceled = false;
                        userCredentials.setUserName(authWindow.getUserName());
                        userCredentials.setPassword(authWindow.getPassword());
                        userCredentials.setPasswordPersistent(
                            authWindow.isRememberPassword());

                        if(authWindow.isRememberPassword())
                        {
                            // if save password is checked save the pass
                            getConfigurationService().setProperty(
                                UPDATE_USERNAME_CONFIG,
                                userCredentials.getUserName());
                            getConfigurationService().setProperty(
                                UPDATE_PASSWORD_CONFIG,
                                new String(Base64.encode(
                                    userCredentials.getPasswordAsString()
                                    .getBytes())));
                        }

                         return new PasswordAuthentication(
                            userCredentials.getUserName(),
                            userCredentials.getPassword());
                    }
                    else
                    {
                        isAuthenticationCanceled = true;
                        userCredentials.setUserName(null);
                        userCredentials = null;

                        getConfigurationService().removeProperty(
                            UPDATE_USERNAME_CONFIG);
                        getConfigurationService().removeProperty(
                            UPDATE_PASSWORD_CONFIG);
                    }

                    return null;
                }
            }
        });
    }

    /**
     * The menu entry under tools menu.
     */
    private class UpdateMenuButtonComponent
        extends AbstractPluginComponent
    {
        /**
         * The menu item to use.
         */
        private final JMenuItem updateMenuItem
            = new JMenuItem(getResources().
                getI18NString("plugin.updatechecker.UPDATE_MENU_ENTRY"));

        /**
         * Creates update menu component.
         *
         * @param container the container of the update menu component
         */
        UpdateMenuButtonComponent(Container container)
        {
            super(container);

            updateMenuItem.addActionListener(new ActionListener()
            {
                public void actionPerformed(ActionEvent e)
                {
                    // run outside swing thread, if password is required we
                    // will block the swing
                    new Thread()
                    {
                        public void run()
                        {
                            checkForUpdate();
                        }
                    }.start();
                }
            });
        }

        /**
         * Get the name of the component.
         *
         * @return name of the component
         */
        public String getName()
        {
            return getResources().getI18NString(
                "plugin.updatechecker.UPDATE_MENU_ENTRY");
        }

        /**
         * Get the <tt>Component</tt>.
         *
         * @return the <tt>Component</tt>
         */
        public Object getComponent()
        {
            return updateMenuItem;
        }
    }

    /**
     * The thread that do the actual checking.
     */
    private class UpdateCheckThread
        implements Runnable
    {
        /**
         * Thread entry point.
         */
        public void run()
        {
            if (OSUtils.IS_WINDOWS)
            {
                // register update button
                Hashtable<String, String> toolsMenuFilter
                    = new Hashtable<String, String>();
                toolsMenuFilter.put( Container.CONTAINER_ID,
                                     Container.CONTAINER_HELP_MENU.getID());

                bundleContext.registerService(
                    PluginComponent.class.getName(),
                    new UpdateMenuButtonComponent(
                        Container.CONTAINER_HELP_MENU),
                    toolsMenuFilter);
            }

            // check whether check at startup is enabled
            if(!getConfigurationService().getBoolean(UPDATECHECKER_ENABLED,
                    true))
            {
                return;
            }

            if(isNewestVersion())
                return;

            if (OSUtils.IS_WINDOWS)
            {
                windowsUpdaterShow();
            }
            else
            {
                UpdaterShow();
            }
        }
    }
}
