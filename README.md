## CrowdMeasure
----------


An android application for **[rogue AP detection][1]** with the crowd wisdom. The basic idea is: 

> *For a specific AP, the received signal strength (rss) measurements from several different locations facilitate a fingerprint. A different fingerprint would be observed for a rogue AP.*

Currently, it is only used to **collect** and **record** rss meausrements. The detection logic is done off-line with matlab.


## Requirements 
----------

Android with:

 - minSdkVersion 14
 - targetSdkVersion 22


## Installation
----------

1. Get the android apk:

```
git clone git@https://github.com/ztqsurfing/CrowdMeasure.git
cd CrowdMeasure/app/
``` 

2. Copy app-release.apk to your phone and install it.


## Start
----------
1. Click the `LOAD THE FLOOR PLAN` button to load a picture for target sensing area;

2. Press on the loaded floor plan for at least *1.5sec* to **mark** the current location of your device;

3. Click the `MEASURE` button to begin rss measuring and recording. For the first round, you would need to input the **target AP** (a prefix is enough) and **measureing cycles** you prefer;

4. Repeat 2, 3 to take samples from **different locations**. The record can be found in the app's directory. A file would be generated **for each round** of measuring.


## Screenshots
----------

* Measuring Config

![](https://raw.githubusercontent.com/ztqsurfing/CrowdMeasure/master/app/screenshot/screenshot1.jpg)

* RSS Measuring

![](https://raw.githubusercontent.com/ztqsurfing/CrowdMeasure/master/app/screenshot/screenshot2.jpg)


  [1]: https://en.wikipedia.org/wiki/Rogue_access_point
