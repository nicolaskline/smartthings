/*
* Author: nkline
* Thanks to tguerena and surge919 for Smartthings jump start
* Huge thanks to greencoder for the X10 api information
*
* Device Handler
*/


preferences {
	section("Internal Access"){
		input "ip", "text", title: "Internal IP of hub", required: true
		input "port", "text", title: "Internal Port (if not 80)", required: true
		input "house_code", "text", title: "X10 House Code", required: true
        input "unit_code", "text", title: "X10 Unit Code", required: true
        input "username", "text", title: "Username for hub", required: true
        input "password", "password", title: "Password for hub", required: true
	}
}




metadata {
	definition (name: "X10 Switch", namespace: "SmartThingsPublic", author: "Nicolas Kline") {
		capability "Actuator"
		capability "Switch"
		capability "Sensor"
        capability "Switch Level"
        command "dim"
        command "brighten"
        command "doX10"
        command "on"
        command "off"
	}

	// simulator metadata
	simulator {
	}

	// UI tile definitions
	tiles {
		standardTile("button", "device.switch", width: 2, height: 2, canChangeIcon: true) {
			state "off", label: 'Off', action: "switch.on", icon: "st.Lighting.light11", backgroundColor: "#ffffff", nextState: "on"
			state "on", label: 'On', action: "switch.off", icon: "st.Lighting.light11", backgroundColor: "#79b821", nextState: "off"
		}
		standardTile("offButton", "device.button", width: 1, height: 1, canChangeIcon: true) {
			state "default", label: 'Force Off', action: "switch.off", icon: "st.switches.switch.off", backgroundColor: "#ffffff"
		}
		standardTile("onButton", "device.button", width: 1, height: 1, canChangeIcon: true) {
			state "default", label: 'Force On', action: "switch.on", icon: "st.switches.switch.on", backgroundColor: "#79b821"
		}
        standardTile("dimButton", "device.button", width: 1, height: 1, canChangeIcon: true) {
			state "default", label: '-', action: "dim", icon: "st.Lighting.light13", backgroundColor: "#cccccc"
		}
		standardTile("brightenButton", "device.button", width: 1, height: 1, canChangeIcon: true) {
			state "default", label: '+', action: "brighten", icon: "st.Lighting.light11", backgroundColor: "#ffffff"
		}
		main "button"
		details (["button","onButton","offButton", "brightenButton", "dimButton"])
	}
}

def parse(String description) {
	log.trace("Parse ${description}")
	def map = stringToMap(description)
	log.debug map
	def result = []

	if (map.bucket && map.key) {
		putImageInS3(map)
	} else if (map.headers && map.body) {
		if (map.body) {
			def body = new String(map.body.decodeBase64())
            log.trace "Parsing Body: '${body}'"
			if(body.find("infraredstat=\"auto\"")) {
				log.info("Polled: LED Status Auto")
				sendEvent(name: "ledStatus", value: "auto")
			} else if(body.find("infraredstat=\"open\"")) {
				log.info("Polled: LED Status Open")
				sendEvent(name: "ledStatus", value: "on")
			} else if(body.find("infraredstat=\"close\"")) {
				log.info("Polled: LED Status Close")
				sendEvent(name: "ledStatus", value: "off")
			}
		}
        
        if (map.headers) {
        	def headers = new String(map.headers.decodeBase64())
            log.trace "Parsing Headers: '${headers}'"
        }
	}
	result
}

def dim() {
    /*
    On	280
    Off	380
    Bright	580
    Dim	480
    All On	180
    All Off	680
    */  
    
    log.debug("Dim ${house_code}${unit_code}")
    def dim = '480'
	doX10(dim)
}

def setLevel(value) {
	log.debug "setLevel to: ${value}"
	def valueaux = (value / 10) as Integer
	def level = Math.max(Math.min(valueaux, 9), 0)
	
    /*
    if (level > 0) {
		on()
	} else {
		off()
	}
    */
	
    // If device is off, dim up
    off()
    
    def dimUp = {
       brighten()
    }
    1.upto(level, dimUp)
    // If device is on, dim down
    
}

