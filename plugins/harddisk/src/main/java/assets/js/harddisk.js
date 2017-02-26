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

        html.push('<g opacity="',opacity,'">');
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

}