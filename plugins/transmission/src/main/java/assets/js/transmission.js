function transmission(moduleId) {

    this.moduleId = moduleId;

    this.width = 1;


    this.onConnect = function () {

    };

    this.documentReady = function () {

        var parent = this;

        rootElement(this.moduleId).on('click', '.btn.add', function () {
            parent.addTorrent();
        });
        rootElement(this.moduleId).on('click', '.btn.alt', function () {
            parent.setAltSpeed();
        });

    };

    this.onMessage_3x2 = function (command, message, extra) {
        this.width = 3;
        this.processData(message);
    };

    this.onMessage_2x2 = function (command, message, extra) {
        this.width = 2;
        this.processData(message);
    };

    this.onMessage_2x1 = function (command, message, extra) {
        this.width = 2;
        this.processData(message);
    };


    this.processData = function (json) {
        var root =  rootElement(this.moduleId);

        if (!json) {
            root.find('.module-overlay').show();
        } else {
            root.find('.count').html(json.status.obj.map.torrentCount);
            root.find('.dl').html(this.humanFileSize(json.status.obj.map.downloadSpeed, true)+'/s');
            root.find('.ul').html(this.humanFileSize(json.status.obj.map.uploadSpeed, true)+'/s');

            var button =  root.find('.alt')
            if (json.alternateSpeeds) {
                button.addClass("btn-primary");
                button.html('<span class="glyphicon glyphicon-sort-by-attributes-alt" aria-hidden="true"></span>');
            } else {
                button.removeClass("btn-primary");
                button.html('<span class="glyphicon glyphicon-sort-by-attributes" aria-hidden="true"></span>');
            }

            root.find('.module-overlay').hide();
        }
    }


    this.addTorrent = function (event) {
        var url = prompt("Margnet link", '');
        sendMessage(this.moduleId, 'addTorrent', url);
    }

    this.setAltSpeed = function () {

        var button = rootElement(this.moduleId).find('.btn.alt');
        var setAltSpeed = true;
        if (button.hasClass("btn-primary")) {
            setAltSpeed = false;
            button.removeClass("btn-primary");
            button.html('<span class="glyphicon glyphicon-sort-by-attributes" aria-hidden="true"></span>');
        } else {
            setAltSpeed = true;
            button.addClass("btn-primary");
            button.html('<span class="glyphicon glyphicon-sort-by-attributes-alt" aria-hidden="true"></span>');
        }

        sendMessage(this.moduleId, 'altSpeed', setAltSpeed);
    }



    this.humanFileSize = function (bytes, si) {
        var thresh = si ? 1000 : 1024;
        if (bytes < thresh)
            return bytes + ' B';
        var units = si ? ['kB', 'MB', 'GB', 'TB', 'PB', 'EB', 'ZB', 'YB'] : [
            'KiB', 'MiB', 'GiB', 'TiB', 'PiB', 'EiB', 'ZiB', 'YiB'];
        var u = -1;
        do {
            bytes /= thresh;
            ++u;
        } while (bytes >= thresh);
        return bytes.toFixed(1) + ' ' + units[u];
    }

}