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
package net.java.sip.communicator.impl.gui.main.authorization;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.main.*;
import net.java.sip.communicator.impl.gui.main.contactlist.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.skin.*;

/**
 * @author Yana Stamcheva
 * @author Adam Netocny
 * @author Hristo Terezov
 */
public class AuthorizationRequestedDialog
    extends SIPCommDialog
    implements  ActionListener,
                Skinnable
{
    private final Logger logger
        = Logger.getLogger(AuthorizationRequestedDialog.class);

    public static final int UNDEFINED_CODE = -2;

    public static final int ACCEPT_CODE = 0;

    public static final int REJECT_CODE = 1;

    public static final int IGNORE_CODE = 2;

    public static final int ERROR_CODE = -1;

    private JTextArea infoTextArea = new JTextArea();

    private JTextPane requestPane = new JTextPane();

    private JPanel buttonsPanel =
        new TransparentPanel(new FlowLayout(FlowLayout.RIGHT));

    private JPanel northPanel = new TransparentPanel(new BorderLayout(10, 0));

    private JPanel titlePanel = new TransparentPanel(new GridLayout(0, 1));

    private JLabel titleLabel = new JLabel();

    private JLabel iconLabel = new JLabel(new ImageIcon(
            ImageLoader.getImage(ImageLoader.AUTHORIZATION_ICON)));

    private JButton acceptButton = new JButton(
        GuiActivator.getResources().getI18NString("service.gui.AUTHORIZE"));

    private JButton rejectButton = new JButton(
        GuiActivator.getResources().getI18NString("service.gui.DENY"));

    private JButton ignoreButton = new JButton(
        GuiActivator.getResources().getI18NString("service.gui.IGNORE"));

    private JScrollPane requestScrollPane = new JScrollPane();

    private JPanel mainPanel = new TransparentPanel(new BorderLayout(10, 10));

    private JPanel reasonsPanel = new TransparentPanel();

    private String title
        = GuiActivator.getResources()
            .getI18NString("service.gui.AUTHORIZATION_REQUESTED");

    private SIPCommCheckBox addContactCheckBox;

    private JComboBox groupComboBox;

    private Object lock = new Object();

    private int result = UNDEFINED_CODE;

    /**
     * Constructs the <tt>RequestAuthorisationDialog</tt>.
     *
     * @param mainFrame the main application window
     * @param contact The <tt>Contact</tt>, which requires authorisation.
     * @param request The <tt>AuthorizationRequest</tt> that will be sent.
     */
    public AuthorizationRequestedDialog(MainFrame mainFrame,
                                        Contact contact,
                                        AuthorizationRequest request)
    {
        super(mainFrame, false);

        this.setModal(false);

        this.setTitle(title);

        titleLabel.setHorizontalAlignment(JLabel.CENTER);
        titleLabel.setText(title);

        Font font = titleLabel.getFont();
        titleLabel.setFont(font.deriveFont(Font.BOLD, font.getSize2D() + 6));

        String contactName = "";
        if(contact.getAddress().equals(contact.getDisplayName()))
        {
            contactName = contact.getAddress();
        }
        else
        {
            String displayName = contact.getDisplayName();
            if(displayName.length() > 64)
            {
                displayName = displayName.substring(0, 47) + "...";
            }
            contactName
                =  displayName + " <" + contact.getAddress() + ">";
        }

        String infoText = GuiActivator.getResources().getI18NString(
            "service.gui.AUTHORIZATION_REQUESTED_INFO",
            new String[]{contactName});
        if(infoText.length() > 256)
        {
            infoText = infoText.substring(0, 253) + "...";
        }
        infoTextArea.setText(infoText);

        this.infoTextArea.setFont(infoTextArea.getFont().deriveFont(Font.BOLD));
        this.infoTextArea.setLineWrap(true);
        this.infoTextArea.setWrapStyleWord(true);
        this.infoTextArea.setOpaque(false);
        this.infoTextArea.setEditable(false);

        this.titlePanel.add(titleLabel);
        this.titlePanel.add(infoTextArea);

        this.northPanel.add(iconLabel, BorderLayout.WEST);
        this.northPanel.add(titlePanel, BorderLayout.CENTER);

        reasonsPanel.setLayout(new BoxLayout(reasonsPanel, BoxLayout.Y_AXIS));
        reasonsPanel.setBorder(BorderFactory.createEmptyBorder(
            0, iconLabel.getIcon().getIconWidth() + 5, 0, 0));

        if(request.getReason() != null && !request.getReason().equals(""))
        {
            this.requestScrollPane.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEtchedBorder(),
                BorderFactory.createEmptyBorder(3, 3, 3, 3)));

            requestScrollPane.getViewport().setOpaque(false);

            this.requestPane.setEditable(false);
            this.requestPane.setOpaque(false);
            this.requestPane.setText(request.getReason());

            this.requestScrollPane.getViewport().add(requestPane);

            this.reasonsPanel.add(requestScrollPane);

            this.mainPanel.setPreferredSize(new Dimension(700, 270));
        }
        else
        {
            this.mainPanel.setPreferredSize(new Dimension(700, 220));
        }

        // If the authorization request comes from a non-persistent contact,
        // we'll suggest to the user to add it to the contact list.
        if (!contact.isPersistent())
        {
            String addContactText = "";
            if(contactName.length() > 50)
            {
                addContactText = GuiActivator.getResources()
                    .getI18NString("service.gui.ADD_CONTACT_TO_CONTACTLIST",
                        new String[]{contactName});
            }
            else
            {
                addContactText = GuiActivator.getResources()
                    .getI18NString("service.gui.ADD_AUTHORIZED_CONTACT",
                        new String[]{contactName});
            }
            addContactCheckBox
                = new SIPCommCheckBox(addContactText, true);
            addContactCheckBox.setBorder(null);

            JPanel checkBoxPanel
                = new TransparentPanel(new FlowLayout(FlowLayout.LEFT));
            checkBoxPanel.add(addContactCheckBox);

            JLabel groupLabel = new JLabel(GuiActivator.getResources()
                .getI18NString("service.gui.SELECT_GROUP"));

            groupComboBox = AddContactDialog.createGroupCombo(this);

            JPanel groupPanel = new TransparentPanel(new BorderLayout(5, 5));
            groupPanel.add(groupLabel, BorderLayout.WEST);
            groupPanel.add(groupComboBox, BorderLayout.CENTER);

            reasonsPanel.add(checkBoxPanel);
            reasonsPanel.add(groupPanel);
        }

        this.acceptButton.setName("authorize");
        this.rejectButton.setName("deny");
        this.ignoreButton.setName("ignore");

        this.acceptButton.addActionListener(this);
        this.rejectButton.addActionListener(this);
        this.ignoreButton.addActionListener(this);

        this.acceptButton.setMnemonic(
            GuiActivator.getResources().getI18nMnemonic("service.gui.AUTHORIZE"));
        this.rejectButton.setMnemonic(
            GuiActivator.getResources().getI18nMnemonic("service.gui.DENY"));
        this.ignoreButton.setMnemonic(
            GuiActivator.getResources().getI18nMnemonic("service.gui.IGNORE"));

        this.buttonsPanel.add(acceptButton);
        this.buttonsPanel.add(rejectButton);
        this.buttonsPanel.add(ignoreButton);

        this.mainPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        this.mainPanel.add(northPanel, BorderLayout.NORTH);
        this.mainPanel.add(reasonsPanel, BorderLayout.CENTER);
        this.mainPanel.add(buttonsPanel, BorderLayout.SOUTH);

        this.getContentPane().add(mainPanel);
    }

    /**
     * Shows this modal dialog.
     * @return the result code, which shows what was the choice of the user
     */
    public void showDialog()
    {
        this.setVisible(true);
    }

    /**
     * Returns the code for the user interaction result.
     * @return the result of the dialog input.
     */
    public int getReturnCode()
    {
        synchronized (lock)
        {
            try
            {
                if(result == UNDEFINED_CODE)
                    lock.wait();
            }
            catch (InterruptedException e)
            {
                logger.error("Authorization request interrupted.", e);
            }
        }

        return result;
    }

    /**
     * Handles the <tt>ActionEvent</tt> triggered when one user clicks
     * on one of the buttons.
     *
     * @param e the <tt>ActionEvent</tt> that notified us
     */
    public void actionPerformed(ActionEvent e)
    {
        JButton button = (JButton)e.getSource();
        String name = button.getName();

        synchronized (lock)
        {
            if (name.equals("authorize"))
            {
                this.result = ACCEPT_CODE;
            }
            else if (name.equals("deny"))
            {
                this.result = REJECT_CODE;
            }
            else if (name.equals("ignore"))
            {
                this.result = IGNORE_CODE;
            }
            else
            {
                this.result = ERROR_CODE;
            }

            lock.notify();
        }

        this.dispose();
    }

    /**
     * Invoked when the window is closed.
     *
     * @param isEscaped indicates if the window was closed by pressing the Esc
     * key
     */
    @Override
    protected void close(boolean isEscaped)
    {
        this.ignoreButton.doClick();
    }

    /**
     * Reloads athorization icon.
     */
    public void loadSkin()
    {
        iconLabel.setIcon(new ImageIcon(
            ImageLoader.getImage(ImageLoader.AUTHORIZATION_ICON)));
    }

    /**
     * Indicates if the "Add contact" checkbox has been selected.
     *
     * @return <tt>true</tt> if the "Add contact" checkbox has been selected,
     * otherwise returns <tt>false</tt>.
     */
    public boolean isAddContact()
    {
        if (addContactCheckBox != null)
            return addContactCheckBox.isSelected();

        return false;
    }

    /**
     * Returns the currently selected group to add the contact to.
     *
     * @return the group to add the contact to
     */
    public MetaContactGroup getSelectedMetaContactGroup()
    {
        return (MetaContactGroup) groupComboBox.getSelectedItem();
    }
}
