#!/usr/bin/env python

import obd_io
from datetime import datetime
import time
import threading
import commands
import time
from gps import *
import math
import json
import gspread
from oauth2client.client import SignedJwtAssertionCredentials

gpsd = None

class GpsPoller(threading.Thread):
  def __init__(self):
    threading.Thread.__init__(self)
    global gpsd 
    gpsd = gps(mode=WATCH_ENABLE) 

  def run(self):
    global gpsd
    while True:
      gpsd.next()


class OBD_Sender():
    def __init__(self):
        self.port = None
        self.sensorlist = [3,4,5,12,13,31,32]

    def connect(self):
        self.port = obd_io.OBDPort("/dev/rfcomm0", None, 2, 2)
        if(self.port):
            print "Connected to "+str(self.port)

    def is_connected(self):
        return self.port

    def get_data(self):
        if(self.port is None):
            return None
        current = 1
        while 1:
            cell_list = []

            localtime = datetime.now()
            cell = sh.cell(current, 1)
            cell.value = localtime
            cell_list.append(cell)

            try:
                gpsd.next()
            except:
                print "gpsd.next() error"

            cell = sh.cell(current, 2)
            cell.value = gpsd.fix.latitude
            cell_list.append(cell)

            cell = sh.cell(current, 3)
            cell.value = gpsd.fix.longitude
            cell_list.append(cell)

            column = 4
            for index in self.sensorlist:
                (name, value, unit) = self.port.sensor(index)
                cell = sh.cell(current, column)
                cell.value = value
                cell_list.append(cell)
                column = column + 1

            try:
                sh.update_cells(cell_list)
                print "sent data"
            except:
                print "update_cells error"

            current = current + 1
            time.sleep(10)

json_key = json.load(open('/home/pi/pyobd-pi/piandroidprojects.json'))
scope = ['https://spreadsheets.google.com/feeds']

credentials = SignedJwtAssertionCredentials(json_key['client_email'], json_key['private_key'], scope)

while True:
    try:
        gc = gspread.authorize(credentials)
        break
    except:
        print "Error in GoogleDocs authorize"

t = datetime.now()
sh = gc.open("CAR_OBD_SHEET").add_worksheet(str(t.year)+"_"+str(t.month)+"_"+str(t.day)+"_"+str(t.hour)+"_"+str(t.minute)+"_"+str(t.second), 100, 20)

gpsp = GpsPoller()
gpsp.start()

o = OBD_Sender()
o.connect()
time.sleep(5)
o.connect()
time.sleep(5)
o.get_data()
