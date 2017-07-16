function harddisk(moduleId) {

    this.moduleId = moduleId;

    this.width = 1;

    this.onConnect = function () {

    };
    this.path = ['.'];

    //The clip board will hold more than one file
    this.clipboard = [];

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


            $('.files').on('click', '.add-clipboard', function () {
                var path = self.path.join('/') + '/' + $(this).attr('data-name');


                if (!self.clipboard.includes(path)) {
                    var data = {
                        path: path,
                        inProgress: false,
                    }
                    self.clipboard.push(data);
                }
                self.clipBoardHtml();
            });


            $('.files').on('click', '.delete', function () {
                var path = self.path.join('/') + '/' + $(this).attr('data-name');
                if (confirm('Delete ' + path + '?')) {
                    sendMessage(self.moduleId, 'delete', path);
                }
            });

            $('.files').on('click', '.rename', function () {
                var newName = prompt('File new name');
                if (newName !== undefined && $.trim(newName) !== '') {
                    var data = {
                        source: self.path.join('/') + '/' + $(this).attr('data-name'),
                        destination: self.path.join('/') + '/' + newName
                    }

                    sendMessage(self.moduleId, 'rename', JSON.stringify(data));
                }
            });

            $('body').on('click', '.action-copy', function () {
                var path = $(this).attr('data-path');
                var data = {
                    'source': path,
                    'destination': self.path.join('/')
                };

                sendMessage(self.moduleId, 'copy', JSON.stringify(data));
                var index = self.indexOfClipBoard(path);
                self.clipboard[index].inProgress = true;

                self.clipBoardHtml();
            });

            $('body').on('click', '.action-move', function () {
                var path = $(this).attr('data-path');
                var data = {
                    'source': path,
                    'destination': self.path.join('/')
                };

                sendMessage(self.moduleId, 'move', JSON.stringify(data));

                var index = self.indexOfClipBoard(path);
                self.clipboard[index].inProgress = true;

                self.clipBoardHtml();
            });

            $('body').on('click', '#new-folder', function () {
                var newName = prompt('New folder name');
                if (newName !== undefined && $.trim(newName) !== '') {
                    var data = {
                        'source': self.path.join('/'),
                        'destination': newName
                    }

                    sendMessage(self.moduleId, 'newFolder', JSON.stringify(data));
                }
            });

            $('body').on('click', '#upload-file', function () {
                $('#file-input').click();
            });


            $('body').on('change', '#file-input', function () {
                self.sendFile();
            });

            $('body').on('click', '.clipboard .counter, .clipboard .content p', function (event) {

                $('.clipboard').toggleClass('expand');
                event.stopPropagation();

            });

            $('body').on('click', '.action-remove-from-clipboard', function () {
                self.removeFromClipboard($(this).attr('data-path'));
                self.clipBoardHtml();
                self.showClipboard();
            });
        }
    };

    this.onMessage = function (size, command, message, extra) {
        switch (size) {
            case '2x1':
            case 'kiosk':
                this.onMessage_2x1(command, message, extra);
                break;
            case '1x1':
                this.onMessage_1x1(command, message, extra);
                break;

            case 'full-screen':
                this.onMessage_fullScreen(command, message, extra);
                break;
        }
    }

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
        } else if (command === 'success' || command === 'error') {
            sendMessage(this.moduleId, 'browse', this.path.join('/'));


            if (extra !== undefined && extra.source !== undefined) {
                this.removeFromClipboard(extra.source);
                this.clipBoardHtml();
                this.showClipboard();

            }


            $('#upload-file').html('Upload file');
            $('#upload-file').removeAttr('disabled');
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

    this.clipBoardHtml = function () {

        var html = [];

        html.push('<p class="counter">');
        html.push(this.clipboard.length);
        html.push(' files in clipboard');
        html.push('<i class="fa fa-chevron-down"></i>');
        html.push('</p>');


        html.push('<div class="content">');
        html.push('<p><i class="fa fa-times"></i></p>');
        html.push('<div class="table-responsive">');
        html.push('<table class="table table-condensed"><tbody>');

        $.each(this.clipboard, function (index, clipboardItem) {
            var filename = clipboardItem.path.replace(/^.*[\\\/]/, '');

            html.push('<tr>');
            html.push('<td class="text">', filename, '</td>');
            if (!clipboardItem.inProgress) {
                html.push('<td class="actions">');
                html.push('<button data-path="', clipboardItem.path, '" class="btn btn-primary btn-sm action-copy" >Copy here</button>');
                html.push('<button data-path="', clipboardItem.path, '" class="btn btn-primary btn-sm action-move" >Move here</button>');
                html.push('<button data-path="', clipboardItem.path, '" class="btn btn-default btn-sm action-remove-from-clipboard"><i class="fa fa-times"></i></buton>');
                html.push('</td>');
            } else {
                html.push("<td>In progress</td>");
            }
            html.push('</tr>');

        });


        html.push('</tbody></table></div>');

        html.push('</div>') // content


        $('.clipboard').html(html.join(''));


        this.showClipboard();
    }

    this.showClipboard = function () {
        if (this.clipboard.length > 0) {
            $('.clipboard').addClass('active');
        } else {
            $('.clipboard').removeClass('active');
        }
    }

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
            html.push('<ul class="dropdown-menu pull-right" aria-labelledby="dLabel">');
            html.push('<li><a data-name="', value.name, '" class="add-clipboard">Add to clipboard</a></li>');
            html.push('<li><a data-name="', value.name, '" class="rename">Rename</a></li>');
            html.push('<li><a data-name="', value.name, '" class="delete">Delete</a></li>');
            html.push('</ul>');
            html.push('</div></td>')
            html.push('</tr>');
        });

        return html.join('');
    }
    /**
     * Sends file as base64 to the backend
     */
    this.sendFile = function () {
        var reader = new FileReader();
        var input = document.querySelector('#file-input');
        var fileName = input.value.split(/(\\|\/)/g).pop();

        var self = this;
        if (fileName !== undefined && $.trim(fileName) !== '') {

            $('#upload-file').html('Uploading...');
            $('#upload-file').attr('disabled', true);

            reader.readAsDataURL(input.files[0]);
            reader.onload = function () {
                //disabling the input + chaning text
                $('#file-input')

                var data = {
                    'source': self.path.join('/') + '/' + fileName,
                    'destination': reader.result
                };

                sendMessage(self.moduleId, 'uploadFile', JSON.stringify(data));

            };
            reader.onerror = function (error) {
                alert('Couldn\'t load file: ' + error);

                $('#upload-file').html('Upload file');
                $('#upload-file').removeAttr('disabled');
            };
        }
    }

    /**
     * Removes an item from the clipboard
     * @param filePath the full path of the file to remove
     */
    this.removeFromClipboard = function (filePath) {
        var found = this.indexOfClipBoard(filePath);

        while (found !== -1) {
            this.clipboard.splice(found, 1);
            found = this.indexOfClipBoard(filePath);
        }
    }

    /**
     * Find index of a clip board item by it's file path
     */
    this.indexOfClipBoard = function (path) {
        var found = -1

        $.each(this.clipboard, function (index, item) {
            if (path === item.path) {
                found = index;
                return;
            }
        });

        return found;

    }


}