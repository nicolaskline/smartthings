/**
* Wansview IP pan tilt camera device type
*
* This implementation supports live streaming, snapshots, pan/tilt, and IR LED control.
*
* Known supported cameras:
*     	Wansview
* and probably several others.
*
* To test if your camera is supported, open a web browser and go to:
* 		http://(camera ip address):(camera port)/cgi-bin/hi3510/param.cgi?cmd=getinfrared
* Type in your camera's username/password. The response will be similar to:
*		var infraredstat= ...
*
* Installation:
* 1) Go to "My Device Types" in your ST developer account (https://graph.api.smartthings.com/ide/devices)
* 2) Click "New Device Type"
* 3) Click "From Code"
* 4) Paste this entire file into the space provided and click "Create"
* 5) CLick "Publish" then "For Me"
* 6) Click "My Devices"
* 7) Click "New Device"
* 8) Fill in Name, Device Network Id
* 9) For "Type", choose "HI3510 Camera Device" (near bottom)
* 10) For "Version", "choose "Published"
* 11) Click "Create"
* 12) Go into the ST app
* 13) Touch the "Marketplace" tab (furthest right)
* 14) Touch "Not Yet Configured" and choose your new device
* 15) Fill in the required information and touch "Next"
* 16) Optionally set up a SmartApp and touch "Done"
*
* Copyright 2016 Nic Kline
*
* This implementation is based on the Foscam Universal Device by skp19 and Foscam device by uncleskippy:
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

metadata {
	definition (name: "Wansview IP Camera", namespace: "smartthings", author: "nicolaskline") {
			capability "Polling"
			capability "Image Capture"
			capability "Video Camera"
			capability "Video Capture"
		
			attribute "ledStatus", "string"
            attribute "alarmStatus", "string"

			command "start"
			command "moveLeft"
			command "moveRight"
			command "moveUp"
			command "moveDown"            

			command "moveToPreset1"
			command "moveToPreset2"
			command "moveToPreset3"
			command "moveToPreset4"

			command "ledOn"
			command "ledOff"
			command "ledAuto"      
            
            command "alarmOn"
			command "alarmOff"
			command "toggleAlarm"
	}

	preferences {
		input("dns", "string", title:"Camera DNS", description: "External DNS or IP Address", required: true, displayDuringSetup: true)
		input("ip", "string", title:"Camera IP Address", description: "Internal IP Address", required: true, displayDuringSetup: true)
		input("port", "string", title:"Camera Port", description: "Camera Port", defaultValue: 80 , required: true, displayDuringSetup: true)
		input("username", "string", title:"Camera Username", description: "Camera Username", required: true, displayDuringSetup: true)
		input("password", "password", title:"Camera Password", description: "Camera Password", required: true, displayDuringSetup: true)
		input("mirror", "bool", title:"Mirror?", description: "Camera Mirrored?")
		input("flip", "bool", title:"Flip?", description: "Camera Flipped?")
	}

	tiles(scale: 2) {
    	multiAttributeTile(name: "videoPlayer", type: "videoPlayer", width: 6, height: 4) {
			tileAttribute("device.switch", key: "CAMERA_STATUS") {
				attributeState("on", label: "Active", icon: "st.camera.dlink-indoor", action: "switch.off", backgroundColor: "#79b821", defaultState: true)
				attributeState("off", label: "Inactive", icon: "st.camera.dlink-indoor", action: "switch.on", backgroundColor: "#ffffff")
				attributeState("restarting", label: "Connecting", icon: "st.camera.dlink-indoor", backgroundColor: "#53a7c0")
				attributeState("unavailable", label: "Unavailable", icon: "st.camera.dlink-indoor", action: "refresh.refresh", backgroundColor: "#F22000")
			}

			tileAttribute("device.errorMessage", key: "CAMERA_ERROR_MESSAGE") {
				attributeState("errorMessage", label: "", value: "", defaultState: true)
			}

			tileAttribute("device.camera", key: "PRIMARY_CONTROL") {
				attributeState("on", label: "Active", icon: "st.camera.dlink-indoor", backgroundColor: "#79b821", defaultState: true)
				attributeState("off", label: "Inactive", icon: "st.camera.dlink-indoor", backgroundColor: "#ffffff")
				attributeState("restarting", label: "Connecting", icon: "st.camera.dlink-indoor", backgroundColor: "#53a7c0")
				attributeState("unavailable", label: "Unavailable", icon: "st.camera.dlink-indoor", backgroundColor: "#F22000")
			}

			tileAttribute("device.startLive", key: "START_LIVE") {
				attributeState("live", action: "start", defaultState: true)
			}

			tileAttribute("device.stream", key: "STREAM_URL") {
				attributeState("activeURL", defaultState: true)
			}
            /*
			tileAttribute("device.profile", key: "STREAM_QUALITY") {
				attributeState("1", label: "720p", action: "setProfileHD", defaultState: true)
				attributeState("2", label: "h360p", action: "setProfileSDH", defaultState: true)
				attributeState("3", label: "l360p", action: "setProfileSDL", defaultState: true)
			}	*/		
		}
        
		standardTile("camera", "device.image", width: 1, height: 1, canChangeIcon: false, inactiveLabel: true, canChangeBackground: false) {
			state "default", label: "", action: "", icon: "st.camera.dropcam-centered", backgroundColor: "#FFFFFF"
		}

		carouselTile("cameraDetails", "device.image", width: 6, height: 4) { }

		standardTile("take", "device.image", width: 2, height: 2, canChangeIcon: false, inactiveLabel: true, canChangeBackground: false) {
			state "take", label: "Take", action: "Image Capture.take", icon: "st.camera.camera", backgroundColor: "#FFFFFF", nextState:"taking"
			state "taking", label:'Taking', action: "", icon: "st.camera.take-photo", backgroundColor: "#53a7c0"
			state "image", label: "Take", action: "Image Capture.take", icon: "st.camera.camera", backgroundColor: "#FFFFFF", nextState:"taking"
		}

		valueTile("left", "device.image", width: 1, height: 2, decoration: "flat") {
			state "blank", label: "<", action: "moveLeft", icon: ""
		}
		valueTile("right", "device.image", width: 1, height: 2, decoration: "flat") {
			state "blank", label: ">", action: "moveRight", icon: ""
		}

		standardTile("up", "device.button", width: 2, height: 1, canChangeIcon: false, canChangeBackground: false, decoration: "flat") {
			state "up", label: "", action: "moveUp", icon: "st.thermostat.thermostat-up"
		}

		standardTile("down", "device.button", width: 2, height: 1, canChangeIcon: false, canChangeBackground: false, decoration: "flat") {
			state "down", label: "", action: "moveDown", icon: "st.thermostat.thermostat-down"
		}

		standardTile("alarmToggle", "device.ledStatus", width: 1, height: 1, canChangeIcon: false, inactiveLabel: true, canChangeBackground: false) {
			state "auto", label: "alarm", action: "toggleAlarm", icon: "st.Lighting.light11", backgroundColor: "#53A7C0"
			state "off", label: "alarm", action: "toggleAlarm", icon: "st.Lighting.light13", backgroundColor: "#FFFFFF"
			state "on", label: "alarm", action: "toggleAlarm", icon: "st.Lighting.light13", backgroundColor: "#FFFF00"
		}
        
        standardTile("alarmStatus", "device.alarmStatus", width: 1, height: 1, canChangeIcon: false, inactiveLabel: true, canChangeBackground: false) {
          state "off", label: "off", action: "toggleAlarm", icon: "st.Weather.weather14", backgroundColor: "#FFFFFF"
          state "on", label: "on", action: "toggleAlarm", icon: "st.Weather.weather14",  backgroundColor: "#FF0000"
        }

		standardTile("ledOn", "device.ledStatus", width: 1, height: 1, canChangeIcon: false, inactiveLabel: true, canChangeBackground: false) {
			state "auto", label: "on", action: "ledOn", icon: "st.Lighting.light11", backgroundColor: "#FFFFFF"
			state "off", label: "on", action: "ledOn", icon: "st.Lighting.light11", backgroundColor: "#FFFFFF"
			state "on", label: "on", action: "ledOn", icon: "st.Lighting.light11", backgroundColor: "#FFFF00"
		}

		standardTile("ledOff", "device.ledStatus", width: 1, height: 1, canChangeIcon: false, inactiveLabel: true, canChangeBackground: false) {
			state "auto", label: "off", action: "ledOff", icon: "st.Lighting.light13", backgroundColor: "#FFFFFF"
			state "off", label: "off", action: "ledOff", icon: "st.Lighting.light13", backgroundColor: "#53A7C0"
			state "on", label: "off", action: "ledOff", icon: "st.Lighting.light13", backgroundColor: "#FFFFFF"
		}

		standardTile("blank11", "device.image", width: 1, height: 1) {
			state "blank", label: " ", backgroundColor: "#FFFFFF"
		}
        
        standardTile("blank21", "device.image", width: 2, height: 1) {
			state "blank", label: " ", backgroundColor: "#FFFFFF"
		}
        
        standardTile("blank12", "device.image", width: 1, height: 2) {
			state "blank", label: " ", backgroundColor: "#FFFFFF"
		}
        
		valueTile("ledLabel", "device.image", width: 1, height: 1, decoration: "flat") {
			state "blank", label: "LED\nMode", backgroundColor: "#FFFFFF"
		}
        
		valueTile("preset1", "device.image", width: 1, height: 1, decoration: "flat") {
			state "blank", label: "move\n1", action: "moveToPreset1", backgroundColor: "#53a7c0"
		}
        
		valueTile("preset2", "device.image", width: 1, height: 1, decoration: "flat") {
			state "blank", label: "move\n2", action: "moveToPreset2", backgroundColor: "#53a7c0"
		}
        
		valueTile("preset3", "device.image", width: 1, height: 1, decoration: "flat") {
			state "blank", label: "move\n3", action: "moveToPreset3", backgroundColor: "#53a7c0"
		}
        
		valueTile("preset4", "device.image", width: 1, height: 1, decoration: "flat") {
			state "blank", label: "move\n4", action: "moveToPreset4", backgroundColor: "#53a7c0"
		}

		main "camera", "alarmStatus"
		details(["videoPlayer", 
        		"blank21", "up", "blank11", "alarmStatus",
				"blank12", "left", "take", "right", "blank12",
				"blank21", "down", "blank21",
                "cameraDetails"])
	}
}

