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

/**
 * The <tt>CallList</tt> is the component that contains history call records.
 * 
 * @author Yana Stamcheva
 */
public class CallList
    extends JList
    implements MouseListener
{
    CallListModel listModel = new CallListModel();
    
    public CallList()
    {
        this.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));

        this.getSelectionModel().setSelectionMode(
                ListSelectionModel.SINGLE_SELECTION);

        this.setCellRenderer(new CallListCellRenderer());
        
        this.setModel(listModel);

        this.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        
        this.addMouseListener(this);
    }
    
    public void addItem(Object item)
    {
        listModel.addElement(item);
    }
    
    public void addItem(Object item, int index)
    {
        listModel.addElement(index, item);
    }
    
    public void removeItem(GuiCallParticipantRecord record)
    {
        listModel.removeElement(record);
    }
    
    public void removeAll()
    {
        listModel.removeAll();
    }
    
    public int getLength()
    {
        return listModel.getSize();
    }
    
    public Object getItem(int index)
    {
        return listModel.getElementAt(index);
    }
    
    /**
     * Closes or opens a group of calls.
     */
    public void mouseClicked(MouseEvent e) {
        if (e.getClickCount() > 1) {

            CallListModel listModel = (CallListModel) this.getModel();

            Object element = listModel.getElementAt(this.getSelectedIndex());

            if (element instanceof String) {
                if (listModel.isDateClosed(element)) {
                    listModel.openDate(element);
                } else {
                    listModel.closeDate(element);
                }
            }
        }
    }

    public void mouseEntered(MouseEvent e)
    {}
    
    public void mouseExited(MouseEvent e)
    {}

    public void mousePressed(MouseEvent e)
    {}

    public void mouseReleased(MouseEvent e)
    {}
}
