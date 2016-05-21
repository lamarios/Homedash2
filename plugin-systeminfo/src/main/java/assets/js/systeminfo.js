function systeminfo(moduleId) {
	
	this.moduleId = moduleId;
	
	this.rootElement = function(){
		return $('#layout .module[data-module="#'+this.moduleId+'"]')
	};
	
	this.onConnect = function(){
		
	}
	
	this.documentReady = function() {
	}
	
	this.onMessage = {
			'2x1': function(command, message, extra){
				console.log('2x1');
			},
			'1x1': function(command, message, extra){
				console.log('1x1');
			}
}
	
}