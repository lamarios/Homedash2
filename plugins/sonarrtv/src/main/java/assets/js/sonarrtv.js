function sonarrtv(moduleId) {

    this.moduleId = moduleId;
    this.shows = [];
    this.interval;
    this.timeout;
    this.currentIndex = 0;
    this.searchResults = [];
    this.qualities = [];
    this.folders = [];
    this.onConnect = function () {
    }

    this.documentReady = function (size) {

        var parent = this;


        if (size !== 'full-screen') {
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
        } else {
            $('#search-button').click(function () {
                var query = $('#search-input').val();
                if (query.length > 0) {
                    parent.searchShow(query);
                }
            });

            $('#results').on('click', '.show .banner', function (e) {
                $(e.target).parent().next('.info').toggle();
            });


            // when the user changes the season monitored status
            $('#results').on('change', '.season input[type="checkbox"]', function (e) {
                var source = $(e.target);
                var index = source.attr('data-index');
                var season = source.attr('data-season');
                var checked = source.prop("checked");

                // console.log('series', index, 'season', season, 'checked', source.prop("checked"));

                parent.searchResults[index].seasons.forEach(function (s) {
                    console.log(s, season, s.seasonNumber === season);
                    if (s.seasonNumber == season) {

                        s.monitored = checked;
                    }
                });

            });

            // add show
            $('#results').on('click', '.add-show', function (e) {
                var source = $(e.target);
                var index = source.attr('data-index');
                var quality = source.siblings('select.quality').val();
                var folder = source.siblings('select.folder').val();

                var show = parent.searchResults[index];
                show.qualityProfileId = quality;
                show.rootFolderPath = folder;
                show.monitored = true;
                show.seasonFolder= true;
                show.addOptions = {
                    ignoreEpisodesWithFiles: true,
                    ignoreEpisodesWithoutFiles: false,
                    searchForMissingEpisodes: true,

                }
                console.log('index', index, 'quality', quality, 'folder', folder);
                console.log(show);

                sendMessage(parent.moduleId, 'add-show', JSON.stringify(show));

            });

            //asking for the profile
            sendMessage(this.moduleId, 'qualities', '');
            sendMessage(this.moduleId, 'folders', '');

        }


    };

    this.onMessage = function (size, command, message, extra) {
        switch (size) {
            case 'full-screen':
                switch (command) {
                    case 'search':
                        this.processSearchResults(JSON.parse(message));
                        break;
                    case 'qualities':
                        this.qualities = JSON.parse(message);
                        break;
                    case 'folders':
                        this.folders = JSON.parse(message);
                        break;
                }
                break;
            default:
                this.processData(message);
                break;
        }
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
    };

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
    };

    /**
     * Show data to html
     * @param show
     * @param index
     * @returns {string}
     */
    this.showToHtml = function (show, index) {
        var html = [];
        html.push('<div class="show" data-show="', index, '" style="background-image:url(/',
            show.fanart, ')">');
        html.push('<div class="show-info">');
        html.push('<p>');
        html.push('<span class="show-date">', show.airDate, '</span>');
        html.push('<span class="show-title">', show.seriesName, '</span>');
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
    this.compareShows = function (a, b) {
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
            if (a[i].showId !== b[i].showId) {
                return false;
            }
        }
        return true;
    };

    /**
     * Controls
     * @param event
     */
    this.showPreviousShow = function (event) {
        this.currentIndex = this.currentIndex - 1;
        this.showShow();
    };

    this.showNextShow = function (event) {
        this.currentIndex = this.currentIndex + 1;
        this.showShow();
    };

    /**
     * Automated slideshow
     */
    this.playSlideShow = function () {
        var parent = this;
        this.interval = setInterval(function () {
            parent.showNextShow();
        }, 7000);
    };

    /**
     * Search for a show
     * @param query
     */
    this.searchShow = function (query) {
        $('#results').html('Searching...');
        sendMessage(this.moduleId, 'search', query);
    };


    /**
     * Process search results
     * @param results
     */
    this.processSearchResults = function (results) {
        console.log(results);
        this.searchResults = results;
        var html = '';

        var qualities = this.qualitiesSelect();
        var folders = this.folderSelect();

        results.forEach(function (show, index) {
            var banner = '';

            show.images.forEach(function (image) {
                if (image.coverType === 'banner') {
                    banner = image.url;
                }
            });

            if (banner === '' && show.images.length > 0) {
                banner = show.images[0].url;
            }


            html += '<div class="show" data-index="' + index + '" >';
            //show banner user to click on it
            html += '   <div class="banner" style="background-image: url(\'' + banner + '\')">';
            html += '       <div class="title">' + show.title + ' - '+show.year+'</div>';
            html += '   </div>';

            //show info
            html += '   <div class="info">';
            html += '       <h3>Summary</h3><p>' + show.overview + '</p>';
            html += '       <h3>Seasons</h3>';
            html += '       <div class="seasons">';

            if (show.seasons !== 'undefined') {
                show.seasons.forEach(function (season) {
                    var monitored = season.monitored;
                    html += '<div class="season">';
                    html += '   <label><input type="checkbox" class="show-season" data-index="' + index + '" data-season="' + season.seasonNumber + '" checked/> Season ' + season.seasonNumber + '</label>';
                    html += "</div>";
                });
            }

            html += '       </div>';
            html += '       <div class="add">';
            html += '           ' + qualities;
            html += '           ' + folders;
            html += '           <button class="add-show btn btn-primary" data-index="' + index + '">Add</button>';
            html += '       </div>';
            html += '   </div>';

            html += '</div>';
        });

        $('#results').html(html);
    }


    /**
     * Builds the quality drop down
     * @return {string}
     */
    this.qualitiesSelect = function () {
        var html = '';
        html += '<select class="quality">';

        this.qualities.forEach(function (quality) {
            html += '<option value="' + quality.id + '">' + quality.name + '</option>';
        });

        html += '</select>';

        return html;
    }

    /**
     * Builds the quality drop down
     * @return {string}
     */
    this.folderSelect = function () {
        var html = '';
        html += '<select class="folder">';

        this.folders.forEach(function (folder) {
            html += '<option value="' + folder.path + '">' + folder.path + '</option>';
        });

        html += '</select>';

        return html;
    }

}