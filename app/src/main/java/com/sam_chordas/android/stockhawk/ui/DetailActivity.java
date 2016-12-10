package com.sam_chordas.android.stockhawk.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.sam_chordas.android.stockhawk.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

// class for launching plot of a stock
public class DetailActivity extends Activity {
    LineDataSet dataset;
    LineChart lineChart;
    ArrayList<String> labels = new ArrayList<>();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        Intent myIntent = getIntent();
        String symbol = myIntent.getExtras().getString("symbol");
        lineChart = (LineChart)findViewById(R.id.chart);

        FetchStockQuotes fetchStockTask = new FetchStockQuotes();
        // fetching stock data corresponding to symbol and display plot
        fetchStockTask.execute(symbol);
    }

    private static final String TAG_FETCH = "FETCH_STOCK_QUOTES";
    class FetchStockQuotes extends AsyncTask<String, String, String>
    {
        FetchStockQuotes()
        {
        }

        public class MyXAxisValueFormatter implements IAxisValueFormatter {
            private String[] mValues;

            public MyXAxisValueFormatter(String[] values) {
                this.mValues = values;
            }

            @Override
            public String getFormattedValue(float value, AxisBase axis) {
                // "value" represents the position of the label on the axis (x or y)
                return mValues[(int) value];
            }
        }

        @Override
        protected void onPreExecute()
        {
            Log.d(TAG_FETCH, "PRE_EXECUTED");

        }
        @Override
        protected void onPostExecute(String v) {
            LineData data = new LineData(dataset);
            lineChart.setData(data);
            lineChart.invalidate();

            XAxis xAxis = lineChart.getXAxis();

            String[] labelsArr = new String[labels.size()];
            labelsArr = labels.toArray(labelsArr);
            xAxis.setValueFormatter(new MyXAxisValueFormatter(labelsArr));
            xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        }

        private boolean checkInternetConnection() {
            ConnectivityManager check = (ConnectivityManager)getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo info = check.getActiveNetworkInfo();

            if (info.getState() == NetworkInfo.State.CONNECTED) {
                return true;
            }

            return false;
        }
        @Override
        protected String doInBackground(String... symbol)
        {
            if (checkInternetConnection() == false)
            {
                Log.d(TAG_FETCH, "NOT CONNECTED");
                return null;
            }
            else
            {
                Log.d(TAG_FETCH, "CONNECTED");
                try {
                    String urlStr="https://query.yahooapis.com/v1/public/yql";
                    String Search="format";
                    String SearchVal="json";
                    String QueryKey="q";
                    String Diag ="diagnostics";
                    String DiagVal="true";
                    String Env="env";
                    String EnvVal="store://datatables.org/alltableswithkeys";
                    String Call="callback";
                    String CallVal="";
                    Uri buildUri;
                    Log.d("SYMBOL", symbol[0]);

                    Calendar c = Calendar.getInstance();
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                    String endDate = sdf.format(c.getTime());
                    c.add(Calendar.MONTH, -1);
                    String startDate = sdf.format(c.getTime());
                    String query="Select * from yahoo.finance.historicaldata where symbol ='"
                              + symbol[0] + "' and startDate = '" +
                              startDate + "' and endDate = '" + endDate + "'";

                    buildUri=Uri.parse(urlStr).buildUpon()
                            .appendQueryParameter(QueryKey,query)
                            .appendQueryParameter(Search,SearchVal)
                            .appendQueryParameter(Diag,DiagVal)
                            .appendQueryParameter(Env,EnvVal)
                            .appendQueryParameter(Call,CallVal)
                            .build();
                    Log.d(TAG_FETCH, buildUri.toString());

                    URL url = new URL(buildUri.toString());

                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.connect();

                    InputStream in = null;
                    int resCode = conn.getResponseCode();
                    if (resCode == HttpURLConnection.HTTP_OK) {
                        in = conn.getInputStream();
                    }
                    BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                    String webPage = "", data = "";
                    while ((data = reader.readLine()) != null) {
                        webPage += data + '\n';
                    }
                    Log.d(TAG_FETCH, webPage);
                    try {
                        ArrayList<Entry> entries = new ArrayList<Entry>();
                        JSONObject jsObj = new JSONObject(webPage);
                        JSONArray jsArray = jsObj.getJSONObject("query").getJSONObject("results").getJSONArray("quote");
                        for (int i = jsArray.length()-1, k = 0; i > 0; i--) {
                            JSONObject jsQuoteObj = jsArray.getJSONObject(i);
                            float value = ((float) Float.parseFloat(jsQuoteObj.getString("Close")));
                            Log.d("QUOTE VALUE", String.valueOf(value));
                            entries.add(new Entry((float)k, value));

                            String date = jsQuoteObj.getString("Date");
                            SimpleDateFormat dispFormat = new SimpleDateFormat("dd-MM");
                            SimpleDateFormat parseFormat = new SimpleDateFormat("yyyy-MM-dd");
                            String dispLabel = null;
                            try {
                                Date myDate = parseFormat.parse(date);
                                dispLabel = dispFormat.format(myDate);
                            } catch (ParseException e) {
                                e.printStackTrace();
                            }
                            labels.add(dispLabel);
                            k++;
                        }
                        dataset = new LineDataSet(entries, "Stock Values");
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                catch (MalformedURLException e) {
                    e.printStackTrace();
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return null;
        }
    }
}
