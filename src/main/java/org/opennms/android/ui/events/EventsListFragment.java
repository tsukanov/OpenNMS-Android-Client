package org.opennms.android.ui.events;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import com.actionbarsherlock.app.SherlockListFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.widget.SearchView;
import org.opennms.android.R;
import org.opennms.android.dao.Columns;
import org.opennms.android.dao.events.Event;
import org.opennms.android.dao.events.EventsListProvider;
import org.opennms.android.service.EventsSyncService;

public class EventsListFragment extends SherlockListFragment
        implements SearchView.OnQueryTextListener, LoaderManager.LoaderCallbacks<Cursor> {

    private static final int LOADER_ID = 3;
    private EventAdapter adapter;
    private boolean isDualPane = false;
    private MenuItem refreshItem;
    private String currentFilter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.list_layout, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        FrameLayout detailsContainer = (FrameLayout) getSherlockActivity().findViewById(R.id.details_fragment_container);
        isDualPane = detailsContainer != null && detailsContainer.getVisibility() == View.VISIBLE;

        if (isDualPane) {
            getListView().setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        }

        adapter = new EventAdapter(getSherlockActivity(), null, CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER);
        getListView().setAdapter(adapter);

        TextView emptyText = (TextView) getActivity().findViewById(R.id.empty_text);
        emptyText.setText(getString(R.string.events_list_empty));

        getActivity().getSupportLoaderManager().initLoader(LOADER_ID, null, this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Uri baseUri;
        if (currentFilter != null) {
            baseUri = Uri.withAppendedPath(
                    Uri.withAppendedPath(EventsListProvider.CONTENT_URI, Columns.EventColumns.SEVERITY),
                    Uri.encode(currentFilter)
            );
        } else {
            baseUri = EventsListProvider.CONTENT_URI;
        }
        String[] projection = {
                Columns.EventColumns.TABLE_ID,
                Columns.EventColumns.EVENT_ID,
                Columns.EventColumns.LOG_MESSAGE,
                Columns.EventColumns.SEVERITY
        };
        return new CursorLoader(getActivity(), baseUri, projection, null, null,
                Columns.EventColumns.EVENT_ID + " DESC");
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        stopRefreshAnimation();
        adapter.swapCursor(cursor);
        if (isDualPane && !adapter.isEmpty() && isVisible()) showDetails(0);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        adapter.swapCursor(null);
    }

    @Override
    public void onStop() {
        super.onStop();
        stopRefreshAnimation();
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        showDetails(position);
    }

    private void showDetails(int position) {
        getListView().setItemChecked(position, true);
        Event event = getEvent(getListView().getItemIdAtPosition(position));
        showDetails(event);
    }

    private void showDetails(Event event) {
        if (isDualPane) {
            FragmentTransaction fragmentTransaction = getChildFragmentManager().beginTransaction();
            FrameLayout detailsContainer = (FrameLayout) getSherlockActivity().findViewById(R.id.details_fragment_container);
            detailsContainer.removeAllViews();
            EventDetailsFragment detailsFragment = new EventDetailsFragment();
            detailsFragment.bindEvent(event);
            fragmentTransaction.add(R.id.details_fragment_container, detailsFragment);
            fragmentTransaction.commit();
        } else {
            Intent detailsIntent = new Intent(getSherlockActivity(), EventDetailsActivity.class);
            detailsIntent.putExtra("event", event);
            startActivity(detailsIntent);
        }
    }

    private Event getEvent(long id) {
        Cursor cursor = getActivity().getContentResolver().query(
                Uri.withAppendedPath(EventsListProvider.CONTENT_URI, String.valueOf(id)),
                null, null, null, null
        );
        if (cursor.moveToFirst()) {
            Event event = new Event(cursor.getInt(cursor.getColumnIndexOrThrow(Columns.EventColumns.EVENT_ID)));
            event.setSeverity(cursor.getString(cursor.getColumnIndexOrThrow(Columns.EventColumns.SEVERITY)));
            event.setLogMessage(cursor.getString(cursor.getColumnIndexOrThrow(Columns.EventColumns.LOG_MESSAGE)));
            event.setDescription(cursor.getString(cursor.getColumnIndexOrThrow(Columns.EventColumns.DESCRIPTION)));
            event.setHost(cursor.getString(cursor.getColumnIndexOrThrow(Columns.EventColumns.HOST)));
            event.setIpAddress(cursor.getString(cursor.getColumnIndexOrThrow(Columns.EventColumns.IP_ADDRESS)));
            event.setCreateTime(cursor.getString(cursor.getColumnIndexOrThrow(Columns.EventColumns.CREATE_TIME)));
            event.setNodeId(cursor.getInt(cursor.getColumnIndexOrThrow(Columns.EventColumns.NODE_ID)));
            event.setNodeLabel(cursor.getString(cursor.getColumnIndexOrThrow(Columns.EventColumns.NODE_LABEL)));
            event.setServiceTypeId(cursor.getInt(cursor.getColumnIndexOrThrow(Columns.EventColumns.SERVICE_TYPE_ID)));
            event.setServiceTypeName(cursor.getString(cursor.getColumnIndexOrThrow(Columns.EventColumns.SERVICE_TYPE_NAME)));
            cursor.close();
            return event;
        }
        cursor.close();
        return null;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.list, menu);
        MenuItem searchItem = menu.add("Search");
        searchItem.setIcon(getResources().getDrawable(R.drawable.ic_action_search));
        searchItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        SearchView search = new SearchView(getActivity());
        search.setOnQueryTextListener(this);
        searchItem.setActionView(search);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_refresh:
                refreshItem = item;
                refreshList();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void refreshList() {
        startRefreshAnimation();
        Intent intent = new Intent(getActivity(), EventsSyncService.class);
        getActivity().startService(intent);
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        String newFilter = !TextUtils.isEmpty(newText) ? newText : null;
        if (currentFilter == null && newFilter == null) {
            return true;
        }
        if (currentFilter != null && currentFilter.equals(newFilter)) {
            return true;
        }
        currentFilter = newFilter;
        getActivity().getSupportLoaderManager().restartLoader(0, null, this);
        return true;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        return true;
    }

    private void startRefreshAnimation() {
        LayoutInflater inflater = (LayoutInflater) getSherlockActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        ImageView iv = (ImageView) inflater.inflate(R.layout.refresh_action_view, null);
        Animation rotation = AnimationUtils.loadAnimation(getSherlockActivity(), R.anim.refresh);
        rotation.setRepeatCount(Animation.INFINITE);
        iv.startAnimation(rotation);
        refreshItem.setActionView(iv);
    }

    private void stopRefreshAnimation() {
        if (refreshItem != null && refreshItem.getActionView() != null) {
            refreshItem.getActionView().clearAnimation();
            refreshItem.setActionView(null);
        }
    }

}