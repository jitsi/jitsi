/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.sip.xcap.model.xcaperror;

/**
 * The XCAP xcap-error element. Indicates the reason for the error.
 * <p/>
 * Compliant with rfc4825
 *
 * @author Grigorii Balutsel
 */
public class XCapErrorType
{
    /**
     * The error element.
     */
    private XCapError error;

    /**
     * Gets the value of the error property.
     *
     * @return the error property.
     */
    public XCapError getError()
    {
        return error;
    }

    /**
     * Sets the value of the error property.
     *
     * @param error the uri to set.
     */
    public void setError(XCapError error)
    {
        this.error = error;
    }
}
