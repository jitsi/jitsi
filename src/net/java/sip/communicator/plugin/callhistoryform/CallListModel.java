/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.callhistoryform;

import java.util.*;

import javax.swing.*;

/**
 * The data model of the Call list.
 * 
 * @author Yana Stamcheva
 */
public class CallListModel extends AbstractListModel
{
    private LinkedList callList = new LinkedList();
    
    private Hashtable closedDates = new Hashtable();
    
    /**
     * Closes the given date by hiding all containing calls.
     * 
     * @param date The date to close.
     */
    public void closeDate(Object date)
    {        
        int startIndex = this.indexOf(date);
        int endIndex = startIndex;
        int currentSize = getSize();
        Collection c = new ArrayList();
        
        for(int i = startIndex + 1; i < currentSize; i ++) {
            Object o = this.getElementAt(i);
            
            if(o instanceof GuiCallPeerRecord) {
                this.closedDates.put(o, date);
                c.add(o);
                endIndex++;
            }
            else
                break;
        }
        removeAll(c);
        fireIntervalRemoved(this, startIndex, endIndex);
    }

    /**
     * Opens the given date by showing all containing calls.
     * 
     * @param date The date to open.
     */
    public void openDate(Object date)
    {
        int startIndex = this.indexOf(date);
        int endIndex = startIndex;
        Hashtable closedDatesCopy = (Hashtable)closedDates.clone();
        
        if(closedDatesCopy.containsValue(date)) {            
            Iterator dates = closedDatesCopy.entrySet().iterator();
            
            while(dates.hasNext()) {
                Map.Entry entry = (Map.Entry)dates.next();
                Object callRecord = entry.getKey();
                Object callDate = entry.getValue();
                
                if(callDate.equals(date)) {
                    endIndex++;
                    closedDates.remove(callRecord);
                    this.addElement(endIndex, callRecord);
                }
            }            
        }
        fireIntervalAdded(this, startIndex, endIndex);
    }

    /**
     * Checks whether the given date is closed.
     * 
     * @param date The date to check.
     * @return True if the date is closed, false - otherwise.
     */
    public boolean isDateClosed(Object date) {        
        if (this.closedDates.containsValue(date))
            return true;
        else
            return false;
    }

    public int getSize()
    {        
        return callList.size();
    }

    public Object getElementAt(int index)
    {
        if (index>=0)
            return callList.get(index);
        else
            return null;
    }

    public void addElement(Object item)
    {
        synchronized (callList) {
            this.callList.add(item);
            
            int index = callList.indexOf(item);
            fireIntervalAdded(this, index, index);
        }
    }
    
    public void addElement(int index, Object item)
    {
        synchronized (callList) {
            this.callList.add(index, item);
            fireIntervalAdded(this, index, index);
        }
    }
    
    public void removeElement(Object item)
    {
        synchronized (callList) {
            this.callList.remove(item);
        }
    }
    
    public void removeAll(Collection c)
    {
        synchronized (callList) {
            callList.removeAll(c);
        }        
    }
    
    public void removeAll()
    {
        int currentSize = getSize();
        
        while(callList.size() > 0) {
            synchronized (callList) {
                callList.removeLast();
            }
        }
        fireIntervalRemoved(this, 0, currentSize);
    }
    
    public int indexOf(Object item)
    {
        synchronized (callList) {
            return callList.indexOf(item);
        }
    }
}
