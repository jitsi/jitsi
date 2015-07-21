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

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.protocol.*;

/**
 * We use this class as a dummy implementation of the
 * <tt>AccountRegistrationWizard</tt> only containing a blank page and not
 * related to a specific protocol. We are using this class so that we could
 * have the NewAccountDialog open without having a specific protocol selected.
 *
 * The point of having this empty page is to avoid users mistakenly filling in
 * data for the default protocol without noticing that it is not really the
 * protocol they had in mind.
 *
 * @author Emil Ivov
 * @author Yana Stamcheva
 */
class EmptyAccountRegistrationForm
    extends TransparentPanel
    implements AccountRegistrationForm
{
    /**
     * Creates the wizard.
     */
    public EmptyAccountRegistrationForm()
    {
        super(new BorderLayout());

        this.initComponents();
    }

    /**
     * Initialize the UI for the first account
     */
    private void initComponents()
    {
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
        add(infoTitlePanel, BorderLayout.NORTH);

        // Description
        firstDescription.setLineWrap(true);
        firstDescription.setEditable(false);
        firstDescription.setOpaque(false);
        firstDescription.setRows(6);
        firstDescription.setWrapStyleWord(true);
        firstDescription.setAutoscrolls(false);
        add(firstDescription);
    }

    /**
     * Called by the NewAccountDialog protocol combo renderer. We don't have an
     * icon so we return <tt>null</tt>
     *
     * @return <tt>null</tt>;
     */
    public byte[] getListIcon()
    {
        return null;
    }

    /**
     * Returns null since we don't have any images associated with this form
     * or no image in our case.
     *
     * @return an empty byte[] array.
     */
    public byte[] getIcon()
    {
        return null;
    }

    /**
     * Returns a dummy protocol description.
     *
     * @return a string containing a dummy protocol description.
     */
    public String getProtocolDescription()
    {
        return GuiActivator.getResources()
            .getI18NString("impl.gui.main.account.DUMMY_PROTOCOL_DESCRIPTION");
    }

    /**
     * Returns the name of a dummy protocol which is actually a prompt to select
     * a network.
     *
     * @return a string prompting the user to select a network.
     */
    public String getProtocolName()
    {
        return GuiActivator.getResources()
            .getI18NString("impl.gui.main.account.DUMMY_PROTOCOL_NAME");
    }

    /**
     * Returns our only wizard page.
     *
     * @return our only wizard page.
     */
    public Component getSimpleForm()
    {
        return this;
    }

    /**
     * Returns the advanced registration form.
     *
     * @return the advanced registration form
     */
    public Component getAdvancedForm()
    {
        return null;
    }

    /**
     * Returns an empty string since never used.
     *
     * @return an empty string as we never use this method.
     */
    public String getUserNameExample()
    {
        return "";
    }

    /**
     * Empty interface method implementation, unused in the case of the
     * {@link EmptyAccountRegistrationWizard}
     */
    public boolean isModification()
    {
        return false;
    }

    /**
     * Empty interface method implementation, unused in the case of the
     * {@link EmptyAccountRegistrationWizard}
     */
    public boolean isSimpleFormEnabled()
    {
        return true;
    }

    /**
     * Empty interface method implementation, unused in the case of the
     * {@link EmptyAccountRegistrationWizard}
     */
    public boolean isWebSignupSupported()
    {
        return false;
    }

    /**
     * Empty interface method implementation, unused in the case of the
     * {@link EmptyAccountRegistrationWizard}
     */
    public void loadAccount(ProtocolProviderService protocolProvider) {}

    /**
     * Empty interface method implementation, unused in the case of the
     * {@link EmptyAccountRegistrationWizard}
     */
    public void setModification(boolean isModification) {}

    /**
     * Empty interface method implementation, unused in the case of the
     * {@link EmptyAccountRegistrationWizard}
     */
    public ProtocolProviderService signin() throws OperationFailedException
    {
        return null;
    }

    /**
     * Empty interface method implementation, unused in the case of the
     * {@link EmptyAccountRegistrationWizard}
     */
    public ProtocolProviderService signin(String userName, String password)
                    throws OperationFailedException
    {
        return null;
    }

    /**
     * Empty interface method implementation, unused in the case of the
     * {@link EmptyAccountRegistrationWizard}
     */
    public void webSignup() throws UnsupportedOperationException {}
}
