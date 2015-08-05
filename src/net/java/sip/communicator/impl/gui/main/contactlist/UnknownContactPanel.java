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
package net.java.sip.communicator.impl.gui.main.contactlist;

import java.awt.*;
import java.awt.event.*;
import java.util.List;

import javax.swing.*;
import javax.swing.text.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.main.*;
import net.java.sip.communicator.impl.gui.main.call.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.plugin.desktoputil.event.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.skin.*;
import org.jitsi.util.*;

/**
 * The <tt>UnknownContactPanel</tt> replaces the contact list, when a
 * <tt>SearchFilter</tt> founds no matches. It is meant to propose the user
 * some alternatives if she's looking for a contact, which is not contained in
 * the contact list.
 *
 * @author Yana Stamcheva
 * @author Adam Netocny
 */
public class UnknownContactPanel
    extends TransparentPanel
    implements  TextFieldChangeListener,
                Skinnable
{
    private final JButton addButton = new JButton(
        GuiActivator.getResources().getI18NString("service.gui.ADD_CONTACT"));

    private final JButton callButton = new JButton(
        GuiActivator.getResources().getI18NString("service.gui.CALL_CONTACT"));

    private JButton smsButton;

    private final JTextPane textArea = new JTextPane();

    private final TransparentPanel buttonPanel
        = new TransparentPanel(new GridLayout(0, 1));

    /**
     * The main application window.
     */
    private MainFrame parentWindow;

    /**
     * An empty constructor allowing to extend this class.
     */
    public UnknownContactPanel() {}

    /**
     * Creates the <tt>UnknownContactPanel</tt> by specifying the parent window.
     * @param window the parent window
     */
    public UnknownContactPanel(MainFrame window)
    {
        super(new BorderLayout());

        this.parentWindow = window;

        TransparentPanel mainPanel = new TransparentPanel(new BorderLayout());

        this.add(mainPanel, BorderLayout.NORTH);

        if (!ConfigurationUtils.isAddContactDisabled())
        {
            initAddContactButton();
        }

        initCallButton();

        initSMSButton();

        initTextArea();
        mainPanel.add(textArea, BorderLayout.CENTER);

        if (callButton.getParent() != null)
        {
            textArea.setText(GuiActivator.getResources()
                .getI18NString( "service.gui.NO_CONTACTS_FOUND",
                    new String[]{'"'
                                + parentWindow.getCurrentSearchText() + '"'}));
        }
        else
        {
            textArea.setText(GuiActivator.getResources()
                .getI18NString( "service.gui.NO_CONTACTS_FOUND_SHORT"));
        }

        if (buttonPanel.getComponentCount() > 0)
        {
            TransparentPanel southPanel
                = new TransparentPanel(new FlowLayout(FlowLayout.CENTER));
            southPanel.add(buttonPanel);

            mainPanel.add(southPanel, BorderLayout.SOUTH);
        }

        loadSkin();
    }

    private void initAddContactButton()
    {
        addButton.setAlignmentX(JButton.CENTER_ALIGNMENT);

        addButton.setMnemonic(GuiActivator.getResources()
            .getI18nMnemonic("service.gui.ADD_CONTACT"));

        buttonPanel.add(addButton);

        addButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                AddContactDialog dialog
                    = new AddContactDialog(parentWindow);

                dialog.setContactAddress(
                    parentWindow.getCurrentSearchText());
                dialog.setVisible(true);
            }
        });
    }

    /**
     * Initializes the call button.
     */
    private void initCallButton()
    {
        List<ProtocolProviderService> telephonyProviders
            = CallManager.getTelephonyProviders();

        if (telephonyProviders!= null && telephonyProviders.size() > 0)
        {
            if (callButton.getParent() != null)
                return;

            callButton.setAlignmentX(JButton.CENTER_ALIGNMENT);

            callButton.setMnemonic(GuiActivator.getResources()
                .getI18nMnemonic("service.gui.CALL_CONTACT"));

            buttonPanel.add(callButton);

            callButton.addActionListener(new ActionListener()
            {
                public void actionPerformed(ActionEvent e)
                {
                    String searchText = parentWindow.getCurrentSearchText();

                    if (searchText == null)
                        return;

                    CallManager.createCall(searchText, callButton);
                }
            });
        }
        else
        {
            buttonPanel.remove(callButton);
        }
    }

    /**
     * Initializes the call button.
     */
    private void initSMSButton()
    {
        if(parentWindow.hasOperationSet(OperationSetSmsMessaging.class)
            && !StringUtils.containsLetters(
                    parentWindow.getCurrentSearchText()))
        {
            if (smsButton != null && smsButton.getParent() != null)
                return;

            smsButton = new JButton(GuiActivator.getResources()
                .getI18NString("service.gui.SEND_SMS"));

            smsButton.setIcon(GuiActivator.getResources()
                .getImage("service.gui.icons.SMS_BUTTON_ICON"));

            buttonPanel.add(smsButton);

            smsButton.addActionListener(new ActionListener()
            {
                @Override
                public void actionPerformed(ActionEvent e)
                {
                    final String searchText
                        = parentWindow.getCurrentSearchText();

                    if(searchText == null)
                        return;

                    SMSManager.sendSMS(smsButton, searchText);
                }
            });
        }
        else
        {
            if(smsButton != null)
                buttonPanel.remove(smsButton);
        }
    }

    /**
     * Clicks the call contact button in order to call the unknown contact.
     */
    public void startCall()
    {
        callButton.doClick();
    }

    /**
     * Clicks the add contact button in order to add the unknown contact
     * to the contact list.
     */
    public void addUnknownContact()
    {
        addButton.doClick();
    }

    /**
     * Invoked when any text is inserted in the search field.
     */
    public void textInserted()
    {
        updateTextArea(parentWindow.getCurrentSearchText());
    }

    /**
     * Invoked when any text is removed from the search field.
     */
    public void textRemoved()
    {
        updateTextArea(parentWindow.getCurrentSearchText());
    }

    /**
     * Creates the text area.
     */
    private void initTextArea()
    {
        textArea.setOpaque(false);
        textArea.setEditable(false);
        StyledDocument doc = textArea.getStyledDocument();

        MutableAttributeSet standard = new SimpleAttributeSet();
        StyleConstants.setAlignment(standard, StyleConstants.ALIGN_CENTER);
        StyleConstants.setFontFamily(standard, textArea.getFont().getFamily());
        StyleConstants.setFontSize(standard, 12);
        doc.setParagraphAttributes(0, 0, standard, true);

        parentWindow.addSearchFieldListener(this);
    }

    /**
     * Updates the text area to take into account the new search text.
     * @param searchText the search text to update
     */
    private void updateTextArea(String searchText)
    {
        if (callButton.getParent() != null)
        {
            textArea.setText(GuiActivator.getResources()
                .getI18NString("service.gui.NO_CONTACTS_FOUND",
                    new String[]{'"' + searchText + '"'}));

            this.revalidate();
            this.repaint();
        }
    }

    /**
     * Reloads button resources.
     */
    public void loadSkin()
    {
        addButton.setIcon(GuiActivator.getResources()
            .getImage("service.gui.icons.ADD_CONTACT_16x16_ICON"));
        callButton.setIcon(GuiActivator.getResources()
            .getImage("service.gui.icons.CALL_16x16_ICON"));

        if(smsButton != null)
            smsButton.setIcon(GuiActivator.getResources()
                .getImage("service.gui.icons.SMS_BUTTON_ICON"));
    }

    /**
     * Updates the call button appearance and shows/hides this panel.
     *
     * @param isVisible indicates if this panel should be shown or hidden
     */
    @Override
    public void setVisible(boolean isVisible)
    {
        if (isVisible)
        {
            initCallButton();
            initSMSButton();
        }

        super.setVisible(isVisible);
    }
}
