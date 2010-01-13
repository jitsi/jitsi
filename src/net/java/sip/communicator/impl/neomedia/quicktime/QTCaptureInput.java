/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia.quicktime;

/**
 * Represents a QTKit <tt>QTCaptureInput</tt> object.
 *
 * @author Lubomir Marinov
 */
public class QTCaptureInput
    extends NSObject
{

    /**
     * Initializes a new <tt>QTCaptureInput</tt> instance which is to represent
     * a specific QTKit <tt>QTCaptureInput</tt> object.
     *
     * @param ptr the pointer to the QTKit <tt>QTCaptureInput</tt> object to be
     * represented by the new instance
     */
    public QTCaptureInput(long ptr)
    {
        super(ptr);
    }
}
