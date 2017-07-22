function plex(moduleId) {

    this.moduleId = moduleId;

    this.onConnect = function () {

    };

    this.documentReady = function (size) {

    };

    this.onMessage = function (size, command, message, extra) {
        switch(size){
            case '1x1':
                this.onMessage1x1(command, message, extra);
                break;
        }
    };


    this.onMessage1x1 = function(command, message, extra){
        var root = rootElement(this.moduleId);
        var polygon = root.find('svg.icon polygon');2
        if(message  == '0'){
            polygon.removeClass('playing');
        }else{
            polygon.addClass('playing');
        }
    }



}