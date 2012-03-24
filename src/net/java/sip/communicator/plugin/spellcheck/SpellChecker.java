/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 * 
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.spellcheck;

import java.io.*;
import java.net.*;
import java.util.*;

import net.java.sip.communicator.service.fileaccess.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.gui.event.*;
import net.java.sip.communicator.util.Logger;

import org.dts.spell.dictionary.*;
import org.osgi.framework.*;

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
        File dictionaryDir = faService.getPrivatePersistentFile(DICT_DIR);

        if (!dictionaryDir.exists())
        {
            dictionaryDir.mkdir();

            // copy default dictionaries so they don't need to be downloaded
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

                    InputStream source = dictUrl.openStream();

                    int filenameStart = dictUrl.getPath().lastIndexOf('/') + 1;
                    String filename = dictUrl.getPath().substring(filenameStart);

                    File dictLocation =
                        faService.getPrivatePersistentFile(DICT_DIR + filename);

                    copyDictionary(source, dictLocation);
                }
            }
        }

        // gets resource for personal dictionary
        this.personalDictLocation =
            faService.getPrivatePersistentFile(DICT_DIR + PERSONAL_DICT_NAME);

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

        // initializes dictionary and saves locale config
        // use the worker to set the locale if it fails
        // will still show spellcheck and will not fail
        // starting spell check plugin
        LanguageMenuBar.makeSelectionField(this)
            .createSpellCheckerWorker(locale).start();

        // attaches to uiService so this'll be attached to future chats
        synchronized (this.attachedChats)
        {
            SpellCheckActivator.getUIService().addChatListener(this);
            for (Chat chat : SpellCheckActivator.getUIService().getAllChats())
                chatCreated(chat);
        }

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
            String path = locale.getDictUrl().getFile();

            int filenameStart = path.lastIndexOf('/') + 1;
            String filename = path.substring(filenameStart);

            File dictLocation =
                SpellCheckActivator.getFileAccessService()
                    .getPrivatePersistentFile(DICT_DIR + filename);

            // downloads dictionary if unavailable (not cached)
            if (!dictLocation.exists())
                copyDictionary(locale.getDictUrl().openStream(), dictLocation);

            // resets dictionary being used to include changes
            synchronized (this.attachedChats)
            {
                InputStream dictInput = new FileInputStream(dictLocation);

                SpellDictionary dict =
                    new OpenOfficeSpellDictionary(dictInput,
                        this.personalDictLocation);

                this.dict = dict;
                this.dictLocation = dictLocation;
                this.locale = locale;

                // saves locale choice to configuration properties
                SpellCheckActivator.getConfigService().setProperty(
                    LOCALE_CONFIG_PARAM, locale.getIsoCode());

                // updates chats
                for (ChatAttachments chat : this.attachedChats)
                    chat.setDictionary(this.dict);
            }
        }
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
                    .getPrivatePersistentFile(DICT_DIR + filename);

            if (dictLocation.exists())
                dictLocation.delete();

            String localeIso = Parameters.getDefault(Parameters.Default.LOCALE);
            Parameters.Locale loc = Parameters.getLocale(localeIso);
            setLocale(loc);
        }
    }
    
    /**
     * Determines if locale's dictionary is locally available or not.
     * 
     * @param locale locale to be checked
     * @return true if local resources for dictionary are available and
     *         accessible, false otherwise
     */
    boolean isLocaleAvailable(Parameters.Locale locale)
    {
        String path = locale.getDictUrl().getFile();
        int filenameStart = path.lastIndexOf('/') + 1;
        String filename = path.substring(filenameStart);
        try
        {
            File dictLocation =
                SpellCheckActivator.getFileAccessService()
                    .getPrivatePersistentFile(DICT_DIR + filename);

            return dictLocation.exists();
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
}
