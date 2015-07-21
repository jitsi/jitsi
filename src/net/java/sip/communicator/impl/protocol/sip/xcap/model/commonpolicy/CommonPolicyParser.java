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

import static net.java.sip.communicator.impl.protocol.sip.xcap.model.XmlUtils.processAny;
import static org.jitsi.util.StringUtils.isNullOrEmpty;
import static org.jitsi.util.xml.XMLUtils.createDocument;
import static org.jitsi.util.xml.XMLUtils.createXml;
import static org.jitsi.util.xml.XMLUtils.getNamespaceUri;
import static org.jitsi.util.xml.XMLUtils.isStandartXmlNamespace;

import java.util.*;

import net.java.sip.communicator.impl.protocol.sip.xcap.model.*;
import net.java.sip.communicator.impl.protocol.sip.xcap.model.presrules.*;

import org.w3c.dom.*;

/**
 * Utility class that helps to converts common-policy xml to the object model
 * and object model to the common-policy xml.
 *
 * @author Grigorii Balutsel
 */
public final class CommonPolicyParser
{
    /**
     * The namespace of the common-policy.
     */
    public static String NAMESPACE = "urn:ietf:params:xml:ns:common-policy";

    /**
     * The ruleset element name.
     */
    public static String RULESET_ELEMENT = "ruleset";

    /**
     * The rule element name.
     */
    public static String RULE_ELEMENT = "rule";

    /**
     * The rule id attribute element name.
     */
    public static String RULE_ID_ATTR = "id";

    /**
     * The conditions element name.
     */
    public static String CONDITIONS_ELEMENT = "conditions";

    /**
     * The actions element name.
     */
    public static String ACTIONS_ELEMENT = "actions";

    /**
     * The transformations element name.
     */
    public static String TRANSFORMATIONS_ELEMENT = "transformations";

    /**
     * The identify element name.
     */
    public static String IDENTITY_ELEMENT = "identity";

    /**
     * The sphere element name.
     */
    public static String SPHERE_ELEMENT = "sphere";

    /**
     * The sphere value element name.
     */
    public static String SPHERE_VALUE_ATTR = "value";

    /**
     * The validity element name.
     */
    public static String VALIDITY_ELEMENT = "validity";

    /**
     * The validity-from element name.
     */
    public static String VALIDITY_FROM_ELEMENT = "from";

    /**
     * The validity-until element name.
     */
    public static String VALIDITY_UNTIL_ELEMENT = "until";

    /**
     * The one element name.
     */
    public static String ONE_ELEMENT = "one";

    /**
     * The one-id element name.
     */
    public static String ONE_ID_ATTR = "id";

    /**
     * The many element name.
     */
    public static String MANY_ELEMENT = "many";

    /**
     * The many domain element name.
     */
    public static String MANY_DOMAIN_ATTR = "domain";

    /**
     * The except element name.
     */
    public static String EXCEPT_ELEMENT = "except";

    /**
     * The except id element name.
     */
    public static String EXCEPT_ID_ATTR = "id";

    /**
     * The except domain element name.
     */
    public static String EXCEPT_DOMAIN_ATTR = "domain";

    private CommonPolicyParser()
    {
    }

    /**
     * Creates ruleset object from the element.
     *
     * @param xml the XML to analyze.
     * @return the ruleset object.
     * @throws ParsingException if there is some error during parsing.
     */
    public static RulesetType fromXml(String xml)
            throws ParsingException
    {
        if (isNullOrEmpty(xml))
        {
            throw new IllegalArgumentException("XML cannot be null or empty");
        }
        try
        {
            RulesetType ruleset = new RulesetType();
            Document document = createDocument(xml);
            Element rulesetElement = document.getDocumentElement();
            if (!NAMESPACE.equals(getNamespaceUri(rulesetElement))
                    || !RULESET_ELEMENT.equals(rulesetElement.getLocalName()))
            {
                throw new Exception("Document doesn't contain ruleset " +
                        "element");
            }
            // Process attributes
            NamedNodeMap attributes = rulesetElement.getAttributes();
            for (int i = 0; i < attributes.getLength(); i++)
            {
                Attr attribute = (Attr) attributes.item(i);
                String namespaceUri = getNamespaceUri(attribute);
                if (namespaceUri == null)
                {
                    throw new Exception("ruleset element is invalid");
                }
                if (isStandartXmlNamespace(namespaceUri))
                {
                    continue;
                }
                throw new Exception("ruleset element is invalid");
            }
            // Process elements
            NodeList childNodes = rulesetElement.getChildNodes();
            for (int i = 0; i < childNodes.getLength(); i++)
            {
                Node node = childNodes.item(i);
                if (node.getNodeType() != Node.ELEMENT_NODE)
                {
                    continue;
                }
                Element childElement = (Element) node;
                ruleset.getRules().add(ruleFromElement(childElement));
            }
            return ruleset;
        }
        catch (Exception ex)
        {
            throw new ParsingException(ex);
        }
    }

