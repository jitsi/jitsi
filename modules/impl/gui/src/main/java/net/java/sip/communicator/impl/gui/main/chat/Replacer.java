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
package net.java.sip.communicator.impl.gui.main.chat;

/**
 * The Replacer interface defines the expectations of message content replacers.
 * Replacers get called with pieces of message content and can then perform
 * appropriate modifications to the content before writing the result.
 *
 * @author Danny van Heumen
 */
public interface Replacer
{
    /**
     * If a replacer expects plain text strings, then html content is
     * automatically unescaped. The replacer is responsible for correctly
     * escaping normal text.
     *
     * @return returns true if it needs plain text or false if it wants html
     *         content
     */
    boolean expectsPlainText();

    /**
     * Actual replace operation. The replacer is called with a piece of text
     * (either plain text or HTML dependening on the result of
     * {@link #expectsPlainText()}) and the result should be written to
     * <tt>target</tt>. If nothing gets written to <tt>target</tt> then that
     * piece of text is lost, so the replacer has full control of the
     * transformation of that particular piece of text.
     * <p>
     * If {@link #expectsPlainText()} returns true, then <tt>replace</tt> is
     * called with a piece of plain text. If <tt>expectsPlainText()</tt> returns
     * false, then a piece of HTML content is provided. In both cases the
     * replacer is expected to write HTML content into <tt>target</tt>.
     * </p>
     * <p>
     * <b>Note</b> that the replacer has to do <b>appropriate HTML escaping</b>
     * itself when necessary, because we cannot determine the nature of the
     * replacer's specific implementation.
     * </p>
     *
     * @param target the target buffer to write the result to
     * @param piece the piece of text that can be replaced
     */
    void replace(StringBuilder target, String piece);
}
