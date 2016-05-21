var PAGE = 1;
var LAYOUT;
var MODULES = [];
$(document).ready(function() {
	// ///////////////////////////////////////////////
	// ///////////INIT
	var gridster;

	getLayout();

	// ///////////////////////////////////////////////
	// ///////////EVENT LISTENERS

	/**
	 * Refresh the layout if the window size changes
	 */
	var resizeTimeout;

	$(window).resize(function() {
		clearTimeout(resizeTimeout);
		resizeTimeout = setTimeout(function() {
			getLayout();
		}, 500);
	});

	/**
	 * show the available sizes for a module
	 */
	$(document).on('click', '.gridster .module .module-resize', function() {
		var element = $(this);
		var moduleId = element.parents('.module').attr('data-module');

		console.log('Getting available sizes for module ' + moduleId);
		$.getJSON('/module/' + moduleId + '/availableSizes', function(json) {
			console.log(json);

			var html = [];
			html.push('<ul><li>');
			html.push(json.join('</li><li>'));
			html.push('</li></ul>');
			element.siblings('.module-sizes').html(html.join(''));

		});

	});

	$(document).on('click', '.gridster .module .module-sizes ul li', function() {
		var element = $(this);
		var moduleElem = element.parents('.module');
		var size = element.html().split('x');

		console.log(size);
		var width = size[0];
		var height = size[1];
		moduleElem.attr('data-size', width + 'x' + height);

		gridster.resize_widget(moduleElem, width, height, function() {
			savePositions();
			getModuleContent(moduleElem.attr('data-module'), moduleElem.attr('data-size'))
		});

	});

	// ///////////////////////////////////////////////
	// ///////////Functions

	/**
	 * Gets the current layout
	 */
	function getLayout() {
		var viewportWidth = Math.max(document.documentElement.clientWidth, window.innerWidth || 0);

		// getting th layout for the page and view port
		$.get('/modules-layout/' + PAGE + '/' + viewportWidth, function(html) {
			$('#layout').html(html);
			getLayoutInfo(viewportWidth);
			getModulesContent();
		});
	}

	/**
	 * Get the layout information so we can use it to initate the grid
	 */
	function getLayoutInfo(viewportWidth) {
		$.getJSON('/layout-info/json/' + viewportWidth, function(json) {
			LAYOUT = json;
			console.log(LAYOUT);
			initGridster();
			// Starting the websocket or just refreshing the layout
			if (ws === undefined) {
				initWebsocket();
			} else {
				sendMessage(-1, "changeLayout", LAYOUT.id);
			}
		});
	}

	/**
	 * Get html of the modules
	 */
	function getModulesContent() {
		$('#layout .module').each(function() {
			var module = $(this);
			getModuleContent(module.attr('data-module'), module.attr('data-size'))
		});
	}

	function getModuleContent(moduleId, size) {
		var module = $('#layout .module[data-module="' + moduleId + '"]');
		$.get('/module-content/' + moduleId + '/' + size, function(html) {
			module.find('.content').html(html);
		});
	}

	/**
	 * Save the positions of the elements on the grid for this particular layout
	 */
	function savePositions(event, ui) {
		var html = [];
		$('#layout .module').each(function() {
			var module = $(this);

			var moduleStr = [];
			moduleStr.push(module.attr('data-module'));
			moduleStr.push(module.attr('data-col'));
			moduleStr.push(module.attr('data-row'));
			moduleStr.push(module.attr('data-size'));

			html.push(moduleStr.join(','));
		});

		var data = {
			data : html.join('-')
		};

		$.post('/save-module-positions/' + LAYOUT.id, data, function(result) {
			console.log(result);
		});

	}

	/**
	 * Initiate the grid
	 */
	function initGridster() {
		gridster = $(".gridster ul").gridster({
			widget_margins : [ 1, 1 ],
			widget_base_dimensions : [ 150, 150 ],
			min_cols: LAYOUT.maxGridWidth,
			max_cols : LAYOUT.maxGridWidth,
			draggable : {
				stop : savePositions
			}
		}).data('gridster');
		gridster.recalculate_faux_grid();
	}

	/**
	 * 
	 */
});