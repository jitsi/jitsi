package net.java.sip.communicator.service.gui;

public class ContainerID{

    private String containerName;
    
    public ContainerID(String containerName){
        this.containerName = containerName;
    }
    
    public String getID(){
        return this.containerName;
    }
    
}
