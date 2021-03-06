/** 
 *  Tesla
 *
 *  Copyright 2020 Armand Welsh
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
preferences {
    input name: "homelink", type: "bool", title: "Use HomeLink presence", defaultValue: false
    if (homelink)
        input name: "geofence", type: "number", title: "Meters from home for presence", defaultValue: 50
    input name: "logEnable", type: "bool", title: "Enable logging", defaultValue: false
}

metadata {
	definition (name: "Tesla", namespace: "apwelsh", author: "Armand Welsh") {
		capability "Actuator"
		capability "Battery"
		capability "Lock"
		capability "MotionSensor"
		capability "PresenceSensor"
		capability "Refresh"
		capability "TemperatureMeasurement"
		capability "ThermostatMode"
		capability "ThermostatSetpoint"

		attribute "state", "string"
		attribute "vin", "string"
		attribute "odometer", "number"
		attribute "batteryRange", "number"
		attribute "chargingState", "string"

		attribute "latitude", "number"
		attribute "longitude", "number"
		attribute "method", "string"
		attribute "heading", "number"
		attribute "lastUpdateTime", "date"
		attribute "distanceAway", "number"

		command "wake"
		command "setThermostatSetpoint"
		command "startCharge"
		command "stopCharge"
		command "openFrontTrunk"
		command "openRearTrunk"
	}

}

def initialize() {
	if (logEnable) log.debug "Executing 'initialize'"
    
    sendEvent(name: "supportedThermostatModes", value: ["auto", "off"])
    
    runEvery15Minutes(refresh)
}

// parse events into attributes
def parse(String description) {
	if (logEnable) log.debug "Parsing '${description}'"
}

private processData(data) {
    if(data) {
        if (logEnable) log.debug "processData: ${data}"
        
        sendEvent(name: "state", value: data.state)
        sendEvent(name: "motion", value: data.motion)
        sendEvent(name: "speed", value: data.speed, unit: "mph")
        sendEvent(name: "vin", value: data.vin)
        sendEvent(name: "thermostatMode", value: data.thermostatMode)
        
        if (data.chargeState) {
            sendEvent(name: "battery", value: data.chargeState.battery)
            sendEvent(name: "batteryRange", value: data.chargeState.batteryRange)
            sendEvent(name: "chargingState", value: data.chargeState.chargingState)
        }
        
        if (data.driveState) {
            sendEvent(name: "latitude", value: data.driveState.latitude)
            sendEvent(name: "longitude", value: data.driveState.longitude)
            sendEvent(name: "method", value: data.driveState.method)
            sendEvent(name: "heading", value: data.driveState.heading)
            sendEvent(name: "lastUpdateTime", value: data.driveState.lastUpdateTime)
            def dist = distance(location.latitude, location.longitude, data.driveState.latitude, data.driveState.longitude)
            if (logEnable) log.debug "distance: ${dist}"
            sendEvent(name: "distanceAway", value: dist)
            if (!homelink) {
                sendEvent(name: "presence", value: (dist <= (geofence?:50) ? "present" : "not present")
            }
        }
        
        if (data.vehicleState) {
            sendEvent(name: "presence", value: data.vehicleState.presence)
            sendEvent(name: "lock", value: data.vehicleState.lock)
            sendEvent(name: "odometer", value: data.vehicleState.odometer)
        }
        
        if (data.climateState) {
        	sendEvent(name: "temperature", value: data.climateState.temperature)
            sendEvent(name: "thermostatSetpoint", value: data.climateState.thermostatSetpoint)
        }
    } else {
        if (logEnable) log.error "No data found for ${device.deviceNetworkId}"
    }
}

def refresh() {
    if (logEnable) log.debug "Executing 'refresh'"
    def data = parent.refresh(this)
    processData(data)
}

def wake() {
    if (logEnable) log.debug "Executing 'wake'"
    def data = parent.wake(this)
    processData(data)
    runIn(30, refresh)
}

def lock() {
    if (logEnable) log.debug "Executing 'lock'"
    def result = parent.lock(this)
    if (result) { refresh() }
}

def unlock() {
    if (logEnable) log.debug "Executing 'unlock'"
    def result = parent.unlock(this)
    if (result) { refresh() }
}

def auto() {
	if (logEnable) log.debug "Executing 'auto'"
	def result = parent.climateAuto(this)
    if (result) { refresh() }
}

def off() {
	if (logEnable) log.debug "Executing 'off'"
	def result = parent.climateOff(this)
    if (result) { refresh() }
}

def heat() {
	if (logEnable) log.debug "Executing 'heat'"
	// Not supported
}

def emergencyHeat() {
	if (logEnable) log.debug "Executing 'emergencyHeat'"
	// Not supported
}

def cool() {
	if (logEnable) log.debug "Executing 'cool'"
	// Not supported
}

def setThermostatMode(mode) {
	if (logEnable) log.debug "Executing 'setThermostatMode'"
	switch (mode) {
    	case "auto":
        	auto()
            break
        case "off":
        	off()
            break
        default:
        	if (logEnable) log.error "setThermostatMode: Only thermostat modes Auto and Off are supported"
    }
}

def setThermostatSetpoint(setpoint) {
	if (logEnable) log.debug "Executing 'setThermostatSetpoint'"
	def result = parent.setThermostatSetpoint(this, setpoint)
    if (result) { refresh() }
}

def startCharge() {
	if (logEnable) log.debug "Executing 'startCharge'"
    def result = parent.startCharge(this)
    if (result) { refresh() }
}

def stopCharge() {
	if (logEnable) log.debug "Executing 'stopCharge'"
    def result = parent.stopCharge(this)
    if (result) { refresh() }
}

def openFrontTrunk() {
	if (logEnable) log.debug "Executing 'openFrontTrunk'"
    def result = parent.openTrunk(this, "front")
    // if (result) { refresh() }
}

def openRearTrunk() {
	if (logEnable) log.debug "Executing 'openRearTrunk'"
    def result = parent.openTrunk(this, "rear")
    // if (result) { refresh() }
}

def distance(lat1, lon1, lat2, lon2) {
    // compute horizontal distance bteween two points at sea-level, in meters

    final int R = 6371; // Radius of the earth (equator 6378, at poles 6357, median radius 6371)

    def latDistance = Math.toRadians(lat2 - lat1)
    def lonDistance = Math.toRadians(lon2 - lon1)
    def sinLat = Math.sin(latDistance/2)
    def sinLon = Math.sin(lonDistance/2)
    def a = sinLat * sinLat + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * sinLon * sinLon
    def c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
    def distance = R * c * 1000; // convert to meters

    return distance
}

