/**
 *  HTD Audio using the HTD (W)GW-SL1
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
        name: "HTD Audio",
        namespace: "synfinatic",
        author: "Aaron Turner",
        description: "HTD MC-66/MCA-66 via (W)GW-SL1 Smart Gateway",
        category: "My Apps",
        iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
        iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
        iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")

preferences {
    page(name: "config", title: "(W)GW-SL1 Config", uninstall: true, nextPage: "active") {
        section() {
            input "ipAddress", "text", multiple: false, required: true, title: "IP Address:", defaultValue: "172.16.1.133"
            input "tcpPort", "integer", multiple: false, required: true, title: "TCP Port:", defaultValue: 10006
            input "HTDtype", "enum", multiple: false, required: true, title: "HTD Controller:", options: ['MC-66', 'MCA-66']
            input "theHub", "hub", multiple: false, required: true, title: "Pair with SmartThings Hub:"
        }
    }
    page(name: "active", title: "Select Active Zones and Sources", nextPage: "naming") {
        def zone_count = 6
        def source_count = 6
        section("Which zones are available?") {
            for (int i = 1; i <= zone_count; i++ ) {
                input "zone${i}_active", "bool", title: "Zone ${i} Active:", defaultValue: true
            }
        }
        section("Which input sources are available?") {
            for (int i = 1; i <= zone_count; i++ ) {
                input "source{i}_active", "bool", title: "Source ${i} Active:", defaultValue: true
            }
        }
    }
    page(name: "naming", title: "Name Zones and Sources", install: true) {
        def zone_count = 6
        def source_count = 6
        section("Name your zones:") {
            for (int i = 1; i <= zone_count; i++) {
                def zone_active_name = this."zone${i}_active"
                if (zone_active_name) {
                    input "zone${i}", "text", multiple: false, required: false, title: "Zone ${i}:", defaultValue: "Zone ${i}"
                }
            }
        }
        section("Name your input sources:") {
            for (int i = 1; i <= source_count; i++) {
                def source_active_name = this."source${i}_active"
                if (source_active_name) {
                    input "source${i}", "text", multiple: false, required: false, title: "Source ${i}:", defaultValue: "Source ${i}"
                }
            }
        }
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
    // nothing is muted by default
    state.zone_mute = [1: false, 2: false, 3: false, 4: false, 5: false, 6: false]
    // all zones default to source = 1.  Hopefully the user enabled it :)
    state.zone_source = [1: 1, 2: 2, 3: 1, 4: 1, 5: 1, 6: 1]
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


// get the zone_id for a child device
private int get_zone_id(child) {
    def values = child.id.split(':')
    return values[2]
}

/*
 * sends a command.to the gateway
 * if read_reply > 0, then read that many bytes from the
 * controller and return them as a byte[]
 */
private byte[] send_command(child, command, read_reply=0) {
    def values = child.id.split(':')
    def ipAddr = convertHexToIP(values[0])
    def portAddr = convertHexToInt(values[1])
    s = new Socket(ipAddr, portAddr)
    infh = socket.getInputStream()
    outfh = socket.getOutputStream()
    outfh.write(command)
    def reply = new byte[read_reply]
    for (int i = 0; i < read_reply; i ++) {
        reply[i] = infh.read()
    }
    infh.close()
    outfh.close()
    s.close()
    return reply
}

/*
 * handle commands from the device
 */
def setMute(child, value) {
    zone_id = get_zone_id(child.device.id)
    log.debug "Executing 'setMute' for zone ${zone_id}"
    if (value != state.zone_mute.get(zone_id)) {
        send_command(child, _toggle_mute(zone_id))
    }
}

def mute(child) {
    zone_id = get_zone_id(child.device.id)
    log.debug "Executing 'mute' for zone ${zone_id}"
    if (! state.zone_mute.get(zone_id)) {
        send_command(child, _toggle_mute(zone_id))
    }
}

def unmute(child) {
    zone_id = get_zone_id(child.device.id)
    log.debug "Executing 'unmute' for zone ${zone_id}"
    if (state.zone_mute.get(zone_id)) {
        send_command(child, _toggle_mute(zone_id))
    }
}

def setVolume(child, value) {
    zone_id = get_zone_id(child.device.id)
    log.debug "setVolume' does nothing at this time..."
    // we need some way of figuring out what the current volume is (0-60) and then
    // calling volumeUp/Down as appropriate to get to the new value. Maybe is returned
    // by queryZoneState?
}

def volumeUp(child) {
    zone_id = get_zone_id(child.device.id)
    log.debug "Executing 'volumeUp' for zone ${zone_id}"
    send_command(child, _volume_up(zone_id))
}

