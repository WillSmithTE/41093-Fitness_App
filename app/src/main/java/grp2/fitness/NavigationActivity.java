package grp2.fitness;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobile.auth.core.IdentityManager;
import com.amazonaws.mobileconnectors.cognito.CognitoSyncManager;
import com.amazonaws.models.nosql.DailyDataDO;
import com.amazonaws.regions.Regions;

import java.util.ArrayList;

import grp2.fitness.fragments.CalculatorFragment;
import grp2.fitness.fragments.DiaryFragment;
import grp2.fitness.fragments.HeartRateFragment;
import grp2.fitness.fragments.HomeFragment;
import grp2.fitness.fragments.LeaderboardFragment;
import grp2.fitness.fragments.PedometerFragment;
import grp2.fitness.fragments.SettingsFragment;
import grp2.fitness.fragments.SetupFragment;
import grp2.fitness.handlers.CognitoDatasetManager;
import grp2.fitness.handlers.DailyDataManager;
import grp2.fitness.handlers.GoogleFitApi;

public class NavigationActivity extends AppCompatActivity implements
        NavigationView.OnNavigationItemSelectedListener,
        SharedPreferences.OnSharedPreferenceChangeListener,
        DailyDataManager.DailyDataListener{

    private DrawerLayout drawerLayout;
    private ActionBarDrawerToggle drawerToggle;
    private NavigationView navigationView;
    private ProgressBar loadingIcon;

    private GoogleFitApi googleFitApi;

    private CognitoCachingCredentialsProvider credentialsProvider;
    private CognitoSyncManager syncClient;
    private DailyDataManager dailyDataManager;
    private CognitoDatasetManager datasetManager;

    private SharedPreferences sharedPreferences;

    private String userName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_navigation);

        Toolbar toolbar = findViewById(R.id.navigation_toolbar);
        navigationView = findViewById(R.id.navigation_view);
        loadingIcon = findViewById(R.id.loading_icon);

        drawerLayout = findViewById(R.id.drawer_layout);
        drawerToggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.open, R.string.close);

        setSupportActionBar(toolbar);
        drawerLayout.addDrawerListener(drawerToggle);

        navigationView.setNavigationItemSelectedListener(this);
        updateView(HomeFragment.class);

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        userName = sharedPreferences.getString(getString(R.string.pref_key_personal_name), "No Profile Name!");

        initialiseCognito();
        testFirstLogin();
    }

    @Override
    protected void onPause() {
        super.onPause();
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);
        TextView userNameTV = navigationView.getHeaderView(0).findViewById(R.id.profile_name);
        userNameTV.setText(userName);
    }

    private void testFirstLogin() {
        if (sharedPreferences.getBoolean("firststart", true)) {
            sharedPreferences.edit().putBoolean("firststart", false).apply();
            updateView(SetupFragment.class);
        }
    }

    private void initialiseCognito() {
        credentialsProvider = new CognitoCachingCredentialsProvider(
                getApplicationContext(),
                "ap-southeast-2:9e086252-ebfb-4c0d-8d66-bdd54c04d6c1", // Identity pool ID
                Regions.AP_SOUTHEAST_2 // Region
        );

        syncClient = new CognitoSyncManager(
                getApplicationContext(),
                Regions.AP_SOUTHEAST_2, // Region
                credentialsProvider);

        dailyDataManager = new DailyDataManager(credentialsProvider.getIdentityId(), this, userName);
        datasetManager = new CognitoDatasetManager(syncClient);
    }

    public void updateView(Class fragmentClass) {
        Fragment fragment = null;
        showLoadingIcon();

        try {
            fragment = (Fragment) fragmentClass.newInstance();
        } catch (Exception e) {
            hideLoadingIcon();
            e.printStackTrace();
        }

        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction().replace(R.id.fragment_frame, fragment).commit();

        drawerLayout.closeDrawers();
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        drawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration configuration) {
        super.onConfigurationChanged(configuration);
        drawerToggle.onConfigurationChanged(configuration);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return drawerToggle.onOptionsItemSelected(item) || super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        updateView(HomeFragment.class);

        if (this.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            this.drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        Class fragmentClass;

        switch (item.getItemId()) {
            case R.id.home:
                fragmentClass = HomeFragment.class;
                break;
            case R.id.diary:
                fragmentClass = DiaryFragment.class;
                break;
            case R.id.pedometer:
                fragmentClass = PedometerFragment.class;
                break;
            case R.id.heartrate:
                fragmentClass = HeartRateFragment.class;
                break;
            case R.id.calculator:
                fragmentClass = CalculatorFragment.class;
                break;
            case R.id.leaderboard:
                fragmentClass = LeaderboardFragment.class;
                break;
            case R.id.settings:
                fragmentClass = SettingsFragment.class;
                break;
            case R.id.logout:
                IdentityManager.getDefaultIdentityManager().signOut();
                startActivity(new Intent(NavigationActivity.this, LoginActivity.class));
                finish();
                return true;
            default:
                return false;
        }

        setTitle(item.getTitle());
        item.setChecked(true);

        updateView(fragmentClass);
        return true;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if( requestCode == GoogleFitApi.REQUEST_KEY) {
            googleFitApi.setAuthState(false);
            if( resultCode == RESULT_OK ) {
                    googleFitApi.getClient().connect();
            }
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String preferenceKey) {
        datasetManager.setValue(preferenceKey, sharedPreferences.getString(preferenceKey, ""));
    }

    @Override
    public void onAllDailyDataSynced(ArrayList<DailyDataDO> allDailyData) {

    }

    @Override
    public void onDailyDataSaved(DailyDataDO dailyData) {

    }

    public void hideLoadingIcon(){
        loadingIcon.setVisibility(View.GONE);
    }

    public void showLoadingIcon(){
        loadingIcon.setVisibility(View.VISIBLE);
    }

    public GoogleFitApi getGoogleFitApi(GoogleFitApi.GoogleFitApiCallback callback){
        if(googleFitApi == null){
            googleFitApi = new GoogleFitApi(this, callback);
        }else {
            googleFitApi.setCallback(callback);
        }

        return googleFitApi;
    }

    public SharedPreferences getSharedPreferences(){
        return this.sharedPreferences;
    }
    public DailyDataManager getDailyDataManager(){
        return this.dailyDataManager;
    }
    public CognitoSyncManager getSyncClient(){
        return this.syncClient;
    }
    public CognitoCachingCredentialsProvider getCredentialsProvider(){
        return this.credentialsProvider;
    }


}
