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
package net.java.sip.communicator.service.protocol;

import java.util.*;

/**
 * The <tt>ChatRoomConfigurationFormField</tt> is contained in the
 * <tt>ChatRoomConfigurationForm</tt> and represents a configuration property of
 * a chat room. It's meant to be used by GUI-s to provide access to the user
 * to chat room configuration.
 * <br>
 * The <tt>ChatRoomConfigurationFormField</tt> defines 8 different types of
 * fields:
 * <ul>
 * <li>TYPE_TEXT_FIXED - information text, that could not be changed</li>
 * <li>TYPE_BOOLEAN - boolean values</li>
 * <li>TYPE_TEXT_PRIVATE - text, that should not be shown to the user as
 * clear text</li>
 * <li>TYPE_TEXT_MULTI - multilines text</li>
 * <li>TYPE_TEXT_SINGLE - single line text</li>
 * <li>TYPE_LIST_MULTI - multi choice list</li>
 * <li>TYPE_LIST_SINGLE - single choice list</li>
 * <li>TYPE_UNDEFINED - undefined type</li>
 * </ul>
 * The type of the field will help the GUI to determine the component to use to
 * represent the given field.
 *
 * @author Yana Stamcheva
 */
public interface ChatRoomConfigurationFormField
{
    /**
     * The undefined type is meant to be used by the implementation if they
     * don't know the type of the configuration property.
     */
    public static final String TYPE_UNDEFINED = "Undefined";

    /**
     * The fixed text type means that the value of this field is a text, that
     * could not be changed. This type is meant to be used for adding additional
     * information helping the user to complete the form.
     */
    public static final String TYPE_TEXT_FIXED = "FixedText";

    /**
     * The private text type indicates that the text, contained in this field
     * should not be shown to the user in clear text, instead if should be
     * protected by showing '*'. This type is used for passwords.
     */
    public static final String TYPE_TEXT_PRIVATE = "PrivateText";

    /**
     * The boolean type means that the value of this field is of type Boolean.
     */
    public static final String TYPE_BOOLEAN = "Boolean";

    /**
     * The multi lines text type means that the value of this field is a text
     * represented on multiple lines.
     */
    public static final String TYPE_TEXT_MULTI = "MultipleLinesText";

    /**
     * The single line text type means that the value of this field is a text
     * represented on one line.
     */
    public static final String TYPE_TEXT_SINGLE = "SingleLineText";

    /**
     * The list multi type means that the value of this field is a list that
     * allows multiple choice (i.e. multiple lines could be selected at the
     * same time).
     */
    public static final String TYPE_LIST_MULTI = "ListMultiChoice";

    /**
     * The list single type means that the value of this field is a list that
     * allows only one line to be selected at a time.
     */
    public static final String TYPE_LIST_SINGLE = "ListSingleChoice";

    /**
     * The multi id type means that the value of this field is a list of ids.
     */
    public static final String TYPE_ID_MULTI = "MultiIDChoice";

    /**
     * The id single type means that the value of this field is only one id
     * that can be selected. As TYPE_TEXT_SINGLE but contains id,
     * most probably in form of user@service.com.
     */
    public static final String TYPE_ID_SINGLE = "SingleIDChoice";

    /**
     * Returns the name of the field to be filled out. This serves as an
     * identifier of the field.
     *
     * @return the name of the field
     */
    public String getName();

    /**
     * Returns a description that provides extra clarification about the
     * field. This information could be presented to the user either in
     * tool-tip,help button, or as a section of text before the question.<p>
     *
     * @return description that provides extra clarification about the question.
     */
    public String getDescription();

    /**
     * Returns the label of the field which should give enough information to
     * the user to fill out the form.
     *
     * @return label of the question.
     */
    public String getLabel();

    /**
     * Returns an Iterator for the available options that the user has in order
     * to answer the question.
     *
     * @return Iterator for the available options.
     */
    public Iterator<String> getOptions();

    /**
     * Returns true if the question must be answered in order to complete the
     * questionnaire
     *
     * @return true if the question must be answered in order to complete the
     * questionnaire.
     */
    public boolean isRequired();


    /**
     * Returns an indicative of the format for the data to answer. The valid
     * types are all TYPE_XXX constants defined in this class.
     *
     * @return format for the data to answer.
     */
    public String getType();

    /**
     * Returns an Iterator for the default values of the field if the
     * field is part of a form to fill out. Otherwise, returns an Iterator
     * for the answered values of the field.
     *
     * @return an Iterator for the default values or answered values of the
     * field
     */
    public Iterator<?> getValues();

    /**
     * Adds the given value to the values of this field.
     *
     * @param value the value to add
     */
    public void addValue(Object value);

    /**
     * Sets the list of values for this field.
     *
     * @param newValues the values of this field
     */
    public void setValues(Object[] newValues);
}
