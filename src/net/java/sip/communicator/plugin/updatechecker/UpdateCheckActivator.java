/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.updatechecker;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Toolkit;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.*;

import javax.swing.*;

import net.java.sip.communicator.service.browserlauncher.*;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.resources.*;
import net.java.sip.communicator.service.version.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.swing.*;

import org.osgi.framework.*;

/**
 * Activates the UpdateCheck plugin
 * @author Damian Minkov
**/
public class UpdateCheckActivator
    implements BundleActivator
{

    private static Logger logger = Logger.getLogger(UpdateCheckActivator.class);

    private static BundleContext bundleContext = null;

    private static BrowserLauncherService browserLauncherService;

    private static ResourceManagementService resourcesService;

    private static UIService            uiService             = null;

    private String downloadLink = null;
    private String lastVersion = null;
    private String changesLink = null;

    /**
     * Starts this bundle
     *
     * @param bundleContext BundleContext
     * @throws Exception
     */
    public void start(BundleContext bundleContext) throws Exception
    {
        try
        {
            logger.logEntry();
            UpdateCheckActivator.bundleContext = bundleContext;
        }
        finally
        {
            logger.logExit();
        }

        String osName = System.getProperty("os.name");

        if (osName.startsWith("Windows"))
        {
            // register update button
            Hashtable<String, String> toolsMenuFilter
                = new Hashtable<String, String>();
            toolsMenuFilter.put( Container.CONTAINER_ID,
                                 Container.CONTAINER_TOOLS_MENU.getID());

            bundleContext.registerService(
                PluginComponent.class.getName(),
                new UpdateMenuButtonComponent(
                    Container.CONTAINER_TOOLS_MENU),
                toolsMenuFilter);
        }

        if(isNewestVersion())
            return;

        if (osName.startsWith("Windows"))
        {
            windowsUpdaterShow();
            return;
        }

        final JDialog dialog = new SIPCommDialog()
        {
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

        JPanel contentPane = new TransparentPanel(new BorderLayout(5,5));
        contentPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        contentPane.add(contentMessage, BorderLayout.CENTER);

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
            JButton downloadButton = new JButton(getResources().getI18NString(
                "plugin.updatechecker.BUTTON_DOWNLOAD"));

            downloadButton.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e)
                {
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

        dialog.setLocation(
            Toolkit.getDefaultToolkit().getScreenSize().width/2
                - dialog.getWidth()/2,
            Toolkit.getDefaultToolkit().getScreenSize().height/2
                - dialog.getHeight()/2
        );

        dialog.setVisible(true);
    }

    /**
    * stop the bundle
    */
    public void stop(BundleContext bundleContext) throws Exception
    {
    }

    /**
     * Returns the <tt>BrowserLauncherService</tt> obtained from the bundle
     * context.
     * @return the <tt>BrowserLauncherService</tt> obtained from the bundle
     * context
     */
    public static BrowserLauncherService getBrowserLauncher()
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
     * Returns a reference to the UIService implementation currently registered
     * in the bundle context or null if no such implementation was found.
     *
     * @return a reference to a UIService implementation currently registered
     * in the bundle context or null if no such implementation was found.
     */
    public static UIService getUIService()
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

    public static ResourceManagementService getResources()
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
     * Check the first link as files on the web are sorted by date
     * @param currentVersionStr
     * @return
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

            String configString = Resources.getConfigString("update_link");

            if(configString == null)
            {
                logger.debug("Updates are disabled. Faking latest version.");
                return true;
            }

            URL url = new URL(configString);

            Properties props = new Properties();
            props.load(url.openStream());

            lastVersion = props.getProperty("last_version");
            downloadLink = props.getProperty("download_link");

            changesLink =
                    configString.substring(0, configString.lastIndexOf("/") + 1) +
                    props.getProperty("changes_html");

            return lastVersion.compareTo(ver.toString()) <= 0;
        }
        catch (Exception e)
        {
            logger.warn("Cannot get and compare versions!");
            logger.debug("Error was: ", e);
            // if we get an exception this mean we were unable to compare versions
            // will retrun that current is newest to prevent opening info dialog
            // about new version
            return true;
        }
    }

    /**
     * Shows dialog informing about new version with button Install
     * which trigers the update process.
     */
    private void windowsUpdaterShow()
    {
        final JDialog dialog = new SIPCommDialog()
        {
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
        scrollChanges.setPreferredSize(new Dimension(400, 200));
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
     * - Trigers update (installer) in separate process with the help
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

                        if(getUIService().getPopupDialog().showConfirmPopupDialog(
                            getResources().getI18NString(
                                "plugin.updatechecker.DIALOG_WARN"),
                            getResources().getI18NString(
                                "plugin.updatechecker.DIALOG_TITLE"),
                            PopupDialog.YES_NO_OPTION,
                            PopupDialog.QUESTION_MESSAGE
                            ) == PopupDialog.CANCEL_OPTION)
                        {
                            return;
                        }

                        // file saved. Now start updater and shutdown.
                        String workingDir = System.getProperty("user.dir");
                        new ProcessBuilder(
                            new String[]{
                                workingDir + File.separator + "updater.exe",
                                temp.getCanonicalPath()}).start();
                        getUIService().beginShutdown();

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

        } catch (Exception e)
        {
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
            getUIService().getPopupDialog().showMessagePopupDialog(
                getResources().getI18NString("plugin.updatechecker.DIALOG_NOUPDATE"),
                getResources().getI18NString("plugin.updatechecker.DIALOG_NOUPDATE_TITLE"),
                PopupDialog.INFORMATION_MESSAGE);
        }
        else
            windowsUpdaterShow();
    }

    /**
     * The menu entry under tools menu.
     */
    private class UpdateMenuButtonComponent
        implements PluginComponent
    {
        private final Container container;

        private final JMenuItem updateMenuItem
            = new JMenuItem(getResources().
                getI18NString("plugin.updatechecker.UPDATE_MENU_ENTRY"));

        UpdateMenuButtonComponent(Container c)
        {
            this.container = c;

            updateMenuItem.addActionListener(new ActionListener()
            {
                public void actionPerformed(ActionEvent e)
                {
                    checkForUpdate();
                }
            });
        }

        public String getName()
        {
            return getResources().getI18NString(
                "plugin.updatechecker.UPDATE_MENU_ENTRY");
        }

        public Container getContainer()
        {
            return this.container;
        }

        public String getConstraints()
        {
            return null;
        }

        public int getPositionIndex()
        {
            return -1;
        }

        public Object getComponent()
        {
            return updateMenuItem;
        }

        public void setCurrentContact(MetaContact metaContact)
        {
        }

        public void setCurrentContactGroup(MetaContactGroup metaGroup)
        {
        }

        public boolean isNativeComponent()
        {
            return false;
        }
    }
}
