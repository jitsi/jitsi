/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber.extensions.inputevt;

import org.jivesoftware.smack.packet.*;
import org.jivesoftware.smack.provider.*;

import org.xmlpull.v1.*;

/**
 * An implementation of a InputEvt IQ provider that parses incoming Input IQs.
 *
 * @author Sebastien Vincent
 */
public class InputEvtIQProvider implements IQProvider
{
    /**
     * Constructs a new InputEvtIQ provider.
     */
    public InputEvtIQProvider()
    {
/*
        ProviderManager providerManager = ProviderManager.getInstance();

        providerManager.addExtensionProvider(
                InputExtensionProvider.ELEMENT_REMOTE_CONTROL,
                InputExtensionProvider.NAMESPACE,
                new InputExtensionProvider());
*/
    }

    /**
     * Parse the Input IQ sub-document and returns the corresponding
     * <tt>InputEvtIQ</tt>.
     *
     * @param parser XML parser
     * @return <tt>InputEvtIQ</tt>
     * @throws Exception if something goes wrong during parsing
     */
    public IQ parseIQ(XmlPullParser parser) throws Exception
    {
        InputEvtIQ inputIQ = new InputEvtIQ();
        boolean done = false;
        RemoteControlExtensionProvider provider = new RemoteControlExtensionProvider();
        InputEvtAction action = InputEvtAction.parseString(parser
                        .getAttributeValue("", InputEvtIQ.ACTION_ATTR_NAME));

        inputIQ.setAction(action);

        int eventType;
        String elementName;

        while (!done)
        {
            eventType = parser.next();
            elementName = parser.getName();

            if (eventType == XmlPullParser.START_TAG)
            {
                // <remote-control/>
                if (elementName.equals(
                        RemoteControlExtensionProvider.ELEMENT_REMOTE_CONTROL))
                {
                    RemoteControlExtension item =
                        (RemoteControlExtension)provider.parseExtension(parser);
                    inputIQ.addRemoteControl(item);
                }
            }

            if (eventType == XmlPullParser.END_TAG)
            {
                if (parser.getName().equals(InputEvtIQ.ELEMENT_NAME))
                {
                    done = true;
                }
            }
        }

        return inputIQ;
    }
}
