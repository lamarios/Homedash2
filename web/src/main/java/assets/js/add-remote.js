$(document).ready(function () {

    var currentRemote;

    $('#browse-remote').click(function () {
        var url = $('#remote-url').val();
        var key = $('#remote-key').val();

        var moduleDiv = $('#remote-modules');

        moduleDiv.html('<div class="loader"></div>');


        $.post('/remote/browse-remote', {url: url, key: key}, function (remote) {

            var fav = '';
            if (remote.favourite === true) {
                fav = '<i class="fa fa-star remove-favourite"  aria-hidden="true"></i>&nbsp;';
            } else {
                fav = '<i class="fa fa-star-o add-favourite"  aria-hidden="true"></i>&nbsp;';
            }


            currentRemote = {
                name: remote.name,
                apiKey: $('#remote-key').val(),
                url: $('#remote-url').val()
            };

            $('#remote-name').html(fav+remote.name);

            moduleDiv.html('');

            moduleDiv.attr('data-url', url);
            moduleDiv.attr('data-key', key);
            moduleDiv.attr('data-name', remote.name);

            $.each(remote.modules, function (index, value) {
                moduleDiv.append(module2html(value));
            });

        }, 'json');

    });

    $(document).on('click', '.add-button a', function () {


        var moduleDiv = $('#remote-modules');

        var url = moduleDiv.attr('data-url');
        var key = moduleDiv.attr('data-key');
        var name = moduleDiv.attr('data-name');
        var id = $(this).attr('data-id');
        var pluginClass = $(this).attr('data-class');

        $.post('/remote/add', {
            url: url,
            key: key,
            name: name,
            id: id,
            pluginClass: pluginClass
        }, function (result) {
            if (result) {
                location.href = "/";
            }
        }, 'json');


    });


    $(document).on('click', '.add-favourite', function () {
        var source = $(this);
        console.log('current remote', currentRemote);
        $.post('/remote/save', currentRemote, function (data) {

            source.removeClass('fa-star-o');
            source.addClass('fa-star');
            source.removeClass('add-favourite');
            source.addClass('remove-favourite');


            getAllFavourites();
        }, 'json');

    });

    $(document).on('click', '.remove-favourite', function () {
        console.log('current remote', currentRemote);
        var source = $(this);
        $.post('/remote/delete', currentRemote, function (data) {

            source.removeClass('fa-star');
            source.addClass('fa-star-o')
            source.removeClass('remove-favourite');
            source.addClass('add-favourite');

            getAllFavourites();

        }, 'json');

    });

    $(document).on('click', '.remotes li', function(){
        var source = $(this);

        $('#remote-url').val(source.attr('data-url'));
        $('#remote-key').val(source.attr('data-key'));

        $('#browse-remote').click();

    });


    getAllFavourites();

    function getAllFavourites(){
        $.getJSON('/remote/all', function(remotes){

            var html = [];
            html.push('<h2>Favourites</h2>');
            html.push('<ul class="remotes">');

            $.each(remotes, function(index, remote){

                html.push('<li data-url="'+remote.url+'" data-key="'+remote.apiKey+'">');

                html.push('<strong>'+remote.name+'</strong> - '+remote.url);


                html.push('</li>');

            });

            html.push('</ul>');

            $('#favourites').html(html.join(''));

        });
    }


    function module2html(module) {
        var html = [];


        html.push('<h3>')


        html.push(module.name);
        html.push('</h3>');
        html.push('<p>', module.description, '</h3>');


        $.each(module.settings, function (index, value) {
            html.push('<p><strong>', index, ':</strong> ', value, '</p>');
        });

        html.push('<div class="add-button">');
        html.push('<a class="btn btn-primary" data-class="', module.pluginClass, '" data-id="', module.id, '"> Add ', module.name, '</a>');
        html.push('</div>');
        html.push('<hr />');

        return html.join('');
    }

});