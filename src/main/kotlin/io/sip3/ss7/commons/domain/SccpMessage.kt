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

class SccpMessage {

    var sls = 0

    var dpc = 0
    var opc = 0

    lateinit var cdpa: Address
    lateinit var cgpa: Address

    lateinit var tcapPayload: ByteArray
    val tcapMessage: TcapMessage by lazy {
        TcapMessage(tcapPayload)
    }

    fun createEchoResponse(): SccpMessage {
        val m = SccpMessage()
        m.sls = sls
        m.dpc = opc
        m.opc = dpc
        m.cdpa = cgpa
        m.cgpa = cdpa
        m.tcapPayload = tcapPayload
        return m
    }

    override fun toString(): String {
        return "SccpMessage(sls=$sls, dpc=$dpc, opc=$opc, cdpa=$cdpa, cgpa=$cgpa, tcapMessage=$tcapMessage)"
    }

    class Address {

        lateinit var gt: String
        var ssn = 146

        override fun toString(): String {
            return "Address(gt='$gt', ssn=$ssn)"
        }
    }
}