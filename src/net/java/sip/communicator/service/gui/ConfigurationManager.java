/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.gui;

/**
 * The <tt>ConfigurationManager</tt> is a contianer contianing
 * <tt>ConfigurationForm</tt>s. It is meant to be implemented by the
 * UIService implementation to provide a mechanism for adding and removing
 * configuration forms in the GUI. 
 * <p>
 * A bundle could have its own specific configurations that should be configured
 * by the user. By implementing the <tt>ConfigurationForm</tt> interface
 * each bundle is able to provide its own configuration form in the UI by adding
 * it through this interface.
 * <p>
 * The <tt>ConfigurationManager</tt> for the current ui implementation could
 * be obtained by invoking <code>UIService.getConfigurationManager</code> method.
 * 
 * @author Yana Stamcheva
 */
public interface ConfigurationManager extends ExportedDialog {

    /**
     * Adds the given <tt>ConfigurationForm</tt> in this
     * <tt>ConfigurationManager</tt>. Meant to be used by bundles that want to
     * add to the UI their own specific configuration forms. 
     * @param configForm The <tt>ConfigurationForm</tt> to add.
     * @throws ClassCastException if the contained form object in the given
     * <tt>ConfigurationForm</tt> is not an instance of a class supported by
     * the service implementation.
     */
    public void addConfigurationForm(ConfigurationForm configForm)
        throws ClassCastException;
    
    /**
     * Removes the given <tt>ConfigurationForm</tt> from this
     * <tt>ConfigurationManager</tt>.
     * 
     * @param configForm The <tt>ConfigurationForm</tt> to remove.
     * @throws IllegalArgumentException if the given <tt>ConfigurationForm</tt>
     * is not contained in this <tt>ConfigurationManager</tt>.
     */
    public void removeConfigurationForm(ConfigurationForm configForm)
        throws IllegalArgumentException;
}
