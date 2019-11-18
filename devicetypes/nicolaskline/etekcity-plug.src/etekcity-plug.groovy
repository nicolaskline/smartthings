/*
* Author: nkline
* Thanks to tylergets and projectskydroid for the jump start on the VeSync API
*
* Device Handler for VeSync API devices, also known as the Etekcity smart plug.
*/


preferences {
	section("Internal Access"){
        input "username", "text", title: "Username for VeSync", required: true
        input "password", "password", title: "Password VeSync", required: true
        input "plug_name", "text", title: "Plug Name", required: true
	}
}


metadata {
	definition (name: "Etekcity Plug", namespace: "nicolaskline", author: "Nicolas Kline") {
		capability "Actuator"
		capability "Switch"
        capability "Refresh"
        capability "Sensor"
        command "on"
        command "off"
        attribute "token", "string"
        attribute "acount_id", "string"
        attribute "device_id", "string"
        attribute "device_type", "string"
        attribute "stored_plug_name", "string"
	}

	// simulator metadata
	simulator {
	}

	// UI tile definitions
	tiles {
    multiAttributeTile(name:"rich-control", type: "lighting", width: 6, height: 4, canChangeIcon: true){
			tileAttribute ("device.switch", key: "PRIMARY_CONTROL") {
				attributeState "on", label:'${name}', action:"switch.off", icon:"st.lights.philips.hue-single", backgroundColor:"#00A0DC", nextState:"turningOff"
				attributeState "off", label:'${name}', action:"switch.on", icon:"st.lights.philips.hue-single", backgroundColor:"#ffffff", nextState:"turningOn"
				attributeState "turningOn", label:'${name}', action:"switch.off", icon:"st.lights.philips.hue-single", backgroundColor:"#00A0DC", nextState:"turningOff"
				attributeState "turningOff", label:'${name}', action:"switch.on", icon:"st.lights.philips.hue-single", backgroundColor:"#ffffff", nextState:"turningOn"
			}
		}
		standardTile("button", "device.switch", width: 2, height: 2, canChangeIcon: true) {
			state "off", label: 'Off', action: "switch.on", icon: "st.Lighting.light11", backgroundColor: "#ffffff", nextState: "on"
			state "on", label: 'On', action: "switch.off", icon: "st.Lighting.light11", backgroundColor: "#79b821", nextState: "off"
		}
        standardTile("blank", "device.image", width: 1, height: 1, canChangeIcon: false,  canChangeBackground: false, decoration: "flat") {
          state "blank", label: "", action: "", icon: "", backgroundColor: "#FFFFFF"
        }
        standardTile("refresh", "device.switch", inactiveLabel: false, decoration: "flat", height: 2, width: 6) {
			state "default", label:"", action:"refresh", icon: "st.secondary.refresh"
        }
		main "button"
		details (["rich-control","onButton","offButton","refresh"])
	}
}



def updated() {
	handler("Updated")
    log.trace("Username ${username}")
	log.trace("Plug Name ${plug_name}")
    
	initialize()
}

def installed() {
	handler("installed")
    log.trace("Username ${username}")
	log.trace("Plug Name ${plug_name}")
    
    initialize()
}

def doAction(String action) {
	log.trace "Action: ${action}"
    
	webPut("/v1/" + device.currentValue("device_type") + "/" + device.currentValue("device_id") + "/status/" + action)
}

