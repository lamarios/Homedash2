function logreader(moduleId) {

    this.moduleId = moduleId;

    this.autoScroll = true;

    this.maxActivity = 1;

    this.onConnect = function () {

    };

    //The clip board will hold more than one file

    this.documentReady = function (size) {
        var self = this;
        if (size === 'full-screen') {
            $('#auto-scroll').click(function () {
                self.autoScroll = !self.autoScroll;
                $(this).toggleClass('active');
            });
        }

    }

    this.onConnect = function () {

    };

    this.onMessage_1x1 = function (command, message, extra) {

        this.maxActivity = Math.max(message.sinceRefresh, this.maxActivity);

        var percentage = message.sinceRefresh / this.maxActivity * 100;
        console.log(percentage)
        rootElement(this.moduleId).find('.activity').css('width', percentage + '%');
        rootElement(this.moduleId).find('.activity').css('height', percentage + '%');

        rootElement(this.moduleId).find('.path').html(message.path);
    }

    this.onMessage_fullScreen = function (command, message, extra) {
        var logs = $('#logs');
        logs.val(message.join('\n'));

        if (logs.length && this.autoScroll === true) {
            logs.scrollTop(logs[0].scrollHeight - logs.height());
        }
    }
}