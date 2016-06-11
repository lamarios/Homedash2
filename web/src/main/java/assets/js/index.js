var PAGE = 1;
var LAYOUT;
var MODULES = [];
var WIDTH;
$(document).ready(function () {
    // ///////////////////////////////////////////////
    // ///////////INIT
    var gridster;

    //finding which page we are
    if (typeof(Storage) !== "undefined") {
        var page = localStorage.getItem("page");

        if (page != undefined) {
            PAGE = page;
        }
    }

    getLayout();


    // ///////////////////////////////////////////////
    // ///////////EVENT LISTENERS

    /**
     * Refresh the layout if the window size changes
     */
    var resizeTimeout;

    $(window).resize(function () {

        var width = Math.max(document.documentElement.clientWidth, window.innerWidth || 0);

        if (width != WIDTH) {
            clearTimeout(resizeTimeout);
            resizeTimeout = setTimeout(function () {
                getLayout();
            }, 500);
        }
    });


    /**
     * show the available sizes for a module
     */
    $(document).on('click', '.gridster .module .module-settings-icon', function () {
        var element = $(this);


        var id = $(this).attr('data-id');
        $('#module-modal').find('a.edit').attr('href', '/module/' + id + '/settings');
        $('#module-modal').attr('data-id', id);

        $('#module-modal').modal('show');

        getSizes();

    });

    /**
     * Changes the size of a module
     */
    $(document).on('click', '#module-modal .resize-size', function () {
        var element = $('#module-modal');
        changeSize(element.attr('data-id'), $(this).attr('data-size'));
    });


    /**
     * delete a module
     */
    $(document).on('click', '#module-modal  p.delete', function () {

        deleteModule($('#module-modal').attr('data-id'));
    });


    /**
     * Enables layout edit mode
     */
    $('#edit-layout, #layout-edit-mode .close-editing').click(function () {
        toggleLayoutEditMode();
    });



    /**
     * Selects an item in layout editting mode
     */
    $(document).on('click', '.gridster .gridster-item .settings-overlay', function () {
        $('.gridster .gridster-item.selected').removeClass('selected');
        $('.settings-overlay').removeClass('drag-box');
        $(this).parents('.gridster-item').addClass('selected');
        $(this).addClass('drag-box');
    });


    $('#add-module').click(function () {
        window.location.href = '/add-module/on-page/' + PAGE
    });

    /**
     * Toggle settings display
     */
    $('#settings-button').click(function () {
        $('.global-settings').toggleClass('showing');
    });

});

// ///////////////////////////////////////////////
// ///////////Functions

/**
 * Get the available sizes for a module
 */
function getSizes() {

    var moduleId = $('#module-modal').attr('data-id');
    var sizes = $('#module-modal').find('.module-sizes');


    console.log('Getting available sizes for module ' + moduleId);

    $.getJSON('/module/' + moduleId + '/availableSizes', function (json) {
        console.log(json);

        var html = [];
        $.each(json, function (index, value) {
            var width = value.split('x')[0];

            //We hide the sizes bigger than current layout
            if (width <= LAYOUT.maxGridWidth && value != "full-screen") {
                html.push('<p class="resize-size" data-size="', value, '"><a>', value, '</a></p>');
            }
        });
        sizes.siblings('.resize-size').remove();
        sizes.after(html.join(''));
    });
}

/**
 * Changes the size of a module
 */
function changeSize(moduleId, size) {

    var gridsterElem = $('.gridster .gridster-item[data-module="' + moduleId + '"]');


    var moduleElem = gridsterElem.find('.module');

    console.log(size);
    size = size.split('x');
    var width = size[0];
    var height = size[1];
    moduleElem.attr('data-size', width + 'x' + height);

    gridster.resize_widget(gridsterElem, width, height, function () {
        savePositions();
        getModuleContent(moduleElem.attr('data-module'), moduleElem.attr('data-size'));
        $('#module-modal').modal('hide');
    });
}


