/*
 * Copyright 2018-2022 SIP3.IO, Corp.
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

import org.mobicents.protocols.ss7.indicator.NatureOfAddress
import org.mobicents.protocols.ss7.indicator.NumberingPlan
import org.mobicents.protocols.ss7.indicator.RoutingIndicator
import org.mobicents.protocols.ss7.sccp.impl.parameter.BCDEvenEncodingScheme
import org.mobicents.protocols.ss7.sccp.impl.parameter.BCDOddEncodingScheme
import org.mobicents.protocols.ss7.sccp.impl.parameter.GlobalTitle0100Impl
import org.mobicents.protocols.ss7.sccp.impl.parameter.SccpAddressImpl
import org.mobicents.protocols.ss7.sccp.parameter.SccpAddress

object SccpAddressFactory {

    fun create(number: String, pc: Int, ssn: Int = 146): SccpAddress {
        val encodingScheme = if (number.length % 2 == 0) BCDEvenEncodingScheme.INSTANCE else BCDOddEncodingScheme.INSTANCE

        val gt = GlobalTitle0100Impl(
            number,
            0,
            encodingScheme,
            NumberingPlan.ISDN_TELEPHONY,
            NatureOfAddress.INTERNATIONAL
        )

        return SccpAddressImpl(
            RoutingIndicator.ROUTING_BASED_ON_GLOBAL_TITLE,
            gt,
            pc,
            ssn
        )
    }
}