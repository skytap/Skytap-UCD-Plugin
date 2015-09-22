import groovyx.net.http.RESTClient
import groovyx.net.http.HttpResponseException
import groovyx.net.http.ContentType
import com.urbancode.air.AirPluginTool

def apTool = new AirPluginTool(this.args[0], this.args[1])
props = apTool.getStepProperties()

def configID = props['configID']
def templateID = props['templateID']
def username = props['username']
def password = props['password']

def unencodedAuthString = username + ":" + password
def bytes = unencodedAuthString.bytes
encodedAuthString = bytes.encodeBase64().toString()

println "Add Template to Environment Info:"
println "	Environment ID: " + configID
println "	Template ID: " + templateID
println "Done"

def skytapRESTClient = new RESTClient('https://cloud.skytap.com/')
skytapRESTClient.defaultRequestHeaders.'Authorization: Basic' = encodedAuthString
skytapRESTClient.defaultRequestHeaders.'Accept' = "application/json"
skytapRESTClient.defaultRequestHeaders.'Content-Type' = "application/json"

loopCounter = 1
locked = 1
while ((loopCounter <= 12) && (locked == 1)) {
	try {
		loopCounter = loopCounter + 1
		locked = 0
		response = skytapRESTClient.put(path: "configurations/" + configID,
			body: ['template_id':templateID],
			requestContentType: ContentType.JSON)
	} catch(HttpResponseException ex) {
		if ((ex.statusCode == 423) || (ex.statusCode == 500)) {
			println "Environment is locked or busy. Retrying..."
			locked = 1
			sleep(10000)
		} else {
			System.err.println "Unexpected Error: " + ex.statusCode + " - " + ex.getMessage()
			System.exit(1)
		}
	}
}

println "Added Template " + templateID + " to Environment " + configID


