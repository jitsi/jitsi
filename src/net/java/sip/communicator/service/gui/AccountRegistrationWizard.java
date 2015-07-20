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

import java.util.*;

import net.java.sip.communicator.service.gui.internal.*;
import net.java.sip.communicator.service.protocol.*;

/**
 * The <tt>AccountRegistrationWizard</tt> is meant to provide a wizard which
 * will guide the user through a protocol account registration. Each
 * <tt>AccountRegistrationWizard</tt> should provide a set of
 * <tt>WizardPage</tt>s, an icon, the name and the description of the
 * corresponding protocol.
 * <p>
 * Note that the <tt>AccountRegistrationWizard</tt> is NOT a real wizard, it
 * doesn't handle wizard events. Each UI Service implementation should provide
 * its own wizard UI control, which should manage all the events, panels and
 * buttons, etc.
 * <p>
 * It depends on the wizard implementation in the UI for whether or not a
 * summary will be shown to the user before "Finish".
 *
 * @author Yana Stamcheva
 */
public abstract class AccountRegistrationWizard
{
    /**
     * Is current wizard run as modification of an existing account.
     */
    private boolean isModification;

    /**
     * The parent wizard container.
     */
    private WizardContainer wizardContainer;

    /**
     * Returns the protocol icon that will be shown on the left of the protocol
     * name in the list, where user will choose the protocol to register to.
     *
     * @return a short description of the protocol.
     */
    public abstract byte[] getIcon();

    /**
     * Returns the image that will be shown on the left of the wizard pages.
     * @return the image that will be shown on the left of the wizard pages
     */
    public abstract byte[] getPageImage();

    /**
     * Returns the protocol display name that will be shown in the list,
     * where user will choose the protocol to register to.
     *
     * @return the protocol name.
     */
    public abstract String getProtocolName();

    /**
     * Returns a short description of the protocol that will be shown on the
     * right of the protocol name in the list, where user will choose the
     * protocol to register to.
     *
     * @return a short description of the protocol.
     */
    public abstract String getProtocolDescription();

    /**
     * Returns an example string, which should indicate to the user how the
     * user name should look like. For example: john@jabber.org.
     * @return an example string, which should indicate to the user how the
     * user name should look like.
     */
    public abstract String getUserNameExample();

    /**
     * Loads all data concerning the given <tt>ProtocolProviderService</tt>.
     * This method is meant to be used when a modification in an already
     * created account is needed.
     *
     * @param protocolProvider The <tt>ProtocolProviderService</tt> to
     * load data from.
     */
    public abstract void loadAccount(ProtocolProviderService protocolProvider);

    /**
     * Returns the set of <tt>WizardPage</tt>-s for this
     * wizard.
     *
     * @return the set of <tt>WizardPage</tt>-s for this
     * wizard.
     */
    public abstract Iterator<WizardPage> getPages();

    /**
     * Returns the identifier of the first account registration wizard page.
     * This method is meant to be used by the wizard container to determine,
     * which is the first page to show to the user.
     *
     * @return the identifier of the first account registration wizard page
     */
    public abstract Object getFirstPageIdentifier();

    /**
     * Returns the identifier of the last account registration wizard page. This
     * method is meant to be used by the wizard container to determine which is
     * the page to show before the summary page (of course if there's a summary).
     *
     * @return the identifier of the last account registration wizard page
     */
    public abstract Object getLastPageIdentifier();

    /**
     * Returns a set of key-value pairs that will represent the summary for
     * this wizard.
     *
     * @return a set of key-value pairs that will represent the summary for
     * this wizard.
     */
    public abstract Iterator<Map.Entry<String, String>> getSummary();

    /**
     * Defines the operations that will be executed when the user clicks on
     * the wizard "Signin" button.
     * @return the created <tt>ProtocolProviderService</tt> corresponding to the
     * new account
     * @throws OperationFailedException if the operation didn't succeed
     */
    public abstract ProtocolProviderService signin()
        throws OperationFailedException;

    /**
     * Defines the operations that will be executed when the user clicks on
     * the wizard "Signin" button.
     *
     * @param userName the user name to sign in with
     * @param password the password to sign in with
     * @return the created <tt>ProtocolProviderService</tt> corresponding to the
     * new account
     * @throws OperationFailedException if the operation didn't succeed
     */
    public abstract ProtocolProviderService signin(
            String userName,
            String password)
        throws OperationFailedException;

