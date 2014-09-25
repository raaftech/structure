// Copyright © 2008-2014 RAAF Technology bv

var eventBus = new vertx.EventBus(
        window.location.protocol + '//' +
        window.location.hostname + ':'  +
        window.location.port     + '/eventbus');

function displayResult(data) {
    // Display query result(s) in console
    console.log(data.result);
    // If there are any results
    if(data.result.cypher.data.length > 0) {
        // Empty list
        $('.result').empty();
        // Display query result(s) in html
        $('<li>', {class: 'header'}).text(data.result.name).appendTo('.result');
        $.each(data.result.cypher.data, function(i, arr) {
            var comp = arr[0].data;
            $('<li>', {text: comp.name, class: 'row', 'data-name': comp.name}).appendTo('.result');
        });
    }
}

function displayVersion(data) {
    $('#version').append(data.result.release.name + '&nbsp;' + data.result.release.version + '<br />' +
        data.result.release.copyright + '<br />' + data.result.release.license);
}

function displayDialog(data) {
    $('<div>', {id: 'dialog'}).appendTo('body');
    var body = $('<div>');
    $('<p>', {text: JSON.stringify(data)}).appendTo(body);
    body.appendTo('#dialog');
}

// Fire when connected to the eventbus
eventBus.onopen = function eventbusConnected() {
    eventBus.send('org.openpcf.universal.actor.frame', { "msgh": "component", "args": {} }, function(data) {
        displayResult(data);
    });

    eventBus.send('org.openpcf.universal.actor.schemamanager', { "msgh": "showVersion", "args": {} }, function(data) {
        displayVersion(data);
    });
};

$(document).ready(function() {
    $('body').on('click', '#dialog', function() {
        $('#dialog').remove();
    });

    $('ul.result').on('click', 'li.row', function(e) {
        if(e.target == this) {
            var name = $(this).data('name');
            eventBus.send('org.openpcf.universal.actor.frame', { "msgh": "component", "args": {"name": name}}, function (data) {
                displayResult(data);
            });
        }
    });

    $('ul.result').on({
        mouseenter: function() {
            var parent = $(this);
            var tools = $('<ul>', {class: 'tools'});
            $('<a>', {text: 'Start', 'href': '#'}).appendTo($('<li>').appendTo(tools));
            $('<a>', {text: 'Stop', 'href': '#'}).appendTo($('<li>').appendTo(tools));
            $('<a>', {text: 'Edit', 'href': '#'}).appendTo($('<li>').appendTo(tools));
            $('<a>', {text: 'Remove', 'href': '#'}).appendTo($('<li>').appendTo(tools));
            tools.appendTo(parent);
        },
        mouseleave: function() {
            $('ul.tools').remove();
        }
    }, 'li.row');

    $('ul.result').on('click', '.tools', function(e) {
        e.preventDefault();
        var name = $(this).parent().data('name'),
            action = $(e.target).text();
        console.log('a tool item is clicked: ' + name + ' ' + action);
    });

    $('ul.nav').on('click', 'li', function(e) {
        e.preventDefault();
        $('ul.nav li').removeClass('clicked');
        $(this).addClass('clicked');
        var name = $(this).data('name');
        eventBus.send('org.openpcf.universal.actor.frame', { "msgh": "component", "args": {"name": name}}, function(data) {
            displayResult(data);
        });
    });

    $('[name="search"]').keyup(function() {
        var src = $(this).val();
        if(src.length > 0) {
            eventBus.send('org.openpcf.universal.actor.frame', { "msgh": "search", "args": {"name": src}}, function(data) {
                displayResult(data);
            });
        }
        else{
            eventBus.send('org.openpcf.universal.actor.frame', { "msgh": "component", "args": {} }, function(data) {
                displayResult(data);
            });
        }
    });
});
