/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.replacement.smilies;

import java.util.*;

/**
 * The <tt>Smiley</tt> interface used to represent a smiley
 * 
 * @author Yana Stamcheva
 */
public interface Smiley
{
    /**
     * Returns the description of this smiley.
     * 
     * @return the description of this smiley.
     */
    public String getDescription();

    /**
     * Returns the set of Strings corresponding to this smiley.
     * @return the set of Strings corresponding to this smiley.
     */
    public List<String> getSmileyStrings();

    /**
     * Returns the default String corresponding for this smiley. For example
     * ":-)".
     * @return the default String corresponding for this smiley.
     */
    public String getDefaultString();

    /**
     * Returns the identifier of the image corresponding to this smiley. 
     * @return the identifier of the image corresponding to this smiley.
     */
    public String getImageID();

    /**
     * Returns the path of the image corresponding to this smiley.
     * @return the path of the image corresponding to this smiley.
     */
    public String getImagePath();
}