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
package net.java.sip.communicator.plugin.update;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.List;

import javax.swing.*;
import javax.swing.text.*;

import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.httputil.*;
import net.java.sip.communicator.util.osgi.ServiceUtils;
import org.jitsi.service.resources.*;
import org.jitsi.utils.version.*;
import org.jitsi.util.*;

/**
 * Implements checking for software updates, downloading and applying them i.e.
 * the very logic of the update plug-in.
 *
 * @author Damian Minkov
 * @author Lyubomir Marinov
 */
public class UpdateServiceImpl
{
    /**
     * The link pointing to the ChangeLog of the update.
     */
    private static String changesLink;

    /**
     * The <tt>JDialog</tt>, if any, which is associated with the currently
     * executing "Check for Updates". While the "Check for Updates"
     * functionality cannot be entered, clicking the "Check for Updates" menu
     * item will bring it to the front.
     */
    private static JDialog checkForUpdatesDialog;

    /**
     * The link pointing at the download of the update.
     */
    private static String downloadLink;

    /**
     * The indicator/counter which determines how many methods are currently
     * executing the "Check for Updates" functionality so that it is known
     * whether it can be entered.
     */
    private static int inCheckForUpdates = 0;

    /**
     * The latest version of the software found at the configured update
     * location.
     */
    private static String latestVersion;

    /**
     * The <tt>Logger</tt> used by the <tt>UpdateServiceImpl</tt> class and its
     * instances for logging output.
     */
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(UpdateServiceImpl.class);

    /**
     * The name of the property which specifies the update link in the
     * configuration file.
     */
    private static final String PROP_UPDATE_LINK
        = "net.java.sip.communicator.UPDATE_LINK";

