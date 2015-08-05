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

import java.io.*;
import java.net.*;
import java.util.*;

import javax.xml.parsers.*;

import net.java.sip.communicator.util.*;

import org.w3c.dom.*;
import org.xml.sax.*;

/**
 * Information provided via the spellchecer's xml parameters.
 *
 * @author Damian Johnson
 */
class Parameters
{
    private static final Logger logger = Logger.getLogger(Parameters.class);

    private static final String RESOURCE_LOC =
        "resources/config/spellcheck/parameters.xml";

    private static final String NODE_DEFAULTS = "defaults";

    private static final String NODE_LOCALES = "locales";

    private static final HashMap<Default, String> DEFAULTS =
        new HashMap<Default, String>();

    private static final ArrayList<Locale> LOCALES = new ArrayList<Locale>();

    static
    {
        try
        {
            URL url =
                SpellCheckActivator.bundleContext.getBundle().getResource(
                    RESOURCE_LOC);

            InputStream stream = url.openStream();

            if (stream == null)
                throw new IOException();

            // strict parsing options
            DocumentBuilderFactory factory =
                DocumentBuilderFactory.newInstance();

            factory.setValidating(false);
            factory.setIgnoringComments(true);
            factory.setIgnoringElementContentWhitespace(true);

            // parses configuration xml
            /*-
             * Warning: Felix is unable to import the com.sun.rowset.internal
             * package, meaning this can't use the XmlErrorHandler. This causes
             * a warning and a default handler to be attached. Otherwise this
             * should have: builder.setErrorHandler(new XmlErrorHandler());
             */
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(stream);

            // iterates over nodes, parsing contents
            Node root = doc.getChildNodes().item(1);

            NodeList categories = root.getChildNodes();

            for (int i = 0; i < categories.getLength(); ++i)
            {
                Node node = categories.item(i);
                if (node.getNodeName().equals(NODE_DEFAULTS))
                {
                    parseDefaults(node.getChildNodes());
                }
                else if (node.getNodeName().equals(NODE_LOCALES))
                {
                    parseLocales(node.getChildNodes());
                }
                else
                {
                    logger.warn("Unrecognized category: " + node.getNodeName());
                }
            }
        }
        catch (IOException exc)
        {
            logger.error("Unable to load spell checker parameters", exc);
        }
        catch (SAXException exc)
        {
            logger.error("Unable to parse spell checker parameters", exc);
        }
        catch (ParserConfigurationException exc)
        {
            logger.error("Unable to parse spell checker parameters", exc);
        }
    }

    /**
     * Retrieves default values from xml.
     *
     * @param list the configuration list
     */
    private static void parseDefaults(NodeList list)
    {
        for (int i = 0; i < list.getLength(); ++i)
        {
            NamedNodeMap mapping = list.item(i).getAttributes();
            String attribute = mapping.getNamedItem("attribute").getNodeValue();
            String value = mapping.getNamedItem("value").getNodeValue();

            try
            {
                Default field = Default.fromString(attribute);
                DEFAULTS.put(field, value);
            }
            catch (IllegalArgumentException exc)
            {
                logger.warn("Unrecognized default attribute: " + attribute);
            }
        }
    }

    /**
     * Populates LOCALES list with contents of xml.
     *
     * @param list the configuration list
     */
    private static void parseLocales(NodeList list)
    {
        for (int i = 0; i < list.getLength(); ++i)
        {
            Node node = list.item(i);
            NamedNodeMap attributes = node.getAttributes();
            String label = ((Attr) attributes.getNamedItem("label")).getValue();
            String code =
                ((Attr) attributes.getNamedItem("isoCode")).getValue();
            String dictLocation =
                ((Attr) attributes.getNamedItem("dictionaryUrl")).getValue();
            String flagIcon =
                ((Attr) attributes.getNamedItem("flagIcon")).getValue();
            try
            {
                LOCALES.add(new Locale(label, code, new URL(dictLocation), flagIcon));
            }
            catch (MalformedURLException exc)
            {
                logger.warn("Unable to parse dictionary location of " + label
                    + " (" + dictLocation + ")", exc);
            }
        }
    }

