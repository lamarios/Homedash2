function docker(moduleId) {
    this.moduleId = moduleId;
    this.selectedContainer;
    this.selectedImage;

    this.documentReady = function (size) {
        var root = rootElement(this.moduleId);
        var self = this;
        if (size == 'full-screen') {

            root.on('click', '.container-modal', function () {
                self.showContainerModal($(this));
            });

            root.on('click', '.image-modal', function () {
                self.showImageModal($(this));
            });

            root.on('click', '.action', function (event) {
                if (self.selectedContainer !== undefined) {
                    sendMessage(self.moduleId, $(this).attr('data-action'), self.selectedContainer);
                    root.find('.modal').modal('hide');
                }
            });


            root.on('click', '.image-action', function (event) {
                if (self.selectedImage !== undefined) {
                    sendMessage(self.moduleId, $(this).attr('data-action'), self.selectedImage);
                    root.find('.modal').modal('hide');
                }
            });

        }
    };

    this.onConnect = function () {

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

        root.find('.count').html(message);
    };

    this.onMessage_fullScreen = function (command, message, extra) {
        if (command === 'refresh') {
            this.refreshContainers(message.containers);
            this.refreshImages(message.images);
        } else if (command === 'success') {
            this.refreshContainers(extra.containers);
            this.refreshImages(extra.images);
        } else if (command === 'details') {
            this.showContainerDetails(message);
        }
    };


    this.showImageModal = function (source) {
        var root = rootElement(this.moduleId);

        var modal = root.find('#image-modal');
        this.selectedImage = source.attr('data-id');
        modal.find('.modal-title').html(source.attr('data-name'));
        modal.find('.id').html(this.selectedImage);

        modal.modal('show');

    };


    this.showContainerModal = function (source) {
        var root = rootElement(this.moduleId);

        var modal = root.find('#container-modal');
        this.selectedContainer = source.attr('data-id');
        var status = root.find(
            '.containers tr[data-id="' + this.selectedContainer + '"] .status').html();

        modal.find('.modal-header h4').html(source.attr('data-name'));

        modal.find('.id').html(this.selectedContainer);

        modal.find('.action').hide();
        if (status.startsWith('Up')) {
            modal.find('.action.running').show();
        } else {
            modal.find('.action.not-running').show();
        }

        modal.find('.container-info').html('<div class="loader"></div>');
        sendMessage(this.moduleId, 'details', this.selectedContainer);

        modal.modal('show');
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

        if (data.networkSettings.networks !== undefined) {
            var network = [];
            $.each(data.networkSettings.networks, function (index, value) {
                network.push('<h4>', index, '</h4>');

                if (value.aliases !== undefined) {
                    network.push('<p><strong>Aliases: </strong>', value.aliases.join(', '), '</p>');
                }

                if (value.gateway.length > 0) {
                    network.push('<p><strong>Gateway: </strong>', value.gateway, '</p>');
                }

                if (value.ipAddress.length > 0) {
                    network.push('<p><strong>IP Address: </strong>', value.ipAddress, '</p>');
                }

                if (value.macAddress.length > 0) {
                    network.push('<p><strong>MAC Adress: </strong>', value.macAddress, '</p>');
                }
                network.push('<p>', '</p>');
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

            if (network.length > 0) {
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

        console.log('ENV', data.config.env)
        if (data.config.env !== undefined && data.config.env.length > 0) {
            var env = [];
            $.each(data.config.env, function (index, value) {
                var split = value.split('\u003d');
                env.push('<p><strong>', split[0], ': </strong>', split[1], '</p>');
            });

            html.push(this.infoPanel('Environment variables', env.join('')));
        }

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

    this.refreshImages = function (images) {

        var root = rootElement(this.moduleId);

        var html = [];
        $.each(images, function (index, image) {
            html.push('<tr data-id="', image.id, '">');

            html.push('<td>', image.tag, '</td>');
            html.push('<td>', image.usedBy, '</td>');
            html.push('<td>', image.size, '</td>');
            html.push('<td>', image.created, '</td>');
            html.push('<td class="image-modal" data-id="', image.id, '", data-name="',
                image.tag, '">',
                '<i class="fa fa-info-circle" aria-hidden="true"></i>',
                '</td>');

            html.push('</tr>');
        });

        root.find('.images tbody').html(html.join(''));
    }


    this.refreshContainers = function (message) {

        var root = rootElement(this.moduleId);

        var html = [];
        $.each(message, function (index, container) {
            html.push('<tr data-id="', container.id, '">');

            html.push('<td>', container.names.join(','), '</td>');
            html.push('<td>', container.image, '</td>');
            html.push('<td class="status">', container.status, '</td>');
            html.push('<td>', container.memoryUsagePretty, '</td>');
            html.push('<td>', container.bytesReceivedPretty, '</td>');
            html.push('<td>', container.bytesSentPretty, '</td>');
            html.push('<td class="container-modal" data-id="', container.id, '", data-name="',
                container.names.join(','), '">',
                '<i class="fa fa-info-circle" aria-hidden="true"></i>',
                '</td>');

            html.push('</tr>');
        });

        root.find('.containers tbody').html(html.join(''));
    }

}