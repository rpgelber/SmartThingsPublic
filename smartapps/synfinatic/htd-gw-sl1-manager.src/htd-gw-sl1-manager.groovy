/**
 *  HTD GW-SL1
 *
 *  Copyright 2018 Aaron Turner
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */
definition(
    name: "HTD GW-SL1 Service Manager",
    namespace: "synfinatic",
    author: "Aaron Turner",
    description: "HTD (W)GW-SL1 Smart Gateway\r\n\r\nAllows controlling your HTD compatible whole house audio system.\r\n\r\nhttps://www.htd.com/GW-SL1\r\nhttps://www.htd.com/WGW-SL1",
    category: "Fun & Social",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png"
)


preferences {
/*
    page(name: "searchTargetSelection", title: "HTD (W)GW-SL1 Search Target", nextPage: "deviceDiscovery") {
        section("Search Target") {
            input "searchTarget", "string", title: "Search Target", defaultValue: "urn:micasaverde-com:serviceId:HTD1", required: false
        }
    }
    page(name: "deviceDiscovery", title: "(W)GW-SL1 Setup", content: "deviceDiscovery")
*/
    section("IP Address of (W)GW-SL1") {
        input "ipAddress", "string", multiple: false, required: true
    }
    section("TCP Port of (W)GW-SL1") {
        input "tcpPort", "integer", multiple: false, required: true, defaultValue: 10006
    }
    section("SmartThings Hub") {
        input "hub", "hub", title: "Select Hub"
    }
}

def installed() {
    log.debug "Installed with settings: ${settings}"

    initialize()
}

def updated() {
    log.debug "Updated with settings: ${settings}"

    unsubscribe()
    initialize()
}

def initialize() {
    log.debug "Trying to connect to ${ipAddress}:${tcpPort}"
    def iphex = convertIPtoHex($ipAddress)
    def porthex = convertPortToHex($tcpPort)
    def dni = "${iphex}:${porthex}"
    def selectedDevice = devices.find { it.value.mac == dni }
    def d = getChildDevices()?.find {
        it.deviceNetworkId == selectedDevice.value.mac
    }
    if (!d) {
        log.debug "Adding new HTD (W)GW-SL1 with dni: ${
    d = addChildDevice("synfinatic", "HTD WG-SL1", dni, theHub, 
}

void ssdpDiscover() {
    sendHubCommand(new physicalgraph.device.HubAction("lan discovery ${searchTarget}", physicalgraph.device.Protocol.LAN))
}

void ssdpSubscribe() {
    subscribe(location, "ssdpTerm.${searchTarget}", ssdpHandler)
}

Map verifiedDevices() {
    def devices = getVerifiedDevices()
    def map = [:]
    devices.each {
        def value = it.value.name ?: "UPnP Device ${it.value.ssdpUSN.split(':')[1][-3..-1]}"
        def key = it.value.mac
        map["${key}"] = value
    }
    map
}

void verifyDevices() {
    def devices = getDevices().findAll { it?.value?.verified != true }
    devices.each {
        int port = convertHexToInt(it.value.deviceAddress)
        String ip = convertHexToIP(it.value.networkAddress)
        String host = "${ip}:${port}"
        sendHubCommand(new physicalgraph.device.HubAction("""GET ${it.value.ssdpPath} HTTP/1.1\r\nHOST: $host\r\n\r\n""", physicalgraph.device.Protocol.LAN, host, [callback: deviceDescriptionHandler]))
    }
}

def getVerifiedDevices() {
    getDevices().findAll{ it.value.verified == true }
}

def getDevices() {
    if (!state.devices) {
        state.devices = [:]
    }
    state.devices
}

def addDevices() {
    def devices = getDevices()

    selectedDevices.each { dni ->
        def selectedDevice = devices.find { it.value.mac == dni }
        def d
        if (selectedDevice) {
            d = getChildDevices()?.find {
                it.deviceNetworkId == selectedDevice.value.mac
            }
        }

        if (!d) {
            log.debug "Creating HTD (W)GW-SL1 with dni: ${selectedDevice.value.mac}"
            addChildDevice("synfinatic", "HTD (W)GW-SL1", selectedDevice.value.mac, selectedDevice?.value.hub, [
                    "label": selectedDevice?.value?.name ?: "HTD (W)GW-SL1",
                    "data": [
                        "mac": selectedDevice.value.mac,
                        "ip": selectedDevice.value.networkAddress,
                        "port": selectedDevice.value.deviceAddress
                    ]
            ])
        }
    }
}

def ssdpHandler(evt) {
    def description = evt.description
    def hub = evt?.hubId

    def parsedEvent = parseLanMessage(description)
    parsedEvent << ["hub":hub]

    def devices = getDevices()
    String ssdpUSN = parsedEvent.ssdpUSN.toString()
    if (devices."${ssdpUSN}") {
        def d = devices."${ssdpUSN}"
        if (d.networkAddress != parsedEvent.networkAddress || d.deviceAddress != parsedEvent.deviceAddress) {
            d.networkAddress = parsedEvent.networkAddress
            d.deviceAddress = parsedEvent.deviceAddress
            def child = getChildDevice(parsedEvent.mac)
            if (child) {
                child.sync(parsedEvent.networkAddress, parsedEvent.deviceAddress)
            }
        }
    } else {
        devices << ["${ssdpUSN}": parsedEvent]
    }
}

void deviceDescriptionHandler(physicalgraph.device.HubResponse hubResponse) {
    def body = hubResponse.xml
    def devices = getDevices()
    def device = devices.find { it?.key?.contains(body?.device?.UDN?.text()) }
    if (device) {
        device.value << [name: body?.device?.roomName?.text(), model:body?.device?.modelName?.text(), serialNumber:body?.device?.serialNum?.text(), verified: true]
    }
}

private Integer convertHexToInt(hex) {
    Integer.parseInt(hex,16)
}

private String convertHexToIP(hex) {
    [convertHexToInt(hex[0..1]),convertHexToInt(hex[2..3]),convertHexToInt(hex[4..5]),convertHexToInt(hex[6..7])].join(".")
}

private String convertIPtoHex(ipAddress) { 
    String hex = ipAddress.tokenize( '.' ).collect {  String.format( '%02x', it.toInteger() ) }.join()
    log.debug "IP address entered is $ipAddress and the converted hex code is $hex"
    return hex

}

private String convertPortToHex(port) {
    String hexport = port.toString().format( '%04x', port.toInteger() )
    log.debug hexport
    return hexport
}
