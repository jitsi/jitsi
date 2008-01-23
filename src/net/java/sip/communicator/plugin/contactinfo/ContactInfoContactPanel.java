package net.java.sip.communicator.plugin.contactinfo;

/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 * 
 * Distributable under LGPL license. See terms of license at gnu.org.
 */

import java.awt.*;
import java.util.*;

import javax.swing.*;
import javax.swing.event.*;

import net.java.sip.communicator.service.protocol.*;

/**
 * The left side panel of ContactInfoDialog. Display all associated subcontacts
 * and their respective protocols in a JList. If a user is selected, the
 * ContactInfoDetailsPanel will be updated to the current contact.
 * 
 * @author Adam Goldstein
 * @author Yana Stamcheva
 */
public class ContactInfoContactPanel
    extends JPanel
{
    /**
     * The list of all subcontacts related to the selected contact.
     */
    private JList contactList = new JList();

    /**
     * The scroll pane containing the list of all sub contacts of a selected
     * contact.
     */
    private JScrollPane contactScrollPane = new JScrollPane();

    private DefaultListModel contactListModel = new DefaultListModel();

    /**
     * The associated ProtocolPanel on our parent ContactInfoDialog.
     */
    private ContactInfoDetailsPanel protocolPanel;

    /**
     * The parent dialog that makes the connection between the contacts and
     * the details panel.
     */
    private ContactInfoDialog contactInfoDialog;

    /**
     * Create a panel with a list of all sub-contacts associated with the
     * contact that was originally selected. Whenever a sub-contact is picked,
     * notifies the protocolPanel of the change and it will update the displayed
     * details.
     * 
     * @param contacts the list of contacts
     * @param dialog the contact info dialog
     */
    public ContactInfoContactPanel( Iterator<Contact> contacts,
                                    ContactInfoDialog dialog)
    {
        super(new BorderLayout());

        this.contactInfoDialog = dialog;

        this.setBorder(BorderFactory.createCompoundBorder(BorderFactory
            .createTitledBorder(Resources.getString("contacts")),
            BorderFactory.createEmptyBorder(5, 5, 5, 5)));

        this.contactList.setOpaque(false);
        this.contactList.setModel(contactListModel);
        this.contactList.setCellRenderer(new ContactPanelCellRenderer());
        this.contactList.addListSelectionListener(new ListSelectionListener()
        {
            public void valueChanged(ListSelectionEvent e)
            {
                // When the user release the mouse button and completes the
                // selection, getValueIsAdjusting() becomes false
                if (!e.getValueIsAdjusting())
                {
                    JList list = (JList) e.getSource();

                    Contact selectedContact
                        = (Contact) list.getSelectedValue();

                    contactInfoDialog.loadContactDetails(selectedContact);
                }
            }
        });

        boolean isFirstIter = true;
        while (contacts.hasNext())
        {
            Contact contact = contacts.next();

            this.contactListModel.addElement(contact);

            if (isFirstIter)
            {
                isFirstIter = false;
                contactInfoDialog.loadContactDetails(contact);
                contactList.setSelectedIndex(0);
            }
        }

        this.contactScrollPane.setPreferredSize(new Dimension(100, 200));
        this.contactScrollPane.getViewport().add(contactList);
        this.add(contactScrollPane);
    }

    /**
     * A cell renderer that allows both text and icons in our contactList.
     */
    private class ContactPanelCellRenderer
        extends DefaultListCellRenderer
    {
        private boolean isSelected;

        private Color blueGreyBorderColor = new Color(131, 149, 178);

        private Color selectedColor = new Color(209, 212, 225);

        public ContactPanelCellRenderer()
        {
            this.setOpaque(false);
        }

        /**
         * Renders a <tt>Contact</tt> object in a JList, by visualizing
         * the contact name and the protocol icon.
         * 
         * @param list the rendered JList
         * @param value the object to be rendered
         * @param index the index of the object in the list
         * @param isSelected indicates if the rendered object is selected
         * @param cellHasFocus indicates if the rendered object is in a focused
         * cell
         */
        public Component getListCellRendererComponent(  JList list,
                                                        Object value,
                                                        int index,
                                                        boolean isSelected,
                                                        boolean cellHasFocus)
        {
            super.getListCellRendererComponent(list, value, index, isSelected,
                cellHasFocus);

            this.isSelected = isSelected;

            Contact contact = (Contact) value;

            this.setIcon(new ImageIcon(contact.getProtocolProvider()
                .getProtocolIcon().getIcon(ProtocolIcon.ICON_SIZE_16x16)));
            this.setText(((Contact) value).getDisplayName());

            return this;
        }

        /**
         * Paint a round blue border and background when a cell is selected.
         */
        public void paintComponent(Graphics g)
        {
            Graphics2D g2 = (Graphics2D) g;

            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

            if (this.isSelected)
            {
                g2.setColor(selectedColor);
                g2.fillRoundRect(1, 0, this.getWidth(), this.getHeight(), 7, 7);

                g2.setColor(blueGreyBorderColor);
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(1, 0, this.getWidth() - 2, this.getHeight() - 1,
                        7, 7);
            }

            super.paintComponent(g);
        }
    }
}