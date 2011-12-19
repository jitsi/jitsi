/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.provisioning;

/**
 * Provisioning service.
 * 
 * @author Sebastien Vincent
 */
public interface ProvisioningService
{
    /**
     * Indicates if the provisioning has been enabled.
     *
     * @return <tt>true</tt> if the provisioning is enabled, <tt>false</tt> -
     * otherwise
     */
    public String getProvisioningMethod();
    
    /**
     * Enables the provisioning with the given method. If the provisioningMethod
     * is null disables the provisioning.
     *
     * @param provisioningMethod the provisioning method
     */
    public void setProvisioningMethod(String provisioningMethod);
    
    /**
     * Returns provisioning username if any.
     * 
     * @return provisioning username
     */
    public String getProvisioningUsername();
    
    /**
     * Returns provisioning password if any.
     * 
     * @return provisioning password
     */
    public String getProvisioningPassword();
    
    /**
     * Returns the provisioning URI.
     *
     * @return the provisioning URI
     */
    public String getProvisioningUri();
}
