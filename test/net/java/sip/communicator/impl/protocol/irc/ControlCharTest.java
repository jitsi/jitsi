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

import net.java.sip.communicator.impl.protocol.irc.ControlChar.ColorFormat;
import junit.framework.*;

/**
 * @author Danny van Heumen
 */
public class ControlCharTest
    extends TestCase
{

    protected void setUp() throws Exception
    {
        super.setUp();
    }

    protected void tearDown() throws Exception
    {
        super.tearDown();
    }
    
    public void testGetTag()
    {
        Assert.assertEquals("i", (new ControlChar.Italics()).getTag());
    }

    public void testGetHtmlStartSimple()
    {
        Assert.assertEquals("<b>", (new ControlChar.Bold()).getHtmlStart());
    }

    public void testGetHtmlEnd()
    {
        Assert.assertEquals("</u>", (new ControlChar.Underline()).getHtmlEnd());
    }
    
    public void testColorFormatControlCharBoth()
    {
        ColorFormat control = new ControlChar.ColorFormat(Color.GREEN, Color.RED);
        Assert.assertEquals("font", control.getTag());
        Assert.assertEquals("<font color=\"Green\" bgcolor=\"Red\">", control.getHtmlStart());
        Assert.assertEquals("</font>", control.getHtmlEnd());
    }
    
    public void testColorFormatControlCharForeground()
    {
        ColorFormat control = new ControlChar.ColorFormat(Color.GREEN, null);
        Assert.assertEquals("font", control.getTag());
        Assert.assertEquals("<font color=\"Green\">", control.getHtmlStart());
        Assert.assertEquals("</font>", control.getHtmlEnd());
    }
    
    public void testColorFormatControlCharBackground()
    {
        ColorFormat control = new ControlChar.ColorFormat(null, Color.RED);
        Assert.assertEquals("font", control.getTag());
        Assert.assertEquals("<font bgcolor=\"Red\">", control.getHtmlStart());
        Assert.assertEquals("</font>", control.getHtmlEnd());
    }
}
