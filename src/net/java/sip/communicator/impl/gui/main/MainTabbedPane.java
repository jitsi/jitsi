/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.customcontrols.*;
import net.java.sip.communicator.impl.gui.i18n.*;
import net.java.sip.communicator.impl.gui.main.call.*;
import net.java.sip.communicator.impl.gui.main.contactlist.*;

/** 
 * The <tt>MainTabbedPane</tt> is a <tt>SIPCommTabbedPane</tt> that contains
 * three tabs: the <tt>ContactListPanel</tt>, the call list panel and
 * the <tt>DialPanel</tt>.
 * 
 * @author Yana Stamcheva
 */
public class MainTabbedPane extends SIPCommTabbedPane {

    private DialPanel dialPanel = new DialPanel();

    private ContactListPanel contactListPanel;

    /**
     * Constructs the <tt>MainTabbedPane</tt>.
     * 
     * @param parent The main application frame.
     */
    public MainTabbedPane(MainFrame parent) {
        super(false, false);

        this.setCloseIcon(false);
        this.setMaxIcon(false);

        contactListPanel = new ContactListPanel(parent);

        dialPanel.setPhoneNumberCombo(parent.getCallManager()
                .getPhoneNumberCombo());

        this.addTab(Messages.getString("contacts"), contactListPanel);
        this.addTab(Messages.getString("callList"), new JPanel());
        this.addTab(Messages.getString("dial"), dialPanel);
        
    }

    /**
     * Returns the <tt>ContactListPanel</tt> contained in this tabbed pane.
     * @return the <tt>ContactListPanel</tt> contained in this tabbed pane.
     */
    public ContactListPanel getContactListPanel() {
        return contactListPanel;
    }
}
