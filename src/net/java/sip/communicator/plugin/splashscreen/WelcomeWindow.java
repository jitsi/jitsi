package net.java.sip.communicator.plugin.splashscreen;

import java.awt.*;

import javax.swing.*;

public class WelcomeWindow
        extends JDialog
{
    private WindowBackground mainPanel
        = new WindowBackground();

    private JLabel titleLabel = new JLabel(
            "SIP Communicator");

    private JLabel versionLabel = new JLabel(
            " " + System.getProperty("sip-communicator.version"));

    private JTextArea logoArea
        = new JTextArea("Open Source VoIP & Instant Messaging");

    private JEditorPane rightsArea = new JEditorPane();

    private JEditorPane licenseArea = new JEditorPane();

    private JPanel textPanel = new JPanel();

    private static final Color DARK_BLUE = new Color(23, 65, 125);

    private static final String FONT_NAME = "Verdana";

    private static final String FONT_SIZE = "12";

    private static final Font FONT = new Font(FONT_NAME, Font.PLAIN,
            new Integer(FONT_SIZE).intValue());

    private JPanel loadingPanel = new JPanel(new BorderLayout());

    private JLabel loadingLabel = new JLabel("Loading: ");

    private JLabel bundleLabel = new JLabel();

    public WelcomeWindow()
    {
        this.setTitle("SIP Communicator");
        this.setModal(false);
        this.setUndecorated(true);

        this.mainPanel.setLayout(new BorderLayout());

        this.textPanel.setPreferredSize(new Dimension(470, 240));
        this.textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        this.textPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 0, 15));
        this.textPanel.setOpaque(false);

        this.titleLabel.setFont(FONT.deriveFont(Font.BOLD, 28));
        this.titleLabel.setForeground(DARK_BLUE);
        this.titleLabel.setAlignmentX(Component.RIGHT_ALIGNMENT);

        this.versionLabel.setFont(FONT.deriveFont(Font.BOLD, 18));
        this.versionLabel.setForeground(Color.GRAY);
        this.versionLabel.setAlignmentX(Component.RIGHT_ALIGNMENT);

        this.logoArea.setFont(FONT.deriveFont(Font.BOLD, 12));
        this.logoArea.setForeground(DARK_BLUE);
        this.logoArea.setOpaque(false);
        this.logoArea.setLineWrap(true);
        this.logoArea.setWrapStyleWord(true);
        this.logoArea.setEditable(false);
        this.logoArea.setPreferredSize(new Dimension(100, 20));
        this.logoArea.setAlignmentX(Component.RIGHT_ALIGNMENT);
        this.logoArea.setBorder(BorderFactory.createEmptyBorder(20, 180, 0, 0));

        this.rightsArea.setContentType("text/html");
        this.rightsArea.setText(
                "<html><font size=3>(c)2003-2006 Copyright <b>sip-communicator.org</b>."
                + " All rights reserved. Visit "
                + "<a href=\"http://sip-communicator.org\">"
                + "http://sip-communicator.org</a>.</font></html>");

        this.rightsArea.setPreferredSize(new Dimension(50, 10));
        this.rightsArea.setBorder(BorderFactory.createEmptyBorder(0, 180, 0, 0));
        this.rightsArea.setOpaque(false);
        this.rightsArea.setEditable(false);
        this.rightsArea.setAlignmentX(Component.RIGHT_ALIGNMENT);

        this.licenseArea.setContentType("text/html");
        this.licenseArea.setText( "<html><font size=3 style=bold>"
            + "The SIP Communicator is currently under active development."
            + "The version you are running is only experimental and WILL NOT "
            + "work as expected. Please refer to "
            + "<a href=http://sip-communicator.org>http://sip-communicator.org</a>"
            + " for more information.</font></html>");

        this.licenseArea.setPreferredSize(new Dimension(50, 20));
        this.licenseArea.setBorder(BorderFactory.createEmptyBorder(0, 180, 0, 0));
        this.licenseArea.setOpaque(false);
        this.licenseArea.setEditable(false);
        this.licenseArea.setAlignmentX(Component.RIGHT_ALIGNMENT);

        this.bundleLabel.setFont(loadingLabel.getFont().deriveFont(Font.PLAIN));
        this.loadingPanel.setOpaque(false);
        this.loadingPanel.add(loadingLabel, BorderLayout.WEST);
        this.loadingPanel.add(bundleLabel, BorderLayout.CENTER);
        this.loadingPanel.setAlignmentX(Component.RIGHT_ALIGNMENT);
        this.loadingPanel
            .setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

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
            Toolkit.getDefaultToolkit().getScreenSize().width/2
                - 527/2,
            Toolkit.getDefaultToolkit().getScreenSize().height/2
                - 305/2
            );
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
}
