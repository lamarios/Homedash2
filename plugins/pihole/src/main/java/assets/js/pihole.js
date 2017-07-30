function pihole(moduleId) {

    this.moduleId = moduleId;

    this.onConnect = function () {

    };

    this.documentReady = function (size) {

    };

    this.onMessage = function (size, command, message, extra) {
        this.processData(message);
    }

    this.processData = function (message) {

        var root = rootElement(this.moduleId);

        root.find('.ads-blocked p').html(message.ads_blocked_today);
        root.find('.dns-queries p').html(message.dns_queries_today);
        root.find('.ads-percentage p')
            .html(message.ads_percentage_today.toFixed(2) + '<small>%</small>');
        root.find('.domains-blocked p').html(message.domains_being_blocked);
    };

}