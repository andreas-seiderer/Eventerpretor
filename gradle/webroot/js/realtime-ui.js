var client_id = 1;
var graph = null;
var canvas = null;
var nodes = {};


function init() {
	
	graph = new LGraph();
	canvas = new LGraphCanvas("#mycanvas", graph);
	
	
	//node constructor class
	function Transformer()
	{
	  this.addInput("In","number");
	  this.addOutput("Out","number");
	}

	//name to show
	Transformer.title = "Transformer";

	//register in the system
	LiteGraph.registerNodeType("basic/transformer", Transformer );
	
	
	loadCurrentStructure();

	//graph.runStep();
	
	//graph.start()
	
    //loadCurrentPrice();
    //registerHandlerForUpdateCurrentPriceAndFeed();
};


function updateNodeView(obj) {
	console.log(obj);
	
	var pos_in =    [20,20];
	var pos_trans = [750,20];
	var pos_out =   [1500,20];
	
	
	nodes = {};
	
	var keys = Object.keys(obj);
	for(var i=0;i<keys.length;i++){
		var key = keys[i];
		
		//var name = key + ":" + obj[key].classname.substr(obj[key].classname.lastIndexOf(".")+1, obj[key].classname.length);
		var name = key;
		
		if(obj[key].basetype == "Input") {
			var node = LiteGraph.createNode("graph/input");
			node.pos = pos_in;
			pos_in = [pos_in[0], pos_in[1]+50]
			node.size = [250,20];
			node.properties.name = "Out";
			node.title = name;
			
			nodes[key] = node;
			graph.add(node);
		} else if (obj[key].basetype == "Output") {
			var node = LiteGraph.createNode("graph/output");
			node.pos = pos_out;
			pos_out = [pos_out[0], pos_out[1]+80]
			node.size = [250,20];
			node.properties.name = "In";
			node.title = name;
			
			nodes[key] = node;
			graph.add(node);
		} else if (obj[key].basetype == "Transformer") {
			var node = LiteGraph.createNode("basic/transformer");
			node.pos = pos_trans;
			pos_trans = [pos_trans[0], pos_trans[1]+80]
			node.size = [250,20];
			node.properties.name = "In";
			node.title = name;
			
			nodes[key] = node;
			graph.add(node);
		}
	}
	
	var keys = Object.keys(nodes);
	for(var i=0; i<keys.length; i++){
		var key = keys[i];
		
		for (var j=0; j < obj[key].publishto.length; j++) {
			var targetkey = obj[key].publishto[j];
			var targetNode = nodes[targetkey];

			if (targetNode.findInputSlot("In") != -1)
				targetNode.removeInput(0);

			targetNode.addInput(key, 0);
			targetNode.size = [250,targetNode.size[1]];
			nodes[key].connect(0, targetNode, key );
		}
	}


	//nodes["pgsqlListenLED"].connect(0, nodes["stdoutNode"], 0 )
	
	
	/*var node_const = LiteGraph.createNode("graph/input");
	node_const.pos = [20,200];
	node_const.title = "testnode1:In_test";
	node_const.size = [200,20];
	node_const.properties.name = "OUT";
	graph.add(node_const);

	var node_out1 = LiteGraph.createNode("graph/output");
	node_out1.pos = [800,200];
	node_out1.title = "testnode2:Out_blubb";
	node_out1.size = [200,20];
	node_out1.properties.name = "IN";
	graph.add(node_out1);
	
	var node_out2 = LiteGraph.createNode("graph/output");
	node_out2.pos = [800,250];
	node_out2.title = "testnode3:Out_blubb";
	node_out2.size = [200,20];
	node_out2.properties.name = "IN";
	graph.add(node_out2);*/

	//node_const.connect(0, node_out1, 0 );
	//node_const.connect(0, node_out2, 0 );


	//canvas.stopRendering();

}

function loadCurrentStructure() {
    var xmlhttp = (window.XMLHttpRequest) ? new XMLHttpRequest() : new ActiveXObject("Microsoft.XMLHTTP");
    xmlhttp.onreadystatechange = function () {
        if (xmlhttp.readyState == 4) {
            if (xmlhttp.status == 200) {
				updateNodeView(JSON.parse(xmlhttp.responseText));
                //document.getElementById('current_price').innerHTML = JSON.parse(xmlhttp.responseText).price.toFixed(2);
            } else {
                //document.getElementById('current_price').innerHTML = '';
            }
        }
    };
    xmlhttp.open("GET", "http://"+location.host+"/api/ui/", true);
    xmlhttp.send(null);
};

/*function registerHandlerForUpdateCurrentPriceAndFeed() {
    var eventBus = new EventBus('http://localhost:8080/eventbus');
    eventBus.onopen = function () {
        eventBus.registerHandler('auction.' + client_id, function (error, message) {
            document.getElementById('current_price').innerHTML = JSON.parse(message.body).price;
            document.getElementById('feed').value += JSON.parse(message.body).price + '\n';
        });
    }
};*/

function stop() {
    var xmlhttp = (window.XMLHttpRequest) ? new XMLHttpRequest() : new ActiveXObject("Microsoft.XMLHTTP");
    xmlhttp.onreadystatechange = function () {
        if (xmlhttp.readyState == 4) {
            if (xmlhttp.status == 200) {
                document.getElementById('error_message').innerHTML = '';
            } else {
                document.getElementById('error_message').innerHTML = 'Invalid value!';
            }
        }
    };
    xmlhttp.open("PATCH", "http://"+location.host+"/api/ui/" + client_id);
    xmlhttp.setRequestHeader("Content-Type", "application/json");
    xmlhttp.send(JSON.stringify({cmd: "stop"}));
};

function restart_node() {
    var xmlhttp = (window.XMLHttpRequest) ? new XMLHttpRequest() : new ActiveXObject("Microsoft.XMLHTTP");
    xmlhttp.onreadystatechange = function () {
        if (xmlhttp.readyState == 4) {
            if (xmlhttp.status == 200) {
                document.getElementById('error_message').innerHTML = '';
            } else {
                document.getElementById('error_message').innerHTML = 'Invalid value!';
            }
        }
    };
    xmlhttp.open("PATCH", "http://"+location.host+"/api/ui/" + client_id);
    xmlhttp.setRequestHeader("Content-Type", "application/json");
    xmlhttp.send(JSON.stringify({cmd: "restart "+ document.getElementById('nodename').value}));
};