    /**
     * Creates XML from the ruleset element.
     *
     * @param ruleset the ruleset to analyze.
     * @return the ruleset xml.
     * @throws ParsingException if there is some error during parsing.
     */
    public static String toXml(RulesetType ruleset)
            throws ParsingException
    {
        if (ruleset == null)
        {
            throw new IllegalArgumentException("ruleset cannot be null");
        }
        try
        {
            Document document = createDocument();
            Element rulesetElement =
                    document.createElementNS(NAMESPACE, RULESET_ELEMENT);
            for (RuleType rule : ruleset.getRules())
            {
                rulesetElement
                        .appendChild(elementFromRule(document, rule));
            }
            document.appendChild(rulesetElement);
            return createXml(document);
        }
        catch (Exception ex)
        {
            throw new ParsingException(ex);
        }
    }

    /**
     * Creates rule object from the element.
     *
     * @param element the element to analyze.
     * @return the ruleset object.
     * @throws ParsingException if there is some error during parsing.
     */
    private static RuleType ruleFromElement(Element element)
            throws Exception
    {
        RuleType rule = new RuleType();
        if (!NAMESPACE.equals(getNamespaceUri(element)) ||
                !RULE_ELEMENT.equals(element.getLocalName()))

        {
            throw new Exception("rule element is invalid");
        }
        String id = null;
        // Process attributes
        NamedNodeMap attributes = element.getAttributes();
        for (int i = 0; i < attributes.getLength(); i++)
        {
            Attr attribute = (Attr) attributes.item(i);
            String namespaceUri = getNamespaceUri(attribute);
            if (namespaceUri == null)
            {
                throw new Exception("rule element is invalid");
            }
            if (isStandartXmlNamespace(namespaceUri))
            {
                continue;
            }
            if (!NAMESPACE.equals(namespaceUri) ||
                    !RULE_ID_ATTR.equals(attribute.getLocalName()) ||
                    id != null)
            {
                throw new Exception("rule element is invalid");
            }
            id = attribute.getValue();
        }
        // Process elements
        NodeList childNodes = element.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++)
        {
            Node node = childNodes.item(i);
            if (node.getNodeType() != Node.ELEMENT_NODE)
            {
                continue;
            }
            Element childElement = (Element) node;
            String localName = childElement.getLocalName();
            String namespaceUri = getNamespaceUri(childElement);
            if (!NAMESPACE.equals(namespaceUri))
            {
                throw new Exception("rule element is invalid");
            }
            // conditions
            if (CONDITIONS_ELEMENT.equals(localName))
            {
                rule.setConditions(conditionsFromElement(childElement));
            }
            // actions
            else if (ACTIONS_ELEMENT.equals(localName))
            {
                rule.setActions(
                        PresRulesParser.actionsFromElement(childElement));
            }
            // transformations
            else if (TRANSFORMATIONS_ELEMENT.equals(localName))
            {
                rule.setTransformations(
                        PresRulesParser.transformationsFromElement(
                                childElement));
            }
            else
            {
                throw new Exception("rule element is invalid");
            }
        }
        if (id == null)
        {
            throw new Exception("rule id attribute is missed");
        }
        rule.setId(id);
        return rule;
    }

    /**
     * Creates rule element from the object.
     *
     * @param document the xml document.
     * @param rule     the rule to analyze.
     * @return the rule element.
     * @throws Exception if there is some error during creating.
     */
    private static Element elementFromRule(Document document, RuleType rule)
            throws Exception
    {
        Element element = document.createElementNS(NAMESPACE, RULE_ELEMENT);
        if (isNullOrEmpty(rule.getId()))
        {
            throw new Exception("rule element is invalid");
        }
        element.setAttribute(RULE_ID_ATTR, rule.getId());
        // conditions
        if (rule.getConditions() != null)
        {
            element.appendChild(
                    elementFromConditions(document, rule.getConditions()));
        }
        // actions
        if (rule.getActions() != null)
        {
            element.appendChild(PresRulesParser.elementFromActions(document,
                    rule.getActions()));
        }
        // transformations
        if (rule.getTransformations() != null)
        {
            element.appendChild(PresRulesParser.elementFromTransfomations(document,
                    rule.getTransformations()));
        }
        return element;
    }

    /**
     * Creates conditions object from the element.
     *
     * @param element the element to analyze.
     * @return the ruleset object.
     * @throws ParsingException if there is some error during parsing.
     */
    private static ConditionsType conditionsFromElement(Element element)
            throws Exception
    {
        ConditionsType conditions = new ConditionsType();
        if (!NAMESPACE.equals(getNamespaceUri(element)) ||
                !CONDITIONS_ELEMENT.equals(element.getLocalName()))
        {
            throw new Exception("conditions element is invalid");
        }
        // Process attributes
        NamedNodeMap attributes = element.getAttributes();
        for (int i = 0; i < attributes.getLength(); i++)
        {
            Attr attribute = (Attr) attributes.item(i);
            String namespaceUri = getNamespaceUri(attribute);
            if (namespaceUri == null)
            {
                throw new Exception("conditions element is invalid");
            }
            if (isStandartXmlNamespace(namespaceUri))
            {
                continue;
            }
            throw new Exception("conditions element is invalid");
        }
        // Process elements
        NodeList childNodes = element.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++)
        {
            Node node = childNodes.item(i);
            if (node.getNodeType() != Node.ELEMENT_NODE)
            {
                continue;
            }
            Element childElement = (Element) node;
            String localName = childElement.getLocalName();
            String namespaceUri = getNamespaceUri(childElement);
            if (NAMESPACE.equals(namespaceUri))
            {
                // identity
                if (IDENTITY_ELEMENT.equals(localName))
                {
                    conditions.getIdentities()
                            .add(identityFromElement(childElement));
                }
                // sphere
                else if (SPHERE_ELEMENT.equals(localName))
                {
                    conditions.getSpheres()
                            .add(sphereFromElement(childElement));
                }
                // validity
                else if (VALIDITY_ELEMENT.equals(localName))
                {
                    conditions.getValidities()
                            .add(validityFromElement(childElement));
                }
                else
                {
                    throw new Exception("conditions element is invalid");
                }
            }
            else
            {
                // any
                conditions.getAny().add(childElement);
            }
        }
        return conditions;
    }

    /**
     * Creates conditions element from the object.
     *
     * @param document   the xml document.
     * @param conditions the conditions to analyze.
     * @return the conditions element.
     * @throws Exception if there is some error during creating.
     */
    private static Element elementFromConditions(
            Document document, ConditionsType conditions) throws Exception
    {
        Element element =
                document.createElementNS(NAMESPACE, CONDITIONS_ELEMENT);

        // identity
        for (IdentityType identity : conditions.getIdentities())
        {
            element.appendChild(elementFromIdentity(document, identity));
        }
        // sphere
        for (SphereType sphere : conditions.getSpheres())
        {
            element.appendChild(elementFromSphere(document, sphere));
        }
        for (ValidityType validity : conditions.getValidities())
        {
            element.appendChild(elementFromValidity(document, validity));
        }
        processAny(element, conditions.getAny());
        return element;
    }

    /**
     * Creates validity object from the element.
     *
     * @param element the element to analyze.
     * @return the list object.
     * @throws Exception if there is some error during parsing.
     */
    private static ValidityType validityFromElement(Element element)
            throws Exception
    {
        ValidityType validity = new ValidityType();
        if (!NAMESPACE.equals(getNamespaceUri(element)) ||
                !VALIDITY_ELEMENT.equals(element.getLocalName()))
        {
            throw new Exception("validity element is invalid");
        }
        // Process attributes
        NamedNodeMap attributes = element.getAttributes();
        for (int i = 0; i < attributes.getLength(); i++)
        {
            Attr attribute = (Attr) attributes.item(i);
            String namespaceUri = getNamespaceUri(attribute);
            if (namespaceUri == null)
            {
                throw new Exception("sphere element is invalid");
            }
            if (isStandartXmlNamespace(namespaceUri))
            {
                continue;
            }

            throw new Exception("validity element is invalid");
        }
        // Process elements
        NodeList childNodes = element.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++)
        {
            Node node = childNodes.item(i);
            if (node.getNodeType() != Node.ELEMENT_NODE)
            {
                continue;
            }
            Element childElement = (Element) node;
            String localName = childElement.getLocalName();
            String namespaceUri = getNamespaceUri(childElement);
            if (!NAMESPACE.equals(namespaceUri))
            {
                throw new Exception("sphere element is invalid");
            }
            if (VALIDITY_FROM_ELEMENT.equals(localName))
            {
                validity.getFromList().add(childElement.getTextContent());
            }
            else if (VALIDITY_UNTIL_ELEMENT.equals(localName))
            {
                validity.getUntilList().add(childElement.getTextContent());
            }
            else
            {
                throw new Exception("sphere element is invalid");
            }
        }
        return validity;
    }

    /**
     * Creates validity element from the object.
     *
     * @param document the xml document.
     * @param validity the validity to analyze.
     * @return the conditions element.
     * @throws Exception if there is some error during creating.
     */
    private static Element elementFromValidity(
            Document document, ValidityType validity) throws Exception
    {
        Element element = document.createElementNS(NAMESPACE, VALIDITY_ELEMENT);
        for (String from : validity.getFromList())
        {
            Element fromElement =
                    document.createElementNS(NAMESPACE, VALIDITY_FROM_ELEMENT);
            fromElement.setTextContent(from);
            element.appendChild(fromElement);
        }
        for (String until : validity.getUntilList())
        {
            Element untilElement =
                    document.createElementNS(NAMESPACE, VALIDITY_UNTIL_ELEMENT);
            untilElement.setTextContent(until);
            element.appendChild(untilElement);
        }
        return element;
    }

    /**
     * Creates sphere object from the element.
     *
     * @param element the element to analyze.
     * @return the list object.
     * @throws Exception if there is some error during parsing.
     */
    private static SphereType sphereFromElement(Element element)
            throws Exception
    {
        SphereType sphere = new SphereType();
        if (!NAMESPACE.equals(getNamespaceUri(element)) ||
                !SPHERE_ELEMENT.equals(element.getLocalName()))

        {
            throw new Exception("sphere element is invalid");
        }
        String value = null;
        // Process attributes
        NamedNodeMap attributes = element.getAttributes();
        for (int i = 0; i < attributes.getLength(); i++)
        {
            Attr attribute = (Attr) attributes.item(i);
            String namespaceUri = getNamespaceUri(attribute);
            if (namespaceUri == null)
            {
                throw new Exception("sphere element is invalid");
            }
            if (isStandartXmlNamespace(namespaceUri))
            {
                continue;
            }
            if (!NAMESPACE.equals(namespaceUri) ||
                    !SPHERE_VALUE_ATTR.equals(attribute.getLocalName()) ||
                    value != null)
            {
                throw new Exception("sphere element is invalid");
            }
            value = attribute.getValue();
        }
        // Process elements
        NodeList childNodes = element.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++)
        {
            Node node = childNodes.item(i);
            if (node.getNodeType() != Node.ELEMENT_NODE)
            {
                continue;
            }
            throw new Exception("sphere element is invalid");
        }
        if (value == null)
        {
            throw new Exception("sphere value attribute is missed");
        }
        sphere.setValue(value);
        return sphere;
    }

    /**
     * Creates sphere element from the object.
     *
     * @param document the xml document.
     * @param sphere   the sphere to analyze.
     * @return the conditions element.
     * @throws Exception if there is some error during creating.
     */
    private static Element elementFromSphere(
            Document document, SphereType sphere) throws Exception
    {
        Element element = document.createElementNS(NAMESPACE, SPHERE_ELEMENT);
        if (isNullOrEmpty(sphere.getValue()))
        {
            throw new Exception("sphere value attribute is missed");
        }
        element.setAttribute(SPHERE_VALUE_ATTR, sphere.getValue());
        return element;
    }

    /**
     * Creates identity object from the element.
     *
     * @param element the element to analyze.
     * @return the list object.
     * @throws Exception if there is some error during parsing.
     */
    private static IdentityType identityFromElement(Element element)
            throws Exception
    {
        IdentityType identity = new IdentityType();
        if (!NAMESPACE.equals(getNamespaceUri(element)) ||
                !IDENTITY_ELEMENT.equals(element.getLocalName()))
        {
            throw new Exception("identity element is invalid");
        }
        // Process attributes
        NamedNodeMap attributes = element.getAttributes();
        for (int i = 0; i < attributes.getLength(); i++)
        {
            Attr attribute = (Attr) attributes.item(i);
            String namespaceUri = getNamespaceUri(attribute);
            if (namespaceUri == null)
            {
                throw new Exception("identity element is invalid");
            }
            if (isStandartXmlNamespace(namespaceUri))
            {
                continue;
            }
            throw new Exception("identity element is invalid");
        }
        // Process elements
        NodeList childNodes = element.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++)
        {
            Node node = childNodes.item(i);
            if (node.getNodeType() != Node.ELEMENT_NODE)
            {
                continue;
            }
            Element childElement = (Element) node;
            String localName = childElement.getLocalName();
            String namespaceUri = getNamespaceUri(childElement);
            if (NAMESPACE.equals(namespaceUri))
            {
                // one
                if (ONE_ELEMENT.equals(localName))
                {
                    identity.getOneList().add(oneFromElement(childElement));
                }
                // many
                else if (MANY_ELEMENT.equals(localName))
                {
                    identity.getManyList().add(manyFromElement(childElement));
                }
                else
                {
                    throw new Exception("identity element is invalid");
                }
            }
            else
            {
                // any
                identity.getAny().add(childElement);
            }
        }
        return identity;
    }

    /**
     * Creates identity element from the object.
     *
     * @param document the xml document.
     * @param identity the identity to analyze.
     * @return the conditions element.
     * @throws Exception if there is some error during creating.
     */
    private static Element elementFromIdentity(
            Document document, IdentityType identity) throws Exception
    {
        Element element = document.createElementNS(NAMESPACE, IDENTITY_ELEMENT);
        // one
        for (OneType one : identity.getOneList())
        {
            element.appendChild(elementFromOne(document, one));
        }
        // many
        for (ManyType many : identity.getManyList())
        {
            element.appendChild(elementFromMany(document, many));
        }
        processAny(element, identity.getAny());
        return element;
    }

    /**
     * Creates one object from the element.
     *
     * @param element the element to analyze.
     * @return the list object.
     * @throws Exception if there is some error during parsing.
     */
    private static OneType oneFromElement(Element element) throws Exception
    {
        OneType one = new OneType();
        if (!NAMESPACE.equals(getNamespaceUri(element)) ||
                !ONE_ELEMENT.equals(element.getLocalName()))
        {
            throw new Exception("one element is invalid");
        }
        String id = null;
        // Process attributes
        NamedNodeMap attributes = element.getAttributes();
        for (int i = 0; i < attributes.getLength(); i++)
        {
            Attr attribute = (Attr) attributes.item(i);
            String namespaceUri = getNamespaceUri(attribute);
            if (namespaceUri == null)
            {
                throw new Exception("one element is invalid");
            }
            if (isStandartXmlNamespace(namespaceUri))
            {
                continue;
            }
            if (!NAMESPACE.equals(namespaceUri) ||
                    !ONE_ID_ATTR.equals(attribute.getLocalName()) ||
                    id != null)
            {
                throw new Exception("one element is invalid");
            }
            id = attribute.getValue();
        }
        Element any = null;
        // Process elements
        NodeList childNodes = element.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++)
        {
            Node node = childNodes.item(i);
            if (node.getNodeType() != Node.ELEMENT_NODE)
            {
                continue;
            }
            Element childElement = (Element) node;
            String namespaceUri = getNamespaceUri(childElement);
            if (NAMESPACE.equals(namespaceUri) ||
                    any != null)
            {
                throw new Exception("one element is invalid");
            }
            any = childElement;
        }
        if (id == null)
        {
            throw new Exception("one id attribute is missed");
        }
        one.setId(id);
        one.setAny(any);
        return one;
    }

    /**
     * Creates one element from the object.
     *
     * @param document the xml document.
     * @param one      the one to analyze.
     * @return the conditions element.
     * @throws Exception if there is some error during creating.
     */
    private static Element elementFromOne(Document document, OneType one)
            throws Exception
    {
        Element element = document.createElementNS(NAMESPACE, ONE_ELEMENT);
        if (isNullOrEmpty(one.getId()))
        {
            throw new Exception("one id attribute is missed");
        }
        element.setAttribute(ONE_ID_ATTR, one.getId());
        if (one.getAny() != null)
        {
            List<Element> any = new ArrayList<Element>();
            any.add(one.getAny());
            processAny(element, any);
        }
        return element;
    }

    /**
     * Creates many object from the element.
     *
     * @param element the element to analyze.
     * @return the list object.
     * @throws Exception if there is some error during parsing.
     */
    private static ManyType manyFromElement(Element element) throws Exception
    {
        ManyType many = new ManyType();
        if (!NAMESPACE.equals(getNamespaceUri(element)) ||
                !MANY_ELEMENT.equals(element.getLocalName()))
        {
            throw new Exception("many element is invalid");
        }
        String domain = null;
        // Process attributes
        NamedNodeMap attributes = element.getAttributes();
        for (int i = 0; i < attributes.getLength(); i++)
        {
            Attr attribute = (Attr) attributes.item(i);
            String namespaceUri = getNamespaceUri(attribute);
            if (namespaceUri == null)
            {
                throw new Exception("many element is invalid");
            }
            if (isStandartXmlNamespace(namespaceUri))
            {
                continue;
            }
            if (!NAMESPACE.equals(namespaceUri) ||
                    !MANY_DOMAIN_ATTR.equals(attribute.getLocalName()) ||
                    domain != null)
            {
                throw new Exception("many element is invalid");
            }
            domain = attribute.getValue();
        }
        // Process elements
        NodeList childNodes = element.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++)
        {
            Node node = childNodes.item(i);
            if (node.getNodeType() != Node.ELEMENT_NODE)
            {
                continue;
            }
            Element childElement = (Element) node;
            String namespaceUri = getNamespaceUri(childElement);
            String localName = childElement.getLocalName();
            if (NAMESPACE.equals(namespaceUri))
            {
                if (EXCEPT_ELEMENT.equals(localName))
                {
                    many.getExcepts().add(exceptFromElement(childElement));
                }
                else
                {
                    throw new Exception("many element is invalid");
                }
            }
            else
            {
                many.getAny().add(childElement);
            }
        }
        many.setDomain(domain);
        return many;
    }

    /**
     * Creates many element from the object.
     *
     * @param document the xml document.
     * @param many     the many to analyze.
     * @return the conditions element.
     * @throws Exception if there is some error during creating.
     */
    private static Element elementFromMany(Document document, ManyType many)
            throws Exception
    {
        Element element = document.createElementNS(NAMESPACE, MANY_ELEMENT);
        if (many.getDomain() != null)
        {
            element.setAttribute(MANY_DOMAIN_ATTR, many.getDomain());
        }
        for (ExceptType except : many.getExcepts())
        {
            element.appendChild(elementFromExept(document, except));
        }
        processAny(element, many.getAny());
        return element;
    }

    /**
     * Creates except object from the element.
     *
     * @param element the element to analyze.
     * @return the list object.
     * @throws Exception if there is some error during parsing.
     */
    private static ExceptType exceptFromElement(Element element)
            throws Exception
    {
        ExceptType except = new ExceptType();
        if (!NAMESPACE.equals(getNamespaceUri(element)) ||
                !EXCEPT_ELEMENT.equals(element.getLocalName()))
        {
            throw new Exception("except element is invalid");
        }
        String id = null;
        String domain = null;
        // Process attributes
        NamedNodeMap attributes = element.getAttributes();
        for (int i = 0; i < attributes.getLength(); i++)
        {
            Attr attribute = (Attr) attributes.item(i);
            String namespaceUri = getNamespaceUri(attribute);
            if (namespaceUri == null)
            {
                throw new Exception("except element is invalid");
            }
            if (isStandartXmlNamespace(namespaceUri))
            {
                continue;
            }
            if (!NAMESPACE.equals(namespaceUri))
            {
                throw new Exception("except element is invalid");
            }
            if (EXCEPT_ID_ATTR.equals(attribute.getLocalName()) && id == null)
            {
                id = attribute.getValue();
                continue;
            }
            else if (EXCEPT_DOMAIN_ATTR.equals(attribute.getLocalName()) &&
                    domain == null)
            {
                domain = attribute.getValue();
                continue;
            }
            throw new Exception("except element is invalid");
        }
        // Process elements
        NodeList childNodes = element.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++)
        {
            Node node = childNodes.item(i);
            if (node.getNodeType() != Node.ELEMENT_NODE)
            {
                continue;
            }
            throw new Exception("except element is invalid");
        }
        except.setId(id);
        except.setDomain(domain);
        return except;
    }

    /**
     * Creates except element from the object.
     *
     * @param document the xml document.
     * @param except   the except to analyze.
     * @return the conditions element.
     * @throws Exception if there is some error during creating.
     */
    private static Element elementFromExept(
            Document document, ExceptType except)
            throws Exception
    {
        Element element = document.createElementNS(NAMESPACE, EXCEPT_ELEMENT);
        if (except.getId() != null)
        {
            element.setAttribute(EXCEPT_ID_ATTR, except.getId());
        }
        if (except.getDomain() != null)
        {
            element.setAttribute(EXCEPT_DOMAIN_ATTR, except.getDomain());
        }
        return element;
    }
}
