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
    name: "HTD GW-SL1",
    namespace: "synfinatic",
    author: "Aaron Turner",
    description: "HTD (W)GW-SL1 Smart Gateway for the MC-66/MCA-66",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")

preferences {
    section("GW-SL1 Config:") {
        input "ipAddress", "text", multiple: false, required: true, title: "IP Address:"
        input "tcpPort", "integer", multiple: false, required: false, title: "TCP Port:", defaultValue: 10006
        input "HTDtype", "enum", multiple: false, required: true, title: "HTD Controller:", options: ['MC-66', 'MCA-66']
        input "theHub", "hub", multiple: false, required: true, title: "Pair with SmartThings Hub:"
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
    def porthex = convertPortToHex(tcpPort)
	def iphex = convertIPtoHex(ipAddress)
    def dni_base = "${iphex}:${porthex}"
    if (HTDtype == 'MC-66' || HTDtype == 'MCA-66') {
    	def dni = "${dni_base}:${i}"
    	for (int i = 1; i < 7; i ++) {
		    def dev = addChildDevice("synfinatic", "HTD Zone", dni, theHub.id,
        		[label: "HTD MC-66 Zone ${i}"])
	        log.info "created ${dev.displayName} with ${dni}"
		}
    }
    // TODO add support for Lynx 6/12
}

def unsubscribe() {
	devices = getChildDevices()
    for (device in devices) {
    	log.debug "unsubscribe(): found child device: ${device.displayName} = ${device.id}"
	    // TODO: should we delete them??
    	// deleteChildDevice(device.id)
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
    log.debug "IP address ${ipAddress} is converted to ${hex}"
    return hex

}

private String convertPortToHex(port) {
    String hexport = port.toString().format('%04x', port.toInteger())
    log.debug "Port ${port} is converted to $hexport"
    return hexport
}

// generates the actual 6 byte command as a string
private String command(child_id, cmd, id) {
	def command_base = 0x0200
	return sprintf("%04x%02x%04x%02x", command_base, child_id, cmd, id)
}

// input channel command
private String _set_input_channel(child_id) {
	def cmd = 0x0403
	def id = 0x0A + child_id - 1
    return command(child_id, cmd, id)
}

private String _volume_up(child_id) {
	def cmd = 0x0409
    def id = 0x10 + child_id - 1
    return command(child_id, cmd, id)    
}

private String _volume_down(child_id) {
	def cmd = 0x040A
    def id = 0x11 + child_id - 1
    return command(child_id, cmd, id)
}

private String _power_on(child_id) {
	def cmd = 0x0420
    def id = 0x27 + child_id - 1
    return command(child_id, cmd, id)
}

private String _power_off(child_id) {
	def cmd = 0x0421
    def id = 0x28 + child_id - 1
    return command(child_id, cmd, id)
}

private String _toggle_mute(child_id) {
	def cmd = 0x0422
    def id = 0x29 + child_id - 1
    return command(child_id, cmd, id)
}

private String _bass_up(child_id) {
	def cmd = 0x0426
    def id = 0x2d + child_id - 1
    return command(child_id, cmd, id)
}

private String _bass_down(child_id) {
	def cmd = 0x0427
    def id = 0x2e + child_id - 1
    return command(child_id, cmd, id)
}

private String _trebel_up(child_id) {
	def cmd = 0x0428
    def id = 0x2f + child_id - 1
    return command(child_id, cmd, id)
}

private String _trebel_down(child_id) {
	def cmd = 0x0429
    def id = 0x30 + child_id - 1
    return command(child_id, cmd, id)
}

private String _balance_right(child_id) {
	def cmd = 0x042a
    def id = 0x31 + child_id - 1
    return command(child_id, cmd, id)
}

private String _balance_left(child_id) {
	def cmd = 0x042b
    def id = 0x32 + child_id - 1
    return command(child_id, cmd, id)
}

private String _query_state(child_id) {
	def cmd = 0x0600
    def id = 0x09 + child_id - 1
    return command(child_id, cmd, id)
}

private boolean send_command(child, command) {
	def values = child.id.split(':')
    def ipAddr = convertHexToIP(values[0])
    def portAddr = convertHexToInt(values[1])
	s = new Socket(ipAddr, portAddr)
    s.withStreams { input, output ->
	  output << command
	  log.debug "sent command to ${child.id}"
	}
    s.close()
}


// handle commands
def setMute(child, value) {
	log.debug "Executing 'setMute'"
	// TODO: handle 'setMute' command
}

def mute(child) {
	log.debug "Executing 'mute'"
	// TODO: handle 'mute' command
}

def unmute(child) {
	log.debug "Executing 'unmute'"
	// TODO: handle 'unmute' command
}

def setVolume(child, value) {
	log.debug "Executing 'setVolume'"
	// TODO: handle 'setVolume' command
}

def volumeUp(child) {
	log.debug "Executing 'volumeUp'"
	// TODO: handle 'volumeUp' command
}

def volumeDown(child) {
	log.debug "Executing 'volumeDown'"
	// TODO: handle 'volumeDown' command
}

def setInputSource(child, source) {
	log.debug "Executing 'setInputSource'"
	// TODO: handle 'setInputSource' command
}

def on(child) {
	log.debug "Executing 'on'"
	// TODO: handle 'on' command
}

def off(child) {
	log.debug "Executing 'off'"
	// TODO: handle 'off' command
}

def setTrebel(child, action) {

}

def setBass(child, action) {

}

def setBalance(child, action) {

}