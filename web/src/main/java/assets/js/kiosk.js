var timeOut;
var currentIndex = 0;
$(document).ready(function () {
    initSingleModuleWebSocket('kiosk');
var showTime = 30000;

    console.log('modules', MODULES);

    if (Object.keys(MODULES).length > 1) {
        console.log('Setting timeout')
        //initiating the rotation of modules
        timeOut = setInterval(swapModule, showTime);

        $('#module-overlay').click(function(){
            clearInterval(timeOut);
            swapModule();
            timeOut = setInterval(swapModule, showTime);
        });
    }



    setOverlayBackground();
});

function setOverlayBackground() {

    $('#module-overlay').css('background-color', $('.content').css('background-color'));

}

function rootElement(moduleId) {
    return $('.content.size-kiosk');
}


function swapModule() {
    var currentClass = MODULE.constructor.name;

    var content = $('.content.size-kiosk');

    $('#module-overlay').addClass('showing');
    content.addClass('switching');

    currentIndex++;

    if (currentIndex === Object.keys(MODULES).length) {
        currentIndex = 0;
    }
    console.log('Current Index', currentIndex);

    MODULE = MODULES[Object.keys(MODULES)[currentIndex]];



    getModuleContent(MODULE.moduleId, content, currentClass);


}


function getModuleContent(moduleId, container, oldClass) {
    $.get('/module-content/' + moduleId + '/kiosk', function (html) {
        var newClass = MODULE.constructor.name;
        container.html(html);
        setOverlayBackground();
        sendMessage(MODULE.moduleId, "setModule", "");
        container.removeClass(oldClass);
        container.addClass(newClass);
        container.removeClass('switching');
        $('#module-overlay').removeClass('showing');

    });
}