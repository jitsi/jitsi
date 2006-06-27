/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main.customcontrols;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.java.sip.communicator.impl.gui.main.i18n.Messages;
import net.java.sip.communicator.impl.gui.utils.Constants;
import net.java.sip.communicator.impl.gui.utils.ImageLoader;

/**
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

    public MessageDialog(Frame owner) {
        super(owner);

        this.setLocationRelativeTo(owner);

        this.setTitle(Messages.getString("removeContact"));

        this.setSize(Constants.OPTION_PANE_WIDTH, Constants.OPTION_PANE_HEIGHT);

        this.getContentPane().setLayout(new BorderLayout(5, 5));

        this.messagePanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 0,
                10));
        this.checkBoxPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 10,
                10));

        this.init();
    }
    
    public MessageDialog(Frame owner, String message) {
        this(owner);
        
        this.messageLabel.setText(message);
    }

    public MessageDialog(Frame owner, String message, 
            String okButtonName) {
        this(owner, message);
        
        this.okButton.setText(okButtonName);
    }
    
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

    public void setMessage(String message) {
        this.messageLabel.setText(message);
    }
    
    public int showDialog() {
        this.setModal(true);
        this.setVisible(true);
        
        return returnCode;
    }
    
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
