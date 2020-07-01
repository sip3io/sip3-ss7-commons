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

package io.sip3.ss7.commons.domain

class SccpMessage {

    var sls = 0
    var opc = 0
    var dpc = 0

    lateinit var gtCdpa: String
    lateinit var gtCgpa: String

    lateinit var tcapPayload: ByteArray
    val tcapMessage: TcapMessage by lazy {
        TcapMessage(tcapPayload)
    }

    override fun toString(): String {
        return "SccpMessage(sls=$sls, opc=$opc, dpc=$dpc, gtCdpa='$gtCdpa', gtCgpa='$gtCgpa', tcapMessage=$tcapMessage)"
    }
}