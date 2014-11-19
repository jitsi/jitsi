/*
 * Jitsi Videobridge, OpenSource video conferencing.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber.extensions.jirecon;

import org.jivesoftware.smack.packet.*;
import org.jivesoftware.smack.provider.*;

import org.xmlpull.v1.*;

/**
 * The IQ provider implementation for {@link JireconIq}.
 *
 * @author Pawel Domas
 */
public class JireconIqProvider
    implements IQProvider
{
    /**
     * Name space of Jirecon packet extension.
     */
    public static final String NAMESPACE = "http://jitsi.org/protocol/jirecon";

    /**
     * Registers this IQ provider into given <tt>ProviderManager</tt>.
     * @param providerManager the <tt>ProviderManager</tt> to which this
     *                        instance wil be bound to.
     */
    public void registerJireconIQs(ProviderManager providerManager)
    {
        // <recording/>
        providerManager.addIQProvider(
                JireconIq.ELEMENT_NAME,
                JireconIq.NAMESPACE,
                this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IQ parseIQ(XmlPullParser parser)
        throws Exception
    {
        String namespace = parser.getNamespace();

        // Check the namespace
        if (!NAMESPACE.equals(namespace))
        {
            return null;
        }

        String rootElement = parser.getName();

        JireconIq iq;

        if (JireconIq.ELEMENT_NAME.equals(rootElement))
        {
            iq = new JireconIq();

            String action
                = parser.getAttributeValue("", JireconIq.ACTION_ATTR_NAME);
            String mucjid
                = parser.getAttributeValue("", JireconIq.MUCJID_ATTR_NAME);
            String output
                = parser.getAttributeValue("", JireconIq.OUTPUT_ATTR_NAME);
            String rid
                = parser.getAttributeValue("", JireconIq.RID_ATTR_NAME);
            String status
                = parser.getAttributeValue("", JireconIq.STATUS_ATTR_NAME);

            iq.setAction(
                JireconIq.Action.parse(action));

            iq.setStatus(
                JireconIq.Status.parse(status));

            iq.setMucJid(mucjid);

            iq.setOutput(output);

            iq.setRid(rid);
        }
        else
        {
            return null;
        }

        boolean done = false;

        while (!done)
        {
            switch (parser.next())
            {
                case XmlPullParser.END_TAG:
                {
                    String name = parser.getName();

                    if (rootElement.equals(name))
                    {
                        done = true;
                    }
                    break;
                }

                case XmlPullParser.TEXT:
                {
                    // Parse some text here
                    break;
                }
            }
        }

        return iq;
    }
}
