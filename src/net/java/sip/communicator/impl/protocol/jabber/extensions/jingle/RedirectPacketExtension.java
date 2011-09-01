/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber.extensions.jingle;

import net.java.sip.communicator.impl.protocol.jabber.extensions.*;

/**
 * The redirect <tt>PacketExtension</tt>.
 *
 * @author Sebastien Vincent
 */
public class RedirectPacketExtension
    extends AbstractPacketExtension
{
    /**
     * The name of the "redirect" element.
     */
    public static final String ELEMENT_NAME = "redirect";

    /**
     * The namespace.
     */
    public static final String NAMESPACE = "http://www.google.com/session";

    /**
     * The redirect text.
     */
    private String redir = null;

    /**
     * Creates a new {@link RedirectPacketExtension} instance.
     */
    public RedirectPacketExtension()
    {
        super(NAMESPACE, ELEMENT_NAME);
    }

    /**
     * Set redirection.
     *
     * @param redir redirection
     */
    public void setRedir(String redir)
    {
        this.setText(redir);
        this.redir = redir;
    }

    /**
     * Get redirection.
     *
     * @return redirection
     */
    public String getRedir()
    {
        return redir;
    }
}
