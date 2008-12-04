/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.call;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.event.*;

import net.java.sip.communicator.impl.gui.customcontrols.*;
import net.java.sip.communicator.impl.gui.i18n.*;
import net.java.sip.communicator.impl.gui.lookandfeel.*;
import net.java.sip.communicator.swing.*;

/**
 * Represents a <code>Dialog</code> which allows specifying the target contact
 * address of a transfer-call operation.
 * 
 * @author Lubomir Marinov
 */
public class TransferCallDialog
    extends SIPCommDialog
{
    private final JButton cancelButton;

    private final JButton okButton;

    /**
     * The target contact address which is the result of this dialog.
     */
    private String target;

    private final JComboBox targetComboBox;

    public TransferCallDialog(Frame owner)
    {
        super(owner);

        setTitle(Messages.getI18NString("transferCallTitle").getText());

        JPanel mainPanel = new JPanel(new BorderLayout(5, 5));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        getContentPane().add(mainPanel);

        JPanel contentPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        mainPanel.add(contentPanel, BorderLayout.NORTH);

        JLabel targetLabel =
            new JLabel(Messages.getI18NString("transferCallTargetLabel")
                .getText());
        contentPanel.add(targetLabel);

        targetComboBox = new SIPCommSmartComboBox();
        targetComboBox.setUI(new SIPCommCallComboBoxUI());
        contentPanel.add(targetComboBox);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.TRAILING));
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        okButton =
            new JButton(Messages.getI18NString("transferCallButton").getText());
        buttonPanel.add(okButton);
        getRootPane().setDefaultButton(okButton);

        cancelButton = new JButton(Messages.getI18NString("cancel").getText());
        buttonPanel.add(cancelButton);

        /*
         * The UI hierarchy has been created and it's now safe to install the
         * listeners to react to its actions.
         */

        /*
         * Enable/disable okButton (i.e. Transfer) in accord with the validity
         * of the target contact address specified in targetComboBox.
         */
        JTextField targetTextField =
            (JTextField) targetComboBox.getEditor().getEditorComponent();
        targetTextField.getDocument().addDocumentListener(
            new DocumentListener()
            {

                /*
                 * (non-Javadoc)
                 * 
                 * @see
                 * javax.swing.event.DocumentListener#changedUpdate(javax.swing
                 * .event.DocumentEvent)
                 */
                public void changedUpdate(DocumentEvent e)
                {
                    TransferCallDialog.this.documentChanged(e);
                }

                /*
                 * (non-Javadoc)
                 * 
                 * @see
                 * javax.swing.event.DocumentListener#insertUpdate(javax.swing
                 * .event.DocumentEvent)
                 */
                public void insertUpdate(DocumentEvent e)
                {
                    TransferCallDialog.this.documentChanged(e);
                }

                /*
                 * (non-Javadoc)
                 * 
                 * @see
                 * javax.swing.event.DocumentListener#removeUpdate(javax.swing
                 * .event.DocumentEvent)
                 */
                public void removeUpdate(DocumentEvent e)
                {
                    TransferCallDialog.this.documentChanged(e);
                }
            });
        documentChanged(null);

        ActionListener actionListener = new ActionListener()
        {

            /**
             * Invoked when an action occurs.
             * 
             * @param evt the <code>ActionEvent</code> instance containing the
             *            data associated with the action and the act of its
             *            performing
             */
            public void actionPerformed(ActionEvent evt)
            {
                TransferCallDialog.this.actionPerformed(this, evt);
            }
        };
        okButton.addActionListener(actionListener);
        cancelButton.addActionListener(actionListener);
    }

    /**
     * Handles actions performed on this dialog on behalf of a specific
     * <code>ActionListener</code>.
     * 
     * @param listener the <code>ActionListener</code> notified about the
     *            performing of the action
     * @param evt the <code>ActionEvent</code> containing the data associated
     *            with the action and the act of its performing
     */
    private void actionPerformed(ActionListener listener, ActionEvent evt)
    {
        Object source = evt.getSource();

        if (okButton.equals(source))
        {
            this.target = getValidTarget();
        }
        else if (cancelButton.equals(source))
        {
            this.target = null;
        }

        setVisible(false);
        dispose();
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * net.java.sip.communicator.impl.gui.customcontrols.SIPCommDialog#close
     * (boolean)
     */
    protected void close(boolean isEscaped)
    {
        cancelButton.doClick();
    }

    /**
     * Handles changes in the <code>Document</code> associated with
     * <code>targetComboBox</code> in order to dynamically enable/disable
     * <code>okButton</code> in accord with the validity of the specified target
     * contact address.
     * 
     * @param e the <code>DocumentEvent</code> containing the data associated
     *            with the change
     */
    private void documentChanged(DocumentEvent e)
    {
        okButton.setEnabled(getValidTarget() != null);
    }

    /**
     * Gets the target contact address specified through this dialog or
     * <tt>null</code> if this dialog was canceled.
     * 
     * @return the target contact address specified through this dialog or
     *         <tt>null</code> if this dialog was canceled
     */
    public String getTarget()
    {
        return target;
    }

    /**
     * Gets the target contact address specified in the UI of this dialog if the
     * specified value is valid or <tt>null</code> if there is no valid value
     * specified in the UI of this dialog.
     * 
     * @return the target contact address specified in the UI of this dialog if
     *         the specified value is valid or <tt>null</code> if there is no
     *         valid value specified in the UI of this dialog
     */
    private String getValidTarget()
    {
        String target = targetComboBox.getEditor().getItem().toString().trim();

        return ((target == null) || (target.length() <= 0)) ? null : target;
    }
}
