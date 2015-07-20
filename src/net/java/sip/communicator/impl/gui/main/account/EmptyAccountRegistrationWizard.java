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
import java.util.*;
import java.util.Map.Entry;

import net.java.sip.communicator.impl.gui.*;
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
 */
class EmptyAccountRegistrationWizard
    extends AccountRegistrationWizard
{
    /**
     * The only page we need in this wizard, containing a prompt for the user
     * to select a wizrd.
     */
    private EmptyAccountRegistrationWizardPage page
                = new EmptyAccountRegistrationWizardPage(this);

    /**
     * A list containing the only page that this dummy wizard has.
     */
    private LinkedList<WizardPage> pages = new LinkedList<WizardPage>();


    /**
     * Creates the wizard.
     */
    public EmptyAccountRegistrationWizard()
    {
        pages.add(page);
    }

    /**
     * Returns the ID of our only page.
     *
     * @return the ID of our only page
     */
    @Override
    public Object getFirstPageIdentifier()
    {
        return page.getIdentifier();
    }

    /**
     * Called by the NewAccountDialog protocol combo renderer. We don't have an
     * icon so we return <tt>null</tt>
     *
     * @return <tt>null</tt>;
     */
    @Override
    public byte[] getIcon()
    {
        return null;
    }

    /**
     * Returns the ID of our last and only page.
     *
     * @return the id of our last (and only) page.
     */
    @Override
    public Object getLastPageIdentifier()
    {
        return EmptyAccountRegistrationWizardPage.FIRST_PAGE_IDENTIFIER;
    }

    /**
     * Returns null since we don't have any images associated with this wizard
     * or no image in our case.
     *
     * return an empty byte[] array.
     */
    @Override
    public byte[] getPageImage()
    {
        return null;
    }

    /**
     * Returns an iterator over a list containing our only page.
     *
     * @return an iterator over a list containing our only page.
     */
    @Override
    public Iterator<WizardPage> getPages()
    {
        return pages.iterator();
    }

    /**
     * Returns a dummy protocol description.
     *
     * @return a string containing a dummy protocol description.
     */
    @Override
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
    @Override
    public String getProtocolName()
    {
        return GuiActivator.getResources()
            .getI18NString("impl.gui.main.account.DUMMY_PROTOCOL_NAME");
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
    @Override
    public Object getSimpleForm(boolean isCreateAccount)
    {
        return page.getSimpleForm();
    }

    /**
     * Returns a dummy size that we never use here.
     */
    public Dimension getSize()
    {
        return new Dimension(600, 500);
    }

    /**
     * Returns a dummy <tt>Iterator</tt>. Never really called.
     *
     * @return an Empty iterator.
     */
    @Override
    public Iterator<Entry<String, String>> getSummary()
    {
        return new java.util.LinkedList<Entry<String, String>>().iterator();
    }

    /**
     * Returns an empty string since never used.
     *
     * @return an empty string as we never use this method.
     */
    @Override
    public String getUserNameExample()
    {
        return "";
    }

    /**
     * Empty interface method implementation, unused in the case of the
     * {@link EmptyAccountRegistrationWizard}
     */
    @Override
    public void loadAccount(ProtocolProviderService protocolProvider)
    {
    }

    /**
     * Empty interface method implementation, unused in the case of the
     * {@link EmptyAccountRegistrationWizard}
     */
    @Override
    public ProtocolProviderService signin() throws OperationFailedException
    {
        return null;
    }

    /**
     * Empty interface method implementation, unused in the case of the
     * {@link EmptyAccountRegistrationWizard}
     */
    @Override
    public ProtocolProviderService signin(String userName, String password)
                    throws OperationFailedException
    {
        return null;
    }
}
