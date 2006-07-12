/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main.customcontrols.wizard;

/**
 * A <tt>RuntimeException</tt>, which is thrown if the wizard doesn't find
 * the panel corresponding to a given <tt>WizardPanelDescriptor</tt>.
 * 
 * @author Yana Stamcheva
 */
public class WizardPanelNotFoundException extends RuntimeException {
        
    public WizardPanelNotFoundException() {
        super();
    }
}