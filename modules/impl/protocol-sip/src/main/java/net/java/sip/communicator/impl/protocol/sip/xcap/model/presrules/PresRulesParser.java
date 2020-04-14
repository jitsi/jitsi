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
package net.java.sip.communicator.impl.protocol.sip.xcap.model.presrules;

import static net.java.sip.communicator.impl.protocol.sip.xcap.model.XmlUtils.processAny;
import static org.jitsi.util.xml.XMLUtils.getNamespaceUri;
import static org.jitsi.util.xml.XMLUtils.hasChildElements;
import static org.jitsi.util.xml.XMLUtils.isStandartXmlNamespace;
import net.java.sip.communicator.impl.protocol.sip.xcap.model.commonpolicy.*;

import org.w3c.dom.*;

/**
 * Utility class that helps to converts pres-rules xml to the object model
 * and object model to the pres-rules xml.
 *
 * @author Grigorii Balutsel
 */
public final class PresRulesParser
{
    private static String NAMESPACE = "urn:ietf:params:xml:ns:pres-rules";

    private static String SUBHANDLING_ELEMENT = "sub-handling";

    private static String OCCURRENCE_ID_ELEMENT = "occurrence-id";

    private static String CLASS_ELEMENT = "class";

    private static String PROVIDE_DEVICES_ELEMENT = "provide-devices";

    private static String PROVIDE_DEVICES_ALL_ELEMENT = "all-devices";

    private static String PROVIDE_DEVICES_DEVICEID_ELEMENT = "deviceID";

    private static String PROVIDE_SERVICES_ELEMENT = "provide-services";

    private static String PROVIDE_SERVICES_ALL_ELEMENT = "all-services";

    private static String PROVIDE_SERVICES_SERBICE_URI_ELEMENT = "service-uri";

    private static String PROVIDE_SERVICES_SERBICE_URI_SCHEME_ELEMENT =
            "service-uri-scheme";

    private static String PROVIDE_PERSONS_ELEMENT = "provide-persons";

    private static String PROVIDE_PERSONS_ALL_ELEMENT = "all-persons";

