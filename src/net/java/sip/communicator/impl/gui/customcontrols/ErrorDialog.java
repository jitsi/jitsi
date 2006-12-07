/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.customcontrols;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.i18n.*;
import net.java.sip.communicator.impl.gui.utils.*;

/**
 * The <tt>MessageDialog</tt> is a <tt>JDialog</tt> that contains a question
 * message, two buttons to confirm or cancel the question and a check box that
 * allows user to choose to not be questioned any more over this subject.
 * <p>
 * The message and the name of the "OK" button could be configured.
 * 
 * @author Yana Stamcheva
 */
public class ErrorDialog
    extends SIPCommDialog
    implements ActionListener {

    private JButton okButton = new JButton(
        Messages.getI18NString("ok").getText());

    private JLabel iconLabel = new JLabel(new ImageIcon(ImageLoader
            .getImage(ImageLoader.ERROR_ICON)));

    private SIPCommMsgTextArea messageTextArea = new SIPCommMsgTextArea();

    private JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));

    private JPanel messagePanel = new JPanel(new BorderLayout(5, 5));
    
    public static final int WARNING = 1;
    
    public static final int ERROR = 0;
    
    /**
     * Creates an instance of <tt>MessageDialog</tt> by specifying the
     * owner window.
     * @param owner This dialog owner.
     */
    public ErrorDialog(Frame owner)
    {
        super(owner);
        
        this.setTitle(Messages.getI18NString("removeContact").getText());

        this.setSize(Constants.MSG_DIALOG_WIDTH, Constants.MSG_DIALOG_HEIGHT);

        this.getContentPane().setLayout(new BorderLayout(5, 5));

        this.messagePanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 0,
                10));

        this.init();
    }
    
    /**
     * Creates an instance of <tt>MessageDialog</tt> by specifying the
     * owner window and the message to be displayed.
     * @param owner The dialog owner.
     * @param message The message to be displayed.
     */
    public ErrorDialog(Frame owner, String message)
    {
        this(owner);
        
        this.messageTextArea.setText(message);
    }

    /**
     * Creates an instance of <tt>MessageDialog</tt> by specifying the
     * owner window and the message to be displayed.
     * @param owner The dialog owner.
     * @param message The message to be displayed.
     */
    public ErrorDialog(Frame owner, String message, String title)
    {
        this(owner, message);
        
        this.setTitle(title);
    }
    
    public ErrorDialog(Frame owner, String message, String title, int type)
    {
        this(owner, message, title);
        
        if(type == WARNING)
            iconLabel.setIcon(new ImageIcon(ImageLoader
                .getImage(ImageLoader.WARNING_ICON)));        
    }
    
    /**
     * Initializes this dialog.
     */
    private void init()
    {
        this.getRootPane().setDefaultButton(okButton);

        this.buttonsPanel.add(okButton);

        this.okButton.addActionListener(this);

        this.messagePanel.add(iconLabel, BorderLayout.WEST);
        this.messagePanel.add(messageTextArea, BorderLayout.CENTER);

        this.getContentPane().add(messagePanel, BorderLayout.CENTER);
        this.getContentPane().add(buttonsPanel, BorderLayout.SOUTH);
    }

    /**
     * Sets the message to be displayed.
     * @param message The message to be displayed.
     */
    public void setMessage(String message)
    {
        this.messageTextArea.setText(message);
    }
    
    /**
     * Shows the dialog.
     * @return The return code that should indicate what was the choice of
     * the user. If the user chooses cancel, the return code is the 
     * CANCEL_RETURN_CODE.
     */
    public void showDialog()
    {   
        setVisible(true);        
    }
    
    /**
     * Handles the <tt>ActionEvent</tt>. Depending on the user choice sets
     * the return code to the appropriate value.
     */
    public void actionPerformed(ActionEvent e)
    {                   
        this.dispose();
    }

    protected void close(boolean isEscaped)
    {
        this.okButton.doClick();
    }
}
