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
package net.java.sip.communicator.plugin.otr.authdialog;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import net.java.otr4j.session.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.plugin.otr.*;
import net.java.sip.communicator.plugin.otr.OtrContactManager.OtrContact;

/**
 * The dialog that pops up when the remote party send us SMP
 * request. It contains detailed information for the user about
 * the authentication process and allows him to authenticate.
 * 
 * @author Marin Dzhigarov
 */
@SuppressWarnings("serial")
public class SmpAuthenticateBuddyDialog
    extends SIPCommDialog
{
    private final OtrContact otrContact;

    private final String question;

    private final InstanceTag receiverTag;

    public SmpAuthenticateBuddyDialog(
        OtrContact contact, InstanceTag receiverTag, String question)
    {
        this.otrContact = contact;
        this.receiverTag = receiverTag;
        this.question = question;
        initComponents();
    }

    private void initComponents()
    {
        this.setTitle(
            OtrActivator.resourceService
                .getI18NString(
                    "plugin.otr.authbuddydialog.TITLE"));

        // The main panel that contains all components.
        JPanel mainPanel = new TransparentPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(
            BorderFactory.createEmptyBorder(10, 10, 10, 10));
        mainPanel.setPreferredSize(new Dimension(300, 350));

        // Add "authentication from contact" to the main panel.
        JTextArea authenticationFrom = new CustomTextArea();
        Font newFont =
            new Font(
                UIManager.getDefaults().getFont("TextArea.font").
                    getFontName()
                , Font.BOLD
                , 14);
        authenticationFrom.setFont(newFont);

        String resourceName = otrContact.resource != null ?
            "/" + otrContact.resource.getResourceName() : "";
        String authFromText =
            String.format(
                OtrActivator.resourceService
                    .getI18NString(
                        "plugin.otr.authbuddydialog.AUTHENTICATION_FROM",
                        new String[]
                            {otrContact.contact.getDisplayName() +
                            resourceName}));
        authenticationFrom.setText(authFromText);
        mainPanel.add(authenticationFrom);

        // Add "general info" text to the main panel.
        JTextArea generalInfo = new CustomTextArea();
        generalInfo.setText(OtrActivator.resourceService
            .getI18NString(
                "plugin.otr.authbuddydialog.AUTHENTICATION_INFO"));
        mainPanel.add(generalInfo);

        // Add "authentication-by-secret" info text to the main panel.
        JTextArea authBySecretInfo = new CustomTextArea();
        newFont =
            new Font(
                UIManager.getDefaults().getFont("TextArea.font").
                    getFontName()
                , Font.ITALIC
                , 10);
        authBySecretInfo.setText(OtrActivator.resourceService
            .getI18NString(
                "plugin.otr.authbuddydialog.AUTH_BY_SECRET_INFO_RESPOND"));
        authBySecretInfo.setFont(newFont);
        mainPanel.add(authBySecretInfo);

        // Create a panel to add question/answer related components
        JPanel questionAnswerPanel = new JPanel(new GridBagLayout());
        questionAnswerPanel.setBorder(BorderFactory.createEtchedBorder());

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(5, 5, 0, 5);
        c.weightx = 0;

        // Add question label.
        JLabel questionLabel =
            new JLabel(
                OtrActivator.resourceService
                    .getI18NString(
                        "plugin.otr.authbuddydialog.QUESTION_RESPOND"));
        questionAnswerPanel.add(questionLabel, c);

        // Add the question.
        c.insets = new Insets(0, 5, 5, 5);
        c.gridy = 1;
        JTextArea questionArea = 
            new CustomTextArea();
        newFont =
            new Font(
                UIManager.getDefaults().getFont("TextArea.font").
                    getFontName()
                , Font.BOLD
                , UIManager.getDefaults().getFont("TextArea.font")
                    .getSize());
        questionArea.setFont(newFont);
        questionArea.setText(question);
        questionAnswerPanel.add(questionArea, c);

        // Add answer label.
        c.insets = new Insets(5, 5, 5, 5);
        c.gridy = 2;
        JLabel answerLabel =
            new JLabel(OtrActivator.resourceService
                .getI18NString("plugin.otr.authbuddydialog.ANSWER"));
        questionAnswerPanel.add(answerLabel, c);

        // Add the answer text field.
        c.gridy = 3;
        final JTextField answerTextBox = new JTextField();
        questionAnswerPanel.add(answerTextBox, c);

        // Add the question/answer panel to the main panel.
        mainPanel.add(questionAnswerPanel);

        // Buttons panel.
        JPanel buttonPanel = new TransparentPanel(new GridBagLayout());
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(0, 5, 5, 5));

        JButton helpButton =
            new JButton(OtrActivator.resourceService
                .getI18NString("plugin.otr.authbuddydialog.HELP"));
        helpButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent arg0)
            {
                OtrActivator.scOtrEngine.launchHelp();
            }
        });

        c.gridwidth = 1;
        c.gridy = 0;
        c.gridx = 0;
        c.weightx = 0;
        c.insets = new Insets(5, 5, 5, 20);
        buttonPanel.add(helpButton, c);

        JButton cancelButton =
            new JButton(OtrActivator.resourceService
                .getI18NString("plugin.otr.authbuddydialog.CANCEL"));
        cancelButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                OtrActivator.scOtrEngine.abortSmp(otrContact);
                SmpAuthenticateBuddyDialog.this.dispose();
            }
        });
        c.insets = new Insets(5, 5, 5, 5);
        c.gridx = 1;
        buttonPanel.add(cancelButton, c);

        c.gridx = 2;
        JButton authenticateButton =
            new JButton(OtrActivator.resourceService
                .getI18NString(
                    "plugin.otr.authbuddydialog.AUTHENTICATE_BUDDY"));
        authenticateButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                OtrActivator.scOtrEngine.respondSmp(
                    otrContact, receiverTag, question, answerTextBox.getText());
                SmpAuthenticateBuddyDialog.this.dispose();
            }
        });

        buttonPanel.add(authenticateButton, c);

        this.getContentPane().add(mainPanel, BorderLayout.NORTH);
        this.getContentPane().add(buttonPanel, BorderLayout.SOUTH);
        this.pack();
    }
}
