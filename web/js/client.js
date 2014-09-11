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
        $('<li>', {class: 'header'}).text(result.query).appendTo('.result');
        $.each(result.data, function(i, arr) {
            var comp = arr[0].data;
            $('<li>', {text: comp.name, class: 'row', 'data-name': comp.name}).appendTo('.result');
        });
    }
}

// Fire when connected to the eventbus
eventBus.onopen = function eventbusConnected() {
    eventBus.send('frame.component.isa', {}, function(result) {
        displayResult(result);
    });
};

$(document).ready(function() {
    $('ul.result').on('click', 'li.row', function() {
        var name = $(this).data('name');
        eventBus.send('frame.component.isa', {'name': name}, function(result) {
            displayResult(result);
        });
    });
});




