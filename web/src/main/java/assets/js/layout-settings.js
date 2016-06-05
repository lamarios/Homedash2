$(document).ready(function () {

    generateLayout();

    $(document).on('click', '.layout .remove', function () {
        var parent = $(this).parents('.layout');

        removeItem(parent);
    });

    $(document).on('click', '.layout .add', function () {
        var parent = $(this).parents('.layout');

        addItem(parent);
    });

    $(document).on('click', '.layout .rename', function () {
        var parent = $(this).parents('.layout');

        renameLayout(parent);
    });


    $(document).on('click', '.layout .delete', function () {
        var parent = $(this).parents('.layout');

        deleteLayout(parent);
    });

    $('#add-layout').click(addLayout);
});

function deleteLayout(layoutElement){
    var id = layoutElement.attr('data-id');

    if(confirm("Delete Layout ?")){
        $.getJSON('/layout/' + id + '/delete', function(){
            alert('Layout deleted');
            layoutElement.remove();
        });
    }
}


/**
 * Renamesa  layout
 * @param layoutElement
 */
function renameLayout(layoutElement) {
    var id = layoutElement.attr('data-id');

    var name = prompt('New name');

    if (name != undefined && $.trim(name).length > 0) {
        $.post('/layout/' + id + '/rename', {name: name}, function(){
            layoutElement.find('.name').html(name);
        }, "json");
    }
}

/**
 * Prefill layouts based on the attribute value
 */
function generateLayout() {
    $('.layout-display').each(function () {
        for (var i = 0; i < $(this).attr('data-max'); i++) {
            $(this).append('<div class="grid item"></div>');
        }


        displayLayoutInfo($(this).parents('.layout'));
        resizeItems($(this));
    });
}

/**
 * Will display the minimum screen width and grid length here
 */
function displayLayoutInfo(layoutElement) {
    var itemsLength = layoutElement.find('.grid.item').length;
    layoutElement.find('.grid-width').html(itemsLength);
    layoutElement.find('.screen-width').html((itemsLength * 102) + 'px');

}

/**
 * Remove item
 * @param element
 */
function removeItem(element) {
    if (element.find('.grid.item').length > 1) {
        element.find('.grid.item:last-of-type').remove();
        validateAndSave(element);
        resizeItems(element);
        displayLayoutInfo(element);
    }
}


/**
 * Add an item
 * @param element
 */
function addItem(element) {
    element.find('.grid.item:last-of-type').after('<div class="grid item"></div>');
    validateAndSave(element);
    resizeItems(element);
    displayLayoutInfo(element);
}

/**
 * Resize the grid items accordingly
 * @param parent
 */
function resizeItems(parent) {
    var elements = parent.find('.grid.item');
    var size = 100 / elements.length;
    elements.css('width', (size - 1) + '%');
    elements.css('height', $(elements[0]).width());
}

/**
 * We don't want 2 layout with the same width
 * @param element
 */
function validateAndSave(element) {
    var grid = element.find('.grid.item');

    console.log(grid);

    var valid = true;

    $('.layout').each(function () {
        var e = $(this);

        if (e.attr('data-id') != element.attr('data-id')) {
            if (e.find('.grid.item').length == grid.length) {
                valid = false;
            }
        }
    });
    if (valid) {

        element.removeClass('invalid');
        $.get('/layout/' + element.attr('data-id') + '/set-size/' + grid.length);
    } else {

        element.addClass('invalid');

    }
}


/**
 * Add a layout
 */
function addLayout() {
    var name = prompt('Layout name');

    if (name != undefined && $.trim('name')) {
        $.post('/layout-settings', {name: name}, function () {
            window.location.reload(true);
        })
    }
}