/**
 * Get html of the modules
 */
function getModulesContent() {
    $('#layout .module').each(function () {
        var module = $(this);
        getModuleContent(module.attr('data-module'), module.attr('data-size'))
    });
}

function getModuleContent(moduleId, size) {
    var module = $('#layout .module[data-module="' + moduleId + '"]');
    module.find('.loading').show();

    $.get('/module-content/' + moduleId + '/' + size, function (html) {
        module.find('.content').html(html);
        module.find('.loading').fadeOut("slow");
        sendMessage(moduleId, 'refresh', size);

        module.find('.content').removeClass(function (index, css) {
            return (css.match(/(^|\s)size-\S+/g) || []).join(' ');
        });
        module.find('.content').addClass('size-' + size);
    });
}

/**
 * Save the positions of the elements on the grid for this particular layout
 */
function savePositions(event, ui) {
    var html = [];
    $('#layout .gridster-item').each(function () {
        var module = $(this);

        var moduleStr = [];
        moduleStr.push(module.attr('data-module'));
        moduleStr.push(module.attr('data-col'));
        moduleStr.push(module.attr('data-row'));
        moduleStr.push(module.attr('data-sizex') + 'x' + module.attr('data-sizey'));

        html.push(moduleStr.join(','));
    });

    var data = {
        data: html.join('-')
    };

    $.post('/save-module-positions/' + LAYOUT.id, data, function (result) {
        console.log(result);
    });

}

/**
 * Initiate the grid
 */
function initGridster() {
    gridster = $(".gridster").gridster({
        widget_selector: '.gridster-item',
        widget_margins: [1, 1],
        widget_base_dimensions: [100, 100],
        min_cols: LAYOUT.maxGridWidth,
        max_cols: LAYOUT.maxGridWidth,
        draggable: {
            stop: savePositions,
            handle: '.settings-overlay.drag-box'
        }
    }).data('gridster');
    gridster.recalculate_faux_grid();
}

/**
 * Deletes a module
 */
function deleteModule(moduleId) {
    if (confirm('Delete this module ?')) {
        $.ajax({
            url: '/module/' + moduleId,
            type: 'DELETE',
            success: function (result) {
                getLayout();
                $('#module-modal').modal('hide');
            }
        });
    }
}


function toggleLayoutEditMode() {
    $('#layout, #layout-edit-mode').toggleClass('layout-edit');
    $('.global-settings').removeClass('showing');
}

function rootElement(moduleId) {
    return $('#layout .module[data-module="' + moduleId + '"] .content');
};

/**
 * Gets the current layout
 */
function getLayout() {
    $('#layout-wrapper .loading').fadeIn('fast');
    WIDTH = Math.max(document.documentElement.clientWidth, window.innerWidth || 0);

    // getting th layout for the page and view port
    $.getJSON('/modules-layout/' + PAGE + '/' + WIDTH, function (json) {
            $('#layout').html(json.html);
            updateLayoutInfo(json.layout);

            $('.gridster-item').each(function (index, module) {
                var module = MODULES[$(this).attr('data-module')];
                if (module != undefined && module.documentReady != undefined) {
                    module.documentReady();
                }
            });


        }
    ).fail(function () {
        PAGE = 1;

        if (typeof(Storage) !== "undefined") {
            localStorage.setItem("page", PAGE);
        }

        setTimeout(getLayout, 500);
    });
}

/**
 * Get the layout information so we can use it to initate the grid
 */
function updateLayoutInfo(layoutJson) {

    LAYOUT = layoutJson;
    console.log(LAYOUT);
    initGridster();
    // Starting the websocket or just refreshing the layout
    if (ws === undefined) {
        initWebsocket();
    } else {
        sendMessage(-1, "changeLayout", LAYOUT.id);
    }

    $('#layout-wrapper .loading').fadeOut('fast');
    $('#page-title .layout').html(' (' + LAYOUT.name + ')');

}