/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.customcontrols.wizard;

import java.util.*;

/**
 * Listens for <tt>WizardEvent</tt>s triggered when a wizard is closed and
 * finished.
 * 
 * @author Yana Stamcheva
 */
public interface WizardListener extends EventListener
{
    /**
     * Listens for <tt>WizardEvent</tt>s triggered when a wizard is closed and
     * finished.
     * @param e the WizardEvent
     */
    public void wizardFinished(WizardEvent e);
}
