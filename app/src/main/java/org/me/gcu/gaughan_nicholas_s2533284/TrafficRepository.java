package org.me.gcu.gaughan_nicholas_s2533284;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

// The Model: gets the traffic data (download, file cache, parse)
// It has no threading of its own
// LoadFeedsTask calls the methods on background thread
public class TrafficRepository {

    // DATEX II feeds
    public static final String UNPLANNED_EVENTS_URL =
            "https://datex2.trafficscotland.org/rest/2.3/publications/UnplannedEvents/Content.xml";
    public static final String ROADWORKS_URL =
            "https://datex2.trafficscotland.org/rest/2.3/publications/CurrentRoadworks/Content.xml";

    // Cache files
    public static final String UNPLANNED_CACHE_FILE = "unplanned.xml";
    public static final String ROADWORKS_CACHE_FILE = "roadworks.xml";

    private final Context context;
    private String clientId;
    private String clientKey;

    public TrafficRepository(Context context) {
        // Application context, so the repository never holds on to an Activity
        this.context = context.getApplicationContext();
        readKeys();
    }

    // Read the DATEX ID/key from local.properties via manifest meta-data
    private void readKeys() {
        try {
            Bundle metaData = context.getPackageManager()
                    .getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA).metaData;
            clientKey = metaData.getString("org.me.gcu.gaughan_nicholas_s2533284.MY_DATEX_KEY");
            clientId = metaData.getString("org.me.gcu.gaughan_nicholas_s2533284.MY_DATEX_ID");
        } catch (PackageManager.NameNotFoundException e) {
            Log.e("TrafficRepository", "Could not read manifest meta-data: " + e);
        }

        // A missing key doesn't throw, it just comes back null, so check
        if (clientId == null || clientKey == null) {
            Log.e("TrafficRepository", "DATEX ID/key missing from local.properties");
        }
    }

    // Downloads one feed and returns the XML
    // null if the download failed
    public String downloadXml(String feedUrl) {
        StringBuilder result = new StringBuilder();
        HttpURLConnection urlConnection = null;
        try {
            URL url = new URL(feedUrl);
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setConnectTimeout(15000);
            urlConnection.setReadTimeout(30000);
            // ADD AUTHORISATION (string with clientID:clientKey)
            urlConnection.setRequestProperty("Authorization",
                    Base64.getEncoder().encodeToString((clientId + ":" + clientKey).getBytes()));

            BufferedReader in = new BufferedReader(
                    new InputStreamReader(urlConnection.getInputStream(), StandardCharsets.UTF_8));
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                result.append(inputLine).append('\n');
            }
            in.close();
        } catch (IOException e) {
            Log.e("TrafficRepository", "Download failed for " + feedUrl + ": " + e);
            return null;
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }

        // Clean up any leading garbage characters
        int i = result.indexOf("<?");

        // Guard: if the download failed, there's no "<?" and i is -1
        if (i < 0) {
            Log.e("TrafficRepository", "No XML downloaded from " + feedUrl);
            return null;
        }
        return result.substring(i);
    }

    // Saves the raw XML to the app's private storage
    // replaces the previous copy
    public void saveCache(String fileName, String xml) {
        try (FileOutputStream fos = context.openFileOutput(fileName, Context.MODE_PRIVATE);
             OutputStreamWriter writer = new OutputStreamWriter(fos, StandardCharsets.UTF_8)) {
            writer.write(xml);
        } catch (IOException e) {
            Log.e("TrafficRepository", "Could not save cache " + fileName + ": " + e);
        }
    }

    // Returns the cached XML
    // null if no cache yet, or it can't be read
    public String readCache(String fileName) {
        File file = new File(context.getFilesDir(), fileName);
        if (!file.exists()) {
            return null;
        }
        StringBuilder result = new StringBuilder();
        try (FileInputStream fis = context.openFileInput(fileName);
             BufferedReader in = new BufferedReader(new InputStreamReader(fis, StandardCharsets.UTF_8))) {
            String line;
            while ((line = in.readLine()) != null) {
                result.append(line).append('\n');
            }
        } catch (IOException e) {
            Log.e("TrafficRepository", "Could not read cache " + fileName + ": " + e);
            return null;
        }
        return result.toString();
    }

    // When the cached copy was saved (ms), or 0 if none
    public long getCacheTime(String fileName) {
        return new File(context.getFilesDir(), fileName).lastModified();
    }

    public List<Situation> parse(String xml, SituationCategory category) {
        return new SituationParser().parse(xml, category);
    }
}
