package cz.destil.wearsquare.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.wearable.activity.ConfirmationActivity;
import android.support.wearable.view.GridViewPager;
import android.view.MotionEvent;
import android.view.View;

import com.squareup.otto.Subscribe;

import java.util.List;

import cz.destil.wearsquare.R;
import cz.destil.wearsquare.adapter.EmptyGridPagerAdapter;
import cz.destil.wearsquare.adapter.ExploreAdapter;
import cz.destil.wearsquare.core.App;
import cz.destil.wearsquare.event.ErrorEvent;
import cz.destil.wearsquare.event.ExitEvent;
import cz.destil.wearsquare.event.ExploreVenueListEvent;
import cz.destil.wearsquare.event.ImageLoadedEvent;
import cz.destil.wearsquare.util.DebugLog;
import cz.destil.wearsquare.util.UiUtils;

public class ExploreActivity extends ProgressActivity {

    private static final int ON_PHONE_ACTIVITY = 41;
    GridViewPager vPager;
    private List<ExploreAdapter.Venue> mVenues;
    private int mImagesLoaded;

    @Override
    int getMainViewResourceId() {
        return R.layout.activity_explore;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        finishOtherActivities();
        super.onCreate(savedInstanceState);
        setupViewPager();
    }

    @Override
    public void startConnected() {
        super.startConnected();
        DebugLog.d("sending start message");
        mImagesLoaded = 0;
        teleport().sendMessage("/explore-list/" + UiUtils.getScreenDimensions(), null);
        showProgress();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == ON_PHONE_ACTIVITY) {
            finish();
            App.bus().post(new ExitEvent());
        }
    }

    @Subscribe
    public void onVenueList(ExploreVenueListEvent event) {
        hideProgress();
        mVenues = event.getVenues();
        setupAdapter();
    }

    @Subscribe
    public void onError(ErrorEvent event) {
        showError(event.getMessage());
    }

    @Subscribe
    public void onExit(ExitEvent event) {
        DebugLog.d("on exit");
        finish();
    }

    @Subscribe
    public void onImageLoaded(ImageLoadedEvent event) {
        for (ExploreAdapter.Venue venue : mVenues) {
            if (venue.getImageUrl().equals(event.getImageUrl())) {
                venue.setPhoto(event.getBitmap());
                break;
            }
        }
        if (event.getBitmap() != null) {
            setupAdapter();
        }
        mImagesLoaded++;
        if (mImagesLoaded == mVenues.size()) {
            enableScroll();
        }
    }

    private void setupViewPager() {
        vPager = (GridViewPager) getMainView();
        vPager.setAdapter(new EmptyGridPagerAdapter()); // bug in the UI library
        disableScroll();
    }


    private void enableScroll() {
        vPager.setOnTouchListener(null);
    }

    /**
     * disable touch, we need to wait until all images are loaded
     * it's a current limitation of FragmentGridViewPager
     */
    private void disableScroll() {
        vPager.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return true;
            }
        });
    }

    private void setupAdapter() {
        vPager.setAdapter(new ExploreAdapter(this, mVenues));
    }

    public void navigate(ExploreAdapter.Venue venue) {
        teleport().sendMessage("/navigate/" + venue.getLatitude() + "/" + venue.getLongitude() + "/" + venue.getName(), null);
        openOnPhoneAnimation();
    }

    public void checkIn(ExploreAdapter.Venue venue) {
        CheckInActivity.call(this, venue.getId(), venue.getName());
    }

    public void openOnPhone(ExploreAdapter.Venue venue) {
        teleport().sendMessage("/open/" + venue.getId(), null);
        openOnPhoneAnimation();
    }

    private void openOnPhoneAnimation() {
        Intent i = new Intent(this, ConfirmationActivity.class);
        i.putExtra(ConfirmationActivity.EXTRA_ANIMATION_TYPE, ConfirmationActivity.OPEN_ON_PHONE_ANIMATION);
        startActivityForResult(i, ON_PHONE_ACTIVITY);
    }
}
