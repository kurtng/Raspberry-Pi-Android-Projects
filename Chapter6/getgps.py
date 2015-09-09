#! /usr/bin/python

from gps import *
import math

gpsd = gps(mode=WATCH_ENABLE) #starting the stream of info

count = 0
while count < 10:  # wait max 50 seconds
    gpsd.next()
    if gpsd.fix.latitude != 0 and not math.isnan(gpsd.fix.latitude) :
        print gpsd.fix.latitude,gpsd.fix.longitude
        break
    count = count + 1
    time.sleep(5)
