/*
 * Copyright 2018-2020 SIP3.IO, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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