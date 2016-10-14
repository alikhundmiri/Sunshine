package com.example.android.sunshine.app;

import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.text.format.Time;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.Inflater;

/**
 * Created by alikhundmiri on 26/08/16.
 */
public class ForcastFragment extends Fragment {

    public ForcastFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    inflater.inflate(R.menu.forcastfragment, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_refresh){
            FetchWeatherTask weatherTask = new FetchWeatherTask();
            weatherTask.execute("1269843");
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        String[] forcastArray = {
                "Monday - Sunny - 34/21",
                "Tuesday - Cloudy - 42/24",
                "Wednesday - Foggy - 45/32",
                "Thursday - Drizzling - 21/22",
                "Friday  - Stormy-ish - 34/55",
                "Saturday - Rainy - 28/43",
                "Sunday - Meh - 33/33",
                "Monday - Clouded - 36/11",
                "Tuesday - plain - 42/24",
                "Wednesday - meh - 15/22",
                "Thursday - rain - 11/22",
                "Friday  - Stormish - 14/15",
                "Saturday - Rain - 28/23",
                "Sunday - Stroke - 33/23",
                "Monday - rain - 24/11",
                "Tuesday - Cloud - 40/24",
                "Wednesday - drizzling - 15/32",
                "Thursday - sunny - 41/52",
                "Friday  - foggy - 32/35",
                "Saturday - drizzling - 21/61",
                "Sunday - winter is coming - 13/31"
        };

        List<String> weekforcast = new ArrayList<String>(
                Arrays.asList(forcastArray));

        ArrayAdapter<String> mForecastAdapter = new ArrayAdapter<String>(
                getActivity(),
                R.layout.list_item_forcast,
                R.id.list_item_forcast_textview,
                weekforcast);

        ListView listView = (ListView) rootView.findViewById(R.id.listview_forcast);
        listView.setAdapter(mForecastAdapter);

        return rootView;
    }

    public class FetchWeatherTask extends AsyncTask <String, Void, String[]> {

        private final String LOG_TAG = FetchWeatherTask.class.getSimpleName();
        /** The date/time conversion code is going to be moved outside the asynctask later,
        * so for convenience we're breaking it out into its own method now.
        **/
        private String getReadableDateString(long time){
        // Because the API returns a unix timestamp (measured in seconds),
        // it must be converted to milliseconds in order to be converted to valid date.
            SimpleDateFormat shortenedDateFormat = new SimpleDateFormat("EEE MMM dd");
            return shortenedDateFormat.format(time);
        }

        /**
        * Prepare the weather high/lows for presentation.
        */
            private String formatHighLows(double high, double low) {
                // For presentation, assume the user doesn't care about tenths of a degree.
                long roundedHigh = Math.round(high);
                long roundedLow = Math.round(low);

                String highLowStr = roundedHigh + "/" + roundedLow;
                return highLowStr;
            }

