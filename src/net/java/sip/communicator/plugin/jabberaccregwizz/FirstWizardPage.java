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
package net.java.sip.communicator.plugin.jabberaccregwizz;

import java.awt.*;

import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.protocol.*;

/**
 * The <tt>FirstWizardPage</tt> is the page, where user could enter the user
 * ID and the password of the account.
 *
 * @author Yana Stamcheva
 * @author Damian Minkov
 */
public class FirstWizardPage
    extends TransparentPanel
    implements  WizardPage
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * Identifier of the first page.
     */
    public static final String FIRST_PAGE_IDENTIFIER = "FirstPageIdentifier";

    private Object nextPageIdentifier = WizardPage.SUMMARY_PAGE_IDENTIFIER;

    private final JabberAccountRegistrationWizard wizard;

    private final JabberAccountRegistrationForm registrationForm;

    private boolean isCommitted = false;

    /**
     * Creates an instance of <tt>FirstWizardPage</tt>.
     *
     * @param wizard the parent wizard
     */
    public FirstWizardPage(JabberAccountRegistrationWizard wizard)
    {
        super(new BorderLayout());

        this.wizard = wizard;

        this.registrationForm = new JabberAccountRegistrationForm(wizard);

        this.add(registrationForm);
    }

    /**
     * Implements the <code>WizardPage.getIdentifier</code> to return this
     * page identifier.
     *
     * @return the id of the first wizard page.
     */
    public Object getIdentifier()
    {
        return FIRST_PAGE_IDENTIFIER;
    }

    /**
     * Implements the <code>WizardPage.getNextPageIdentifier</code> to return
     * the next page identifier - the summary page.
     *
     * @return the id of the next wizard page.
     */
    public Object getNextPageIdentifier()
    {
        return nextPageIdentifier;
    }

    /**
     * Implements the <code>WizardPage.getBackPageIdentifier</code> to return
     * the back identifier. In this case it's null because this is the first
     * wizard page.
     *
     * @return the identifier of the previous wizard page
     */
    public Object getBackPageIdentifier()
    {
        return null;
    }

    /**
     * Implements the <code>WizardPage.getWizardForm</code> to return this
     * panel.
     *
     * @return this wizard page.
     */
    public Object getWizardForm()
    {
        registrationForm.init();

        return this;
    }

    /**
     * Before this page is displayed enables or disables the "Next" wizard
     * button according to whether the User ID field is empty.
     */
    public void pageShowing()
    {
        wizard.getWizardContainer().setBackButtonEnabled(false);
    }

    /**
     * Saves the user input when the "Next" wizard buttons is clicked.
     */
    public void commitPage()
    {
        isCommitted
            = registrationForm.commitPage(wizard.getRegistration());

        nextPageIdentifier = SUMMARY_PAGE_IDENTIFIER;
    }

    public void pageHiding() {}

    /**
     * Dummy implementation
     */
    public void pageShown() {}

    /**
     * Dummy implementation
     */
    public void pageBack() {}

    /**
     * Fills the User ID and Password fields in this panel with the data coming
     * from the given protocolProvider.
     *
     * @param protocolProvider The <tt>ProtocolProviderService</tt> to load
     *            the data from.
     */
    public void loadAccount(ProtocolProviderService protocolProvider)
    {
        registrationForm.setModification(wizard.isModification());

        // Loads account properties into registration object
        wizard.getRegistration()
                .loadAccount(protocolProvider.getAccountID(),
                               JabberAccRegWizzActivator.bundleContext);

        // Initialize registration form
        registrationForm.loadAccount(wizard.getRegistration());
    }

    /**
     * Returns the simple form.
     * @return the simple form
     */
    public Object getSimpleForm()
    {
        return registrationForm.getSimpleForm();
    }

    /**
     * Indicates if this page has been already committed.
     * @return <tt>true</tt> if this page has been already committed,
     * <tt>false</tt> - otherwise
     */
    public boolean isCommitted()
    {
        return isCommitted;
    }
}
