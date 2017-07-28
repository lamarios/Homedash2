function plex(moduleId) {

    this.moduleId = moduleId;
    this.currentIndex = 0;
    this.onConnect = function () {

    };

    this.documentReady = function (size) {

    };

    this.onMessage = function (size, command, message, extra) {


        var root = rootElement(this.moduleId);
        if (message.size == '0') {
            root.find('.now-playing').addClass('nothing');
        } else {
            var video;
            if (message.size == 1) {
                video = message.videos[0];
            } else {
                this.currentIndex++;
                if (message.size > this.currentIndex) {
                    video = message.videos[this.currentIndex];
                } else {
                    video = message.videos[0];
                    this.currentIndex = 0;
                }
            }

            var name = '', art = video.art;
            if (video.type == "episode") {
                if (video.grandparentTitle != '') {
                    name = video.grandparentTitle + ' - ';
                }
                if (video.parentTitle != '') {
                    name += video.parentTitle + ' - ';
                }
                name += video.title;

                if(video.grandparentArt != ''){
                    art = video.grandparentArt;
                }
            }else{
                name = video.title;
            }

            var progress = (video.viewOffset / video.duration)*100;

            var nowPlaying = root.find('.now-playing');
            nowPlaying.removeClass('nothing');
            nowPlaying.css('background-image', "url('" + art + "')");
            nowPlaying.find('.name').html(name);
            nowPlaying.find('.progress').css('width', progress+'%');
            nowPlaying.find('.player').html(video.player.title);
        }
    }


}