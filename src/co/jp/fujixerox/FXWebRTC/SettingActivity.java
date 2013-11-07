package co.jp.fujixerox.FXWebRTC;

/**
 * Created with IntelliJ IDEA.
 * User: haiyang
 * Date: 10/21/13
 * Time: 4:40 PM
 * To change this template use File | Settings | File Templates.
 */


import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.MenuItem;

public class SettingActivity  extends Activity {

       private static final String TAG="SettingActivity";

       //connection and message relay server
       private static final String KEY_SERVER_HOST="server_host";
       private static final String KEY_SERVER_PORT="server_port";

      //STUN Server
       private static final String KEY_STUN_SERVER_HOST="stun_server_host";
       private static final String KEY_STUN_SERVER_PORT="stun_server_port";
       public static Context context;





     public static class Settings{

        public static Context getContext()
        {
            return context;
        }

        public static void setContext(Context mcontext)
        {
            context=mcontext;
        }


        public static String getServerHost()
        {


            SharedPreferences sp= PreferenceManager.getDefaultSharedPreferences(context);

            return sp.getString(KEY_SERVER_HOST,"10.0.2.2");
        }

        public static int getServerPort()
        {
            SharedPreferences sp=PreferenceManager.getDefaultSharedPreferences(context);
            return sp.getInt(KEY_SERVER_PORT, 8888);
        }

        public static String getStunServerHost()
        {
            SharedPreferences sp=PreferenceManager.getDefaultSharedPreferences(context);
            return sp.getString(KEY_STUN_SERVER_HOST,"114.160.59.82");

        }

        public static int getStunServerPort()
        {
            SharedPreferences sp=PreferenceManager.getDefaultSharedPreferences(context);
            return sp.getInt(KEY_STUN_SERVER_PORT,10813);
        }





     }



    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        context=getApplicationContext();


        //display the fragment as the main content
        getFragmentManager().beginTransaction().
                replace(android.R.id.content, new SettingsFragment()).commit();


        PreferenceManager.setDefaultValues(this,R.layout.settings,false);

        //settings of Action Bar
        ActionBar actionBar=getActionBar();

        actionBar.setTitle(R.string.settings_name);
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.show();
    }




    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch(item.getItemId())
        {
            case android.R.id.home:
                Intent intent=new Intent(this,FXWebRTCMainActivity.class);
                startActivity(intent);
                return true;
            default:
                break;

        }

        return super.onOptionsItemSelected(item);
    }

    public static class SettingsFragment extends PreferenceFragment  implements SharedPreferences.OnSharedPreferenceChangeListener{

        @Override
        public void onCreate(Bundle savedInstanceState)
        {
                super.onCreate(savedInstanceState);

            //load the preference from an XML resource
            addPreferencesFromResource(R.layout.settings);

            setValuetoSummary();
        }

        @Override
        public void onPause()
        {
               super.onPause();
               getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onResume()
        {
                super.onResume();

                getPreferenceScreen().getSharedPreferences().
                        registerOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            //To change body of implemented methods use File | Settings | File Templates.

            //set changed value as summary
            findPreference(key).setSummary(String.valueOf(sharedPreferences.getAll().get(key)));
        }

        private void setValuetoSummary()
        {
                 findPreference(KEY_SERVER_HOST).setSummary(Settings.getServerHost());

                 findPreference(KEY_SERVER_PORT).setSummary(String.valueOf(Settings.getServerPort()));

                 findPreference(KEY_STUN_SERVER_HOST).setSummary(Settings.getStunServerHost());

                 findPreference(KEY_STUN_SERVER_PORT).setSummary(String.valueOf(Settings.getStunServerPort()));
        }

    }



}
