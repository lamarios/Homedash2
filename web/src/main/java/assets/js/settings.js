$(document).ready(function(){
	
	
	
	$('input[type="checkbox"]').change(function(){
		updateCheckboxDependant($(this));
	});
	
	$('input[type="checkbox"]').each(function(){
		updateCheckboxDependant($(this));
	});
	
	function updateCheckboxDependant(checkbox){
		//alert(checkbox.prop('checked'));
		if(checkbox.prop('checked')){
			$('div[data-dependent="'+checkbox.attr('name')+'"]').slideDown('fast');
		}else{
			$('div[data-dependent="'+checkbox.attr('name')+'"]').slideUp('fast');
		}
	}
});