$(document).ready(function () {
    getPages();

    $('#page-title, #pages .close-panel').click(function () {
        $('#pages').toggleClass('showing');
    });

    $('#pages .icons .edit').click(function () {
        $('#pages').toggleClass('editing');
    });

    $('#pages .add').click(addPage);

    $(document).on('click', '#pages .edit-icon.glyphicon-pencil', function () {
        var pageId = $(this).parents('li').attr('data-id');
        editPage(pageId);
    });

    $(document).on('click', '#pages .edit-icon.glyphicon-remove', function () {
        var pageId = $(this).parents('li').attr('data-id');
        deletePage(pageId);
    });

    $(document).on('click', '#pages ul li .page-name', function () {
        var pageId = $(this).parents('li').attr('data-id');
        changePage(pageId);
    });

    $(document).on('click', '.gridster .module  li.move', function () {
        var element = $(this);

        moveModule(element.attr('data-id'));
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
    $('#pages').removeClass('showing');

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
        p
        var html = [];
        $.each(pages, function (index, page) {
            if (page.id != PAGE) {
                html.push('<div data-id="', page.id, '" data-module="',moduleId,'" class="page">', page.name, '</div>');
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
function moveModuleToPage(moduleId, pageId){
    $.get('/module/'+moduleId+'/move-to-page/'+pageId, function(){
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
        html.push('<li data-id="', page.id, '"><span class="page-name">', page.name, '</span> <span class="glyphicon glyphicon-pencil edit-icon" aria-hidden="true"></span>');
        if (page.id > 1) {
            html.push('<span class="glyphicon glyphicon-remove edit-icon" aria-hidden="true"></span>');
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
