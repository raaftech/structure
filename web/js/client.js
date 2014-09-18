// Copyright © 2008-2014 RAAF Technology bv

var eventBus = new vertx.EventBus(
        window.location.protocol + '//' +
        window.location.hostname + ':'  +
        window.location.port     + '/eventbus');

function displayResult(result) {
    // Display query result(s) in console
    console.log(result);
    // If there are any results
    if(result.data.length > 0) {
        // Empty list
        $('.result').empty();
        // Display query result(s) in html
        $('<li>', {class: 'header'}).text(result.name).appendTo('.result');
        $.each(result.data, function(i, arr) {
            var comp = arr[0].data;
            $('<li>', {text: comp.name, class: 'row', 'data-name': comp.name}).appendTo('.result');
        });
    }
}

// Fire when connected to the eventbus
eventBus.onopen = function eventbusConnected() {
    eventBus.send('org.openpcf.universal.frame.component.isa', {}, function(data) {
        displayResult(data);
    });

    eventBus.send('org.openpcf.universal.actor.schemamanager', { "msgh": "showVersion", "args": {} }, function(data) {
        $('#version').html('Universal version ' + data.result.version + '<br />Copyright &copy; 2008-2014 RAAF Technology bv');
    });
};

$(document).ready(function() {
    $('ul.result').on('click', 'li.row', function() {
        var name = $(this).data('name');
        eventBus.send('org.openpcf.universal.frame.component.isa', {'name': name}, function(data) {
            displayResult(data);
        });
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

    $('ul.nav').on('click', 'li', function(e) {
        e.preventDefault();
        $('ul.nav li').removeClass('clicked');
        $(this).addClass('clicked');
        var name = $(this).data('name');
        eventBus.send('org.openpcf.universal.frame.component.isa', {'name': name}, function(data) {
            displayResult(data);
        });
    });

    $('[name="search"]').keyup(function() {
        var src = $(this).val();
        if(src.length > 0) {
            eventBus.send('org.openpcf.universal.frame.search', {'name': src}, function(data) {
                displayResult(data);
            });
        }
        else{
            eventBus.send('org.openpcf.universal.frame.component.isa', {}, function(data) {
                displayResult(data);
            });
        }
    });
});
