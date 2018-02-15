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

package net.java.sip.communicator.impl.protocol.jabber.extensions.colibri;

import junit.framework.TestCase;
import org.jivesoftware.smack.packet.IQ;
import org.xmlpull.mxp1.MXParser;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
//import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;

public class ColibriIQProviderTest extends TestCase
{
    String testXml = "\n" +
            "<iq type=\"set\" to=\"jvb-0.brian2.jitsi.net\" from=\"focus@auth.brian2.jitsi.net/focus358522582823562\" id=\"qV8NP-1407\">" +
              "<conference xmlns=\"http://jitsi.org/protocol/colibri\" id=\"cce6f2fe74002273\" name=\"test\" gid=\"ff62ef\">" +
                "<content name=\"audio\">" +
                  "<channel id=\"b9007c3af259d44e\">" +
                    "<payload-type name=\"opus\" clockrate=\"48000\" id=\"111\" channels=\"2\">" +
                      "<parameter value=\"48000\" name=\"maxplaybackrate\"/>" +
                      "<parameter value=\"1\" name=\"stereo\"/>" +
                      "<parameter value=\"1\" name=\"useinbandfec\"/>" +
                    "</payload-type>" +
                    "<payload-type name=\"telephone-event\" clockrate=\"8000\" id=\"126\" channels=\"1\">" +
                      "<parameter value=\"0-15\" name=\"\"/>" +
                    "</payload-type>" +
                    "<source xmlns=\"urn:xmpp:jingle:apps:rtp:ssma:0\" ssrc=\"803212360\">" +
                      "<parameter value=\"5311ad66-bc71-7d4e-be71-66ac3a73182a\" name=\"cname\"/>" +
                      "<parameter value=\"cb031733-052e-6840-9d69-d6b2cba08952 35eef4c8-4718-114b-893f-76642f230cd0\" name=\"msid\"/>" +
                      "<ssrc-info xmlns=\"http://jitsi.org/jitmeet\" owner=\"test@conference.brian2.jitsi.net/66e3ea10\"/>" +
                    "</source>" +
                  "</channel>" +
                "</content>" +
                "<content name=\"video\">" +
                  "<channel id=\"762b391899b4efa2\">" +
                    "<payload-type name=\"VP8\" clockrate=\"90000\" id=\"100\" channels=\"1\">" +
                      "<parameter value=\"12288\" name=\"max-fs\"/>" +
                      "<parameter value=\"60\" name=\"max-fr\"/>" +
                      "<rtcp-fb xmlns=\"urn:xmpp:jingle:apps:rtp:rtcp-fb:0\" type=\"nack\"/>" +
                      "<rtcp-fb xmlns=\"urn:xmpp:jingle:apps:rtp:rtcp-fb:0\" type=\"nack\" subtype=\"pli\"/>" +
                      "<rtcp-fb xmlns=\"urn:xmpp:jingle:apps:rtp:rtcp-fb:0\" type=\"ccm\" subtype=\"fir\"/>" +
                      "<rtcp-fb xmlns=\"urn:xmpp:jingle:apps:rtp:rtcp-fb:0\" type=\"goog-remb\"/>" +
                    "</payload-type>" +
                    "<source xmlns=\"urn:xmpp:jingle:apps:rtp:ssma:0\" ssrc=\"246878015\">" +
                      "<parameter value=\"5311ad66-bc71-7d4e-be71-66ac3a73182a\" name=\"cname\"/>" +
                      "<parameter value=\"404a72b1-a51a-a545-a5e9-5a50cd94431c 135dd87a-1c7a-fb4d-9e42-d64cfbcfcc0d\" name=\"msid\"/>" +
                      "<ssrc-info xmlns=\"http://jitsi.org/jitmeet\" owner=\"test@conference.brian2.jitsi.net/66e3ea10\"/>" +
                    "</source>" +
                    "<source xmlns=\"urn:xmpp:jingle:apps:rtp:ssma:0\" ssrc=\"3236476221\">" +
                      "<parameter value=\"5311ad66-bc71-7d4e-be71-66ac3a73182a\" name=\"cname\"/>" +
                      "<parameter value=\"404a72b1-a51a-a545-a5e9-5a50cd94431c 135dd87a-1c7a-fb4d-9e42-d64cfbcfcc0d\" name=\"msid\"/>" +
                      "<ssrc-info xmlns=\"http://jitsi.org/jitmeet\" owner=\"test@conference.brian2.jitsi.net/66e3ea10\"/>" +
                    "</source>" +
                    "<source xmlns=\"urn:xmpp:jingle:apps:rtp:ssma:0\" ssrc=\"3703044675\">" +
                      "<parameter value=\"5311ad66-bc71-7d4e-be71-66ac3a73182a\" name=\"cname\"/>" +
                      "<parameter value=\"404a72b1-a51a-a545-a5e9-5a50cd94431c 135dd87a-1c7a-fb4d-9e42-d64cfbcfcc0d\" name=\"msid\"/>" +
                      "<ssrc-info xmlns=\"http://jitsi.org/jitmeet\" owner=\"test@conference.brian2.jitsi.net/66e3ea10\"/>" +
                    "</source>" +
                    "<source xmlns=\"urn:xmpp:jingle:apps:rtp:ssma:0\" rid=\"1\">" +
                      "<ssrc-info xmlns=\"http://jitsi.org/jitmeet\" owner=\"test@conference.brian2.jitsi.net/66e3ea10\"/>" +
                    "</source>" +
                    "<source xmlns=\"urn:xmpp:jingle:apps:rtp:ssma:0\" rid=\"2\">" +
                      "<ssrc-info xmlns=\"http://jitsi.org/jitmeet\" owner=\"test@conference.brian2.jitsi.net/66e3ea10\"/>" +
                    "</source>" +
                    "<source xmlns=\"urn:xmpp:jingle:apps:rtp:ssma:0\" rid=\"3\">" +
                      "<ssrc-info xmlns=\"http://jitsi.org/jitmeet\" owner=\"test@conference.brian2.jitsi.net/66e3ea10\"/>" +
                    "</source>" +
                    "<ssrc-group xmlns=\"urn:xmpp:jingle:apps:rtp:ssma:0\" semantics=\"SIM\">" +
                      "<source ssrc=\"246878015\"/>" +
                      "<source ssrc=\"3236476221\"/>" +
                      "<source ssrc=\"3703044675\"/>" +
                    "</ssrc-group>" +
                    "<rid-group xmlns=\"urn:xmpp:jingle:apps:rtp:ssma:0\" semantics=\"SIM\">" +
                      "<source rid=\"1\"/>" +
                      "<source rid=\"2\"/>" +
                      "<source rid=\"3\"/>" +
                    "</rid-group>" +
                  "</channel>" +
                "</content>" +
                "<channel-bundle id=\"66e3ea10\">" +
                  "<transport xmlns=\"urn:xmpp:jingle:transports:ice-udp:1\" ufrag=\"117916df\" pwd=\"15929b4d44ae40fbcc6d51b6e4a468aa\">" +
                    "<rtcp-mux/>" +
                    "<fingerprint xmlns=\"urn:xmpp:jingle:apps:dtls:0\" hash=\"sha-256\" setup=\"active\" required=\"false\">44:AF:49:E3:3B:E0:0D:A2:FA:AB:F4:93:EC:5D:32:39:78:F8:01:06:1F:8E:E4:35:36:15:56:59:6B:3C:52:49</fingerprint>" +
                  "</transport>" +
                "</channel-bundle>" +
              "</conference>" +
            "</iq>";

//    XmlPullParserFactory xmlPullParserFactory;
    XmlPullParser xmlPullParser;
    ColibriIQProvider colibriIQProvider;

