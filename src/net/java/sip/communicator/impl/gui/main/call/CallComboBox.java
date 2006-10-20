/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.call;

import java.awt.*;
import java.awt.event.*;
import java.beans.*;
 
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
 
import javax.swing.AbstractListModel;
import javax.swing.ComboBoxEditor;
import javax.swing.MutableComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

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
    extends JComboBox
    implements  ActionListener,
                FocusListener
{
    
    private ArrayList historyList = new ArrayList();
    
    private CallManager callManager;
    
    public CallComboBox(CallManager callManager) {
        
        this.callManager = callManager;
        
        historyList.add("gigi@ekyga.net");
        historyList.add("sc3@voipgw.u-strasbg.fr");
        
        setModel(new FilterableComboBoxModel(historyList));
        setEditor(new CallComboEditor());
        setEditable(true);
        setFocusable(true);
        
        this.addActionListener(this);
        this.getEditor().getEditorComponent().addFocusListener(this);        
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
    
    public void focusGained(FocusEvent e)
    {
        this.callManager.setCallMetaContact(false);
        
        ContactList clist = this.callManager.getMainFrame()
            .getContactListPanel().getContactList();
        clist.removeSelectionInterval(
                clist.getSelectedIndex(), clist.getSelectedIndex());
    }

    public void focusLost(FocusEvent e)
    {}    
        
    public class FilterableComboBoxModel extends AbstractListModel
            implements MutableComboBoxModel {
        
        private List items;
        private Filter filter;
        private List filteredItems;
        private Object selectedItem;
 
        public FilterableComboBoxModel(List items) {
            
            this.items = new ArrayList(items);
            filteredItems = new ArrayList(items.size());
            updateFilteredItems();            
        }
                
        public void addElement( Object obj ) {
        
            items.add(obj);
            updateFilteredItems();
        }
        
        public void removeElement( Object obj ) {
        
            items.remove(obj);
            updateFilteredItems();
        }
                
        public void removeElementAt(int index) {
            
            items.remove(index);
            updateFilteredItems();
        }
        
        public void insertElementAt( Object obj, int index ) {}
        
        public void setFilter(Filter filter) {
            
            this.filter = filter;
            updateFilteredItems();
        }
 
        protected void updateFilteredItems() {
            
            fireIntervalRemoved(this, 0, filteredItems.size());
            filteredItems.clear();
 
            if (filter == null)
                filteredItems.addAll(items);
            else {
                for (Iterator iterator = items.iterator(); iterator.hasNext();) {
                    
                    Object item = iterator.next();
 
                    if (filter.accept(item))
                        filteredItems.add(item);
                }
            }
            fireIntervalAdded(this, 0, filteredItems.size());
        }
 
        public int getSize() {
            return filteredItems.size();
        }
 
        public Object getElementAt(int index) {
            return filteredItems.get(index);
        }
 
        public Object getSelectedItem() {
            return selectedItem;
        }
 
        public void setSelectedItem(Object val) {
            
            if ((selectedItem == null) && (val == null))
                return;
 
            if ((selectedItem != null) && selectedItem.equals(val))
                return;
 
            if ((val != null) && val.equals(selectedItem))
                return;
 
            selectedItem = val;
            fireContentsChanged(this, -1, -1);
        }
    }
 
    public static interface Filter {
        public boolean accept(Object obj);
    }
 
    class StartsWithFilter implements Filter {
        
        private String prefix;
        public StartsWithFilter(String prefix) { this.prefix = prefix; }
        public boolean accept(Object o) { return o.toString().startsWith(prefix); }
    }
 
    public class CallComboEditor
        implements ComboBoxEditor, DocumentListener
    {   
        private JTextField text;
        private volatile boolean filtering = false;
        private volatile boolean setting = false;
 
        public CallComboEditor() {
            
            text = new JTextField(15);
            text.getDocument().addDocumentListener(this);
        }
 
        public Component getEditorComponent() { return text; }
 
        public void setItem(Object item) {
            if(filtering)
                return;
 
            setting = true;
            String newText = (item == null) ? "" : item.toString();
            
            text.setText(newText);
            setting = false;
        }
 
        public Object getItem() {            
            return text.getText();
        }
 
        public void selectAll() { text.selectAll(); }
 
        public void addActionListener(ActionListener l) {
            text.addActionListener(l);
        }
 
        public void removeActionListener(ActionListener l) {
            text.removeActionListener(l);
        }
 
        public void insertUpdate(DocumentEvent e) { handleChange(); }
        public void removeUpdate(DocumentEvent e) { handleChange(); }
        public void changedUpdate(DocumentEvent e) { }
 
        protected void handleChange() {
            if (setting)
                return;
 
            filtering = true;
            
            Filter filter = null;
            if (text.getText().length() > 0) {
                filter = new StartsWithFilter(text.getText());
                 
                callManager.setCallMetaContact(false);
                
                callManager.getCallButton().setEnabled(true);
            }
            else {
                Object o = callManager.getMainFrame().getContactListPanel()
                    .getContactList().getSelectedValue();
                
                if(o == null || !(o instanceof MetaContact))
                    callManager.getCallButton().setEnabled(false);
            }
            
            ((FilterableComboBoxModel) getModel()).setFilter(filter);
            // A bit nasty but it seems to get the popup validated properly
            setPopupVisible(false);
            setPopupVisible(true);
            filtering = false;
        }
    }    
}