def brighten() {
    log.debug("Brighten ${house_code}${unit_code}")
    def brighten = '580'
    doX10(brighten)
}

def on() {
	sendEvent(name: "switch", value: "on") 
	log.debug("Switch ${house_code}${unit_code} ON")
    def on = '280'
	return doX10(on)
}


def off() {
    sendEvent(name: "switch", value: "off") 
	log.debug("Switch ${house_code}${unit_code} OFF")
    def off = '380'
    return doX10(off)
}

def doX10(def command) {
	def x10 = '0263'
	def house = getHouseCode(house_code)
    def unit = getUnitCode(unit_code)
    
    def hubActions = [hubGet("/3?${x10}${house}${unit}=I=3", true), delayAction(200), hubGet("/3?${x10}${house}${command}=I=3", true)]
  	
    hubActions
}

def getHouseCode(def house) {
  def houseMap = [:]
    houseMap["A"] = "6"
    houseMap["B"] = "E"
    houseMap["C"] = "2"
    houseMap["D"] = "A"
    houseMap["E"] = "1"
    houseMap["F"] = "9"
    houseMap["G"] = "5"
    houseMap["H"] = "D"
    houseMap["I"] = "7"
    houseMap["J"] = "F"
    houseMap["K"] = "3"
    houseMap["L"] = "B"
    houseMap["M"] = "0"
    houseMap["N"] = "8"
    houseMap["O"] = "4"
    houseMap["P"] = "C"
    
    return houseMap[house]
}

def getUnitCode(def unit) {
  def unitMap = [:]
    unitMap["1"] = "600"
    unitMap["2"] = "E00"
    unitMap["3"] = "200"
    unitMap["4"] = "A00"
    unitMap["5"] = "100"
    unitMap["6"] = "900"
    unitMap["7"] = "500"
    unitMap["8"] = "D00"
    unitMap["9"] = "700"
    unitMap["10"] = "F00"
    unitMap["11"] = "300"
    unitMap["12"] = "B00"
    unitMap["13"] = "000"
    unitMap["14"] = "800"
    unitMap["15"] = "400"
    unitMap["16"] = "C00"
    
    return unitMap[unit]
}

private hubGet(def apiCommand, def useS3) {
	//Setting Network Device Id
	//def iphex = convertIPtoHex(ip)
	//def porthex = convertPortToHex(port)
	//device.deviceNetworkId = "$iphex:$porthex:$house_code$unit_code"
	//log.debug "Device Network Id set to ${iphex}:${porthex}:${house_code}${unit_code}"

	// Create headers
	def headers = [:]
	def hostAddress = "${ip}:${port}"
	headers.put("Host", hostAddress)
	def authorizationClear = "${username}:${password}"
	def authorizationEncoded = "Basic " + authorizationClear.encodeAsBase64().toString()
	headers.put("Authorization", authorizationEncoded)

	log.trace("Getting ${apiCommand}")
    //log.trace "with query: ${queryParams}"
    log.trace "with Headers: ${headers}"
	def hubAction = new physicalgraph.device.HubAction(
		method: "GET",
		path: apiCommand,
        headers: headers)
	if(useS3) {
		//log.debug "Outputting to S3"
		hubAction.options = [outputMsgToS3:true]
	} else {
		//log.debug "Outputting to local"
		hubAction.options = [outputMsgToS3:false]
	}

	hubAction
}

private String convertIPtoHex(ipAddress) { 
	String hex = ipAddress.tokenize( '.' ).collect {  String.format( '%02x', it.toInteger() ) }.join()
	return hex
}

private String convertPortToHex(port) {
	String hexport = port.toString().format( '%04x', port.toInteger() )
	return hexport
}

private delayAction(long time) {
	new physicalgraph.device.HubAction("delay $time")
}
