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
package net.java.sip.communicator.impl.protocol.jabber.extensions.jibri;

import org.jivesoftware.smack.packet.*;

import java.util.*;

/**
 * Wraps Smack's <tt>XMPPError</tt> into <tt>PacketExtension</tt>, so that it
 * can be easily inserted into {@link RecordingStatus}.
 */
public class XMPPErrorPE
    implements PacketExtension
{
    /**
     * <tt>XMPPError</tt> wrapped into this <tt>XMPPErrorPE</tt>.
     */
    private XMPPError error;

    /**
     * Creates new instance of <tt>XMPPErrorPE</tt>.
     * @param xmppError the instance of <tt>XMPPError</tt> that will be wrapped
     * by the newly created <tt>XMPPErrorPE</tt>.
     */
    public XMPPErrorPE(XMPPError xmppError)
    {
        setError(xmppError);
    }

    /**
     * Returns the underlying instance of <tt>XMPPError</tt>.
     */
    public XMPPError getError()
    {
        return error;
    }

    /**
     * Sets new instance of <tt>XMPPError</tt> to be wrapped by this
     * <tt>XMPPErrorPE</tt>.
     * @param error <tt>XMPPError</tt> that will be wrapped by this
     * <TT>XMPPErrorPE</TT>.
     */
    public void setError(XMPPError error)
    {
        Objects.requireNonNull(error, "error");

        this.error = error;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getElementName()
    {
        return "error";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getNamespace()
    {
        return "";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toXML()
    {
        return error.toXML();
    }
}
