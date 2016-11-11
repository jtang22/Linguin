package linguin.data.org.linguin;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteException;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import linguin.data.org.linguin.database.DataSource;
import linguin.data.org.linguin.model.DataItem;
import linguin.data.org.linguin.sample.SampleDataProvider;
import linguin.data.org.linguin.utils.JSONHelper;

public class MainActivity extends AppCompatActivity {

    private static final int SIGNIN_REQUEST = 1001;
    public static final String MY_GLOBAL_PREFS = "my global prefs";
    private static final int REQUEST_PERMISSION_WRITE = 1002;
    private static final String TAG = "Items imported";
    private List<DataItem> dataItemList = SampleDataProvider.dataItemList;
    private List<String> itemNames = new ArrayList<String>();
    private boolean permissionGranted;

    DataSource mDataSource;
    List<DataItem> listFromDB;
    DrawerLayout mDrawerLayout;
    ListView mDrawerList;
    String[] mCategories;
    RecyclerView mRecyclerView;
    DataItemAdapter mItemAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //      Code to manage sliding navigation drawer
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mCategories = getResources().getStringArray(R.array.categories);
        mDrawerList = (ListView) findViewById(R.id.left_drawer);
        mDrawerList.setAdapter(new ArrayAdapter<>(this,
                R.layout.drawer_list_item, mCategories));
        mDrawerList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String category = mCategories[position];
                Toast.makeText(MainActivity.this, "You chose " + category,
                        Toast.LENGTH_SHORT).show();
                displayDataItems(category);
                mDrawerLayout.closeDrawer(mDrawerList);
            }
        });
//      end of navigation drawera

        //SQLiteOpenHelper dbHelper = new DBHelper(this);
        mDataSource = new DataSource(this);
        mDataSource.open();
        mDataSource.seedDatabase(dataItemList);

        checkPermissions();

//        Collections.sort(dataItemList, new Comparator<DataItem>() {
//            @Override
//            public int compare(DataItem dataItem, DataItem t1) {
//                return dataItem.getItemName().compareTo(t1.getItemName());
//            }
//        });

//        List<DataItem> listFromDB = mDataSource.getAllItems(null);
//
//        DataItemAdapter adapter = new DataItemAdapter(this, listFromDB);

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        boolean grid = settings.getBoolean(getString(R.string.pref_display_grid), false);

        mRecyclerView = (RecyclerView) findViewById(R.id.rvItems);
        if (grid) {
            mRecyclerView.setLayoutManager(new GridLayoutManager(this, 3));
        }
        displayDataItems(null);
    }

    private void displayDataItems(String category) {
        listFromDB = mDataSource.getAllItems(category);
        mItemAdapter = new DataItemAdapter(this, listFromDB);
        mRecyclerView.setAdapter(mItemAdapter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mDataSource.close();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mDataSource.open();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_signin:
                Intent intent = new Intent(this, SigninActivity.class);
                startActivityForResult(intent, SIGNIN_REQUEST);
                return true;
            case R.id.action_settings:
                Intent settingsIntent = new Intent(this, PrefsActivity.class);
                startActivity(settingsIntent);
                return true;
            case R.id.action_export:
                boolean result = JSONHelper.exportToJSON(this, dataItemList);
                if(result) {
                    Toast.makeText(this, "Data exported", Toast.LENGTH_SHORT).show();
                }
                else {
                    Toast.makeText(this, "Export failed", Toast.LENGTH_SHORT).show();
                }
                return true;
            case R.id.action_import:
                List<DataItem> dataItems = JSONHelper.importFromJSON(this);
                if(dataItems != null) {
                    for (DataItem dataItem:
                         dataItems) {
                        Log.i(TAG, "onOptionsItemSelected " + dataItem.getItemName());
                    }
                }
                return true;
            case R.id.action_display_all:
                displayDataItems(null);
                return true;
            case R.id.action_choose_category:
                mDrawerLayout.openDrawer(mDrawerList);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK && requestCode == SIGNIN_REQUEST) {
            String email = data.getStringExtra(SigninActivity.EMAIL_KEY);
            Toast.makeText(this, "You signed in as " + email, Toast.LENGTH_SHORT).show();

            SharedPreferences.Editor editor = getSharedPreferences(MY_GLOBAL_PREFS, MODE_PRIVATE).edit();
            editor.putString(SigninActivity.EMAIL_KEY, email);
            editor.apply();

        }

    }

    /* Checks if external storage is available for read and write */
    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }

    /* Checks if external storage is available to at least read */
    public boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        return (Environment.MEDIA_MOUNTED.equals(state) ||
                Environment.MEDIA_MOUNTED_READ_ONLY.equals(state));
    }

    // Initiate request for permissions.
    private boolean checkPermissions() {

        if (!isExternalStorageReadable() || !isExternalStorageWritable()) {
            Toast.makeText(this, "This app only works on devices with usable external storage",
                    Toast.LENGTH_SHORT).show();
            return false;
        }

        int permissionCheck = ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_PERMISSION_WRITE);
            return false;
        } else {
            return true;
        }
    }

    // Handle permissions result
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_PERMISSION_WRITE:
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    permissionGranted = true;
                    Toast.makeText(this, "External storage permission granted",
                            Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "You must grant permission!", Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }
}