mappings {
   path("/getInHomeURL") {
       action:
       [GET: "getInHomeURL"]
   }
}

def toggleAlarm() {
	log.debug "Toggling Alarm"
	if(device.currentValue("alarmStatus") == "on") {
    	alarmOff()
  	}
	else {
    	alarmOn()
	}
}

def alarmOn() {
	log.debug "Enabling Alarm"
    sendEvent(name: "alarmStatus", value: "on");
    hubGet("/set_alarm.cgi?alarm_enabled=0&ioin_level=1&motion_armed=1&sounddetect_armed=0&input_armed=0&motion_sensitivity=5&sounddetect_sensitivity=5&iolinkage=0&mail=0&upload_interval=1&schedule_enable=0&schedule_sun_0=0&schedule_sun_1=0&schedule_sun_2=0&schedule_mon_0=0&schedule_mon_1=0&schedule_mon_2=0&schedule_tue_0=0&schedule_tue_1=0&schedule_tue_2=0&schedule_wed_0=0&schedule_wed_1=0&schedule_wed_2=0&schedule_thu_0=0&schedule_thu_1=0&schedule_thu_2=0&schedule_fri_0=0&schedule_fri_1=0&schedule_fri_2=0&schedule_sat_0=0&schedule_sat_1=0&schedule_sat_2=0", true);
}

