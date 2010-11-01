/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.spellcheck;

import java.awt.event.*;
import java.util.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;

import net.java.sip.communicator.service.gui.Chat;
import net.java.sip.communicator.service.gui.event.ChatMenuListener;
import net.java.sip.communicator.util.Logger;

import org.dts.spell.dictionary.*;

/**
 * Wrapper for handling the multiple listeners associated with chats for the
 * spell checker.
 *
 * @author Damian Johnson
 */
class ChatAttachments
{
    private static final Logger logger
        = Logger.getLogger(ChatAttachments.class);
    private static final ImageIcon ADD_WORD_ICON
        = Resources.getImage(Resources.ADD_WORD_ICON);
    private final Chat chat;
    private final DocUnderliner docListener; //The red-squibble drawing code
    private final CaretListener caretListener;
    private final ChatMenuListener menuListener;
    private boolean isEnabled = true;
    private SpellDictionary dict;
    private boolean isAttached = false;

    ChatAttachments(Chat chat, SpellDictionary dict)
    {
        this.chat = chat;
        this.dict = dict;

        this.docListener = new DocUnderliner(chat.getHighlighter())
        {
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
                            "Spell checker dictionary failed to be accessed",
                            exc);
                    return false;
                }
            }

            int getCaretPosition()
            {
                return ChatAttachments.this.chat.getCaretPosition();
            }

            void promptRepaint()
            {
                ChatAttachments.this.chat.promptRepaint();
            }
        };

        this.caretListener = this.docListener.getEndChecker();

        this.menuListener = new ChatMenuListener()
        {
            public List <JMenuItem> getMenuElements(Chat chat, MouseEvent event)    //Overridden Here
            {
                if (isEnabled && event.getSource() instanceof JTextComponent)
                {
                    JTextComponent comp = (JTextComponent) event.getSource();
                    int index = comp.viewToModel(event.getPoint());

                    if (index != -1 && comp.getText().length() != 0)
                    {
                        return getCorrections(Word.getWord(comp.getText(),
                                index, false));
                    }
                }

                return new ArrayList <JMenuItem>();
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
    private ArrayList <JMenuItem> getCorrections(final Word clickedWord)
    {
        ArrayList <JMenuItem> correctionEntries = new ArrayList <JMenuItem>();

        synchronized (this.dict)
        {
            if (!this.dict.isCorrect(clickedWord.getText()))
            {
                List <String> corrections =
                        this.dict.getSuggestions(clickedWord.getText());
                for (String correction : corrections)
                {
                    JMenuItem newEntry = new JMenuItem(correction);
                    newEntry.addActionListener(new CorrectionListener(
                            clickedWord, correction));
                    correctionEntries.add(newEntry);
                }

                // entry to add word
                JMenuItem addWord = new JMenuItem("Add Word", ADD_WORD_ICON);
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
        }

        return correctionEntries;
    }

    // Applies corrections from popup menu to chat
    private class CorrectionListener
        implements ActionListener
    {
        private Word clickedWord;
        private String correction;

        CorrectionListener(Word clickedWord, String correction)
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