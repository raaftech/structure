// Eventbus stuff

var eventBus = new vertx.EventBus("http://localhost:8080/eventbus");

eventBus.onopen = function() {
    console.log("Registered to eventbus");
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
