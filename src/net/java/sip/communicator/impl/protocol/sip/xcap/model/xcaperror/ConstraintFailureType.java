/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.sip.xcap.model.xcaperror;

/**
 * The XCAP constraint-failure element. Indicates that the requested operation
 * would result in a document that failed a data constraint defined by the
 * application usage, but not enforced by the schema or a uniqueness constraint.
 * <p/>
 * Compliant with rfc4825
 *
 * @author Grigorii Balutsel
 */
public class ConstraintFailureType extends BaseXCapError
{
    /**
     * Creates the XCAP constraint-failure with phrase attribute.
     *
     * @param phrase the phrase to set.
     */
    public ConstraintFailureType(String phrase)
    {
        super(phrase);
    }
}
