/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.sip.xcap.model.commonpolicy;

/**
 * <p>Java class for ruleType complex type.
 * <p/>
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p/>
 * <pre>
 * &lt;complexType name="ruleType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="conditions" type="{urn:ietf:params:xml:ns:common-policy}conditionsType" minOccurs="0"/>
 *         &lt;element name="actions" type="{urn:ietf:params:xml:ns:common-policy}extensibleType" minOccurs="0"/>
 *         &lt;element name="transformations" type="{urn:ietf:params:xml:ns:common-policy}extensibleType" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attribute name="id" use="required" type="{http://www.w3.org/2001/XMLSchema}ID" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 *
 * @author Grigorii Balutsel
 */
//@XmlAccessorType(XmlAccessType.FIELD)
//@XmlType(name = "ruleType", propOrder = {
//        "conditions",
//        "actions",
//        "transformations"
//        })
public class RuleType
{

//    @XmlElement(namespace = "urn:ietf:params:xml:ns:common-policy")
    protected ConditionsType conditions;

//    @XmlElement(namespace = "urn:ietf:params:xml:ns:common-policy")
    protected ActionsType actions;

//    @XmlElement(namespace = "urn:ietf:params:xml:ns:common-policy")
    protected TransfomationsType transformations;

//    @XmlAttribute(required = true)
//    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
//    @XmlID
    protected String id;

    /**
     * Gets the value of the conditions property.
     *
     * @return possible object is
     *         {@link ConditionsType }
     */
    public ConditionsType getConditions()
    {
        return conditions;
    }

    /**
     * Sets the value of the conditions property.
     *
     * @param value allowed object is
     *              {@link ConditionsType }
     */
    public void setConditions(ConditionsType value)
    {
        this.conditions = value;
    }

    /**
     * Gets the value of the actions property.
     *
     * @return possible object is
     *         {@link TransfomationsType }
     */
    public ActionsType getActions()
    {
        return actions;
    }

    /**
     * Sets the value of the actions property.
     *
     * @param value allowed object is
     *              {@link TransfomationsType }
     */
    public void setActions(ActionsType value)
    {
        this.actions = value;
    }

    /**
     * Gets the value of the transformations property.
     *
     * @return possible object is
     *         {@link TransfomationsType }
     */
    public TransfomationsType getTransformations()
    {
        return transformations;
    }

    /**
     * Sets the value of the transformations property.
     *
     * @param value allowed object is
     *              {@link TransfomationsType }
     */
    public void setTransformations(TransfomationsType value)
    {
        this.transformations = value;
    }

    /**
     * Gets the value of the id property.
     *
     * @return possible object is
     *         {@link String }
     */
    public String getId()
    {
        return id;
    }

    /**
     * Sets the value of the id property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setId(String value)
    {
        this.id = value;
    }
}
