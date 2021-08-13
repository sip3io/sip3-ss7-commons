/*
 * Copyright 2018-2021 SIP3.IO, Corp.
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

import org.mobicents.protocols.ss7.m3ua.As
import org.mobicents.protocols.ss7.m3ua.impl.AsImpl
import org.mobicents.protocols.ss7.m3ua.impl.AsState
import org.mobicents.protocols.ss7.m3ua.impl.TransitionState

fun As.patchPeerFsm() {
    this as AsImpl

    peerFSM.createTransition(TransitionState.AS_STATE_CHANGE_ACTIVE, AsState.DOWN.toString(), AsState.ACTIVE.toString())
}