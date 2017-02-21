package net.java.sip.communicator.launcher;

import org.jxmpp.jid.impl.*;
import org.jxmpp.stringprep.*;

/**
 * Created by Ingo on 07.02.2017.
 */
public class Test
{
    public static void main(String[] args)
            throws XmppStringprepException
    {
        System.out.println(JidCreate.from("asdf").getClass());
        System.out.println(JidCreate.from("asdf").hasLocalpart());
//        System.out.println(JidCreate.from("asdf.ch").getClass());
//        System.out.println(JidCreate.from("asdf@ch").getClass());
//        System.out.println(JidCreate.from("asdf@asdf.ch").getClass());
//        System.out.println(JidCreate.from("asdf/asdf").getClass());
//        System.out.println(JidCreate.from("asdf.ch/asdf").getClass());
//        System.out.println(JidCreate.from("asdf.ch/asdf").getClass());
//        System.out.println(JidCreate.entityBareFrom("asdf"));
    }
}
