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
package net.java.sip.communicator.impl.neomedia.codec.video.h264;

import net.java.sip.communicator.impl.neomedia.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.browserlauncher.*;
import org.apache.commons.lang3.*;
import org.jitsi.service.configuration.*;
import org.jitsi.service.resources.*;
import org.jitsi.util.*;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.beans.*;
import java.io.*;
import java.nio.file.*;

import org.apache.commons.compress.compressors.bzip2.*;
import org.osgi.framework.*;

/**
 * OpenH264 downloading and installing in correct folder, gives an option
 * to the user to also disable it.
 *
 * @author Damian Minkov
 */
public class OpenH264Retriever
{
    /**
     * The <tt>Logger</tt> used by the <tt>OpenH264Retriever</tt> class and its
     * instances for logging output.
     */
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(OpenH264Retriever.class);

    /**
     * The current version.
     */
    private static final String OPENH264_CURRENT_VERSION = "1.7.0";

    /**
     * These are the download links as said in:
     * https://github.com/cisco/openh264/blob/master/RELEASES
     */
    private static final String OPENH264_CURRENT_VERSION_URL_MAC_64
        = "http://ciscobinary.openh264.org/libopenh264-2.2.0-osx-x64.6.dylib.bz2";
    private static final String OPENH264_CURRENT_VERSION_URL_MAC_ARM
        = "http://ciscobinary.openh264.org/libopenh264-2.2.0-osx-arm64.6.dylib.bz2";
    private static final String OPENH264_CURRENT_VERSION_URL_WINDOWS_32
        = "http://ciscobinary.openh264.org/openh264-2.3.0-win32.dll.bz2";
    private static final String OPENH264_CURRENT_VERSION_URL_WINDOWS_64
        = "http://ciscobinary.openh264.org/openh264-2.3.0-win64.dll.bz2";

    /**
     * These are windows and mac os x locations where the binaries
     * needs to be extracted after downloading.
     */
    private static final String OPENH264_INSTALL_DIR_MAC
        = "/Users/Shared/Library/Application Support/Jitsi/native";
    private static final String OPENH264_INSTALL_DIR_WINDOWS
        = "%ALLUSERSPROFILE%\\Jitsi\\native";

    /**
     * Button action download.
     */
    private static final String ACTION_DOWNLOAD = "DOWNLOAD_OPENH264";

    /**
     * Button action disable.
     */
    private static final String ACTION_DISABLE = "DISABLE_OPENH264";

    /**
     * Property used to store the downloaded version of OpenH264.
     * If nothing is stored, we assume nothing is downloaded, so OpenH264 is
     * currently disabled.
     */
    private static final String OPENH264_INSTALLED_VERSION_PROP
        = OpenH264Retriever.class.getPackage().getName()
            + ".OPENH264_INSTALLED_VERSION";

