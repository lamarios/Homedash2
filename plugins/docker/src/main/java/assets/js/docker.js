function docker(moduleId) {
    this.moduleId = moduleId;
    this.selectedContainer;

    this.documentReady = function (size) {
        var root = rootElement(this.moduleId);
        var self = this;
        if (size == 'full-screen') {

            root.on('click', '.container-modal', function (event) {
                var modal = root.find('.modal');
                self.selectedContainer = $(this).attr('data-id');
                var status = root.find(
                    '.containers tr[data-id="' + self.selectedContainer + '"] .status').html();

                modal.find('.modal-header h4').html($(this).attr('data-name'));

                modal.find('.action').hide();
                if (status.startsWith('Up')) {
                    modal.find('.action.running').show();
                } else {
                    modal.find('.action.not-running').show();
                }

                modal.find('.container-info').html('<div class="loader"></div>');
                sendMessage(self.moduleId, 'details', self.selectedContainer);

                modal.modal('show');
            });

            root.on('click', '.action', function (event) {
                if (self.selectedContainer !== undefined) {
                    sendMessage(self.moduleId, $(this).attr('data-action'), self.selectedContainer);
                    root.find('.modal').modal('hide');
                }
            });

        }
    };

    this.onConnect = function () {

    };

    this.onMessage_1x1 = function (command, message, extra) {
        var root = rootElement(this.moduleId);

        root.find('.count').html(message);
    };

    this.onMessage_fullScreen = function (command, message, extra) {
        if (command === 'refresh') {
            this.refreshContainers(message);
        } else if (command === 'success') {
            this.refreshContainers(extra);
        } else if (command === 'details') {
            this.showContainerDetails(message);
        }
    };

    this.showContainerDetails = function (data) {
        var root = rootElement(this.moduleId);

        var html = [];

        if (data.config.image.length > 0) {
            html.push(this.infoPanel('Image', data.config.image));
        }

        if (data.hostConfig.restartPolicy.name !== undefined) {
            html.push(this.infoPanel('Restart policy', data.hostConfig.restartPolicy.name));
        }

        if(data.networkSettings.networks !== undefined){
            var network = [];
            $.each(data.networkSettings.networks, function(index, value){
                network.push('<h4>', index,'</h4>');

                if(value.aliases !== undefined){
                    network.push('<p><strong>Aliases: </strong>',value.aliases.join(', '),'</p>');
                }

                if(value.gateway.length > 0){
                    network.push('<p><strong>Gateway: </strong>',value.gateway,'</p>');
                }

                if(value.ipAddress.length > 0){
                    network.push('<p><strong>IP Address: </strong>',value.ipAddress,'</p>');
                }

                if(value.macAddress.length > 0){
                    network.push('<p><strong>MAC Adress: </strong>',value.macAddress,'</p>');
                }
                network.push('<p>','</p>');
                network.push('<hr />')
            });

            html.push(this.infoPanel('Networks', network.join('')));
        }

        if (data.networkSettings.ports !== undefined) {
            var network = [];
            $.each(data.networkSettings.ports, function (index, value) {
                if (value.length > 0) {
                    console.log('Port:', value);
                    network.push('<p>');
                    network.push(value[0].hostPort,
                                 ' <i class="fa fa-arrow-right" aria-hidden="true"></i> ', index);
                    network.push('</p>');
                }
            });

            if(network.length>0) {
                html.push(this.infoPanel('Ports', network.join('')));
            }
        }


        var mounts = [];
        $.each(data.mounts, function (index, value) {
            mounts.push('<p>');
            mounts.push(value.source, ' <i class="fa fa-arrow-right" aria-hidden="true"></i> ',
                        value.destination, ':', value.mode);
            mounts.push('</p>');
        });

        html.push(this.infoPanel("Mounts", mounts.join('')));

        root.find('.modal .container-info').html(html.join(''));
    };

    this.infoPanel = function (title, content) {
        var html = [];

        //
        html.push('<div class="panel panel-default">');
        html.push('<div class="panel-heading">', title, '</div>');
        html.push('<div class="panel-body">');
        html.push(content);
        html.push('</div>');
        html.push('</div>');

        return html.join('');
    };

    this.refreshContainers = function (message) {

        var root = rootElement(this.moduleId);

        var html = [];
        $.each(message, function (index, container) {
            html.push('<tr data-id="', container.id, '">');

            html.push('<td>', container.id, '</td>');
            html.push('<td>', container.names.join(','), '</td>');
            html.push('<td class="status">', container.status, '</td>');
            html.push('<td>', container.memoryUsagePretty, '</td>');
            html.push('<td class="container-modal" data-id="', container.id, '", data-name="',
                      container.names.join(','), '">',
                      '<i class="fa fa-ellipsis-v" aria-hidden="true"></i>',
                      '</td>');

            html.push('</tr>');
        });

        root.find('.containers tbody').html(html.join(''));
    }

}