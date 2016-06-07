function sonarrtv(moduleId) {

    this.moduleId = moduleId;
    this.shows = [];
    this.interval;
    this.timeout;
    this.currentIndex = 0;


    this.documentReady = function (size) {

        var parent = this;

        var root = rootElement(this.moduleId);
        root.on('click', 'a.previous', function () {
            parent.showPreviousShow();
            clearInterval(parent.interval);
            clearTimeout(parent.timeout);
            parent.timeout = setTimeout(function () {
                parent.playSlideShow()
            }, 10000);
        });


        root.on('click', 'a.next', function () {
            parent.showNextShow();
            clearInterval(parent.interval);
            clearTimeout(parent.timeout);
            parent.timeout = setTimeout(function () {
                parent.playSlideShow()
            }, 10000);
        });

    }


    this.onMessage_2x2 = function (command, message, extra) {
        this.processData(message);
    }

    this.onMessage_3x3 = function (command, message, extra) {
        this.processData(message);
    }

    this.onMessage_4x4 = function (command, message, extra) {
        this.processData(message);
    }

    this.onMessage_3x1 = function (command, message, extra) {
        this.processData(message);
    }


    this.processData = function (message) {
        clearInterval(this.interval);
        clearTimeout(this.timeout);

        this.shows = message;
        this.currentIndex = 0;
        var body = rootElement(this.moduleId).find('.shows-container');
        for (i = 0; i < this.shows.length; i++) {
            body.append(this.showToHtml(this.shows[i], i));
        }

        this.showShow();
        this.playSlideShow();
    }


    /**
     * Display all the shows
     */
    this.showShow = function () {

        var root = rootElement(this.moduleId);

        var links = root.find('.next, .previous');
        links.attr('disabled', true);


        var oldShow = root.find('.shows-container .active');

        if (this.currentIndex >= this.shows.length) {
            this.currentIndex = 0;
        }

        if (this.currentIndex < 0) {
            this.currentIndex = this.shows.length - 1;
        }

        var newShow = root.find('.shows-container .show[data-show=' + this.currentIndex + ']');

        newShow.addClass('active');

        oldShow.removeClass('active');

        setTimeout(function () {
            links.removeAttr('disabled');
        }, 600);
    }


    /**
     * Show data to html
     * @param show
     * @param index
     * @returns {string}
     */
    this.showToHtml = function (show, index) {
        var html = [];
        html.push('<div class="show" data-show="', index, '" style="background-image:url(', show.fanart, ')">');
        html.push('<div class="show-info">');
        html.push('<p>');
        html.push('<span class="show-date">', show.airDate, '</span>');
        html.push('<span class="show-title">', show.seriesName, '</span>');
        html.push('</p>');
        html.push('</div>');
        html.push('</div>');

        return html.join('');
    }


    /**
     * Compares if the list of shows is identical
     * @param a
     * @param b
     * @returns {boolean}
     */
    this.compareShows = function (a, b) {
        if (a === b)
            return true;
        if (a == null || b == null)
            return false;
        if (a.length != b.length)
            return false;

        // If you don't care about the order of the elements inside
        // the array, you should sort both arrays here.

        for (var i = 0; i < a.length; ++i) {
            if (a[i].showId !== b[i].showId)
                return false;
        }
        return true;
    }


    /**
     * Controls
     * @param event
     */
    this.showPreviousShow = function (event) {
        this.currentIndex = this.currentIndex - 1;
        this.showShow();
    }

    this.showNextShow = function (event) {
        this.currentIndex = this.currentIndex + 1;
        this.showShow();
    }


    /**
     * Automated slideshow
     */
    this.playSlideShow = function () {
        var parent = this;
        this.interval = setInterval(function () {
            parent.showNextShow();
        }, 7000);
    }
}