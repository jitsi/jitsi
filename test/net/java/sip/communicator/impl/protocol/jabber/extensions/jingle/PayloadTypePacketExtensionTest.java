/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Copyright @ 2015-2018 Atlassian Pty Ltd
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
package net.java.sip.communicator.impl.protocol.jabber.extensions.jingle;

import junit.framework.*;

import java.util.*;

/**
 * Tests {@link PayloadTypePacketExtension} as well as it's children (
 * {@link ParameterPacketExtension} and {@link RtcpFbPacketExtension}).
 *
 * @author Boris Grozev
 */
public class PayloadTypePacketExtensionTest
    extends TestCase
{
    private static final String TEST_ATTR_NAME = "my-test-attribute-name";
    private static final String TEST_ATTR_VALUE = "my-test-attribute-value";

    /**
     * Tests the
     * {@link PayloadTypePacketExtension#clone(PayloadTypePacketExtension)}
     * method.
     */
    public void testClone()
    {
        PayloadTypePacketExtension p = new PayloadTypePacketExtension();
        p.setId(101);
        p.setName("opus");
        ParameterPacketExtension apt
            = new ParameterPacketExtension("apt", "100");
        p.addParameter(apt);
        p.setAttribute(TEST_ATTR_NAME, TEST_ATTR_VALUE);

        RtcpFbPacketExtension fb = new RtcpFbPacketExtension();
        fb.setFeedbackType("nack");
        fb.setFeedbackSubtype("pli");
        p.addRtcpFeedbackType(fb);

        PayloadTypePacketExtension c = PayloadTypePacketExtension.clone(p);
        assertEquals(p.getChannels(), c.getChannels());
        assertEquals(p.getName(), c.getName());
        assertEquals(1, c.getParameters().size());
        assertEquals(1, c.getRtcpFeedbackTypeList().size());
        assertEquals(p.getID(), c.getID());

        c.setChannels(2);
        assertEquals(1, p.getChannels());

        c.setName("vp8");
        assertEquals("opus", p.getName());

        ParameterPacketExtension cApt = c.getParameters().get(0);
        assertTrue(apt != cApt);
        assertEquals(apt.getName(), cApt.getName());
        assertEquals(apt.getValue(), cApt.getValue());
        cApt.setName("stereo");
        assertEquals("apt", apt.getName());

        RtcpFbPacketExtension cFb = c.getRtcpFeedbackTypeList().get(0);
        assertTrue(fb != cFb);
        assertEquals(fb.getFeedbackType(), cFb.getFeedbackType());
        assertEquals(fb.getFeedbackSubtype(), cFb.getFeedbackSubtype());
        cFb.setFeedbackType("x1");
        cFb.setFeedbackSubtype("x2");
        assertEquals("nack", fb.getFeedbackType());
        assertEquals("pli", fb.getFeedbackSubtype());


        Set<String> attributeNames = new HashSet<>(p.getAttributeNames());
        attributeNames.addAll(c.getAttributeNames());

        attributeNames.remove(PayloadTypePacketExtension.CHANNELS_ATTR_NAME);
        attributeNames.remove(PayloadTypePacketExtension.ID_ATTR_NAME);
        attributeNames.remove(PayloadTypePacketExtension.NAME_ATTR_NAME);

        for (String s : attributeNames)
        {
            assertEquals(c.getAttribute(s), p.getAttribute(s));
        }

        c.setAttribute(TEST_ATTR_NAME, "WRONG");
        assertEquals(p.getAttribute(TEST_ATTR_NAME), TEST_ATTR_VALUE);
    }

    public void testSettersAndGetters()
    {
        PayloadTypePacketExtension p = new PayloadTypePacketExtension();

        p.setId(101);
        assertEquals(101, p.getID());

        p.setName("opus");
        assertEquals("opus", p.getName());

        ParameterPacketExtension apt
            = new ParameterPacketExtension("apt", "100");
        p.addParameter(apt);
        assertEquals(1, p.getParameters().size());
        assertEquals(apt, p.getParameters().get(0));

        RtcpFbPacketExtension fb = new RtcpFbPacketExtension();
        fb.setFeedbackType("nack");
        fb.setFeedbackSubtype("pli");
        p.addRtcpFeedbackType(fb);
        assertEquals(1, p.getRtcpFeedbackTypeList().size());
        assertEquals(fb, p.getRtcpFeedbackTypeList().get(0));
    }
}
