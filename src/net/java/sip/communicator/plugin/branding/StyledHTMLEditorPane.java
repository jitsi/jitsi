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
package net.java.sip.communicator.plugin.branding;

import java.io.*;

import javax.swing.*;
import javax.swing.text.*;
import javax.swing.text.html.*;

import net.java.sip.communicator.util.*;

public class StyledHTMLEditorPane
    extends JEditorPane
{
    private final Logger logger = Logger.getLogger(StyledHTMLEditorPane.class);

    private final HTMLDocument document;

    public StyledHTMLEditorPane()
    {
        this.setContentType("text/html");

        this.document
            = (HTMLDocument) this.getDocument();

        this.setDocument(document);

        Constants.loadSimpleStyle(document.getStyleSheet());
    }

    public void appendToEnd(String text)
    {
        Element root = document.getDefaultRootElement();
        try
        {
            document.insertAfterEnd(root
                .getElement(root.getElementCount() - 1), text);
        }
        catch (BadLocationException e)
        {
            logger.error("Insert in the HTMLDocument failed.", e);
        }
        catch (IOException e)
        {
            logger.error("Insert in the HTMLDocument failed.", e);
        }
    }

    public void insertAfterStart(String text)
    {
        Element root = this.document.getDefaultRootElement();

        try {
            this.document.insertBeforeStart(root
                    .getElement(0), text);
        } catch (BadLocationException e) {
            logger.error("Insert in the HTMLDocument failed.", e);
        } catch (IOException e) {
            logger.error("Insert in the HTMLDocument failed.", e);
        }
    }
}
