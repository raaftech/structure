// Eventbus stuff

var eventBus = new vertx.EventBus("http://localhost:8080/eventbus");

eventBus.onopen = function() {
    console.log("Registered to eventbus");
    fillTemplateNodeList();
}

function startUpdate() {
    if (eventBus.readyState()) {
        eventBus.send("org.openpcf.universal.actor.schemamanager",
                {"msgh":"update", "args":{}}, function(data) {
                    eventBus.registerHandler("org.openpcf.universal.worker.status_"
                        + data.result.says, function(msg) {
                        console.log(msg);
                    });
                });
    }
}

function findTypeNode(node) {
    if (eventBus.readyState()) {
        eventBus.send("org.openpcf.universal.actor.framehelper",
                {"msgh":"typenode", "args":{"node":node}}, function(data) {
                    fillTypeNodeTable(data);
                });
    }
}

function findInstanceNode(node) {
    if (eventBus.readyState()) {
        eventBus.send("org.openpcf.universal.actor.framehelper",
                {"msgh":"instancenode", "args":{"node":node}}, function(data) {
                    fillInstanceNodeTable(data);
                });
    }
}

function findTemplateNode(node) {
    if (eventBus.readyState()) {
        eventBus.send("org.openpcf.universal.actor.framehelper",
                {"msgh":"templatenode", "args":{"node":node}}, function(data) {
                    console.log(data);
                    label = document.getElementById("selectedTemplateUuid");
                    label.innerHTML = node;
                    fillTemplateTable(data);
                });
    }
}

function createInstanceNode(form) {
    if (eventBus.readyState()) {
        kvs = {};
        for (i =  0; i < form.length; i++) {
            if (form.elements[i].id == "kvs") {
                kvs[form.elements[i].name] = form.elements[i].value;
            }
        }
        args = { name: form.elements.namedItem("instanceName").value,
                 desc: form.elements.namedItem("instanceDesc").value,
                 nodeuuid: form.elements.namedItem("instanceOf").value,
                 kvs: kvs };
        eventBus.send("org.openpcf.universal.actor.framehelper",
                {"msgh":"createinstancenode", "args":args}, function(data) {
                    console.log(data);
                });
    }
    return false;
}

// DOM modification helpers

function fillTypeNodeTable(data) {
    console.log(data.result);
    if (data.result.error == null) {
        document.getElementById("typeNodeName").innerHTML = data.result.name;
        document.getElementById("typeNodeUuid").innerHTML = data.result.uuid;
        document.getElementById("typeNodeDesc").innerHTML = data.result.desc;
        document.getElementById("typeNodeIsa").innerHTML  = "<a href=\"javascript:findTypeNode('" + data.result.isa + "')\">" + data.result.isa + "</a>";

        schema = JSON.parse(data.result.schema);
        createInstanceNodeForm(data.result.uuid, schema);
    } else {
        alert("ERROR: " + data.result.error);
    }
}

function createInstanceNodeForm(instanceOf, schema) {
    htmlForm = document.getElementById("createInstance");

    html = ['<form onsubmit="return false" id="instanceNodeForm"><table>',
            '<input type="hidden" name="instanceOf" value="' + instanceOf + '">',
            '<tr><td>Name:</td><td><input type="text" name="instanceName"></td></tr>',
            '<tr><td>Desc:</td><td><input type="text" name="instanceDesc"></td></tr>']

    for (var key in schema) {
        obj = schema[key];

        if (obj.defaultValue == "null") {
            defaultValue = "";
        } else {
            defaultValue = obj.defaultValue;
        }

        html.push('<tr><td>' + key + '</td><td><input type="text" name="' + key + '" id="kvs" value="' + defaultValue + '" title="' + obj.description + '"></td></tr>');
    }

    html.push('<tr><td><button onclick="javascript:createInstanceNode(document.getElementById(\'instanceNodeForm\'))">Create InstanceNode</button></td></tr>');
    html.push('</table></form>');

    htmlForm.innerHTML = html.join('\n');
}

