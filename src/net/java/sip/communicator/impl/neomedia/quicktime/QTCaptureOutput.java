/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.quicktime;

/**
 * Represents a QTKit <tt>QTCaptureOutput</tt> object.
 *
 * @author Lubomir Marinov
 */
public class QTCaptureOutput
    extends NSObject
{

    /**
     * Initializes a new <tt>QTCaptureOutput</tt> instance which is to represent
     * a specific QTKit <tt>QTCaptureOutput</tt> object.
     *
     * @param ptr the pointer to the QTKit <tt>QTCaptureOutput</tt> object to be
     * represented by the new instance
     */
    public QTCaptureOutput(long ptr)
    {
        super(ptr);
    }
}
