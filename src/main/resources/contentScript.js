(function () {
    let frame = document.createElement('iframe');
    frame.src = document.getElementById('header-v6a8oxpf48xfzy0rhjra').getAttribute('data-file');
    frame.border = 0;
    frame.id = 'header-frame-6jk3h6a234g2a3';
    let frameStyle = frame.style;
    frameStyle.position = 'absolute';
    frameStyle.overflow = 'hidden';
    frameStyle.width = '1000px';
    frameStyle.top = '0';
    frameStyle.left = '0';
    frameStyle.zIndex = '100003';
    frameStyle.border = 'none';
    frameStyle.padding = '0';
    frameStyle.margin = '0';

    document.body.insertBefore(frame, document.body.firstChild);

    let oldVal = parseInt(document.getElementsByTagName('html')[0].style.paddingTop) || 0;

    window.addEventListener('message', function (event) {
        document.getElementById('header-frame-6jk3h6a234g2a3').height = event.data;
        document.getElementsByTagName('html')[0].style.paddingTop = (oldVal + event.data) + 'px';

        // TODO push down all position absolute elements and remember their old positions
//		var all = document.querySelector('*');
//		for(var i = 0; i < all.length; i++) {
//		  if (all[i].style.position == 'absolute' && all[i] !== f) {
//		    all[i].style.top = ((parseInt(all[i].style.top) || 0) + event.data) + 'px';
//		  }
//		}
    });
});