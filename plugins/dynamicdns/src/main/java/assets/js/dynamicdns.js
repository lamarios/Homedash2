function dynamicdns(moduleId) {
    this.moduleId = moduleId;

    this.onConnect = function () {
    };

    this.documentReady = function (size) {
        var parent = this;
        var root = this.root();


        root.on('click', '.refresh', function () {
            sendMessage(parent.moduleId, 'forceRefresh', '');
        });
    };

    this.root = function () {
        return rootElement(this.moduleId);
    };

    /**
     * Help function to get the modal of the html
     * @returns {*|jQuery}
     */
    this.modal = function () {
        var root = this.root();
        if (root.find('.modal') != undefined) {
            root.find('.modal').attr('data-module', this.moduleId);
            root.find('.modal').appendTo(".modal-dump");
        }

        return $(document).find('.modal[data-module="' + this.moduleId + '"]');
    };


    this.onMessage = function (size, command, message, extra) {
        switch (size) {
            case '2x1':
                this.processMessage(command, message, extra);
                break;
        }
    }

    this.processMessage = function (command, message, extra) {
        switch (command) {
            case 'refresh':
                this.processData(message);
                break;
        }
    };

    this.processData = function (json) {
        var root = this.root();
        if(json.ip !== 'undefined') {
            root.find('.address').html(json.ip.address);
            root.find('.update-date').html('Last update:' + json.ip.date + ' - via ' + json.ip.method);
        }
        root.find('.host').html(json.host+'@'+json.provider);
    };
}