    public void setUp()
            throws Exception
    {
//        xmlPullParserFactory = XmlPullParserFactory.newInstance();
//        xmlPullParserFactory.setNamespaceAware(true);
//
//        xmlPullParser = xmlPullParserFactory.newPullParser();
        xmlPullParser = new MXParser();
        xmlPullParser.setFeature(
            "http://xmlpull.org/v1/doc/features.html#process-namespaces",
            true);

        colibriIQProvider = new ColibriIQProvider();
    }

    public void testParseSource()
            throws Exception
    {
        // Make sure that both ssrc and rid sources are parsed correctly
        xmlPullParser.setInput(new StringReader(testXml));
        // Step forward to the the 'iq' element
        int eventType = xmlPullParser.next();
        String name = xmlPullParser.getName();
        assertEquals(XmlPullParser.START_TAG, eventType);
        assertEquals("iq", name);

        // Move forward to the 'conference' element, which is what
        // ColibriIQProvider::parse expects
        eventType = xmlPullParser.next();
        name = xmlPullParser.getName();
        assertEquals(XmlPullParser.START_TAG, eventType);
        assertEquals(ColibriConferenceIQ.ELEMENT_NAME, name);

        IQ result = colibriIQProvider.parse(xmlPullParser, 0);
        List<SourcePacketExtension> sources =
                ((ColibriConferenceIQ) result)
                        .getContent("video")
                        .getChannel(0)
                        .getSources();
        // There are 6 video sources in testXml, 3 ssrc and 3 rid
        assertEquals(6, sources.size());
        int numSsrcSources = 0;
        int numRidSources = 0;
        for (SourcePacketExtension s : sources)
        {
            if (s.hasRid())
            {
                ++numRidSources;
            }
            if (s.hasSSRC())
            {
                ++numSsrcSources;
            }
        }
        assertEquals(3, numSsrcSources);
        assertEquals(3, numRidSources);
    }
}