//
// Copyright 2015 Skytap Inc.
// 
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//

import groovyx.net.http.RESTClient
import groovyx.net.http.HttpResponseException
import groovyx.net.http.ContentType
import com.urbancode.air.AirPluginTool

def apTool = new AirPluginTool(this.args[0], this.args[1])
props = apTool.getStepProperties()
def configID = props['configID']
def urlName = props['urlName']
def username = props['username']
def password = props['password']
def proxyHost = props['proxyHost']
def proxyPort = props['proxyPort']

def unencodedAuthString = username + ":" + password
def bytes = unencodedAuthString.bytes
encodedAuthString = bytes.encodeBase64().toString()

println "List Environment Published URL Command Info:"
println "	Environment ID: " + configID
println "	URL Name: " + urlName
println "	Proxy Host: " + proxyHost
println "	Proxy Port: " + proxyPort
println "Done"

def skytapRESTClient = new RESTClient('https://cloud.skytap.com/')
skytapRESTClient.defaultRequestHeaders.'Authorization' = 'Basic ' + encodedAuthString
skytapRESTClient.defaultRequestHeaders.'Accept' = "application/json"
skytapRESTClient.defaultRequestHeaders.'Content-Type' = "application/json"
if (proxyHost) {
	if (proxyPort) {
		IDTESRESTClient.setProxy(proxyHost, proxyPort.toInteger(), "http")
	} else {
		println "Error: Proxy Host was specified but no Proxy Port was specified"
		System.exit(1)
	}
}


def locked = 1

while (locked == 1) {
	try {
		locked = 0
		response = skytapRESTClient.get(path: "configurations/" + configID,
			requestContentType: ContentType.JSON)
	} catch (HttpResponseException ex) {
		if (ex.statusCode == 423) {
			println "Environment " + configID + " locked. Retrying..."
			locked = 1
			sleep(5000)
		} else {
			System.err.println "Unexpected Error: " + ex.statusCode + " - " + ex.getMessage()
			System.exit(1)
		}
	}
}
//
// Get Published Set
//

publishSetList = response.data.publish_sets
publishSetFound = 0
publishSetList.each {
	if (it.name == urlName) {
		println "Found Published URL with Name: \"" + it.name + "\" URL: " + it.desktops_url
		urlValue = it.desktops_url
		publishSetFound = 1
	}
}
if (publishSetFound == 0) {
	System.err.println "Error: Published URL with name \"" + urlName + "\" not found"
	System.exit(1)
}

println "Setting publishedURL property to : \"" + urlValue + "\""

apTool.setOutputProperty("publishedURL", urlValue)
apTool.setOutputProperties()