            /**
            * Take the String representing the complete forecast in JSON Format and
            * pull out the data we need to construct the Strings needed for the wireframes.
            *
            * Fortunately parsing is easy:  constructor takes the JSON string and converts it
            * into an Object hierarchy for us.
            */
        private String[] getWeatherDataFromJson(String forecastJsonStr, int numDays)
        throws JSONException {

            // These are the names of the JSON objects that need to be extracted.
                final String OWM_LIST = "list";
                final String OWM_WEATHER = "weather";
                final String OWM_TEMPERATURE = "temp";
                final String OWM_MAX = "temp_max";
                final String OWM_MIN = "temp_min";
                final String OWM_DESCRIPTION = "main";

                JSONObject forecastJson = new JSONObject(forecastJsonStr);
                JSONArray weatherArray = forecastJson.getJSONArray(OWM_LIST);
                // OWM returns daily forecasts based upon the local time of the city that is being
                // asked for, which means that we need to know the GMT offset to translate this data
                // properly.
                // Since this data is also sent in-order and the first day is always the
                // current day, we're going to take advantage of that to get a nice
                // normalized UTC date for all of our weather.
                Time dayTime = new Time();
                dayTime.setToNow();

                // we start at the day returned by local time. Otherwise this is a mess.
                int julianStartDay = Time.getJulianDay(System.currentTimeMillis(), dayTime.gmtoff);

                // now we work exclusively in UTC
                dayTime = new Time();

                String[] resultStrs = new String[numDays];
                for(int i = 0; i < weatherArray.length(); i++) {
                // For now, using the format "Day, description, hi/low"
                String day;
                String description;
                String highAndLow;

                // Get the JSON object representing the day
                JSONObject dayForecast = weatherArray.getJSONObject(i);

                // The date/time is returned as a long.  We need to convert that
                // into something human-readable, since most people won't read "1400356800" as
                // "this saturday".
                long dateTime;
                // Cheating to convert this to UTC time, which is what we want anyhow
                dateTime = dayTime.setJulianDay(julianStartDay+i);
                day = getReadableDateString(dateTime);

                // description is in a child array called "weather", which is 1 element long.
                JSONObject weatherObject = dayForecast.getJSONArray(OWM_WEATHER).getJSONObject(0);
                description = weatherObject.getString(OWM_DESCRIPTION);

                // Temperatures are in a child object called "temp".  Try not to name variables
                // "temp" when working with temperature.  It confuses everybody.
                JSONObject temperatureObject = dayForecast.getJSONObject(OWM_TEMPERATURE);
                double high = temperatureObject.getDouble(OWM_MAX);
                double low = temperatureObject.getDouble(OWM_MIN);

                highAndLow = formatHighLows(high, low);
                resultStrs[i] = day + " - " + description + " - " + highAndLow;
            }

            for (String s : resultStrs) {
                Log.v(LOG_TAG, "Forecast entry: " + s);
            }
        return resultStrs;
        }

        @Override
        protected String[] doInBackground(String... params) {

            if (params.length == 0){
                return null;
            }

            // These two need to be declared outside the try/catch
            // so that they can be closed in the finally block.
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;

            // Will contain the raw JSON response as a string.
            String forecastJsonStr = null;
            String appid = "a6e437c2839cd8f47318dbff1bf214ca";
            int numDays = 7;
            try {
                // Construct the URL for the OpenWeatherMap query
                // Possible parameters are avaiable at OWM's forecast API page, at
                // http://openweathermap.org/API#forecast
                //URL url = new URL("api.openweathermap.org/data/2.5/forecast/city?id=1269843&APPID=a6e437c2839cd8f47318dbff1bf214ca");

                final String FORCAST_BASE_URL = "http://api.openweathermap.org/data/2.5/forecast/city?";
                final String ID_PARAM = "id";
                final String APPID_PARAM = "APPID";
                final String DAYS_PARAM = "cnt";

                Uri Uribuilder =  Uri.parse(FORCAST_BASE_URL).buildUpon()
                        .appendQueryParameter(ID_PARAM, params[0])
                        .appendQueryParameter(APPID_PARAM, appid)
                        .appendQueryParameter(DAYS_PARAM, Integer.toString(numDays))
                        .build();


                URL url = new URL(Uribuilder.toString());

                Log.v(LOG_TAG, "Built Url " + Uribuilder.toString());

                // Create the request to OpenWeatherMap, and open the connection
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                // Read the input stream into a String
                InputStream inputStream = urlConnection.getInputStream();
                StringBuffer buffer = new StringBuffer();
                if (inputStream == null) {
                    // Nothing to do.
                    return null;
                }
                reader = new BufferedReader(new InputStreamReader(inputStream));

                String line;
                while ((line = reader.readLine()) != null) {
                    // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                    // But it does make debugging a *lot* easier if you print out the completed
                    // buffer for debugging.
                    buffer.append(line + "\n");
                }

                if (buffer.length() == 0) {
                    // Stream was empty.  No point in parsing.
                    return null;
                }


                forecastJsonStr = buffer.toString();
                Log.v(LOG_TAG, "Forecast string: " + forecastJsonStr);

            } catch (IOException e) {
                Log.e(LOG_TAG, "Error ", e);
                // If the code didn't successfully get the weather data, there's no point in attemping
                // to parse it.
                return null;
            } finally{
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                        Log.e(LOG_TAG, "Error closing stream", e);
                    }
                }
            }


            // enter try code here
            try {
                return getWeatherDataFromJson(forecastJsonStr, numDays);
            }catch (JSONException e){
                Log.e(LOG_TAG, e.getMessage(),e);
                e.printStackTrace();
            }
            return null;
            // This will only happen if there was an error getting or parsing the forecast.
        }
    }
}