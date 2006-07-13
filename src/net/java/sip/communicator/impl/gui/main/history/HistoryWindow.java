/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main.history;

import java.awt.BorderLayout;
import java.util.Vector;

import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import net.java.sip.communicator.impl.gui.main.i18n.Messages;
import net.java.sip.communicator.impl.gui.utils.Constants;
import net.java.sip.communicator.impl.gui.utils.ImageLoader;
import net.java.sip.communicator.service.contactlist.MetaContact;

/**
 * The <tt>HistoryWindow</tt> is the window, where user could view or search
 * in the message history. The <tt>HistoryWindow</tt> could contain the history
 * for one or a group of <tt>MetaContact</tt>s.
 * 
 * @author Yana Stamcheva
 */
public class HistoryWindow extends JFrame {

    private JScrollPane historyPane = new JScrollPane();

    private JPanel mainPanel = new JPanel(new BorderLayout());

    private NavigationPanel navigationPanel = new NavigationPanel();

    private SearchPanel searchPanel = new SearchPanel();

    private JMenuBar historyMenuBar = new JMenuBar();

    private HistoryMenu historyMenu;

    private JMenu settingsMenu = new JMenu(Messages.getString("settings"));

    private JPanel northPanel = new JPanel(new BorderLayout());

    private Vector contacts;

    private MetaContact contactItem;

    private String title = Messages.getString("history") + " - ";

    /**
     * Creates an instance of the <tt>HistoryWindow</tt>.
     */
    public HistoryWindow() {
        historyMenu = new HistoryMenu(this);

        this.setSize(Constants.HISTORY_WINDOW_WIDTH,
                Constants.HISTORY_WINDOW_HEIGHT);

        this.setIconImage(ImageLoader.getImage(ImageLoader.SIP_LOGO));

        this.init();
    }

    /**
     * Constructs the window, by adding all components and panels.
     */
    private void init() {
        this.historyMenuBar.add(historyMenu);

        this.historyMenuBar.add(settingsMenu);

        this.northPanel.add(historyMenuBar, BorderLayout.NORTH);

        this.northPanel.add(searchPanel, BorderLayout.CENTER);

        this.mainPanel.add(northPanel, BorderLayout.NORTH);

        this.mainPanel.add(historyPane, BorderLayout.CENTER);

        this.mainPanel.add(navigationPanel, BorderLayout.SOUTH);

        this.getContentPane().add(mainPanel);
    }

    /**
     * The <tt>HistoryWindow</tt> could contain the history for one or a
     * group of <tt>MetaContact</tt>s. This method returns the list of
     * <tt>MetaContact</tt>s for this history.
     * 
     * @return returns the list of contacts for this history.
     */
    public Vector getContacts() {
        return contacts;
    }

    /**
     * The <tt>HistoryWindow</tt> could contain the history for one or a
     * group of <tt>MetaContact</tt>s. Sets the list of contacts for this
     * <tt>HistoryWindow</tt>.
     * @param contacts The Vector containing all <tt>MetaContact</tt>s for
     * this <tt>HistoryWindow</tt>.
     */
    public void setContacts(Vector contacts) {
        this.contacts = contacts;

        for (int i = 0; i < contacts.size(); i++) {

            MetaContact contact = (MetaContact) contacts.get(i);

            this.title += " " + contact.getDisplayName();
        }
        this.setTitle(title);
    }

    /**
     * Sets the contact, which history will be displayed in the case of a 
     * <tt>HistoryWindow</tt> for only one contact.
     * @param contact The <tt>MetaContact</tt>, which history will be displayed.
     */
    public void setContact(MetaContact contact) {
        this.contactItem = contact;

        this.title += " " + contact.getDisplayName();

        this.setTitle(title);
    }
}
