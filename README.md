# bluez-adv-monitor
Experiment with new bluez AdvertisementMonitor api

## Platforms
This project targets the raspberry platform running 
raspberry pi os (aka raspbian).

## Requirements
* At least raspbian buster 5.10 is required on raspberry
(the necessary bluez kernel support must be available).

* A pre-release of bluez must be installed on the raspberry.
The minumum commit this project is based on is
  [0c102742a8b4bc8d07545fd91d1a930885cc2867 ](https://git.kernel.org/pub/scm/bluetooth/bluez.git/tree/)
  
* My fork of [bluez-dbus](https://github.com/gpaglia/bluez-dbus)
Most probably the original [bluez-dbus](https://github.com/hypfvieh/bluez-dbus) 
  may work as well, but I am utiizing the gradle build system for my own convenience.

* Python3 installed on the rapberry, with dbus-python also installed.
This is needed to run some simple example while I experiment before 
  translating the code to java.

