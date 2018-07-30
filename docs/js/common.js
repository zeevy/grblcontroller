jQuery('#siteNavigation').load('navigation.html', function(){
    jQuery("nav.navbar-fixed-top").autoHidingNavbar({animationDuration: 250, showOnUpscroll: true, showOnBottom: false});
});
