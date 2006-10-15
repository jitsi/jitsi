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
public class MessageDialog extends JDialog
    implements ActionListener {

    private JButton cancelButton = new JButton(Messages.getString("cancel"));

    private JButton okButton = new JButton(Messages.getString("ok"));

    private JCheckBox doNotAskAgain = new JCheckBox(Messages
            .getString("doNotAskAgain"));

    private JLabel iconLabel = new JLabel(new ImageIcon(ImageLoader
            .getImage(ImageLoader.WARNING_ICON)));

    private JLabel messageLabel = new JLabel();

    private JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));

    private JPanel checkBoxPanel = new JPanel(
            new FlowLayout(FlowLayout.LEADING));

    private JPanel messagePanel = new JPanel(new BorderLayout(5, 5));
    
    private int returnCode;
    
    public static final int OK_RETURN_CODE = 0;
    
    public static final int CANCEL_RETURN_CODE = 1;
    
    public static final int OK_DONT_ASK_CODE = 2;

    /**
     * Creates an instance of <tt>MessageDialog</tt> by specifying the
     * owner window.
     * @param owner This dialog owner.
     */
    public MessageDialog(Frame owner) {
        super(owner);

        this.setLocationRelativeTo(owner);

        this.setTitle(Messages.getString("removeContact"));

        this.setSize(Constants.MSG_DIALOG_WIDTH, Constants.MSG_DIALOG_HEIGHT);

        this.getContentPane().setLayout(new BorderLayout(5, 5));

        this.messagePanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 0,
                10));
        this.checkBoxPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 10,
                10));

        this.init();
    }
    
    /**
     * Creates an instance of <tt>MessageDialog</tt> by specifying the
     * owner window and the message to be displayed.
     * @param owner The dialog owner.
     * @param message The message to be displayed.
     */
    public MessageDialog(Frame owner, String message) {
        this(owner);
        
        this.messageLabel.setText(message);
    }

    /**
     * Creates an instance of <tt>MessageDialog</tt> by specifying the
     * owner window and the message to be displayed.
     * @param owner The dialog owner.
     * @param message The message to be displayed.
     */
    public MessageDialog(Frame owner, String message,
            String okButtonName) {
        this(owner, message);
        
        this.okButton.setText(okButtonName);
    }
    
    /**
     * Initializes this dialog.
     */
    private void init() {
        this.checkBoxPanel.add(doNotAskAgain);

        this.buttonsPanel.add(okButton);
        this.buttonsPanel.add(cancelButton);

        this.okButton.addActionListener(this);
        this.cancelButton.addActionListener(this);
        
        this.messagePanel.add(iconLabel, BorderLayout.WEST);
        this.messagePanel.add(messageLabel, BorderLayout.CENTER);

        this.getContentPane().add(messagePanel, BorderLayout.NORTH);
        this.getContentPane().add(checkBoxPanel, BorderLayout.CENTER);
        this.getContentPane().add(buttonsPanel, BorderLayout.SOUTH);
    }

    /**
     * Sets the message to be displayed.
     * @param message The message to be displayed.
     */
    public void setMessage(String message) {
        this.messageLabel.setText(message);
    }
    
    /**
     * Shows the dialog.
     * @return The return code that should indicate what was the choice of
     * the user. If the user chooses cancel, the return code is the 
     * CANCEL_RETURN_CODE.
     */
    public int showDialog() {
        this.setVisible(true);
        
        return returnCode;
    }
    
    /**
     * Handles the <tt>ActionEvent</tt>. Depending on the user choice sets
     * the return code to the appropriate value.
     */
    public void actionPerformed(ActionEvent e) {
        JButton button = (JButton)e.getSource();
        
        if(button.equals(okButton)) {
            if (doNotAskAgain.isSelected()) {
                this.returnCode = OK_DONT_ASK_CODE;
            }
            else {
                this.returnCode = OK_RETURN_CODE;
            }
        }
        else {
            this.returnCode = CANCEL_RETURN_CODE;
        }
        
        this.dispose();
    }
}
