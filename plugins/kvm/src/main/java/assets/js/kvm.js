function kvm(moduleId) {
    this.moduleId = moduleId;

    this.onConnect = function () {

    };

    this.documentReady = function (size) {
        if (size === 'full-screen') {
            var self = this;
            $(document).on('click', '.action', function () {
                console.log('Uoooo');
                self.sendAction($(this).attr('data-domain'), $(this).attr('data-action'));
            });
        }
    };


    this.onMessage = function (size, command, message, extra) {
        switch (size) {
            case '1x1':
                this.onMessage_1x1(command, message, extra);
                break;
            case 'full-screen':
                this.onMessage_fullScreen(command, message, extra);
                break;
        }
    }

    this.onMessage_1x1 = function (command, message, extra) {
        var root = rootElement(this.moduleId);
        root.find('.running').html(message);
    };

    this.onMessage_fullScreen = function (command, message, extra) {
        if (command === 'refresh') {
            this.refreshVMInfo(message);
        } else if (command === 'success') {
            this.refreshVMInfo(extra);

        }
    }

    this.refreshVMInfo = function (message) {
        var html = '';

        message.forEach(function (domain) {
            html += '<tr>';
            html += '<td>' + domain.name + '</td>';
            html += '<td>' + domain.status + '</td>';

            var actions = '';
            switch (domain.status) {
                case 'Paused':
                    actions += '<button class="action btn btn-sm btn-default" data-domain="' + domain.name + '" data-action="resume"><i class="fa fa-play"/></button>';
                    actions += '<button class="action btn btn-sm btn-default" data-domain="' + domain.name + '" data-action="force-off"><i class="fa fa-bolt"/> <i class="fa fa-stop"/></button> ';
                    break;
                case 'Running':
                    actions += '<button class="action btn btn-sm btn-default" data-domain="' + domain.name + '" data-action="pause"><i class="fa fa-pause"/></button> ';
                    actions += '<button class="action btn btn-sm btn-default" data-domain="' + domain.name + '" data-action="shutdown"><i class="fa fa-stop"/></button> ';
                    actions += '<button class="action btn btn-sm btn-default" data-domain="' + domain.name + '" data-action="reboot"><i class="fa fa-refresh"/></button> ';
                    actions += '<button class="action btn btn-sm btn-default" data-domain="' + domain.name + '" data-action="force-off"><i class="fa fa-bolt"/> <i class="fa fa-stop"/></button> ';
                    actions += '<button class="action btn btn-sm btn-default" data-domain="' + domain.name + '" data-action="force-reset"><i class="fa fa-bolt"/> <i class="fa fa-refresh"/></button> ';
                    break;
                default:
                    actions += '<button class="action btn btn-sm btn-default" data-domain="' + domain.name + '" data-action="start"><i class="fa fa-play"/></button>';
                    break;
            }

            html += '<td>' + actions + '</td>';


            html += '</tr>';
        });


        var root = rootElement(this.moduleId);
        root.find('#vms').html(html);

    }

    this.sendAction = function (domain, action) {
        console.log('send action !');
        sendMessage(this.moduleId, 'action', JSON.stringify({'domain': domain, 'action': action}));
    }

}