    /**
     * Provides the value of a particular default field, null if undefined.
     *
     * @param field default field to retrieve
     * @return value corresponding to default field
     */
    public static String getDefault(Default field)
    {
        return DEFAULTS.get(field);
    }

    /**
     * Provides locale with a given iso code. Null if undefined.
     *
     * @param isoCode iso code of locale to be retrieved
     * @return locale with corresponding iso code
     */
    public static Locale getLocale(String isoCode)
    {
        for (Locale locale : LOCALES)
        {
            if (locale.getIsoCode().equals(isoCode))
                return locale;
        }

        return null;
    }

    /**
     * Provides locales in which dictionary resources are available.
     *
     * @return locations with dictionary resources
     */
    public static ArrayList<Locale> getLocales()
    {
        return new ArrayList<Locale>(LOCALES);
    }

    /**
     * Locale with an available dictionary resource.
     */
    public static class Locale
    {
        private final String label;

        private final String isoCode;

        private final URL dictLocation;

        private final String flagIcon;

        private boolean isLoading = false;

        private Locale(String label, String isoCode, URL dictLocation, String flagIcon)
        {
            this.label = label;
            this.isoCode = isoCode;
            this.dictLocation = dictLocation;
            this.flagIcon = flagIcon;
        }

        /**
         * Provides user readable name of language.
         *
         * @return name of language presented to user
         */
        public String getLabel()
        {
            return this.label;
        }

        /**
         * Provides ISO code as defined by:<br />
         * http://en.wikipedia.org/wiki/ISO_3166-1_alpha-2
         *
         * @return iso code
         */
        public String getIsoCode()
        {
            return this.isoCode;
        }

        /**
         * Gets the ICU locale, which is a combination of the ISO code and the
         * country variant. English for the United States is therefore en_US,
         * German for Switzerland de_CH.
         * 
         * @return ICU locale
         */
        public String getIcuLocale()
        {
            String[] parts = this.isoCode.split(",");
            return parts[0].toLowerCase() + "_" + parts[1].toUpperCase();
        }

        /**
         * Provides the url where the dictionary resource can be found for this
         * language.
         *
         * @return url of dictionary resource
         */
        public URL getDictUrl()
        {
            return this.dictLocation;
        }

        /**
         * Provides the file name of the image files used for the locale's flag,
         * without file extension of path.
         *
         * @return flagIcon of dictionary resource
         */
        public String getFlagIcon()
        {
            return this.flagIcon;
        }

        /**
         * Sets the loading property. Indicates if this locale is currently
         * loaded in the list.
         *
         * @param loading indicates if this locale is currently loading in the
         * locales list
         */
        public void setLoading(boolean loading)
        {
            this.isLoading = loading;
        }

        /**
         * Indicates if this locale is currenly loading in the list of locales.
         *
         * @return <tt>true</tt> if the locale is loading, <tt>false</tt> -
         * otherwise
         */
        public boolean isLoading()
        {
            return isLoading;
        }

        @Override
        public String toString()
        {
            return this.label + " (" + this.isoCode + ")";
        }
    }

    /**
     * Default attribute that may be defined in the parameters xml.
     */
    public enum Default
    {
        LOCALE("locale");

        private String tag;

        Default(String tag)
        {
            this.tag = tag;
        }

        /**
         * Returns the enum representation of a string. This is case sensitive.
         *
         * @param str toString representation of a default field
         * @return default field associated with a string
         * @throws IllegalArgumentException if argument is not represented by a
         *             default field.
         */
        public static Default fromString(String str)
        {
            for (Default field : Default.values())
            {
                if (str.equals(field.toString()))
                {
                    return field;
                }
            }
            throw new IllegalArgumentException();
        }

        @Override
        public String toString()
        {
            return this.tag;
        }
    }
}
