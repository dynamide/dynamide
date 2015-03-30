function makeTzPopUp(buttonID, tzAutoHidePopupID){
    var POPUP_PAD_TOP = 1;
        function tzDropDownMenu(e){
            var $target = $(this);
            var thePopupID = $target.data('tzRelatedPopupID');
            //console.log('thePopup: '+thePopupID);
            var dis = $(thePopupID).css('display');
            if (dis == 'none'){
                $(".tzPopup").hide();
                $(thePopupID).show();
                $(thePopupID).offset(
                    { top:  $target.offset().top+$target.outerHeight()+POPUP_PAD_TOP, 
                      left: $target.offset().left
                    }
                );
            } else {
                 $(".tzPopup").hide();
                 $(thePopupID).hide();
                 $('#theInput').focus(); //TODO: make this a parameter/pubsub/notify.
            }
            e.stopPropagation();
        }
    return $(buttonID).click(tzDropDownMenu).data('tzRelatedPopupID', tzAutoHidePopupID);
}