    /**
     * Initializes a new Web browser <tt>Component</tt> instance and navigates
     * it to a specific URL.
     *
     * @param url the URL to navigate the new Web browser <tt>Component</tt>
     * instance
     * @return the new Web browser <tt>Component</tt> instance which has been
     * navigated to the specified <tt>url</tt>
     */
    private static Component createBrowser(String url)
    {
        // Initialize the user interface.
        JEditorPane editorPane = new JEditorPane();

        editorPane.setContentType("text/html");
        editorPane.setEditable(false);

        JScrollPane scrollPane = new JScrollPane();

        scrollPane.setViewportView(editorPane);

        // Navigate the user interface to the specified URL.
        try
        {
            Document document = editorPane.getDocument();

            if (document instanceof AbstractDocument)
                ((AbstractDocument) document).setAsynchronousLoadPriority(0);

            editorPane.setPage(new URL(url));
        }
        catch (Throwable t)
        {
            if (t instanceof ThreadDeath)
                throw (ThreadDeath) t;
            else
            {
                logger.error(
                        "Failed to navigate the Web browser to: " + url,
                        t);
            }
        }

        return scrollPane;
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
    private static synchronized void enterCheckForUpdates(
            JDialog checkForUpdatesDialog)
    {
        inCheckForUpdates++;
        if (checkForUpdatesDialog != null)
            UpdateServiceImpl.checkForUpdatesDialog = checkForUpdatesDialog;
    }

    /**
     * Notifies this <tt>UpdateCheckActivator</tt> that a method is exiting the
     * "Check for Updates" functionality and it may thus be allowed to enter it
     * again.
     *
     * @param checkForUpdatesDialog the <tt>JDialog</tt> which was associated
     * with the matching call to {@link #enterCheckForUpdates(JDialog)} if any
     */
    private static synchronized void exitCheckForUpdates(
            JDialog checkForUpdatesDialog)
    {
        if (inCheckForUpdates == 0)
            throw new IllegalStateException("inCheckForUpdates");
        else
        {
            inCheckForUpdates--;
            if ((checkForUpdatesDialog != null)
                    && (UpdateServiceImpl.checkForUpdatesDialog
                            == checkForUpdatesDialog))
                UpdateServiceImpl.checkForUpdatesDialog = null;
        }
    }

    /**
     * Gets the current (software) version.
     *
     * @return the current (software) version
     */
    private static Version getCurrentVersion()
    {
        return getVersionService().getCurrentVersion();
    }

    /**
     * Returns the currently registered instance of version service.
     * @return the current version service.
     */
    private static VersionService getVersionService()
    {
        return ServiceUtils.getService(
                    UpdateActivator.bundleContext,
                    VersionService.class);
    }

    /**
     * Determines whether we are currently running the latest version.
     *
     * @return <tt>true</tt> if we are currently running the latest version;
     * otherwise, <tt>false</tt>
     */
    private static boolean isLatestVersion()
    {
        try
        {
            String updateLink
                = UpdateActivator.getConfiguration().getString(
                        PROP_UPDATE_LINK);

            if(updateLink == null)
            {
                updateLink
                    = Resources.getUpdateConfigurationString("update_link");
            }
            if(updateLink == null)
            {
                if (logger.isDebugEnabled())
                    logger.debug(
                            "Updates are disabled, faking latest version.");
            }
            else
            {
                HttpUtils.HTTPResponseResult res
                    = HttpUtils.openURLConnection(updateLink);

                if (res != null)
                {
                    Properties props = new Properties();
                    try (var in = res.getContent())
                    {
                        props.load(in);
                    }

                    latestVersion = props.getProperty("last_version");
                    downloadLink = props.getProperty("download_link");
                    changesLink
                        = updateLink.substring(
                                0,
                                updateLink.lastIndexOf("/") + 1)
                            + props.getProperty("changes_html");

                    try
                    {
                        VersionService versionService = getVersionService();

                        Version latestVersionObj =
                            versionService.parseVersionString(latestVersion);

                        if(latestVersionObj != null)
                            return latestVersionObj.compareTo(
                                        getCurrentVersion()) <= 0;
                        else
                            logger.error("Version obj not parsed("
                                                + latestVersion + ")");
                    }
                    catch(Throwable t)
                    {
                        logger.error("Error parsing version string", t);
                    }

                    // fallback to lexicographically compare
                    // of version strings in case of an error
                    return latestVersion.compareTo(
                                getCurrentVersion().toString()) <= 0;
                }
            }
        }
        catch (Exception e)
        {
            logger.warn(
                    "Could not retrieve latest version or compare it to current"
                        + " version",
                    e);
            /*
             * If we get an exception, then we will return that the current
             * version is the newest one in order to prevent opening the dialog
             * notifying about the availability of a new version.
             */
        }
        return true;
    }

    /**
     * Runs in a daemon/background <tt>Thread</tt> dedicated to checking whether
     * a new version of the application is available and notifying the user
     * about the result of the check.
     *
     * @param notifyAboutNewestVersion <tt>true</tt> to notify the user in case
     * she is running the newest/latest version available already; otherwise,
     * <tt>false</tt>
     */
    private static void runInCheckForUpdatesThread(
            boolean notifyAboutNewestVersion)
    {
        if(isLatestVersion())
        {
            if(notifyAboutNewestVersion)
            {
                SwingUtilities.invokeLater(
                    new Runnable()
                    {
                        public void run()
                        {
                            UIService ui = UpdateActivator.getUIService();
                            ResourceManagementService r
                                = Resources.getResources();

                            ui.getPopupDialog().showMessagePopupDialog(
                                    r.getI18NString(
                                        "plugin.updatechecker.DIALOG_NOUPDATE"),
                                    r.getI18NString(
                                        "plugin.updatechecker.DIALOG_NOUPDATE_TITLE"),
                                    PopupDialog.INFORMATION_MESSAGE);
                        }
                    });
            }
        }
        else
        {
            SwingUtilities.invokeLater(
                    new Runnable()
                    {
                        public void run()
                        {
                            if (OSUtils.IS_WINDOWS)
                                showWindowsNewVersionAvailableDialog();
                            else
                                showGenericNewVersionAvailableDialog();
                        }
                    });
        }
    }

    /**
     * Shows dialog informing about the availability of a new version with a
     * Download button which launches the system Web browser.
     */
    private static void showGenericNewVersionAvailableDialog()
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

            @Override
            protected void close(boolean escaped)
            {
                synchronized (exitCheckForUpdates)
                {
                    if (exitCheckForUpdates[0])
                        exitCheckForUpdates(this);
                }
            }
        };
        ResourceManagementService resources = Resources.getResources();
        dialog.setTitle(
                resources.getI18NString("plugin.updatechecker.DIALOG_TITLE"));

        JEditorPane contentMessage = new JEditorPane();
        contentMessage.setContentType("text/html");
        contentMessage.setOpaque(false);
        contentMessage.setEditable(false);

        String dialogMsg
            = resources.getI18NString(
                    "plugin.updatechecker.DIALOG_MESSAGE",
                    new String[]
                            {
                                resources.getSettingsString(
                                        "service.gui.APPLICATION_NAME")
                            });
        if(latestVersion != null)
            dialogMsg
                += resources.getI18NString(
                        "plugin.updatechecker.DIALOG_MESSAGE_2",
                        new String[]
                                {
                                    resources.getSettingsString(
                                            "service.gui.APPLICATION_NAME"),
                                    latestVersion
                                });
        contentMessage.setText(dialogMsg);

