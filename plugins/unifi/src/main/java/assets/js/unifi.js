function unifi(moduleId) {

    this.moduleId = moduleId;

    this.onConnect = function () {

    };

    this.documentReady = function (size) {


    };

    this.onMessage = function (size, command, message, extra) {
        switch (size) {
            default:
                this.processData(message);
                break;
        }
    }


    this.processData = function (json) {
        var root = rootElement(this.moduleId);
        root.find('.site').html(json.site);
        root.find('.latency').html(json.latency);
        root.find('.unit').html(json.unit);
        root.find('.up').html(json.upHumanReadable);
        root.find('.down').html(json.downHumanReadable);
    };

}