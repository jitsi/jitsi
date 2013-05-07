/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
#ifndef __MSOUTLOOKADDRBOOKCOM_MSOUTLOOKADDRBOOKCLIENTCLASSFACTORY_H
#define __MSOUTLOOKADDRBOOKCOM_MSOUTLOOKADDRBOOKCLIENTCLASSFACTORY_H

#include "ClassFactory.h"

#include "MsOutlookAddrBookClient.h"


/**
 * Implements the <tt>IClassFactory</tt> interface for the
 * <tt>IMsOutlookAddrBookClient</tt>
 * interface implementation.
 *
 * @author Lyubomir Marinov
 */
class MsOutlookAddrBookClientClassFactory:
    public ClassFactory
{
    public:
        MsOutlookAddrBookClientClassFactory();

        STDMETHODIMP CreateInstance(LPUNKNOWN, REFIID, PVOID *);

    protected:
        virtual ~MsOutlookAddrBookClientClassFactory();

    private:
        IMsOutlookAddrBookClient *_msOutlookAddrBookClient;
};

#endif /* _JMSOFFICECOMM_MSOUTLOOKADDRBOOKCLASSFACTORY_H_ */
