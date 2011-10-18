/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.sip.xcap.model.xcaperror;

/**
 * The base XCAP error.
 *
 * @author Grigorii Balutsel
 */
public abstract class BaseXCapError implements XCapError
{
    /**
     * The phrase attribute.
     */
    private String phrase;

    /**
     * Creates the XCAP error with phrase attribute.
     *
     * @param phrase the phrase to set.
     */
    public BaseXCapError(String phrase)
    {
        this.phrase = phrase;
    }

    /**
     * Gets the phrase attribute.
     *
     * @return User readable error description.
     */
    public String getPhrase()
    {
        return phrase;
    }

    /**
     * Sets the value of the phrase property.
     *
     * @param phrase the phrase to set.
     */
    void setPhrase(String phrase)
    {
        this.phrase = phrase;
    }
}
