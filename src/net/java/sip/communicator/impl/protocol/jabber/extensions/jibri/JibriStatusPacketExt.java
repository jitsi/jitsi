package net.java.sip.communicator.impl.protocol.jabber.extensions.jibri;

import net.java.sip.communicator.impl.protocol.jabber.extensions.*;
import org.jitsi.util.*;
import org.jivesoftware.smack.provider.*;

/**
 * Status extension included in MUC presence by Jibri to indicate it's status.
 * On of:
 * <li>idle</li> - the instance is idle and can be used for recording
 * <li>busy</li> - the instance is currently recording or doing something very
 *                 important and should not be disturbed
 *
 *
 */
public class JibriStatusPacketExt
    extends AbstractPacketExtension
{
    /**
     * The namespace of this packet extension.
     */
    public static final String NAMESPACE = JibriIq.NAMESPACE;

    /**
     * XML element name of this packet extension.
     */
    public static final String ELEMENT_NAME = "jibri-status";

    private static final String STATUS_ATTRIBUTE = "status";

    /**
     * Creates new instance of <tt>VideoMutedExtension</tt>.
     */
    public JibriStatusPacketExt()
    {
        super(NAMESPACE, ELEMENT_NAME);
    }

    static public void registerExtensionProvider()
    {
        ProviderManager.getInstance().addExtensionProvider(
                ELEMENT_NAME,
                NAMESPACE,
                new DefaultPacketExtensionProvider<JibriStatusPacketExt>(
                        JibriStatusPacketExt.class)
        );
    }

    public Status getStatus()
    {
        return Status.parse(getAttributeAsString(STATUS_ATTRIBUTE));
    }

    public void setStatus(Status status)
    {
        setAttribute(STATUS_ATTRIBUTE, String.valueOf(status));
    }

    public enum Status
    {
        IDLE("idle"),
        BUSY("busy"),
        UNDEFINED("undefined");

        private String name;

        Status(String name)
        {
            this.name = name;
        }

        @Override
        public String toString()
        {
            return name;
        }

        /**
         * Parses <tt>Status</tt> from given string.
         *
         * @param status the string representation of <tt>Status</tt>.
         *
         * @return <tt>Status</tt> value for given string or
         *         {@link #UNDEFINED} if given string does not
         *         reflect any of valid values.
         */
        public static Status parse(String status)
        {
            if (StringUtils.isNullOrEmpty(status))
                return UNDEFINED;

            try
            {
                return Status.valueOf(status.toUpperCase());
            }
            catch(IllegalArgumentException e)
            {
                return UNDEFINED;
            }
        }
    }
}
