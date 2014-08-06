package cz.destil.wearsquare.activity;

import android.os.Bundle;
import android.support.wearable.view.WearableListView;
import android.widget.TextView;

import com.squareup.otto.Subscribe;

import java.util.List;

import cz.destil.wearsquare.R;
import cz.destil.wearsquare.adapter.CheckInAdapter;
import cz.destil.wearsquare.event.CheckInVenueListEvent;
import cz.destil.wearsquare.event.ErrorEvent;
import cz.destil.wearsquare.event.ExitEvent;
import cz.destil.wearsquare.event.ImageLoadedEvent;
import cz.destil.wearsquare.util.DebugLog;

public class CheckInListActivity extends ProgressActivity {

    WearableListView vList;
    private List<CheckInAdapter.Venue> mVenues;
    private CheckInAdapter mAdapter;

    @Override
    int getMainViewResourceId() {
        return R.layout.activity_check_in_list;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        finishOtherActivities();
        super.onCreate(savedInstanceState);
        vList = (WearableListView) getMainView();
    }

    @Override
    public void startConnected() {
        super.startConnected();
        DebugLog.d("sending start message");
        teleport().sendMessage("/check-in-list", null);
        showProgress();
    }

    @Subscribe
    public void onVenueList(CheckInVenueListEvent event) {
        DebugLog.d("setting up adapter: " + event.getVenues());
        hideProgress();
        mVenues = event.getVenues();
        mAdapter = new CheckInAdapter(CheckInListActivity.this, mVenues);
        vList.setAdapter(mAdapter);
        vList.setClickListener(new WearableListView.ClickListener() {
                                   @Override
                                   public void onClick(WearableListView.ViewHolder viewHolder) {
                                       String id = (String) viewHolder.itemView.getTag();
                                       String name = ((TextView) viewHolder.itemView.findViewById(R.id.text)).getText
                                               ().toString();
                                       DebugLog.d("id=" + id + " name=" + name);
                                       CheckInActivity.call(CheckInListActivity.this, id, name);
                                   }

                                   @Override
                                   public void onTopEmptyRegionClick() {

                                   }
                               }
        );
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
    public void onImageDownloaded(ImageLoadedEvent event) {
        for (CheckInAdapter.Venue venue : mVenues) {
            if (venue.getImageUrl().equals(event.getImageUrl())) {
                venue.setIcon(event.getBitmap());
                mAdapter.notifyDataSetChanged();
                break;
            }
        }
    }
}