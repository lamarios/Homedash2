$(document).ready(function () {

    $('#browse-remote').click(function () {
        var url = $('#remote-url').val();
        var key = $('#remote-key').val();

        var moduleDiv = $('#remote-modules');

        moduleDiv.html('<div class="loader"></div>');


        $.post('/remote/browse-remote', {url: url, key: key}, function (remote) {
            $('#remote-name').html(remote.name);

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
                location.href ="/";
            }
        }, 'json');


    });

    function module2html(module) {
        var html = [];
        html.push('<h3>', module.name, '</h3>');
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