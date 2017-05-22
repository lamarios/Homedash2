/**
 * This file will handle all the websocket related stuff
 */
var ws;

$(document).ready(function () {
    $('#overlay .reload-page').click(function () {
        location.reload();
    });
});


function initWebsocket() {
    ws = new WebSocket('ws://' + window.location.host + '/ws');
    try {
        ws.onmessage = onMessage;

        ws.onopen = function (e) {
            for (i = 0; i < MODULES.length; i++) {
                if (MODULES[i] != null && MODULES[i].onConnect != undefined) {
                    MODULES[i].onConnect();
                }
            }
            sendMessage(-1, "changePage", PAGE);
            sendMessage(-1, "changeLayout", LAYOUT.id);
        };

        ws.onerror = function (error) {
            console.error('There was an un-identified Web Socket error');
        };

        ws.onclose = showOfflineOverlay;
    } catch (e) {
        console.error('Sorry, the web socket at "%s" is un-available error', WS_ADDRESS);
        console.log(e);
    }
}

function initFullScreenWebsocket() {
    ws = new WebSocket('ws://' + window.location.host + '/ws-full-screen');
    try {
        ws.onmessage = onFullScreenMessage;

        ws.onopen = function (e) {
            MODULE.onConnect();
            MODULE.documentReady('full-screen');
            sendMessage(MODULE.moduleId, "setModule", "");

        };

        ws.onerror = function (error) {
            console.error('There was an un-identified Web Socket error');
        };

        ws.onclose = showOfflineOverlay;
    } catch (e) {
        console.error('Sorry, the web socket at "%s" is un-available error', WS_ADDRESS);
        console.log(e);
    }
}

/**
 * what happens when we receive a message
 *
 * @param event
 */
function onMessage(event) {
    var json = JSON.parse(event.data);

    console.log(event.data);

    switch (json.command) {
        case 'success':
            showSuccessMessage(json.message);
            break;
        case 'error':
            showErrorMessage(json.message);
            break;
        case 'reload':
            location.reload();
            break;
        case 'remote404':
            $('#' + json.id + '-overlay').html('This remote module is not available at the moment.');
            $('#' + json.id + '-overlay').show();
            break;
        default:
            $('#' + json.id + '-overlay').hide();
            break;

    }

    var size = $('.gridster .module[data-module="' + json.id + '"]').attr('data-size');
    MODULES[json.id]['onMessage_' + size](json.command, json.message, json.extra);

    //removing the loading overlay if it exists
    var loadingOverlay = $('.gridster-item[data-module="' + json.id + '"] .module-loading');

    if (loadingOverlay.length === 1) {
        loadingOverlay.addClass('fade');
        setTimeout(function () {
            loadingOverlay.hide();
        }, 250);
    }
}


/**
 * what happens when we receive a message
 *
 * @param event
 */
function onFullScreenMessage(event) {
    var json = JSON.parse(event.data);

    console.log(event.data);

    switch (json.command) {
        case 'success':
            showSuccessMessage(json.message);
            break;
        case 'error':
            showErrorMessage(json.message);
            break;
        case 'reload':
            location.reload();
            break;
        case 'remote404':
            $('#' + json.id + '-overlay').html('This remote module is not available at the moment.');
            $('#' + json.id + '-overlay').show();
            break;
        default:
            $('#' + json.id + '-overlay').hide();
            break;

    }

    MODULE['onMessage_fullScreen'](json.command, json.message, json.extra);
}


var notificationTimeout;


/*
 Display the overlay when the connection is off
 */
function showOfflineOverlay() {
    $('#overlay').addClass('showing');
}
/**
 * Show a success notification
 * @param message
 */
function showSuccessMessage(message) {
    showNotification('success', message);
}


/**
 * Show Error notification
 * @param message
 */
function showErrorMessage(message) {
    showNotification('error', message);
}


/**
 * Show a notification, will remove the current one first if there is
 * @param type
 * @param message
 */
function showNotification(type, message) {

    var notification = $('#notification');
    if (notification.hasClass('showing')) {
        notification.removeClass('showing');
        clearTimeout(notificationTimeout);
        notificationTimeout = setTimeout(function () {
            showNotification(type, message);
        }, 300);
    } else {
        notification.removeClass();
        notification.find('.message').html(message);
        notification.addClass(type + ' showing');
        clearTimeout(notificationTimeout);
        notificationTimeout = setTimeout(function () {
            notification.removeClass('showing');
        }, 4000);
    }

}

/**
 * sending a message back to the backend
 *
 * @param moduleId
 * @param method
 * @param message
 */
function sendMessage(moduleId, command, message) {
    var wsMsg = new WebsocketMessage();
    wsMsg.message = message;
    wsMsg.id = moduleId;
    wsMsg.command = command;
    var json = JSON.stringify(wsMsg);
    ws.send(json);
    console.log(json);
}

function WebsocketMessage() {
    this.id = -1;
    this.command = "";
    this.message = "";
}