def volumeDown(child) {
    zone_id = get_zone_id(child.device.id)
    log.debug "Executing 'volumeDown' for zone ${zone_id}"
    send_command(child, _volume_down(zone_id))
}

def setInputSource(child, source_id) {
    zone_id = get_zone_id(child.device.id)
    log.debug "Executing 'setInputSource' for zone ${zone_id} => ${source_id}"
    send_command(child, _set_input_channel(zone_id, source_id))
}

def on(child) {
    zone_id = get_zone_id(child.device.id)
    log.debug "Executing 'on' for zone ${zone_id}"
    send_command(child, _power_on(zone_id))
}

def off(child) {
    zone_id = get_zone_id(child.device.id)
    log.debug "Executing 'off' for zone ${zone_id}"
    send_command(child, _power_off(zone_id))
}

def setTrebel(child, action) {
    zone_id = get_zone_id(child.device.id)
    if (action == 'up') {
        log.debug "Executing trebel up for zone ${zone_id}"
        send_command(child, _trebel_up(zone_id))
    } else {
        log.debug "Executing trebel down for zone ${zone_id}"
        send_command(child, _trebel_down(zone_id))
    }
}

def setBass(child, action) {
    zone_id = get_zone_id(child.device.id)
    if (action == 'up') {
        log.debug "Executing bass up for zone ${zone_id}"
        send_command(child, _bass_up(zone_id))
    } else {
        log.debug "Executing bass down for zone ${zone_id}"
        send_command(child, _bass_down(zone_id))
    }
}

def setBalance(child, action) {
    zone_id = get_zone_id(child.device.id)
    if (action == 'left') {
        log.debug "Executing balance left for zone ${zone_id}"
        send_command(child, _balance_left(zone_id))
    } else {
        log.debug "Executing balance right for zone ${zone_id}"
        send_command(child, _balance_rigth(zone_id))
    }
}

def partyMode(child) {
    zone_id = get_zone_id(child.device.id)
    log.debug "Executing party mode for zoe ${zone_id}"
    send_command(child, _party_mode(zone_id))
}

def queryZoneState(child) {
    zone_id = get_zone_id(child.device.id)
    log.debug "Executing query zone state for zoe ${zone_id}"
    // the returns some bytes... what should we do with them?
    ret = send_command(child, _query_zone_state(zone_id))
}

/*
 * Helper methods to generate the actual 6 byte message sent on the wire
 */

// generates the actual 6 byte command as a string.  x is _almost_ always 0x04
private byte[] command(zone_id, x, y) {
    def cmd = [0x02, 0x0, zone_id, x, y, 0] as byte[]
    // last byte is a "checksum" which is just all the bytes added up
    cmd[5] = cmd.sum()
    return cmd
}

// input channel source (channel)
private byte[] _set_input_channel(zone_id, source_id) {
    def cmd = 0x03 + source_id - 1
    state.zone_source.put(zone_id, source_id)
    return command(zone_id, 0x04, cmd)
}

private byte[] _volume_up(zone_id) {
    return command(zone_id, 0x04, 0x09)
}

private byte[] _volume_down(zone_id) {
    return command(zone_id, 0x04, 0x10)
}

private byte[] _all_power_on() {
    return command(0x01, 0x04, 0x38)
}

private byte[] _all_power_off() {
    return command(0x01, 0x04, 0x39)
}

private byte[] _power_on(zone_id) {
    return command(zone_id, 0x04, 0x20)
}

private byte[] _power_off(zone_id) {
    return command(zone_id, 0x04, 0x21)
}

private byte[] _toggle_mute(zone_id) {
    state.zone_mute.put(zone_id, ! state.zone_mute.get(zone_id))
    return command(zone_id, 0x04, 0x22)
}

private byte[] _bass_up(zone_id) {
    return command(zone_id, 0x04, 0x26)
}

private byte[] _bass_down(zone_id) {
    return command(zone_id, 0x04, 0x27)
}

private byte[] _trebel_up(zone_id) {
    return command(zone_id, 0x04, 0x28)
}

private byte[] _trebel_down(zone_id) {
    return command(zone_id, 0x04, 0x29)
}

private byte[] _balance_right(zone_id) {
    return command(zone_id, 0x04, 0x31)
}

private byte[] _balance_left(zone_id) {
    return command(zone_id, 0x04, 0x32)
}

/*
 * I assume I should read something when this is sent?
 * trying to get a secret decoder ring for the reply from HTD
 */
private byte[] _query_zone_state(zone_id) {
    return command(zone_id, 0x06, 0x00)
}

/*
 * Party mode!  Retrieve the current source for our given
 * zone
 */
private byte[] _party_mode(zone_id) {
    def source_id = state.zone_source.get(zone_id)
    return command(zone_id, 0x04, 0x39 + zone_id + source_id)
}
