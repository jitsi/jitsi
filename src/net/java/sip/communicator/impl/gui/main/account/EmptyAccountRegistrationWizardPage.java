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
package net.java.sip.communicator.impl.gui.main.account;

import java.awt.*;

import javax.swing.*;
import javax.swing.event.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.gui.*;

/**
 * The <tt>EmptyAccountRegistrationWizardPage</tt> is the page that users
 * would see in the NewAccountDialog as a default choice which would make them
 * pick a new option.
 *
 * @author Emil Ivov
 */
public class EmptyAccountRegistrationWizardPage
    implements WizardPage
{
    public static final String FIRST_PAGE_IDENTIFIER = "FirstPageIdentifier";

    private JPanel mainPanel = new TransparentPanel(new BorderLayout());

    /**
     * Creates an instance of <tt>FirstWizardPage</tt>.
     *
     * @param wizard the parent wizard
     */
    public EmptyAccountRegistrationWizardPage(
                        EmptyAccountRegistrationWizard wizard)
    {

        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        this.initComponents();
    }

    /**
     * Initialize the UI for the first account
     */
    private void initComponents()
    {
        // Init strategies list
        this.mainPanel = new TransparentPanel(new BorderLayout());

        JPanel infoTitlePanel
            = new TransparentPanel(new FlowLayout(FlowLayout.CENTER));
        JTextArea firstDescription
            = new JTextArea(GuiActivator.getResources().getI18NString(
                "impl.gui.main.account.DEFAULT_PAGE_BODY"));
        JLabel title
            = new JLabel(GuiActivator.getResources().getI18NString(
                "impl.gui.main.account.DEFAULT_PAGE_TITLE"));

        // Title
        title.setFont(title.getFont().deriveFont(Font.BOLD, 14.0f));
        infoTitlePanel.add(title);
        this.mainPanel.add(infoTitlePanel, BorderLayout.NORTH);

        // Description
        firstDescription.setLineWrap(true);
        firstDescription.setEditable(false);
        firstDescription.setOpaque(false);
        firstDescription.setRows(6);
        firstDescription.setWrapStyleWord(true);
        firstDescription.setAutoscrolls(false);
        this.mainPanel.add(firstDescription);
    }

    /**
     * Returns the <tt>JPanel</tt> that contains the message prompting the user
     * to select a protocol.
     *
     * @return the <tt>JPanel</tt> that contains the message prompting the user
     * to select a protocol.
     */
    public Object getSimpleForm()
    {
        return mainPanel;
    }

    /**
     * Implements the <code>WizardPage.getIdentifier</code> to return this
     * page identifier.
     *
     * @return Returns the identifier of the current (the first) page of the
     * wizard.
     */
    public Object getIdentifier()
    {
        return FIRST_PAGE_IDENTIFIER;
    }

    /**
     * Implements <tt> WizardPage.getNextPageIdentifier</tt> to return
     * the next page identifier - the summary page.
     *
     * @return Returns the identifier of the next page of the wizard.
     */
    public Object getNextPageIdentifier()
    {
        return WizardPage.SUMMARY_PAGE_IDENTIFIER;
    }

    /**
     * Implements the <code>WizardPage.getBackPageIdentifier</code> to return
     * the back identifier. In this case it's null because this is the first
     * wizard page.
     *
     * @return the identifier of the previous page of the wizard
     */
    public Object getBackPageIdentifier()
    {
        return null;
    }

    /**
     * Implements the <code>WizardPage.getWizardForm</code> to return this
     * panel.
     * @return Returns this form of the wizard.
     */
    public Object getWizardForm()
    {
        return this;
    }

    /**
     * Empty interface method implementation, unused in the case of the
     * {@link EmptyAccountRegistrationWizardPage}
     */
    public void pageShowing()
    {
    }

    /**
     * Empty interface method implementation, unused in the case of the
     * {@link EmptyAccountRegistrationWizardPage}
     */
    public void commitPage()
    {
    }

    /**
     * Empty interface method implementation, unused in the case of the
     * {@link EmptyAccountRegistrationWizardPage}
     */
    public void changedUpdate(DocumentEvent e)
    {
    }

    /**
     * Empty interface method implementation, unused in the case of the
     * {@link EmptyAccountRegistrationWizardPage}
     */
    public void pageHiding()
    {
    }

    /**
     * Empty interface method implementation, unused in the case of the
     * {@link EmptyAccountRegistrationWizardPage}
     */
    public void pageShown()
    {
    }

    /**
     * Empty interface method implementation, unused in the case of the
     * {@link EmptyAccountRegistrationWizardPage}
     */
    public void pageBack()
    {
    }
}
