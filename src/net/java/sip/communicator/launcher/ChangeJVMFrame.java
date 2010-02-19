/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.launcher;

import java.awt.*;

import javax.swing.*;
import javax.swing.event.*;

/**
 * The <tt>ChangeJVMFrame</tt> will ask the user to install the newest java
 * version if she's using an old and incompatible one.
 * 
 * @author Yana Stamcheva
 */
public class ChangeJVMFrame
    extends JFrame
{
    /**
     * The MacOSX operating system.
     */
    public static final String MAC_OSX = "MacOSX";

    /**
     * The Windows operating system.
     */
    public static final String WINDOWS = "Windows";

    /**
     * The Linux operating system.
     */
    public static final String LINUX = "Linux";

    private JTextArea textArea = new JTextArea();

    private JEditorPane javaLinkPane = new JEditorPane();

    private String text = "Sorry. Your Java version is too old. The minimum"
        + " Java version required is 1.5. Please folow the link below to install"
        + " the newest version for your environment.";

    private String macLink
        = "<a href=\"http://www.apple.com/downloads/macosx/apple/application_updates/" +
          "javaformacosx104release9.html\">Download Java 1.5 for MacOSX</a>";

    private String defaultLink
        = "<a href=\"https://cds.sun.com/is-bin/INTERSHOP.enfinity/" +
        "WFS/CDS-CDS_Developer-Site/en_US/-/USD/ViewProductDetail-Start?" +
        "ProductRef=jre-6u18-oth-JPR@CDS-CDS_Developer\">Download Java 1.6</a>";

    private JPanel mainPanel = new JPanel(new BorderLayout());

    public ChangeJVMFrame(String osName)
    {
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        this.setTitle("SIP Communicator requirements");

        this.mainPanel.setPreferredSize(
            new Dimension(450, 150));

        this.mainPanel.setBorder(
            BorderFactory.createEmptyBorder(10, 10, 10, 10));

        this.textArea.setOpaque(false);
        this.textArea.setLineWrap(true);
        this.textArea.setWrapStyleWord(true);
        this.textArea.setText(text);
        this.textArea.setEditable(false);

        this.javaLinkPane.setOpaque(false);
        this.javaLinkPane.setContentType("text/html");
        this.javaLinkPane.setEditable(false);

        if (osName.equals(MAC_OSX))
            this.javaLinkPane.setText(macLink);
        else
            this.javaLinkPane.setText(defaultLink);

        this.javaLinkPane.addHyperlinkListener(new HyperlinkListener()
        {
            public void hyperlinkUpdate(HyperlinkEvent e)
            {
                if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED)
                {
                    new BrowserLauncher().openURL(e.getDescription());
                }
            }
        });

        this.mainPanel.add(textArea, BorderLayout.NORTH);
        this.mainPanel.add(javaLinkPane, BorderLayout.CENTER);

        this.getContentPane().add(mainPanel);

        this.pack();
    }
}
