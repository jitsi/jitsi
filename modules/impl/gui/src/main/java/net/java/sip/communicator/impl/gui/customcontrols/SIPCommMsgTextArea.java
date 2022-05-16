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
package net.java.sip.communicator.impl.gui.customcontrols;

import javax.swing.*;

/**
 * The <tt>SIPCommMsgTextArea</tt> is a text area defined specially for warning
 * messages. It defines an area with a fixed number of columns and wraps the
 * text within it.
 *
 * @author Yana Stamcheva
 */
public class SIPCommMsgTextArea
    extends JTextArea
{
    private static final long serialVersionUID = 0L;

    public SIPCommMsgTextArea()
    {
        init();
    }

    /**
     * Creates a text area with a fixed number of columns and wraps the
     * text within it.
     * @param text The text to insert in this text area.
     */
    public SIPCommMsgTextArea(String text){
        super(text);

        init();
    }

    private void init()
    {
        this.setEditable(false);
        this.setLineWrap(true);
        this.setWrapStyleWord(true);
        this.setOpaque(false);

        int col = 40;
        this.setColumns(col);
        int docLen = this.getDocument().getLength();

        /*
         * FIXME The original code was "(int)Math.ceil(docLen/col)". But it was
         * unnecessary because both docLen and col are integers and,
         * consequently, docLen/col gives an integer. Was the intention to have
         * the division produce a real number?
         */
        this.setRows(docLen/col);
    }
}
