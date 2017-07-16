function dynamicdns(moduleId) {
    this.moduleId = moduleId;

    this.onConnect = function () {
    };

    this.documentReady = function (size) {
        var parent = this;
        var root = this.root();

        root.find('.modal').attr('data-module', this.moduleId);

        var modal = this.modal();

        this.modal().on('change', '.provider-type', function () {
            parent.getFields($(this).val());
        });

        root.find('.add-provider').click(function () {
            modal.find('.provider-type').val(-1);
            modal.find('.provider-type').change();
            modal.modal('show');
        });

        modal.on('submit', '.provider-form', this.submitProvider);

        modal.on('click', '.submit', function () {
            parent.submitProvider();
        });

        root.on('click', '.delete-provider', function () {
            if (confirm('Delete provider ?')) {
                sendMessage(parent.moduleId, 'deleteProvider', $(this).attr('data'));
            }
        });

        root.on('click', '.refresh', function () {
            sendMessage(parent.moduleId, 'forceRefresh', '');
        });
    };

    this.root = function () {
        return rootElement(this.moduleId);
    };

    /**
     * Help function to get the modal of the html
     * @returns {*|jQuery}
     */
    this.modal = function () {
        var root = this.root();
        if (root.find('.modal') != undefined) {
            root.find('.modal').attr('data-module', this.moduleId);
            root.find('.modal').appendTo(".modal-dump");
        }

        return $(document).find('.modal[data-module="' + this.moduleId + '"]');
    };


    this.onMessage = function (message, command, message, extra) {
        switch (size) {
            case '2x1':
                this.onMessage_2x1(command, message, extra);
                break;
            case '4x3':
                this.onMessage_4x3(command, message, extra);
                break;
            case 'full-screen':
                this.onMessage_fullScreen(command, message, extra);
                break;
        }
    }

    this.onMessage_2x1 = function (command, message, extra) {
        this.processMessage(command, message, extra);
    };

    this.onMessage_4x3 = function (command, message, extra) {
        this.processMessage(command, message, extra);
    };

    this.onMessage_fullScreen = function (command, message, extra) {
        this.processMessage(command, message, extra);
    };

    this.processMessage = function (command, message, extra) {
        switch (command) {
            case 'refresh':
                this.processData(message);
                break;
            case 'getFields':
                this.processFields(message);
                break;
            case 'success':
            case 'error':
                this.processProviders(extra);
                break;
        }
    };

    this.processData = function (json) {
        var root = this.root();
        root.find('.address').html(json.ip.address);
        root.find('.update-date').html('Last update:' + json.ip.date + ' - via ' + json.ip.method);
        this.processProviders(json.providers);
    };

    /**
     * Display the providers in the table
     * @param providers
     */
    this.processProviders = function (providers) {
        var body = this.root().find('.providers tbody');
        body.html('');
        var parent = this;

        if (providers.length > 0) {

            $.each(providers, function (index, provider) {
                body.append(parent.provider2html(provider.data));
            });
        } else {
            body.append('<tr><td colspan="3">No providers available.</td></tr>');
        }
    };

    /**
     * Generates the HTML for each field of the provider
     * @param json
     */
    this.processFields = function (json) {
        console.log(json);

        var html = [];

        var parent = this;
        $.each(json, function (index, input) {
            switch (input.type) {
                case 0:
                    html.push(parent.inputText2html(input));
                    break;
                case 1:
                    html.push(parent.inputPassword2html(input));
                    break;
            }
        });

        this.modal().find('.form-fields').html(html.join(''));
    };

    this.inputPassword2html = function (input) {
        var html = [];
        html.push('<div class="form-group">');
        html.push('<label for="ddns-', this.moduleId, '-input-', input.name, '">', input.label,
            '</label>');
        html.push('<input type="password" class="form-control" "ddns-', this.moduleId, '-input-',
            input.name, '" name="', input.name, '">');
        html.push('</div>');

        return html.join('');
    };

    this.inputText2html = function (input) {
        var html = [];
        html.push('<div class="form-group">');
        html.push('<label for="ddns-', this.moduleId, '-input-', input.name, '">', input.label,
            '</label>');
        html.push('<input type="text" class="form-control""ddns-', this.moduleId, '-input-',
            input.name, '" name="', input.name, '">');
        html.push('</div>');
        return html.join('');
    };

    this.getFields = function (provider) {
        if (provider != -1) {
            sendMessage(this.moduleId, 'getFields', provider);
        } else {
            this.modal().find('.form-fields').html('');
        }
    };

    this.submitProvider = function () {
        var data = this.modal().find('.provider-form').serializeArray();
        sendMessage(this.moduleId, 'addProvider', data);
        this.modal().modal('hide');
        return false;
    };

    this.provider2html = function (provider) {
        var html = [];
        html.push('<tr>');
        html.push('<td>', provider.name, '</td>');
        html.push('<td>', provider.hostname, '</td>');
        html.push('<td>');
        //html.push('<a class="btn btn-sm btn-primary edit-provider" data="', provider.id,
        // '">edit</a>');
        html.push('<a class="btn btn-sm btn-danger delete-provider" data="', provider.id,
            '">delete</a>');
        html.push('</td>');

        html.push('</tr>');

        return html.join('');
    }

}