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
package net.java.sip.communicator.plugin.branding;

import java.awt.*;
import java.awt.event.*;
import java.io.*;

import javax.imageio.*;
import javax.swing.*;
import javax.swing.event.*;

import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.plugin.desktoputil.plaf.*;
import net.java.sip.communicator.service.browserlauncher.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.util.Logger;
import net.java.sip.communicator.util.skin.*;

import org.jitsi.service.resources.*;
import org.jitsi.util.*;
import org.osgi.framework.*;

/**
 * The <tt>AboutWindow</tt> is containing information about the application
 * name, version, license etc..
 *
 * @author Yana Stamcheva
 * @author Adam Netocny
 * @author Lyubomir Marinov
 */
public class AboutWindow
    extends JDialog
    implements  HyperlinkListener,
                ActionListener,
                ExportedWindow,
                Skinnable
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * The global/shared <code>AboutWindow</code> currently showing.
     */
    private static AboutWindow aboutWindow;

    /**
     * Class id key used in UIDefaults for the version label.
     */
    private static final String uiClassID =
        AboutWindow.class.getName() +  "$VersionTextFieldUI";

    /**
     * Adds the ui class to UIDefaults.
     */
    static
    {
        UIManager.getDefaults().put(uiClassID,
            SIPCommTextFieldUI.class.getName());
    }

    private final JTextField versionLabel;

    /**
     * Shows a <code>AboutWindow</code> creating it first if necessary. The
     * shown instance is shared in order to prevent displaying multiple
     * instances of one and the same <code>AboutWindow</code>.
     */
    public static void showAboutWindow()
    {
        if (aboutWindow == null)
        {
            aboutWindow = new AboutWindow(null);

            /*
             * When the global/shared AboutWindow closes, don't keep a reference
             * to it and let it be garbage-collected.
             */
            aboutWindow.addWindowListener(new WindowAdapter()
            {
                @Override
                public void windowClosed(WindowEvent e)
                {
                    if (aboutWindow == e.getWindow())
                        aboutWindow = null;
                }
            });
        }
        aboutWindow.setVisible(true);
    }

    private static final int DEFAULT_TEXT_INDENT
        = BrandingActivator.getResources()
            .getSettingsInt("plugin.branding.ABOUT_TEXT_INDENT");

    /**
     * Creates an <tt>AboutWindow</tt> by specifying the parent frame owner.
     * @param owner the parent owner
     */
    public AboutWindow(Frame owner)
    {
        super(owner);

        ResourceManagementService resources = BrandingActivator.getResources();

        String applicationName
            = resources.getSettingsString("service.gui.APPLICATION_NAME");

        this.setTitle(
            resources.getI18NString("plugin.branding.ABOUT_WINDOW_TITLE",
                new String[]{applicationName}));

        setModal(false);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setResizable(false);

        JPanel mainPanel = new WindowBackground();
        mainPanel.setLayout(new BorderLayout());

        JPanel textPanel = new JPanel();
        textPanel.setPreferredSize(new Dimension(470, 280));
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.setBorder(BorderFactory
                .createEmptyBorder(15, 15, 15, 15));
        textPanel.setOpaque(false);

        JLabel titleLabel = null;
        if (isApplicationNameShown())
        {
            titleLabel = new JLabel(applicationName);
            titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 28));
            titleLabel.setForeground(Constants.TITLE_COLOR);
            titleLabel.setAlignmentX(Component.RIGHT_ALIGNMENT);
        }

        // Force the use of the custom text field UI in order to fix an
        // incorrect rendering on Ubuntu.
        versionLabel
            = new JTextField(" "
                    + System.getProperty("sip-communicator.version"))
        {
            /**
             * Returns the name of the L&F class that renders this component.
             *
             * @return the string "TreeUI"
             * @see JComponent#getUIClassID
             * @see UIDefaults#getUI
             */
            @Override
            public String getUIClassID()
            {
                return uiClassID;
            }
        };

        versionLabel.setBorder(null);
        versionLabel.setOpaque(false);
        versionLabel.setEditable(false);
        versionLabel.setFont(versionLabel.getFont().deriveFont(Font.BOLD, 18));
        versionLabel.setForeground(Constants.TITLE_COLOR);
        versionLabel.setAlignmentX(Component.RIGHT_ALIGNMENT);
        versionLabel.setHorizontalAlignment(JTextField.RIGHT);

        int logoAreaFontSize
            = resources.getSettingsInt("plugin.branding.ABOUT_LOGO_FONT_SIZE");

        // FIXME: the message exceeds the window length
        JTextArea logoArea =
            new JTextArea(resources.getI18NString(
                "plugin.branding.LOGO_MESSAGE"));
        logoArea.setFont(
            logoArea.getFont().deriveFont(Font.BOLD, logoAreaFontSize));
        logoArea.setForeground(Constants.TITLE_COLOR);
        logoArea.setOpaque(false);
        logoArea.setLineWrap(true);
        logoArea.setWrapStyleWord(true);
        logoArea.setEditable(false);
        logoArea.setPreferredSize(new Dimension(100, 20));
        logoArea.setAlignmentX(Component.RIGHT_ALIGNMENT);
        logoArea.setBorder(BorderFactory
            .createEmptyBorder(30, DEFAULT_TEXT_INDENT, 0, 0));

        StyledHTMLEditorPane rightsArea = new StyledHTMLEditorPane();
        rightsArea.setContentType("text/html");

        rightsArea.appendToEnd(resources.getI18NString(
            "plugin.branding.COPYRIGHT",
            new String[]
            { Constants.TEXT_COLOR }));

        rightsArea.setPreferredSize(new Dimension(50, 20));
        rightsArea
                .setBorder(BorderFactory
                    .createEmptyBorder(0, DEFAULT_TEXT_INDENT, 0, 0));
        rightsArea.setOpaque(false);
        rightsArea.setEditable(false);
        rightsArea.setAlignmentX(Component.RIGHT_ALIGNMENT);
        rightsArea.addHyperlinkListener(this);

        StyledHTMLEditorPane licenseArea = new StyledHTMLEditorPane();
        licenseArea.setContentType("text/html");
        licenseArea.appendToEnd(resources.
            getI18NString("plugin.branding.LICENSE",
            new String[]{Constants.TEXT_COLOR}));

        licenseArea.setPreferredSize(new Dimension(50, 20));
        licenseArea.setBorder(
            BorderFactory.createEmptyBorder(
                resources.getSettingsInt("plugin.branding.ABOUT_PARAGRAPH_GAP"),
                DEFAULT_TEXT_INDENT,
                0, 0));
        licenseArea.setOpaque(false);
        licenseArea.setEditable(false);
        licenseArea.setAlignmentX(Component.RIGHT_ALIGNMENT);
        licenseArea.addHyperlinkListener(this);

        if (titleLabel != null)
            textPanel.add(titleLabel);

        textPanel.add(versionLabel);
        textPanel.add(logoArea);
        textPanel.add(rightsArea);
        textPanel.add(licenseArea);

        JButton okButton
            = new JButton(resources.getI18NString("service.gui.OK"));

        this.getRootPane().setDefaultButton(okButton);

        okButton.setMnemonic(resources.getI18nMnemonic("service.gui.OK"));
        okButton.addActionListener(this);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(okButton);
        buttonPanel.setOpaque(false);

        mainPanel.add(textPanel, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        this.getContentPane().add(mainPanel);

        this.pack();

        setLocationRelativeTo(getParent());

        this.getRootPane().getActionMap().put("close", new CloseAction());

        InputMap imap = this.getRootPane().getInputMap(
                JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "close");

        if(OSUtils.IS_MAC)
        {
            imap.put(
                KeyStroke.getKeyStroke(KeyEvent.VK_W, InputEvent.META_DOWN_MASK),
                "close");
            imap.put(
                KeyStroke.getKeyStroke(KeyEvent.VK_W, InputEvent.CTRL_DOWN_MASK),
                "close");
        }

        WindowUtils.addWindow(this);
    }

    /**
     * Reloads text field UI.
     */
    public void loadSkin()
    {
        if(versionLabel.getUI() instanceof Skinnable)
            ((Skinnable)versionLabel.getUI()).loadSkin();
    }

    /**
     * Constructs the window background in order to have a background image.
     */
    private static class WindowBackground
        extends JPanel
        implements Skinnable
    {
        /**
         * Serial version UID.
         */
        private static final long serialVersionUID = 0L;

        private static final Logger logger
            = Logger.getLogger(WindowBackground.class);

        private Image bgImage = null;

        public WindowBackground()
        {
            loadSkin();
        }

        /**
         * Reloads resources for this component.
         */
        public void loadSkin()
        {
            try
            {
                bgImage = ImageIO.read(BrandingActivator.getResources().
                    getImageURL("plugin.branding.ABOUT_WINDOW_BACKGROUND"));

                this.setPreferredSize(new Dimension(bgImage.getWidth(this),
                    bgImage.getHeight(this)));
            }
            catch (IOException e)
            {
                logger.error("Error cannot obtain background image", e);
                bgImage = null;
            }
        }

        @Override
        protected void paintComponent(Graphics g)
        {
            super.paintComponent(g);

            g = g.create();
            try
            {
                AntialiasingManager.activateAntialiasing(g);

                int bgImageWidth = bgImage.getWidth(null);
                int bgImageHeight = bgImage.getHeight(null);
                boolean bgImageHasBeenDrawn = false;

                if ((bgImageWidth != -1) && (bgImageHeight != -1))
                {
                    int width = getWidth();
                    int height = getHeight();

                    if ((bgImageWidth < width) || (bgImageHeight < height))
                    {
                        g.drawImage(bgImage, 0, 0, width, height, null);
                        bgImageHasBeenDrawn = true;
                    }
                }

                if (!bgImageHasBeenDrawn)
                    g.drawImage(bgImage, 0, 0, null);
            }
            finally
            {
                g.dispose();
            }
        }
    }

    /**
     * Opens a browser when the link has been activated (clicked).
     * @param e the <tt>HyperlinkEvent</tt> that notified us
     */
    public void hyperlinkUpdate(HyperlinkEvent e)
    {
        if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED)
        {
            String href = e.getDescription();
            ServiceReference serviceReference = BrandingActivator
                    .getBundleContext().getServiceReference(
                            BrowserLauncherService.class.getName());

            if (serviceReference != null)
            {
                BrowserLauncherService browserLauncherService
                    = (BrowserLauncherService) BrandingActivator
                        .getBundleContext().getService(serviceReference);

                browserLauncherService.openURL(href);
            }
        }
    }

    /**
     * Indicates that the ok button has been pressed. Closes the window.
     * @param e the <tt>ActionEvent</tt> that notified us
     */
    public void actionPerformed(ActionEvent e)
    {
        setVisible(false);
        dispose();
    }

    /**
     * Implements the <tt>ExportedWindow.getIdentifier()</tt> method.
     * @return the identifier of this exported window
     */
    public WindowID getIdentifier()
    {
        return ExportedWindow.ABOUT_WINDOW;
    }

    /**
     * This dialog could not be minimized.
     */
    public void minimize()
    {
    }

    /**
     * This dialog could not be maximized.
     */
    public void maximize()
    {
    }

    /**
     * Implements the <tt>ExportedWindow.bringToFront()</tt> method. Brings
     * this window to front.
     */
    public void bringToFront()
    {
        this.toFront();
    }

    /**
     * The source of the window
     * @return the source of the window
     */
    public Object getSource()
    {
        return this;
    }

    /**
     * Implementation of {@link ExportedWindow#setParams(Object[])}.
     */
    public void setParams(Object[] windowParams) {}

    /**
     * The action invoked when user presses Escape key.
     */
    private class CloseAction extends UIAction
    {
        /**
         * Serial version UID.
         */
        private static final long serialVersionUID = 0L;

        public void actionPerformed(ActionEvent e)
        {
            setVisible(false);
            dispose();
        }
    }

    /**
     * Indicates if the application name should be shown.
     *
     * @return <tt>true</tt> if the application name should be shown,
     * <tt>false</tt> - otherwise
     */
    private boolean isApplicationNameShown()
    {
        String showApplicationNameProp
            = BrandingActivator.getResources().getSettingsString(
                "plugin.branding.IS_APPLICATION_NAME_SHOWN");

        if (showApplicationNameProp != null
            && showApplicationNameProp.length() > 0)
        {
            return Boolean.parseBoolean(showApplicationNameProp);
        }

        return true;
    }
}
