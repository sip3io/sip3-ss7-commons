package io.sip3.ss7.commons.util

import org.mobicents.protocols.ss7.m3ua.M3UAManagement
import org.mobicents.protocols.ss7.m3ua.impl.M3UAManagementImpl
import org.mobicents.protocols.ss7.m3ua.impl.M3UARouteManagement

fun M3UAManagement.patchLinksetSelection() {
    this as M3UAManagementImpl

    isUseLsbForLinksetSelection = true

    val routeManagement = M3UARouteManagement::class.java
            .getDeclaredConstructor(M3UAManagementImpl::class.java)
            .apply {
                isAccessible = true
            }
            .newInstance(this)

    M3UAManagementImpl::class.java
            .getDeclaredField("routeManagement")
            .apply {
                isAccessible = true
            }
            .set(this, routeManagement)
}