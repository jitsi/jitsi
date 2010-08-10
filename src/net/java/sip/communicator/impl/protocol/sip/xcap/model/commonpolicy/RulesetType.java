/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.sip.xcap.model.commonpolicy;

import java.util.*;
import java.util.List;
//import javax.xml.bind.annotation.*;

/**
 * <p>Java class for ruleset element declaration.
 * <p/>
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p/>
 * <pre>
 * &lt;element name="ruleset">
 *   &lt;complexType>
 *     &lt;complexContent>
 *       &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *         &lt;sequence>
 *           &lt;element name="rule" type="{urn:ietf:params:xml:ns:common-policy}ruleType" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;/sequence>
 *       &lt;/restriction>
 *     &lt;/complexContent>
 *   &lt;/complexType>
 * &lt;/element>
 * </pre>
 *
 * @author Grigorii Balutsel
 */
//@XmlAccessorType(XmlAccessType.FIELD)
//@XmlType(name = "", propOrder = {
//        "rule"
//        })
//@XmlRootElement(name = "ruleset")
public class RulesetType
{
//
//    @XmlElement(namespace = "urn:ietf:params:xml:ns:common-policy",
//            required = true)
    protected List<RuleType> rule;

    /**
     * Gets the value of the rule property.
     * <p/>
     * <p/>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the rule property.
     * <p/>
     * <p/>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getRule().add(newItem);
     * </pre>
     * <p/>
     * <p/>
     * <p/>
     * Objects of the following type(s) are allowed in the list
     * {@link RuleType }
     */
    public List<RuleType> getRule()
    {
        if (rule == null)
        {
            rule = new ArrayList<RuleType>();
        }
        return this.rule;
    }
}
