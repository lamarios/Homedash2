$(document).ready(function () {


    $('input[type="checkbox"]').change(function () {
        updateCheckboxDependant($(this));
    });

    $('input[type="checkbox"]').each(function () {
        updateCheckboxDependant($(this));
    });

    $('#generate-key').click(function () {

        generateKey();
        return false;
    });

    if ($.trim($('#remote_api_key').val()).length == 0) {
        generateKey();
    }

    function updateCheckboxDependant(checkbox) {
        //alert(checkbox.prop('checked'));
        if (checkbox.prop('checked')) {
            $('div[data-dependent="' + checkbox.attr('name') + '"]').slideDown('fast');
        } else {
            $('div[data-dependent="' + checkbox.attr('name') + '"]').slideUp('fast');
        }
    }

    /**
     * Generate api key
     */
    function generateKey() {
        $.get('/generate-api-key', function (key) {
            $('#remote_api_key').val(key);
        });
    }
});