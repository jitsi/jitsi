/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.sip.xcap.model.commonpolicy;

import org.w3c.dom.*;

import java.util.*;

/**
 * The Authorization Rules many element.
 * <p/>
 * Compliant with rfc5025
 *
 * @author Grigorii Balutsel
 */
public class ManyType
{
    /**
     * The domain attribute.
     */
    private String domain;

    /**
     * The list of excepts element.
     */
    private List<ExceptType> excepts;

    /**
     * The list of any element.
     */
    private List<Element> any;

    /**
     * Gets the value of the domain property.
     *
     * @return the domain property.
     */
    public String getDomain()
    {
        return domain;
    }

    /**
     * Sets the value of the domain property.
     *
     * @param domain the domain to set.
     */
    public void setDomain(String domain)
    {
        this.domain = domain;
    }

    /**
     * Gets the value of the excepts property.
     *
     * @return the any property.
     */
    public List<ExceptType> getExcepts()
    {
        if (this.excepts == null)
        {
            this.excepts = new ArrayList<ExceptType>();
        }
        return excepts;
    }

    /**
     * Gets the value of the any property.
     *
     * @return the any property.
     */
    public List<Element> getAny()
    {
        if (this.any == null)
        {
            this.any = new ArrayList<Element>();
        }
        return any;
    }
}
