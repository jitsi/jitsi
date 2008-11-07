package net.java.sip.communicator.plugin.branding;

import java.awt.*;
import java.awt.event.*;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.event.*;

import net.java.sip.communicator.service.browserlauncher.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.resources.*;
import net.java.sip.communicator.util.*;

import org.osgi.framework.ServiceReference;

public class AboutWindow extends JDialog implements HyperlinkListener,
        ActionListener, ExportedWindow
{
    private static final int DEFAULT_TEXT_INDENT
        = BrandingActivator.getResources()
            .getSettingsInt("aboutWindowTextIndent");

    public AboutWindow(Frame owner)
    {
        super(owner);

        ResourceManagementService resources = BrandingActivator.getResources();

        this.setTitle(
            resources.getI18NString("aboutWindowTitle",
                new String[]{resources.
                    getSettingsString("applicationName")}));

        setModal(false);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JPanel mainPanel = new WindowBackground();
        mainPanel.setLayout(new BorderLayout());

        JPanel textPanel = new JPanel();
        textPanel.setPreferredSize(new Dimension(470, 280));
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.setBorder(BorderFactory
                .createEmptyBorder(15, 15, 15, 15));
        textPanel.setOpaque(false);

        JLabel titleLabel =
            new JLabel(resources.getSettingsString("applicationName"));
        titleLabel.setFont(Constants.FONT.deriveFont(Font.BOLD, 28));
        titleLabel.setForeground(Constants.TITLE_COLOR);
        titleLabel.setAlignmentX(Component.RIGHT_ALIGNMENT);

        JLabel versionLabel =
            new JLabel(" " + System.getProperty("sip-communicator.version"));
        versionLabel.setFont(Constants.FONT.deriveFont(Font.BOLD, 18));
        versionLabel.setForeground(Constants.TITLE_COLOR);
        versionLabel.setAlignmentX(Component.RIGHT_ALIGNMENT);

        int logoAreaFontSize = resources.getSettingsInt("aboutLogoFontSize");

        JTextArea logoArea =
            new JTextArea(resources.getI18NString("logoMessage"));
        logoArea.setFont(
            Constants.FONT.deriveFont(Font.BOLD, logoAreaFontSize));
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

        rightsArea.appendToEnd(resources.getI18NString("copyright",
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
            getI18NString("license",
            new String[]{Constants.TEXT_COLOR}));

        licenseArea.setPreferredSize(new Dimension(50, 20));
        licenseArea.setBorder(
            BorderFactory.createEmptyBorder(
                resources.getSettingsInt("paragraphGap"),
                DEFAULT_TEXT_INDENT,
                0, 0));
        licenseArea.setOpaque(false);
        licenseArea.setEditable(false);
        licenseArea.setAlignmentX(Component.RIGHT_ALIGNMENT);
        licenseArea.addHyperlinkListener(this);

        textPanel.add(titleLabel);
        textPanel.add(versionLabel);
        textPanel.add(logoArea);
        textPanel.add(rightsArea);
        textPanel.add(licenseArea);

        JButton okButton = new JButton(resources.getI18NString("ok"));

        this.getRootPane().setDefaultButton(okButton);

        okButton.setMnemonic(resources.getI18nMnemonic("ok"));
        okButton.addActionListener(this);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(okButton);
        buttonPanel.setOpaque(false);

        mainPanel.add(textPanel, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        this.getContentPane().add(mainPanel);
        this.setSize(mainPanel.getPreferredSize());

        this.setResizable(false);

        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        setLocation(screenSize.width / 2 - getWidth() / 2, screenSize.height
            / 2 - getHeight() / 2);
    }

    /**
     * Constructs the window background in order to have a background image.
     */
    private static class WindowBackground extends JPanel
    {
        private final Logger logger =
            Logger.getLogger(WindowBackground.class.getName());
        
        private Image bgImage = null;

        public WindowBackground()
        {
            try
            {
                bgImage = ImageIO.read(BrandingActivator.getResources().
                    getImageURL("aboutWindowBg"));
            }
            catch (IOException e)
            {
                logger.error("Error cannot obtain background image", e);
                bgImage = null;
            }
            this.setPreferredSize(new Dimension(bgImage.getWidth(this),
                                                bgImage.getHeight(this)));
        }

        protected void paintComponent(Graphics g)
        {
            super.paintComponent(g);

            activateAntialiasing(g);

            Graphics2D g2 = (Graphics2D) g;

            g2.drawImage(bgImage, 0, 0, null);

//            g2.setColor(new Color(255, 255, 255, 100));
//
//            g2.fillRect(0, 0, getWidth(), getHeight());
        }
    }

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

    public void actionPerformed(ActionEvent e)
    {
        setVisible(false);
        dispose();
    }

    /**
     * Implements the <tt>ExportedWindow.getIdentifier()</tt> method.
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

    public static void activateAntialiasing(Graphics g)
    {
        Graphics2D g2d = (Graphics2D) g;

        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
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
}