def alarmOff() {
	log.debug "Disabling Alarm"
    sendEvent(name: "alarmStatus", value: "off");
    hubGet("/set_alarm.cgi?alarm_enabled=0&ioin_level=1&motion_armed=0&sounddetect_armed=0&input_armed=0&motion_sensitivity=5&sounddetect_sensitivity=5&iolinkage=0&mail=0&upload_interval=1&schedule_enable=0&schedule_sun_0=0&schedule_sun_1=0&schedule_sun_2=0&schedule_mon_0=0&schedule_mon_1=0&schedule_mon_2=0&schedule_tue_0=0&schedule_tue_1=0&schedule_tue_2=0&schedule_wed_0=0&schedule_wed_1=0&schedule_wed_2=0&schedule_thu_0=0&schedule_thu_1=0&schedule_thu_2=0&schedule_fri_0=0&schedule_fri_1=0&schedule_fri_2=0&schedule_sat_0=0&schedule_sat_1=0&schedule_sat_2=0", false);
}

//TAKE PICTURE
def take() {
	hubGet("/snapshot.cgi?user=${username}&pwd=${password}", true)
}

def ledOn() {
	log.debug("LED changed to: on")
	sendEvent(name: "ledStatus", value: "on");
	//hubGet("/cgi-bin/hi3510/param.cgi?cmd=setinfrared&-infraredstat=open", false)
}

