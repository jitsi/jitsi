/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.sip.xcap.model.commonpolicy;

import net.java.sip.communicator.impl.protocol.sip.xcap.model.presrules.*;
import org.w3c.dom.*;

import java.util.*;

/**
 * The Authorization Rules transformations element.
 * <p/>
 * Compliant with rfc5025
 *
 * @author Grigorii Balutsel
 */
public class TransfomationsType
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
