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

        var parent = $(this).parent('.module');
        updateResizeButton(parent);
    });

    $('#add-module').click(function () {
        window.location.href = '/add-module/on-page/' + PAGE
    });

    /**
     * Toggle settings display
     */
    $('#settings-button').click(function () {
        $('#settings').toggleClass('showing');
    });

    /**
     * Toggle kiosk rotation for a module
     */
    $(document).on('click', '.toggle-kiosk', function (event) {
        var source = $(this);
        $.get('/kiosk/' + source.attr('data-moduleId') + '/toggleRotation', function (json) {
            var icon = source.find('a .fa');

            if (json == 'true') {
                icon.removeClass('fa-square-o');
                icon.addClass('fa-check-square-o');
            } else {
                icon.removeClass('fa-check-square-o');
                icon.addClass('fa-square-o');
            }
        });
    });

    $(document).on('click', '.module-resize-icon', function () {
        changeSize($(this).attr('data-id'), $(this).attr('data-size'));
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
        var hasKiosk = false;
        $.each(json, function (index, value) {
            var width = value.split('x')[0];


            //We hide the sizes bigger than current layout
            if (width <= LAYOUT.maxGridWidth && value != "full-screen" && value != 'kiosk') {
                html.push('<p class="resize-size" data-size="', value, '"><a>', value, '</a></p>');
            } else if (value === 'kiosk') {
                hasKiosk = true;
            }
        });

        html.push('<div class="kiosk">');
        if (hasKiosk) {
            html.push('<hr />');
            html.push('<p><a href="/kiosk/' + moduleId + '"><i class="fa fa-television" aria-hidden="true"></i> View in kiosk mode </a></p>');
            $.ajax({
                async: false,
                dataType: "json",
                url: '/kiosk/' + moduleId + '/inRotation',
                success: function (isKiosk) {

                    if (isKiosk) {
                        html.push('<p class="toggle-kiosk" data-moduleId="' + moduleId + '"><a><i class="fa fa-check-square-o"></i> <span>Show in kiosk rotation</span></a></p>');
                    } else {
                        html.push('<p class="toggle-kiosk" data-moduleId="' + moduleId + '"><a><i class="fa fa-square-o"></i> <span>Show in kiosk rotation</span></a></p>');
                    }
                }
            });

        } else {
            html.push('<hr />');
            html.push('<p>Plugin doest\'t support kiosk view</p>');
        }
        html.push('</div>');

        sizes.siblings('.resize-size').remove();
        sizes.siblings('.kiosk').remove();
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

    gridster.resize_widget(gridsterElem, width, height,  function () {
        savePositions();
        getModuleContent(moduleElem.attr('data-module'), moduleElem.attr('data-size'));
        $('#module-modal').modal('hide');

        updateResizeButton(moduleElem);
        gridster.init();
    });
}

function getModuleContent(moduleId, size) {
    var module = $('#layout .gridster-item[data-module="' + moduleId + '"]');
    var loadingOverlay = module.find('.module-loading');
    loadingOverlay.show();
    loadingOverlay.removeClass('fade');

    $.get('/module-content/' + moduleId + '/' + size, function (html) {
        module.find('.content').html(html);

        sendMessage(moduleId, 'refresh', size);

        var moduleObject = MODULES[moduleId];
        if (moduleObject != undefined && moduleObject.documentReady != undefined) {
            moduleObject.documentReady(size);
        }

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
        gridster.init();
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
    // gridster.recalculate_faux_grid();
    console.log('reloading gridster');


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
    $('#settings').removeClass('showing');
}

function rootElement(moduleId) {
    return $('#layout .module[data-module="' + moduleId + '"] .content');
}

/**
 * Gets the current layout
 */
function getLayout() {
    $('#layout-wrapper .loading').fadeIn('fast');
    WIDTH = Math.max(document.documentElement.clientWidth, window.innerWidth || 0);

    $('.modal-dump').html('');

    // getting th layout for the page and view port
    $.getJSON('/modules-layout/' + PAGE + '/' + WIDTH, function (json) {
            $('#layout').html(json.html);
            updateLayoutInfo(json.layout);

            $('.gridster-item').each(function (index, module) {
                var element = $(this);

                var module = MODULES[element.attr('data-module')];
                if (module != undefined && module.documentReady != undefined) {
                    module.documentReady(element.find('.module').attr('data-size'));
                }

                //Setting the loading background color to the same as the module background color if available.
                var cssColor = element.find('.content').css('background-color');
                element.find('.module-loading').css('background-color', cssColor);
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

    $('#page-title .layout').html(' (' + LAYOUT.name + ')');


}

/**
 * Will find the next size for an item based on the current position
 * and availabloe space
 * @param parent div of class .module
 */
function updateResizeButton(parent) {

    var availableWidth = LAYOUT.maxGridWidth - parent.parent('.gridster-item').attr('data-col') + 1;

    if (availableWidth > 0) {
        var payLoad = {
            currentSize: parent.attr('data-size'),
            moduleId: parent.attr('data-module'),
            availableWidth: availableWidth
        }

        var button = $('.module-resize-icon[data-id="' + payLoad.moduleId + '"]');

        button.addClass('disabled');

        $.post('/module/getNextAvailableSize', payLoad, function (nextSize) {
            button.removeClass('disabled');
            var split = nextSize.split('x');
            var newWidth = parseInt(split[0]);
            var newHeight = parseInt(split[1]);

            split = payLoad.currentSize.split('x');
            var currentWidth = parseInt(split[0]);
            var currentHeight = parseInt(split[1]);

            var widthDiff = (newWidth > currentWidth) ? 1 : (newWidth === currentWidth) ? 0 : -1;
            var heightDiff = (newHeight > currentHeight) ? 1 : (newHeight === currentHeight) ? 0 : -1;

            var angle = 0;

            //base chevron direction is right (so already 90 degrees set)
            if (widthDiff === 0 && heightDiff === 0) {
                button.removeClass('showing');
            } else {
                button.addClass('showing');
                if (widthDiff === 1) {
                    if (heightDiff === 1) {
                        angle = 45;
                    } else if (heightDiff === 0) {
                        angle = 0;
                    } else if (heightDiff === -1) {
                        angle = -45;
                    }
                } else if (widthDiff === 0) {
                    if (heightDiff === 1) {
                        angle = 90;
                    } else if (heightDiff === 0) {
                        angle = 0;
                    } else if (heightDiff === -1) {
                        angle = -90;
                    }

                } else if (widthDiff === -1) {
                    if (heightDiff === 1) {
                        angle = 135;
                    } else if (heightDiff === 0) {
                        angle = 180;
                    } else if (heightDiff === -1) {
                        angle = 225;
                    }

                }

            }
            button.css('transform', 'rotate(' + angle + 'deg)');
            button.attr('data-size', nextSize);
        });
        console.log(payLoad);

    }


}

