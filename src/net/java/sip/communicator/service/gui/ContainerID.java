/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.gui;

/**
 * The <tt>ContainerID</tt> wraps a string which is meant to point 
 * to a container which could contain plugin components.
 * 
 * @author Yana Stamcheva
 */
public class ContainerID{

    private String containerName;
    
    public ContainerID(String containerName){
        this.containerName = containerName;
    }
    
    public String getID(){
        return this.containerName;
    }
    
}
