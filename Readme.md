## Description
Eventerpretor is an open-source node-based, lightweight Complex Event Processor (CEP) written in Kotlin. It is intended to process discrete events which arrive sporadically or at low sample rates. Each processing node uses an own thread and all nodes can be connected together as pipelines. It is possible to run multiple pipelines in one instance. All nodes are connected with each other by passing data into [LinkedBlockingQueues](https://docs.oracle.com/javase/8/docs/api/?java/util/concurrent/LinkedBlockingQueue.html) so that the nodes are not active until new data arrives. 

Eventerpretor was especially created to process and interpret data from home automation software like [FHEM](https://fhem.de/). It can be used independently of a specific software solution but can also combine the data of different data sources. Additionally, due to the integration of MQTT, generic serial port communication and TCP data input it is able to directly access specific sensors and software.

To allow faster prototyping Eventerpretor includes the possibility to use webbased user interfaces for data input and visualizations which are connected via websockets to be able to receive or transmit data from or to other nodes. Another feature allows recording of received messages e.g. from a home automation software that can be replayed with the recorded delays between the original events to allow more realistic testing of pipelines.

Eventerpretor is focused on running headless. A webbased GUI exists to gently stop a pipeline, graphically show current pipelines (read only) and restart specific nodes to load new settings without the need to restart the whole program.
Pipelines are defined in Groovy scripts where nodes can be connected. Settings are automatically created and saved as json files. They can be adapted by the user with a text editor.

Since Kotlin uses the Java VM all libraries available for Java can be integrated.

## Limitations
* Please note that a development on regular basis is not possible, just very limited support can be given! Nevertheless, contributors are welcome!
* This software is not intended to provide the same functionality as Node-RED or other flow-based frameworks!

## General Features
* lightweight Complex Event Processor written in Kotlin
* threadsafe data transport between threaded nodes
* functions for easier prototyping
* low memory consumption (limited by Java's garbage collector)
* low CPU usage: near to zero if there is no data to process
* easy stand-alone deployment (just Java 8 is required)
* postgresql can be used as data storage but also for asynchronous communication and window based data processing
* secure MQTT and Postgresql connections with certificate pinning are possible
* crossplatform (tested on Windows and Linux - including Raspbian (ARM))

## Documentation and Examples
The documentation is located in "docs". An introduction can be found in this [PDF](docs/Introduction.pdf). Please note that the documentation is currently incomplete!

Several examples can be found in "examples". Further examples will be added!

## Building
An installed version of gradle and Java JDK 8 is required. With the console you just have to type in "gradle build" in the "gradle" directory of this repository.

Another option is to use IntelliJ Idea where you can import the gradle project and use the "gradle [build]" task.

## Running pipelines
### with gradle
With installed gradle and Java JDK 8 you can directly run the examples from console. Enter the gradle directory of this repository and run ```gradle run --args="main_timer2stdout.groovy ../examples/basic 0"```.

### with IntelliJ Idea
After importing the project in the gradle directory you can set up IntelliJ Idea to run the program by choosing the run task. You have to edit it so that arguments are passed to Eventerpretor by setting "Arguments" to ```run --args="main_timer2stdout.groovy ../examples/basic 0"``` in the "gradle [run]" configuration dialog.

## Nodes
Pipelines can contain three different types of processing nodes: "input", "transform", "output". 
Please note that the TransformNode "TransformGroovy" is a special node since every Java command or function from an included library can be used!

A list of currently available nodes is given in the following:

### Input
* InCalDavEvent
* [InFile](docs/plugins/InFile.md)
* [InMQTT](docs/plugins/InMQTT.md)
* InPgsqlListen
* InPgsqlSensorListen
* InSerial
* InSunCalc
* InTCP
* InTimer
* InWebInput

### Transform
* TransformAllowDatatype
* TransformAllowMsgRegex
* TransformArraySelect
* TransformBoolean
* TransformDrools
* TransformFHEMstring
* TransformGroovy
* TransformJSONtoMAP
* TransformMvgFunc
* TransformPassOnChange
* TransformSampleRate
* TransformSensorModel
* TransformSplitToArray
* TransformZigbee2Mqttstring

### Output
* OutFile
* OutMQTT
* OutPgsqlAutoDataInsert
* OutPgsqlNotify
* OutSerial
* OutSqliteAutoDataInsert
* [OutSTDOUT](docs/plugins/OutSTDOUT.md)
* OutTCP
* OutWebVis

## Used libraries
Not all dependencies (of dependencies) are included here. Please visit the websites of specific libraries to see their licenses!

### Java
Java libraries are located in "gradle/libs". Gradle is not used to download them!

* [Kotlin](https://github.com/JetBrains/kotlin): Apache License Version 2.0
* [Drools](https://github.com/kiegroup/drools): Apache License Version 2.0
* [Groovy](https://github.com/apache/groovy): Apache License Version 2.0
* [ical4j](https://github.com/ical4j/ical4j): BSD-3-Clause
* [jSerialComm](https://github.com/Fazecast/jSerialComm): GPL v3
* [moshi](https://github.com/square/moshi): Apache 2 License
* [pgjdbc-ng](https://github.com/impossibl/pgjdbc-ng): 3 clause BSD license
* [sqlite-jdbc](https://github.com/xerial/sqlite-jdbc): Apache License Version 2.0
* [vertx](https://github.com/vert-x3/vertx-lang-kotlin): Apache License Version 2.0
* [jansi](https://github.com/fusesource/jansi): Apache License Version 2.0
* [commons-suncalc](https://github.com/shred/commons-suncalc): Apache License Version 2.0
* [paho.mqtt.java](https://github.com/eclipse/paho.mqtt.java): Eclipse Public License


### Javascript
* [litegraph.js](https://github.com/jagenjo/litegraph.js): MIT License
* [SockJS](https://github.com/sockjs/sockjs-client): MIT License
* [vertx-eventbus](http://vertx.io/): Eclipse Public License and Apache License 2.0


## Usage in literature
Parts of this software have been used in the following scientific publication as a part of the prototype:
* Hannes Ritschel, Andreas Seiderer, Kathrin Janowski, Ilhan Aslan, and Elisabeth André. 2018. Drink-O-Mender: An Adaptive Robotic Drink Adviser. In Proceedings of the 3rd International Workshop on Multisensory Approaches to Human-Food Interaction (MHFI'18). ACM, New York, NY, USA, Article 3, 8 pages. DOI: https://doi.org/10.1145/3279954.3279957 
* Andreas Seiderer, Ilhan Aslan, Chi Tai Dang, and Elisabeth André. 2019. Indoor air quality and wellbeing-enabling awareness and sensitivity with ambient IoT displays. In European Conference on Ambient Intelligence (pp. 266-282). Springer, Cham.
