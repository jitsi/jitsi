/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.sip.xcap.model.commonpolicy;

import java.util.*;

/**
 * The Authorization Rules ruleset element.
 * <p/>
 * Compliant with rfc5025
 *
 * @author Grigorii Balutsel
 */
public class RulesetType
{
    /**
     * The list of rule elements.
     */
    private List<RuleType> rules;

    /**
     * Gets the value of the rules property.
     *
     * @return the rules property.
     */
    public List<RuleType> getRules()
    {
        if (rules == null)
        {
            rules = new ArrayList<RuleType>();
        }
        return this.rules;
    }
}
