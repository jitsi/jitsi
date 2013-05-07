/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
#ifndef __MSOUTLOOKADDRBOOKCOM_MSOUTLOOKADDRBOOKSERVERCLASSFACTORY_H
#define __MSOUTLOOKADDRBOOKCOM_MSOUTLOOKADDRBOOKSERVERCLASSFACTORY_H

#include "ClassFactory.h"

#include "MsOutlookAddrBookServer.h"


/**
 * Implements the <tt>IClassFactory</tt> interface for the
 * <tt>IMsOutlookAddrBookServer</tt>
 * interface implementation.
 *
 * @author Lyubomir Marinov
 */
class MsOutlookAddrBookServerClassFactory:
    public ClassFactory
{
    public:
        MsOutlookAddrBookServerClassFactory();

        STDMETHODIMP CreateInstance(LPUNKNOWN, REFIID, PVOID *);

    protected:
        virtual ~MsOutlookAddrBookServerClassFactory();

    private:
        IMsOutlookAddrBookServer *_msOutlookAddrBookServer;
};

#endif
