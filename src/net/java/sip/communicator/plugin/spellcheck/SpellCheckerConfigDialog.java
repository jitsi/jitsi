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
import java.awt.event.*;
import java.util.List;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;

import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.plugin.desktoputil.*;

import org.dts.spell.dictionary.*;
import org.jitsi.service.resources.*;

/**
 * The spell check dialog that would be opened from the right click menu in the
 * chat window.
 *
 * @author Purvesh Sahoo
 */
public class SpellCheckerConfigDialog
    extends SIPCommDialog
    implements ActionListener
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    private static final Logger logger = Logger
        .getLogger(SpellCheckerConfigDialog.class);

    /**
     * UI Components
     */
    private JTextComponent currentWord;

    private JList suggestionList;

    private JScrollPane suggestionScroll;

    private JButton changeButton;

    private JButton nextButton;

    private JButton addButton;

    private JPanel checkPanel;

    private JPanel buttonsPanel;

    private JPanel topPanel;

    private JPanel suggestionPanel;

    private SpellDictionary dict;

    private Chat chat;

    private final ResourceManagementService resources = Resources
        .getResources();

    private String word;

    private int index;

    private Word clickedWord;

    public SpellCheckerConfigDialog(Chat chat, Word clickedWord,
        SpellDictionary dict)
    {

        super(false);

        this.dict = dict;
        this.chat = chat;

        initComponents(clickedWord);

        this.setTitle(resources.getI18NString("plugin.spellcheck.TITLE"));
        this.setMinimumSize(new Dimension(450, 320));
        this.setPreferredSize(new Dimension(450, 320));
        this.setResizable(false);

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        mainPanel.add(topPanel);

        this.getContentPane().add(mainPanel);

        this.pack();
        Toolkit toolkit = Toolkit.getDefaultToolkit();
        Dimension screenSize = toolkit.getScreenSize();

        int x = (screenSize.width - this.getWidth()) / 2;
        int y = (screenSize.height - this.getHeight()) / 2;

        this.setLocation(x, y);

        if (!currentWord.getText().equals("  ")
            && this.dict.isCorrect(currentWord.getText()))
        {
            nextButton.doClick();
        }
    }

    /**
     * Initialises the UI components.
     */
    private void initComponents(final Word clickWord)
    {

        clickedWord =
            (clickWord == null) ? Word.getWord("  ", 1, false) : clickWord;

        topPanel = new TransparentPanel(new BorderLayout());
        topPanel.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));

        checkPanel = new TransparentPanel(new BorderLayout(10, 10));
        checkPanel.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
        checkPanel.setLayout(new BoxLayout(checkPanel, BoxLayout.X_AXIS));

        currentWord = new JTextField(clickedWord.getText());

        currentWord.setAlignmentX(LEFT_ALIGNMENT);
        currentWord.setMaximumSize(new Dimension(550, 30));

        currentWord.setText(clickedWord.getText());
        currentWord.selectAll();

        // JPanel wordPanel = new TransparentPanel(new BorderLayout());
        // wordPanel.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
        // wordPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 5));
        // wordPanel.add(currentWord);

        buttonsPanel =
            new TransparentPanel(new FlowLayout(FlowLayout.RIGHT, 0, 10));
        changeButton =
            new JButton(
                resources.getI18NString("plugin.spellcheck.dialog.REPLACE"));
        changeButton.setMnemonic(resources
            .getI18nMnemonic("plugin.spellcheck.dialog.REPLACE"));

        changeButton.addActionListener(new ActionListener()
        {

            public void actionPerformed(ActionEvent e)
            {
                if (suggestionList.getSelectedValue() != null)
                {

                    StringBuffer newMessage =
                        new StringBuffer(chat.getMessage());
                    int endIndex;

                    if (word != null)
                    {
                        endIndex = index + currentWord.getText().length();
                        newMessage.replace(index, endIndex,
                            (String) suggestionList.getSelectedValue());
                        word = (String) suggestionList.getSelectedValue();
                    }
                    else
                    {
                        endIndex =
                            clickedWord.getStart()
                                + clickedWord.getText().length();
                        newMessage.replace(clickedWord.getStart(), endIndex,
                            (String) suggestionList.getSelectedValue());
                    }
                    currentWord.setText((String) suggestionList
                        .getSelectedValue());
                    chat.setMessage(newMessage.toString());

                }
            }
        });
        changeButton.setEnabled(false);

        nextButton =
            new JButton(
                resources.getI18NString("plugin.spellcheck.dialog.FIND"));
        nextButton.setMnemonic(resources
            .getI18nMnemonic("plugin.spellcheck.dialog.FIND"));

        nextButton.addActionListener(new ActionListener()
        {

            public Word getNextWord()
            {

                Word nextWord;
                int wordIndex;

                if (word == null)
                {
                    if (currentWord.getText().equals("  "))
                    {
                        String words[] = chat.getMessage().split(" ");
                        currentWord.setText(words[0]);

                    }

                    wordIndex =
                        chat.getMessage().indexOf(currentWord.getText());
                    if (dict.isCorrect(currentWord.getText()))
                        currentWord.setText("");
                }
                else
                {
                    wordIndex = chat.getMessage().indexOf(word, index);
                }

                Word presentWord =
                    Word.getWord(chat.getMessage(), wordIndex, false);

                if (presentWord.getEnd() == chat.getMessage().length())
                {
                    nextWord = Word.getWord(chat.getMessage(), 0, false);

                }
                else
                {
                    nextWord =
                        Word.getWord(chat.getMessage(),
                            presentWord.getEnd() + 1, false);
                }

                index = nextWord.getStart();
                word = nextWord.getText();

                return nextWord;
            }

            public void actionPerformed(ActionEvent e)
            {
                Word nextWord = getNextWord();
                int breakIndex = nextWord.getStart();
                if(breakIndex == 0)
                    breakIndex = nextWord.getEnd() + 1;

                if(nextWord.getText().length() == 0)
                {
                    breakIndex++;
                    nextWord = getNextWord();
                }

                while (dict.isCorrect(nextWord.getText())
                    && nextWord.getEnd() + 1 != breakIndex)
                {
                    nextWord = getNextWord();

                }

                if (!dict.isCorrect(nextWord.getText()))
                {
                    word = nextWord.getText();
                    currentWord.setText(nextWord.getText());

                    String clickedWord = currentWord.getText();
                    setSuggestionModel(clickedWord);
                }

            }
        });

        buttonsPanel.setLayout(new BoxLayout(buttonsPanel, BoxLayout.Y_AXIS));
        buttonsPanel.add(changeButton);
        buttonsPanel.add(nextButton);

        checkPanel.add(currentWord, BorderLayout.NORTH);
        checkPanel.add(Box.createHorizontalStrut(10));
        checkPanel.add(buttonsPanel, BorderLayout.EAST);

        topPanel.add(checkPanel, BorderLayout.NORTH);
        topPanel.add(Box.createVerticalStrut(10));

        DefaultListModel dataModel = new DefaultListModel();
        suggestionList = new JList(dataModel);

        suggestionScroll = new JScrollPane(suggestionList);
        suggestionScroll.setAlignmentX(LEFT_ALIGNMENT);

        if (!dict.isCorrect(clickedWord.getText()))
            setSuggestionModel(clickedWord.getText());

        suggestionList.addListSelectionListener(new ListSelectionListener()
        {

            public void valueChanged(ListSelectionEvent e)
            {

                if (!e.getValueIsAdjusting())
                {
                    changeButton.setEnabled(true);
                }
            }
        });

        MouseListener clickListener = new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent e)
            {
                if (e.getClickCount() == 2)
                {

                    StringBuffer newMessage =
                        new StringBuffer(chat.getMessage());
                    int endIndex;

                    if (word != null)
                    {
                        endIndex = index + currentWord.getText().length();
                        newMessage.replace(index, endIndex,
                            (String) suggestionList.getSelectedValue());
                        word = (String) suggestionList.getSelectedValue();
                    }
                    else
                    {
                        endIndex =
                            clickedWord.getStart()
                                + clickedWord.getText().length();
                        newMessage.replace(clickedWord.getStart(), endIndex,
                            (String) suggestionList.getSelectedValue());
                    }
                    currentWord.setText((String) suggestionList
                        .getSelectedValue());
                    chat.setMessage(newMessage.toString());

                }
            }
        };

        suggestionList.addMouseListener(clickListener);

        addButton =
            new JButton(resources.getI18NString("plugin.spellcheck.dialog.ADD"));
        addButton.setMnemonic(resources
            .getI18nMnemonic("plugin.spellcheck.dialog.ADD"));

        addButton.addActionListener(new ActionListener()
        {

            public void actionPerformed(ActionEvent e)
            {

                try
                {
                    dict.addWord(currentWord.getText());
                    chat.promptRepaint();
                }
                catch (SpellDictionaryException exc)
                {
                    String msg = "Unable to add word to personal dictionary";
                    logger.error(msg, exc);
                }
            }
        });

        suggestionPanel = new TransparentPanel(new BorderLayout(10, 10));
        suggestionPanel.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
        suggestionPanel.setLayout(new BoxLayout(suggestionPanel,
            BoxLayout.X_AXIS));
        suggestionPanel.add(suggestionScroll);
        suggestionPanel.add(Box.createHorizontalStrut(10));
        suggestionPanel.add(addButton);

        topPanel.add(suggestionPanel, BorderLayout.SOUTH);

    }

    /**
     * Sets the model for the suggestion list
     *
     * @param clickedWord
     */
    private void setSuggestionModel(String clickedWord)
    {

        DefaultListModel dataModel = new DefaultListModel();
        List<String> corrections = this.dict.getSuggestions(clickedWord);
        for (String correction : corrections)
        {
            dataModel.addElement(correction);
        }

        suggestionList.setModel(dataModel);
    }

    /**
     * Returns the selected correction value
     *
     * @return selected value from suggestion list
     */
    public Object getCorrection()
    {

        return suggestionList.getSelectedValue();
    }

    public void actionPerformed(ActionEvent e)
    {
        // TODO Auto-generated method stub

    }

    @Override
    protected void close(boolean escaped)
    {
        // TODO Auto-generated method stub

    }

}
