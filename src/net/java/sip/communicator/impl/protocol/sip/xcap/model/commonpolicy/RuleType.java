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
package net.java.sip.communicator.impl.protocol.sip.xcap.model.commonpolicy;

/**
 * The Authorization Rules rule element.
 * <p/>
 * Compliant with rfc5025
 *
 * @author Grigorii Balutsel
 */
public class RuleType
{
    /**
     * The id attribute.
     */
    private String id;

    /**
     * The conditions element.
     */
    private ConditionsType conditions;

    /**
     * The actions element.
     */
    private ActionsType actions;

    /**
     * The transformations element.
     */
    private TransformationsType transformations;

    /**
     * Gets the value of the conditions property.
     *
     * @return the conditions property.
     */
    public ConditionsType getConditions()
    {
        return conditions;
    }

    /**
     * Sets the value of the conditions property.
     *
     * @param conditions the uri to set.
     */
    public void setConditions(ConditionsType conditions)
    {
        this.conditions = conditions;
    }

    /**
     * Gets the value of the actions property.
     *
     * @return the actions property.
     */
    public ActionsType getActions()
    {
        return actions;
    }

    /**
     * Sets the value of the actions property.
     *
     * @param actions the actions to set.
     */
    public void setActions(ActionsType actions)
    {
        this.actions = actions;
    }

    /**
     * Gets the value of the transformations property.
     *
     * @return the transformations property.
     */
    public TransformationsType getTransformations()
    {
        return transformations;
    }

    /**
     * Sets the value of the transformations property.
     *
     * @param transformations the uri to set.
     */
    public void setTransformations(TransformationsType transformations)
    {
        this.transformations = transformations;
    }

    /**
     * Gets the value of the id property.
     *
     * @return the id property.
     */
    public String getId()
    {
        return id;
    }

    /**
     * Sets the value of the id property.
     *
     * @param id the uri to set.
     */
    public void setId(String id)
    {
        this.id = id;
    }
}