    /**
     * Creates actions object from the element.
     *
     * @param element the element to analyze.
     * @return the actions object.
     * @throws Exception if there is some error during parsing.
     */
    public static ActionsType actionsFromElement(Element element)
            throws Exception
    {
        ActionsType actions = new ActionsType();
        if (!CommonPolicyParser.NAMESPACE
                .equals(getNamespaceUri(element)) ||
                !CommonPolicyParser.ACTIONS_ELEMENT
                        .equals(element.getLocalName()))
        {
            throw new Exception("actions element is invalid");
        }
        // Process attributes
        NamedNodeMap attributes = element.getAttributes();
        for (int i = 0; i < attributes.getLength(); i++)
        {
            Attr attribute = (Attr) attributes.item(i);
            String namespaceUri = getNamespaceUri(attribute);
            if (namespaceUri == null)
            {
                throw new Exception("actions element is invalid");
            }
            if (isStandartXmlNamespace(namespaceUri))
            {
                continue;
            }
            throw new Exception("actions element is invalid");
        }
        SubHandlingType subHandling = null;
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
            if (CommonPolicyParser.NAMESPACE.equals(namespaceUri))
            {
                throw new Exception("actions element is invalid");
            }
            else if (NAMESPACE.equals(namespaceUri))
            {
                if (!SUBHANDLING_ELEMENT.equals(localName) ||
                        subHandling != null)
                {
                    throw new Exception("actions element is invalid");
                }
                subHandling = SubHandlingType
                        .fromString(
                                childElement.getTextContent().toLowerCase());
            }
            else
            {
                // any
                actions.getAny().add(childElement);
            }
        }
        actions.setSubHandling(subHandling);
        return actions;
    }

    /**
     * Creates actions element from the object.
     *
     * @param document the xml document.
     * @param actions  the actions to analyze.
     * @return the rule element.
     * @throws Exception if there is some error during creating.
     */
    public static Element elementFromActions(
            Document document, ActionsType actions)
            throws Exception
    {
        Element element = document.createElementNS(CommonPolicyParser.NAMESPACE,
                CommonPolicyParser.ACTIONS_ELEMENT);
        if (actions.getSubHandling() != null)
        {
            Element subHandlingElement = document.createElementNS(NAMESPACE,
                    SUBHANDLING_ELEMENT);
            subHandlingElement.setTextContent(actions.getSubHandling().value());
            element.appendChild(subHandlingElement);
        }
        processAny(element, actions.getAny());
        return element;
    }

    /**
     * Creates transfomations object from the element.
     *
     * @param element the element to analyze.
     * @return the transfomations object.
     * @throws Exception if there is some error during parsing.
     */
    public static TransformationsType transformationsFromElement(
            Element element)
            throws Exception
    {
        TransformationsType transfomations = new TransformationsType();
        if (!CommonPolicyParser.NAMESPACE
                .equals(getNamespaceUri(element)) ||
                !CommonPolicyParser.TRANSFORMATIONS_ELEMENT
                        .equals(element.getLocalName()))
        {
            throw new Exception("transfomations element is invalid");
        }
        // Process attributes
        NamedNodeMap attributes = element.getAttributes();
        for (int i = 0; i < attributes.getLength(); i++)
        {
            Attr attribute = (Attr) attributes.item(i);
            String namespaceUri = getNamespaceUri(attribute);
            if (namespaceUri == null)
            {
                throw new Exception("transfomations element is invalid");
            }
            if (isStandartXmlNamespace(namespaceUri))
            {
                continue;
            }
            throw new Exception("transfomations element is invalid");
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
            if (CommonPolicyParser.NAMESPACE.equals(namespaceUri))
            {
                throw new Exception("transfomations element is invalid");
            }
            else if (NAMESPACE.equals(namespaceUri))
            {
                if (PROVIDE_DEVICES_ELEMENT.equals(localName))
                {
                    transfomations.setDevicePermission(
                            devicePermissionFromElement(childElement));
                }
                else if (PROVIDE_SERVICES_ELEMENT.equals(localName))
                {
                    transfomations.setServicePermission(
                            servicePermissionFromElement(childElement));
                }
                else if (PROVIDE_PERSONS_ELEMENT.equals(localName))
                {
                    transfomations.setPersonPermission(
                            personPermissionFromElement(childElement));
                }
                else
                {
                    // There are a lot of elements without good examples, so
                    // just put them in any elements as temporary solution.
                    transfomations.getAny().add(childElement);
                }
            }
            else
            {
                // any
                transfomations.getAny().add(childElement);
            }
        }
        return transfomations;
    }

    /**
     * Creates transformations element from the object.
     *
     * @param document        the xml document.
     * @param transformations the transformations to analyze.
     * @return the rule element.
     * @throws Exception if there is some error during creating.
     */
    public static Element elementFromTransfomations(
            Document document, TransformationsType transformations)
            throws Exception
    {
        Element element = document.createElementNS(CommonPolicyParser.NAMESPACE,
                CommonPolicyParser.TRANSFORMATIONS_ELEMENT);
        if (transformations.getDevicePermission() != null)
        {
            element.appendChild(elementFromDevicePermission(document,
                    transformations.getDevicePermission()));
        }
        if (transformations.getPersonPermission() != null)
        {
            element.appendChild(elementFromPersonPermission(document,
                    transformations.getPersonPermission()));
        }
        if (transformations.getServicePermission() != null)
        {
            element.appendChild(elementFromServicePermission(document,
                    transformations.getServicePermission()));
        }
        processAny(element, transformations.getAny());
        return element;
    }

    /**
     * Creates servicePermission object from the element.
     *
     * @param element the element to analyze.
     * @return the servicePermission object.
     * @throws Exception if there is some error during parsing.
     */
    private static ProvideServicePermissionType servicePermissionFromElement(
            Element element) throws Exception
    {
        ProvideServicePermissionType servicePermission =
                new ProvideServicePermissionType();
        if (!NAMESPACE.equals(getNamespaceUri(element)) ||
                !PROVIDE_SERVICES_ELEMENT.equals(element.getLocalName()))
        {
            throw new Exception("provide-services element is invalid");
        }
        // Process attributes
        NamedNodeMap attributes = element.getAttributes();
        for (int i = 0; i < attributes.getLength(); i++)
        {
            Attr attribute = (Attr) attributes.item(i);
            String namespaceUri = getNamespaceUri(attribute);
            if (namespaceUri == null)
            {
                throw new Exception("provide-services element is invalid");
            }
            if (isStandartXmlNamespace(namespaceUri))
            {
                continue;
            }
            throw new Exception("provide-services element is invalid");
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
                // all-services
                if (PROVIDE_SERVICES_ALL_ELEMENT.equals(localName))
                {
                    if (hasChildElements(childElement))
                    {
                        throw new Exception("all-services element is invalid");
                    }
                    servicePermission.setAllServices(
                            new ProvideServicePermissionType.AllServicesType());
                }
                // service-uri
                else if (PROVIDE_SERVICES_SERBICE_URI_ELEMENT.equals(localName))
                {
                    if (hasChildElements(childElement))
                    {
                        throw new Exception("service-uri element is invalid");
                    }
                    servicePermission.getServiceUriList().add(
                            new ProvideServicePermissionType.ServiceUriType(
                                    childElement.getTextContent()));
                }
                // service-scheme-uri
                else if (PROVIDE_SERVICES_SERBICE_URI_SCHEME_ELEMENT.
                        equals(localName))
                {
                    if (hasChildElements(childElement))
                    {
                        throw new Exception(
                                "service-scheme-uri element is invalid");
                    }
                    servicePermission.getServiceUriSchemeList()
                            .add(new ProvideServicePermissionType.
                                    ServiceUriSchemeType(
                                    childElement.getTextContent()));
                }
                // occurrence-id
                else if (OCCURRENCE_ID_ELEMENT.equals(localName))
                {
                    if (hasChildElements(childElement))
                    {
                        throw new Exception("occurrence-id element is invalid");
                    }
                    servicePermission.getOccurrences().add(new OccurrenceIdType(
                            childElement.getTextContent()));
                }
                // class
                else if (CLASS_ELEMENT.equals(localName))
                {
                    if (hasChildElements(childElement))
                    {
                        throw new Exception("class element is invalid");
                    }
                    servicePermission.getClasses().add(
                            new ClassType(childElement.getTextContent()));
                }
                else
                {
                    throw new Exception("provide-services element is invalid");
                }
            }
            else
            {
                // any
                servicePermission.getAny().add(childElement);
            }
        }
        return servicePermission;
    }

    /**
     * Creates serviceService element from the object.
     *
     * @param document       the xml document.
     * @param serviceService the serviceService to analyze.
     * @return the rule element.
     * @throws Exception if there is some error during creating.
     */
    public static Element elementFromServicePermission(
            Document document, ProvideServicePermissionType serviceService)
            throws Exception
    {
        Element element = document.createElementNS(NAMESPACE,
                PROVIDE_SERVICES_ELEMENT);
        if (serviceService.getAllServices() != null)
        {
            Element allServices = document.createElementNS(NAMESPACE,
                    PROVIDE_SERVICES_ALL_ELEMENT);
            element.appendChild(allServices);
        }
        else
        {
            for (ProvideServicePermissionType.ServiceUriType serviceUri :
                    serviceService.getServiceUriList())
            {
                Element serviceUriElement = document.createElementNS(NAMESPACE,
                        PROVIDE_SERVICES_SERBICE_URI_ELEMENT);
                serviceUriElement.setTextContent(serviceUri.getValue());
                element.appendChild(serviceUriElement);
            }
            for (ProvideServicePermissionType.ServiceUriSchemeType serviceUriSheme :
                    serviceService.getServiceUriSchemeList())
            {
                Element serviceUriElement = document.createElementNS(NAMESPACE,
                        PROVIDE_SERVICES_SERBICE_URI_SCHEME_ELEMENT);
                serviceUriElement.setTextContent(serviceUriSheme.getValue());
                element.appendChild(serviceUriElement);
            }
            for (ClassType classType : serviceService.getClasses())
            {
                Element classElement = document.createElementNS(NAMESPACE,
                        CLASS_ELEMENT);
                classElement.setTextContent(classType.getValue());
                element.appendChild(classElement);
            }
            for (OccurrenceIdType occurrence : serviceService
                    .getOccurrences())
            {
                Element occurrenceElement = document.createElementNS(NAMESPACE,
                        OCCURRENCE_ID_ELEMENT);
                occurrenceElement.setTextContent(occurrence.getValue());
                element.appendChild(occurrenceElement);
            }
        }
        processAny(element, serviceService.getAny());
        return element;
    }

    /**
     * Creates devicePermission object from the element.
     *
     * @param element the element to analyze.
     * @return the devicePermission object.
     * @throws Exception if there is some error during parsing.
     */
    private static ProvideDevicePermissionType devicePermissionFromElement(
            Element element) throws Exception
    {
        ProvideDevicePermissionType devicePermission =
                new ProvideDevicePermissionType();
        if (!NAMESPACE.equals(getNamespaceUri(element)) ||
                !PROVIDE_DEVICES_ELEMENT.equals(element.getLocalName()))
        {
            throw new Exception("provide-devices element is invalid");
        }
        // Process attributes
        NamedNodeMap attributes = element.getAttributes();
        for (int i = 0; i < attributes.getLength(); i++)
        {
            Attr attribute = (Attr) attributes.item(i);
            String namespaceUri = getNamespaceUri(attribute);
            if (namespaceUri == null)
            {
                throw new Exception("provide-devices element is invalid");
            }
            if (isStandartXmlNamespace(namespaceUri))
            {
                continue;
            }
            throw new Exception("provide-devices element is invalid");
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
                // all-devices
                if (PROVIDE_DEVICES_ALL_ELEMENT.equals(localName))
                {
                    if (hasChildElements(childElement))
                    {
                        throw new Exception("all-devices element is invalid");
                    }
                    devicePermission.setAllDevices(
                            new ProvideDevicePermissionType.AllDevicesType());
                }
                // deviceID
                else if (PROVIDE_DEVICES_DEVICEID_ELEMENT.equals(localName))
                {
                    if (hasChildElements(childElement))
                    {
                        throw new Exception("deviceID element is invalid");
                    }
                    devicePermission.getDevices().add(
                            new ProvideDevicePermissionType.DeviceIdType(
                                    childElement.getTextContent()));
                }
                // occurrence-id
                else if (OCCURRENCE_ID_ELEMENT.equals(localName))
                {
                    if (hasChildElements(childElement))
                    {
                        throw new Exception("occurrence-id element is invalid");
                    }
                    devicePermission.getOccurrences().add(new OccurrenceIdType(
                            childElement.getTextContent()));
                }
                // class
                else if (CLASS_ELEMENT.equals(localName))
                {
                    if (hasChildElements(childElement))
                    {
                        throw new Exception("class element is invalid");
                    }
                    devicePermission.getClasses().add(
                            new ClassType(childElement.getTextContent()));
                }
                else
                {
                    throw new Exception("provide-devices element is invalid");
                }
            }
            else
            {
                // any
                devicePermission.getAny().add(childElement);
            }
        }
        return devicePermission;
    }

    /**
     * Creates devicePermission element from the object.
     *
     * @param document         the xml document.
     * @param devicePermission the devicePermission to analyze.
     * @return the rule element.
     * @throws Exception if there is some error during creating.
     */
    public static Element elementFromDevicePermission(
            Document document, ProvideDevicePermissionType devicePermission)
            throws Exception
    {
        Element element = document.createElementNS(NAMESPACE,
                PROVIDE_DEVICES_ELEMENT);
        if (devicePermission.getAllDevices() != null)
        {
            Element allDevices = document.createElementNS(NAMESPACE,
                    PROVIDE_DEVICES_ALL_ELEMENT);
            element.appendChild(allDevices);
        }
        else
        {
            for (ProvideDevicePermissionType.DeviceIdType device :
                    devicePermission.getDevices())
            {
                Element deviceElement = document.createElementNS(NAMESPACE,
                        PROVIDE_DEVICES_DEVICEID_ELEMENT);
                deviceElement.setTextContent(device.getValue());
                element.appendChild(deviceElement);
            }
            for (ClassType classType : devicePermission.getClasses())
            {
                Element classElement = document.createElementNS(NAMESPACE,
                        CLASS_ELEMENT);
                classElement.setTextContent(classType.getValue());
                element.appendChild(classElement);
            }
            for (OccurrenceIdType occurrence : devicePermission
                    .getOccurrences())
            {
                Element occurrenceElement = document.createElementNS(NAMESPACE,
                        OCCURRENCE_ID_ELEMENT);
                occurrenceElement.setTextContent(occurrence.getValue());
                element.appendChild(occurrenceElement);
            }
        }
        processAny(element, devicePermission.getAny());
        return element;
    }

    /**
     * Creates personPermission object from the element.
     *
     * @param element the element to analyze.
     * @return the personPermission object.
     * @throws Exception if there is some error during parsing.
     */
    private static ProvidePersonPermissionType personPermissionFromElement(
            Element element) throws Exception
    {
        ProvidePersonPermissionType personPermission =
                new ProvidePersonPermissionType();
        if (!NAMESPACE.equals(getNamespaceUri(element)) ||
                !PROVIDE_PERSONS_ELEMENT.equals(element.getLocalName()))
        {
            throw new Exception("provide-persons element is invalid");
        }
        // Process attributes
        NamedNodeMap attributes = element.getAttributes();
        for (int i = 0; i < attributes.getLength(); i++)
        {
            Attr attribute = (Attr) attributes.item(i);
            String namespaceUri = getNamespaceUri(attribute);
            if (namespaceUri == null)
            {
                throw new Exception("provide-persons element is invalid");
            }
            if (isStandartXmlNamespace(namespaceUri))
            {
                continue;
            }
            throw new Exception("provide-persons element is invalid");
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
                // all-devices
                if (PROVIDE_PERSONS_ALL_ELEMENT.equals(localName))
                {
                    if (hasChildElements(childElement))
                    {
                        throw new Exception("all-persons element is invalid");
                    }
                    personPermission.setAllPersons(
                            new ProvidePersonPermissionType.AllPersonsType());
                }
                // occurrence-id
                else if (OCCURRENCE_ID_ELEMENT.equals(localName))
                {
                    if (hasChildElements(childElement))
                    {
                        throw new Exception("occurrence-id element is invalid");
                    }
                    personPermission.getOccurrences().add(new OccurrenceIdType(
                            childElement.getTextContent()));
                }
                // class
                else if (CLASS_ELEMENT.equals(localName))
                {
                    if (hasChildElements(childElement))
                    {
                        throw new Exception("class element is invalid");
                    }
                    personPermission.getClasses().add(
                            new ClassType(childElement.getTextContent()));
                }
                else
                {
                    throw new Exception("provide-persons element is invalid");
                }
            }
            else
            {
                // any
                personPermission.getAny().add(childElement);
            }
        }
        return personPermission;
    }

    /**
     * Creates personPermission element from the object.
     *
     * @param document         the xml document.
     * @param personPermission the personPermission to analyze.
     * @return the rule element.
     * @throws Exception if there is some error during creating.
     */
    public static Element elementFromPersonPermission(
            Document document, ProvidePersonPermissionType personPermission)
            throws Exception
    {
        Element element = document.createElementNS(NAMESPACE,
                PROVIDE_PERSONS_ELEMENT);
        if (personPermission.getAllPersons() != null)
        {
            Element allPersons = document.createElementNS(NAMESPACE,
                    PROVIDE_PERSONS_ALL_ELEMENT);
            element.appendChild(allPersons);
        }
        else
        {
            for (ClassType classType : personPermission.getClasses())
            {
                Element classElement = document.createElementNS(NAMESPACE,
                        CLASS_ELEMENT);
                classElement.setTextContent(classType.getValue());
                element.appendChild(classElement);
            }
            for (OccurrenceIdType occurrence : personPermission
                    .getOccurrences())
            {
                Element occurrenceElement = document.createElementNS(NAMESPACE,
                        OCCURRENCE_ID_ELEMENT);
                occurrenceElement.setTextContent(occurrence.getValue());
                element.appendChild(occurrenceElement);
            }
        }
        processAny(element, personPermission.getAny());
        return element;
    }
}