def ledOff() {
	log.debug("LED changed to: off")
	sendEvent(name: "ledStatus", value: "off");
	//hubGet("/cgi-bin/hi3510/param.cgi?cmd=setinfrared&-infraredstat=close", false)
}

def ledAuto() {
	log.debug("LED changed to: auto")
	sendEvent(name: "ledStatus", value: "auto");
	//hubGet("/cgi-bin/hi3510/param.cgi?cmd=setinfrared&-infraredstat=auto", false)
}

def moveToPreset1() {
	log.debug("preset1")
   	hubGet("/cgi-bin/hi3510/preset.cgi?-act=goto&-number=0", false);
}

def moveToPreset2() {
	log.debug("preset2")
	hubGet("/cgi-bin/hi3510/preset.cgi?-act=goto&-number=1", false);
}

def moveToPreset3() {
	log.debug("preset3")
	hubGet("/cgi-bin/hi3510/preset.cgi?-act=goto&-number=2", false);
}

def moveToPreset4() {
	log.debug("preset4")
	hubGet("/cgi-bin/hi3510/preset.cgi?-act=goto&-number=3", false);
}

//PTZ CONTROLS
def move(command) {
	log.debug("Move Cmd: ${command}")
    webGet("/decoder_control.cgi", [command:command, onestep: '0'], false); //?command=${command}&onestep=0", false);
}

def moveLeft() {
	if(mirror == "true") {
		move(6)
	} else {
    	move(4)
    }
}

def moveRight() {
	if(mirror == "true") {
		move(4)
	} else {
    	move(6)
    }
}

def stopMoving() {
	hubGet("/decoder_control.cgi?command=1", false);
}

def moveUp() {
	if(flip == "true") {
		move(2)
	} else {
    	move(0)
    }
}

def moveDown() {
	if(flip == "true") {
		move(0)
	} else {
    	move(2)
    }
}

def poll() {
	log.trace("poll");
    //runIn(1, stopMoving);
	//hubGet("/cgi-bin/hi3510/param.cgi?cmd=getinfrared", false);
}

