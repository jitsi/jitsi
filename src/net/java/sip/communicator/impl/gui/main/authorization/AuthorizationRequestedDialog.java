/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.authorization;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;

import net.java.sip.communicator.impl.gui.lookandfeel.*;
import net.java.sip.communicator.impl.gui.main.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.skin.*;
import net.java.sip.communicator.util.swing.*;

/**
 * @author Yana Stamcheva
 * @author Adam Netocny
 */
public class AuthorizationRequestedDialog
    extends SIPCommDialog
    implements  ActionListener,
                Skinnable
{
    public static final int ACCEPT_CODE = 0;

    public static final int REJECT_CODE = 1;

    public static final int IGNORE_CODE = 2;

    public static final int ERROR_CODE = -1;

    private JTextArea infoTextArea = new JTextArea();

    private JEditorPane requestPane = new JEditorPane();

    private JPanel buttonsPanel =
        new TransparentPanel(new FlowLayout(FlowLayout.RIGHT));

    private JPanel northPanel = new TransparentPanel(new BorderLayout(10, 0));

    private JPanel titlePanel = new TransparentPanel(new GridLayout(0, 1));

    private JLabel titleLabel = new JLabel();

    private JLabel iconLabel = new JLabel(new ImageIcon(
            ImageLoader.getImage(ImageLoader.AUTHORIZATION_ICON)));

    private JButton acceptButton = new JButton(
        GuiActivator.getResources().getI18NString("service.gui.ACCEPT"));

    private JButton rejectButton = new JButton(
        GuiActivator.getResources().getI18NString("service.gui.REJECT"));

    private JButton ignoreButton = new JButton(
        GuiActivator.getResources().getI18NString("service.gui.IGNORE"));

    private JScrollPane requestScrollPane = new JScrollPane();

    private JPanel mainPanel = new TransparentPanel(new BorderLayout(10, 10));

    private JPanel reasonsPanel =
        new TransparentPanel(new GridLayout(0, 1, 5, 5));

    private String title
        = GuiActivator.getResources()
            .getI18NString("service.gui.AUTHORIZATION_REQUESTED");

    private Object lock = new Object();

    private int result;

    /**
     * Constructs the <tt>RequestAuthorisationDialog</tt>.
     *
     * @param mainFrame the main application window
     * @param contact The <tt>Contact</tt>, which requires authorisation.
     * @param request The <tt>AuthorizationRequest</tt> that will be sent.
     */
    public AuthorizationRequestedDialog(MainFrame mainFrame, Contact contact,
            AuthorizationRequest request)
    {
        super(mainFrame);

        this.setModal(false);

        this.setTitle(title);

        titleLabel.setHorizontalAlignment(JLabel.CENTER);
        titleLabel.setText(title);

        Font font = titleLabel.getFont();
        titleLabel.setFont(font.deriveFont(Font.BOLD, font.getSize2D() + 6));

        infoTextArea.setText(
            GuiActivator.getResources().getI18NString(
                "service.gui.AUTHORIZATION_REQUESTED_INFO", 
                new String[]{contact.getDisplayName()}));

        this.infoTextArea.setFont(infoTextArea.getFont().deriveFont(Font.BOLD));
        this.infoTextArea.setLineWrap(true);
        this.infoTextArea.setWrapStyleWord(true);
        this.infoTextArea.setOpaque(false);
        this.infoTextArea.setEditable(false);

        this.titlePanel.add(titleLabel);
        this.titlePanel.add(infoTextArea);

        this.northPanel.add(iconLabel, BorderLayout.WEST);
        this.northPanel.add(titlePanel, BorderLayout.CENTER);

        if(request.getReason() != null && !request.getReason().equals(""))
        {
            this.requestScrollPane.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(3, 3, 3, 3),
                SIPCommBorders.getBoldRoundBorder()));

            this.requestPane.setEditable(false);
            this.requestPane.setOpaque(false);
            this.requestPane.setText(request.getReason());

            this.requestScrollPane.getViewport().add(requestPane);

            this.reasonsPanel.add(requestScrollPane);

            this.mainPanel.setPreferredSize(new Dimension(550, 300));
        }
        else {
            this.mainPanel.setPreferredSize(new Dimension(550, 200));
        }

        this.acceptButton.setName("service.gui.ACCEPT");
        this.rejectButton.setName("reject");
        this.ignoreButton.setName("ignore");

        this.getRootPane().setDefaultButton(acceptButton);
        this.acceptButton.addActionListener(this);
        this.rejectButton.addActionListener(this);
        this.ignoreButton.addActionListener(this);

        this.acceptButton.setMnemonic(
            GuiActivator.getResources().getI18nMnemonic("service.gui.ACCEPT"));
        this.rejectButton.setMnemonic(
            GuiActivator.getResources().getI18nMnemonic("service.gui.REJECT"));
        this.ignoreButton.setMnemonic(
            GuiActivator.getResources().getI18nMnemonic("service.gui.IGNORE"));

        this.buttonsPanel.add(acceptButton);
        this.buttonsPanel.add(rejectButton);
        this.buttonsPanel.add(ignoreButton);

        this.mainPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        this.mainPanel.add(northPanel, BorderLayout.NORTH);        
        this.mainPanel.add(reasonsPanel, BorderLayout.CENTER);
        this.mainPanel.add(buttonsPanel, BorderLayout.SOUTH);

        this.getContentPane().add(mainPanel);
    }

    /**
     * Shows this modal dialog.
     * @return the result code, which shows what was the choice of the user
     */
    public int showDialog()
    {
        this.setVisible(true);

        synchronized (lock)
        {
            try
            {
                lock.wait();
            }
            catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        return result;
    }

    /**
     * Handles the <tt>ActionEvent</tt> triggered when one user clicks
     * on one of the buttons.
     *
     * @param e the <tt>ActionEvent</tt> that notified us
     */
    public void actionPerformed(ActionEvent e)
    {
        JButton button = (JButton)e.getSource();
        String name = button.getName();

        if (name.equals("service.gui.ACCEPT"))
        {
            this.result = ACCEPT_CODE;
        }
        else if (name.equals("reject"))
        {
            this.result = REJECT_CODE;
        }
        else if (name.equals("ignore"))
        {
            this.result = IGNORE_CODE;
        }
        else
        {
            this.result = ERROR_CODE;
        }

        synchronized (lock)
        {
            lock.notify();
        }

        this.dispose();
    }

    /**
     * Invoked when the window is closed.
     *
     * @param isEscaped indicates if the window was closed by pressing the Esc
     * key
     */
    protected void close(boolean isEscaped)
    {
        this.ignoreButton.doClick();
    }

    /**
     * Reloads athorization icon.
     */
    public void loadSkin()
    {
        iconLabel.setIcon(new ImageIcon(
            ImageLoader.getImage(ImageLoader.AUTHORIZATION_ICON)));
    }
}
