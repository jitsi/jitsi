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
package net.java.sip.communicator.service.gui;

/**
 * The <tt>WizardPage</tt> represents a page in a <tt>WizardContainer</tt>.
 * A page has a unique identifier. Each page should specify the identifier
 * of both next and previous page to be displayed when user clicks "Next"
 * or "Back" wizard button. These identifiers will be used from the
 * <tt>WizardContainer</tt> to determine, which page should be displayed
 * when the "Next" wizard button is clicked and which when "Back" is clicked.
 * <p>
 * In the construction of an account registration wizard container the UI
 * Service implementation could implement this interface to define
 * a Default wizard page and a Summary page. Each
 * <tt>AccountRegistrationWizard</tt> could obtain the identifier of both
 * Default and Summary page from the corresponding
 * <tt>AccountRegistrationWizardContainer</tt>, where it is added.
 * <p>
 * The predefined FINISH_PAGE_IDENTIFIER should be used to mark the end of
 * a wizard.
 *
 * @author Yana Stamcheva
 */
public interface WizardPage
{
    /**
     * The identifier of the last wizard page.
     */
    String FINISH_PAGE_IDENTIFIER = "FINISH";

    /**
     * The identifier of the summary wizard page.
     */
    String SUMMARY_PAGE_IDENTIFIER = "SUMMARY";

    /**
     * The identifier of the default wizard page.
     * <p>
     * At the time of this writing, it seems from its current uses that the
     * constant indicates a <tt>null</tt> back page.
     * </p>
     */
    String DEFAULT_PAGE_IDENTIFIER = "DEFAULT";

    /**
     * Returns the identifier of this <tt>WizardPage</tt>.
     *
     * @return the identifier of this <tt>WizardPage</tt>
     */
    public Object getIdentifier();

    /**
     * Returns the identifier of the next wizard page. Meant to be used by
     * the <tt>WizardContainer</tt> to determine which is the next page to
     * display when user clicks on the "Next" wizard button.<p>
     * When this instance corresponds to the last page of the wizard this
     * method should return <tt>WizardPage.FINISH_PAGE_IDENTIFIER</tt>
     *
     * @return the identifier of the next wizard page
     */
    public Object getNextPageIdentifier();

    /**
     * Returns the identifier of the previous wizard page. Meant to be used by
     * the <tt>WizardContainer</tt> to determine which is the page to display
     * when user clicks on the "Back" wizard button.
     *
     * @return the identifier of the prevoious wizard page
     */
    public Object getBackPageIdentifier();

    /**
     * Returns the user interface form represented by this page. The form should
     * be developed by using a library that is supported from current UI Service
     * implementation. For example if the current UI Service implementation is
     * made usind Java Swing, this method should return a java.awt.Component to
     * be added properly in the <tt>WizardContainer</tt> implemented in UI
     * Service implementation.
     *
     * @return the user interface form represented by this page
     */
    public Object getWizardForm();

    /**
     * Invoked when this <tt>WizardPage</tt> will be hidden eighter because
     * the user has clicked "Back" or "Next". This method should be invoked
     * from the <tt>WizardContainer</tt> implementation just before this page
     * is hidden when replacing it with the previous or the next one.
     * <p>
     * You should add here all operations you need to be executed when this
     * <tt>WizardPage</tt> is about to be hidden.
     */
    public void pageHiding();

    /**
     * Invoked when this <tt>WizardPage</tt> is shown to the user and has
     * become the current wizard page. This method should be invoked from the
     * <tt>WizardContainer</tt> implementation just after this page is shown
     * to the user.
     * <p>
     * You should add here all operations you need to be executed when this
     * <tt>WizardPage</tt> is shown.
     */
    public void pageShown();

    /**
     * Invoked when this <tt>WizardPage</tt> will be shown eighter because
     * the user has clicked "Back" on the next wizard page or "Next" on the
     * previous one. This method should be invoked from the
     * <tt>WizardContainer</tt> implementation just before this page
     * is shown to the user.
     * <p>
     * You should add here all operations you need to be executed when this
     * <tt>WizardPage</tt> is about to be shown.
     */
    public void pageShowing();

    /**
     * Invoked when user clicks on the "Next" wizard button. You should add
     * here all operations you need to be executed when user clicks "Next" on
     * this <tt>WizardPage</tt>.
     */
    public void commitPage();

    /**
     * Invoked when user clicks on the "Back" wizard button. You should add
     * here all operations you need to be executed when user clicks "Back" on
     * this <tt>WizardPage</tt>.
     */
    public void pageBack();
}
