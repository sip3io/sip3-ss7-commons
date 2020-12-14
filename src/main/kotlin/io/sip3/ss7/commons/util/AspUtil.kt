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

import javolution.util.FastMap
import org.mobicents.protocols.ss7.m3ua.Asp
import org.mobicents.protocols.ss7.m3ua.impl.AsImpl
import org.mobicents.protocols.ss7.m3ua.impl.AspImpl
import org.mobicents.protocols.ss7.m3ua.impl.AspState
import org.mobicents.protocols.ss7.m3ua.impl.TransitionState
import org.mobicents.protocols.ss7.m3ua.impl.fsm.FSMState

fun Asp.patchLocalFsm() {
    this as AspImpl

    val states = localFSM.javaClass
        .getDeclaredField("states")
        .apply {
            isAccessible = true
        }
        .get(localFSM) as FastMap<String, FSMState>

    states.get(AspState.ACTIVE_SENT.toString())?.setOnTimeOut({
        aspFactory.javaClass
            .getDeclaredMethod("sendAspActive", AsImpl::class.java)
            .apply {
                isAccessible = true
                invoke(aspFactory, `as`)
            }
    }, 2000)

    localFSM.createTransition(TransitionState.ASP_INACTIVE_ACK, AspState.ACTIVE.toString(), AspState.INACTIVE.toString())
}