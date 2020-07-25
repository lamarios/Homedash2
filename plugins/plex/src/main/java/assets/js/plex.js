function plex(moduleId) {

    this.moduleId = moduleId;
    this.currentIndex = 0;
    this.onConnect = function () {

    };

    this.documentReady = function (size) {

    };

    this.onMessage = function (size, command, message, extra) {

        var root = rootElement(this.moduleId);
        root.addClass(message.type);
        if (message.videos === undefined || message.videos.length === 0) {
            root.find('.now-playing').addClass('nothing');
        } else {
            var video;
            if (message.videos.length === 1) {
                video = message.videos[0];
            } else {
                this.currentIndex++;
                if (message.videos.size > this.currentIndex) {
                    video = message.videos[this.currentIndex];
                } else {
                    video = message.videos[0];
                    this.currentIndex = 0;
                }
            }

            var nowPlaying = root.find('.now-playing');
            nowPlaying.removeClass('nothing');
            nowPlaying.css('background-image', "url('" + video.image + "')");
            nowPlaying.find('.name').html(video.name);
            nowPlaying.find('.progress').css('width', video.progress + '%');
            nowPlaying.find('.player').html(video.player);
        }
    }

}