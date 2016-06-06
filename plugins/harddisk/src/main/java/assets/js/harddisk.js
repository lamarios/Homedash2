function harddisk(moduleId) {

    this.moduleId = moduleId;

    this.width = 1;

    this.onConnect = function () {

    };

    this.documentReady = function () {

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


        root.find('.hdd-container').html(this.getDiskSpaceSVG(percentage));
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


}