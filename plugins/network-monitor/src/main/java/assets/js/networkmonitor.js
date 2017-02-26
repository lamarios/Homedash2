function networkmonitor(moduleId) {

    this.moduleId = moduleId;

    this.width = 1;

    this.onConnect = function () {

    };

    this.documentReady = function (size) {

    };

    this.onMessage_2x1 = function (command, message, extra) {
        this.width = 2;
        this.processData(message);
    }

    this.onMessage_3x2 = function (command, message, extra) {
        this.width = 3;
        this.processData(message);
    };

    this.root = function () {
        return rootElement(this.moduleId);
    }

    this.processData = function (data) {

        var obj = data[data.length - 1];
        var root = this.root();

        root.find('.up-txt').html(obj.readableUp);
        root.find('.down-txt').html(obj.readableDown);

        root.find('.up-total-txt').html(' '+obj.readableTotalUp);
        root.find('.down-total-txt').html(' '+obj.readableTotalDown);

        root.find('.interface-name').html(obj.name + '<small> - ' + obj.ip + '</small>');

        var graph = this.buildGraph(data);

        root.find('.svg').html(graph);

    };

    this.buildGraph = function (data) {
        var upStr = [];
        var downStr = [];

        var upMax = 0;
        var downMax = 0;
        $.each(data, function (index, value) {
            upMax = Math.max(value.up, upMax);
            downMax = Math.max(value.down, downMax);
        });

        //gives some space inbetween the graphs
        upMax = upMax * 1.1;
        downMax = downMax * 1.1;

        var step = 100 / data.length;
        var width = step - 1;

        for (var i = 0; i < data.length; i++) {
            var downPerc = (data[i].down / downMax) * 100;
            var upPerc = (data[i].up / upMax) * 100;
            var position = i * step;

            downStr.push('<rect width="', width, '" height="', downPerc, '" y="0" x="', position,
                         '" />');
            upStr.push('<rect width="', width, '" height="', upPerc, '" y="', 100 + (100 - upPerc),
                       '" x="', position, '" />');
        }

        var html = [];
        html.push(
            '<svg preserveAspectRatio="none" class="graph" version="1.1" xmlns:xlink="http://www.w3.org/1999/xlink" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 200" opacity="0.1">');
        html.push('<g class="down">', downStr.join(''), '</g>');

        html.push('<g class="up">', upStr.join(''), '</g>');
        html.push('</svg>');

        return html.join('');
    };

}