private webGet(def apiCommand, def queryParams, def useS3) {
	//Setting Network Device Id
	//def iphex = convertIPtoHex(ip)
	//def porthex = convertPortToHex(port)
	//device.deviceNetworkId = "$iphex:$porthex"
	//log.debug "Device Network Id set to ${iphex}:${porthex}"

	// Create headers
	def headers = [:]
	def hostAddress = "${dns}:${port}"
	headers.put("Host", hostAddress)
	def authorizationClear = "${username}:${password}"
	def authorizationEncoded = "Basic " + authorizationClear.encodeAsBase64().toString()
	headers.put("Authorization", authorizationEncoded)

	log.trace("Getting http://${dns}:${port}${apiCommand}")
    log.trace "with Headers: '${headers}'"
	def params = [
		uri: "http://${dns}:${port}",
		path: apiCommand,
		headers: headers,
        query: queryParams
        ]
        
        //?command=${command}&onestep=0
    
    	log.debug "params: " + params;
    
	try {
    httpGet(params) { resp ->
            resp.headers.each {
               log.debug "${it.name} : ${it.value}"
            }
            log.debug "response contentType: ${resp.contentType}"
            log.debug "response data: ${resp.data}"
        }
    } catch (e) {
        log.error "something went wrong: $e"
    }
}

private hubGet(def apiCommand, def useS3) {
	//Setting Network Device Id
	def iphex = convertIPtoHex(ip)
	def porthex = convertPortToHex(port)
	device.deviceNetworkId = "$iphex:$porthex"
	//log.debug "Device Network Id set to ${iphex}:${porthex}"

	// Create headers
	def headers = [:]
	def hostAddress = "${ip}:${port}"
	headers.put("Host", hostAddress)
	def authorizationClear = "${username}:${password}"
	def authorizationEncoded = "Basic " + authorizationClear.encodeAsBase64().toString()
	headers.put("Authorization", authorizationEncoded)

	log.trace("Getting ${apiCommand}")
    log.trace "with Headers: '${headers}'"
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

def parse(String description) {
	log.trace("Parse ${description}")
	def map = stringToMap(description)
	//log.debug map
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

def putImageInS3(map) {
	def s3ObjectContent
	try {
		def imageBytes = getS3Object(map.bucket, map.key + ".jpg")
		if(imageBytes) {
			s3ObjectContent = imageBytes.getObjectContent()
			def bytes = new ByteArrayInputStream(s3ObjectContent.bytes)
			//log.debug("PutImageInS3: Storing Image")
			storeImage(getPictureName(), bytes)
		}
	} catch(Exception e) {
		log.error e
	} finally {
		if (s3ObjectContent) {
			s3ObjectContent.close()
		}
	}
}

private getPictureName() {
	def pictureUuid = java.util.UUID.randomUUID().toString().replaceAll('-', '')
	"image" + "_$pictureUuid" + ".jpg"
}

private String convertIPtoHex(ipAddress) { 
	String hex = ipAddress.tokenize( '.' ).collect {  String.format( '%02x', it.toInteger() ) }.join()
	return hex
}

private String convertPortToHex(port) {
	String hexport = port.toString().format( '%04x', port.toInteger() )
	return hexport
}

def start() {

	def dataLiveVideo = [
		OutHomeURL  : "http://${dns}:${port}/videostream.cgi?user=${username}&pwd=${password}",
		InHomeURL   : "http://${ip}:${port}/videostream.cgi?user=${username}&pwd=${password}",
		ThumbnailURL: "http://cdn.device-icons.smartthings.com/camera/dlink-indoor@2x.png",
		cookie      : [key: "key", value: "value"]
	]

	def event = [
		name           : "stream",
		value          : groovy.json.JsonOutput.toJson(dataLiveVideo).toString(),
		data		   : groovy.json.JsonOutput.toJson(dataLiveVideo),
		descriptionText: "Starting the livestream",
		eventType      : "VIDEO",
		displayed      : false,
		isStateChange  : true
	]
    
    
	sendEvent(event)
}

def getInHomeURL() {
	 [InHomeURL: "http://${ip}:${port}/videostream.cgi?user=${username}&pwd=${password}"]
}