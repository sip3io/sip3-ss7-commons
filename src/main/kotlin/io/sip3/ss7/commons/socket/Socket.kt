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

package io.sip3.ss7.commons.socket

import io.sip3.commons.micrometer.Metrics
import io.sip3.commons.vertx.annotations.Instance
import io.sip3.commons.vertx.util.localRequest
import io.sip3.ss7.commons.Routes
import io.sip3.ss7.commons.domain.SccpMessage
import io.sip3.ss7.commons.util.SccpAddressFactory
import io.sip3.ss7.commons.util.patchLocalFsm
import io.sip3.ss7.commons.util.patchPeerFsm
import io.vertx.core.AbstractVerticle
import io.vertx.core.json.JsonObject
import mu.KotlinLogging
import org.mobicents.protocols.api.IpChannelType
import org.mobicents.protocols.api.Management
import org.mobicents.protocols.sctp.ManagementImpl
import org.mobicents.protocols.ss7.m3ua.ExchangeType
import org.mobicents.protocols.ss7.m3ua.Functionality
import org.mobicents.protocols.ss7.m3ua.M3UAManagement
import org.mobicents.protocols.ss7.m3ua.impl.M3UAManagementImpl
import org.mobicents.protocols.ss7.mtp.Mtp3PausePrimitive
import org.mobicents.protocols.ss7.mtp.Mtp3ResumePrimitive
import org.mobicents.protocols.ss7.mtp.Mtp3UserPart
import org.mobicents.protocols.ss7.sccp.*
import org.mobicents.protocols.ss7.sccp.impl.SccpStackImpl
import org.mobicents.protocols.ss7.sccp.impl.message.SccpDataMessageImpl
import org.mobicents.protocols.ss7.sccp.impl.parameter.ProtocolClassImpl
import org.mobicents.protocols.ss7.sccp.message.SccpDataMessage
import org.mobicents.protocols.ss7.sccp.message.SccpNoticeMessage
import kotlin.system.exitProcess

@Instance(singleton = true)
class Socket : AbstractVerticle(), SccpListener {

    private val logger = KotlinLogging.logger {}

    companion object {
        const val ASSOCIATION_NAME = "ASS"
        const val ASP_NAME = "ASP"
        const val AS_NAME = "AS"
        const val NETWORK_INDICATOR = 3
    }

    private var ssnApp = 146

    private lateinit var sctp: Management
    private lateinit var m3ua: M3UAManagement
    private lateinit var sccp: SccpStack

    private val sccpMessagesReceived = Metrics.counter("sccp_messages_received")
    private val sccpMessagesSent = Metrics.counter("sccp_messages_sent")

    override fun start() {
        config().getJsonObject("socket")?.getInteger("ssnApp")?.let {
            ssnApp = it
        }

        try {
            open()
        } catch (e: Exception) {
            logger.error(e) { "Socket 'open()' failed." }
            exitProcess(-1)
        }

        vertx.eventBus().localConsumer<SccpMessage>(Routes.send) { event ->
            try {
                send(event.body())
            } catch (e: Exception) {
                logger.error(e) { "Socket 'send()' failed." }
            }
        }
    }