    /**
     * Indicates that the account corresponding to the given
     * <tt>protocolProvider</tt> has been removed.
     * @param protocolProvider the protocol provider that has been removed
     */
    public void accountRemoved(ProtocolProviderService protocolProvider) {}

    /**
     * Returns <code>true</code> if the web sign up is supported by the current
     * implementation, <code>false</code> - otherwise.
     * @return <code>true</code> if the web sign up is supported by the current
     * implementation, <code>false</code> - otherwise
     */
    public boolean isWebSignupSupported()
    {
        return false;
    }

    /**
     * Defines the operation that will be executed when user clicks on the
     * "Sign up" link.
     *
     * @throws UnsupportedOperationException if the web sign up operation is
     * not supported by the current implementation.
     */
    public void webSignup() throws UnsupportedOperationException {}

    /**
     * Returns the forgot password link name.
     *
     * @return the forgot password link name
     */
    public String getForgotPasswordLinkName()
    {
        return null;
    }

    /**
     * Returns the forgot password link if one exists.
     *
     * @return the forgot password link
     */
    public String getForgotPasswordLink()
    {
        return null;
    }

    /**
     * Returns a simple account registration form that would be the first form
     * shown to the user. Only if the user needs more settings she'll choose
     * to open the advanced wizard, consisted by all pages.
     *
     * @param isCreateAccount indicates if the simple form should be opened as
     * a create account form or as a login form
     * @return a simple account registration form
     */
    public abstract Object getSimpleForm(boolean isCreateAccount);

    /**
     * Sets the modification property to indicate if this wizard is opened for
     * a modification.
     *
     * @param isModification indicates if this wizard is opened for modification
     * or for creating a new account.
     */
    public void setModification(boolean isModification)
    {
        this.isModification = isModification;
    }

    /**
     * Indicates if this wizard is modifying an existing account or is creating
     * a new one.
     *
     * @return <code>true</code> to indicate that this wizard is currently in
     * modification mode, <code>false</code> - otherwise.
     */
    public boolean isModification()
    {
        return isModification;
    }

    /**
     * Indicates whether this wizard enables the simple "sign in" form shown
     * when the user opens the application for the first time. The simple
     * "sign in" form allows user to configure her account in one click, just
     * specifying her username and password and leaving any other configuration
     * as by default.
     * @return <code>true</code> if the simple "Sign in" form is enabled or
     * <code>false</code> otherwise.
     */
    public boolean isSimpleFormEnabled()
    {
        return true;
    }

    /**
     * Whether the advanced configuration is enabled. Gives an option
     * to disable/hide advanced config button.
     * @return whether the advanced configuration is enabled.
     */
    public boolean isAdvancedConfigurationEnabled()
    {
        return true;
    }

    /**
     * Returns the wizard container, where all pages are added.
     *
     * @return the wizard container, where all pages are added
     */
    public WizardContainer getWizardContainer()
    {
        return wizardContainer;
    }

    /**
     * Sets the wizard container, where all pages are added.
     *
     * @param wizardContainer the wizard container, where all pages are added
     */
    protected void setWizardContainer(WizardContainer wizardContainer)
    {
        this.wizardContainer = wizardContainer;
    }

    /**
     * Indicates if this wizard is for the preferred protocol.
     *
     * @return <tt>true</tt> if this wizard corresponds to the preferred
     * protocol, otherwise returns <tt>false</tt>
     */
    public boolean isPreferredProtocol()
    {
        // Check for preferred account through the PREFERRED_ACCOUNT_WIZARD
        // property.
        String prefWName = GuiServiceActivator.getResources().
            getSettingsString("impl.gui.PREFERRED_ACCOUNT_WIZARD");

        if(prefWName != null && prefWName.length() > 0
            && prefWName.equals(this.getClass().getName()))
            return true;

        return false;
    }

    /**
     * Indicates if a wizard is hidden. This may be used if we don't want that
     * a wizard appears in the list of available networks.
     *
     * @return <tt>true</tt> to indicate that a wizard is hidden, <tt>false</tt>
     * otherwise
     */
    public boolean isHidden()
    {
        return false;
    }
}
