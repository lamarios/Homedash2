$(document).ready(function () {
    getPages();

    $('#page-title, #pages .close-panel, #pages-overlay').click(function (event) {
        $('#pages-overlay, #pages').toggleClass('showing');
    });

    $('#pages .icons .edit').click(function (event) {
        $('#pages').toggleClass('editing');
        event.stopPropagation();

    });

    $('#pages .add').click(function (event) {
        addPage();
        event.stopPropagation();
    });


    $(document).on('click', '#pages .edit-icon.fa-pencil', function (event) {
        event.stopPropagation();

        var pageId = $(this).parents('li').attr('data-id');
        editPage(pageId);
    });

    $(document).on('click', '#pages .edit-icon.fa-times', function (event) {
        event.stopPropagation();

        var pageId = $(this).parents('li').attr('data-id');
        deletePage(pageId);
    });


    $(document).on('click', '.page-name', function () {
        var pageId = $(this).parents('li').attr('data-id');
        changePage(pageId);
    });

    $(document).on('click', '#module-modal p.move', function (event) {
        moveModule($('#module-modal').attr('data-id'));
    });

    $(document).on('click', '#page-move-modal .page', function () {
        var element = $(this);

        moveModuleToPage(element.attr('data-module'), element.attr('data-id'));
    });

});

/////////
///functions


/**
 * change to page
 */
function changePage(id) {
    PAGE = id;
    sendMessage(-1, 'changePage', PAGE);
    getLayout();
    getPages();
    $('#pages-overlay, #pages').removeClass('showing');

    //finding which page we are
    if (typeof(Storage) !== "undefined") {
        localStorage.setItem("page", PAGE);
    }
}


/**
 * Moves a module to a different page
 * @param moduleId
 */
function moveModule(moduleId) {
    $('#module-modal').modal('hide');

    if ($('#page-move-modal').length == 0) {
        var modal = [];
        modal.push('<div id="page-move-modal" class="modal fade" tabindex="-1" role="dialog">'
            , '<div class="modal-dialog">'
            , '<div class="modal-content">'
            , '<div class="modal-header">'
            , '<button type="button" class="close" data-dismiss="modal" aria-label="Close"><span aria-hidden="true">&times;</span></button>'
            , '<h4 class="modal-title">Which page to move this module ?</h4>'
            , '</div>'
            , '<div class="modal-body">'
            , '<div class="loader"></div>'
            , '</div>'
            , '</div>'
            , '</div>'
            , '</div>');

        $('body').append(modal.join(''));
    } else {
        $('#page-move-modal .modal-body').html('<div class="loader"></div>');

    }
    $('#page-move-modal').modal('show');

    //Getting the list of pages
    $.getJSON('/pages', function (pages) {
        p;
        var html = [];
        $.each(pages, function (index, page) {
            if (page.id != PAGE) {
                html.push('<div data-id="', page.id, '" data-module="', moduleId, '" class="page">', page.name, '</div>');
            }
        });

        $('#page-move-modal .modal-body').html(html.join(''));
    });

}

/**
 * Moves a module to a page
 * @param moduleId
 * @param pageId
 */
function moveModuleToPage(moduleId, pageId) {
    $.get('/module/' + moduleId + '/move-to-page/' + pageId, function () {
        getLayout();
        $('#page-move-modal').modal('hide');
        $('#edit-layout').removeClass('editing');
    });
}

/**
 * Get all the pages
 */
function getPages() {
    $.getJSON('/pages', function (pages) {
        pages2html(pages);
    });
}

/**
 * Builds the html list of the pages
 * @param pages
 */
function pages2html(pages) {
    var html = [];
    $.each(pages, function (index, page) {
        if (page.id == PAGE) {
            $('#page-title .name').html(page.name);
        }
        html.push('<li data-id="', page.id, '"><a class="page-name">', page.name, '</a> <i class="fa fa-pencil edit-icon" aria-hidden="true"></i>');
        if (page.id > 1) {
            html.push('<i class="fa fa-times edit-icon" aria-hidden="true"></i>');
        }
        html.push('</li>');
    });

    $('#pages ul').html(html.join(''));
}

/**
 * Adds a page
 * @param name
 */
function addPage(name) {
    var name = prompt('New page name');
    if (name != undefined && $.trim(name).length > 0) {
        $.post('/pages/add', {name: name}, function (pages) {
            pages2html(pages);
        }, 'json');
    }
}


/**
 * Edits a page
 * @param name
 */
function editPage(id) {
    var name = prompt('New name', $('#pages ul li[data-id="' + id + '"] .page-name').html());
    if (name != undefined && $.trim(name).length > 0) {
        $.post('/pages/edit/' + id, {name: name}, function (pages) {
            pages2html(pages);
        }, 'json');
    }
}

/**
 * Edits a page
 * @param name
 */
function deletePage(id) {

    if (confirm("Deleting this page will also delete all the modules on it. Continue ?")) {
        $.ajax({
            url: '/page/' + id,
            type: 'DELETE',
            success: function (pages) {
                pages2html(pages);
            },
            dataType: 'json',
        });
    }
}
