function changePage(e){PAGE=e,sendMessage(-1,"changePage",PAGE),getLayout(),getPages(),$("#pages").removeClass("showing"),typeof Storage!="undefined"&&localStorage.setItem("page",PAGE)}function moveModule(e){if($("#page-move-modal").length==0){var a=[];a.push('<div id="page-move-modal" class="modal fade" tabindex="-1" role="dialog">','<div class="modal-dialog">','<div class="modal-content">','<div class="modal-header">','<button type="button" class="close" data-dismiss="modal" aria-label="Close"><span aria-hidden="true">&times;</span></button>','<h4 class="modal-title">Which page to move this module ?</h4>',"</div>",'<div class="modal-body">','<div class="loader"></div>',"</div>","</div>","</div>","</div>"),$("body").append(a.join(""))}else $("#page-move-modal .modal-body").html('<div class="loader"></div>');$("#page-move-modal").modal("show"),$.getJSON("/pages",function(a){var t=[];$.each(a,function(a,o){o.id!=PAGE&&t.push('<div data-id="',o.id,'" data-module="',e,'" class="page">',o.name,"</div>")}),$("#page-move-modal .modal-body").html(t.join(""))})}function moveModuleToPage(e,a){$.get("/module/"+e+"/move-to-page/"+a,function(){getLayout(),$("#page-move-modal").modal("hide"),$("#edit-layout").removeClass("editing")})}function getPages(){$.getJSON("/pages",function(e){pages2html(e)})}function pages2html(e){var a=[];$.each(e,function(e,t){t.id==PAGE&&$("#page-title .name").html(t.name),a.push('<li data-id="',t.id,'"><span class="page-name">',t.name,'</span> <span class="glyphicon glyphicon-pencil edit-icon" aria-hidden="true"></span>'),t.id>1&&a.push('<span class="glyphicon glyphicon-remove edit-icon" aria-hidden="true"></span>'),a.push("</li>")}),$("#pages ul").html(a.join(""))}function addPage(e){var e=prompt("New page name");e!=void 0&&$.trim(e).length>0&&$.post("/pages/add",{name:e},function(e){pages2html(e)},"json")}function editPage(e){var a=prompt("New name",$('#pages ul li[data-id="'+e+'"] .page-name').html());a!=void 0&&$.trim(a).length>0&&$.post("/pages/edit/"+e,{name:a},function(e){pages2html(e)},"json")}function deletePage(e){confirm("Deleting this page will also delete all the modules on it. Continue ?")&&$.ajax({url:"/page/"+e,type:"DELETE",success:function(e){pages2html(e)},dataType:"json"})}$(document).ready(function(){getPages(),$("#page-title, #pages .close-panel").click(function(){$("#pages").toggleClass("showing")}),$("#pages .icons .edit").click(function(){$("#pages").toggleClass("editing")}),$("#pages .add").click(addPage),$(document).on("click","#pages .edit-icon.glyphicon-pencil",function(){var e=$(this).parents("li").attr("data-id");editPage(e)}),$(document).on("click","#pages .edit-icon.glyphicon-remove",function(){var e=$(this).parents("li").attr("data-id");deletePage(e)}),$(document).on("click","#pages ul li .page-name",function(){var e=$(this).parents("li").attr("data-id");changePage(e)}),$(document).on("click",".gridster .module  li.move",function(){var e=$(this);moveModule(e.attr("data-id"))}),$(document).on("click","#page-move-modal .page",function(){var e=$(this);moveModuleToPage(e.attr("data-module"),e.attr("data-id"))})})