    private fun open() {
        config().getJsonObject("socket").let { config ->
            val priAddr = config.getString("priAddr")
            val secAddr = config.getString("secAddr")

            // Configure SCTP layer
            sctp = object : ManagementImpl("SCTP") {
                override fun store() {
                    // Do nothing...
                }
            }.apply {
                start()
                removeAllResourses()
            }
            config.getJsonArray("stp")?.forEach { stp ->
                stp as JsonObject
                val lPort = stp.getInteger("lPort")
                val rAddr = stp.getString("rAddr")
                val rPort = stp.getInteger("rPort")

                sctp.addAssociation(priAddr, lPort, rAddr, rPort, ASSOCIATION_NAME + "_$lPort", IpChannelType.SCTP, arrayOf(secAddr))
            }

            val pcApp = config.getInteger("pcApp")

            // Configure M3UA layer
            m3ua = object : M3UAManagementImpl("M3UA", null) {
                override fun store() {
                    // Do nothing...
                }
            }.apply {
                transportManagement = sctp
                start()
                removeAllResourses()
            }
            config.getJsonArray("stp")?.forEach { stp ->
                stp as JsonObject
                val lPort = stp.getInteger("lPort")
                val rPc = stp.getInteger("rPC")

                m3ua.createAspFactory(ASP_NAME + "_$lPort", ASSOCIATION_NAME + "_$lPort", false)
                m3ua.createAs(AS_NAME + "_$lPort", Functionality.AS, ExchangeType.SE, null, null, null, 1, null).apply {
                    // This patch will help AS to recover in case of blocked M3UA links
                    patchPeerFsm()
                }
                m3ua.assignAspToAs(AS_NAME + "_$lPort", ASP_NAME + "_$lPort").apply {
                    // This patch will help ASP to recover in case of blocked M3UA links
                    patchLocalFsm()
                }
                m3ua.addRoute(rPc, pcApp, -1, AS_NAME + "_$lPort")
            }

            // Configure SCCP layer
            sccp = object : SccpStackImpl("SCCP") {
                override fun onMtp3PauseMessage(msg: Mtp3PausePrimitive) {
                    // Do nothing...
                }

                override fun onMtp3ResumeMessage(msg: Mtp3ResumePrimitive) {
                    // Do nothing...
                }

                override fun store() {
                    // Do nothing...
                }
            }.apply {
                setMtp3UserPart(1, m3ua as Mtp3UserPart)
                start()
                removeAllResourses()
            }
            config.getJsonArray("stp")?.forEach { stp ->
                stp as JsonObject
                val lPort = stp.getInteger("lPort")
                val rPc = stp.getInteger("rPC")

                sccp.router.apply {
                    addMtp3ServiceAccessPoint(lPort, 1, pcApp, NETWORK_INDICATOR, 0)
                    addMtp3Destination(lPort, rPc, rPc, rPc, 0, 255, 255)
                }
                sccp.sccpResource.addRemoteSpc(lPort, rPc, 0, 0)
            }
            sccp.router.apply {
                val sccpAddressVrt = config.getString("gtVrt")?.let { SccpAddressFactory.create(it, pcApp) }
                config.getJsonArray("gtApp")
                        .map { it as String }
                        .map { SccpAddressFactory.create(it, pcApp) }
                        .forEachIndexed { i, sccpAddressApp ->
                            addRoutingAddress(i, sccpAddressApp)
                            addRule(i, RuleType.SOLITARY, LoadSharingAlgorithm.Undefined, OriginationType.ALL, sccpAddressVrt, "K", i, -1, null, 0)
                        }
            }

            // Start ASP
            config.getJsonArray("stp")?.forEach { stp ->
                stp as JsonObject
                val lPort = stp.getInteger("lPort")

                m3ua.startAsp(ASP_NAME + "_$lPort")
            }

            sccp.sccpProvider.registerSccpListener(ssnApp, this)
        }

        logger.info { "Stack configuration checked" }
    }

    override fun onMessage(message: SccpDataMessage) {
        sccpMessagesReceived.increment()

        val m = SccpMessage().apply {
            sls = message.sls
            opc = message.incomingOpc
            dpc = message.incomingDpc
            gtCdpa = message.calledPartyAddress
                    .globalTitle
                    .digits
            gtCgpa = message.callingPartyAddress
                    .globalTitle
                    .digits
            tcapPayload = message.data
        }

        vertx.eventBus().localRequest<Any>(Routes.handle, m)
    }

    override fun onNotice(message: SccpNoticeMessage) {
        // Do nothing...
    }

    override fun onCoordResponse(ssn: Int, multiplicityIndicator: Int) {
        // Do nothing...
    }

    override fun onState(dpc: Int, ssn: Int, inService: Boolean, multiplicityIndicator: Int) {
        // Do nothing...
    }

    override fun onPcState(dpc: Int, status: SignallingPointStatus, restrictedImportanceLevel: Int?, remoteSccpStatus: RemoteSccpStatus) {
        // Do nothing...
    }

    override fun onNetworkIdState(networkId: Int, networkIdState: NetworkIdState) {
        // Do nothing...
    }

    private fun send(message: SccpMessage) {
        sccpMessagesSent.increment()

        val sccpMessage = object : SccpDataMessageImpl(
                2560, ProtocolClassImpl(1, false),
                message.sls, ssnApp,
                SccpAddressFactory.create(message.gtCdpa, message.dpc),
                SccpAddressFactory.create(message.gtCgpa, 0),
                message.tcapPayload,
                null, null
        ) {}

        sccp.sccpProvider.send(sccpMessage)
    }

    override fun stop() {
        try {
            sccp.stop()
            m3ua.stop()
            sctp.stop()
        } catch (e: Exception) {
            logger.error(e) { "Socket 'stop()' failed." }
        }
    }
}