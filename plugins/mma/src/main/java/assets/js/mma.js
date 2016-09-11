function mma(moduleId) {
    this.organization;
    this.moduleId = moduleId;
    this.history = [];

    this.size;
    this.timer = null

    this.onConnect = function () {

    };

    this.documentReady = function (size) {

        this.size = size;
console.log('size: '+size);
        var root = rootElement(this.moduleId);
        var self = this;
        root.find('.modal').attr('data-module', this.moduleId);
        this.modal().appendTo("body");

        root.on('click', '.org-events tbody tr', function () {
            sendMessage(self.moduleId, 'getEvent', $(this).attr('data-url'));
            self.modal().modal('show');
            self.modal().find('.modal-title').html('...');
            self.modal().find('.modal-body').html('<div class="loader"></div>');

            self.history = [];
            //Adding to history
            self.history.push({command: 'getEvent', url: $(this).attr('data-url')});
        });

        root.on('keyup', '.search', function(){
            var query = $(this).val();
            if(self.timer !== null) {
                clearTimeout(self.timer);
            }
            self.timer = setTimeout(function(){
                sendMessage(self.moduleId, 'search', query);
            }, 500);
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


        if(this.size == 'full-screen'){
            sendMessage(self.moduleId, 'search', '');
        }
    };

    this.modal = function () {
        return $(document).find('.modal[data-module="' + this.moduleId + '"]');
    }

    this.onMessage_3x4 = function (command, message, extra) {
        this.onMessage_4x4(command, message, extra);
    }

    this.onMessage_fullScreen = function (command, message, extra) {
        this.onMessage_4x4(command, message, extra);
    }

    this.onMessage_4x4 = function (command, message, extra) {
        switch (command) {
            case 'getEvent':
                this.showEvent(message);
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
        root.find('.org-events tbody').html(this.eventListHtml(org.events));
    };

    this.eventListHtml = function (events) {
        if (this.size == 'full-screen') {
            events = events.reverse();
        }
        var html = [];
        $.each(events, function (index, value) {
            html.push('<tr data-url="', value.sherdogUrl, '">');
            html.push('<td>', value.date.dateTime.date.year, '-', value.date.dateTime.date.month,
                      '-', value.date.dateTime.date.day, '</td>');
            html.push('<td>', value.name, '</td>');
            html.push('</tr>')
        });

        console.log(html.join(''));

        return html.join('');
    }

    this.showEvent = function (event) {

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
                body.push('<td>', value.winMethod, '</td>')
                body.push('<td>', value.winRound, '</td>')
                body.push('<td>', value.winTime, '</td>')
            }
            body.push('</tr>');

        });

        body.push('</body></table></div>');

        modal.find('.modal-body').html(body.join(''));

    }

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
            body.push('<td>', value.winMethod, '</td>')
            body.push('<td>', value.winRound, '</td>')
            body.push('<td>', value.winTime, '</td>')

            body.push('</tr>');
        });
        body.push('</tbody></table</div>');
        modal.find('.modal-body').html(body.join(''));
    }

    this.goBack = function () {
        this.history.pop();
        var last = this.history[this.history.length - 1];
        sendMessage(this.moduleId, last.command, last.url);

    }

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
    }
}