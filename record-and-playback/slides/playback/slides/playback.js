goToSlide = function(time) {
  var pop = Popcorn("#audioRecording");
  pop.currentTime(time);
}

getUrlParameters = function() {
  var map = {};
  var parts = window.location.href.replace(/[?&]+([^=&]+)=([^&]*)/gi, function(m,key,value) {
    map[key] = value;
  });
  return map;
}

var params = getUrlParameters();
var MEETINGID = params['meetingId'];
var RECORDINGS = "/slides/" + MEETINGID;
var SLIDES_XML = "/slides/" + MEETINGID + '/slides.xml';

/*
 * Sets the title attribute in a thumbnail.
 */
setTitleOnThumbnail = function($thumb) {
  var src = $thumb.attr("src")
  if (src !== undefined) {
    var num = "?";
    var name = "undefined";
    var match = src.match(/slide-(.*).png/)
    if (match) { num = match[1]; }
    match = src.match(/([^/]*)\/slide-.*\.png/)
    if (match) { name = match[1]; }
    $thumb.attr("title", name + " (" + num + ")")
  }
}

/*
 * Sets the click event in a thumbnail to change the
 * current slide in popcorn.
 */
setChangeSlideOnThumbnail = function($thumb) {
  $thumb.on("click", function() {
    goToSlide($thumb.attr("data-in"));
  });
}

/*
 * Associates popcorn events with a thumbnail to make it
 * active (mark it) when the slide is being shown. 
 */
setMarkThumbnailOnSlideChange = function($thumb) {
  var timeIn = $thumb.attr("data-in");
  var timeOut = $thumb.attr("data-out");
  var pop = Popcorn("#audioRecording");
  pop.code({
    start: timeIn,
    end: timeOut,
    onStart: function( options ) {
      $("#thumbnail-" + options.start).parent().addClass("active");
    },
    onEnd: function( options ) {
      $("#thumbnail-" + options.start).parent().removeClass("active");
    }
  });
}

/*
 * Generates the list of thumbnails using slides.xml
 */
generateThumbnails = function() {
  if (window.XMLHttpRequest) {// code for IE7+, Firefox, Chrome, Opera, Safari
    xmlhttp = new XMLHttpRequest();
  } else {// code for IE6, IE5
    xmlhttp = new ActiveXObject("Microsoft.XMLHTTP");
  }
  xmlhttp.open("GET", SLIDES_XML, false);
  xmlhttp.send();
  xmlDoc = xmlhttp.responseXML;
  xmlList = xmlDoc.getElementsByTagName("image");
  for (var i = 0; i < xmlList.length; i++) {
    img = $(document.createElement('img'));
    var src = xmlList[i].getAttribute("src");
    if (src.match(/\/slides\/.*slide-.*\.png/)) {
      var timeIn = xmlList[i].getAttribute("in");
      var timeOut = xmlList[i].getAttribute("out");
      img.attr("src", src);
      img.attr("id", "thumbnail-" + timeIn);
      img.attr("data-in", timeIn);
      img.attr("data-out", timeOut);
      img.addClass("thumbnail");
      setMarkThumbnailOnSlideChange(img);
      setChangeSlideOnThumbnail(img);
      setTitleOnThumbnail(img);
      // a wrapper around the img
      var div = $(document.createElement('div'));
      div.addClass("thumbnail-wrapper");
      div.append(img);
      $("#thumbnails").append(div);
    }
  }
}

document.addEventListener( "DOMContentLoaded", function() {
  var audio;
  var appName = navigator.appName;
  var appVersion = navigator.appVersion;
  audio = document.getElementById("audioRecording");
  if (appName == "Microsoft Internet Explorer") {
    if (navigator.userAgent.match("chromeframe")) {
      audio.setAttribute('src', RECORDINGS + '/audio/audio.ogg');
      audio.setAttribute('type','audio/ogg');
    } else {
      var message = "To support this playback please install 'Google Chrome Frame', or use other browser: Firefox, Safari, Chrome, Opera";
      var line = document.createElement("p");
      var link = document.createElement("a");
      line.appendChild(document.createTextNode(message));
      link.setAttribute("href", "http://www.google.com/chromeframe")
      link.setAttribute("target", "_blank")
      link.appendChild(document.createTextNode("Install Google Chrome Frame"));
      document.getElementById("chat").appendChild(line);
      document.getElementById("chat").appendChild(link);
    }
  } else if (appVersion.match("Safari") != null && appVersion.match("Chrome") == null) {
    audio.setAttribute('src', RECORDINGS + '/audio/recording.wav');
    audio.setAttribute('type','audio/x-wav');
  } else {
    audio.setAttribute('src', RECORDINGS + '/audio/audio.ogg');
    audio.setAttribute('type','audio/ogg');
  }
  audio.setAttribute('data-timeline-sources', SLIDES_XML);

  generateThumbnails();
}, false);
