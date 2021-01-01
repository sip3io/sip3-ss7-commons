/*
 * Copyright 2018-2021 SIP3.IO, Inc.
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

package io.sip3.ss7.commons.domain

import org.mobicents.protocols.asn.AsnInputStream
import org.mobicents.protocols.asn.AsnOutputStream
import org.mobicents.protocols.ss7.tcap.asn.DialogPortion
import org.mobicents.protocols.ss7.tcap.asn.TcapFactory
import org.mobicents.protocols.ss7.tcap.asn.Utils
import org.mobicents.protocols.ss7.tcap.asn.comp.*

class TcapMessage {

    companion object {

        const val UNKNOWN = "UNKNOWN"
        const val BEGIN = "BEGIN"
        const val CONTINUE = "CONTINUE"
        const val END = "END"
        const val ABORT = "ABORT"
    }

    var type = UNKNOWN

    var otid: Long? = null
    var dtid: Long? = null

    var dialogPortion: DialogPortion? = null
    var components: MutableList<Component> = mutableListOf()
    var abortCause: PAbortCauseType? = null

    constructor()

    constructor(payload: ByteArray) {
        AsnInputStream(payload).use { ais ->
            when (ais.readTag()) {
                TCBeginMessage._TAG -> {
                    type = BEGIN
                    val tbm = TcapFactory.createTCBeginMessage(ais)

                    otid = Utils.decodeTransactionId(tbm.originatingTransactionId)
                    tbm.dialogPortion?.let { dialogPortion = it }
                    tbm.component?.let { components = it.toMutableList() }
                }
                TCContinueMessage._TAG -> {
                    type = CONTINUE
                    val tcm = TcapFactory.createTCContinueMessage(ais)

                    otid = Utils.decodeTransactionId(tcm.originatingTransactionId)
                    dtid = Utils.decodeTransactionId(tcm.destinationTransactionId)
                    tcm.dialogPortion?.let { dialogPortion = it }
                    tcm.component?.let { components = it.toMutableList() }
                }
                TCEndMessage._TAG -> {
                    type = END
                    val tem = TcapFactory.createTCEndMessage(ais)

                    dtid = Utils.decodeTransactionId(tem.destinationTransactionId)
                    tem.dialogPortion?.let { dialogPortion = it }
                    tem.component?.let { components = it.toMutableList() }
                }
                TCAbortMessage._TAG -> {
                    type = ABORT
                    val tam = TcapFactory.createTCAbortMessage(ais)

                    dtid = Utils.decodeTransactionId(tam.destinationTransactionId)
                    tam.dialogPortion?.let { dialogPortion = it }
                    tam.pAbortCause?.let { abortCause = it }
                }
                else -> {
                    type = UNKNOWN
                }
            }
        }
    }

    fun encode(): ByteArray {
        AsnOutputStream().use { aos ->
            when (type) {
                BEGIN -> {
                    val tbm = TcapFactory.createTCBeginMessage()

                    tbm.originatingTransactionId = Utils.encodeTransactionId(otid!!)
                    dialogPortion?.let { tbm.dialogPortion = it }
                    components.let {
                        if (components.isNotEmpty()) {
                            tbm.component = it.toTypedArray()
                        }
                    }

                    tbm.encode(aos)
                }
                CONTINUE -> {
                    val tcm = TcapFactory.createTCContinueMessage()

                    tcm.originatingTransactionId = Utils.encodeTransactionId(otid!!)
                    tcm.destinationTransactionId = Utils.encodeTransactionId(dtid!!)
                    dialogPortion?.let { tcm.dialogPortion = it }
                    components.let {
                        if (components.isNotEmpty()) {
                            tcm.component = it.toTypedArray()
                        }
                    }

                    tcm.encode(aos)
                }
                END -> {
                    val tem = TcapFactory.createTCEndMessage()

                    tem.destinationTransactionId = Utils.encodeTransactionId(dtid!!)
                    dialogPortion?.let { tem.dialogPortion = it }
                    components.let {
                        if (components.isNotEmpty()) {
                            tem.component = it.toTypedArray()
                        }
                    }

                    tem.encode(aos)
                }
                ABORT -> {
                    val tam = TcapFactory.createTCAbortMessage()

                    tam.destinationTransactionId = Utils.encodeTransactionId(dtid!!)
                    dialogPortion?.let { tam.dialogPortion = it }
                    abortCause?.let { tam.pAbortCause = it }

                    tam.encode(aos)
                }
                else -> {
                    throw UnsupportedOperationException("Unsupported TCAP message type. Message: $this")
                }
            }

            return aos.toByteArray()
        }
    }

    override fun toString(): String {
        return "TcapMessage(type='$type', otid=$otid, dtid=$dtid, dialogPortion=$dialogPortion, components=$components, abortCause=$abortCause)"
    }
}