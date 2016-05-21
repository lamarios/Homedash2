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
	$(document).on('click', '.gridster .module .module-settings-icon', function() {
		var element = $(this);

		getSizes(element);

	});

	$(document).on('click', '.gridster .module .module-sizes li', function() {
		var element = $(this);
		changeSize(element);
	});
	
	$(document).on('click', '.gridster .module  li.delete', function() {
		var element = $(this);
		
		deleteModule(element.attr('data-id'));
	});
	
	$('#edit-layout').click(function (){
		toggleLayoutEditMode();
	});
	
	
	$(document).on('click', '.gridster .gridster-item .settings-overlay', function() {
		$('.gridster .gridster-item.selected').removeClass('selected');
		$('.settings-overlay').removeClass('drag-box');
		$(this).parents('.gridster-item').addClass('selected');
		$(this).addClass('drag-box');
	});

	// ///////////////////////////////////////////////
	// ///////////Functions

	/**
	 * Get the available sizes for a module
	 */
	function getSizes(element) {
		
		var moduleElement = element.parents('.module');
		var moduleId = moduleElement.attr('data-module');
		var sizes = moduleElement.find('.module-sizes');
		
		console.log('Getting available sizes for module ' + moduleId);
		sizes.html('<div class="loading"></div>');
		$.getJSON('/module/' + moduleId + '/availableSizes', function(json) {
			console.log(json);

			var html = [];
			$.each(json, function(index, value){
				html.push('<li data-size="', value,'"><a>', value,'</a></li>');
			});
			
			sizes.html(html.join(''));
		});
	}

	/**
	 * Changes the size of a module
	 */
	function changeSize(element) {
		var gridsterElem = element.parents('.gridster-item');
		var moduleElem = element.parents('.module');
		var size = element.attr('data-size').split('x');

		console.log(size);
		var width = size[0];
		var height = size[1];
		moduleElem.attr('data-size', width + 'x' + height);

		gridster.resize_widget(gridsterElem, width, height, function() {
			savePositions();
			getModuleContent(moduleElem.attr('data-module'), moduleElem.attr('data-size'))
		});
	}

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
		module.find('.loading').show();

		$.get('/module-content/' + moduleId + '/' + size, function(html) {
			module.find('.content').html(html);
			module.find('.loading').fadeOut("slow");
		});
	}

	/**
	 * Save the positions of the elements on the grid for this particular layout
	 */
	function savePositions(event, ui) {
		var html = [];
		$('#layout .gridster-item').each(function() {
			var module = $(this);

			var moduleStr = [];
			moduleStr.push(module.attr('data-module'));
			moduleStr.push(module.attr('data-col'));
			moduleStr.push(module.attr('data-row'));
			moduleStr.push(module.attr('data-sizex')+'x'+module.attr('data-sizey'));

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
		gridster = $(".gridster").gridster({
			widget_selector : '.gridster-item',
			widget_margins : [ 1, 1 ],
			widget_base_dimensions : [ 100, 100 ],
			min_cols : LAYOUT.maxGridWidth,
			max_cols : LAYOUT.maxGridWidth,
			draggable : {
				stop : savePositions,
				handle : '.settings-overlay.drag-box'
			}
		}).data('gridster');
		gridster.recalculate_faux_grid();
	}
	
	/**
	 * Deletes a module
	 */
	function deleteModule(moduleId){
		if(confirm('Delete this module ?')){
			window.location.href = '/module/'+moduleId+'/delete';
		}
	}
	
	
	function toggleLayoutEditMode(){
		$('#layout .gridster').toggleClass('layout-edit');
	}
});

/**
 * needs to be available for everyone
 */
function rootElement(moduleId) {
	return $('#layout .module[data-module="' + moduleId + '"] .content');
};