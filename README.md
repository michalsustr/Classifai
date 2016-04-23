Classifai recognition app
=========================
An android app for real time object classification. This code can be freely extended for your special use-case.

Please see http://michal.sustr.sk/classifai for more information.

## License
Noncommercial license. You may not use this work for commercial purposes.

If you want to use it for commercial purposes, please let me know.

Commercial users should use the whole Classifai package.

https://creativecommons.org/licenses/by-nc-sa/4.0/

## TODO

Recognize:
- [X] setup camera UI
- [X] save a frame from the stream
- [X] interface with caffe
- [X] initialize - find what is optimal FPS processing
- [X] recognition in progress indicator
- [ ] multiframe recognition using avg over scores
- [ ] calculate probability with bayes using prior probability of labels
- [ ] splash loading screen
- [ ] use accelerometer to find when it is appropriate to do processing
- [ ] direct loading of captured file by caffe, not via storing to sd card
- [ ] storing snapshots with probabilities
- [ ] caffe using GPU? (probably not going to happen)

Record:
- [ ] use accelerometer for recording
- [ ] add clock for recording
- [ ] check for available space before recording

Both:
- [ ] allow turning light on/off
- [ ] touch to focus camera
- [ ] create install procedure which checks for available space (needed to store snapshots and models)
- [ ] multi model support

Future:
- [ ] API calls for finding available models
- [ ] QR code reading for getting record/model info
- [ ] GPS localization to get models automatically?