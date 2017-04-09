function googlepubliccalendar(moduleId) {
    this.moduleId = moduleId;

    this.onConnect = function () {
    };

    this.documentReady = function (size) {

        var parent = this;
        var root = this.root();

        var modal = this.modal();

        root.on('click', '.gcal-event', function (event) {
            var tr = $(this);
            var link = '<a href="' + tr.attr('data-href')
                       + '" target="_blank">Link to the event</a>';

            modal.find('.event-title').html(tr.attr('data-title'));
            modal.find('.event-date').html(tr.attr('data-date'));
            modal.find('.description')
                .html(parent.url2link(parent.nl2br(tr.attr('data-description')))
                      + '<br /><br />' + link);

            modal.modal('show');
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

    this.onMessage_3x1 = function (command, message, extra) {
        var root = this.root();

        root.find('.title').html('Next on ' + message.title);

        if (message.events.length > 0) {
            var event = message.events[0];
            var eventElem = root.find('.gcal-event');

            eventElem.attr('data-title', event.summary);
            eventElem.attr('data-description', event.description);
            eventElem.attr('data-href', event.link);
            eventElem.attr('data-date', event.startTime);

            root.find('.event-title').html(event.summary);
            root.find('.date').html(event.startTime);
        }
    };

    this.onMessage_fullScreen = function (command, message, extra) {
        this.processMessage(command, message, extra);
    };

    this.onMessage_3x4 = function (command, message, extra) {
        this.processMessage(command, message, extra);
    };

    this.onMessage_4x4 = function (command, message, extra) {
        this.processMessage(command, message, extra);
    };

    this.processMessage = function (command, message, extra) {
        switch (command) {
            case 'refresh':
            default:
                this.processData(message);
                break;
        }
    };

    this.processData = function (message) {
        var parent = this;
        var root = this.root();
        var table = root.find('.events tbody');

        root.find('.title').html(message.title);

        table.html('');

        $.each(message.events, function (index, event) {
            var html = [];

            html.push('<tr data-date="', event.startTime, '" data-title="', event.summary,
                      '" data-description="', event.description, '" data-href="', event.link,
                      '" class="gcal-event">');
            html.push('<td>', event.startTime, '</td><td>', event.summary, '</td>');
            html.push('</tr>');

            table.append(html.join(''));
        });
    };

    this.nl2br = function (str, is_xhtml) {
        var breakTag = (is_xhtml || typeof is_xhtml === 'undefined') ? '<br />' : '<br>';
        return (str + '').replace(/([^>\r\n]?)(\r\n|\n\r|\r|\n)/g, '$1' + breakTag + '$2');
    };

    this.url2link = function (text) {
        var exp = /(\b(https?|ftp|file):\/\/[-A-Z0-9+&@#\/%?=~_|!:,.;]*[-A-Z0-9+&@#\/%=~_|])/ig;
        return text.replace(exp, '<a href="$1" target="_blank">$1</a>');
    };

}