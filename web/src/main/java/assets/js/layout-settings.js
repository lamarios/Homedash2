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

function deleteLayout(layoutElement) {
    var id = layoutElement.attr('data-id');

    if (confirm("Delete Layout ?")) {
        $.getJSON('/layout/' + id + '/delete', function () {
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
        $.post('/layout/' + id + '/rename', {name: name}, function () {
            layoutElement.find('.name').html(name);
        }, "json");
    }
}

/**
 * Prefill layouts based on the attribute value
 */
function generateLayout() {
    $('.layout-display').each(function () {

        var size = $(this).attr('data-max');

        $(this).html(generateSvg(size, 0));

        displayLayoutInfo($(this).parents('.layout'));
    });
}

/**
 * Will display the minimum screen width and grid length here
 */
function displayLayoutInfo(layoutElement) {
    var itemsLength = layoutElement.find('.layout-display').attr('data-max');
    layoutElement.find('.grid-width').html(itemsLength);
    layoutElement.find('.screen-width').html((itemsLength * 102) + 'px');

}

/**
 * Remove item
 * @param element
 */
function removeItem(element) {
    var layout = element.find('.layout-display');

    var currentSize = layout.attr('data-max');
    if (currentSize > 1) {
        layout.attr('data-max', --currentSize);

        layout.html(generateSvg(currentSize, 1));

        validateAndSave(element);
        displayLayoutInfo(element);
    }
}

/**
 * Generates the svg
 * @param size which size we want to draw
 * @param animate -1 to remove, 0 for default, 1 to add
 * @returns {string}
 */
function generateSvg(size, animate) {

    var svgViewBox = size * 105;


    var svg = '<svg viewBox="0 0 ' + svgViewBox + ' 210" xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" version="1.1"><g class="surfaces">';


    svg +='<defs>\n' +
        '    <linearGradient id="gradient" x1="0%" y1="0%" x2="0%" y2="100%">' +
        '      <stop offset="0%" stop-color="'+$($('hr')[0]).css('border-color')+'" />' +
        '      <stop offset="50%" stop-color="#FFFFFF" />' +
        '    </linearGradient>' +
        '    <linearGradient id="gradient-invalid" x1="0%" y1="0%" x2="0%" y2="100%">' +
        '      <stop offset="0%" stop-color="red" />' +
        '      <stop offset="50%" stop-color="#FFFFFF" />' +
        '    </linearGradient>' +
        '  </defs>';

    for (var i = 0; i < size; i++) {
        var animation = '';
        //we add the animation class
        if (animate === 0) {
            //we animate all
            animation = "pop-up"
        } else if (animate === 1 && i === size - 1) {
            //we only animate the last one otherwise
            animation = "pop-up";
        }

        svg += '<rect class="' + animation + '" x="' + (i * 105) + '" y="0"width="100" height="100" />';
        svg += '<rect class="' + animation + ' bottom" x="' + (i * 105) + '" y="105"width="100" height="100" />';

    }

    svg += '</g></svg>';

    return svg;
}

/**
 * Add an item
 * @param element
 */
function addItem(element) {
    var layout = element.find('.layout-display');

    var currentSize = layout.attr('data-max');
    layout.attr('data-max', ++currentSize);

    layout.html(generateSvg(currentSize, 1));

    validateAndSave(element);
    displayLayoutInfo(element);
}

/**
 * We don't want 2 layout with the same width
 * @param element
 */
function validateAndSave(element) {


    var size = element.find('.layout-display').attr('data-max');

    var valid = true;
    console.log('classes', element.attr('class'));
    console.log('size', size);

    $('.layout').each(function () {
        var e = $(this);
        var layout = e.find('.layout-display');


        if (e.attr('data-id') != element.attr('data-id')) {
            if (layout.attr('data-max') == size) {
                valid = false;
            }
        }
    });

    if (valid) {
        element.removeClass('invalid');
        $.get('/layout/' + element.attr('data-id') + '/set-size/' + size);
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