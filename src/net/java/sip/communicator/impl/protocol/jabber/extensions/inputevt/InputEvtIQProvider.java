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
package net.java.sip.communicator.impl.protocol.jabber.extensions.inputevt;

import org.jivesoftware.smack.packet.*;
import org.jivesoftware.smack.provider.*;
import org.xmlpull.v1.*;

/**
 * Implements an <tt>IQProvider</tt> which parses incoming <tt>InputEvtIQ</tt>s.
 *
 * @author Sebastien Vincent
 */
public class InputEvtIQProvider
    implements IQProvider
{
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
        InputEvtIQ inputEvtIQ = new InputEvtIQ();
        InputEvtAction action
            = InputEvtAction.parseString(
                    parser.getAttributeValue("", InputEvtIQ.ACTION_ATTR_NAME));

        inputEvtIQ.setAction(action);

        boolean done = false;

        while (!done)
        {
            switch (parser.next())
            {
            case XmlPullParser.START_TAG:
                // <remote-control>
                if (RemoteControlExtensionProvider.ELEMENT_REMOTE_CONTROL
                        .equals(parser.getName()))
                {
                    RemoteControlExtensionProvider provider
                        = new RemoteControlExtensionProvider();
                    RemoteControlExtension item
                        = (RemoteControlExtension)
                            provider.parseExtension(parser);

                    inputEvtIQ.addRemoteControl(item);
                }
                break;

            case XmlPullParser.END_TAG:
                if (InputEvtIQ.ELEMENT_NAME.equals(parser.getName()))
                    done = true;
                break;
            }
        }

        return inputEvtIQ;
    }
}