    /**
     * Returns the configuration panel to be shown to the user.
     */
    public static Container getConfigPanel()
    {
        ResourceManagementService resources = NeomediaActivator.getResources();
        JPanel container = new TransparentPanel(new BorderLayout(5, 5));

        JLabel needRestart = new JLabel(
            resources.getI18NString(
                "impl.neomedia.configform.video.NEED_RESTART"));
        needRestart.setForeground(Color.RED);

        container.setBorder(
            BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(10, 0, 0, 0),
                BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(Color.LIGHT_GRAY),
                    BorderFactory.createEmptyBorder(5, 5 ,5 ,5)
                )));

        ConfigurationService cfg = NeomediaActivator.getConfigurationService();
        JButton actionButton;
        if (isOpenH264Installed(cfg))
        {
            actionButton = new JButton(
                resources.getI18NString(
                    "impl.neomedia.configform.video.DISABLE_OPENH264"));
            actionButton.setActionCommand(ACTION_DISABLE);
        }
        else
        {
            actionButton = new JButton(
                resources
                    .getI18NString("plugin.updatechecker.BUTTON_DOWNLOAD"));
            actionButton.setActionCommand(ACTION_DOWNLOAD);
        }
        container.add(actionButton, BorderLayout.WEST);

        actionButton.addActionListener(new ButtonActionListener());

        // The text as required by the license:
        // http://www.openh264.org/BINARY_LICENSE.txt
        StyledHTMLEditorPane licenseText = new StyledHTMLEditorPane();
        licenseText.appendToEnd(
            "<html><div>OpenH264 Video Codec provided by Cisco Systems, Inc. "
            + "<a href=\"http://www.openh264.org/BINARY_LICENSE.txt\">"
                    + "Show License</a></div></html>");
        licenseText.setOpaque(false);
        licenseText.setEditable(false);
        licenseText.addHyperlinkListener(new HyperlinkListener()
        {
            @Override
            /**
             * Opens a browser when the link has been activated (clicked).
             * @param e the <tt>HyperlinkEvent</tt> that notified us
             */
            public void hyperlinkUpdate(HyperlinkEvent e)
            {
                if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED)
                {
                    ServiceReference<BrowserLauncherService> serviceReference =
                        NeomediaActivator.getBundleContext().getServiceReference(
                            BrowserLauncherService.class);

                    if (serviceReference != null)
                    {
                        BrowserLauncherService browserLauncherService
                            = NeomediaActivator.getBundleContext().getService(serviceReference);

                        browserLauncherService.openURL(e.getDescription());
                    }
                }
            }
        });

        container.add(
            licenseText,
            BorderLayout.NORTH);
        container.add(needRestart, BorderLayout.SOUTH);

        return container;
    }

    /**
     * Listens for download or disable actions from the button.
     */
    private static class ButtonActionListener
        implements ActionListener
    {
        @Override
        public void actionPerformed(ActionEvent e)
        {
            final JButton button = (JButton)e.getSource();
            final ResourceManagementService resources
                = NeomediaActivator.getResources();
            if (e.getActionCommand().equals(ACTION_DOWNLOAD))
            {
                downloadInNewThread();

                NeomediaActivator.getConfigurationService()
                    .addPropertyChangeListener(
                        OPENH264_INSTALLED_VERSION_PROP,
                        new PropertyChangeListener()
                        {
                            @Override
                            public void propertyChange(PropertyChangeEvent evt)
                            {
                                if (evt.getNewValue() != null)
                                {
                                    button.setText(
                                        resources.getI18NString(
                                            "impl.neomedia.configform"
                                                + ".video.DISABLE_OPENH264"));
                                    button.setActionCommand(ACTION_DISABLE);
                                }
                            }
                        });
            }
            else if (e.getActionCommand().equals(ACTION_DISABLE))
            {
                removeFile();

                NeomediaActivator.getConfigurationService().removeProperty(
                    OPENH264_INSTALLED_VERSION_PROP);

                button.setText(resources.getI18NString(
                    "plugin.updatechecker.BUTTON_DOWNLOAD"));
                button.setActionCommand(ACTION_DOWNLOAD);
            }
        }
    }

    /**
     * Checks whether OpenH264 is installed.
     *
     * @param cfg the config service.
     * @return <tt>true</tt> if installed, <tt>false</tt> otherwise.
     */
    private static boolean isOpenH264Installed(ConfigurationService cfg)
    {
        return cfg.getString(OPENH264_INSTALLED_VERSION_PROP) != null;
    }

    /**
     * Checks whether current version matches the one that is already installed,
     * and shows a dialog to inform user to download the new version.
     */
    public static void checkForUpdateAndDownload()
    {
        ConfigurationService cfg = NeomediaActivator.getConfigurationService();

        String installedVersion
            = cfg.getString(OPENH264_INSTALLED_VERSION_PROP);
        if (installedVersion == null
            || installedVersion.equals(OPENH264_CURRENT_VERSION))
        {
            // no need for update
            return;
        }

        final JDialog dialog = new SIPCommDialog();
        ResourceManagementService r = NeomediaActivator.getResources();

        dialog.setTitle(r.getI18NString(
            "impl.neomedia.configform.video.OPENH264_DIALOG_TITLE"));

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
            installedVersion != null ?
                "impl.neomedia.configform.video.OPENH264_UPDATE_AVAILABLE"
                : "impl.neomedia.configform.video.OPENH264_WILL_BE_DOWNLOADED");
        contentMessage.setText(dialogMsg);

        JPanel contentPane = new SIPCommFrame.MainContentPane();
        contentMessage.setBorder(
            BorderFactory.createEmptyBorder(10, 10, 0, 10));
        contentPane.add(contentMessage, BorderLayout.NORTH);

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
                }
            });

        JButton downloadButton = new JButton(
            r.getI18NString("plugin.updatechecker.BUTTON_DOWNLOAD"));

        downloadButton.addActionListener(
            new ActionListener()
            {
                public void actionPerformed(ActionEvent e)
                {
                    try
                    {
                        closeButton.doClick();
                    }
                    finally
                    {
                        downloadInNewThread();
                    }
                }
            });

        buttonPanel.add(downloadButton);

        buttonPanel.add(closeButton);

        contentPane.add(buttonPanel, BorderLayout.SOUTH);

        dialog.setContentPane(contentPane);
        dialog.setMinimumSize(new Dimension(500, 100));
        dialog.pack();

        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        dialog.setLocation(
            screenSize.width/2 - dialog.getWidth()/2,
            screenSize.height/2 - dialog.getHeight()/2);

        dialog.setVisible(true);
    }

    /**
     * Starts a new thread and starts the download in it.
     */
    private static void downloadInNewThread()
    {
        new Thread(() -> {
            try
            {
                String url = chooseOpenH264URL();

                if (url == null)
                {
                    logger.error("Unsupported OS!");
                    return;
                }

                logger.info("Downloading OpenH264 codec from {}", url);
                File f = FileUtils.download(
                    url, "libopenh264", ".bz2");
                if (f != null)
                {
                    install(f);
                }
                else
                {
                    logger.error("Error downloading openh264 binary!");
                }
            }
            catch (IOException e1)
            {
                logger.error("Error downloading openh264 binary!", e1);
            }
        }).start();
    }

    /**
     * Installs the file by extracting it to the correct location.
     *
     * @param file the file that was just downloaded.
     */
    private static void install(File file)
    {
        File destFile = null;
        if (OSUtils.IS_WINDOWS)
        {
            destFile = new File(
                OPENH264_INSTALL_DIR_WINDOWS.replace(
                    "%ALLUSERSPROFILE%",
                    System.getenv("ALLUSERSPROFILE")),
                "libopenh264.dll");
        }
        else if (OSUtils.IS_MAC)
        {
            destFile = new File(
                OPENH264_INSTALL_DIR_MAC, "libopenh264.4.dylib");
        }

        if (destFile == null)
        {
            logger.warn("Fail to install openh264");
            return;
        }

        // create parent folders if they do not exist
        if (!destFile.getParentFile().exists())
        {
            destFile.getParentFile().mkdirs();
        }

        try
        {
            // extract
            BZip2CompressorInputStream bzIn
                = new BZip2CompressorInputStream(new FileInputStream(file));

            // create
            Files.copy(bzIn, destFile.toPath(),
                StandardCopyOption.REPLACE_EXISTING);

            // save the current version as installed
            NeomediaActivator.getConfigurationService().setProperty(
                OPENH264_INSTALLED_VERSION_PROP,
                OPENH264_CURRENT_VERSION
            );
        }
        catch (IOException e)
        {
            logger.error("Failed to install OpenH264 file", e);
        }
    }

    /**
     * Removes installed file, used for disabling the codec.
     */
    private static void removeFile()
    {
        if (SystemUtils.IS_OS_WINDOWS)
        {
            new File(
                OPENH264_INSTALL_DIR_WINDOWS.replace(
                    "%ALLUSERSPROFILE%",
                    System.getenv("ALLUSERSPROFILE")),
                "libopenh264.dll").delete();
        }
        else if (SystemUtils.IS_OS_MAC)
        {
            var installDir = new File(OPENH264_INSTALL_DIR_MAC);
            if (installDir.exists() && installDir.isDirectory())
            {
                for (var f : installDir.listFiles((dir, name) -> name.startsWith("libopenh264")))
                {
                    f.delete();
                }
            }
        }
    }

    /**
     * Chooses correct download URL based on the current running OS.
     * @return the download URL as string.
     */
    private static String chooseOpenH264URL()
    {
        if (SystemUtils.IS_OS_MAC && "aarch64".equalsIgnoreCase(SystemUtils.OS_ARCH))
        {
            return OPENH264_CURRENT_VERSION_URL_MAC_ARM;
        }
        else if (SystemUtils.IS_OS_MAC)
        {
            return OPENH264_CURRENT_VERSION_URL_MAC_64;
        }
        else if (SystemUtils.IS_OS_WINDOWS && "x86".equalsIgnoreCase(SystemUtils.OS_ARCH))
        {
            return OPENH264_CURRENT_VERSION_URL_WINDOWS_32;
        }
        else if (SystemUtils.IS_OS_WINDOWS && "amd64".equalsIgnoreCase(SystemUtils.OS_ARCH))
        {
            return OPENH264_CURRENT_VERSION_URL_WINDOWS_64;
        }
        else
        {
            return null;
        }
    }
}
