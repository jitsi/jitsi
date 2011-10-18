/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.sip.xcap.model.xcaperror;

/**
 * The XCAP no-parent element. Indicates that an attempt to insert a document,
 * element, or attribute failed because the directory, document, or element into
 * which the insertion was supposed to occur does not exist. This error element
 * can contain an optional ancestor element, which provides an HTTP URI that
 * represents the closest parent that would be a valid point of insertion. This
 * HTTP URI MAY be a relative URI, relative to the document itself. Because this
 * is a valid HTTP URI, its node selector component MUST be percent-encoded.
 * <p/>
 * Compliant with rfc4825
 *
 * @author Grigorii Balutsel
 */
public class NoParentType extends BaseXCapError
{
    /**
     * The ancestor element. HTTP uri that represents the closest parent that
     * would be a valid point of insertion.
     */
    protected String ancestor;

    NoParentType()
    {
        super(null);
    }

    /**
     * Creates the XCAP no-parent error with phrase attribute.
     *
     * @param phrase the phrase to set.
     */
    public NoParentType(String phrase)
    {
        super(phrase);
    }

    /**
     * Gets the phrase attribute.
     *
     * @return User readable error description.
     */
    public String getAncestor()
    {
        return ancestor;
    }

    /**
     * Sets the value of the ancestor property.
     *
     * @param ancestor the ancestor to set.
     */
    void setAncestor(String ancestor)
    {
        this.ancestor = ancestor;
    }
}
