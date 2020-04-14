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

import java.awt.event.*;
import java.util.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;

import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.gui.event.*;
import net.java.sip.communicator.util.*;

import org.dts.spell.dictionary.*;
import org.jitsi.service.resources.*;

/**
 * Wrapper for handling the multiple listeners associated with chats for the
 * spell checker.
 *
 * @author Damian Johnson
 * @author Purvesh Sahoo
 */
class ChatAttachments
{
    private static final Logger logger = Logger
        .getLogger(ChatAttachments.class);

    private static final ImageIcon ADD_WORD_ICON = Resources
        .getImage(Resources.ADD_WORD_ICON);

    public final Chat chat;

    private final DocUnderliner docListener; // The red-squibble drawing code

    private final CaretListener caretListener;

    private final ChatMenuListener menuListener;

    private boolean isEnabled = true;

    private SpellDictionary dict;

    private boolean isAttached = false;

    private final ResourceManagementService resources = Resources
        .getResources();

    private SpellCheckerConfigDialog dialog;

    public ChatAttachments(Chat chat, final SpellDictionary dict)
    {
        this.chat = chat;
        this.dict = dict;

        this.docListener = new DocUnderliner(chat.getHighlighter())
        {
            @Override
            boolean getFormatting(String word)
            {
                try
                {
                    return !ChatAttachments.this.dict.isCorrect(word);
                }
                catch (NullPointerException exc)
                {
                    // thrown by spell checker API if problem occurs
                    logger.error(
                        "Spell checker dictionary failed to be accessed", exc);
                    return false;
                }
            }

            @Override
            int getCaretPosition()
            {
                return ChatAttachments.this.chat.getCaretPosition();
            }

            @Override
            void promptRepaint()
            {
                ChatAttachments.this.chat.promptRepaint();
            }
        };

        this.caretListener = this.docListener.getEndChecker();

        this.menuListener = new ChatMenuListener()
        {

            public List<JMenuItem> getMenuElements(final Chat chat,
                MouseEvent event)
            {

                if (isEnabled && event.getSource() instanceof JTextComponent)
                {
                    JTextComponent comp = (JTextComponent) event.getSource();
                    int index = comp.viewToModel(event.getPoint());
                    try
                    {
                        String compText =
                            comp.getDocument().getText(0,
                                comp.getDocument().getLength());

                        if (index != -1 && compText.length() != 0)
                        {

                            return getCorrections(Word.getWord(
                                comp.getDocument().getText(0,
                                    comp.getDocument().getLength()), index,
                                false));

                        }

                    }
                    catch (BadLocationException e)
                    {
                        logger.error("Error", e);
                    }
                }

                JMenuItem spellCheck =
                    new JMenuItem(
                        resources.getI18NString("plugin.spellcheck.MENU"));

                ArrayList<JMenuItem> spellCheckItem =
                    new ArrayList<JMenuItem>();
                spellCheckItem.add(spellCheck);
                spellCheck.addActionListener(new ActionListener()
                {

                    public void actionPerformed(ActionEvent e)
                    {
                        if(dialog != null) {
                            dialog.dispose();
                        }
                        dialog =
                            new SpellCheckerConfigDialog(chat, null, dict);
                        dialog.setVisible(true);
                    }
                });

                return spellCheckItem;
            }
        };
    }

    /**
     * Attaches spell checker capabilities the associated chat.
     */
    synchronized void attachListeners()
    {
        if (!this.isAttached)
        {
            this.chat.addChatEditorDocumentListener(this.docListener);
            this.chat.addChatEditorCaretListener(this.caretListener);
            this.chat.addChatEditorMenuListener(this.menuListener);

        }
    }

    /**
     * Removes spell checker listeners from the associated chat.
     */
    synchronized void detachListeners()
    {
        if (this.isAttached)
        {
            this.chat.removeChatEditorDocumentListener(this.docListener);
            this.chat.removeChatEditorCaretListener(this.caretListener);
            this.chat.removeChatEditorMenuListener(this.menuListener);

        }
    }

    boolean isEnabled()
    {
        return this.isEnabled;
    }

    void setEnabled(boolean enable)
    {
        synchronized (this.dict)
        {
            this.isEnabled = enable;
            this.docListener.setEnabled(enable, this.chat.getMessage());
        }
    }

    void setDictionary(SpellDictionary dict)
    {
        synchronized (this.dict)
        {
            this.dict = dict;
            this.docListener.reset(this.chat.getMessage());
        }
    }

    // provides popup menu entries (mostly separated for readability)
    private List<JMenuItem> getCorrections(final Word clickedWord)
    {
        ArrayList<JMenuItem> correctionEntries = new ArrayList<JMenuItem>();

        synchronized (this.dict)
        {
            if (!this.dict.isCorrect(clickedWord.getText()))
            {
                List<String> corrections =
                    this.dict.getSuggestions(clickedWord.getText());
                for (String correction : corrections)
                {
                    JMenuItem newEntry = new JMenuItem(correction);
                    newEntry.addActionListener(new CorrectionListener(
                        clickedWord, correction));
                    correctionEntries.add(newEntry);

                }

                // entry to add word
                JMenuItem addWord = new JMenuItem(
                    resources.getI18NString("plugin.spellcheck.dialog.ADD"),
                    ADD_WORD_ICON);
                addWord.addActionListener(new ActionListener()
                {
                    public void actionPerformed(ActionEvent event)
                    {
                        try
                        {
                            dict.addWord(clickedWord.getText());

                            // clears underline
                            docListener.format(clickedWord);
                            chat.promptRepaint();
                        }
                        catch (SpellDictionaryException exc)
                        {
                            String msg =
                                "Unable to add word to personal dictionary";
                            logger.error(msg, exc);
                        }
                    }
                });
                correctionEntries.add(addWord);

            }

            JMenuItem spellCheck =
                new JMenuItem(
                    resources.getI18NString("plugin.spellcheck.MENU"));
            correctionEntries.add(spellCheck);
            spellCheck.addActionListener(new ActionListener()
            {

                public void actionPerformed(ActionEvent e)
                {
                    if(dialog != null) {
                        dialog.dispose();
                    }

                    dialog =
                        new SpellCheckerConfigDialog(chat, clickedWord,
                            dict);
                    dialog.setVisible(true);
                }
            });

        }
        return correctionEntries;

    }

    @Override
    public boolean equals(Object obj)
    {
        if(obj instanceof ChatAttachments)
            return this.chat.equals(((ChatAttachments)obj).chat);
        else
            return false;
    }

    @Override
    public int hashCode()
    {
        return this.chat.hashCode();
    }

    // Applies corrections from popup menu to chat
    private class CorrectionListener
        implements ActionListener
    {
        private final Word clickedWord;

        private final String correction;

        public CorrectionListener(Word clickedWord, String correction)
        {
            this.clickedWord = clickedWord;
            this.correction = correction;
        }

        public void actionPerformed(ActionEvent event)
        {
            StringBuffer newMessage = new StringBuffer(chat.getMessage());

            int endIndex =
                this.clickedWord.getStart()
                    + this.clickedWord.getText().length();
            newMessage.replace(this.clickedWord.getStart(), endIndex,
                this.correction);
            chat.setMessage(newMessage.toString());
        }
    }
}
