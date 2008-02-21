package net.java.sip.communicator.plugin.branding;

import java.awt.*;
import java.awt.event.*;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.event.*;

import net.java.sip.communicator.util.*;
import net.java.sip.communicator.service.browserlauncher.*;
import net.java.sip.communicator.service.gui.*;

import org.osgi.framework.ServiceReference;

public class AboutWindow extends JDialog implements HyperlinkListener,
        ActionListener, ExportedWindow
{
    private Logger logger = Logger.getLogger(AboutWindow.class.getName());
    
    private WindowBackground mainPanel = new WindowBackground();

    private JLabel titleLabel = new JLabel(
        BrandingResources.getApplicationString("applicationName"));

    private JLabel versionLabel = new JLabel(" "
            + System.getProperty("sip-communicator.version"));

    private JTextArea logoArea = new JTextArea(
            Resources.getString("logoMessage"));

    private StyledHTMLEditorPane rightsArea = new StyledHTMLEditorPane();

    private StyledHTMLEditorPane licenseArea = new StyledHTMLEditorPane();

    private JButton okButton = new JButton(Resources.getString("ok"));

    private JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

    private JPanel textPanel = new JPanel();

    public AboutWindow(Frame owner)
    {
        super(owner);

        this.setTitle(  Resources.getString("aboutWindowTitle",
                        new String[]{BrandingResources
                                        .getApplicationString("applicationName")}));

        this.setModal(false);

        this.mainPanel.setLayout(new BorderLayout());

        this.textPanel.setPreferredSize(new Dimension(470, 280));
        this.textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        this.textPanel.setBorder(BorderFactory
                .createEmptyBorder(15, 15, 15, 15));
        this.textPanel.setOpaque(false);

        this.titleLabel.setFont(Constants.FONT.deriveFont(Font.BOLD, 28));
        this.titleLabel.setForeground(Constants.TITLE_COLOR);
        this.titleLabel.setAlignmentX(Component.RIGHT_ALIGNMENT);

        this.versionLabel.setFont(Constants.FONT.deriveFont(Font.BOLD, 18));
        this.versionLabel.setForeground(Constants.TITLE_COLOR);
        this.versionLabel.setAlignmentX(Component.RIGHT_ALIGNMENT);

        int logoAreaFontSize = new Integer(
            BrandingResources.getApplicationString("aboutLogoFontSize")).intValue();

        this.logoArea.setFont(
            Constants.FONT.deriveFont(Font.BOLD, logoAreaFontSize));

        this.logoArea.setForeground(Constants.TITLE_COLOR);
        this.logoArea.setOpaque(false);
        this.logoArea.setLineWrap(true);
        this.logoArea.setWrapStyleWord(true);
        this.logoArea.setEditable(false);
        this.logoArea.setPreferredSize(new Dimension(100, 20));
        this.logoArea.setAlignmentX(Component.RIGHT_ALIGNMENT);
        this.logoArea.setBorder(BorderFactory.createEmptyBorder(30, 180, 0, 0));

        this.rightsArea.setContentType("text/html");

        this.rightsArea.appendToEnd(Resources.getString("copyright",
            new String[]{Constants.TEXT_COLOR}));

        this.rightsArea.setPreferredSize(new Dimension(50, 20));
        this.rightsArea
                .setBorder(BorderFactory.createEmptyBorder(0, 180, 0, 0));
        this.rightsArea.setOpaque(false);
        this.rightsArea.setEditable(false);
        this.rightsArea.setAlignmentX(Component.RIGHT_ALIGNMENT);
        this.rightsArea.addHyperlinkListener(this);

        this.licenseArea.setContentType("text/html");
        this.licenseArea.appendToEnd(Resources.getString("license",
            new String[]{Constants.TEXT_COLOR}));

        this.licenseArea.setPreferredSize(new Dimension(50, 20));
        this.licenseArea.setBorder(
            BorderFactory.createEmptyBorder(10, 180, 0, 0));
        this.licenseArea.setOpaque(false);
        this.licenseArea.setEditable(false);
        this.licenseArea.setAlignmentX(Component.RIGHT_ALIGNMENT);
        this.licenseArea.addHyperlinkListener(this);

        this.textPanel.add(titleLabel);
        this.textPanel.add(versionLabel);
        this.textPanel.add(logoArea);
        this.textPanel.add(rightsArea);
        this.textPanel.add(licenseArea);

        this.getRootPane().setDefaultButton(okButton);

        this.okButton.setMnemonic(Resources.getString("ok").charAt(0));

        this.okButton.addActionListener(this);

        this.buttonPanel.add(okButton);
        this.buttonPanel.setOpaque(false);

        this.mainPanel.add(textPanel, BorderLayout.CENTER);
        this.mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        this.getContentPane().add(mainPanel);
        this.setSize(mainPanel.getPreferredSize());

        this.setResizable(false);
        this.setLocation(Toolkit.getDefaultToolkit().getScreenSize().width / 2
                - getWidth() / 2,
                Toolkit.getDefaultToolkit().getScreenSize().height / 2
                        - getHeight() / 2);

    }

    protected void close(boolean isEscaped)
    {
        this.dispose();
    }

    /**
     * Constructs the window background in order to have a background image.
     */
    private class WindowBackground extends JPanel
    {
        private Image bgImage = null;

        public WindowBackground()
        {
            try
            {
                bgImage = ImageIO.read(WindowBackground.class.getClassLoader()
                        .getResource(BrandingResources
                            .getResourceString("aboutWindowBg")));
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
        this.dispose();
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
}
