/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Copyright @ 2015 Atlassian Pty Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.java.sip.communicator.plugin.contactinfo;

import java.awt.*;
import java.util.*;

import javax.swing.*;
import javax.swing.event.*;

import net.java.sip.communicator.plugin.desktoputil.*;
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
    extends TransparentPanel
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

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
            .createTitledBorder(Resources.getString("service.gui.CONTACTS")),
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
    private static class ContactPanelCellRenderer
        extends DefaultListCellRenderer
    {
        /**
         * Serial version UID.
         */
        private static final long serialVersionUID = 0L;

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
        @Override
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
        @Override
        public void paintComponent(Graphics g)
        {
            if (this.isSelected)
            {
                Graphics2D g2 = (Graphics2D) g.create();

                try
                {
                    AntialiasingManager.activateAntialiasing(g2);

                    int width = getWidth();
                    int height = getHeight();

                    g2.setColor(selectedColor);
                    g2.fillRoundRect(1, 0, width, height, 7, 7);

                    g2.setColor(blueGreyBorderColor);
                    g2.setStroke(new BasicStroke(1.5f));
                    g2.drawRoundRect(1, 0, width - 2, height - 1, 7, 7);
                }
                finally
                {
                    g2.dispose();
                }
            }

            super.paintComponent(g);
        }
    }
}
