function transmission(moduleId) {

    this.moduleId = moduleId;

    this.width = 1;

    this.torrents = [];

    this.categoryToggles = new Map();

    this.selection = [];

    this.onConnect = function () {

    };

    this.documentReady = function (size) {


        var parent = this;

        rootElement(this.moduleId).on('click', '.btn.add', function () {
            parent.addTorrent();
        });
        rootElement(this.moduleId).on('click', '.btn.alt', function () {
            parent.setAltSpeed();
        });

        if (size == "full-screen") {

            this.categoryToggles.set('', false);

            this.categoryToggles.set('Downloading', false);
            this.categoryToggles.set('Seeding', true);
            this.categoryToggles.set('Checking', true);
            this.categoryToggles.set('Paused', true);
            this.categoryToggles.set('Waiting', true);
            this.categoryToggles.set('Done', true);


            var root = rootElement(this.moduleId);


            root.on('click', '.pause-torrent', function () {
                parent.pauseTorrent([parent.selectedTorrent]);
                $(".modal").modal('hide');
            });

            root.on('click', '.remove-torrent', function () {
                parent.removeTorrent([parent.selectedTorrent]);
                $(".modal").modal('hide');
            });

            root.on('click', '.selection .pause-torrent-multiple', function () {
                parent.pauseTorrent(parent.selection);
                $(".modal").modal('hide');
            });

            root.on('click', '.selection .remove-torrent-multiple', function () {
                parent.removeTorrent(parent.selection);
                $(".modal").modal('hide');
                parent.selection = [];
                parent.updateSelection();
            });
            root.on('click', '.torrent', function (e) {
                $(".modal").modal('show');
                parent.selectedTorrent = $(this).attr('data');
            });

            root.on('click', '.category-toggle', function (e) {
                var status = $(this).attr('data-status');
                var current = parent.categoryToggles.get(status);
                parent.categoryToggles.set(status, !current);

                $('.torrent-category[data-status="' + status + '"]').toggleClass('hidden');

                var icon = $(this).find('.fa');

                icon.toggleClass('fa-caret-right');
                icon.toggleClass('fa-caret-down');
            });

            root.on('click', '.multiple-select-btn', function (event) {
                $(this).toggleClass('active');
                $('.torrent-list .torrent-row').toggleClass('multiple-select');

                parent.updateSelection();

            });

            root.on('click', '.torrent-row.multiple-select', function () {
                var source = $(this);
                var id = parseInt(source.attr('data-id'));

                var index = parent.selection.indexOf(id);
                if (index !== -1) {
                    parent.selection[index] = undefined;

                    //cleaning the array
                    parent.selection = $.grep(parent.selection, function (n) {
                        return n == 0 || n
                    });

                } else {
                    parent.selection.push(id);
                }
                console.log('Selection', parent.selection);
                source.find('.torrent-checkbox .fa').toggleClass('fa-square-o');
                source.find('.torrent-checkbox .fa').toggleClass('fa-check-square-o');

                parent.updateSelection();
            });
        }
    };

    this.onMessage = function (size, command, message, extra) {
        switch (size) {
            case 'kiosk':
                this.onMessage_3x2(command, message, extra);
                break;
            case 'full-screen':
                this.onMessage_fullScreen(command, message, extra);
                break;
            default:
                this['onMessage_' + size](command, message, extra);
                break;
        }
    }

    this.onMessage_3x2 = function (command, message, extra) {
        if (command == 'refresh') {
            this.processData(message);
        }
    };

    this.onMessage_2x2 = function (command, message, extra) {
        if (command == 'refresh') {
            this.processData(message);
        }
    };

    this.onMessage_2x1 = function (command, message, extra) {
        if (command == 'refresh') {
            this.processData(message);
        }
    };

    this.onMessage_fullScreen = function (command, message, extra) {
        if (command == 'refresh') {
            this.processData(message);
            this.displayTorrents(message);
        }
    };

    this.processData = function (json) {
        var root = rootElement(this.moduleId);

        if (!json) {
            root.find('.module-overlay').show();
        } else {
            root.find('.count').html(json.status.obj.map.torrentCount);
            root.find('.dl')
                .html(this.humanFileSize(json.status.obj.map.downloadSpeed, true) + '/s');
            root.find('.ul').html(this.humanFileSize(json.status.obj.map.uploadSpeed, true) + '/s');

            var button = root.find('.alt');
            if (json.alternateSpeeds) {
                button.addClass("btn-primary");
                button.html('<i class="fa fa-sort-amount-desc" aria-hidden="true"></i>');
            } else {
                button.removeClass("btn-primary");
                button.html('<i class="fa fa-sort-amount-asc" aria-hidden="true"></i>');
            }

            root.find('.module-overlay').hide();
        }
    };

    this.isMultiSelectOn = function () {
        return $('.multiple-select-btn').hasClass('active');
    }

    this.updateSelection = function () {
        $('.selection .torrent-count').html(this.selection.length);

        if (this.selection.length === 0 || !this.isMultiSelectOn()) {
            $('.selection').removeClass('active');
        } else {
            $('.selection').addClass('active');
        }
    }

    this.addTorrent = function (event) {
        var url = prompt("Margnet link", '');
        if (url != undefined) {
            if (url.length > 0) {
                sendMessage(this.moduleId, 'addTorrent', url);
            } else {
                showErrorMessage("Empty URL, can't add torrent");
            }
        }
    };

    this.setAltSpeed = function () {

        var button = rootElement(this.moduleId).find('.btn.alt');
        var setAltSpeed = true;
        if (button.hasClass("btn-primary")) {
            setAltSpeed = false;
            button.removeClass("btn-primary");
            button.html('<i class="fa fa-sort-amount-asc" aria-hidden="true"></i>');
        } else {
            setAltSpeed = true;
            button.addClass("btn-primary");
            button.html('<i class="fa fa-sort-amount-desc" aria-hidden="true"></i>');
        }

        sendMessage(this.moduleId, 'altSpeed', setAltSpeed);
    };

    this.humanFileSize = function (bytes, si) {
        var thresh = si ? 1000 : 1024;
        if (bytes < thresh) {
            return bytes + ' B';
        }
        var units = si ? ['kB', 'MB', 'GB', 'TB', 'PB', 'EB', 'ZB', 'YB'] : [
            'KiB', 'MiB', 'GiB', 'TiB', 'PiB', 'EiB', 'ZiB', 'YiB'];
        var u = -1;
        do {
            bytes /= thresh;
            ++u;
        } while (bytes >= thresh);
        return bytes.toFixed(1) + ' ' + units[u];
    };

//Full screen specific methods

    this.displayTorrents = function (json) {
        var parent = this;
        var div = rootElement(parent.moduleId).find('.torrent-list');
        div.html('');

        var sortedTorrents = new Map();
        $.each(json.torrents, function (index, torrent) {
            parent.torrents[torrent.id] = torrent;

            var statusName = parent.getStatusName(torrent.status, json.rpcVersion);

            if (!sortedTorrents.has(statusName)) {
                sortedTorrents.set(statusName, []);
            }

            sortedTorrents.get(statusName).push(torrent);

        });

        this.printTorrentCategory('Downloading', sortedTorrents, div, json.rpcVersion,
            this.categoryToggles.get('Downloading'));
        this.printTorrentCategory('Seeding', sortedTorrents, div, json.rpcVersion,
            this.categoryToggles.get('Seeding'));
        this.printTorrentCategory('Checking', sortedTorrents, div, json.rpcVersion,
            this.categoryToggles.get('Checking'));
        this.printTorrentCategory('Paused', sortedTorrents, div, json.rpcVersion,
            this.categoryToggles.get('Paused'));
        this.printTorrentCategory('Waiting', sortedTorrents, div, json.rpcVersion,
            this.categoryToggles.get('Waiting'));
        this.printTorrentCategory('Done', sortedTorrents, div, json.rpcVersion,
            this.categoryToggles.get('Done'));

    };

    this.printTorrentCategory = function (name, torrents, element, rpcVersion, hidden) {
        if (torrents.has(name)) {

            var parent = this;

            var html = [];

            html.push('<h2 class="category-toggle" data-status="', name, '">');

            var hiddenClass = '';
            if (hidden === true) {
                hiddenClass = 'hidden';
                html.push('<i class="fa fa-caret-right" aria-hidden="true"></i> ');
            } else {
                html.push('<i class="fa fa-caret-down" aria-hidden="true"></i> ');
            }

            html.push(name, '</h2>');
            html.push('<div class="torrent-category ', hiddenClass, '" data-status="', name, '">');
            $.each(torrents.get(name), function (index, value) {
                html.push(parent.torrentToHtml(value, rpcVersion));
            });
            html.push('</div>');

            element.append(html.join(''));
        }
    };

    this.torrentToHtml = function (torrent, rpcVersion) {
        var html = [];

        var multipleSelect = this.isMultiSelectOn() ? 'multiple-select' : '';
        var percent = Math.ceil(torrent.percentDone * 100);

        var selected = this.selection.indexOf(torrent.id) !== -1;
        var maxUpload = torrent.totalSize * torrent.seedRatioLimit;

        var percentUploaded = Math.ceil((torrent.uploaded / maxUpload) * 100);

        html.push('<div class="torrent-row ', multipleSelect, '" data-id="', torrent.id, '" >');
        html.push('<div class="torrent-checkbox">');
        if (selected) {
            html.push('<p><i class="fa fa-check-square-o"></i></p>');
        } else {
            html.push('<p><i class="fa fa-square-o"></i></p>');
        }
        html.push('</div>')//.torrent-checkbox
        html.push('<div  class="torrent-details" id="torrent-', torrent.id, '">');
        html.push('<p style="overflow: hidden; white-space: nowrap; text-overflow: ellipsis;">');
        html.push('<button  data="', torrent.id,
            '" class="torrent btn btn-primary btn-xs" style="float: right"><i class="fa fa-pencil"></i></button>');
        html.push(this.getStatusIcon(torrent.status, rpcVersion), ' ');
        html.push('<strong>', torrent.name, '</strong>');
        html.push('</p>');
        //downloading torrent
        if (percent < 100) {
            html.push('<div class="progress small-progress-bar">');
            html.push('<div class="progress-bar" role="progressbar" aria-valuenow="', percent,
                '" aria-valuemin="0" aria-valuemax="100" style="width: ', percent, '%;">');
        } else {
            //seeding torrent
            html.push('<div class="progress small-progress-bar seeding">');
            html.push('<div class="progress-bar" role="progressbar" aria-valuenow="', percentUploaded,
                '" aria-valuemin="0" aria-valuemax="100" style="width: ', percentUploaded, '%;">');

        }

        html.push('</div>');
        html.push('</div>');
        html.push('<p>');
        html.push('DL: ', this.humanFileSize(torrent.downloadSpeed, true), '/s | Ul: ',
            this.humanFileSize(torrent.uploadSpeed, true), '/s');
        html.push('<span style="float:right">');

        //downloading torrent
        if (percent < 100) {
            html.push(this.humanFileSize(torrent.downloaded, true), '/',
                this.humanFileSize(torrent.totalSize, true));
        } else {
            //torrent is now seeding
            html.push(this.humanFileSize(torrent.uploaded, true), '/',
                this.humanFileSize(maxUpload, true));
        }
        html.push('</span>');
        html.push('</p>');
        html.push('</div>'); //.torrent

        html.push('</div>') //.row
        html.push('<hr />');

        return html.join("");
    };

    this.getStatusIcon = function (value, rpcVersion) {
        if (rpcVersion < 14) {
            switch (value) {
                case 16:
                    return '<i class="fa fa-check-square-o"></i>';
                case 8:
                    return '<i class="fa fa-cloud-upload"></i>';
                case 4:
                    return '<i class="fa fa-cloud-download"></i>';
                case 2:
                    return '<i class="fa fa-refresh"></i>';
                case 4:
                    return '<i class="fa fa-clock-o"></i>';
                default:
                    return '';
            }
        } else {
            switch (value) {
                case 6:
                    return '<i class="fa fa-cloud-upload"></i>';
                case 5:
                    return '<i class="fa fa-clock-o"></i>';
                case 4:
                    return '<i class="fa fa-cloud-download"></i>';
                case 3:
                    return '<i class="fa fa-clock-o"></i>';
                case 2:
                    return '<i class="fa fa-refresh"></i>';
                case 1:
                    return '<i class="fa fa-clock-o"></i>';
                case 0:
                    return '<i class="fa fa-pause"></i>';
                default:
                    return '';
            }
        }

    };

    this.getStatusName = function (value, rpcVersion) {
        if (rpcVersion < 14) {
            switch (value) {
                case 16:
                    return 'Done';
                case 8:
                    return 'Seeding';
                case 4:
                    return 'Downloading';
                case 2:
                    return 'Waiting';
                case 4:
                    return 'Waiting';
                default:
                    return '';
            }
        } else {
            switch (value) {
                case 6:
                    return 'Seeding';
                case 5:
                    return 'Waiting';
                case 4:
                    return 'Downloading';
                case 3:
                    return 'Waiting';
                case 2:
                    return 'Refreshing';
                case 1:
                    return 'Waiting';
                case 0:
                    return 'Paused';
                default:
                    return '';
            }
        }

    };

    this.removeTorrent = function (ids) {
        if (confirm("Remove torrent ?")) {
            if (confirm("Delete downloaded data as well ?")) {
                sendMessage(this.moduleId, 'removeTorrentDelete', ids);
            } else {
                sendMessage(this.moduleId, 'removeTorrent', ids);
            }
        }
    };

    this.pauseTorrent = function (ids) {
        sendMessage(this.moduleId, 'pauseTorrent', ids);
    }

}