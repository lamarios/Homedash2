/**
 * This file will handle all the websocket related stuff
 */
var ws;
function initWebsocket() {
	ws = new WebSocket('ws://' + window.location.host + '/ws');
	try {
		ws.onmessage = onMessage;

		ws.onopen = function(e) {
			for (i = 0; i < MODULES.length; i++) {
				if (MODULES[i] != null && MODULES[i].onConnect != undefined) {
					MODULES[i].onConnect();
				}
			}
			sendMessage(-1, "changePage", PAGE);
			sendMessage(-1, "changeLayout", LAYOUT.id);
		}

		ws.onerror = function(error) {
			console.error('There was an un-identified Web Socket error');
		};

		ws.onclose = function() {
			// $("#global-overlay p").html('Connection to server lost.<br /><a
			// href="' + window.location + '" class="btn btn-warning" > Refresh
			// page </a>');
			// $("#global-overlay").show();
			// $("#global-overlay").addClass('bounceDown');

		}
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
	case 'clientId':
		CLIENT_ID = json.message;
		break;
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
		$('#' + json.moduleId + '-overlay').html('This remote module is not available at the moment.');
		$('#' + json.moduleId + '-overlay').show();
		break;
	default:
		$('#' + json.moduleId + '-overlay').hide();
		break;

	}

	// getting the module element to know the size
	//var functionName = 'modules[' + json.moduleId + '].onMessage.size' + $('.gridster .module[data-module="' + json.moduleId + '"]').attr('data-size');
//	var functionName = 'modules[' + json.moduleId + '].onMessage';
//	console.log(functionName);
//	var fn = window[functionName];
//	if (typeof fn === 'function') {
//		fn(json.command, json.message, json.extra);
//	}else{
//		console.log(functionName+': not a function')
//	}
	var size = $('.gridster .module[data-module="' + json.moduleId + '"]').attr('data-size');
	MODULES[json.moduleId]['onMessage_'+size](json.command, json.message, json.extra);
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
