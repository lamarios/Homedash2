function systeminfo(moduleId) {

    this.moduleId = moduleId;

    this.cpuHistory = [];
    this.ramHistory = [];
    this.width = 1;

    this.onConnect = function () {

    }

    this.documentReady = function (size) {
        if (size === 'full-screen') {
            var root = rootElement(this.moduleId);

            var self = this;

            root.on('click', '.sort', function (event) {
                root.find('.sort').removeClass('selected');
                $(this).addClass('selected');
                sendMessage(self.moduleId, 'sort', $(this).attr('data-sort'));
            });
        }
    }

    this.onMessage_2x1 = function (command, message, extra) {
        this.width = 2;
        this.processData(message);
    }
    this.onMessage_1x1 = function (command, message, extra) {
        this.width = 1;
        this.processData(message);
    }

    this.onMessage_fullScreen = function (command, message, extra) {
        var root = rootElement(this.moduleId);

        this.cpuHistory = message.cpuInfo;
        this.ramHistory = message.ramInfo;

        var cpu = this.cpuHistory[this.cpuHistory.length - 1].cpuUsage;
        var ram = this.ramHistory[this.ramHistory.length - 1];
        var temp = this.cpuHistory[this.cpuHistory.length - 1].temperature;
        var hardware = message.hardwareInfo;
        var os = message.osInfo;

        root.find('.ram .text').html(this.humanFileSize(ram.usedRam, ram.maxRam, false));
        root.find('.cpu .text').html(cpu + '%');
        root.find('.temp .text').html(temp + '&#8451');

        root.find('.ram .graph svg path').attr('d', this.ramArrayToSVGGraph(this.ramHistory));
        //root.find('.cpu .graph svg path').attr('d', this.cpuArrayToSVGGraph(this.cpuHistory));
        root.find('.temp .graph svg path').attr('d', this.tempArrayToSVGGraph(this.cpuHistory));
        root.find('.cores').html(this.buildCoreHtml(this.cpuHistory));
        //System info

        root.find('.cpu-info .name').html(hardware.name);
        root.find('.cpu-info .logical-cores').html(hardware.logicalCores);
        root.find('.cpu-info .physical-cores').html(hardware.physicalCores);

        root.find('.os-info .os')
            .html(os.manufacturer + ' ' + os.family + ' ' + os.version + ' build ' + os.build);

        root.find('.os-info .uptime .value').html(this.uptimeToString(hardware.uptime));
        root.find('.processes table tbody').html(this.buildProcessTableBody(os.processes));
    }

    this.processData = function (obj) {

        var root = rootElement(this.moduleId);

        this.cpuHistory = obj.cpuInfo;
        this.ramHistory = obj.ramInfo;

        if (this.ramHistory.length > 0 && this.cpuHistory.length > 0) {

            var cpu = this.cpuHistory[this.cpuHistory.length - 1].cpuUsage;
            var ram = this.ramHistory[this.ramHistory.length - 1];

            var cpuText = root.find('.cpu-txt');
            var ramText = root.find('.ram-txt');

            cpuText.html(cpu);

            switch (this.width) {
                case 1:

                    ramText.html(ram.percentageUsed);
                    break;
                case 2:
                    ramText.html(this.humanFileSize(ram.usedRam, ram.maxRam, false));
                    break;
            }

            root.find('.cpu-svg').attr('d', this.cpuArrayToSVGGraph(this.cpuHistory));
            root.find('.ram-svg').attr('d', this.ramArrayToSVGGraph(this.ramHistory));
        }

    };

    this.buildProcessTableBody = function (processes) {
        var html = [];

        var self = this;

        $.each(processes, function (index, process) {
            html.push('<tr>');
            html.push('<td>', process.pid, '</td>');
            html.push('<td>', process.name, '</td>');
            html.push('<td>', Math.ceil(process.cpuUsage), '%</td>');
            html.push('<td>', self.humanFileSizeSingle(process.memory, true), '</td>');
        });

        return html.join('');
    }

    this.cpuArrayToSVGGraph = function (array) {
        var html = [];
        html.push('M0,100');
        var lastIndex = 0;
        var step = (100) / array.length;
        html.push(' L0,', 100 - array[0].cpuUsage);
        $.each(array, function (index, cpuInfo) {

            html.push(' L', (index + 1) * step, ',', 100 - cpuInfo.cpuUsage);
            lastIndex = index * step;
        });
        html.push(' L', 100, ',100 Z');
        return html.join('');
    }

    this.tempArrayToSVGGraph = function (array) {
        var html = [];
        html.push('M0,100');
        var lastIndex = 0;
        var step = (100) / array.length;
        html.push(' L0,', 100 - array[0].cpuUsage);
        $.each(array, function (index, cpuInfo) {

            html.push(' L', (index + 1) * step, ',', 100 - cpuInfo.temperature);
            lastIndex = index * step;
        });
        html.push(' L', 100, ',100 Z');
        return html.join('');
    }

    this.ramArrayToSVGGraph = function (array) {
        var html = [];
        html.push('M0,100');
        var lastIndex = 0;
        var step = (100) / array.length;
        html.push(' L0,', 100 - array[0].percentageUsed);
        $.each(array, function (index, ramInfo) {

            html.push(' L', (index + 1) * step, ',', 100 - ramInfo.percentageUsed);
            lastIndex = index * step;
        });
        html.push(' L', 100, ',100 Z');
        return html.join('');
    }

    this.coreGraph = function (array, coreIndex) {
        var html = [];
        html.push('M0,100');
        var lastIndex = 0;
        var step = (100) / array.length;
        html.push(' L0,', 100 - array[0].coreUsage[coreIndex]);
        $.each(array, function (index, cpuInfo) {

            html.push(' L', (index + 1) * step, ',', 100 - cpuInfo.coreUsage[coreIndex]);
            lastIndex = index * step;
        });
        html.push(' L', 100, ',100 Z');
        return html.join('');
    }

    this.humanFileSizeSingle = function (memory, si) {

        var thresh = si ? 1000 : 1024;
        if (memory < thresh) {
            return memory + ' B';
        }
        var units = si ? ['kB', 'MB', 'GB', 'TB', 'PB', 'EB', 'ZB', 'YB'] : ['KiB', 'MiB', 'GiB',
                                                                             'TiB', 'PiB', 'EiB',
                                                                             'ZiB', 'YiB'];
        var u = -1;
        do {
            memory /= thresh;
            ++u;
        } while (memory >= thresh);
        return memory.toFixed(1) + ' ' + units[u];
    }

    this.humanFileSize = function (used, max, si) {
        var thresh = si ? 1000 : 1024;
        if (max < thresh) {
            return max + ' B';
        }
        var units = si ? ['kB', 'MB', 'GB', 'TB', 'PB', 'EB', 'ZB', 'YB'] : ['KiB', 'MiB', 'GiB',
                                                                             'TiB', 'PiB', 'EiB',
                                                                             'ZiB', 'YiB'];
        var u = -1;
        do {
            used /= thresh;
            max /= thresh;
            ++u;
        } while (max >= thresh);
        return used.toFixed(1) + '/' + max.toFixed(1) + ' ' + units[u];
    }

    this.uptimeToString = function secondsToString(seconds) {
        var numyears = Math.floor(seconds / 31536000);
        var numdays = Math.floor((seconds % 31536000) / 86400);
        var numhours = Math.floor(((seconds % 31536000) % 86400) / 3600);
        var numminutes = Math.floor((((seconds % 31536000) % 86400) % 3600) / 60);
        var numseconds = (((seconds % 31536000) % 86400) % 3600) % 60;

        var str = [];
        if (numyears > 0) {
            str.push(numyears, ' years ');
        }

        if (numdays > 0) {
            str.push(numdays, ' days ');
        }

        if (numhours > 0) {
            str.push(numhours, ' hours ');
        }

        if (numminutes > 0) {
            str.push(numminutes, ' minutes ');
        }

        if (numseconds > 0) {
            str.push(numseconds, ' seconds ');
        }

        return str.join('');
    }

    /**
     * Build the core graphs and encompassing html
     * @param cores
     */
    this.buildCoreHtml = function (array) {
        var svgBase = '<svg viewBox="0 0 100 100" preserveAspectRatio="none" xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" version="1.1"><g class="surfaces">';
        var svgEnd = '</g></svg>';
        var html = [];
        if (array.length > 0) {
            var coreCount = array[0].coreUsage.length;

            console.log("Corecount", coreCount);
            for (var i = 0; i < coreCount; i++) {
                html.push('<div class="col-xs-4 col-sm-3 graph-container"><h3 class="core-number">',
                          i + 1, '</h3><div class="graph">', svgBase);

                html.push('<path d="', this.coreGraph(array, i), '" />');
                html.push(svgEnd, '</div><div class="text">', array[array.length - 1].coreUsage[i],
                          '%</div></div>');
            }

        }
        return html.join('');
    }

}