/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.sip.xcap.model.commonpolicy;

import java.util.*;

import org.w3c.dom.*;

/**
 * The Authorization Rules identity element.
 * <p/>
 * Compliant with rfc5025
 *
 * @author Grigorii Balutsel
 */
public class IdentityType
{
    /**
     * The list of one elements.
     */
    private List<OneType> oneList;

    /**
     * The list of many elements.
     */
    private List<ManyType> manyList;

    /**
     * The list of any elements.
     */
    private List<Element> any;

    /**
     * Gets the value of the oneList property.
     *
     * @return the oneList property.
     */
    public List<OneType> getOneList()
    {
        if (oneList == null)
        {
            oneList = new ArrayList<OneType>();
        }
        return this.oneList;
    }

    /**
     * Gets the value of the manyList property.
     *
     * @return the manyList property.
     */
    public List<ManyType> getManyList()
    {
        if (manyList == null)
        {
            manyList = new ArrayList<ManyType>();
        }
        return this.manyList;
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
        return this.any;
    }
}
