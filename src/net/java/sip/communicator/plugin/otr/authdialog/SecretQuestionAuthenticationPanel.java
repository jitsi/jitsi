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

import javax.swing.*;

import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.plugin.otr.*;

/**
 * @author Marin Dzhigarov
 */
@SuppressWarnings("serial")
public class SecretQuestionAuthenticationPanel
    extends TransparentPanel
{
    /**
     * The text field where the authentication initiator will type his question.
     */
    private final JTextField question = new JTextField();

    /**
     * The text field where the authentication initiator will type his answer.
     */
    private final JTextField answer = new JTextField();


    /**
     * Creates an instance SecretQuestionAuthenticationPanel.
     */
    SecretQuestionAuthenticationPanel()
    {
        initComponents();
    }

    /**
     * Initializes the {@link SecretQuestionAuthenticationPanel} components.
     */
    private void initComponents()
    {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        JTextArea generalInformation = new CustomTextArea();
        generalInformation.setText(
            OtrActivator.resourceService
                .getI18NString(
                    "plugin.otr.authbuddydialog.AUTH_BY_QUESTION_INFO_INIT"));
        this.add(generalInformation);

        this.add(Box.createVerticalStrut(10));

        JPanel questionAnswerPanel = new JPanel(new GridBagLayout());
        questionAnswerPanel.setBorder(BorderFactory.createEtchedBorder());

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(5, 5, 0, 5);
        c.weightx = 1;

        JLabel questionLabel =
            new JLabel(
                OtrActivator.resourceService
                    .getI18NString(
                        "plugin.otr.authbuddydialog.QUESTION_INIT"));
        questionAnswerPanel.add(questionLabel, c);

        c.gridy = 1;
        c.insets = new Insets(0, 5, 5, 5);
        questionAnswerPanel.add(question, c);

        c.gridy = 2;
        c.insets = new Insets(5, 5, 0, 5);
        JLabel answerLabel =
            new JLabel(
                OtrActivator.resourceService
                    .getI18NString(
                        "plugin.otr.authbuddydialog.ANSWER"));
        questionAnswerPanel.add(answerLabel, c);

        c.gridy = 3;
        c.insets = new Insets(0, 5, 5, 5);
        questionAnswerPanel.add(answer, c);

        this.add(questionAnswerPanel);
        this.add(new Box.Filler(
            new Dimension(300, 100),
            new Dimension(300, 100),
            new Dimension(300, 100)));
    }

    /**
     * Returns the secret answer text.
     * 
     * @return The secret answer text.
     */
    String getSecret()
    {
        return answer.getText();
    }

    /**
     * Returns the secret question text.
     * 
     * @return The secret question text.
     */
    String getQuestion()
    {
        return question.getText();
    }
}
