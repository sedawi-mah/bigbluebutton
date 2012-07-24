#!/bin/bash

sudo cp -r slides/playback/slides/* /var/bigbluebutton/playback/slides/
sudo cp core/lib/recordandplayback/generators/video.rb /usr/local/bigbluebutton/core/lib/recordandplayback/generators/
sudo cp slides/scripts/process/slides.rb /usr/local/bigbluebutton/core/scripts/process/
sudo cp slides/scripts/publish/slides.rb /usr/local/bigbluebutton/core/scripts/publish/
