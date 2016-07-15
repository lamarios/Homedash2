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

    this.processData = function (obj) {

        var root = this.root();

        root.find('.up-txt').html(obj.readableUp);
        root.find('.down-txt').html(obj.readableDown);

        root.find('.up-total-txt').html(obj.readableTotalUp);
        root.find('.down-total-txt').html(obj.readableTotalDown);

        root.find('.interface-name').html(obj.name + '<small> - ' + obj.ip + '</small>');

    };

}