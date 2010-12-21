/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.msoutlook;

/**
 * Represents a specific Microsoft Outlook MAPI <tt>HRESULT</tt> as an
 * <tt>Exception</tt>.
 *
 * @author Lyubomir Marinov
 */
public class MsOutlookMAPIHResultException
        extends Exception
{
    /**
     * The <tt>HRESULT</tt> which is represented by this <tt>Exception</tt>.
     */
    private final long hResult;

    /**
     * Initializes a new <tt>MsOutlookMAPIHResultException</tt> instance which
     * is to represent a specific <tt>HRESULT</tt>.
     *
     * @param hResult the <tt>HRESULT</tt> to be represented by the new instance
     */
    public MsOutlookMAPIHResultException(long hResult)
    {
        this.hResult = hResult;
    }

    /**
     * Initializes a new <tt>MsOutlookMAPIHResultException</tt> instance which
     * is to represent a specific <tt>HRESULT</tt> and to provide a specific
     * <tt>String</tt> message.
     *
     * @param hResult the <tt>HRESULT</tt> to be represented by the new instance
     * @param message the <tt>String</tt> message to be provided by the new
     * instance
     */
    public MsOutlookMAPIHResultException(long hResult, String message)
    {
        super(message);

        this.hResult = hResult;
    }

    /**
     * Initializes a new <tt>MsOutlookMAPIHResultException</tt> instance with a
     * specific <tt>String</tt> message.
     *
     * @param message the <tt>String</tt> message to be provided by the new
     * instance
     */
    public MsOutlookMAPIHResultException(String message)
    {
        this(0, message);
    }

    /**
     * Gets the <tt>HRESULT</tt> which is represented by this
     * <tt>Exception</tt>.
     *
     * @return the <tt>HRESULT</tt> which is represented by this
     * <tt>Exception</tt>
     */
    public long getHResult()
    {
        return hResult;
    }
}
