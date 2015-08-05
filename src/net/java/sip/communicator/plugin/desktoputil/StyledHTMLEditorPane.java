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
package net.java.sip.communicator.plugin.desktoputil;

import java.io.*;

import javax.swing.*;
import javax.swing.text.*;
import javax.swing.text.html.*;

import net.java.sip.communicator.util.*;

/**
 * A custom styled HTML editor pane.
 *
 * @author Yana Stamcheva
 */
public class StyledHTMLEditorPane
    extends JEditorPane
{
    /**
     * The serial version id.
     */
    private static final long serialVersionUID = 1L;

    /**
     * The logger for this class.
     */
    private final Logger logger = Logger.getLogger(StyledHTMLEditorPane.class);

    /**
     * The editor kit of this editor pane.
     */
    private final HTMLEditorKit editorKit;

    /**
     * The document of this editor pane.
     */
    private final HTMLDocument document;

    /**
     * Creates an instance of <tt>StyledHTMLEditorPane</tt>.
     */
    public StyledHTMLEditorPane()
    {
        editorKit = new SIPCommHTMLEditorKit(this);

        this.document = (HTMLDocument) editorKit.createDefaultDocument();

        this.setContentType("text/html");
        this.setEditorKitForContentType("text/html", editorKit);
        this.setEditorKit(editorKit);
        this.setDocument(document);

        putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
    }

    /**
     * Appends text to end of the editor pane.
     *
     * @param text the text to append
     */
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

    /**
     * Inserts the given text in the beginning of the editor pane.
     *
     * @param text the text to insert
     */
    public void insertAfterStart(String text)
    {
        Element root = this.document.getDefaultRootElement();

        try
        {
            this.document.insertBeforeStart(root
                    .getElement(0), text);
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
}
