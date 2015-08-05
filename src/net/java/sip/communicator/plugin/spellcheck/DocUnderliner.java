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
package net.java.sip.communicator.plugin.spellcheck;

import java.awt.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;

import net.java.sip.communicator.util.*;

/**
 * Notifies subclasses when words are changed and lets them decide if text
 * should be underlined with a red squiggle. Text appended to the end isn't
 * formatted until the word's completed.
 *
 * @author Damian Johnson
 */
abstract class DocUnderliner
    implements DocumentListener
{
    private static final Logger logger = Logger.getLogger(DocUnderliner.class);

    private static final Color UNDERLINE_COLOR = new Color(255, 100, 100);

    private static final DefaultHighlighter.DefaultHighlightPainter UNDERLINER;

    private final Highlighter docHighlighter;

    private final CaretListener endChecker;

    private boolean isEnabled = true;

    static
    {
        UNDERLINER =
            new DefaultHighlighter.DefaultHighlightPainter(UNDERLINE_COLOR)
            {
                @Override
                public Shape paintLayer(Graphics g, int offs0, int offs1,
                    Shape area, JTextComponent comp, View view)
                {
                    Color color = getColor();
                    if (color == null)
                        g.setColor(comp.getSelectionColor());
                    else
                        g.setColor(color);

                    if (offs0 == view.getStartOffset()
                        && offs1 == view.getEndOffset())
                    {
                        // contained in view, can just use bounds
                        drawWavyLine(g, area.getBounds());
                        return area;
                    }
                    else
                    {
                        // should only render part of View
                        try
                        {
                            Shape shape =
                                view.modelToView(offs0, Position.Bias.Forward,
                                    offs1, Position.Bias.Backward, area);
                            drawWavyLine(g, shape.getBounds());
                            return shape.getBounds();
                        }
                        catch (BadLocationException exc)
                        {
                            String msg =
                                "Bad bounds (programmer error in spell checker)";
                            logger.error(msg, exc);
                            return area; // can't render
                        }
                    }
                }

                private void drawWavyLine(Graphics g, Rectangle bounds)
                {
                    int y = (int) (bounds.getY() + bounds.getHeight());
                    int x1 = (int) bounds.getX();
                    int x2 = (int) (bounds.getX() + bounds.getWidth());

                    boolean upperCurve = true;
                    for (int i = x1; i < x2 - 2; i += 3)
                    {
                        if (upperCurve)
                            g.drawArc(i, y - 2, 3, 3, 0, 180);
                        else
                            g.drawArc(i, y - 2, 3, 3, 180, 180);
                        upperCurve = !upperCurve;
                    }
                }
            };
    }

    {
        this.endChecker = new CaretListener()
        {
            private boolean atEnd = false;

            public void caretUpdate(CaretEvent event)
            {
                if (event.getSource() instanceof JTextComponent)
                {
                    JTextComponent comp = (JTextComponent) event.getSource();
                    Document doc = comp.getDocument();

                    boolean currentlyAtEnd = event.getDot() == doc.getLength();
                    if (isEnabled && this.atEnd && !currentlyAtEnd)
                    {
                        String text = comp.getText();
                        Word changed =
                            Word.getWord(text, text.length() - 1, false);
                        format(changed);
                        promptRepaint();
                    }

                    this.atEnd = currentlyAtEnd;
                }
            }
        };
    }

    /**
     * Queries to see if a word should be underlined. This is called on every
     * internal change and whenever a word's completed so it should be a
     * lightweight process.
     *
     * @param word word to be checked
     * @return true if the word should be underlined, false otherwise
     */
    abstract boolean getFormatting(String word);

    /**
     * Provides the index of the character the cursor is in front of.
     *
     * @return index of caret
     */
    abstract int getCaretPosition();

    /**
     * Prompts the text field to repaint.
     */
    abstract void promptRepaint();

    public static void main(String[] args)
    {
        // Basic demo that underlines words containing "foo"
        JFrame frame = new JFrame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        final JEditorPane editorPane = new JEditorPane();
        editorPane.setPreferredSize(new Dimension(400, 500));

        final DocUnderliner formatter =
            new DocUnderliner(editorPane.getHighlighter())
            {
                @Override
                boolean getFormatting(String word)
                {
                    return word.contains("foo");
                }

                @Override
                int getCaretPosition()
                {
                    return editorPane.getCaretPosition();
                }

                @Override
                void promptRepaint()
                {
                    editorPane.repaint();
                }
            };
        editorPane.getDocument().addDocumentListener(formatter);
        editorPane.addCaretListener(formatter.getEndChecker());

        frame.add(editorPane);
        frame.pack();
        frame.setVisible(true);
    }

    DocUnderliner(Highlighter docHighlighter)
    {
        this.docHighlighter = docHighlighter;
    }

    public void insertUpdate(DocumentEvent event)
    {
        if (!this.isEnabled)
            return;

        try
        {
            Document doc = event.getDocument();
            String text = doc.getText(0, doc.getLength());

            if (event.getLength() == 1)
            {
                char changeChar = text.charAt(event.getOffset());
                if (getCaretPosition() == text.length() - 1)
                {
                    if (!Character.isLetter(changeChar))
                    {
                        // finished last word
                        Word last = Word.getWord(text, text.length() - 1, true);
                        format(last);
                    }
                    else
                    {
                        // new character at end (ensure it isn't initially
                        // underlined)
                        clearUnderlining(event.getOffset(),
                            event.getOffset() + 1);
                    }
                }
                else
                {
                    if (Character.isLetter(changeChar))
                    {
                        // change within word
                        Word changed;
                        int previousIndex = Math.max(0, event.getOffset() - 1);
                        if (Character.isLetter(text.charAt(previousIndex)))
                            changed =
                                Word.getWord(text, event.getOffset(), true);
                        else
                            changed =
                                Word.getWord(text, event.getOffset(), false);
                        format(changed);
                    }
                    else
                    {
                        // dividing a word - need to check both sides
                        Word firstWord =
                            Word.getWord(text, event.getOffset(), true);
                        Word secondWord =
                            Word.getWord(text, event.getOffset() + 1, false);
                        format(firstWord);
                        format(secondWord);
                    }
                }
            }
            else
            {
                // pasting in a chunk of text (checks all words in modified
                // range)
                Word changed = Word.getWord(text, event.getOffset(), true);
                int wordStart = changed.getStart();
                while (wordStart < event.getOffset() + event.getLength())
                {
                    format(changed);
                    int end =
                        Math.min(changed.getStart()
                            + changed.getText().length() + 1, text.length());
                    changed = Word.getWord(text, end, false);
                    wordStart = end;
                }
            }
        }
        catch (BadLocationException exc)
        {
            String msg = "Bad bounds (programmer error in spell checker)";
            logger.error(msg, exc);
        }
        catch (Throwable exc)
        {
            logger.error("Error words processing", exc);
        }

        promptRepaint();
    }

    public void removeUpdate(DocumentEvent event)
    {
        if (!this.isEnabled)
            return;

        try
        {
            Document doc = event.getDocument();
            String text = doc.getText(0, doc.getLength());
            if (text.length() != 0)
            {
                Word changed;
                if (event.getOffset() == 0
                    || !Character.isLetter(text.charAt(event.getOffset() - 1)))
                {
                    changed = Word.getWord(text, event.getOffset(), false);
                }
                else
                {
                    changed = Word.getWord(text, event.getOffset() - 1, true);
                }

                format(changed);
            }

            promptRepaint();
        }
        catch (BadLocationException exc)
        {
            String msg = "Bad bounds (programmer error in spell checker)";
            logger.error(msg, exc);
        }
        catch (Throwable exc)
        {
            logger.error("Error words processing", exc);
        }
    }

    public void changedUpdate(DocumentEvent e)
    {
    }

    /**
     * Provides a listener that prompts the last word to be checked when the
     * cursor moves away from it.
     *
     * @return listener for caret position that formats last word when
     *         appropriate
     */
    public CaretListener getEndChecker()
    {
        return this.endChecker;
    }

    /**
     * Formats the word with the appropriate underlining (or lack thereof).
     *
     * @param word word to be formatted
     */
    public void format(Word word)
    {
        if (!this.isEnabled)
            return;

        String text = word.getText();
        if (text.length() > 0)
        {
            clearUnderlining(word.getStart(), word.getStart() + text.length());
            if (getFormatting(text))
                underlineRange(word.getStart(), word.getStart() + text.length());
        }
    }

    /**
     * Sets a range in the editor to be underlined.
     *
     * @param start start of range to be underlined
     * @param end end of range to be underlined
     */
    private void underlineRange(int start, int end)
    {
        if (end > start)
        {
            try
            {
                if (this.isEnabled)
                    this.docHighlighter.addHighlight(start, end, UNDERLINER);
            }
            catch (BadLocationException exc)
            {
                String msg = "Bad bounds (programmer error in spell checker)";
                logger.error(msg, exc);
            }
        }
    }

    /**
     * Clears any underlining that spans to include the given range. Since
     * formatting is defined by ranges this will likely clear more than the
     * defined range.
     *
     * @param start start of range in which to clear underlining
     * @param end end of range in which to clear underlining
     */
    private void clearUnderlining(int start, int end)
    {
        if (end > start)
        {
            // removes highlighting if visible
            if (this.isEnabled)
            {
                for (Highlighter.Highlight highlight : this.docHighlighter
                    .getHighlights())
                {
                    if ((highlight.getStartOffset() <= start && highlight
                        .getEndOffset() > start)
                        || (highlight.getStartOffset() < end && highlight
                            .getEndOffset() >= end))
                    {
                        this.docHighlighter.removeHighlight(highlight);
                    }
                }
            }
        }
    }

    public void setEnabled(boolean enable, String message)
    {
        if (this.isEnabled != enable)
        {
            this.isEnabled = enable;
            if (this.isEnabled)
                reset(message);
            else
                this.docHighlighter.removeAllHighlights();
            promptRepaint();
        }
    }

    /**
     * Clears underlining and re-evaluates message's contents
     *
     * @param message textual contents of document
     */
    public void reset(String message)
    {
        if (!this.isEnabled)
            return;

        // clears previous underlined sections
        this.docHighlighter.removeAllHighlights();

        // runs over message
        if (message.length() > 0)
        {
            Word changed = Word.getWord(message, 0, true);
            int wordStart = changed.getStart();
            while (wordStart < message.length())
            {
                format(changed);
                int end =
                    Math.min(changed.getStart() + changed.getText().length()
                        + 1, message.length());
                changed = Word.getWord(message, end, false);
                wordStart = end;
            }
        }

        promptRepaint();
    }
}
