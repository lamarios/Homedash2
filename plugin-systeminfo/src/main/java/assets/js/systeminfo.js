function systeminfo(moduleId) {

	this.moduleId = moduleId;

	this.cpuHistory = [];
	this.ramHistory = [];
	this.width = 1;
	
	this.onConnect = function() {

	}

	this.documentReady = function() {
	}

	this.onMessage_2x1 = function(command, message, extra) {
		this.width = 2;
		this.processData(message);
	}
	this.onMessage_1x1 = function(command, message, extra) {
		this.width = 1;
		this.processData(message);
	}

	this.processData = function(obj) {


		var root = rootElement(this.moduleId);

		this.cpuHistory = obj.cpuInfo;
		this.ramHistory = obj.ramInfo;

		if (this.ramHistory.length > 0 && this.cpuHistory.length > 0) {
			

			
			var cpu = this.cpuHistory[this.cpuHistory.length - 1].cpuUsage;
			var ram = this.ramHistory[this.ramHistory.length - 1];

			var cpuText = root.find('.cpu-txt');
			var ramText = root.find('.ram-txt');

			console.log(root.html());

			cpuText.html(cpu);

			
			switch(this.width){
			case 1:
				
				ramText.html(ram.percentageUsed);
				break;
			case 2:
				ramText.html(this.humanFileSize(ram.usedRam, ram.maxRam, false));
				break;
			}
			

			root.find('.cpu-svg').attr('d', this.cpuArrayToSVGGraph(this.cpuHistory));
			root.find('.ram-svg').attr('d', this.ramArrayToSVGGraph(this.ramHistory));
		}


	};

	this.cpuArrayToSVGGraph = function(array) {
		var html = [];
		html.push('M0,100');
		var lastIndex = 0;
		var step = (100) / array.length;
		html.push(' L0,', 100 - array[0].cpuUsage);
		$.each(array, function(index, cpuInfo) {

			html.push(' L', (index + 1) * step, ',', 100 - cpuInfo.cpuUsage);
			lastIndex = index * step;
		});
		html.push(' L', 100, ',100 Z');
		return html.join('');
	}

	this.ramArrayToSVGGraph = function(array) {
		var html = [];
		html.push('M0,100');
		var lastIndex = 0;
		var step = (100) / array.length;
		html.push(' L0,', 100 - array[0].percentageUsed);
		$.each(array, function(index, ramInfo) {

			html.push(' L', (index + 1) * step, ',', 100 - ramInfo.percentageUsed);
			lastIndex = index * step;
		});
		html.push(' L', 100, ',100 Z');
		return html.join('');
	}
	
	this.humanFileSize = function (used, max, si) {
		var thresh = si ? 1000 : 1024;
		if (max < thresh)
			return max + ' B';
		var units = si ? [ 'kB', 'MB', 'GB', 'TB', 'PB', 'EB', 'ZB', 'YB' ] : [ 'KiB', 'MiB', 'GiB', 'TiB', 'PiB', 'EiB', 'ZiB', 'YiB' ];
		var u = -1;
		do {
			used /= thresh;
			max /= thresh;
			++u;
		} while (max >= thresh);
		return used.toFixed(1)+'/'+max.toFixed(1) + ' ' + units[u];
	}

}