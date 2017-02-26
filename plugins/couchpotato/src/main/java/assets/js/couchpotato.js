function couchpotato(moduleId) {

    this.moduleId = moduleId;

    this.documentReady = function (size) {
        var self = this;
        var root = self.root();

        root.find('.modal').attr('data-module', this.moduleId);

        root.find(".show-search").click(function () {
            self.modal().appendTo(".modal-dump").modal('show');
        });

        this.modal().find(".search-submit").click(function (event) {
            self.searchMovie();
        });

        this.modal().on('click', ".movie", function (event) {

            var movieName = $(this).attr("data-title");
            var imdb = $(this).attr("data-imdb");
            self.addMovie(movieName, imdb);

        });
    }

    this.modal = function () {
        return $(document).find('.modal[data-module="' + this.moduleId + '"]');
    }

    this.root = function () {
        return rootElement(this.moduleId);
    }

    this.onMessage_1x1 = function (command, message, extra) {
        this.processData(command, message, extra);
    }

    this.onMessage_1x3 = function (command, message, extra) {
        this.processData(command, message, extra);
    }

    this.onMessage_2x1 = function (command, message, extra) {
        this.processData(command, message, extra);
    }
    this.onMessage_2x2 = function (command, message, extra) {
        this.processData(command, message, extra);
    }

    this.onMessage_2x3 = function (command, message, extra) {
        this.processData(command, message, extra);
    }
    this.onMessage_3x3 = function (command, message, extra) {
        this.processData(command, message, extra);
    }
    this.onMessage_3x2 = function (command, message, extra) {
        this.processData(command, message, extra);
    }

    this.processData = function (command, message, extra) {
        if (command == 'refresh') {
            this.refresh(message);
        } else if (command == 'movieList') {
            this.populateMovieList(message);
        } else if (command == 'error') {
            this.modal().modal('hide');
        }
    }

    this.searchMovie = function () {

        this.modal().find('.couchpotato-movie-list').html('<div class="loader"></div>');
        //$("#cp"+this.moduleId+"-modal").appendTo("body").modal('show');
        sendMessage(this.moduleId, 'searchMovie', this.modal().find('.search-query').val());
    }

    this.refresh = function (message) {
        if (!message) {
            this.root.find(".overlay").html('Couch potato is not available at the moment');
            this.root.find(".overlay").show();
        } else {
            this.root().css('background-image', 'url(' + message + ')');
        }

    }

    this.addMovie = function (movieName, imdb) {

        sendMessage(this.moduleId, 'addMovie', movieName + '___' + imdb);
        this.modal().modal('hide');
    }

    this.populateMovieList = function (message) {
        var parent = this;
        var movieList = parent.modal().find('.couchpotato-movie-list');
        movieList.html('');
        if (message.length > 0) {
            $.each(message, function (index, value) {
                movieList.append(parent.movieToHtml(value));
                //$("#cp" + parent.moduleId + "-movieList").append('<hr style="border-color: black;
                // margin:0"/>');
            });
        } else {
            movieList.html('<p>No results</p>');
        }

    }

    this.movieToHtml = function (movie) {
        var html = [];
        html.push('<div class="movie" data-imdb="', movie.imdbId, '" data-title="',
                  movie.originalTitle, '">');
        html.push('<div class="movie-poster" style="background-image:url(', movie.poster,
                  ');"></div>');
        html.push('<p class="movie-name"><strong>', movie.originalTitle, ' </strong>');

        if (movie.wanted) {
            html.push('<small>(already wanted)</small>');
        }

        if (movie.inLibrary) {
            html.push('<small>(already in library)</small>');
        }

        html.push(' - <span class="cp-movie-year">', movie.year, '</span></p>');

        html.push('</div>')
        return html.join('');

    }

}