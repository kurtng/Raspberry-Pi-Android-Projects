import json
import gspread
from datetime import datetime
from oauth2client.client import SignedJwtAssertionCredentials

json_key = json.load(open('piandroidprojects.json'))
scope = ['https://spreadsheets.google.com/feeds']

credentials = SignedJwtAssertionCredentials(json_key['client_email'], json_key['private_key'], scope)

gc = gspread.authorize(credentials)
t = datetime.now()
sh = gc.open("CAR_OBD_SHEET").add_worksheet(str(t.year) + "_" + str(t.month) + "_" + str(t.day) + "_" + str(t.hour) + "_" + str(t.minute) + "_" + str(t.second), 100, 20)

sh.update_cell(1, 1, 0.23)
