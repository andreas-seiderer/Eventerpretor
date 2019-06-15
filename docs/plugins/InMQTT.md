## Description
Receive MQTT messages from a topic / multiple topics of a broker. SSL certificate pinning for a secure connection is possible.

Received messages are passed to other nodes as hashmap with the keys "topic" and "message".

## Special options
* clientId: id of the MQTT client; please choose a unique id for each InMQTT node!
