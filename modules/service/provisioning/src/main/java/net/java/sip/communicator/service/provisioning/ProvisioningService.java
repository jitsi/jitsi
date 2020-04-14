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
