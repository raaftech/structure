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
        $('<li>', {class: 'header open'}).text(data.result.name).appendTo('.result');
        $.each(data.result.cypher.data, function(i, arr) {
            var comp = arr[0].data;
            $('<li>', {text: comp.name, class: 'row', 'data-name': comp.name}).appendTo('.result');
        });
    }
}

function displayInformation(data) {
    $('#info').empty();
    var node = data.result.cypher.data[0][0].data;
    var labels = data.result.cypher.data[0][1];
    var basic = $('<div>', {class: 'basic'});
    var detail = $('<ul>', {class: 'detail'});
    $('<img>', {'src': '/img/logo_dummy.png', 'align': 'left'}).appendTo(basic);
    $('<h1>', {text: node.name}).appendTo(basic);
    $('<p>', {text: node.desc}).appendTo(basic);
    var buttons = $('<p>');
    $('<a>', {class: 'button', text: 'edit'}).appendTo(buttons);
    $('<a>', {class: 'button-transparant', text: '...'}).appendTo(buttons);
    buttons.appendTo(basic);
    $('<h2>', {text: 'Details'}).appendTo(detail);
    $.each(node, function(k,v) {
        var li = $('<li>');
        $('<p>', {text: v}).appendTo(li);
        li.prepend($('<label>', {text: k}));
        li.appendTo(detail);
    });
    var li = $('<li>');
    $('<p>', {text: labels.join(', ')}).appendTo(li);
    li.prepend($('<label>', {text: 'labels'}));
    li.appendTo(detail);
    basic.appendTo('#info');
    detail.appendTo('#info');
}

function displayVersion(data) {
    $('#version').append(data.result.release.name + '&nbsp;' + data.result.release.version + '&nbsp;<a href="#" id="cfu">Check for updates</a><br />' +
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
    eventBus.send('org.openpcf.universal.actor.framehelper', { "msgh": "component", "args": {} }, function(data) {
        displayResult(data);
    });

    eventBus.send('org.openpcf.universal.actor.framehelper', { "msgh": "version", "args": {} }, function(data) {
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
            eventBus.send('org.openpcf.universal.actor.framehelper', { "msgh": "component", "args": {"name": name}}, function (data) {
                displayResult(data);
            });
            eventBus.send('org.openpcf.universal.actor.framehelper', { "msgh": "node", "args": {"name": name}}, function (data) {
                displayInformation(data);
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

    $('ul.result').on('click', '.header', function() {
        if($(this).hasClass('closed')) {
            $(this).removeClass('closed');
            $(this).addClass('open');
            $('ul.result > li.row').show();

        }
        else{
            $(this).removeClass('open');
            $(this).addClass('closed');
            $('ul.result > li.row').hide();
        }
    });

    $('ul.nav').on('click', 'li', function(e) {
        e.preventDefault();
        $('ul.nav li').removeClass('clicked');
        $(this).addClass('clicked');
        var name = $(this).data('name');
        eventBus.send('org.openpcf.universal.actor.framehelper', { "msgh": "component", "args": {"name": name}}, function(data) {
            displayResult(data);
        });
    });

    $('[name="search"]').keyup(function() {
        var src = $(this).val();
        if(src.length > 0) {
            eventBus.send('org.openpcf.universal.actor.framehelper', { "msgh": "search", "args": {"name": src}}, function(data) {
                displayResult(data);
            });
        }
        else{
            eventBus.send('org.openpcf.universal.actor.framehelper', { "msgh": "component", "args": {} }, function(data) {
                displayResult(data);
            });
        }
    });

    $('#version').on('click', '#cfu', function(e) {
        e.preventDefault();
        eventBus.send('org.openpcf.universal.actor.schemamanager', { "msgh": "update", "args": {} }, function(data) {
            displayDialog(data);
        });
    })
});
