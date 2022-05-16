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
package net.java.sip.communicator.impl.protocol.irc;

import static org.junit.Assert.*;

import net.java.sip.communicator.impl.protocol.irc.ControlChar.*;
import org.junit.*;

/**
 * @author Danny van Heumen
 */
public class ControlCharTest
{
    @Test
    public void testGetTag()
    {
        assertEquals("i", (new ControlChar.Italics()).getTag());
    }

    @Test
    public void testGetHtmlStartSimple()
    {
        assertEquals("<b>", (new ControlChar.Bold()).getHtmlStart());
    }

    @Test
    public void testGetHtmlEnd()
    {
        assertEquals("</u>", (new ControlChar.Underline()).getHtmlEnd());
    }

    @Test
    public void testColorFormatControlCharBoth()
    {
        ColorFormat control = new ColorFormat(Color.GREEN, Color.RED);
        assertEquals("font", control.getTag());
        assertEquals("<font color=\"Green\" bgcolor=\"Red\">",
            control.getHtmlStart());
        assertEquals("</font>", control.getHtmlEnd());
    }

    @Test
    public void testColorFormatControlCharForeground()
    {
        ColorFormat control = new ColorFormat(Color.GREEN, null);
        assertEquals("font", control.getTag());
        assertEquals("<font color=\"Green\">", control.getHtmlStart());
        assertEquals("</font>", control.getHtmlEnd());
    }

    @Test
    public void testColorFormatControlCharBackground()
    {
        ColorFormat control = new ColorFormat(null, Color.RED);
        assertEquals("font", control.getTag());
        assertEquals("<font bgcolor=\"Red\">", control.getHtmlStart());
        assertEquals("</font>", control.getHtmlEnd());
    }
}
