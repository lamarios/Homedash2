function portmapper(moduleId) {

    this.moduleId = moduleId;

    this.onConnect = function () {

    };

    this.width = 2;

    this.modal = function () {
        return $(document).find('.modal[data-module="' + this.moduleId + '"]');
    };

    this.documentReady = function (size) {

        var parent = this;
        var root = rootElement(this.moduleId);
        //$('#' + this.moduleId + '-overlay').html("No router found.");
        //$('#' + this.moduleId + '-overlay').show();
        root.find('.modal').attr('data-module', this.moduleId);
        root.find('.add').click(function () {
            parent.modal().appendTo(".modal-dump").modal('show');
        });

        root.find('form').submit(function () {
            parent.addPort();
            return false;
        });

        this.modal().find('.save').click(function () {
            parent.modal().find('form').submit();
        });

        root.on('click', '.remove-port', function () {
            parent.removePort($(this).attr('data'));
        });

        root.on('click', '.save-port', function () {
            parent.savePort($(this).attr('data'));
        });

    };


    this.onMessage = function(size, command, message, extra){
        switch(size){
            case 'full-screen':
                this.onMessage_fullScreen(command, message, extra);
                break;
            default:
                this['onMessage_'+size](command, message, extra);
                break;
        }
    }

    this.onMessage_5x5 = function (command, message, extra) {
        this.width = 5;
        this.processMessage(command, message);
    };

    this.onMessage_6x5 = function (command, message, extra) {
        this.width = 6;
        this.processMessage(command, message);
    };

    this.onMessage_fullScreen = function (command, message, extra) {
        this.width = 9001;
        this.processMessage(command, message);
    };

    this.onMessage_2x1 = function (command, message, extra) {
        this.width = 2;
        this.processMessage(command, message);
    };

    this.processMessage = function (command, message) {
        if (command == 'getRouter') {
            this.getRouter(message, true);
        } else if (command = 'refresh') {
            if (message.router != undefined) {
                this.getRouter(message.router, false);
            }
            this.getMappings(message.mappings);
        } else if (command = 'savePort') {
            showSuccessMessage('Port saved successfully');
            this.getMappings(message);
        } else if (command == 'getMappings') {
            this.getMappings(message);
        }

    };

    this.getRouter = function (router, getMappings) {
        var root = rootElement(this.moduleId);

        root.find('.router').html(router.name);
        root.find('.ip').html(router.externalIp);
        root.find('.overlay').hide();
        if (getMappings) {
            sendMessage(this.moduleId, 'getMappings', '');
        }
    };

    this.getMappings = function (mappings) {
        var root = rootElement(this.moduleId);

        if (mappings != undefined) {
            if (this.width > 2) {
                var table = root.find('.table tbody');
                table.html('');
                var parent = this;
                $.each(mappings, function (index, mapping) {
                    table.append(parent.port2Html(mapping));
                });
            } else {
                root.find('.port-count').html(mappings.length);
            }
        }
    };

    this.port2Html = function (mapping) {
        var html = [];
        if (mapping.forced) {
            html.push('<tr class="success">');
        } else {
            html.push('<tr>');
        }
        html.push('<td>', mapping.name, '</td>');
        html.push('<td>', mapping.protocol, '</td>');
        html.push('<td>', mapping.externalPort, '</td>');
        html.push('<td>', mapping.internalPort, '</td>');
        html.push('<td>', mapping.internalIp, '</td>');
        html.push('<td>');
        html.push('<button class="btn btn-danger btn-xs mapper remove-port" data="',
                  mapping.externalPort, '|', mapping.protocol,
                  '"><i class="fa fa-minus"></i></button>');

        if (!mapping.forced) {
            html.push('&nbsp;&nbsp;');
            html.push('<button class="btn btn-success btn-xs mapper save-port" data="',
                      mapping.externalPort, '|', mapping.protocol, '|', mapping.internalIp, '|',
                      mapping.name,
                      '"><i class="fa fa-plus"></i></button>');
        }

        html.push('</td>');
        html.push('</tr>');

        return html.join('');
    };

    this.addPort = function () {
        var root = rootElement(this.moduleId);

        var name = this.modal().find('form input[name="name"]').val();
        var protocol = this.modal().find('form select[name="protocol"]').val();
        var externalPort = this.modal().find('form input[name="externalPort"]').val();
        var internalPort = this.modal().find('form input[name="internalPort"]').val();
        var internalIp = this.modal().find('form input[name="client"]').val();

        var force = this.modal().find('form input[name="force"]:checked').length > 0;

        var data = [];
        data.push(name, '|', protocol, '|', externalPort, '|', internalPort, '|', internalIp);

        if (force) {
            sendMessage(this.moduleId, 'addPortForce', data.join(''));
        } else {
            sendMessage(this.moduleId, 'addPort', data.join(''));
        }

        this.modal().modal('hide');

    };

    this.removePort = function (data) {
        if (confirm('Delete this port ?')) {
            sendMessage(this.moduleId, 'removePort', data);
        }
    };

    this.savePort = function (data) {
        sendMessage(this.moduleId, 'savePort', data);
    }

}