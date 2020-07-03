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