function fillInstanceNodeTable(data) {
    console.log(data.result);
    if (data.result.error == null) {
        table = document.getElementById("instanceNodeResult");

        html = ['<tr><td>name</td><td>' + data.result.name + '</td></tr>',
                '<tr><td>uuid</td><td>' + data.result.uuid + '</td></tr>',
                '<tr><td>desc</td><td>' + data.result.desc + '</td></tr>'];

        for (el in data.result.kv) {
            obj = JSON.parse(data.result.kv[el]);
            for (key in obj) {
                html.push('<tr><td>' + key + '</td><td>' + obj[key] + '</td></tr>');
            }
        }

        html.push('<tr><td colspan="2">');
        if (data.result.hasOwnProperty('incomponent')) {
            html.push('<a href="javascript:findInstanceNode(\'' + data.result.incomponent + '\')">parent component</a><br>');
        }
        if (data.result.hasOwnProperty('components')) {
            html.push('<select onchange="javascript:findInstanceNode(this.options[this.selectedIndex].value)" onfocus="this.selectedIndex=-1">');
            for (var i in data.result.components) {
                var obj = data.result.components[i];
                html.push('<option value="' + obj.uuid + '">' + obj.name + '</option>');
            }
            html.push('</select>');
        }

        table.innerHTML = html.join('\n');
    } else {
        alert("ERROR: " + data.result.error);
    }
}

function createTemplateNode(parenttmpl, name, desc, isa, required, unique, start) {
    if (eventBus.readyState()) {
        args = { parent: parenttmpl.value,
                 name: name.value,
                 desc: desc.value,
                 isa: isa.value,
                 required: required,
                 unique: unique,
                 start: start};

        eventBus.send("org.openpcf.universal.actor.framehelper",
                {"msgh":"createtemplatenode", "args":args}, function(data) {
                    console.log(data);
                    document.getElementById("uuidNewTemplateNode").value = data.result.uuid;
                });
        if (start == true) {
            clearTemplateNodesList();
            fillTemplateNodeList();
        } else {
            list = document.getElementById("templateNodesList");
            findTemplateNode(list.options[list.selectedIndex].value);
        }
    }
    return false;
}

function recurseTemplate(data, belongsTo, depth) {
    for (var i = 0; i < data.length; i++) {
        obj = data[i];

        html.push('<tr><td><fieldset name="' + obj.isa + '"><legend><h3>' + obj.name + '</h3></legend>',
            '<p>' + obj.desc + '<br><small>' + obj.uuid + '</small></p>',
            '<table>',
            '<tr><td>Name:</td><td><input type="text" name="name"></td></tr>',
            '<tr><td>Desc:</td><td><input type="text" name="desc"></td></tr>');

        schema = JSON.parse(obj.schema);
        for (var key in schema) {
            val = schema[key];

            if (val.defaultValue == "null") {
                defaultValue = "";
            } else {
                defaultValue = val.defaultValue;
            }
            html.push('<tr><td>' + key + '</td><td><input type="text" name="' + key + '" id="kvs" value="' + defaultValue + '" title="' + val.description + '"></td></tr>');
        }
        html.push('<input type="hidden" name="templateuuid" id="templateUuid" value="' + belongsTo + '">');
        html.push('<input type="hidden" name="nodeuuid" id="nodeUuid" value="' + obj.isa + '">');
        html.push('<input type="hidden" class="depth" name="depth" id="depth" value="' + depth + '">');
        html.push('<tr><td colspan="2"><p align="right"><a href="javascript:removeNodeFromTemplate(\'' + belongsTo + '\', \'' + obj.uuid + '\')">remove</a></p></td></tr>')
        html.push('</table></fieldset></td></tr>');
        if (obj.hasOwnProperty("children")) {
            ret = recurseTemplate(obj.children, obj.uuid, depth+1);
            html.concat(ret);
        }
    }
    return html;
}

