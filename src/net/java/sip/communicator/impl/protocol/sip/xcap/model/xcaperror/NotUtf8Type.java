/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.sip.xcap.model.xcaperror;

/**
 * The XCAP not-utf-8 element. Indicates that the request could not be completed
 * because it would have produced a document not encoded in UTF-8.
 * <p/>
 * Compliant with rfc4825
 *
 * @author Grigorii Balutsel
 */
public class NotUtf8Type extends BaseXCapError
{
    /**
     * Creates the XCAP not-utf-8 error with phrase attribute.
     *
     * @param phrase the phrase to set.
     */
    public NotUtf8Type(String phrase)
    {
        super(phrase);
    }
}
