/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Copyright @ 2015 Atlassian Pty Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.java.sip.communicator.plugin.addrbook.msoutlook;

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
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

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
        this(hResult, toString(hResult));
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

    /**
     * Converts a specific <tt>HRESULT</tt> to a touch more readable
     * <tt>String</tt> in accord with the rule of constructing MAPI
     * <tt>HRESULT</tt> values.
     *
     * @param hResult the <tt>HRESULT</tt> to convert
     * @return a <tt>String</tt> which represents the specified <tt>hResult</tt>
     * in a touch more readable form
     */
    private static String toString(long hResult)
    {
        if (hResult == 0)
            return "S_OK";
        else
        {
            StringBuilder s = new StringBuilder("MAPI_");

            s.append(((hResult & 0x80000000L) == 0) ? 'W' : 'E');
            s.append("_0x");
            s.append(Long.toHexString(hResult & 0xFFFL));
            s.append(" (" + Long.toHexString(hResult) + ")");
            return s.toString();
        }
    }

    /**
     * Returns the string representation for the current hResult code.
     *
     * @return The string representation for the current hResult code.
     */
    public String getHresultString()
    {
        return MsOutlookMAPIHResultException.toString(this.getHResult());
    }
}
