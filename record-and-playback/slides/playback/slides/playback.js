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

setTitleOnThumbnails = function() {
  $(".thumbnail").each(function() {
    var src = $(this).attr("src")
    if (src !== undefined) {
      var num = "?";
      var name = "undefined";
      var match = src.match(/slide-(.*).png/)
      if (match) { num = match[1]; }
      match = src.match(/([^/]*)\/slide-.*\.png/)
      if (match) { name = match[1]; }
      $(this).attr("title", name + " (" + num + ")")
    }
  });
}

goToSlide = function(time) {
  return function() {
    var pop = Popcorn("#audioRecording");
    pop.currentTime(time);
  }
}

generateThumbnails = function() {
  if (window.XMLHttpRequest) {// code for IE7+, Firefox, Chrome, Opera, Safari
    xmlhttp=new XMLHttpRequest();
  } else {// code for IE6, IE5
    xmlhttp=new ActiveXObject("Microsoft.XMLHTTP");
  }
  xmlhttp.open("GET",SLIDES_XML,false);
  xmlhttp.send();
  xmlDoc = xmlhttp.responseXML;
  xmlList = xmlDoc.getElementsByTagName("image");
  for (var i = 0; i < xmlList.length; i++) {
    var img = new Image();
    img.src = xmlList[i].getAttribute("src");
    if (img.src.match(/\/slides\/.*slide-.*\.png/)) {
      img.onclick = goToSlide(xmlList[i].getAttribute("in"));
      img.className = "thumbnail";
      document.getElementById("thumbnails").appendChild(img);
    }
  }
  setTitleOnThumbnails();
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
