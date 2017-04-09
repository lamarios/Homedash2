function mma(moduleId) {
    this.organization;
    this.moduleId = moduleId;
    this.history = [];

    this.size;
    this.timer = null;


    this.onConnect = function () {

    };

    this.documentReady = function (size) {

        this.size = size;
        console.log('size: ' + size);
        var root = rootElement(this.moduleId);
        var self = this;
        root.find('.modal').attr('data-module', this.moduleId);
        this.modal().appendTo(".modal-dump");

        root.on('click', '.event-link', function () {
            sendMessage(self.moduleId, 'getEvent', $(this).attr('data-url'));
            self.modal().modal('show');
            self.modal().find('.modal-title').html('...');
            self.modal().find('.modal-body').html('<div class="loader"></div>');

            self.history = [];
            //Adding to history
            self.history.push({command: 'getEvent', url: $(this).attr('data-url')});
        });


        this.modal().on('click', '.mma-link', function () {
            var source = $(this);
            self.modal().find('.modal-title').html('...');
            self.modal().find('.modal-body').html('<div class="loader"></div>');
            sendMessage(self.moduleId, source.attr('data-method'), source.attr('data-url'));

            self.history.push({command: source.attr('data-method'), url: source.attr('data-url')});
        });

        this.modal().on('click', '.mma-back', function () {
            self.modal().find('.modal-title').html('...');
            self.modal().find('.modal-body').html('<div class="loader"></div>');
            self.goBack();
        });

        if (this.size == 'full-screen') {

            root.on('keyup', '.search', function () {
                var query = $(this).val();
                if (self.timer !== null) {
                    clearTimeout(self.timer);
                }
                self.timer = setTimeout(function () {
                    sendMessage(self.moduleId, 'search', query);
                }, 500);
            });

            sendMessage(self.moduleId, 'search', '');
        }

        if (this.size == '3x2') {
            this.slideshowEvents();
        }
    };

    this.modal = function () {
        return $(document).find('.modal[data-module="' + this.moduleId + '"]');
    };


    this.onMessage_3x4 = function (command, message, extra) {
        this.onMessage_4x4(command, message, extra);
    };

    this.onMessage_fullScreen = function (command, message, extra) {
        this.onMessage_4x4(command, message, extra);
    };

    this.onMessage_4x4 = function (command, message, extra) {
        switch (command) {
            case 'getEvent':
                this.showEventDialog(message);
                this.verifyHistory();
                break;
            case 'getFighter':
                this.showFighter(message);
                this.verifyHistory();
                break;
            case 'refresh':
                this.processData(message);

        }
    };

    this.processData = function (org) {
        this.organization = org;
        var root = rootElement(this.moduleId);

        root.find('.org-name').html(org.name);
        root.find('.org-events tbody').html(this.eventListHtml(org.homeDashEvents));
    };

    this.eventListHtml = function (events) {
        if (this.size == 'full-screen') {
            events = events.reverse();
        }
        var html = [];
        $.each(events, function (index, value) {
            html.push('<tr class="event-link" data-url="', value.sherdogUrl, '">');
            html.push('<td>', value.date.dateTime.date.year, '-', value.date.dateTime.date.month,
                '-', value.date.dateTime.date.day, '</td>');
            html.push('<td>', value.name, '</td>');
            html.push('</tr>')
        });

        console.log(html.join(''));

        return html.join('');
    };

    this.showEventDialog = function (event) {

        var modal = this.modal();

        modal.find('.modal-title')
            .html(event.name + '<br /><em><small>' + event.date.dateTime.date.year + '-' +
                event.date.dateTime.date.month +
                '-' + event.date.dateTime.date.day + '</small></em>');
        var body = [];
        body.push(
            '<div class="table-responsive"><table class="event table table-condensed"><tbody>');

        $.each(event.fights, function (index, value) {
            body.push('<tr class=" ', value.result, '">');
            body.push('<td><a class="mma-link" data-method="getFighter" data-url="',
                value.fighter1.sherdogUrl,
                '">', value.fighter1.name,
                '</a></td><td>vs.</td><td> <a class="mma-link" data-method="getFighter" data-url="',
                value.fighter2.sherdogUrl,
                '">', value.fighter2.name, '</a></td>');

            if (value.result != "NOT_HAPPENED") {
                body.push('<td>', value.winMethod, '</td>');
                body.push('<td>', value.winRound, '</td>');
                body.push('<td>', value.winTime, '</td>')
            }
            body.push('</tr>');

        });

        body.push('</body></table></div>');

        modal.find('.modal-body').html(body.join(''));

    };

    this.showFighter = function (fighter) {
        var modal = this.modal();

        modal.find('.modal-title').html(fighter.name);
        var body = [];
        body.push('<h3>', fighter.wins, '-', fighter.losses, '-', fighter.draws, '-', fighter.nc,
            '</h3>');

        body.push('<div class="fighter-picture" style="background-image: url(\'', fighter.picture,
            '\')"></div>');
        body.push("<p>Weight: ", fighter.weight, '</p>');
        body.push("<p>Height: ", fighter.height, '</p>');
        body.push("<p>Birth date: ", fighter.birthday, '</p>');

        body.push('<h3>Fights</h3>');
        body.push(
            '<div class="table-responsive"><table class=" fighter-fights table table-condensed">');
        body.push(
            '<thead><tr><th>Opponent</th><th>Event</th><th>Method</th><th>Round</th><th>Time</th></tr></thead>');
        body.push('<tbody>');
        fighter.fights.reverse();
        $.each(fighter.fights, function (index, value) {

            body.push('<tr class="', value.result, '">');
            body.push('<td><a class="mma-link" data-method="getFighter" data-url="',
                value.fighter2.sherdogUrl,
                '">', value.fighter2.name, '</a></td>');
            body.push('<td><a class="mma-link" data-method="getEvent" data-url="',
                value.event.sherdogUrl,
                '">', value.event.name, '</a></td>');
            body.push('<td>', value.winMethod, '</td>');
            body.push('<td>', value.winRound, '</td>');
            body.push('<td>', value.winTime, '</td>');

            body.push('</tr>');
        });
        body.push('</tbody></table</div>');
        modal.find('.modal-body').html(body.join(''));
    };

    this.goBack = function () {
        this.history.pop();
        var last = this.history[this.history.length - 1];
        sendMessage(this.moduleId, last.command, last.url);

    };

    this.verifyHistory = function () {
        var toShow = this.history.length > 1;

        if (toShow) {
            if (this.modal().find('.mma-back').length == 0) {
                this.modal().find('.modal-title')
                    .prepend('<i class="mma-back fa fa-chevron-left" aria-hidden="true"></i>');
            }
        }
        else {
            this.modal().find('.mma-back').remove();
        }
    };

    ////////////////////////
    //// slideshow related Stuff
    /////////////////////////////////


    this.events = [];
    this.interval;
    this.timeout;
    this.currentIndex = 0;

    this.slideshowEvents = function () {
        var parent = this;

        var root = rootElement(this.moduleId);
        root.on('click', 'a.previous', function () {
            parent.showPreviousEvent();
            clearInterval(parent.interval);
            clearTimeout(parent.timeout);
            parent.timeout = setTimeout(function () {
                parent.playSlideShow()
            }, 10000);
        });

        root.on('click', 'a.next', function () {
            parent.showNextEvent();
            clearInterval(parent.interval);
            clearTimeout(parent.timeout);
            parent.timeout = setTimeout(function () {
                parent.playSlideShow()
            }, 10000);
        });
    };

    this.onMessage_3x2 = function (command, message, extra) {
        if(command === 'refresh') {
            clearInterval(this.interval);
            clearTimeout(this.timeout);

            this.events = message.homeDashEvents;
            this.currentIndex = 0;
            var body = rootElement(this.moduleId).find('.event-container');
            for (i = 0; i < this.events.length; i++) {
                body.append(this.eventToHtml(this.events[i], i));
            }

            this.showEvent();
            this.playSlideShow();
        }else{
            this.onMessage_4x4(command, message, extra);
        }
    };


    /**
     * Display all the shows
     */
    this.showEvent = function () {

        var root = rootElement(this.moduleId);

        var links = root.find('.next, .previous');
        links.attr('disabled', true);

        var oldShow = root.find('.event-container .active');

        if (this.currentIndex >= this.events.length) {
            this.currentIndex = 0;
        }

        if (this.currentIndex < 0) {
            this.currentIndex = this.events.length - 1;
        }

        var newShow = root.find('.event-container .event[data-event=' + this.currentIndex + ']');

        newShow.addClass('active');

        oldShow.removeClass('active');

        setTimeout(function () {
            links.removeAttr('disabled');
        }, 600);
    };

    /**
     * Show data to html
     * @param show
     * @param index
     * @returns {string}
     */
    this.eventToHtml = function (event, index) {
        var html = [];
        html.push('<div class="event" data-event="', index, '">');
        html.push('<div class="event-fighter event-fighter-1" style="background-image: url(\'', event.mainEventPhoto1, '\')"></div>');
        html.push('<div class="event-fighter event-fighter-2" style="background-image: url(\'', event.mainEventPhoto2, '\')"></div>');
        html.push('<div class="event-info">');
        html.push('<a class="event-link" data-url="', event.sherdogUrl,'"><i class="fa fa-info-circle" aria-hidden="true"></i></a>');
        html.push('<p class="event-title">', event.name, '</p>');
        html.push('<p class="event-date">', event.date.dateTime.date.year, '-', event.date.dateTime.date.month,
            '-', event.date.dateTime.date.day, '</p>');
        html.push('</p>');
        html.push('</div>');
        html.push('</div>');

        return html.join('');
    };

    /**
     * Compares if the list of shows is identical
     * @param a
     * @param b
     * @returns {boolean}
     */
    this.compareEvents = function (a, b) {
        if (a === b) {
            return true;
        }
        if (a == null || b == null) {
            return false;
        }
        if (a.length != b.length) {
            return false;
        }

        // If you don't care about the order of the elements inside
        // the array, you should sort both arrays here.

        for (var i = 0; i < a.length; ++i) {
            if (a[i].sherdogUrl !== b[i].sherdogUrl) {
                return false;
            }
        }
        return true;
    };

    /**
     * Controls
     * @param event
     */
    this.showPreviousEvent = function (event) {
        this.currentIndex = this.currentIndex - 1;
        this.showEvent();
    };

    this.showNextEvent = function (event) {
        this.currentIndex = this.currentIndex + 1;
        this.showEvent();
    };

    /**
     * Automated slideshow
     */
    this.playSlideShow = function () {
        var parent = this;
        this.interval = setInterval(function () {
            parent.showNextEvent();
        }, 7000);
    }
}