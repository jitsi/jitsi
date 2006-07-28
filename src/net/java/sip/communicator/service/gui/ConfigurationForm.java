/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.gui;

/**
 * The <tt>ConfigurationForm</tt> interface is meant to be implemented by all
 * bundles that want to add their own specific configuration forms in the UI.
 * Each <tt>ConfigurationForm</tt> implementation could be added to the UI
 * by invoking the <code>ConfigurationDialog.addConfigurationForm</code> method.
 * <p>
 * The <tt>ConfigurationDialog</tt> for the current ui implementation could
 * be obtained by invoking <code>UIService.getConfigurationDialog</code> method.
 * 
 * @author Yana Stamcheva
 */
public interface ConfigurationForm {

    /**
     * Returns the title of this configuration form.
     * @return the title of this configuration form
     */
    public String getTitle();

    /**
     * Returns the icon of this configuration form. It depends on the
     * UI implementation, how this icon will be used and where it will be
     * placed.
     * 
     * @return the icon of this configuration form
     */
    public byte[] getIcon();

    /**
     * Returns the containing form. This should be a container with all the
     * fields, buttons, etc.
     * <p>
     * Note that it's very important to return here an object that is compatible
     * with the current UI implementation library.
     * @return the containing form
     */
    public Object getForm();
}
