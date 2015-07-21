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
package net.java.sip.communicator.impl.protocol.sip.xcap.model.commonpolicy;

import java.util.*;

import net.java.sip.communicator.impl.protocol.sip.xcap.model.presrules.*;

import org.w3c.dom.*;

/**
 * The Authorization Rules transformations element.
 * <p/>
 * Compliant with rfc5025
 *
 * @author Grigorii Balutsel
 */
public class TransformationsType
{
    /**
     * The service-permissions element.
     */
    private ProvideServicePermissionType servicePermission;

    /**
     * The person-permissions element.
     */
    private ProvidePersonPermissionType personPermission;

    /**
     * The device-permissions element.
     */
    private ProvideDevicePermissionType devicePermission;

    /**
     * The list of any elements.
     */
    private List<Element> any;

    /**
     * Gets the value of the servicePermission property.
     *
     * @return the servicePermission property.
     */
    public ProvideServicePermissionType getServicePermission()
    {
        return servicePermission;
    }

    /**
     * Sets the value of the servicePermission property.
     *
     * @param servicePermission the servicePermission to set.
     */
    public void setServicePermission(
            ProvideServicePermissionType servicePermission)
    {
        this.servicePermission = servicePermission;
    }

    /**
     * Gets the value of the personPermission property.
     *
     * @return the personPermission property.
     */
    public ProvidePersonPermissionType getPersonPermission()
    {
        return personPermission;
    }

    /**
     * Sets the value of the personPermission property.
     *
     * @param personPermission the personPermission to set.
     */
    public void setPersonPermission(
            ProvidePersonPermissionType personPermission)
    {
        this.personPermission = personPermission;
    }

    /**
     * Gets the value of the devicePermission property.
     *
     * @return the devicePermission property.
     */
    public ProvideDevicePermissionType getDevicePermission()
    {
        return devicePermission;
    }

    /**
     * Sets the value of the devicePermission property.
     *
     * @param devicePermission the devicePermission to set.
     */
    public void setDevicePermission(
            ProvideDevicePermissionType devicePermission)
    {
        this.devicePermission = devicePermission;
    }

    /**
     * Gets the value of the any property.
     *
     * @return the any property.
     */
    public List<Element> getAny()
    {
        if (any == null)
        {
            any = new ArrayList<Element>();
        }
        return any;
    }
}
