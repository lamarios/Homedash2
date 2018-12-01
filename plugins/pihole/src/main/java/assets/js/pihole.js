function pihole(moduleId) {

    this.moduleId = moduleId;

    this.onConnect = function () {

    };

    this.documentReady = function (size) {

    };

    this.onMessage = function (size, command, message, extra) {
        if (size !== 'full-screen') {
            this.processData(message);
        } else {
            this.processFullScreen(message);
        }
    }

    this.processData = function (message) {

        var root = rootElement(this.moduleId);

        root.find('.ads-blocked p').html(message.ads_blocked_today);
        root.find('.dns-queries p').html(message.dns_queries_today);
        root.find('.ads-percentage p')
            .html(message.ads_percentage_today.toFixed(2) + '<small>%</small>');
        root.find('.domains-blocked p').html(message.domains_being_blocked);
    };


    this.processFullScreen = function (message) {

        var root = rootElement(this.moduleId);
        var table = root.find('#queries');

        var html = "";
        var self = this;
        message.forEach(function (m) {
            html += "<tr>";
            html += "<td>" + m.date.date.year + "-" + self.pad(m.date.date.month, 2) + "-" + self.pad(m.date.date.day, 2) + ' ' + self.pad(m.date.time.hour, 2) + ':' + self.pad(m.date.time.minute, 2) + ':'
                + self.pad(m.date.time.second, 2) + "</td>";
            // html += "<td>" + m.blocked === true?'yes':'no'+ '</td>';
            html += "<td>" + m.blocked + '</td>';
            html += "<td>" + m.type + '</td>';
            html += "<td>" + m.requestedDomain + '</td>';
            html += "<td>" + m.requestingClient + '</td>';
            html += "<td>" + m.answerTypeString + '</td>';
            html += "</tr>";
        });


        table.html(html);


    }

    this.pad = function (number, digits) {
        return Array(Math.max(digits - String(number).length + 1, 0)).join(0) + number;
    }
}