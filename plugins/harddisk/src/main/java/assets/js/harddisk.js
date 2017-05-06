function harddisk(moduleId) {

    this.moduleId = moduleId;

    this.width = 1;

    this.onConnect = function () {

    };
    this.path = ['.'];

    this.documentReady = function (size) {
        if (size === 'full-screen') {

            var self = this;
            sendMessage(this.moduleId, 'browse', this.path.join('/'));

            $('.files').on('click', 'tr.folder td:first-of-type', function () {
                var file = $(this).attr('data-name');
                if (file === '..' && self.path.length > 1) {
                    self.path.pop();
                } else {
                    self.path.push(file);
                }

                $('.files tbody').html('<tr><td>Loading...</td></tr>');

                sendMessage(self.moduleId, 'browse', self.path.join('/'));
            });
        }
    };

    this.onMessage_2x1 = function (command, message, extra) {
        this.width = 2;
        this.processData(message);
    };
    this.onMessage_1x1 = function (command, message, extra) {
        this.width = 1;
        this.processData(message);
    };

    this.onMessage_fullScreen = function (command, message, extra) {
        if (command === 'refresh') {

            var totalSpace = message.total;
            var usedSpace = message.used;
            var percentage = Math.ceil((usedSpace / totalSpace) * 100);

            $('.mount').html(message.path);
            $('.progress-bar').css('width', percentage + '%');
            $('.progress-bar').html(message.pretty);

        } else if (command === 'browse') {
            $('.current-path').html(this.path.join('/'));
            $('.files tbody').html(this.files2html(message));
        }

    }

    this.processData = function (diskSpace) {

        var root = rootElement(this.moduleId);

        root.find('.path').html(diskSpace.path);

        var totalSpace = diskSpace.total;
        var usedSpace = diskSpace.used;
        var percentage = Math.ceil((usedSpace / totalSpace) * 100);

        if (this.width === 2) {
            root.find('.data').html(diskSpace.pretty);
        }

        root.find('.hdd-container').html(this.generateSVG(percentage, diskSpace.usage));
    };

    this.generateSVG = function (percentage, usePercentage) {
        console.log('percentage', usePercentage);
        var html = [];
        var opacity = (0.5 + 0.5 * usePercentage);

        html.push(
            '<svg class="hdd-svg" version="1.1" xmlns:xlink="http://www.w3.org/1999/xlink" xmlns="http://www.w3.org/2000/svg"   viewBox="0 0 220 220">');
        html.push('<polygon points="10,160 110,210 110,110 10,60" />');
        html.push('<polygon points="110,210 210,160 210,60 110,110" />');
        html.push('<polygon points="10,60 110,110 210,60 110,10" />');

        html.push('<g opacity="', opacity, '">');
        html.push('<!-- keep:bottom left, bottom right| change: top right, top left-->');
        //html.push('<polygon points="10,160 110,210 110,200 10,160">');
        html.push('<polygon points="10,160 110,210 110,', 110 + (100 - percentage), ' 10,',
            60 + 100 - percentage, '">');
        //html.push('<animate attributeName="points" dur="1000ms" to="10,160 110,210
        // 110,',110+(100-percentage),' 10,',60+100-percentage,'" fill="freeze" />');
        html.push('</polygon>');
        html.push('<polygon points="110,210 210,160 210,', 60 + 100 - percentage, ' 110,',
            110 + 100 - percentage, '" >');
        //html.push('<polygon points="110,210 210,160 210,160 110,210" >');
        //html.push('<animate attributeName="points" dur="1000ms" to="110,210 210,160
        // 210,',60+100-percentage,' 110,',110+100-percentage,'" fill="freeze"/>');
        html.push('</polygon>');

        if (percentage == 100) {
            html.push('<polygon points="10,60 110,110 210,60 110,10" />');
        }

        html.push('</g>');

        html.push('</svg>');

        return html.join('');
    }


    this.files2html = function (files) {
        var html = [];

        if (this.path.length > 1) {
            html.push('<tr class="folder"><td colspan="2"  data-name="..">..</td></tr>');
        }

        $.each(files, function (index, value) {
            html.push('<tr ', value.folder ? 'class="folder"' : '', '>');


            var icon = '<i class="fa fa-file-o" aria-hidden="true"></i>';

            if (value.folder === true) {
                icon = '<i class="fa fa-folder-o" aria-hidden="true"></i>';
            }

            html.push('<td data-name="', value.name, '">', icon, '&nbsp;', value.name, '</td>');

            html.push('<td><div class="dropdown">');
            html.push('<button class="btn btn-primary btn-sm" id="dLabel" type="button" data-toggle="dropdown" aria-haspopup="true" aria-expanded="false">');
            html.push('<i class="fa fa-ellipsis-v" aria-hidden="true"></i>');
            html.push('</button>');
            html.push('<ul class="dropdown-menu" aria-labelledby="dLabel">');
            html.push('<li><a data-name="',value.name,'" class="add-clipboard">Add to clipboard</a></li>');
            html.push('<li><a data-name="',value.name,'" class="rename">Rename</a></li>');
            html.push('<li><a data-name="',value.name,'" class="delete">Delete</a></li>');
            html.push('</ul>');
            html.push('</div></td>')
            html.push('</tr>');
        });

        return html.join('');
    }

}