package org.opennms.android.test.ui;

import android.test.ActivityInstrumentationTestCase2;
import android.view.View;
import android.widget.Button;
import com.jayway.android.robotium.solo.Solo;
import org.opennms.android.R;
import org.opennms.android.ui.MainActivity;

import java.util.ArrayList;

public class MainActivityTest extends ActivityInstrumentationTestCase2<MainActivity> {
    private Solo solo;

    public MainActivityTest() {
        super(MainActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        solo = new Solo(getInstrumentation(), getActivity());
    }

    public void testAboutDialog() throws Exception {
        solo.clickOnMenuItem(solo.getString(R.string.about), true);
        assertTrue("About dialog text not found", solo.searchText(solo.getString(R.string.about_info)));

        Button dialogCloseButton = (Button) solo.getView(android.R.id.button3);
        solo.clickOnButton(solo.getString(R.string.close_dialog));
        solo.sleep(200);

        ArrayList<View> currentViews = solo.getCurrentViews();
        assertFalse("Not returned to MainActivity", currentViews.contains(dialogCloseButton));
    }

    public void testSettings() throws Exception {
        solo.clickOnMenuItem(solo.getString(R.string.settings), true);
        assertTrue("SettingsActivity was not found", solo.waitForActivity("SettingsActivity"));
    }

    public void testTabs() throws Exception {
        final int TIMEOUT = 300; // ms

        // Nodes
        solo.clickOnText(solo.getString(R.string.nodes));
        assertTrue(solo.waitForFragmentByTag(MainActivity.NODES_TAG, TIMEOUT));

        // Outages
        solo.clickOnText(solo.getString(R.string.outages));
        assertTrue(solo.waitForFragmentByTag(MainActivity.OUTAGES_TAG, TIMEOUT));

        // Events
        solo.clickOnText(solo.getString(R.string.events));
        assertTrue(solo.waitForFragmentByTag(MainActivity.EVENTS_TAG, TIMEOUT));

        // Alarms
        solo.clickOnText(solo.getString(R.string.alarms));
        assertTrue(solo.waitForFragmentByTag(MainActivity.ALARMS_TAG, TIMEOUT));
    }

    @Override
    protected void tearDown() throws Exception {
        solo.finishOpenedActivities();
    }

}