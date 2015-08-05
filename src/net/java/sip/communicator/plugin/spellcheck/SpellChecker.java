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

import java.beans.*;
import java.io.*;
import java.lang.ref.*;
import java.net.*;
import java.util.*;

import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.gui.event.*;
import net.java.sip.communicator.util.*;

import org.dts.spell.dictionary.*;
import org.jitsi.service.fileaccess.*;
import org.jitsi.util.OSUtils;
import org.osgi.framework.*;

import javax.swing.*;

/**
 * Model for spell checking capabilities. This allows for the on-demand
 * retrieval of dictionaries in other languages which are cached with the user's
 * configurations.
 *
 * @author Damian Johnson
 * @author Lyubomir Marinov
 */
class SpellChecker
    implements ChatListener
{
    /**
     * The <tt>Logger</tt> used by the <tt>SpellChecker</tt> class and its
     * instances for logging output.
     */
    private static final Logger logger = Logger.getLogger(SpellChecker.class);

    private static final String LOCALE_CONFIG_PARAM =
        "net.java.sip.communicator.plugin.spellchecker.LOCALE";

    // default bundled dictionary
    private static final String DEFAULT_DICT_PATH =
        "/resources/config/spellcheck/";

    // location where dictionaries are stored
    private static final String DICT_DIR = "spellingDictionaries/";

    // filename of custom dictionary (added words)
    private static final String PERSONAL_DICT_NAME = "custom.per";

    /**
     * System directory for hunspell-dictionaries.
     */
    private static final String SYSTEM_HUNSPELL_DIR
        = "net.java.sip.communicator.plugin.spellcheck.SYSTEM_HUNSPELL_DIR";

    /*-
     * Dictionary resources.
     * Note: Dictionary needs to be created with input streams that AREN'T CLOSED
     * (dictionaries will handle it). Closing the stream or creating with a file
     * will cause an internal NullPointerException in the spell checker.
     */
    private File personalDictLocation;

    private File dictLocation;

    private SpellDictionary dict;

    private Parameters.Locale locale; // dictionary locale

    // chat instances the spell checker is currently attached to
    private ArrayList<ChatAttachments> attachedChats =
        new ArrayList<ChatAttachments>();

    private boolean enabled = true;

    static final String LOCALE_CHANGED_PROP = "LocaleChanged";

    /**
     * Listeners waiting for spell checker locale update.
     */
    private final List<WeakReference<PropertyChangeListener>>
            propertyListeners
                = new ArrayList<WeakReference<PropertyChangeListener>>();

    /**
     * Associates spell checking capabilities with all chats. This doesn't do
     * anything if this is already running.
     *
     * @param bc execution context of the bundle
     * @throws Exception
     */
    synchronized void start(BundleContext bc) throws Exception
    {
        enabled
            = SpellCheckActivator.getConfigService().getBoolean(
                    "plugin.spellcheck.ENABLE",
                    true);

        FileAccessService faService =
            SpellCheckActivator.getFileAccessService();

        // checks if DICT_DIR exists to see if this is the first run
        File dictionaryDir = faService.getPrivatePersistentFile(DICT_DIR,
            FileCategory.CACHE);

        if (!dictionaryDir.exists())
        {
            dictionaryDir.mkdir();
        }

        // gets resource for personal dictionary
        this.personalDictLocation =
            faService.getPrivatePersistentFile(DICT_DIR + PERSONAL_DICT_NAME,
                FileCategory.PROFILE);

        if (!personalDictLocation.exists())
            personalDictLocation.createNewFile();

        // gets dictionary locale
        String localeIso =
            SpellCheckActivator.getConfigService().getString(
                LOCALE_CONFIG_PARAM);

        if (localeIso == null)
        {
            // sets locale to be the default
            localeIso = Parameters.getDefault(Parameters.Default.LOCALE);
            if (localeIso == null)
                throw new Exception(
                    "No default locale provided for spell checker");
        }

        Parameters.Locale tmp = Parameters.getLocale(localeIso);
        if (tmp == null)
            throw new Exception("No dictionary resources defined for locale: "
                + localeIso);
        this.locale = tmp; // needed for synchronization lock

        SwingUtilities.invokeLater(new Runnable()
        {
            public void run()
            {
                // attaches to uiService so we'll be attached to future chats
                SpellCheckActivator.getUIService()
                    .addChatListener(SpellChecker.this);

                for (Chat chat : SpellCheckActivator.getUIService()
                                    .getAllChats())
                    chatCreated(chat);
            }
        });

        if (logger.isInfoEnabled())
            logger.info("Spell Checker loaded.");
    }

    /**
     * Removes the spell checking capabilities from all chats. This doesn't do
     * anything if this isn't running.
     */
    synchronized void stop()
    {
        // removes spell checker listeners from chats
        synchronized (this.attachedChats)
        {
            for (ChatAttachments chat : this.attachedChats)
                chat.detachListeners();
            this.attachedChats.clear();
            SpellCheckActivator.getUIService().removeChatListener(this);
        }
    }

    /**
     * Notifies this instance that a <tt>Chat</tt> has been closed.
     *
     * @param chat the <tt>Chat</tt> which has been closed
     */
    public void chatClosed(Chat chat)
    {
        synchronized (attachedChats)
        {
            ChatAttachments wrapper = getChatAttachments(chat);

            if (wrapper != null)
            {
                attachedChats.remove(wrapper);
                wrapper.detachListeners();
            }

            // this is the last chat, window is closed
            if(attachedChats.size() == 0)
                stop();
        }
    }

    /**
     * Notifies this instance that a new <tt>Chat</tt> has been created.
     * Attaches listeners to the new <tt>chat</tt>.
     *
     * @param chat the new <tt>Chat</tt> which has been created
     */
    public void chatCreated(Chat chat)
    {
        synchronized (attachedChats)
        {
            if (getChatAttachments(chat) == null
                && this.dict != null)
            {
                ChatAttachments wrapper = new ChatAttachments(chat, this.dict);

                // if it contains wrapper for the same chat, don't add it
                if(attachedChats.contains(wrapper))
                    return;

                wrapper.setEnabled(enabled);
                wrapper.attachListeners();
                attachedChats.add(wrapper);
            }
        }
    }

    /**
     * Gets the <tt>ChatAttachments</tt> instance associated with a specific
     * <tt>Chat</tt>.
     *
     * @param chat the <tt>Chat</tt> whose associated <tt>ChatAttachments</tt>
     * instance is to be returned
     * @return the <tt>ChatAttachments</tt> instances associated with the
     * specified <tt>chat</tt>; otherwise, <tt>null</tt>
     */
    private ChatAttachments getChatAttachments(Chat chat)
    {
        synchronized (attachedChats)
        {
            for (ChatAttachments chatAttachments : attachedChats)
                if (chatAttachments.chat.equals(chat))
                    return chatAttachments;
        }
        return null;
    }

    /**
     * Provides the user's list of words to be ignored by the spell checker.
     *
     * @return user's word list
     */
    List<String> getPersonalWords()
    {
        List<String> personalWords = new ArrayList<String>();

        synchronized (personalDictLocation)
        {
            try
            {
                // Retrieves contents of the custom dictionary
                Scanner personalDictScanner
                    = new Scanner(personalDictLocation);

                try
                {
                    while (personalDictScanner.hasNextLine())
                        personalWords.add(personalDictScanner.nextLine());
                }
                finally
                {
                    personalDictScanner.close();
                }
            }
            catch (FileNotFoundException exc)
            {
                logger.error("Unable to read custom dictionary", exc);
            }
        }
        return personalWords;
    }

    /**
     * Writes custom dictionary and updates spell checker to utilize new
     * listing.
     *
     * @param words words to be ignored by the spell checker
     */
    void setPersonalWords(List<String> words)
    {
        synchronized (this.personalDictLocation)
        {
            try
            {
                // writes new word list
                BufferedWriter writer =
                    new BufferedWriter(
                        new FileWriter(this.personalDictLocation));

                for (String customWord : words)
                {
                    writer.append(customWord);
                    writer.newLine();
                }
                writer.flush();
                writer.close();

                // resets dictionary being used to include changes
                synchronized (this.attachedChats)
                {
                    InputStream dictInput =
                        new FileInputStream(this.dictLocation);
                    this.dict =
                        new OpenOfficeSpellDictionary(dictInput,
                            this.personalDictLocation);

                    // updates chats
                    for (ChatAttachments chat : this.attachedChats)
                        chat.setDictionary(this.dict);
                }
            }
            catch (IOException exc)
            {
                logger.error("Unable to access personal spelling dictionary",
                    exc);
            }
        }
    }

    /**
     * Provides the locale of the dictionary currently being used by the spell
     * checker.
     *
     * @return locale of current dictionary
     */
    Parameters.Locale getLocale()
    {
        synchronized (this.locale)
        {
            return this.locale;
        }
    }

    /**
     * Resets spell checker to use a different locale's dictionary. This uses
     * the local copy of the dictionary if available, otherwise it's downloaded
     * and saved for future use.
     *
     * @param locale locale of dictionary to be used
     * @throws Exception problem occurring in utilizing locale's dictionary
     */
    void setLocale(Parameters.Locale locale) throws Exception
    {
        synchronized (this.locale)
        {
            if(this.locale.equals(locale)
                && this.dict != null)
                return;

            File dictLocation = getLocalDictForLocale(locale);
            InputStream dictInput = null;
            InputStream affInput = null;

            if (OSUtils.IS_LINUX && !dictLocation.exists())
            {
                String sysDir =
                    SpellCheckActivator.getConfigService().getString(
                        SYSTEM_HUNSPELL_DIR);
                File systemDict =
                    new File(sysDir, locale.getIcuLocale() + ".dic");
                if (systemDict.exists())
                {
                    dictInput = new FileInputStream(systemDict);
                    affInput = new FileInputStream(new File(sysDir,
                            locale.getIcuLocale() + ".aff"));
                }
            }

            if (!dictLocation.exists() && dictInput == null)
            {
                boolean dictFound = false;

                // see if the requested locale is a built-in that doesn't
                // need to be downloaded
                @SuppressWarnings ("unchecked")
                Enumeration<URL> dictUrls
                    = SpellCheckActivator.bundleContext.getBundle()
                        .findEntries(DEFAULT_DICT_PATH,
                                    "*.zip",
                                    false);

                if (dictUrls != null)
                {
                    while (dictUrls.hasMoreElements())
                    {
                        URL dictUrl = dictUrls.nextElement();
                        if (new File(dictUrl.getFile()).getName().equals(
                            new File(locale.getDictUrl().getFile()).getName()))
                        {
                            dictInput = dictUrl.openStream();
                            dictFound = true;
                            break;
                        }
                    }
                }

                // downloads dictionary if unavailable (not cached)
                if (!dictFound)
                {
                    copyDictionary(locale.getDictUrl().openStream(),
                        dictLocation);
                }
            }

            if (dictInput == null)
            {
                dictInput = new FileInputStream(dictLocation);
            }

            // resets dictionary being used to include changes
            synchronized (this.attachedChats)
            {
                SpellDictionary dict;
                if (affInput == null)
                {
                    dict = new OpenOfficeSpellDictionary(dictInput,
                        this.personalDictLocation);
                }
                else
                {
                    dict =
                        new OpenOfficeSpellDictionary(affInput, dictInput,
                            personalDictLocation, true);
                }

                this.dict = dict;
                this.dictLocation = dictLocation;
                Parameters.Locale oldLocale = this.locale;
                this.locale = locale;

                // saves locale choice to configuration properties
                SpellCheckActivator.getConfigService().setProperty(
                    LOCALE_CONFIG_PARAM, locale.getIsoCode());

                // updates chats
                for (ChatAttachments chat : this.attachedChats)
                    chat.setDictionary(this.dict);

                firePropertyChangedEvent(
                    LOCALE_CHANGED_PROP, oldLocale, this.locale);
            }
        }
    }

    /**
     * Gets the file object for user-installed dictionaries.
     * 
     * @param locale The locale whose filename is needed.
     * @return The file object of the locale.
     * @throws Exception 
     */
    File getLocalDictForLocale(Parameters.Locale locale) throws Exception
    {
        String path = locale.getDictUrl().getFile();
        int filenameStart = path.lastIndexOf('/') + 1;
        String filename = path.substring(filenameStart);
        File dictLocation =
            SpellCheckActivator.getFileAccessService()
                .getPrivatePersistentFile(DICT_DIR + filename,
                    FileCategory.CACHE);

        return dictLocation;
    }

    /**
     * Removes the dictionary from the system, and sets the default locale
     * dictionary as the current dictionary
     *
     * @param locale locale to be removed
     */
    void removeLocale(Parameters.Locale locale) throws Exception
    {
        synchronized (this.locale)
        {
            String path = locale.getDictUrl().getFile();

            int filenameStart = path.lastIndexOf('/') + 1;
            String filename = path.substring(filenameStart);

            File dictLocation =
                SpellCheckActivator.getFileAccessService()
                    .getPrivatePersistentFile(DICT_DIR + filename,
                        FileCategory.CACHE);

            if (dictLocation.exists())
                dictLocation.delete();

            String localeIso = Parameters.getDefault(Parameters.Default.LOCALE);
            Parameters.Locale loc = Parameters.getLocale(localeIso);
            setLocale(loc);
        }
    }

    /**
     * Determines if locale's dictionary is locally available or a system.
     *
     * @param locale locale to be checked
     * @return true if local resources for dictionary are available and
     *         accessible, false otherwise
     */
    boolean isLocaleAvailable(Parameters.Locale locale)
    {
        try
        {
            if (getLocalDictForLocale(locale).exists())
            {
                return true;
            }
            else
            {
                @SuppressWarnings ("unchecked")
                Enumeration<URL> dictUrls
                    = SpellCheckActivator.bundleContext.getBundle()
                        .findEntries(DEFAULT_DICT_PATH,
                                    "*.zip",
                                    false);

                if (dictUrls != null)
                {
                    while (dictUrls.hasMoreElements())
                    {
                        URL dictUrl = dictUrls.nextElement();
                        if (new File(dictUrl.getFile()).getName().equals(
                            new File(locale.getDictUrl().getFile()).getName()))
                        {
                            return true;
                        }
                    }
                }
            }

            return false;
        }
        catch (Exception exc)
        {
            return false;
        }
    }

    /**
     * Determines if locale's dictionary is locally available or a system.
     *
     * @param locale locale to be checked
     * @return true if local resources for dictionary are available and
     *         accessible, false otherwise
     */
    boolean isUserLocale(Parameters.Locale locale)
    {
        try
        {
            return getLocalDictForLocale(locale).exists();
        }
        catch (Exception exc)
        {
            return false;
        }
    }

    boolean isEnabled()
    {
        synchronized (this.attachedChats)
        {
            return enabled;
        }
    }

    void setEnabled(boolean enabled)
    {
        if (this.enabled != enabled)
        {
            synchronized (this.attachedChats)
            {
                this.enabled = enabled;
                for (ChatAttachments chatAttachment : this.attachedChats)
                    chatAttachment.setEnabled(this.enabled);
            }
        }
    }

    // copies dictionary to appropriate location, closing the stream afterward
    private void copyDictionary(InputStream input, File dest)
        throws IOException,
        FileNotFoundException
    {
        byte[] buf = new byte[1024];
        FileOutputStream output = new FileOutputStream(dest);

        int len;
        while ((len = input.read(buf)) > 0)
            output.write(buf, 0, len);

        input.close();
        output.close();
    }

    /**
     * Determines if spell checker dictionary works. Backend API often fails
     * when used so this tests that the current dictionary is able to process
     * words.
     *
     * @return true if current dictionary can check words, false otherwise
     */
    private boolean isDictionaryValid(SpellDictionary dict)
    {
        try
        {
            // spell checker API often works until used
            dict.isCorrect("foo");
            return true;
        }
        catch (Exception exc)
        {
            logger.error("Dictionary validation failed", exc);
            return false;
        }
    }

    /**
     * Adds a PropertyChangeListener to the listener list.
     * <p>
     * @param listener the PropertyChangeListener to be added
     */
    public void addPropertyChangeListener(PropertyChangeListener listener)
    {
        synchronized (propertyListeners)
        {
            Iterator<WeakReference<PropertyChangeListener>> i
                = propertyListeners.iterator();
            boolean contains = false;

            while (i.hasNext())
            {
                PropertyChangeListener l = i.next().get();

                if (l == null)
                    i.remove();
                else if (l.equals(listener))
                    contains = true;
            }
            if (!contains)
                propertyListeners.add(
                        new WeakReference<PropertyChangeListener>(listener));
        }
    }

    /**
     * Removes a PropertyChangeListener from the listener list.
     * <p>
     * @param listener the PropertyChangeListener to be removed
     */
    public void removePropertyChangeListener(PropertyChangeListener listener)
    {
        synchronized (propertyListeners)
        {
            Iterator<WeakReference<PropertyChangeListener>> i
                = propertyListeners.iterator();

            while (i.hasNext())
            {
                PropertyChangeListener l = i.next().get();

                if ((l == null) || l.equals(listener))
                    i.remove();
            }
        }
    }

    /**
     * Fires event.
     * @param property
     * @param oldValue
     * @param newValue
     */
    private void firePropertyChangedEvent(String property,
                                          Object oldValue,
                                          Object newValue)
    {
        PropertyChangeEvent evt = new PropertyChangeEvent(
            this, property, oldValue, newValue);

        if (logger.isDebugEnabled())
            logger.debug("Will dispatch the following plugin component event: "
            + evt);

        synchronized (propertyListeners)
        {
            Iterator<WeakReference<PropertyChangeListener>> i
                = propertyListeners.iterator();

            while (i.hasNext())
            {
                PropertyChangeListener l = i.next().get();

                if (l == null)
                    i.remove();
                else
                {
                    l.propertyChange(evt);
                }
            }
        }
    }
}
