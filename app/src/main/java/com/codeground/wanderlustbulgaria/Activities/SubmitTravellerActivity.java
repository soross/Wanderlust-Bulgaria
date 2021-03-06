package com.codeground.wanderlustbulgaria.Activities;

import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.codeground.wanderlustbulgaria.R;
import com.codeground.wanderlustbulgaria.Utilities.DialogWindowManager;
import com.codeground.wanderlustbulgaria.Utilities.ParseUtils.ParseLocation;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlacePicker;
import com.google.android.gms.maps.model.LatLng;
import com.parse.FindCallback;
import com.parse.GetCallback;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;
import com.wdullaer.materialdatetimepicker.date.DatePickerDialog;
import com.wdullaer.materialdatetimepicker.time.TimePickerDialog;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SubmitTravellerActivity extends AppCompatActivity implements View.OnClickListener,
        TimePickerDialog.OnTimeSetListener,
        DatePickerDialog.OnDateSetListener {

    private final int PLACE_PICKER_REQUEST = 1;

    private LocationManager mLocationManager;
    private Double mLongitude;
    private Double mLatitude;
    private String mCity;
    private SimpleDateFormat dateFormatter;
    private Date mDepartureDate;

    private Button mSubmitBtn;
    private Button mMapBtn;
    private AutoCompleteTextView mToLocationSearch;
    private TextView mDateTextField;
    private TextView mLatitudeField;
    private TextView mLongitudeField;
    private EditText mCityField;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_submit_traveller);

        mCityField = (EditText) findViewById(R.id.travellers_city_field);

        mLatitudeField = (TextView) findViewById(R.id.travellers_latitude);
        mLongitudeField = (TextView) findViewById(R.id.travellers_longitude);

        mSubmitBtn = (Button) findViewById(R.id.travellers_submit_btn);
        mMapBtn = (Button) findViewById(R.id.travellers_location_btn);

        mSubmitBtn.setOnClickListener(this);
        mMapBtn.setOnClickListener(this);

        mToLocationSearch = (AutoCompleteTextView) findViewById(R.id.to_location_search);
        initAutoComplete();

        mDateTextField = (TextView) findViewById(R.id.travellers_departure_date);
        mDateTextField.setOnClickListener(this);
        dateFormatter = new SimpleDateFormat("dd-MM-yyyy HH-mm");

        initLocation();
    }

    private void initAutoComplete() {
        ParseQuery<ParseLocation> locationQuery = ParseQuery.getQuery(ParseLocation.class);
        locationQuery.findInBackground(new FindCallback<ParseLocation>() {
            public void done(List<ParseLocation> parseLocations, ParseException e) {
                if (e == null) {
                    ParseLocation[] data = parseLocations.toArray(new ParseLocation[parseLocations.size()]);
                    String[] strings = new String[data.length];
                    for (int i = 0; i < data.length; i++) {
                        strings[i] = data[i].getName();
                    }
                    // Test to see if it was correctly printing out the array I wanted.
                    // System.out.println(Arrays.toString(strings));
                    ArrayAdapter<String> adapter = new ArrayAdapter<String>(getApplicationContext(), R.layout.search_result_traveller_target_location_row, strings);

                    if(parseLocations.size() < 40){
                        mToLocationSearch.setThreshold(1);
                    }
                    else {
                        mToLocationSearch.setThreshold(2);
                    }

                    mToLocationSearch.setAdapter(adapter);
                } else {
                    Log.d("users", "Error: " + e.getMessage());
                }
            }
        });
    }

    private void initLocation() {
        Location currLocation = getLastKnownLocation();

        if(currLocation != null){
            mLongitude =  currLocation.getLongitude();
            mLatitude =  currLocation.getLatitude();
            mLongitudeField.setText("Longitude: "+mLongitude);
            mLatitudeField.setText("Latitude: "+mLatitude);

            updateCity();
        }
    }

    private void updateCity() {
        Geocoder gcd = new Geocoder(this, Locale.getDefault());
        List<Address> addresses = null;
        try {
            addresses = gcd.getFromLocation(mLatitude, mLongitude, 1);

            if (addresses.size() > 0)
            {
                mCity = addresses.get(0).getLocality();
            }
            else
            {
                mCity = getString(R.string.label_undefined_city);
            }

            if(mCityField!=null){
                mCityField.setText(mCity);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void onClick(View v) {
        if(v.getId()==R.id.travellers_location_btn){
            DialogWindowManager.show(this);
            pickLocation();
        }
        if(v.getId()==R.id.travellers_submit_btn){
            submitTraveller();
        }
        if(v.getId()== R.id.travellers_departure_date){
            showDatePicker();
        }
    }

    private void showDatePicker() {
        Calendar now = Calendar.getInstance();
        DatePickerDialog dpd = DatePickerDialog.newInstance(
                this,
                now.get(Calendar.YEAR),
                now.get(Calendar.MONTH),
                now.get(Calendar.DAY_OF_MONTH)
        );
        dpd.vibrate(false);
        dpd.show(getFragmentManager(), "Pick a date:");
    }

    private void showTimePicker() {
        Calendar now = Calendar.getInstance();
        TimePickerDialog tpd = TimePickerDialog.newInstance(
                this,
                now.get(Calendar.HOUR_OF_DAY),
                now.get(Calendar.MINUTE), true
        );
        tpd.vibrate(false);
        tpd.show(getFragmentManager(), "Pick a time:");
    }

    private void submitTraveller() {
        ParseUser user = ParseUser.getCurrentUser();

        final ParseObject traveller = new ParseObject(getString(R.string.db_traveller_dbname));

        traveller.put("origin_user", user);
        traveller.put("origin_user_name", user.getString("first_name") + " " + user.getString("last_name"));

        if(mLatitude !=null && mLongitude != null){
            ParseGeoPoint point = new ParseGeoPoint(mLatitude,mLongitude);
            traveller.put("from_location", point);
        }else if(mCity == null || mCity.isEmpty()){
            Toast.makeText(getApplicationContext(), "Please choose a departure location or city", Toast.LENGTH_SHORT).show();
            return;
        }

        traveller.put("from_city", mCity);

        if(mDepartureDate != null){
            traveller.put("travel_date", mDepartureDate);
        }else{
            Toast.makeText(getApplicationContext(), "Please set date and time for your travel!", Toast.LENGTH_SHORT).show();
        }

        DialogWindowManager.show(this);

        final String locationName = mToLocationSearch.getText().toString();
        ParseQuery<ParseLocation> currentLocation = ParseQuery.getQuery(ParseLocation.class);
        currentLocation.whereEqualTo("name", locationName);
        currentLocation.getFirstInBackground(new GetCallback<ParseLocation>() {
            @Override
            public void done(ParseLocation currentLocationObj, ParseException e) {
                if(e==null && currentLocationObj != null){
                    traveller.put("to_location", currentLocationObj);
                    traveller.put("to_location_name", locationName);
                    traveller.saveInBackground(new SaveCallback() {
                        @Override
                        public void done(ParseException e) {
                            if(e!=null){
                                Log.d("Save",e.getMessage());
                            }
                            DialogWindowManager.dismiss();
                            finish();
                        }
                    });
                }else{
                    DialogWindowManager.dismiss();
                    Toast.makeText(getApplicationContext(), "Location "+ mToLocationSearch.getText().toString() + " was not found!", Toast.LENGTH_SHORT).show();
                }
            }
        });

    }

    private Location getLastKnownLocation() {
        mLocationManager = (LocationManager)getApplicationContext().getSystemService(LOCATION_SERVICE);
        List<String> providers = mLocationManager.getProviders(true);
        Location bestLocation = null;
        for (String provider : providers) {
            try {
                Location l = mLocationManager.getLastKnownLocation(provider);
                if (l == null) {
                    continue;
                }
                if (bestLocation == null || l.getAccuracy() < bestLocation.getAccuracy()) {
                    // Found best last known location: %s", l);
                    bestLocation = l;
                }
            } catch (SecurityException e){

            }

        }
        return bestLocation;
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            if (requestCode == PLACE_PICKER_REQUEST) {
                    Place place = PlacePicker.getPlace(getApplicationContext(),data);
                    LatLng coords = place.getLatLng();
                    mLatitude = coords.latitude;
                    mLongitude = coords.longitude;
                    mLatitudeField.setText("Latitude: "+ mLatitude);
                    mLongitudeField.setText("Longitude: "+ mLongitude);
                    updateCity();
                    DialogWindowManager.dismiss();
            }
        }
        else {
            DialogWindowManager.dismiss();
        }
    }

    private void pickLocation(){
        PlacePicker.IntentBuilder builder = new PlacePicker.IntentBuilder();

        try {
            startActivityForResult(builder.build(this), PLACE_PICKER_REQUEST);
        } catch (GooglePlayServicesRepairableException e) {
            e.printStackTrace();
        } catch (GooglePlayServicesNotAvailableException e) {
            e.printStackTrace();
        }
    }


    @Override
    public void onTimeSet(TimePickerDialog view, int hourOfDay, int minute, int second) {
        Calendar cal = Calendar.getInstance();
        if(mDepartureDate!=null) {
            cal.setTime(mDepartureDate);
        }
        cal.set(Calendar.HOUR_OF_DAY,hourOfDay);
        cal.set(Calendar.MINUTE,minute);
        cal.set(Calendar.SECOND,second);

        mDepartureDate.setTime(cal.getTimeInMillis());
        mDateTextField.setText(dateFormatter.format(mDepartureDate));
    }

    @Override
    public void onDateSet(DatePickerDialog view, int year, int monthOfYear, int dayOfMonth) {
        Calendar newDate = Calendar.getInstance();
        newDate.set(year, monthOfYear, dayOfMonth);
        mDepartureDate = newDate.getTime();
        mDateTextField.setText(dateFormatter.format(mDepartureDate));

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                showTimePicker();
            }
        },500);
    }
}
