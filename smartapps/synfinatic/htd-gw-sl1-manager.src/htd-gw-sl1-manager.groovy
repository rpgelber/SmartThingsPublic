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
    name: "HTD (W)GW-SL1 Service Manager",
    namespace: "synfinatic",
    author: "Aaron Turner",
    description: "HTD (W)GW-SL1 Smart Gateway\r\n\r\nAllows controlling your HTD compatible whole house audio system.\r\n\r\nhttps://www.htd.com/GW-SL1\r\nhttps://www.htd.com/WGW-SL1",
    category: "Music & Sounds",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png"
)


preferences {
    section("IP Address of (W)GW-SL1") {
        input "ipAddress", "string", multiple: false, required: true
    }
    section("TCP Port of (W)GW-SL1") {
        input "tcpPort", "integer", multiple: false, required: true, defaultValue: 10006, hideable: true, hidden: true, 
    }
    section("SmartThings Hub") {
        input "theHub", "string", multiple: false, required: true, options: location.hubs.collect { it.name }
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
    def dev = addChildDevice("synfinatic", "HTD GW-SL1", dni, theHub.id, 
        [label: "${app.label}", name: "HTD GW-SL1")
    log.trace "created ${dev.displayName} with id $dni"
}

private Integer convertHexToInt(hex) {
    Integer.parseInt(hex,16)
}

private String convertHexToIP(hex) {
    [convertHexToInt(hex[0..1]),convertHexToInt(hex[2..3]),convertHexToInt(hex[4..5]),convertHexToInt(hex[6..7])].join(".")
}

private String convertIPtoHex(ipAddress) { 
    String hex = ipAddress.tokenize( '.' ).collect {  String.format( '%02x', it.toInteger() ) }.join()
    log.debug "IP address $ipAddress is converted to $hex"
    return hex

}

private String convertPortToHex(port) {
    String hexport = port.toString().format('%04x', port.toInteger())
    log.debug "Port $port is converted to $hexport"
    return hexport
}
