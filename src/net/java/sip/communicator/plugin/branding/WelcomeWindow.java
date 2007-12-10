package net.java.sip.communicator.plugin.branding;

import java.awt.*;
import java.awt.event.*;
import java.io.*;

import javax.imageio.*;
import javax.swing.*;

public class WelcomeWindow extends JDialog
{
    private WindowBackground mainPanel = new WindowBackground();

    private JLabel titleLabel
        = new JLabel(BrandingResources.getString("productName"));

    private JLabel versionLabel = new JLabel(" "
            + System.getProperty("sip-communicator.version"));

    private JTextArea logoArea = new JTextArea(Resources
            .getString("logoMessage"));

    private StyledHTMLEditorPane rightsArea = new StyledHTMLEditorPane();

    private StyledHTMLEditorPane licenseArea = new StyledHTMLEditorPane();

    private JPanel textPanel = new JPanel();

    private static final Color DARK_BLUE = new Color(23, 65, 125);

    private JPanel loadingPanel = new JPanel(new BorderLayout());

    private JLabel loadingLabel = new JLabel(
        Resources.getString("loading") + ": ");

    private JLabel bundleLabel = new JLabel();

    public WelcomeWindow()
    {
        this.setModal(false);
        this.setUndecorated(true);

        this.mainPanel.setLayout(new BorderLayout());

        this.textPanel.setPreferredSize(new Dimension(470, 280));
        this.textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        this.textPanel
                .setBorder(BorderFactory.createEmptyBorder(15, 15, 0, 15));
        this.textPanel.setOpaque(false);

        this.titleLabel.setFont(Constants.FONT.deriveFont(Font.BOLD, 28));
        this.titleLabel.setForeground(DARK_BLUE);
        this.titleLabel.setAlignmentX(Component.RIGHT_ALIGNMENT);

        this.versionLabel.setFont(Constants.FONT.deriveFont(Font.BOLD, 18));
        this.versionLabel.setForeground(Color.GRAY);
        this.versionLabel.setAlignmentX(Component.RIGHT_ALIGNMENT);

        this.logoArea.setFont(Constants.FONT.deriveFont(Font.BOLD, 12));
        this.logoArea.setForeground(DARK_BLUE);
        this.logoArea.setOpaque(false);
        this.logoArea.setLineWrap(true);
        this.logoArea.setWrapStyleWord(true);
        this.logoArea.setEditable(false);
        this.logoArea.setPreferredSize(new Dimension(100, 20));
        this.logoArea.setAlignmentX(Component.RIGHT_ALIGNMENT);
        this.logoArea.setBorder(BorderFactory.createEmptyBorder(20, 180, 0, 0));

        this.rightsArea.setContentType("text/html");
        this.rightsArea.appendToEnd(Resources.getString("welcomeMessage",
            new String[]{   BrandingResources.getString("productName"),
                            BrandingResources.getString("productWebSite")}));

        this.rightsArea.setPreferredSize(new Dimension(50, 50));
        this.rightsArea
                .setBorder(BorderFactory.createEmptyBorder(0, 180, 0, 0));
        this.rightsArea.setOpaque(false);
        this.rightsArea.setEditable(false);
        this.rightsArea.setAlignmentX(Component.RIGHT_ALIGNMENT);

        this.licenseArea.setContentType("text/html");
        this.licenseArea.appendToEnd(Resources.getString("license", new String[]
        {
            "<a href=http://sip-communicator.org>"
                    + "http://sip-communicator.org</a>"
        }));

        this.licenseArea.setPreferredSize(new Dimension(50, 20));
        this.licenseArea.setBorder(BorderFactory
                .createEmptyBorder(0, 180, 0, 0));
        this.licenseArea.setOpaque(false);
        this.licenseArea.setEditable(false);
        this.licenseArea.setAlignmentX(Component.RIGHT_ALIGNMENT);

        this.bundleLabel.setFont(loadingLabel.getFont().deriveFont(Font.PLAIN));
        this.loadingPanel.setOpaque(false);
        this.loadingPanel.add(loadingLabel, BorderLayout.WEST);
        this.loadingPanel.add(bundleLabel, BorderLayout.CENTER);
        this.loadingPanel.setAlignmentX(Component.RIGHT_ALIGNMENT);
        this.loadingPanel.setBorder(
            BorderFactory.createEmptyBorder(10, 10, 10, 10));

        this.textPanel.add(titleLabel);
        this.textPanel.add(versionLabel);
        this.textPanel.add(logoArea);
        this.textPanel.add(rightsArea);
        this.textPanel.add(licenseArea);

        this.mainPanel.add(textPanel, BorderLayout.CENTER);
        this.mainPanel.add(loadingPanel, BorderLayout.SOUTH);

        this.getContentPane().add(mainPanel);

        this.setResizable(false);

        this.mainPanel.setPreferredSize(new Dimension(570, 330));

        this.setLocation(
            Toolkit.getDefaultToolkit().getScreenSize().width / 2 - 527 / 2,
            Toolkit.getDefaultToolkit().getScreenSize().height / 2 - 305 / 2);

        // Close the splash screen on simple click or Esc.
        this.getGlassPane().addMouseListener(new MouseAdapter()
        {
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

    protected void close()
    {
        this.dispose();
    }

    public void setBundle(String bundleName)
    {
        this.bundleLabel.setText(bundleName);

        this.loadingPanel.revalidate();
        this.loadingPanel.repaint();
    }

    /**
     * The action invoked when user presses Escape key.
     */
    private class CloseAction extends AbstractAction
    {
        public void actionPerformed(ActionEvent e)
        {
            WelcomeWindow.this.close();
        }
    }

    /**
     * Constructs the window background in order to have a background image.
     */
    private class WindowBackground
        extends JPanel
    {
        private Image bgImage;

        public WindowBackground()
        {
            this.setOpaque(true);
            
            try
            {
                bgImage = ImageIO.read(WindowBackground.class.getClassLoader()
                    .getResource(BrandingResources.getString("splashScreenBg")));
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }

            this.setPreferredSize(new Dimension(bgImage.getWidth(this), bgImage
                .getHeight(this)));
        }

        protected void paintComponent(Graphics g)
        {
            super.paintComponent(g);

            Graphics2D g2 = (Graphics2D) g;

            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

            g2.drawImage(bgImage, 0, 0, null);

            g2.setColor(new Color(255, 255, 255, 170));

            g2.fillRect(0, 0, getWidth(), getHeight());
            
            g2.setColor(new Color(150, 150, 150));
            
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 5, 5);
        }
    }
}
