/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
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
 * The <tt>CallComboBox</tt> is a history editable combo box that is positioned
 * above call and hangup buttons and is used when writing a number or a contact
 * name in order to be called.
 * 
 * @author Yana Stamcheva
 */
public class CallComboBox
    extends SmartComboBox
    implements  ActionListener,
                DocumentListener
{   
    
    private CallManager callManager;
    
    public final static int MAX_COMBO_SIZE = 10;
    
    public CallComboBox(CallManager callManager) {
        
        this.callManager = callManager;
        
        this.setUI(new SIPCommCallComboBoxUI());
        this.addActionListener(this);
        
        JTextField textField = (JTextField)this.getEditor().getEditorComponent();
               
        textField.getDocument().addDocumentListener(this);
        
        textField.getActionMap().put("createCall", new CreateCallAction());
        textField.getInputMap().put(
                KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "createCall");
    }
 
    /**
     * Checks if this combo box editor field is empty. This will be the case if
     * the user hasn't selected an item from the combobox and hasn't written
     * anything the field.
     * 
     * @return TRUE if the combobox editor field is empty, otherwise FALSE
     */
    public boolean isComboFieldEmpty()
    {
        String item = ((CallComboEditor)this.getEditor()).getItem().toString();
        
        if(item.length() > 0)
            return false;
        else
            return true;
    }
    
    /**
     * Handles events triggered by user selection. Enables the call button
     * when user selects something in the combo box.
     */
    public void actionPerformed(ActionEvent e)
    {   
        callManager.setCallMetaContact(false);
        callManager.getCallButton().setEnabled(true);        
    }
    
            
    public void insertUpdate(DocumentEvent e) { handleChange(); }
    public void removeUpdate(DocumentEvent e) { handleChange(); }
    public void changedUpdate(DocumentEvent e) {}
    
    /**
     * Enables or disabled the call button according to the content in the 
     * combo box editor field.
     */
    protected void handleChange() {
        String item = ((CallComboEditor)this.getEditor()).getItem().toString();
        
        if (item.length() > 0) {
            callManager.setCallMetaContact(false);
            
            ContactList clist = this.callManager.getMainFrame()
                .getContactListPanel().getContactList();
        
            clist.removeSelectionInterval(
                clist.getSelectedIndex(), clist.getSelectedIndex());
            
            callManager.getCallButton().setEnabled(true);
        }
        else {
            Object o = callManager.getMainFrame().getContactListPanel()
                .getContactList().getSelectedValue();
            
            if(o == null || !(o instanceof MetaContact))
                callManager.getCallButton().setEnabled(false);
        }
    }
    
    /**
     * Creates a call to the contact given by the string in the combo box
     * editor field.
     */
    private class CreateCallAction extends AbstractAction
    {
        public void actionPerformed(ActionEvent e)
        {
            String item = ((CallComboEditor)getEditor()).getItem().toString();
            
            if(item.length() > 0)
                callManager.createCall(item);
            else {                
                if(!isPopupVisible())
                    setPopupVisible(true);
            }
        }
    }
}