        JPanel contentPane = new TransparentPanel(new BorderLayout(5,5));
        contentPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        contentPane.add(contentMessage, BorderLayout.CENTER);

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
            JButton downloadButton
                = new JButton(
                        resources.getI18NString(
                                "plugin.updatechecker.BUTTON_DOWNLOAD"));

            downloadButton.addActionListener(new ActionListener()
            {
                public void actionPerformed(ActionEvent e)
                {
                    UpdateActivator.getBrowserLauncher().openURL(downloadLink);

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
    private static void showWindowsNewVersionAvailableDialog()
    {
        /*
         * Before showing the dialog, we'll enterCheckForUpdates() in order to
         * notify that it is not safe to enter "Check for Updates" again. If we
         * don't manage to show the dialog, we'll have to exitCheckForUpdates().
         * If we manage though, we'll have to exitCheckForUpdates() but only
         * once depending on its modality.
         */
        final boolean[] exitCheckForUpdates = new boolean[] { false };
        @SuppressWarnings("serial")
        final JDialog dialog
            = new SIPCommDialog()
            {
                @Override
                protected void close(boolean escaped)
                {
                    synchronized (exitCheckForUpdates)
                    {
                        if (exitCheckForUpdates[0])
                            exitCheckForUpdates(this);
                    }
                }
            };
        ResourceManagementService r = Resources.getResources();

        dialog.setTitle(r.getI18NString("plugin.updatechecker.DIALOG_TITLE"));

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
            = r.getI18NString(
                    "plugin.updatechecker.DIALOG_MESSAGE",
                    new String[]
                            {
                                r.getSettingsString(
                                        "service.gui.APPLICATION_NAME")
                            });

        if(latestVersion != null)
        {
            dialogMsg
                += r.getI18NString(
                        "plugin.updatechecker.DIALOG_MESSAGE_2",
                        new String[]
                                {
                                    r.getSettingsString(
                                            "service.gui.APPLICATION_NAME"),
                                    latestVersion
                                });
        }

        contentMessage.setText(dialogMsg);

        JPanel contentPane = new SIPCommFrame.MainContentPane();
        contentMessage.setBorder(BorderFactory.createEmptyBorder(10, 0, 20, 0));
        contentPane.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
        contentPane.add(contentMessage, BorderLayout.NORTH);

        Component browser = createBrowser(changesLink);

        if (browser != null)
        {
            browser.setPreferredSize(new Dimension(550, 200));
            contentPane.add(browser, BorderLayout.CENTER);
        }

        JPanel buttonPanel
            = new TransparentPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        final JButton closeButton
            = new JButton(
                    r.getI18NString(
                            "plugin.updatechecker.BUTTON_CLOSE"));

        closeButton.addActionListener(
                new ActionListener()
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
                        r.getI18NString("plugin.updatechecker.BUTTON_INSTALL"));

            installButton.addActionListener(
                    new ActionListener()
                    {
                        public void actionPerformed(ActionEvent e)
                        {
                            enterCheckForUpdates(null);
                            try
                            {
                                /*
                                 * Do the same as the Close button in order to
                                 * not duplicate the code.
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
     * Implements the very update procedure on Windows which includes without
     * being limited to:
     * <ol>
     * <li>Downloads the setup in a temporary directory.</li>
     * <li>Warns that the update procedure will shut down the application.</li>
     * <li>Executes the setup in a separate process and shuts down the
     * application.</li>
     * </ol>
     */
    private static void windowsUpdate()
    {
        /*
         * Firstly, try a delta update which contains a bspatch file to be used
         * to reconstruct the latest MSI from the locally-cached one. If it
         * fails, fall back to a full update.
         */
        File delta = null;
        boolean deleteDelta = true;
        File msi = null;

        try
        {
            String deltaTarget = null;

            Version ver = getCurrentVersion();

            if(ver.isNightly())
                deltaTarget = ver.getNightlyBuildID();
            else
                deltaTarget = ver.toString();

            String deltaLink
                = downloadLink.replace(
                        latestVersion,
                        latestVersion + "-delta-" + deltaTarget);

            if (!deltaLink.equalsIgnoreCase(downloadLink))
                delta = FileUtils.download(deltaLink, "setup", ".exe");

            if (delta != null)
            {
                File[] deltaMsi = new File[1];

                FileUtils.createTempFileOutputStream(
                        delta.toURI().toURL(),
                        ".msi",
                        /*
                         * Do not actually create a FileOutputStream, we just
                         * want the File (name).
                         */
                        true,
                        deltaMsi,
                        "setup",
                        ".exe"
                    );

                Process process
                    = new ProcessBuilder(
                            delta.getCanonicalPath(),
                            "--quiet",
                            deltaMsi[0].getCanonicalPath())
                        .start();

                int exitCode = 1;

                while (true)
                {
                    try
                    {
                        exitCode = process.waitFor();
                        break;
                    }
                    catch (InterruptedException ie)
                    {
                        /*
                         * Ignore it, we're interested in the exit code of the
                         * process.
                         */
                    }
                }
                if (0 == exitCode)
                {
                    deleteDelta = false;
                    msi = deltaMsi[0];
                }
            }
        }
        catch (Exception e)
        {
            /* Ignore it, we'll try the full update. */
        }
        finally
        {
            if (deleteDelta && (delta != null))
            {
                delta.delete();
                delta = null;
            }
        }

        /*
         * Secondly, either apply the delta update or download and apply a full
         * update.
         */
        boolean deleteMsi = true;
        deleteDelta = true;

        try
        {
            if (msi == null)
                msi = FileUtils.download(downloadLink, "setup", ".exe");
            if (msi != null)
            {
                ResourceManagementService resources = Resources.getResources();

                if(UpdateActivator.getUIService()
                        .getPopupDialog().showConfirmPopupDialog(
                                resources.getI18NString(
                                        "plugin.updatechecker.DIALOG_WARN",
                                        new String[]{
                                            resources.getSettingsString(
                                                "service.gui.APPLICATION_NAME")
                                            }),
                                resources.getI18NString(
                                        "plugin.updatechecker.DIALOG_TITLE"),
                                PopupDialog.YES_NO_OPTION,
                                PopupDialog.QUESTION_MESSAGE)
                        == PopupDialog.YES_OPTION)
                {
                    List<String> command = new ArrayList<String>();

                    /*
                     * If a delta update is in effect, the delta will execute
                     * the latest MSI it has previously recreated from the
                     * locally-cached MSI. Otherwise, a full update is in effect
                     * and it will just execute itself.
                     */
                    command.add(
                            ((delta == null) ? msi : delta).getCanonicalPath());
                    command.add("--wait-parent");
                    if (delta != null)
                    {
                        command.add("--msiexec");
                        command.add(msi.getCanonicalPath());
                    }
                    command.add(
                            "SIP_COMMUNICATOR_AUTOUPDATE_INSTALLDIR=\""
                                + System.getProperty("user.dir")
                                + "\"");

                    deleteMsi = false;
                    deleteDelta = false;

                    /*
                     * The setup has been downloaded. Now start it and shut
                     * down.
                     */
                    new ProcessBuilder(command).start();

                    UpdateActivator.getShutdownService().beginShutdown();
                }
            }
        }
        catch(FileNotFoundException fnfe)
        {
            ResourceManagementService resources = Resources.getResources();

            UpdateActivator.getUIService()
                .getPopupDialog().showMessagePopupDialog(
                        resources.getI18NString(
                                "plugin.updatechecker.DIALOG_MISSING_UPDATE"),
                        resources.getI18NString(
                                "plugin.updatechecker.DIALOG_NOUPDATE_TITLE"),
                        PopupDialog.INFORMATION_MESSAGE);
        }
        catch (Exception e)
        {
            if (logger.isInfoEnabled())
                logger.info("Could not update", e);
        }
        finally
        {
            /*
             * If we've failed, delete the temporary file into which the setup
             * was supposed to be or has already been downloaded.
             */
            if (deleteMsi && (msi != null))
            {
                msi.delete();
                msi = null;
            }
            if (deleteDelta && (delta != null))
            {
                delta.delete();
                delta = null;
            }
        }
    }

    /**
     * Invokes "Check for Updates".
     *
     * @param notifyAboutNewestVersion <tt>true</tt> if the user is to be
     * notified if they have the newest version already; otherwise,
     * <tt>false</tt>
     */
    public synchronized void checkForUpdates(
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
                        runInCheckForUpdatesThread(notifyAboutNewestVersion);
                    }
                    finally
                    {
                        exitCheckForUpdates(null);
                    }
                }
            };

        checkForUpdatesThread.setDaemon(true);
        checkForUpdatesThread.setName(
                getClass().getName() + ".checkForUpdates");

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
}
