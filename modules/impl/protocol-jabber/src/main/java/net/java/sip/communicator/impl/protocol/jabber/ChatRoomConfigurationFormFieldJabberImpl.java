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
package net.java.sip.communicator.impl.protocol.jabber;

import java.util.*;

import java.util.stream.*;
import net.java.sip.communicator.service.protocol.*;

import org.jivesoftware.smackx.xdata.*;

/**
 * The Jabber protocol implementation of the
 * <tt>ChatRoomConfigurationFormField</tt>. This implementation is based on the
 * smack Form and FormField types.
 *
 * @author Yana Stamcheva
 */
public class ChatRoomConfigurationFormFieldJabberImpl
    implements ChatRoomConfigurationFormField
{
    /**
     * The smack library for field.
     */
    private final FormField smackFormField;

    /**
     * Store the value the user has entered in the form.
     */
    private final List<Object> fieldValues = new ArrayList<>();

    /**
     * Creates an instance of <tt>ChatRoomConfigurationFormFieldJabberImpl</tt>
     * by passing to it the smack form field and the smack submit form, which
     * are the base of this implementation.
     *
     * @param formField the smack form field
     */
    public ChatRoomConfigurationFormFieldJabberImpl(FormField formField)
    {
        this.smackFormField = formField;
    }

    /**
     * Returns the variable name of the corresponding smack property.
     *
     * @return the variable name of the corresponding smack property
     */
    public String getName()
    {
        return smackFormField.getFieldName();
    }

    /**
     * Returns the description of the corresponding smack property.
     *
     * @return the description of the corresponding smack property
     */
    public String getDescription()
    {
        return smackFormField.getDescription();
    }

    /**
     * Returns the label of the corresponding smack property.
     *
     * @return the label of the corresponding smack property
     */
    public String getLabel()
    {
        return smackFormField.getLabel();
    }

    /**
     * Returns the options of the corresponding smack property.
     *
     * @return the options of the corresponding smack property
     */
    public Iterator<String> getOptions()
    {
        if (smackFormField instanceof FormFieldWithOptions)
        {
            List<String> options = new ArrayList<>();
            for (FormField.Option smackOption :
                ((FormFieldWithOptions) smackFormField).getOptions())
            {
                options.add(smackOption.getValue().getValue().toString());
            }

            return Collections.unmodifiableList(options).iterator();
        }
        else
        {
            return Collections.emptyListIterator();
        }
    }

    /**
     * Returns the isRequired property of the corresponding smack property.
     *
     * @return the isRequired property of the corresponding smack property
     */
    public boolean isRequired()
    {
        return smackFormField.isRequired();
    }

    /**
     * For each of the smack form field types returns the corresponding
     * <tt>ChatRoomConfigurationFormField</tt> type.
     *
     * @return the type of the property
     */
    public String getType()
    {
        FormField.Type smackType = smackFormField.getType();

        if(smackType.equals(FormField.Type.bool))
            return TYPE_BOOLEAN;
        if(smackType.equals(FormField.Type.fixed))
            return TYPE_TEXT_FIXED;
        else if(smackType.equals(FormField.Type.text_private))
            return TYPE_TEXT_PRIVATE;
        else if(smackType.equals(FormField.Type.text_single))
            return TYPE_TEXT_SINGLE;
        else if(smackType.equals(FormField.Type.text_multi))
            return TYPE_TEXT_MULTI;
        else if(smackType.equals(FormField.Type.list_single))
            return TYPE_LIST_SINGLE;
        else if(smackType.equals(FormField.Type.list_multi))
            return TYPE_LIST_MULTI;
        else if(smackType.equals(FormField.Type.jid_single))
            return TYPE_ID_SINGLE;
        else if(smackType.equals(FormField.Type.jid_multi))
            return TYPE_ID_MULTI;
        else
            return TYPE_UNDEFINED;
    }

    /**
     * Returns an Iterator over the list of values of this field.
     *
     * @return an Iterator over the list of values of this field
     */
    public List<?> getInitialValues()
    {
        if(smackFormField.getType() == FormField.Type.bool)
        {
            List<Boolean> values = new ArrayList<>();
            for (CharSequence smackValue : smackFormField.getValues())
            {
                boolean boolVal =
                    smackValue.equals("1") || smackValue.toString()
                        .equalsIgnoreCase("true")
                        ? Boolean.TRUE
                        : Boolean.FALSE;
                values.add(boolVal);
            }

            return values;
        }
        else
        {
            return smackFormField.getValues();
        }
    }

    /**
     * Adds the given value to the list of values of this field.
     *
     * @param value the value to add
     */
    public void addValue(Object value)
    {
        fieldValues.add(value);
    }

    /**
     * Gets the value as entered by the user.
     */
    public List<Object> getValues()
    {
        return fieldValues;
    }

    /**
     * Gets the value as entered by the user.
     */
    public List<CharSequence> getValuesAsString()
    {
        return fieldValues.stream().map(Object::toString).collect(
            Collectors.toList());
    }
}
