/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 * 
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.call;

import java.awt.event.*;

import javax.swing.*;
import javax.swing.event.*;

import net.java.sip.communicator.impl.gui.customcontrols.*;
import net.java.sip.communicator.impl.gui.lookandfeel.*;
import net.java.sip.communicator.impl.gui.main.contactlist.*;
import net.java.sip.communicator.service.contactlist.*;

/**
 * The <tt>CallComboBox</tt> is a history editable combo box that is
 * positioned above call and hangup buttons and is used when writing a number or
 * a contact name in order to be called.
 * 
 * @author Yana Stamcheva
 */
public class CallComboBox
    extends SIPCommSmartComboBox
    implements  ActionListener,
                DocumentListener,
                FocusListener
{
    public final static int MAX_COMBO_SIZE = 10;

    private MainCallPanel parentCallPanel;

    /**
     * Creates a <tt>CallComboBox</tt> by specifying the parent panel, where
     * this combo box will be placed.
     * 
     * @param parentCallPanel The parent panel.
     */
    public CallComboBox(MainCallPanel parentCallPanel)
    {
        this.parentCallPanel = parentCallPanel;

        this.setUI(new SIPCommCallComboBoxUI());
        this.addActionListener(this);

        JTextField textField =
            (JTextField) this.getEditor().getEditorComponent();

        textField.getDocument().addDocumentListener(this);

        textField.getActionMap().put("createCall", new CreateCallAction());
        textField.getInputMap().put(
                KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "createCall");

        textField.addFocusListener(this);
    }

    /**
     * Checks if this combobox editor field is empty. This will be the case if
     * the user hasn't selected an item from the combobox and hasn't written
     * anything the field.
     * 
     * @return TRUE if the combobox editor field is empty, otherwise FALSE
     */
    public boolean isComboFieldEmpty()
    {
        String item = ((CallComboEditor) this.getEditor()).getItem().toString();

        if (item.length() > 0)
            return false;
        else
            return true;
    }

    /**
     * Handles events triggered by user selection. Enables the call button when
     * user selects something in the combo box.
     */
    public void actionPerformed(ActionEvent e)
    {
        parentCallPanel.setCallMetaContact(false);
        parentCallPanel.setCallButtonEnabled(true);
    }

    public void insertUpdate(DocumentEvent e)
    {
        handleChange();
    }

    public void removeUpdate(DocumentEvent e)
    {
        handleChange();
    }

    public void changedUpdate(DocumentEvent e)
    {
    }

    /**
     * Enables or disabled the call button according to the content in the combo
     * box editor field.
     */
    protected void handleChange()
    {
        String item = ((CallComboEditor) this.getEditor()).getItem().toString();

        if (item.length() > 0)
        {
            parentCallPanel.setCallMetaContact(false);

            ContactList clist =
                parentCallPanel.getMainFrame().getContactListPanel()
                    .getContactList();

            clist.removeSelectionInterval(clist.getSelectedIndex(), clist
                .getSelectedIndex());

            parentCallPanel.setCallButtonEnabled(true);
        }
        else
        {
            Object o =
                parentCallPanel.getMainFrame().getContactListPanel()
                    .getContactList().getSelectedValue();

            boolean enabled = true;
            if (o == null || !(o instanceof MetaContact))
            {
                // no contact can be called. call button not active
                enabled = false;
            }

            parentCallPanel.setCallButtonEnabled(enabled);

        }
    }

    public void focusGained(FocusEvent e)
    {
        this.handleChange();
    }

    public void focusLost(FocusEvent e)
    {
    }

    /**
     * Creates a call to the contact given by the string in the combo box editor
     * field.
     */
    private class CreateCallAction
        extends AbstractAction
    {
        public void actionPerformed(ActionEvent e)
        {
            String item = ((CallComboEditor) getEditor()).getItem().toString();

            if (item.length() > 0)
                CallManager.createCall(parentCallPanel.getCallProvider(), item);
            else
            {
                if (!isPopupVisible())
                    setPopupVisible(true);
            }
        }
    }
}
