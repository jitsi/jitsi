/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 * 
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.replacement;

/**
 * Property for Replacement Service
 *
 * @author Purvesh Sahoo
 */
public final class ReplacementProperty
{
    /**
     *  The replacement property.
     */
    public static final String REPLACEMENT_ENABLE =
        "net.java.sip.communicator.service.replacement.enable";

    /**
     * Returns the property name of individual replacement sources
     *
     * @param source the replacement source name.
     * @return the property name of the specified source as will be stored in
     *         the properties file.
     */
    public static String getPropertyName(String source)
    {
        return "net.java.sip.communicator.service.replacement."
                + source
                + ".enable";
    }
}