private initialize() {
	
	// If reset conditions exist, clear everything.
    if (!device.currentValue("stored_plug_name") || device.currentValue("stored_plug_name") != plug_name) {
    	log.debug "Clearing stored data"
    	sendEvent(name: "token", value: null)
        sendEvent(name: "account_id", value: null)
        sendEvent(name: "device_id", value: null)
        sendEvent(name: "device_type", value: null)
    }

	if (!device.currentValue("token")) {
   		log.trace("Token is not set")
        //sendEvent(name: "token", value: "321");
        setDeviceToken()
	} else {
    	log.trace("Token has been set and was remembered")
        //log.trace("clearing token")
        //sendEvent(name: "token", value: null);
    }
    
    if (!device.currentValue("account_id")) {
   		log.trace("Account ID is not set")
        //sendEvent(name: "token", value: "321");
        setDeviceToken()
	} else {
    	log.trace("Account ID has been set and was remembered")
        //log.trace("clearing token")
        //sendEvent(name: "token", value: null);
    }
    
    if (!device.currentValue("device_id")) {
    	setDeviceId(plug_name)
    } else {
		log.trace "Device ID has been set as ${device.currentValue("device_id")} with type ${device.currentValue("device_type")}"
    }
    
    if (!device.currentValue("device_id")) {
    	log.error "Could not find matching device for ${plug_name}"
    }
    
	log.trace "Token: ${device.currentValue("token")}"
    log.trace "Account ID: ${device.currentValue("account_id")}"
	log.trace "Device ID: ${device.currentValue("device_id")}"
	log.trace "Device Type: ${device.currentValue("device_type")}"
}

void setDeviceToken() {
	log.trace "Getting device token"
	def hash = generateMD5(password)
	def data = jsonPost('{"account": "' + username + '","password": "' + hash + '"}')    
	sendEvent(name: "token", value: data["tk"])
    sendEvent(name: "account_id", value: data["accountID"])
}

void setDeviceId(plug) {
	log.trace "Getting device ID for ${plug}"
	def data = webGet("/vold/user/devices")
	
	data.each {
		if (it["deviceName"] == plug) {
			sendEvent(name: "device_id", value: it["cid"])
			sendEvent(name: "device_type", value: it["deviceType"])    
            sendEvent(name: "stored_plug_name", value: plug)
		}
	}
}

private webGet(path) {

	def headers = [
    	"accountId": device.currentValue("account_id"),
        "tk": device.currentValue("token")
    ]

	def params = [
	    uri: "https://smartapi.vesync.com",
	    path: path,
        headers: headers
	]
    
    log.trace "Getting with ${params}"
	
	try {
	    httpGet(params) { resp ->
	        	resp.headers.each {
	        	log.debug "Get response header: ${it.name} : ${it.value}"
	    	}
	    	log.debug "Get response data: ${resp.data}"
	    
	    	return resp.data
        }
	} catch (e) {
	    log.error "something went wrong: $e"
	}
}

private webPut(path) {

	def headers = [
    	"accountId": device.currentValue("account_id"),
        "tk": device.currentValue("token")
    ]

	def params = [
	    uri: "https://smartapi.vesync.com",
	    path: path,
        headers: headers
	]
    
    log.trace "Putting with ${params}"
	
	try {
	    httpPut(params) { resp ->
	        	resp.headers.each {
	    	}
	    
	    	return resp.data
        }
	} catch (e) {
	    log.error "something went wrong: $e"
	}
}

private jsonPost(def bodyMap) {
		
	def headers = [:]
    headers.put("Content-Type", "application/json")

	def params = [
    	uri: "https://smartapi.vesync.com/vold/user/login",
        HOST: "smartapi.vesync.com",
    	body: bodyMap,
        requestContentType: "application/json",
		contentType: "application/json"
	]
    
    log.trace("Posting with ${params}")

    try {
        httpPostJson(params) { resp ->
            resp.headers.each {
                log.debug "response header: ${it.name} : ${it.value}"
            }
            log.debug "response contentType: ${resp.contentType}"
            log.debug "response status: ${resp.getStatus()}"
            log.debug "response success: ${resp.isSuccess()}"
            log.debug "response rdata: ${resp.responseData}"
            log.debug "response data: ${resp.data}"
			log.debug "response data2: ${resp.getData()}"
                        
            return resp.responseData
        }
    } catch (e) {
        log.debug "something went wrong: $e"
    }
}

def refresh() {
	def data = webGet("/v1/device/" + device.currentValue("device_id") + "/detail")
    sendEvent(name: "switch", value: data["deviceStatus"]) 
}

def on() {
	sendEvent(name: "switch", value: "on") 
	return doAction("on")
}

def off() {
    sendEvent(name: "switch", value: "off") 
    return doAction("off")
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

import java.security.MessageDigest

def generateMD5(String s){
    return MessageDigest.getInstance("MD5").digest(s.bytes).encodeHex()
}
