package delatoan.mapsonly;

import android.*;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.location.Location;
import android.provider.BaseColumns;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationListener;

public class MainActivity extends AppCompatActivity implements
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    SQLiteExample mSQLiteExample;
    Button mSQLSubmitButton;
    Cursor mSQLCursor;
    SimpleCursorAdapter mSQLCursorAdapter;
    private static final String TAG = "SQLActivity";
    SQLiteDatabase mSQLDB;

    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    private TextView mLatText;
    private TextView mLonText;
    private Location mLastLocation;
    private LocationListener mLocationListener;
    private static final int LOCATION_PERMISSION_RESULT = 17;
    private boolean askedPermission = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }
        mLatText = (TextView) findViewById(R.id.lat_placeholder);
        mLonText = (TextView) findViewById(R.id.lon_placeholder);
        mLatText.setText("No Lat available.");
        mLonText.setText("No Lon available.");
        mLocationRequest = LocationRequest.create();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(5000);
        mLocationRequest.setFastestInterval(5000);
        mLocationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                if (location != null) {
                    mLonText.setText(String.valueOf(location.getLongitude()));
                    mLatText.setText(String.valueOf(location.getLatitude()));
                } else {
                    mLatText.setText("No Lat available.");
                    mLonText.setText("No Lon available.");
                }
            }
        };

        mSQLiteExample = new SQLiteExample(this);
        mSQLDB = mSQLiteExample.getWritableDatabase();

        mSQLSubmitButton = (Button) findViewById(R.id.add_btn);
        mSQLSubmitButton.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){
                if (mSQLDB != null){
                    updateLocation();
                    ContentValues vals = new ContentValues();
                    vals.put(DBContract.DemoTable.COLUMN_NAME_TABLE_LONG, ((TextView)findViewById(R.id.lon_placeholder)).getText().toString());
                    vals.put(DBContract.DemoTable.COLUMN_NAME_TABLE_LAT, ((TextView)findViewById(R.id.lat_placeholder)).getText().toString());
                    vals.put(DBContract.DemoTable.COLUMN_NAME_TABLE_STR, ((EditText)findViewById(R.id.entry_item)).getText().toString());
                    mSQLDB.insert(DBContract.DemoTable.TABLE_NAME, null, vals);
                    populateTable();
                }
                else
                    Log.d(TAG, "Can't access db for writing");
            }
        });

        populateTable();
    }

    private void populateTable(){
        if (mSQLDB != null){
            try {
                if (mSQLCursorAdapter != null && mSQLCursorAdapter.getCursor() != null){
                    if (!mSQLCursorAdapter.getCursor().isClosed()){
                        mSQLCursorAdapter.getCursor().close();
                    }
                }
                mSQLCursor = mSQLDB.query(DBContract.DemoTable.TABLE_NAME,
                        new String[]{DBContract.DemoTable._ID, DBContract.DemoTable.COLUMN_NAME_TABLE_STR, DBContract.DemoTable.COLUMN_NAME_TABLE_LONG, DBContract.DemoTable.COLUMN_NAME_TABLE_LAT},
                        null,
                        null,
                        null,
                        null,
                        null);
                ListView SQLListView = (ListView) findViewById(R.id.sql_list);
                mSQLCursorAdapter = new SimpleCursorAdapter(this, R.layout.item_list, mSQLCursor, new String[]{DBContract.DemoTable.COLUMN_NAME_TABLE_STR, DBContract.DemoTable.COLUMN_NAME_TABLE_LAT, DBContract.DemoTable.COLUMN_NAME_TABLE_LONG}, new int[]{R.id.text_item, R.id.lat_item, R.id.lon_item}, 0);
                SQLListView.setAdapter(mSQLCursorAdapter);
            } catch (Exception e) {
                Log.d(TAG, "Error loading from db.");
            }
        }
    }

    @Override
    protected void onStart() {
        mGoogleApiClient.connect();
        super.onStart();
    }

    @Override
    protected void onStop() {
        mGoogleApiClient.disconnect();
        super.onStop();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        if (ActivityCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_COARSE_LOCATION, android.Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_RESULT);
            return;
        }
        updateLocation();
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Dialog errDialog = GoogleApiAvailability.getInstance().getErrorDialog(this, connectionResult.getErrorCode(), 0);
        errDialog.show();
        return;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == LOCATION_PERMISSION_RESULT) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) { //.length > 0) {
                    updateLocation();
            }
        }
    }

    private void updateLocation() {
        //askedPermission = false;
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_COARSE_LOCATION, android.Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_RESULT);
            return;
        }
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (mLastLocation != null) {
            mLatText.setText(String.valueOf(mLastLocation.getLatitude()));
            mLonText.setText(String.valueOf(mLastLocation.getLongitude()));
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, mLocationListener);
    }
}

class SQLiteExample extends SQLiteOpenHelper {
    public SQLiteExample(Context context){
        super(context, DBContract.DemoTable.DB_NAME, null, DBContract.DemoTable.DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db){
        db.execSQL(DBContract.DemoTable.SQL_CREATE_TABLE);

        ContentValues mContentValues = new ContentValues();
//        mContentValues.put(DBContract.DemoTable.COLUMN_NAME_TABLE_LAT, "12.345");
//        mContentValues.put(DBContract.DemoTable.COLUMN_NAME_TABLE_LONG, "67.890");
//        mContentValues.put(DBContract.DemoTable.COLUMN_NAME_TABLE_STR, "testing");
//        db.insert(DBContract.DemoTable.TABLE_NAME, null, mContentValues);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion){
        db.execSQL(DBContract.DemoTable.SQL_DROP_TABLE);
        onCreate(db);
    }
}

final class DBContract {
    private DBContract(){};

    public final class DemoTable implements BaseColumns {
        public static final String DB_NAME = "my_db";
        public static final String TABLE_NAME = "my_table";
        public static final String COLUMN_NAME_TABLE_LONG = "table_long";
        public static final String COLUMN_NAME_TABLE_LAT = "table_lat";
        public static final String COLUMN_NAME_TABLE_STR = "table_str";
        public static final int DB_VERSION = 8;

        public static final String SQL_CREATE_TABLE = "CREATE TABLE " +
                DemoTable.TABLE_NAME + "(" + DemoTable._ID + " INTEGER PRIMARY KEY NOT NULL," +
                DemoTable.COLUMN_NAME_TABLE_LAT + " VARCHAR(255)," +
                DemoTable.COLUMN_NAME_TABLE_LONG + " VARCHAR(255)," +
                DemoTable.COLUMN_NAME_TABLE_STR + " VARCHAR(255));";

        public static final String SQL_TEST_INSERT = "INSERT INTO " + TABLE_NAME +
                " (" + COLUMN_NAME_TABLE_LAT + "," + COLUMN_NAME_TABLE_LONG +  "," + COLUMN_NAME_TABLE_STR + ") VALUES ('12.345', '67.890', 'testStr');";

        public static final String SQL_DROP_TABLE = "DROP TABLE IF EXISTS " + DemoTable.TABLE_NAME;
    }
}