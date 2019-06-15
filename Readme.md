## Description
Eventerpretor is an open-source node-based Complex Event Processor (CEP) written in Kotlin. It is intended to process discrete events which arrive sporadically or at low sample rates. Each processing node uses an own thread and all nodes can be connected together as pipelines. It is possible to run multiple pipelines in one instance. All nodes are connected with each other by passing data into "LinkedBlockingQueues" so that the nodes are not active until new data arrives. 
Eventerpretor was especially created to process and interpret data from home automation software like FHEM. It can be used independently of a specific software solution but can also combine the data of different data sources. Additionally, due to the integration of MQTT, generic serial port communication and TCP data input it is able to directly access specific sensors and software.
To allow faster prototyping Eventerpretor includes the possibility to use webbased user interfaces for data input and visualizations which are connected via websockets to be able to receive or transmit data from or to other nodes. Another feature allows recording of received messages e.g. from a home automation software that can be replayed with the recorded delays between the original events to allow more realistic testing of pipelines.
Eventerpretor is focused on running headless. A webbased GUI exists to gently stop a pipeline, graphically show current pipelines and restart specific nodes to load new settings without the need to restart the whole program.
Pipelines are defined in Groovy scripts where nodes can be connected. Settings are automatically created and saved as json files. They can be adapted by the user with a text editor.
Since Kotlin uses the Java VM all libraries available for Java can be integrated.

Parts of this software have been used in the following scientific publication as a part of the prototype:
* Hannes Ritschel, Andreas Seiderer, Kathrin Janowski, Ilhan Aslan, and Elisabeth André. 2018. Drink-O-Mender: An Adaptive Robotic Drink Adviser. In Proceedings of the 3rd International Workshop on Multisensory Approaches to Human-Food Interaction (MHFI'18). ACM, New York, NY, USA, Article 3, 8 pages. DOI: https://doi.org/10.1145/3279954.3279957 

## Limitations
* Please note that a development on regular basis is not possible, just very limited support can be given! Nevertheless, contributors are welcome!
* This software is not intended to provide the same functionality as Node-RED or other flow-based frameworks!

## General Features
* Complex Event Processor written in Kotlin
* threadsafe data transport between threaded nodes
* functions for easier prototyping
* low memory consumption (limited by Java's garbage collector)
* low CPU usage: near to zero if there is no data to process
* stand-alone deployment (just Java 8 is required)
* postgresql can be used as data storage but also for asynchronous communication and window based data processing
* secure MQTT and Postgresql connections with certificate pinning are possible
* crossplatform (tested on Windows and Linux - including Raspbian (ARM))

## Documentation and Examples
Is currently located in "docs". Please note that it is not yet complete!
Several examples can be found in "examples". Further examples will be added!

## Nodes
Pipelines can contain three different types of processing nodes: "input", "transform", "output". 
Please note that the TransformNode "TransformGroovy" is a special node since every available in Java command or a function from the included libraries can be used!

A list of currently available nodes is given in the following:

### Input
* InCalDavEvent
* InFile
* InMQTT
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
* TransformMvgFunc
* TransformPassOnChange
* TransformSampleRate
* TransformSensorModel
* TransformSplitToArray

### Output
* OutFile
* OutMQTT
* OutPgsqlAutoDataInsert
* OutPgsqlNotify
* OutSerial
* OutSqliteAutoDataInsert
* OutSTDOUT
* OutTCP
* OutWebVis

## Used libraries
Not all dependencies (of dependencies) are included here. Please visit the websites of specific libraries to see their licenses!

### Java
Java libraries are located in "gradle/libs". Gradle is not used to download them!

* Drools: Apache License Version 2.0
* Groovy:Apache License Version 2.0
* ical4j: BSD-3-Clause
* jSerialComm: GPL v3
* moshi: Apache 2 License
* pgjdbc-ng: 3 clause BSD license
* sqlite-jdbc: Apache License Version 2.0
* vertx: Apache License Version 2.0
* jansi: Apache License Version 2.0


### Javascript
* litegraph: MIT License
* SockJS: MIT License
* vertx-eventbus: Apache License 2.0