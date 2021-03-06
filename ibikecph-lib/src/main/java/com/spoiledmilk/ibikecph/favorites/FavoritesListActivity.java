package com.spoiledmilk.ibikecph.favorites;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListAdapter;
import com.spoiledmilk.ibikecph.IbikeApplication;
import com.spoiledmilk.ibikecph.LeftMenu;
import com.spoiledmilk.ibikecph.R;
import com.spoiledmilk.ibikecph.controls.SortableListView;
import com.spoiledmilk.ibikecph.navigation.routing_engine.SMLocationManager;
import com.spoiledmilk.ibikecph.util.DB;
import com.spoiledmilk.ibikecph.util.LOG;
import com.spoiledmilk.ibikecph.util.Util;

import java.util.ArrayList;

public class FavoritesListActivity extends Activity {
	public static final int ADD_FAVORITE = 510;
	
	SortableListView favoritesList;
	private ListAdapter listAdapter;
	private FavoritesAdapter adapter;
	protected ArrayList<FavoritesData> favorites = new ArrayList<FavoritesData>();
	private tFetchFavorites fetchFavorites;

	public boolean favoritesEnabled = true;
	private AlertDialog dialog;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_favorites_list);

        try {
            this.getActionBar().setTitle(IbikeApplication.getString("favorites"));
        } catch(NullPointerException e) {
            // If we have no action bar, then whatever.
        }

		favoritesList = (SortableListView) findViewById(R.id.favoritesList);		
		listAdapter = getAdapter();

		favoritesList.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
				Log.d("JC", "Clicked on some favorite");

				if (!((FavoritesAdapter) favoritesList.getAdapter()).isEditMode) {
					if (favoritesEnabled) {
						Log.d("JC", "Calling click handler");
						favoritesEnabled = false;
						onListItemClick(position);
					}
				}
			}
			

		});
		/*
		favoritesList.setOnItemLongClickListener( new OnItemLongClickListener() {
			public boolean onItemLongClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
				Log.d("JC", "Long clicked on some favorite");
				
				// TODO: We want to spawn a dialog to ask about editing or deleting a favorite here. 
				
				return true;
			}
		});
		*/
		
		registerForContextMenu(favoritesList);
		reloadFavorites();
	}

	public void onResume(View v) {
		reloadFavorites();
	}

	public void reloadFavorites() {
		if (IbikeApplication.isUserLogedIn()) {
			fetchFavorites = new tFetchFavorites();
			fetchFavorites.start();
		}
	}
	
	@Override
	public void onCreateContextMenu(android.view.ContextMenu menu, View v, android.view.ContextMenu.ContextMenuInfo menuInfo) {
		Log.d("JC", "FavoritesList context menu");
	    AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
	    menu.setHeaderTitle(favorites.get(info.position).getName());
	    String[] menuItems = {"Edit" /*, "Delete" */};
	    for (int i = 0; i<menuItems.length; i++) {
	    	menu.add(Menu.NONE, i, i, menuItems[i]);
	    }

	};
	
	@Override
	public boolean onContextItemSelected(MenuItem item) {		
		AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
		int menuItemIndex = item.getItemId();
		String[] menuItems = {"Edit" /*, "Delete" */};
		FavoritesData fav = favorites.get(info.position);
		
		switch (menuItemIndex) {
		case 0:
			Log.d("JC", "Editing "+fav.getName());
			
	        Intent i = new Intent(this, EditFavoriteActivity.class);
	        
	        // Add the favorite to the intent so it can be passed on to the EditFavoriteFragment later on.
	        i.putExtra("favoritesData", fav);
	        
	        this.startActivityForResult(i, ADD_FAVORITE);
	        this.overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);

			break;
			
		case 1:
			Log.d("JC", "Deleting "+fav.getName());
			break;
		
		}
				
		return true;
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.favorites_list, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.addButton) {
			Intent i = new Intent(this, AddFavoriteActivity.class);
	        this.startActivityForResult(i, ADD_FAVORITE);
	        this.overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
			
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	protected FavoritesAdapter getAdapter() {
		if (adapter == null) {
			adapter = new FavoritesAdapter(IbikeApplication.getContext(), favorites, null);
		}
		return adapter;
	}


	public void onListItemClick(int position) {
		if (!Util.isNetworkConnected(this)) {
			favoritesEnabled = true;
			Util.launchNoConnectionDialog(this);
		} else {
			FavoritesData fd = (FavoritesData) (favoritesList.getAdapter().getItem(position));

			Log.d("JC", "Clicked favorite: " + fd.getName());
			Log.d("JC", "Coords: " + fd.getLatitude() + " , "+ fd.getLongitude());

			if (SMLocationManager.getInstance().hasValidLocation()) {
				Intent returnIntent = new Intent();

				// Return some information as to where to route, so the MapActivity knows and can handle it.				
				returnIntent.putExtra("ROUTE_TO", fd);
				setResult(RESULT_OK, returnIntent);
				finishActivity(LeftMenu.LAUNCH_FAVORITE);
				finish();
				Log.d("JC", "Should have finished activity");

			} else {
				favoritesEnabled = true;
				showRouteNotFoundDlg();
			}
		}
	}

	public void showRouteNotFoundDlg() {
		if (dialog == null) {
			String message = IbikeApplication.getString("error_route_not_found");
			if (!SMLocationManager.getInstance().hasValidLocation())
				message = IbikeApplication.getString("error_no_gps");
			Builder builder = new AlertDialog.Builder(this);
			builder.setMessage(message).setPositiveButton(IbikeApplication.getString("OK"), new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
				}
			});
			dialog = builder.create();
			dialog.setCancelable(false);
		}
		if (!dialog.isShowing()) {
			dialog.show();
		}
	}
	
	public void onPause() {
		super.onPause();
        if (fetchFavorites != null && fetchFavorites.isAlive()) {
            fetchFavorites.interrupt();
        }
	}
	
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == ADD_FAVORITE && resultCode == RESULT_OK) {
			this.reloadFavorites();
		}
	}

	private class tFetchFavorites extends Thread {
		@Override
		public void run() {
			Log.d("JC", "FavFetcher started");
			while (!interrupted()) {
				LOG.d("fetching the favorites");
				final ArrayList<FavoritesData> favs = (new DB(IbikeApplication.getContext())).getFavoritesFromServer(IbikeApplication.getContext(), null);
				runOnUiThread(new Runnable() {
					public void run() {
						Log.d("JC", "Got some favorites");
						favorites.clear();
						favorites.addAll(favs);
						((FavoritesAdapter) listAdapter).notifyDataSetChanged();

						favoritesList.setAdapter(listAdapter);
					}
				});

				if (Util.isNetworkConnected(IbikeApplication.getContext())) {
					// favorites have been fetched
					break;
				}
				try {
					Thread.sleep(5000);
				} catch (InterruptedException e) {
					break;
				}
			}

		}
	}

}
