function spotify(moduleId) {

    this.moduleId = moduleId;
    this.currentIndex = 0;
    this.clientID;
    this.onConnect = function () {

    };

    this.documentReady = function (size) {
        var root = rootElement(this.moduleId);
        var self = this;
        root.on('click', '.authorize', function () {
            self.spotifyLogin(self.clientID, self.moduleId);
        });

    };

    this.onMessage = function (size, command, message, extra) {
        var root = rootElement(this.moduleId);
        //We need to authenticate
        if (message.clientID != undefined) {
            this.clientID = message.clientID;
            console.log('id', this.clientID);
            root.find('.authentication').addClass('needed');
        } else {

            root.find('.authentication').removeClass('needed');

            var nowPlaying = root.find('.now-playing');
            //we have something
            if (message.progress_ms >= 0) {

                var progress = (message.progress_ms / message.item.duration_ms) * 100;
               nowPlaying.find('.progress').css('width', progress+'%');


                nowPlaying.removeClass('nothing');

                nowPlaying.find('.name').html(message.item.name);

                var artists = [];

                $.each(message.item.artists, function (index, value) {
                    artists.push(value.name);
                });

                nowPlaying.find('.artists').html(artists.join(' - '));

                nowPlaying.find('.album').html(message.item.album.name);

                var imageUrl = "";
                var biggestSize = 0;

                $.each(message.item.album.images, function (index, image) {
                    if (image.width * image.height > biggestSize) {
                        image = imageUrl = image.url;
                        biggestSize = image.width * image.height;
                    }
                });

                if (message.is_playing == false) {
                    nowPlaying.addClass('paused');
                }else {
                    nowPlaying.removeClass('paused');
                }

                nowPlaying.css('background-image', 'url("' + imageUrl + '")');

            } else {
                nowPlaying.addClass('nothing');
            }
        }

    };


    this.spotifyLogin = function (clientID, moduleId) {

        var redirectUri = location.origin + '/external/' + moduleId + '/authorize';

        var url = 'https://accounts.spotify.com/authorize';
        url += '?client_id=' + clientID;
        url += '&response_type=code';
        url += '&state=' + encodeURIComponent(redirectUri);
        url += '&scope=user-read-playback-state';
        url += '&redirect_uri=' + encodeURIComponent(redirectUri);

        prompt('Make sure you add the following redirect uri to your spotify developer console', redirectUri);

        window.open(url);

    };

}