function fillTemplateTable(data) {
    if (data.result.error == null) {
        table = document.getElementById("templateResult");
        html = ['<tr><td><fieldset name="' + data.result.isa + '"><legend><h3>' + data.result.name + '</h3></legend>',
                '<p>' + data.result.desc + '<br><small>' + data.result.uuid + '</small></p>',
                '<table>',
                '<tr><td>Name:</td><td><input type="text" name="name"></td></tr>',
                '<tr><td>Desc:</td><td><input type="text" name="desc"></td></tr>'];

        schema = JSON.parse(data.result.schema);
        for (var key in schema) {
            val = schema[key];

            if (val.defaultValue == "null") {
                defaultValue = "";
            } else {
                defaultValue = val.defaultValue;
            }
            html.push('<tr><td>' + key + '</td><td><input type="text" name="' + key + '" id="kvs" value="' + defaultValue + '" title="' + val.description + '"></td></tr>');
        }

        html.push('<input type="hidden" name="templateuuid" id="templateUuid" value="' + data.result.uuid + '">');
        html.push('<input type="hidden" name="nodeuuid" id="nodeUuid" value="' + data.result.isa + '">');
        html.push('<input type="hidden" class="depth" name="depth" id="depth" value="0">');
        html.push('</table></fieldset></td></tr>');

        if (data.result.hasOwnProperty("children")) {
            comprises = data.result.children;
            html.concat(recurseTemplate(comprises, data.result.uuid, 1));
        }
        table.innerHTML = html.join('\n');
    } else {
        alert("ERROR: " + data.result.error);
    }
}

function instantiateTemplate(form) {
	if (eventBus.readyState()) {
		list = document.getElementById("templateNodesList");
		var selectedtemplate = list.options[list.selectedIndex].value;
		var jsonform = { template: selectedtemplate,
				nodes: {}};
		var fieldsets = form.getElementsByTagName("fieldset");
		for (var i = 0; i < fieldsets.length; i++) {
			var fieldsetname = fieldsets[i].name;
			//jsonform["nodes"][fieldsetname]["kvs"] = {};
			var inputs = fieldsets[i].getElementsByTagName("input");
			var depth = fieldsets[i].getElementsByClassName("depth")[0].value;
			if (!jsonform.nodes.hasOwnProperty(fieldsetname)) {
				jsonform["nodes"][fieldsetname] = {};
			}
			jsonform["nodes"][fieldsetname][depth] = [];

			var obj = {};
			obj["kvs"] = {};
			for (var j = 0; j < inputs.length; j++) {
				var name = inputs[j].name;
				var id = inputs[j].id;
				var value = inputs[j].value;
				if (name != "depth" && name != "templateuuid" && name != "nodeuuid") {
					if (id  == "kvs") {
						obj["kvs"][name] = value;
					} else {
						obj[name] = value;
					}
				}
			}
			jsonform["nodes"][fieldsetname][depth].push(obj);
			console.log(JSON.stringify(jsonform));
		}
        eventBus.send("org.openpcf.universal.actor.framehelper",
            {"msgh":"instantiatetemplate", "args":jsonform}, function(data) {
                console.log(data);
            });
    }
}

function fillTemplateNodeList() {
    if (eventBus.readyState()) {
        eventBus.send("org.openpcf.universal.actor.framehelper",
            {"msgh":"findalltemplatenodes", "args":{}}, function(data) {
                list = document.getElementById("templateNodesList");
                for (el in data.result) {
                    option = document.createElement("option");
                    option.text = el;
                    option.value = data.result[el];
                    list.add(option);
                }
            });
        return true;
    }
    return false;
}

function addNodeToTemplate(uuid, templateUuid) {
    if (eventBus.readyState()) {
        console.log(uuid);
        args  = { uuid: uuid,
                  templateUuid: templateUuid };
        console.log(args);
        eventBus.send("org.openpcf.universal.actor.framehelper",
        {"msgh":"addnodetotemplate", "args":args},
        function(data) {
        });
    }
}

function clearTemplateNodesList() {
    list = document.getElementById("templateNodesList");
    length = list.options.length;

    var i;
    for (i = list.options.length-1; i >= 0; i--) {
        list.remove(i);
    }
}

function removeNodeFromTemplate(templateUuid, nodeUuid) {
    if (eventBus.readyState()) {
        list = document.getElementById("templateNodesList");
        args = { templateUuid: templateUuid,
               nodeUuid: nodeUuid };

        if (list.options[list.selectedIndex].value != templateUuid) {
            confirm = window.confirm("Trying to delete element from template that's in a subtemplate. Remove from original template?");
        }
        if (confirm || list.options[list.selectedIndex].value == templateUuid) {
            eventBus.send("org.openpcf.universal.actor.framehelper",
            {"msgh":"removenodefromtemplate", "args":args},
            function(data) {
                console.log(data);
                findTemplateNode(list.options[list.selectedIndex].value);
            });
        }
    }
}