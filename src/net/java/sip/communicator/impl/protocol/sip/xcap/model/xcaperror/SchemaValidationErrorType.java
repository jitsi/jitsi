/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.sip.xcap.model.xcaperror;

/**
 * The XCAP schema-validation-error element. Indicates that the document was not
 * compliant to the schema after the requested operation was performed.
 * <p/>
 * Compliant with rfc4825
 *
 * @author Grigorii Balutsel
 */
public class SchemaValidationErrorType extends BaseXCapError
{
    /**
     * Creates the XCAP schema-validation-error error with phrase attribute.
     *
     * @param phrase the phrase to set.
     */
    public SchemaValidationErrorType(String phrase)
    {
        super(phrase);
    }
}
