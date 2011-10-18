/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.replacement.smilies;

import java.util.*;

import net.java.sip.communicator.service.replacement.*;

/**
 * 
 * @author Yana Stamcheva
 * @author Adam Netocny
 */
public interface SmiliesReplacementService
    extends ReplacementService
{
    /**
     * Returns the smileys pack to use in the user interface.
     * @return a collection of all smileys available
     */
    public Collection<Smiley> getSmiliesPack();

    /**
     * Reloads all smilies.
     */
    public void reloadSmiliesPack();
}
