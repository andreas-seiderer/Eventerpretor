
## Description
Reads in file of OutFile node.

Example entry of OutFile recorded from FHEM via MQTT:<br>
``1505048625300	{"message":"MQTT_DEVICE;mqtt_co2;measured-co2: 867","topic":"fhem"}``

* The timestamp and value have to be separated by a tabulator.
* The data value next to the timestamp is first tried to be parsed from json, if its not in json format a string will be output.

## Special options
* fixeddelay: replay entries of file with a fixed time interval in ms between the entries.

* delayfromfile:
    * true: replay entries with the original time delays as recorded.
    * false: use fixeddelay
