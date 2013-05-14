package fr.neamar.cinetime;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

import com.bugsense.trace.BugSenseHandler;

public class CineTimeActivity extends FragmentActivity {

    static String APIKEY = "48aa4b1d";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        BugSenseHandler.initAndStartSession(this, APIKEY);
    }
}
