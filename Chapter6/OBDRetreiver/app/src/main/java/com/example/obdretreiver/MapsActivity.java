package com.example.obdretreiver;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson.JacksonFactory;
import com.google.gdata.client.spreadsheet.*;
import com.google.gdata.data.spreadsheet.*;
import com.google.gdata.util.*;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.google.api.client.http.HttpTransport;

public class MapsActivity extends FragmentActivity {

    private GoogleMap mMap; // Might be null if Google Play services APK is not available.
    SpreadsheetService service;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        setUpMapIfNeeded();

        new RetrieveSpreadsheets().execute();
    }

    class RetrieveSpreadsheets extends AsyncTask<Void, Void, List<WorksheetEntry>> {

        @Override
        protected List<WorksheetEntry> doInBackground(Void... params) {
            try {
                service = new SpreadsheetService("MySpreadsheetIntegration-v1");

                HttpTransport httpTransport = new NetHttpTransport();
                JacksonFactory jsonFactory = new JacksonFactory();
                String[] SCOPESArray = {"https://spreadsheets.google.com/feeds", "https://spreadsheets.google.com/feeds/spreadsheets/private/full", "https://docs.google.com/feeds"};
                final List SCOPES = Arrays.asList(SCOPESArray);

                KeyStore keystore = KeyStore.getInstance("PKCS12");
                keystore.load(getResources().openRawResource(R.raw.piandroidprojects), "notasecret".toCharArray());
                PrivateKey key = (PrivateKey) keystore.getKey("privatekey", "notasecret".toCharArray());

                GoogleCredential credential = new GoogleCredential.Builder()
                        .setTransport(httpTransport)
                        .setJsonFactory(jsonFactory)
                        .setServiceAccountPrivateKey(key)
                        .setServiceAccountId("14902682557-05eecriag0m9jbo50ohnt59sest5694d@developer.gserviceaccount.com")
                        .setServiceAccountScopes(SCOPES)
                        .build();

                service.setOAuth2Credentials(credential);
                URL SPREADSHEET_FEED_URL = new URL("https://spreadsheets.google.com/feeds/spreadsheets/private/full");
                SpreadsheetFeed feed = service.getFeed(SPREADSHEET_FEED_URL, SpreadsheetFeed.class);
                List<SpreadsheetEntry> spreadsheets = feed.getEntries();

                return spreadsheets.get(0).getWorksheets();

            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (ServiceException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (GeneralSecurityException e) {
                e.printStackTrace();
            }

            return null;
        }

        protected void onPostExecute(final List<WorksheetEntry> worksheets) {
            if(worksheets == null || worksheets.size() == 0) {
                Toast.makeText(MapsActivity.this, "Nothing saved yet", Toast.LENGTH_LONG).show();
            } else {
                final List<String> worksheetTitles = new ArrayList<String>();
                for(WorksheetEntry worksheet : worksheets) {
                    worksheetTitles.add(worksheet.getTitle().getPlainText());
                }

                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(MapsActivity.this);
                alertDialogBuilder.setTitle("Select a worksheet");
                alertDialogBuilder.setAdapter(
                        new ArrayAdapter<String>(
                                MapsActivity.this,
                                android.R.layout.simple_list_item_1,
                                worksheetTitles.toArray(new String[0])),
                        new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                new RetrieveWorksheetContent().execute(worksheets.get(which));
                            }
                        });

                alertDialogBuilder.create().show();
            }
        }
    }

    class RetrieveWorksheetContent extends AsyncTask<WorksheetEntry, Void, List<List<Object>>> {

        @Override
        protected List<List<Object>> doInBackground(WorksheetEntry... params) {
            WorksheetEntry worksheetEntry = params[0];
            URL listFeedUrl= worksheetEntry.getListFeedUrl();
            List<List<Object>> values = new ArrayList<List<Object>>();
            try {
                ListFeed feed = service.getFeed(listFeedUrl, ListFeed.class);
                for(ListEntry entry : feed.getEntries()) {
                    List<Object> rowValues = new ArrayList<Object>();
                    for (String tag : entry.getCustomElements().getTags()) {
                        Object value = entry.getCustomElements().getValue(tag);
                        rowValues.add(value);
                    }
                    values.add(rowValues);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ServiceException e) {
                e.printStackTrace();
            }
            return values;
        }

        @Override
        protected void onPostExecute(List<List<Object>> values) {

            setUpMap(values);
            super.onPostExecute(values);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        setUpMapIfNeeded();
    }

    /**
     * Sets up the map if it is possible to do so (i.e., the Google Play services APK is correctly
     * installed) and the map has not already been instantiated.. This will ensure that we only ever
     * call {@link #setUpMap()} once when {@link #mMap} is not null.
     * <p/>
     * If it isn't installed {@link SupportMapFragment} (and
     * {@link com.google.android.gms.maps.MapView MapView}) will show a prompt for the user to
     * install/update the Google Play services APK on their device.
     * <p/>
     * A user can return to this FragmentActivity after following the prompt and correctly
     * installing/updating/enabling the Google Play services. Since the FragmentActivity may not
     * have been completely destroyed during this process (it is likely that it would only be
     * stopped or paused), {@link #onCreate(Bundle)} may not be called again so we should call this
     * method in {@link #onResume()} to guarantee that it will be called.
     */
    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (mMap == null) {
            // Try to obtain the map from the SupportMapFragment.
            mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
                    .getMap();
            // Check if we were successful in obtaining the map.
            if (mMap != null) {
                setUpMap();
            }
        }
    }

    /**
     * This is where we can add markers or lines, add listeners or move the camera. In this case, we
     * just add a marker near Africa.
     * <p/>
     * This should only be called once and when we are sure that {@link #mMap} is not null.
     */
    private void setUpMap() {
        //comment out
        //mMap.addMarker(new MarkerOptions().position(new LatLng(0, 0)).title("Marker"));
    }

    private void setUpMap(List<List<Object>> values) {
        for(List<Object> value : values) {
            String title = values.get(0).toString();
            try {
                double latitude = Double.parseDouble(value.get(1).toString());
                double longitude = Double.parseDouble(value.get(2).toString());
                if (latitude != 0 && longitude != 0)
                    mMap.addMarker(new MarkerOptions().position(new LatLng(latitude, longitude))).setTitle(title);
            } catch(NumberFormatException ex) {

            }
        }
    }
}
