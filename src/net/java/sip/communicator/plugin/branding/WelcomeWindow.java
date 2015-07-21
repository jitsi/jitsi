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
import java.awt.image.*;
import java.io.*;

import javax.imageio.*;
import javax.swing.*;

import net.java.sip.communicator.plugin.desktoputil.*;

import org.jitsi.service.resources.*;

/**
 * The <tt>WelcomeWindow</tt> is actually the splash screen shown while the
 * application is loading. It displays the status of the loading process and
 * some general information about the version, licenses and contact details.
 *
 * @author Yana Stamcheva
 */
public class WelcomeWindow extends JDialog
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    private static final String APPLICATION_NAME
        = BrandingActivator.getResources()
            .getSettingsString("service.gui.APPLICATION_NAME");

    private static final int DEFAULT_TEXT_INDENT
        = BrandingActivator.getResources()
            .getSettingsInt("plugin.branding.SPLASH_SCREEN_TEXT_INDENT");

    private static final int PREFERRED_HEIGHT = 330;

    private static final int PREFERRED_WIDTH = 570;

    private final JLabel bundleLabel = new JLabel();

    /**
     * Constructor.
     */
    public WelcomeWindow()
    {
        JLabel titleLabel = new JLabel(APPLICATION_NAME);

        JLabel versionLabel = new JLabel(" "
                + System.getProperty("sip-communicator.version"));

        JTextArea logoArea = new JTextArea(
            BrandingActivator.getResources()
                .getI18NString("plugin.branding.LOGO_MESSAGE"));

        StyledHTMLEditorPane rightsArea = new StyledHTMLEditorPane();

        StyledHTMLEditorPane licenseArea = new StyledHTMLEditorPane();

        JPanel textPanel = new JPanel();

        Container mainPanel = new WindowBackground();

        this.setTitle(APPLICATION_NAME);

        this.setModal(false);
        this.setUndecorated(true);

        mainPanel.setLayout(new BorderLayout());

        textPanel.setPreferredSize(new Dimension(470, 280));
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel
                .setBorder(BorderFactory.createEmptyBorder(15, 15, 0, 15));
        textPanel.setOpaque(false);

        this.initTitleLabel(titleLabel);

        this.initVersionLabel(versionLabel);

        this.initLogoArea(logoArea);

        this.initRightsArea(rightsArea);

        this.initLicenseArea(licenseArea);

        Component loadingPanel = initLoadingPanel();

        textPanel.add(titleLabel);
        textPanel.add(versionLabel);
        textPanel.add(logoArea);
        textPanel.add(rightsArea);
        textPanel.add(licenseArea);

        mainPanel.add(textPanel, BorderLayout.CENTER);
        mainPanel.add(loadingPanel, BorderLayout.SOUTH);

        this.getContentPane().add(mainPanel);

        this.setResizable(false);

        mainPanel.setPreferredSize(
            new Dimension(PREFERRED_WIDTH, PREFERRED_HEIGHT));

        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        this.setLocation(screenSize.width / 2 - 527 / 2,
            screenSize.height / 2 - 305 / 2);

        this.initCloseActions();
    }

    /**
     * Initializes the title label.
     *
     * @param titleLabel the title label
     */
    private void initTitleLabel(JLabel titleLabel)
    {
        titleLabel.setFont(
            titleLabel.getFont().deriveFont(Font.BOLD, 28));
        titleLabel.setForeground(Constants.TITLE_COLOR);
        titleLabel.setAlignmentX(Component.RIGHT_ALIGNMENT);
    }

    /**
     * Initializes the version label.
     *
     * @param versionLabel the version label
     */
    private void initVersionLabel(JLabel versionLabel)
    {
        versionLabel.setFont(
            versionLabel.getFont().deriveFont(Font.BOLD, 18));
        versionLabel.setForeground(Constants.TITLE_COLOR);
        versionLabel.setAlignmentX(Component.RIGHT_ALIGNMENT);
    }

    /**
     * Initializes the logo area.
     *
     * @param logoArea the logo area
     */
    private void initLogoArea(JTextArea logoArea)
    {
        int logoAreaFontSize = BrandingActivator.getResources().
            getSettingsInt("plugin.branding.ABOUT_LOGO_FONT_SIZE");

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
            .createEmptyBorder(20, DEFAULT_TEXT_INDENT, 0, 0));
    }

    /**
     * Initializes the copyright area.
     *
     * @param rightsArea the copyright area.
     */
    private void initRightsArea(StyledHTMLEditorPane rightsArea)
    {
        rightsArea.setContentType("text/html");
        rightsArea.appendToEnd(
            BrandingActivator.getResources().getI18NString(
            "plugin.branding.WELCOME_MESSAGE",
            new String[]{
                Constants.TEXT_COLOR,
                APPLICATION_NAME,
                BrandingActivator.getResources()
                    .getSettingsString("service.gui.APPLICATION_WEB_SITE")
                }));

        rightsArea.setPreferredSize(new Dimension(50, 50));
        rightsArea
                .setBorder(BorderFactory
                    .createEmptyBorder(0, DEFAULT_TEXT_INDENT, 0, 0));
        rightsArea.setOpaque(false);
        rightsArea.setEditable(false);
        rightsArea.setAlignmentX(Component.RIGHT_ALIGNMENT);
    }

    /**
     * Initializes the license area.
     *
     * @param licenseArea the license area.
     */
    private void initLicenseArea(StyledHTMLEditorPane licenseArea)
    {
        licenseArea.setContentType("text/html");
        licenseArea.appendToEnd(
            BrandingActivator.getResources().getI18NString(
            "plugin.branding.LICENSE",
            new String[]
                       {
                            Constants.TEXT_COLOR
                       }));

        licenseArea.setPreferredSize(new Dimension(50, 20));
        licenseArea.setBorder(BorderFactory
                .createEmptyBorder(0, DEFAULT_TEXT_INDENT, 0, 0));
        licenseArea.setOpaque(false);
        licenseArea.setEditable(false);
        licenseArea.setAlignmentX(Component.RIGHT_ALIGNMENT);
    }

    private JPanel initLoadingPanel()
    {
        ResourceManagementService resources = BrandingActivator.getResources();
        JLabel loadingLabel
            = new JLabel(
                    resources.getI18NString("plugin.branding.LOADING") + ": ");
        JPanel loadingPanel = new JPanel(new BorderLayout());

        this.bundleLabel.setFont(loadingLabel.getFont().deriveFont(Font.PLAIN));

        loadingPanel.setOpaque(false);
        loadingPanel.add(loadingLabel, BorderLayout.WEST);
        loadingPanel.add(bundleLabel, BorderLayout.CENTER);

        int loadingPanelBorder
            = resources
                .getSettingsInt("plugin.branding.LOADING_BUNDLE_PANEL_BORDER");

        loadingPanel.setBorder(
            BorderFactory.createEmptyBorder(loadingPanelBorder,
                                            loadingPanelBorder,
                                            loadingPanelBorder,
                                            loadingPanelBorder));

        int loadingPanelHeight
            = resources
                .getSettingsInt("plugin.branding.LOADING_BUNDLE_PANEL_HEIGHT");

        loadingPanel.setPreferredSize(
            new Dimension(PREFERRED_WIDTH, loadingPanelHeight));

        return loadingPanel;
    }

    /**
     * Initializes close actions on mouse click and esc key.
     */
    private void initCloseActions()
    {
        // Close the splash screen on simple click or Esc.
        this.getGlassPane().addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent e)
            {
                WelcomeWindow.this.close();
            }
        });

        this.getGlassPane().setVisible(true);

        ActionMap amap = this.getRootPane().getActionMap();

        amap.put("close", new CloseAction());

        InputMap imap = this.getRootPane().getInputMap(
                JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "close");
    }

    /**
     * Disposes this window.
     */
    protected void close()
    {
        this.dispose();
    }

    /**
     * Sets the name of the currently loading bundle.
     *
     * @param bundleName the name of the bundle to display
     */
    public void setBundle(String bundleName)
    {
        bundleLabel.setText(bundleName);

        bundleLabel.revalidate();
        bundleLabel.getParent().repaint();
    }

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
            WelcomeWindow.this.close();
        }
    }

    /**
     * Constructs the window background in order to have a background image.
     */
    private static class WindowBackground
        extends JPanel
    {
        /**
         * Serial version UID.
         */
        private static final long serialVersionUID = 0L;

        private BufferedImage cache;

        private int cacheHeight;

        private int cacheWidth;

        private final Image image;

        public WindowBackground()
        {
            setOpaque(true);

            Image image = null;
            try
            {
                image =
                    ImageIO.read(BrandingActivator.getResources().getImageURL(
                        "plugin.branding.SPLASH_SCREEN_BACKGROUND"));
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
            this.image = image;

            if (image != null)
            {
                setPreferredSize(new Dimension(image.getWidth(this), image
                    .getHeight(this)));
            }
        }

        @Override
        protected void paintComponent(Graphics g)
        {
            super.paintComponent(g);

            g = g.create();
            try
            {
                internalPaintComponent(g);
            }
            finally
            {
                g.dispose();
            }
        }

        private void internalPaintComponent(Graphics g)
        {
            AntialiasingManager.activateAntialiasing(g);

            Graphics2D g2 = (Graphics2D) g;

            /*
             * Drawing an Image with a data layout and color model compatible
             * with this JPanel is magnitudes faster so create and use such an
             * Image from the original drawn by this instance.
             */
            int width = getWidth();
            int height = getHeight();
            boolean imageIsChanging = false;
            if ((cache == null) || (cacheWidth != width)
                || (cacheHeight != height))
            {
                cache =
                    g2.getDeviceConfiguration().createCompatibleImage(width,
                        height);
                cacheWidth = width;
                cacheHeight = height;

                Graphics2D cacheGraphics = cache.createGraphics();
                try
                {
                    super.paintComponent(cacheGraphics);

                    AntialiasingManager.activateAntialiasing(cacheGraphics);

                    imageIsChanging =
                        !cacheGraphics.drawImage(image, 0, 0, null);

                    cacheGraphics.setColor(new Color(150, 150, 150));
                    cacheGraphics.drawRoundRect(0, 0, width - 1, height - 1, 5,
                        5);
                }
                finally
                {
                    cacheGraphics.dispose();
                }
            }

            g2.drawImage(cache, 0, 0, null);

            /*
             * Once the original Image drawn by this instance has been fully
             * loaded, we're free to use its "compatible" caching representation
             * for the purposes of optimized execution speed.
             */
            if (imageIsChanging)
            {
                cache = null;
            }
        }
    }
}
