function dockercompose(moduleId) {
    this.moduleId = moduleId;

    this.documentReady = function (size) {
        var self = this;

        if (size === 'full-screen') {
            var root = rootElement(self.moduleId);
            sendMessage(this.moduleId, 'compose-file', '');
            root.on('click', '.compose-actions .edit', function () {
                root.find('.editable').toggleClass('active');
            });

            root.on('click', '.compose-actions .refresh', function () {
                sendMessage(self.moduleId, 'compose-file', '');
            });

            root.on('click', '.save', function () {
                var content = root.find('textarea.compose').val();
                sendMessage(self.moduleId, 'save-compose', content);
            });


            root.on('click', '.actions .cmd', function () {
                var cmd = $(this).attr('data-cmd');
                sendMessage(self.moduleId, 'cmd', cmd);
                $('.modal .modal-body').html('Loading...');
                $('.modal .modal-title').html('docker-compose ' + cmd);
                $('.modal').modal('show');

            });

            root.on('click', '.actions .custom', function () {
                var cmd = prompt('Enter command to run.\nNo need to prepend by docker-compose.\nAvoid command that hog the output (i.e. \'up\', prefer \'up -d\'');
                sendMessage(self.moduleId, 'cmd', cmd);
                $('.modal .modal-body').html('Loading...');
                $('.modal .modal-title').html('docker-compose ' + cmd);
                $('.modal').modal('show');

            });

        }
    };

    this.onConnect = function () {

    };

    this.onMessage = function (size, command, message, extra) {
        switch (size) {
            case '1x1':
            case '2x1':
                this.onMessage_1x1(command, message, extra);
                break;
            case 'full-screen':
                this.onMessage_fullScreen(command, message, extra);
                break;
        }
    }


    this.onMessage_1x1 = function (command, message, extra) {
        var root = rootElement(this.moduleId);

        root.find('.count').html(message.count);
        root.find('.folder').html(message.folder);
    };


    this.onMessage_fullScreen = function (command, message, extra) {
        var root = rootElement(this.moduleId);
        if (command === 'refresh') {
            root.find('.containers table tbody').html(this.containersToHtml(message));
        } else if (command === 'compose-file') {
            root.find('.compose').html(message);
        } else if (command === 'success') {
            sendMessage(this.moduleId, 'compose-file', '');
        } else if (command === 'cmd') {
            if (message.output.length > 0) {
                $('.modal .modal-body').html(message.output.join('<br />'));
            } else {
                $('.modal .modal-body').html(message.errorOutput.join('<br />'));
            }
        }


    };


    this.containersToHtml = function (containers) {
        var html = [];

        $.each(containers, function (index, value) {
            html.push('<tr>');
            html.push('<td>', value.name, '</td>');
            html.push('<td>', value.command, '</td>');
            html.push('<td>', value.state, '</td>');
            html.push('<td>', value.ports, '</td>');
            html.push('</tr>');
        });

        return html.join('');
    }


}