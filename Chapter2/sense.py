#!/usr/bin/python

import sys
import Adafruit_DHT
import MySQLdb

humidity, temperature = Adafruit_DHT.read_retry(Adafruit_DHT.DHT11, 4)
#temperature = temperature * 1.8 + 32 # fahrenheit
print str(temperature) + " " + str(humidity)
if humidity is not None and temperature is not None:
    db = MySQLdb.connect("localhost", "root", "admin", "measurements")
    curs = db.cursor()
    try:
        sqlline = "insert into measurements values(NOW(), {0:0.1f}, {1:0.1f});".format(temperature, humidity)
        curs.execute(sqlline)

        curs.execute ("DELETE FROM measurements WHERE ttime < NOW() - INTERVAL 180 DAY;")
        db.commit()
        print "Data committed"
    except MySQLdb.Error as e:
        print "Error: the database is being rolled back" + str(e)
        db.rollback()
else:
    print "Failed to get reading. Try again!"
