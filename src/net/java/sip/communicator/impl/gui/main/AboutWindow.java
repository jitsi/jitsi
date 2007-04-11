package net.java.sip.communicator.impl.gui.main;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.event.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.customcontrols.*;
import net.java.sip.communicator.impl.gui.i18n.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.gui.*;

public class AboutWindow
        extends SIPCommDialog
        implements  HyperlinkListener,
                    ActionListener,
                    ExportedWindow
{
    private WindowBackground mainPanel
        = new WindowBackground();

    private JLabel titleLabel = new JLabel(
            "SIP Communicator");

    private JLabel versionLabel = new JLabel(
            " " + System.getProperty("sip-communicator.version"));

    private JTextArea logoArea
        = new JTextArea("Open Source VoIP & Instant Messaging");

    private StyledHTMLEditorPane rightsArea = new StyledHTMLEditorPane();

    private StyledHTMLEditorPane licenseArea = new StyledHTMLEditorPane();

    private I18NString okString = Messages.getI18NString("ok");
    
    private JButton okButton = new JButton(okString.getText());

    private JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

    private JPanel textPanel = new JPanel();

    public AboutWindow(Frame owner)
    {
        super(owner);

        this.setTitle(Messages.getI18NString("about").getText());
        this.setModal(false);

        this.mainPanel.setLayout(new BorderLayout());

        this.textPanel.setPreferredSize(new Dimension(470, 280));
        this.textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        this.textPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        this.textPanel.setOpaque(false);

        this.titleLabel.setFont(Constants.FONT.deriveFont(Font.BOLD, 28));
        this.titleLabel.setForeground(Constants.DARK_BLUE);
        this.titleLabel.setAlignmentX(Component.RIGHT_ALIGNMENT);

        this.versionLabel.setFont(Constants.FONT.deriveFont(Font.BOLD, 18));
        this.versionLabel.setForeground(Color.GRAY);
        this.versionLabel.setAlignmentX(Component.RIGHT_ALIGNMENT);

        this.logoArea.setFont(Constants.FONT.deriveFont(Font.BOLD, 14));
        this.logoArea.setForeground(Constants.DARK_BLUE);
        this.logoArea.setOpaque(false);
        this.logoArea.setLineWrap(true);
        this.logoArea.setWrapStyleWord(true);
        this.logoArea.setEditable(false);
        this.logoArea.setPreferredSize(new Dimension(100, 20));
        this.logoArea.setAlignmentX(Component.RIGHT_ALIGNMENT);
        this.logoArea.setBorder(BorderFactory.createEmptyBorder(30, 180, 0, 0));

        this.rightsArea.setContentType("text/html");
        
        String startDivTag = "<DIV id=\"message\">";
        String endDivTag = "</DIV>";
        
        this.rightsArea.appendToEnd(startDivTag
                + "(c)2003-2007 Copyright <b>sip-communicator.org</b>."
                + " All rights reserved. Visit "
                + "<a href=\"http://sip-communicator.org\">"
                + "http://sip-communicator.org</a>."
                + endDivTag);

        this.rightsArea.setPreferredSize(new Dimension(50, 20));
        this.rightsArea.setBorder(BorderFactory.createEmptyBorder(0, 180, 0, 0));
        this.rightsArea.setOpaque(false);
        this.rightsArea.setEditable(false);
        this.rightsArea.setAlignmentX(Component.RIGHT_ALIGNMENT);
        this.rightsArea.addHyperlinkListener(this);

        this.licenseArea.setContentType("text/html");
        this.licenseArea.appendToEnd(startDivTag
                + "The <b>SIP Communicator</b> is distributed under the"
                + " terms of the  LGPL "
                + "(<a href=\"http://www.gnu.org\">"
                + "http://www.gnu.org</a>)."
                + endDivTag);

        this.licenseArea.setPreferredSize(new Dimension(50, 20));
        this.licenseArea.setBorder(BorderFactory.createEmptyBorder(10, 180, 0, 0));
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
        
        this.okButton.setMnemonic(okString.getMnemonic());
        
        this.okButton.addActionListener(this);
        
        this.buttonPanel.add(okButton);
        this.buttonPanel.setOpaque(false);

        this.mainPanel.add(textPanel, BorderLayout.CENTER);
        this.mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        this.getContentPane().add(mainPanel);

        this.setResizable(false);
    }

    protected void close(boolean isEscaped)
    {
        this.dispose();
    }

    /**
     * Constructs the window background in order to have a background image.
     */
    private class WindowBackground extends JPanel {

        private Image bgImage
            = ImageLoader.getImage(ImageLoader.ABOUT_WINDOW_BACKGROUND);

        public WindowBackground() {
            this.setPreferredSize(new Dimension(bgImage.getWidth(this), bgImage
                    .getHeight(this)));
        }

        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            AntialiasingManager.activateAntialiasing(g);

            Graphics2D g2 = (Graphics2D) g;

            g2.drawImage(bgImage, 0, 0, null);

            g2.setColor(new Color(255, 255, 255, 100));

            g2.fillRect(0, 0, getWidth(), getHeight());
        }
    }

    public void hyperlinkUpdate(HyperlinkEvent e)
    {
        if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
            String href = e.getDescription();

            GuiActivator.getBrowserLauncher().openURL(href);
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
     * Implements the <tt>ExportedWindow.bringToFront()</tt> method. Brings this
     * window to front.
     */
    public void bringToFront()
    {
        this.toFront();
    }
}
