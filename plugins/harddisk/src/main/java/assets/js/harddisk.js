function harddisk(moduleId) {

    this.moduleId = moduleId;

    this.width = 1;

    this.onConnect = function () {

    };

    this.documentReady = function (size) {

    };

    this.onMessage_2x1 = function (command, message, extra) {
        this.width = 2;
        this.processData(message);
    };
    this.onMessage_1x1 = function (command, message, extra) {
        this.width = 1;
        this.processData(message);
    };

    this.processData = function (diskSpace) {

        var root = rootElement(this.moduleId);

        root.find('.path').html(diskSpace.path);

        var totalSpace = diskSpace.total;
        var usedSpace = diskSpace.used;
        var percentage = Math.ceil((usedSpace / totalSpace) * 100);


        if (this.width === 2) {
            root.find('.data').html(diskSpace.pretty);
        }


        root.find('.hdd-container').html(this.generateSVG(percentage));
    };


    this.getDiskSpaceSVG = function (percentage) {

        var html = [];
        html.push('<svg class="hdd-svg" preserveAspectRatio="none" version="1.1" xmlns:xlink="http://www.w3.org/1999/xlink" xmlns="http://www.w3.org/2000/svg"  viewBox="0 0 100 100">');
        html.push('<g class="surfaces">');
        html.push('<rect class="hdd-rect-full" x="0" y="0" width="100" height="100" />');
        html.push('<rect class="hdd-rect" x="0" y="0" width="', percentage, '" height="100" />');
        html.push('</g>');
        html.push('</svg>');

        return html.join('');
    };


    this.generateSVG = function (percentage) {

        var html = [];
        html.push('<svg class="hdd-svg" preserveAspectRatio="all" version="1.1" xmlns:xlink="http://www.w3.org/1999/xlink" xmlns="http://www.w3.org/2000/svg"  viewBox="0 0 220 220">');
        html.push('<polygon points="10,160 110,210 110,110 10,60" />');
        html.push('<polygon points="110,210 210,160 210,60 110,110" />');
        html.push('<polygon points="10,60 110,110 210,60 110,10" />');

        
        html.push('<g>');
        html.push('<!-- keep:bottom left, bottom right| change: top right, top left-->');
        //html.push('<polygon points="10,160 110,210 110,200 10,160">');
        html.push('<polygon points="10,160 110,210 110,', 110 + (100 - percentage), ' 10,', 60 + 100 - percentage, '">');
        //html.push('<animate attributeName="points" dur="1000ms" to="10,160 110,210 110,',110+(100-percentage),' 10,',60+100-percentage,'" fill="freeze" />');
        html.push('</polygon>');
        html.push('<polygon points="110,210 210,160 210,', 60 + 100 - percentage, ' 110,', 110 + 100 - percentage, '" >');
        //html.push('<polygon points="110,210 210,160 210,160 110,210" >');
        //html.push('<animate attributeName="points" dur="1000ms" to="110,210 210,160 210,',60+100-percentage,' 110,',110+100-percentage,'" fill="freeze"/>');
        html.push('</polygon>');

        if (percentage == 100) {
            html.push('<polygon points="10,60 110,110 210,60 110,10" />');
        }


        html.push('</g>');

        html.push('</svg>');

        return html.join('');
    }

}