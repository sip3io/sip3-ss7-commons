package io.sip3.ss7.commons.util

import org.mobicents.protocols.ss7.m3ua.As
import org.mobicents.protocols.ss7.m3ua.impl.AsImpl
import org.mobicents.protocols.ss7.m3ua.impl.AsState
import org.mobicents.protocols.ss7.m3ua.impl.TransitionState

fun As.patchPeerFsm() {
    this as AsImpl

    peerFSM.createTransition(TransitionState.AS_STATE_CHANGE_ACTIVE, AsState.DOWN.toString(), AsState.DOWN.toString())
}