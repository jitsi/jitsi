/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.utils;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.event.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.lookandfeel.*;
import net.java.sip.communicator.impl.gui.main.contactlist.*;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.util.swing.*;

/**
 * The invite dialog is a widget that shows a list of contacts, from which the
 * user could pick in order to create a conference chat or call.
 *
 * @author Yana Stamcheva
 */
public class OneChoiceInviteDialog
    extends SIPCommDialog
{
    /**
     * The information text area.
     */
    private final JTextArea infoTextArea;

    /**
     * The label containing the icon of this dialog.
     */
    private final JLabel infoIconLabel = new JLabel();

    /**
     * The OK button.
     */
    private final JButton okButton = new JButton(
        GuiActivator.getResources().getI18NString("service.gui.OK"));

    /**
     * The cancel button.
     */
    private final JButton cancelButton = new JButton(
        GuiActivator.getResources().getI18NString("service.gui.CANCEL"));

    /**
     * The contact list.
     */
    private DefaultContactList contactList;

    /**
     * The contact list model.
     */
    private final DefaultListModel contactListModel = new DefaultListModel();

    /**
     * The new contact field.
     */
    private final SIPCommTextField newContactField
        = new SIPCommTextField(GuiActivator.getResources()
            .getI18NString("service.gui.OR_ENTER_PHONE_NUMBER"));

    /**
     * Constructs an <tt>OneChoiceInviteDialog</tt>, by specifying the initial
     * list of contacts available.
     *
     * @param title the title to show on the top of this dialog
     * @param metaContacts the list of contacts available for invite
     */
    public OneChoiceInviteDialog(String title,
                                java.util.List<MetaContact> metaContacts)
    {
        this(title);

        // Initialize contacts list.
        for(MetaContact metaContact : metaContacts)
        {
            this.addMetaContact(metaContact);
        }
    }

    /**
     * Constructs an <tt>OneChoiceInviteDialog</tt>.
     * @param title the title to show on the top of this dialog
     */
    public OneChoiceInviteDialog (String title)
    {
        this.setModal(false);

        this.setTitle(title);

        TransparentPanel mainPanel
            = new TransparentPanel(new BorderLayout(5, 5));

        TransparentPanel northPanel
            = new TransparentPanel(new BorderLayout(10, 10));

        mainPanel.setPreferredSize(new Dimension(450, 350));

        mainPanel.setBorder(
            BorderFactory.createEmptyBorder(15, 15, 15, 15));

        infoTextArea = createInfoArea();

        northPanel.add(infoIconLabel, BorderLayout.WEST);
        northPanel.add(infoTextArea, BorderLayout.CENTER);

        TransparentPanel buttonsPanel
            = new TransparentPanel(new FlowLayout(FlowLayout.RIGHT));

        buttonsPanel.add(okButton);
        buttonsPanel.add(cancelButton);

        this.getRootPane().setDefaultButton(okButton);
        okButton.setMnemonic(
            GuiActivator.getResources().getI18nMnemonic("service.gui.OK"));
        cancelButton.setMnemonic(
            GuiActivator.getResources().getI18nMnemonic("service.gui.CANCEL"));

        Component contactListComponent = createContactListComponent();

        newContactField.addFocusListener(new FocusAdapter()
        {
            /**
             * Removes all other selections.
             * @param e the <tt>FocusEvent</tt> that notified us
             */
            public void focusGained(FocusEvent e)
            {
                contactList.removeSelectionInterval(
                    contactList.getMinSelectionIndex(),
                    contactList.getMaxSelectionIndex());
            }
        });

        TransparentPanel listPanel = new TransparentPanel(new BorderLayout());
        listPanel.setBorder(SIPCommBorders.getRoundBorder());
        listPanel.add(contactListComponent);
        listPanel.add(newContactField, BorderLayout.SOUTH);

        mainPanel.add(northPanel, BorderLayout.NORTH);
        mainPanel.add(listPanel, BorderLayout.CENTER);
        mainPanel.add(buttonsPanel, BorderLayout.SOUTH);

        this.getContentPane().add(mainPanel);
    }

    /**
     * Adds the given <tt>metaContact</tt> to the left list of contacts
     * available for invite.
     * @param metaContact the <tt>MetaContact</tt> to add
     */
    public void addMetaContact(MetaContact metaContact)
    {
        contactListModel.addElement(metaContact);
    }

    /**
     * Removes the given <tt>metaContact</tt> from the left list of contacts
     * available for invite.
     * @param metaContact the <tt>MetaContact</tt> to add
     */
    public void removeMetaContact(MetaContact metaContact)
    {
        contactListModel.removeElement(metaContact);
    }

    /**
     * Removes all <tt>MetaContact</tt>-s from the left list of contacts
     * available for invite.
     */
    public void removeAllMetaContacts()
    {
        contactListModel.removeAllElements();
    }

    /**
     * Returns an enumeration of the list of selected <tt>MetaContact</tt>s.
     * @return an enumeration of the list of selected <tt>MetaContact</tt>s
     */
    public MetaContact getSelectedMetaContact()
    {
        return (MetaContact) contactList.getSelectedValue();
    }

    /**
     * Returns an enumeration of the list of selected Strings.
     * @return an enumeration of the list of selected Strings
     */
    public String getSelectedString()
    {
        return newContactField.getText();
    }

    /**
     * Sets the information text explaining how to use the containing form.
     * @param text the text
     */
    public void setInfoText(String text)
    {
        infoTextArea.setText(text);
    }

    /**
     * Sets the icon shown in the left top corner of this dialog.
     * @param icon the icon
     */
    public void setIcon(Icon icon)
    {
        infoIconLabel.setIcon(icon);
    }

    /**
     * Sets the text of the ok button.
     * @param text the text of the ok button
     */
    public void setOkButtonText(String text)
    {
        okButton.setText(text);
    }

    /**
     * Adds an <tt>ActionListener</tt> to the contained "Invite" button.
     * @param l the <tt>ActionListener</tt> to add
     */
    public void addOkButtonListener(ActionListener l)
    {
        this.okButton.addActionListener(l);
    }

    /**
     * Adds an <tt>ActionListener</tt> to the contained "Cancel" button.
     * @param l the <tt>ActionListener</tt> to add
     */
    public void addCancelButtonListener(ActionListener l)
    {
        this.cancelButton.addActionListener(l);
    }

    /**
     * Closes this dialog by clicking on the "Cancel" button.
     * @param isEscaped indicates if this <tt>close</tt> is provoked by an
     * escape
     */
    protected void close(boolean isEscaped)
    {
        this.cancelButton.doClick();
    }

    /**
     * Creates the an info text area.
     * @return the created <tt>JTextArea</tt>
     */
    private JTextArea createInfoArea()
    {
        JTextArea infoTextArea = new JTextArea();

        infoTextArea.setFont(infoTextArea.getFont().deriveFont(Font.BOLD));
        infoTextArea.setLineWrap(true);
        infoTextArea.setOpaque(false);
        infoTextArea.setWrapStyleWord(true);
        infoTextArea.setEditable(false);

        return infoTextArea;
    }

    /**
     * Creates the contact list component.
     * @return the created contact list component
     */
    private Component createContactListComponent()
    {
        contactList = new DefaultContactList();

        contactList.setModel(contactListModel);

        contactList.addListSelectionListener(new ListSelectionListener()
        {
            public void valueChanged(ListSelectionEvent e)
            {
                if (contactList.getSelectedIndex() >= 0)
                    newContactField.setText("");
            }
        });

        JScrollPane contactListScrollPane = new JScrollPane();

        contactListScrollPane.setOpaque(false);
        contactListScrollPane.getViewport().setOpaque(false);
        contactListScrollPane.getViewport().add(contactList);
        contactListScrollPane.getViewport().setBorder(null);
        contactListScrollPane.setViewportBorder(null);
        contactListScrollPane.setBorder(null);

        return contactListScrollPane;
    }
}
