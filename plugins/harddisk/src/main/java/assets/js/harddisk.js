function harddisk(moduleId) {

    this.moduleId = moduleId;

    this.width = 1;

    this.onConnect = function () {

    };
    this.path = ['.'];

    //The clip board will hold more than one file

    this.documentReady = function (size) {
        if (size === 'full-screen') {

            var self = this;
            sendMessage(this.moduleId, 'browse', this.path.join('/'));

            $('.files').on('click', 'tr.folder.readable td:first-of-type', function () {
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

                sendMessage(self.moduleId, 'addClipboard', path);


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

            $('.files').on('click', '.calculate-size', function () {
                var source = self.path.join('/') + '/' + $(this).attr('data-name');
                sendMessage(self.moduleId, 'calculate', source);

            });

            $('body').on('click', '.action-copy', function () {
                var path = $(this).attr('data-path');
                var data = {
                    'source': path,
                    'destination': self.path.join('/')
                };

                sendMessage(self.moduleId, 'copy', JSON.stringify(data));
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
                sendMessage(self.moduleId, 'removeClipboard', $(this).attr('data-path'));
            });

            $('#hidden').click(function () {
                self.showHiddenFiles();
            });
        }
    };


    this.showHiddenFiles = function () {
        if ($('#hidden').is(':checked')) {
            $('.hiddenFile').removeClass('hidden');
        } else {
            $('.hiddenFile').addClass('hidden');
        }
    }

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


            this.clipBoardHtml(message.clipboard);
        } else if (command === 'browse') {
            $('.current-path').html(this.path.join('/'));
            $('.files tbody').html(this.files2html(message));
        } else if (command === 'success' || command === 'error') {
            sendMessage(this.moduleId, 'browse', this.path.join('/'));


            if (extra !== undefined && extra.source !== undefined) {
            }


            $('#upload-file').html('Upload file');
            $('#upload-file').removeAttr('disabled');
        } else if (command === 'calculate') {
            var target = $('td[data-hash="' + message.hash + '"]');
            target.html('');
            target.html(message.size.replace(' ', '&nbsp;'));
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

    this.clipBoardHtml = function (clipboard) {

        var html = [];

        html.push('<p class="counter">');
        html.push(Object.keys(clipboard).length);
        html.push(' files in clipboard');
        html.push('<i class="fa fa-chevron-down"></i>');
        html.push('</p>');


        html.push('<div class="content">');
        html.push('<p><i class="fa fa-times"></i></p>');
        // html.push('<div class="table-responsive">');

        $.each(clipboard, function (index, clipboardItem) {
            var filename = index.replace(/^.*[\\\/]/, '');
            var inProgress = clipboardItem.progress > 0;


            html.push('<div class="row">');
            html.push('<div class="col-md-6 text">', filename, '</div>');
            html.push('<div  class="col-md-6 actions">');
            if (!inProgress) {
                html.push('<button data-path="', index, '" class="btn btn-primary btn-sm action-copy" >Copy here</button>');
                html.push('<button data-path="', index, '" class="btn btn-primary btn-sm action-move" >Move here</button>');
                html.push('<button data-path="', index, '" class="btn btn-default btn-sm action-remove-from-clipboard"><i class="fa fa-times"></i></buton>');
            } else {
                html.push("<div class='actions'>");
                html.push('<div class="progress"><div class="progress-bar" style="width: ' + clipboardItem.progress + '%"></div></div>');
            }
            html.push("</div>");

            html.push('</div>');

        });


        html.push('</div>');

        // html.push('</div>') // content


        $('.clipboard').html(html.join(''));


        this.showClipboard(clipboard);
    }

    this.showClipboard = function (clipboard) {
        if (Object.keys(clipboard).length > 0) {
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
            html.push('<tr class="folder readable"><td colspan="3"  data-name="..">..</td></tr>');
        }

        var hideNow = !$('#hidden').is(':checked');
        $.each(files, function (index, value) {

            var isHidden = value.name !== '..' && value.name.startsWith('.');
            var hiddenClass = isHidden ? 'hiddenFile' : '';

            if (isHidden) {
                hiddenClass += hideNow ? ' hidden' : '';
            }

            var readableClass = value.readable ? 'readable' : '';

            var folderClass = value.folder ? 'folder' : '';
            html.push('<tr ', 'class="', readableClass, ' ', hiddenClass, ' ', folderClass, '">');


            var icon = '<i class="fa fa-file-o" aria-hidden="true"></i>';

            if (value.folder === true) {
                icon = '<i class="fa fa-folder-o" aria-hidden="true"></i>';
            }

            html.push('<td data-name="', value.name, '">', icon, '&nbsp;', value.name, '</td>');
            if (value.folder === true && value.readable) {
                html.push('<td data-hash="', value.hash, '"><a data-name="', value.name, '" class="calculate-size"><i class="fa fa-calculator" aria-hidden="true"></i></td>');
            } else if (value.size !== undefined) {
                html.push('<td>', value.size.replace(' ', '&nbsp;'), '</td>');
            } else {
                html.push('<td></td>');
            }
            html.push('<td><div class="dropdown">');
            if (value.readable) {
                html.push('<button class="btn btn-primary btn-sm" id="dLabel" type="button" data-toggle="dropdown" aria-haspopup="true" aria-expanded="false">');
                html.push('<i class="fa fa-ellipsis-v" aria-hidden="true"></i>');
                html.push('</button>');
                html.push('<ul class="dropdown-menu pull-right" aria-labelledby="dLabel">');
                html.push('<li><a data-name="', value.name, '" class="add-clipboard">Add to clipboard</a></li>');
                html.push('<li><a data-name="', value.name, '" class="rename">Rename</a></li>');
                html.push('<li><a data-name="', value.name, '" class="delete">Delete</a></li>');
                html.push('</ul>');
            }
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


}