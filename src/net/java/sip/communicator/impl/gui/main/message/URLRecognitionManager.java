package net.java.sip.communicator.impl.gui.main.message;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class URLRecognitionManager {
    private static String digits         =  "(?:\\d+)";
    private static String hex             =  "[a-fA-F\\d]";
    private static String alpha          =  "[a-zA-Z]"; 
    private static String alphanum    =  "[a-zA-Z\\d]";
    private static String safe             =  "[$\\-_.+]";
    private static String extra           =  "[!*'(),]";
    private static String escape         =  "(?:%"+hex+"{2})";
    private static String uchar           
        =  "(?:"+alphanum+"|"+safe+"|"+extra+"|"+escape+")";
     
    private static String qm = "\\?";
    
    private static String hostnumber   
        =  "(?:"+digits+"(?:\\."+digits+"){3})";
    private static String toplabel          
        =  "(?:"+alpha+"(?:(?:"+alphanum+"|-)*"+alphanum+")?)";
    private static String domainlabel   
        =  "(?:"+alphanum+"(?:(?:"+alphanum+"|-)*"+alphanum+")?)";
    private static String hostname       
        =  "(?:(?:"+domainlabel+"\\.)*"+toplabel+")";
    private static String host                  
        =  "(?:"+hostname+"|"+hostnumber+")";
    private static String hostPort     = "(?:"+host+"(?::"+digits+")?)";
    
    public static String processURL(String sourceMsg){
        
        return recognizeHTTP(sourceMsg);
    }
    
    private static String recognizeHTTP(String sourceMsg){
        String message = sourceMsg;
                
        String hsegment =  "(?:(?:"+uchar+"|[;:\\@&=])*)";        
        String hpath         =  "(?:" + hsegment +"(?:/" + hsegment + ")*)";
        
        //String httpurl       =  "(?:http://" + hostPort +
          //  "(?:/" + hpath + "(?:" + qm + hsegment + ")?)?)";
        String httpurl = "(?:http://(?:(?:(?:(?:(?:[a-zA-Z\\d](?:(?:[a-zA-Z\\d]|-)*[a-zA-Z\\d])?)\\." +
        ")*(?:[a-zA-Z](?:(?:[a-zA-Z\\d]|-)*[a-zA-Z\\d])?))|(?:(?:\\d+)(?:\\.(?:\\d+)" +
        "){3}))(?::(?:\\d+))?)(?:/(?:(?:(?:(?:[a-zA-Z\\d$\\-_.+!*'(),]|(?:%[a-fA-F" +
        "\\d]{2}))|[;:\\@&=])*)(?:/(?:(?:(?:[a-zA-Z\\d$\\-_.+!*'(),]|(?:%[a-fA-F\\d]{" +
        "2}))|[;:\\@&=])*))*)(?:\\?(?:(?:(?:[a-zA-Z\\d$\\-_.+!*'(),]|(?:%[a-fA-F\\d]{" +
        "2}))|[;:\\@&=])*))?)?)";
        System.out.println("REGEXP============" + httpurl);
        
        Pattern p = Pattern.compile(httpurl);
        
        Matcher m = p.matcher(message);
        
        while (m.find()) {          
            message = message.replaceAll(m.group().trim(), "<A href='"
                    + m.group() + "'>" + m.group() + "</A>");
        }
        System.out.println("EHPOOOOOOOOOO:::::::" + message);
        return message;
    }
}
