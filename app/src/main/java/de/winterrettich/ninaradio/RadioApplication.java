package de.winterrettich.ninaradio;

import android.app.Application;
import android.content.Intent;
import android.content.SharedPreferences;

import com.activeandroid.ActiveAndroid;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;
import com.squareup.otto.ThreadEnforcer;

import java.lang.reflect.Type;
import java.util.List;

import de.winterrettich.ninaradio.discover.DiscoverService;
import de.winterrettich.ninaradio.discover.RadioTimeDeserializer;
import de.winterrettich.ninaradio.event.EventLogger;
import de.winterrettich.ninaradio.event.PlaybackEvent;
import de.winterrettich.ninaradio.model.RadioDatabase;
import de.winterrettich.ninaradio.model.Station;
import de.winterrettich.ninaradio.service.RadioPlayerService;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RadioApplication extends Application {
    public static Bus sBus = new Bus(ThreadEnforcer.MAIN);
    public static RadioDatabase sDatabase;
    public static DiscoverService sDiscovererService;
    private EventLogger mLogger;

    @Override
    public void onCreate() {
        super.onCreate();
        sBus.register(this);

        setupLogger();
        setupDatabase();
        setupDiscoverService();
    }

    protected void setupLogger() {
        mLogger = new EventLogger();
        sBus.register(mLogger);
    }

    protected void setupDatabase() {
        SharedPreferences prefs = getSharedPreferences(BuildConfig.APPLICATION_ID, MODE_PRIVATE);
        ActiveAndroid.initialize(this);
        sDatabase = new RadioDatabase(prefs);
        sBus.register(sDatabase);
    }

    protected void setupDiscoverService() {
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(RadioTimeDeserializer.STATION_LIST_TYPE, new RadioTimeDeserializer())
                .create();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://opml.radiotime.com/")
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();
        sDiscovererService = retrofit.create(DiscoverService.class);
    }

    @Subscribe
    public void handlePlaybackEvent(PlaybackEvent event) {
        switch (event) {
            case PLAY:
                if (sDatabase.selectedStation == null) {
                    throw new IllegalStateException("Select a Station before playing");
                }
                // start service
                Intent serviceIntent = new Intent(this, RadioPlayerService.class);
                startService(serviceIntent);
                break;

            case STOP:
                // stop service
                Intent intent = new Intent(getApplicationContext(), RadioPlayerService.class);
                stopService(intent);
                break;
        }
    }

}
