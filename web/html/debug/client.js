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
                 instanceofuuid: form.elements.namedItem("instanceOf").value,
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

        table.innerHTML = html.join('\n');
    } else {
        alert("ERROR: " + data.result.error);
    }
}

function createTemplateNode(name, desc, owner) {
    if (eventBus.readyState()) {
        args = { name: name.value,
                 desc: desc.value,
                 owner: owner.value };

        eventBus.send("org.openpcf.universal.actor.framehelper",
                {"msgh":"createtemplatenode", "args":args}, function(data) {
                    console.log(data);
                    document.getElementById("uuidNewTemplateNode").value = data.result.uuid;
                });
        clearTemplateNodesList();
        fillTemplateNodeList();
    }
    return false;
}

function recurseTemplate(data, belongsTo) {
//    var html = []
    for (var i = 0; i < data.length; i++) {
        obj = data[i];

        if (obj.hasOwnProperty("comprises")) {
            html.push('<tr><td><fieldset name="' + obj.groupUuid + '"><legend><h3>' + obj.templateName + '</h3></legend>',
                '<p>' + obj.templateDesc + '</p>',
                '<table>',
                '<tr><td>Name:</td><td><input type="text" name="name"></td></tr>',
                '<tr><td>Desc:</td><td><input type="text" name="desc"></td></tr>');

            schema = JSON.parse(obj.groupSchema);
            for (var key in schema) {
                val = schema[key];

                if (val.defaultValue == "null") {
                    defaultValue = "";
                } else {
                    defaultValue = val.defaultValue;
                }
                html.push('<tr><td>' + key + '</td><td><input type="text" name="' + key + '" id="kvs" value="' + defaultValue + '" title="' + val.description + '"></td></tr>');
            }
            html.push('<tr><td><input type="hidden" name="templateuuid" id="templateUuid" value="' + belongsTo + '">');
            html.push('<tr><td><input type="hidden" name="nodeuuid" id="nodeUuid" value="' + obj.groupUuid + '">');
            html.push('</table></fieldset></td></tr>');
            ret = recurseTemplate(obj.comprises, obj.templateUuid);
            html.concat(ret);
        } else {
            html.push('<tr><td><fieldset name="' + obj.typeUuid + '"><legend><h3>' + obj.typeName + '</h3></legend>',
            '<p>' + obj.typeDesc + '</p>',
            '<table>',
            '<tr><td>Name:</td><td><input type="text" name="name"></td></tr>',
            '<tr><td>Desc:</td><td><input type="text" name="desc"></td></tr>');
            schema = JSON.parse(obj.typeSchema);
            for (var key in schema) {
                val = schema[key];

                if (val.defaultValue == "null") {
                    defaultValue = "";
                } else {
                    defaultValue = val.defaultValue;
                }
                html.push('<tr><td>' + key + '</td><td><input type="text" name="' + key + '" id="kvs" value="' + defaultValue + '" title="' + val.description + '"></td></tr>');
            }
            html.push('<tr><td><input type="hidden" name="templateuuid" id="templateUuid" value="' + belongsTo + '">');
            html.push('<input type="hidden" name="nodeuuid" id="nodeUuid" value="' + obj.typeUuid + '">');
            html.push('<a href="#" onclick="javascript:removeNodeFromTemplate(this.parentNode.childNodes[0].value, this.parentNode.childNodes[2].value)">remove</a></td></tr>');
            html.push('</table></fieldset></td></tr>');
        }
    }
    return html;
}

function fillTemplateTable(data) {
    if (data.result.error == null) {
        table = document.getElementById("templateResult");
        html = ['<tr><td><fieldset name="' + data.result.groupUuid + '"><legend><h3>' + data.result.templateName + '</h3></legend>',
                '<p>' + data.result.templateDesc + '</p>',
                '<table>',
                '<tr><td>Name:</td><td><input type="text" name="name"></td></tr>',
                '<tr><td>Desc:</td><td><input type="text" name="desc"></td></tr>'];

        schema = JSON.parse(data.result.groupSchema);
        for (var key in schema) {
            val = schema[key];

            if (val.defaultValue == "null") {
                defaultValue = "";
            } else {
                defaultValue = val.defaultValue;
            }
            html.push('<tr><td>' + key + '</td><td><input type="text" name="' + key + '" id="kvs" value="' + defaultValue + '" title="' + val.description + '"></td></tr>');
        }

        html.push('<tr><td><input type="hidden" name="templateuuid" id="templateUuid" value="' + data.result.templateUuid + '">');
        html.push('<tr><td><input type="hidden" name="nodeuuid" id="nodeUuid" value="' + data.result.groupUuid + '">');
        html.push('</table></fieldset></td></tr>');

        comprises = data.result.comprises;
        html.concat(recurseTemplate(comprises, data.result.templateUuid));
        console.log(html);
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
                         children: {}};
        var fieldsets = form.getElementsByTagName("fieldset");
        for (var i = 0; i < fieldsets.length; i++) {
            var fieldsetname = fieldsets[i].name;
            jsonform["children"][fieldsetname] = {};
            jsonform["children"][fieldsetname]["kvs"] = {};
            console.log(fieldsetname);
            var inputs = fieldsets[i].getElementsByTagName("input");
            for (var j = 0; j < inputs.length; j++) {
                var name = inputs[j].name;
                var id = inputs[j].id;
                var value = inputs[j].value;
                if (id == "kvs") {
                    jsonform["children"][fieldsetname]["kvs"][name] = value;
                } else {
                    jsonform["children"][fieldsetname][name] = value;
                }
            }
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
        console.log(templateUuid);
        args  = { uuid: uuid,
                  templateUuid: templateUuid };
        console.log(args);
        eventBus.send("org.openpcf.universal.actor.framehelper",
        {"msgh":"addnodetotemplate", "args":args},
        function(data) {
            list = document.getElementById("templateNodesList");
            findTemplateNode(list.options[list.selectedIndex].value);
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
