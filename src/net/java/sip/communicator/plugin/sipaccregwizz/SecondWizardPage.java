/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.sipaccregwizz;

import javax.swing.*;

import net.java.sip.communicator.service.gui.*;

public class SecondWizardPage extends JPanel
    implements WizardPage {

    public static final String SECOND_PAGE_IDENTIFIER = "SecondPageIdentifier";

    public Object getIdentifier() {
        return SECOND_PAGE_IDENTIFIER;
    }

    public Object getNextPageIdentifier() {
        return FINISH_PAGE_IDENTIFIER;
    }

    public Object getBackPageIdentifier() {
        return FirstWizardPage.FIRST_PAGE_IDENTIFIER;
    }

    public Object getWizardForm() {
        return this;
    }

    public void pageHiding() {
    }

    public void pageShown() {
    }

    public void pageShowing() {
    }

    public void pageNext() {
    }

    public void pageBack() {
    }
}
