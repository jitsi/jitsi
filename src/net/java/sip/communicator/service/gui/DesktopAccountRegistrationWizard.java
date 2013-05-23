/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.gui;

import java.awt.*;

/**
 * The <tt>DesktopAccountRegistrationWizard</tt> extends the
 * <tt>AccountRegistrationWizard</tt> to provide a desktop specific account
 * registration. It is meant to provide a wizard which will guide the user
 * through a protocol account registration. Each
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
public abstract class DesktopAccountRegistrationWizard
    extends AccountRegistrationWizard
{
    /**
     * Returns the preferred dimensions of this wizard.
     *
     * @return the preferred dimensions of this wizard.
     */
    public abstract Dimension getSize();
}
