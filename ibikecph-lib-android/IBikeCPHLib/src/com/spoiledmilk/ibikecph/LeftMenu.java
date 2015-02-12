// Copyright (C) 2013 City of Copenhagen.
//
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at 
// http://mozilla.org/MPL/2.0/.
package com.spoiledmilk.ibikecph;

import java.lang.reflect.Method;
import java.util.ArrayList;

import android.content.Intent;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.analytics.tracking.android.Log;
import com.spoiledmilk.ibikecph.controls.SortableListView;
import com.spoiledmilk.ibikecph.favorites.AddFavoriteFragment;
import com.spoiledmilk.ibikecph.favorites.EditFavoriteFragment;
import com.spoiledmilk.ibikecph.favorites.FavoritesActivity;
import com.spoiledmilk.ibikecph.favorites.FavoritesAdapter;
import com.spoiledmilk.ibikecph.favorites.FavoritesData;
import com.spoiledmilk.ibikecph.favorites.FavoritesListActivity;
import com.spoiledmilk.ibikecph.login.FacebookProfileActivity;
import com.spoiledmilk.ibikecph.login.LoginActivity;
import com.spoiledmilk.ibikecph.login.ProfileActivity;
import com.spoiledmilk.ibikecph.map.MapActivity;
import com.spoiledmilk.ibikecph.map.SMHttpRequest;
import com.spoiledmilk.ibikecph.navigation.routing_engine.SMLocationManager;
import com.spoiledmilk.ibikecph.util.DB;
import com.spoiledmilk.ibikecph.util.LOG;
import com.spoiledmilk.ibikecph.util.Util;

/**
 * A menu that can be spawned in the MapActivity. It allows the user to access
 * the preferences, go to their favorites, and spawn the AboutActivity.
 * @author jens
 *
 */
public class LeftMenu extends Fragment implements iLanguageListener {
	public static final int LAUNCH_LOGIN = 501;
	public static final int LAUNCH_FAVORITE = 502;
	
    protected static final int menuItemHeight = Util.dp2px(40);
    protected static final int dividerHeight = Util.dp2px(2);

    protected int favoritesContainerHeight;
    TextView textFavorites,  textNewFavorite, textFavoriteHint;
    SortableListView favoritesList;
    protected ArrayList<FavoritesData> favorites = new ArrayList<FavoritesData>();
    ImageView imgAdd;
    tFetchFavorites fetchFavorites;
    ImageButton btnEditFavorites;
    LinearLayout addContainer;
    protected RelativeLayout favoritesHeaderContainer, favoritesContainer, profileContainer, aboutContainer, settingsContainer;
    Button btnDone;
    View lastListDivider;
    private boolean isEditMode = false;
    public boolean favoritesEnabled = true;
    private ListAdapter listAdapter;
    protected ListView menuList;
    
    public ArrayList<LeftMenuItem> menuItems;
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
    	LeftMenu self = this;
    	
        LOG.d("Left menu on createView");
        final View ret = inflater.inflate(R.layout.fragment_left_menu, container, false);
        lastListDivider = ret.findViewById(R.id.lastListDivider);
        textFavorites = (TextView) ret.findViewById(R.id.textFavorites);
        textNewFavorite = (TextView) ret.findViewById(R.id.textNewFavorite);
        textFavoriteHint = (TextView) ret.findViewById(R.id.textFavoriteHint);
        imgAdd = (ImageView) ret.findViewById(R.id.imgAdd);
        addContainer = (LinearLayout) ret.findViewById(R.id.addContainer);
        favoritesContainer = (RelativeLayout) ret.findViewById(R.id.favoritesContainer);
        favoritesHeaderContainer = (RelativeLayout) ret.findViewById(R.id.favoritesHeaderContainer);
        btnEditFavorites = (ImageButton) ret.findViewById(R.id.btnEditFavourites);
        
        
        // Initialize the menu
        this.menuList = (ListView) ret.findViewById(R.id.menuListView);
        this.menuItems = new ArrayList<LeftMenuItem>();
        
        menuItems.add(new LeftMenuItem("favorites", R.drawable.ic_menu_favorite, "spawnFavoritesListActivity"));
        
        if (IbikeApplication.isUserLogedIn() || IbikeApplication.isFacebookLogin()) {
        	menuItems.add(new LeftMenuItem("account", "spawnLoginActivity"));
        } else {
        	menuItems.add(new LeftMenuItem("login", "spawnLoginActivity"));
        }
        
        menuItems.add(new LeftMenuItem("about_app", "spawnAboutActivity"));
        menuItems.add(new LeftMenuItem("tts_settings", "spawnTTSSettingsActivity"));
        
        populateMenuList();
        
		// This is kind of magical. Each LeftMenuItem has a handler field that describes
		// a method to be called when that element is tapped. Here we read out that field
		// and launch the right method by reflection.
        this.menuList.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				String handler = menuItems.get(position).getHandler();
				spawnFunction(handler);
			}
		});
        
        // TODO: Get rid of all these handlers...
        btnEditFavorites.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                favoritesList.disableScroll();
                FavoritesAdapter adapter = (FavoritesAdapter) favoritesList.getAdapter();
                if (adapter != null)
                    adapter.setIsEditMode(isEditMode = true);
                btnDone.setVisibility(View.VISIBLE);
                btnEditFavorites.setVisibility(View.INVISIBLE);
                btnDone.setEnabled(true);
                btnEditFavorites.setEnabled(false);
                addContainer.setVisibility(View.GONE);
                updateControls();
            }
        });
        btnDone = (Button) ret.findViewById(R.id.btnDone);
        btnDone.setEnabled(false);
        btnDone.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                favoritesList.enableScroll();
                FavoritesAdapter adapter = (FavoritesAdapter) favoritesList.getAdapter();
                if (adapter != null)
                    adapter.setIsEditMode(isEditMode = false);
                btnDone.setVisibility(View.INVISIBLE);
                btnDone.setEnabled(false);
                btnEditFavorites.setVisibility(View.VISIBLE);
                btnEditFavorites.setEnabled(true);
                addContainer.setVisibility(View.VISIBLE);
                updateControls();
            }

        });
        ret.findViewById(R.id.favoritesContainer).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                if (IbikeApplication.isUserLogedIn())
                    openNewFavoriteFragment();
            }
        });
        addContainer.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                if (IbikeApplication.isUserLogedIn())
                    openNewFavoriteFragment();
            }
        });
        favoritesList = (SortableListView) ret.findViewById(R.id.favoritesList);
        favoritesList.setParentContainer(this);
        favoritesList.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
                if (!((FavoritesAdapter) favoritesList.getAdapter()).isEditMode) {
                    if (favoritesEnabled) {
                        favoritesEnabled = false;
                        onListItemClick(position);
                    }
                }

            }

        });
       
        return ret;
    }
    
    /**
     * This method is called as a click handler on all items in the menu. It is called with the 
     * name parameter corresponding to the method that the particular FavoritesListActivity denotes,
     * for example spawnAboutActivity for the About button. 
     * @param name Name of the method to be called. 
     */
    public void spawnFunction(String name) {
		Method handlerMethod;
		try {
			handlerMethod = this.getClass().getDeclaredMethod(name, null);
			handlerMethod.invoke(this);
		} catch (Exception e) {
			Log.e("Handler " + name + " not found");
			e.printStackTrace();
		}
    }
    
    protected void populateMenuList() {      
        this.menuList.setAdapter(new LeftMenuItemAdapter(IbikeApplication.getContext(), menuItems));
    }

	@Override
	public void reloadStrings() {
		Log.d("JC: LeftMenu reloadStrings");
		//this.populateMenuList();
		this.menuList.invalidate();
	}
	
    public void spawnLoginActivity() {
    	if (!Util.isNetworkConnected(getActivity())) {
            Util.launchNoConnectionDialog(getActivity());
        } else {
            Intent i;
            
            // If the user is not logged in, show her a login screen, otherwise show the relevant profile activity. 
            if ( !IbikeApplication.isUserLogedIn() && !IbikeApplication.isFacebookLogin()) {
                i = new Intent(getActivity(), LoginActivity.class);
            } else {
                if (IbikeApplication.isFacebookLogin())
                    i = new Intent(getActivity(), FacebookProfileActivity.class);
                else
                    i = new Intent(getActivity(), ProfileActivity.class);
            }
            getActivity().startActivityForResult(i, LAUNCH_LOGIN);
            getActivity().overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        }
    }
    
    public void spawnAboutActivity() {
        Intent i = new Intent(getActivity(), AboutActivity.class);
        getActivity().startActivity(i);
        getActivity().overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
	}
    
    public void spawnFavoritesListActivity() {
        Intent i = new Intent(getActivity(), FavoritesListActivity.class);
        getActivity().startActivityForResult(i, LAUNCH_FAVORITE);
        getActivity().overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
	}
	
    public void spawnLanguageActivity() {
        Util.showLanguageDialog(getActivity());
	}
    
    public void spawnTTSSettingsActivity() {
        Intent i = new Intent(getActivity(), TTSSettingsActivity.class);
        getActivity().startActivity(i);
        getActivity().overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
	}

    public void onListItemClick(int position) {
        if (!Util.isNetworkConnected(getActivity())) {
            favoritesEnabled = true;
            Util.launchNoConnectionDialog(getActivity());
        } else {
            FavoritesData fd = (FavoritesData) (favoritesList.getAdapter().getItem(position));
            if (SMLocationManager.getInstance().hasValidLocation()) {
                getActivity().findViewById(R.id.progressBar).setVisibility(View.VISIBLE);
                Location start = SMLocationManager.getInstance().getLastValidLocation();
                IbikeApplication.getTracker().sendEvent("Route", "Menu", "Favorites", (long) 0);
                new SMHttpRequest().getRoute(start, Util.locationFromCoordinates(fd.getLatitude(), fd.getLongitude()), null,
                        (MapActivity) getActivity());
            } else {
                favoritesEnabled = true;
                ((MapActivity) getActivity()).showRouteNotFoundDlg();
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        LOG.d("Left menu onResume");
        initStrings();
        if (IbikeApplication.isUserLogedIn()) {
            favorites = (new DB(getActivity())).getFavorites(favorites);
            fetchFavorites = new tFetchFavorites();
            fetchFavorites.start();
        } 
        
        listAdapter = getAdapter();
        favoritesList.setAdapter(listAdapter);
        textNewFavorite.setTextColor(getAddFavoriteTextColor());
        btnEditFavorites.setEnabled(IbikeApplication.isUserLogedIn() && favorites != null && favorites.size() != 0 && !isEditMode);
        updateControls();
    }

    protected int getAddFavoriteTextColor() {
        return Color.rgb(24, 138, 230);
    }

    public void reloadFavorites() {
        DB db = new DB(getActivity());
        favorites = db.getFavorites(favorites);
        LOG.d("update favorites from reloadFavorites() count = " + favorites.size());
        updateControls();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (fetchFavorites != null && fetchFavorites.isAlive()) {
            fetchFavorites.interrupt();
        }
    }

    public void initStrings() {
        textFavorites.setText(IbikeApplication.getString("favorites"));
        textFavorites.setTypeface(IbikeApplication.getBoldFont());
        textNewFavorite.setText(IbikeApplication.getString("cell_add_favorite"));
        textNewFavorite.setTypeface(IbikeApplication.getBoldFont());
        if (IbikeApplication.isUserLogedIn())
            textFavoriteHint.setText(IbikeApplication.getString("cell_empty_favorite_text"));
        else
            textFavoriteHint.setText(IbikeApplication.getString("favorites_login"));
        textFavoriteHint.setTypeface(IbikeApplication.getItalicFont());
        btnDone.setText(IbikeApplication.getString("Done"));
        btnDone.setTypeface(IbikeApplication.getBoldFont());
    }

    private void openNewFavoriteFragment() {
        AddFavoriteFragment aff = getAddFavoriteFragment();
        FragmentTransaction fragmentTransaction = ((FragmentActivity) getActivity()).getSupportFragmentManager().beginTransaction();
        fragmentTransaction.setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left, R.anim.slide_in_left, R.anim.slide_out_right);
        fragmentTransaction.replace(R.id.leftContainerDrawer, aff);
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
        ((FragmentActivity) getActivity()).getSupportFragmentManager().executePendingTransactions();
    }

    protected AddFavoriteFragment getAddFavoriteFragment() {
        return new AddFavoriteFragment();
    }

    protected EditFavoriteFragment getEditFavoriteFragment() {
        return new EditFavoriteFragment();
    }

    @SuppressWarnings("deprecation")
    public void updateControls() {
        LOG.d("LeftMenu updateControls");
        if (!IbikeApplication.isUserLogedIn()) {
            favoritesContainer.setBackgroundDrawable(null);
            textFavoriteHint.setTextColor(getHintDisabledTextColor());
            textNewFavorite.setTextColor(Color.rgb(60, 60, 60));
            imgAdd.setImageResource(R.drawable.fav_plus_none);
            favoritesList.setAdapter(null);
            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, Util.dp2px(150));
            favoritesContainerHeight = Util.dp2px(150);
            params.addRule(RelativeLayout.CENTER_HORIZONTAL);
            params.addRule(RelativeLayout.BELOW, favoritesHeaderContainer.getId());
            favoritesContainer.setLayoutParams(params);
            addContainer.setPadding((int) (Util.getScreenWidth() / 7 + Util.dp2px(7)), (int) Util.dp2px(5), 0, (int) Util.dp2px(34));
            // getView().findViewById(R.id.imgRightArrow).setVisibility(View.INVISIBLE);
            textFavoriteHint.setVisibility(View.VISIBLE);
            btnEditFavorites.setVisibility(View.INVISIBLE);
            btnEditFavorites.setEnabled(false);
            lastListDivider.setVisibility(View.GONE);
            params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
            params.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
            params.addRule(RelativeLayout.ALIGN_PARENT_TOP);
            params.topMargin = Util.dp2px(3);
            // imgAdd.setLayoutParams(params);
            textNewFavorite.setPadding(Util.dp2px(0), 0, 0, Util.dp2px(0));
            params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
            params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
            params.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
            params.bottomMargin = Util.getDensity() >= 2 ? Util.dp2px(40) : Util.dp2px(20);
            addContainer.setLayoutParams(params);
            addContainer.setPadding(Util.getDensity() >= 2 ? Util.dp2px(60) : Util.dp2px(40), Util.dp2px(7), 0, Util.dp2px(7));
            addContainer.setBackgroundColor(Color.TRANSPARENT);
            addContainer.setClickable(false);
        } else if (favorites == null || favorites.size() == 0) {
            favoritesContainer.setBackgroundResource(R.drawable.add_fav_background_selector);
            addContainer.setBackgroundColor(Color.TRANSPARENT);// addContainer.setBackgroundResource(R.drawable.add_fav_background_selector);
            lastListDivider.setVisibility(View.GONE);
            favoritesList.setVisibility(View.GONE);
            textFavoriteHint.setVisibility(View.VISIBLE);
            // getView().findViewById(R.id.imgRightArrow).setVisibility(View.VISIBLE);
            favoritesContainer.setClickable(true);
            imgAdd.setImageResource(R.drawable.fav_add);
            textFavoriteHint.setTextColor(getHintEnabledTextColor());
            textNewFavorite.setTextColor(getAddFavoriteTextColor());
            btnEditFavorites.setVisibility(View.INVISIBLE);
            btnEditFavorites.setEnabled(false);
            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, Util.dp2px(150));
            favoritesContainerHeight = Util.dp2px(150);
            params.addRule(RelativeLayout.CENTER_HORIZONTAL);
            params.addRule(RelativeLayout.BELOW, getView().findViewById(R.id.favoritesHeaderContainer).getId());
            favoritesContainer.setLayoutParams(params);
            addContainer.setPadding((int) (Util.getScreenWidth() / 7 + Util.dp2px(7)), (int) Util.dp2px(5), 0, (int) Util.dp2px(34));
            params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
            params.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
            params.addRule(RelativeLayout.ALIGN_PARENT_TOP);
            params.topMargin = Util.dp2px(3);
            // imgAdd.setLayoutParams(params);
            textNewFavorite.setPadding(0, 0, 0, 0);
            params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
            params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
            params.addRule(RelativeLayout.CENTER_HORIZONTAL);
            params.bottomMargin = Util.getDensity() >= 2 ? Util.dp2px(40) : Util.dp2px(20);
            addContainer.setLayoutParams(params);
            addContainer.setPadding(Util.getDensity() >= 2 ? Util.dp2px(0) : Util.dp2px(10), Util.dp2px(7), 0, Util.dp2px(7));
            addContainer.setClickable(false);
        } else {
            // Loged in, and there is a list of favorites
            favoritesList.clearAnimations();
            favoritesList.setVisibility(View.VISIBLE);
            if (listAdapter != null) {
                ((FavoritesAdapter) listAdapter).setIsEditMode(isEditMode);
                btnEditFavorites.setVisibility(isEditMode ? View.INVISIBLE : View.VISIBLE);
                btnEditFavorites.setEnabled(!isEditMode);
                btnDone.setVisibility(isEditMode ? View.VISIBLE : View.INVISIBLE);
                btnDone.setEnabled(isEditMode);
            }
            textFavoriteHint.setVisibility(View.GONE);
            if (getView() != null) {
                addContainer.setVisibility(isEditMode ? View.GONE : View.VISIBLE);
                // getView().findViewById(R.id.imgRightArrow).setVisibility(View.GONE);
                getView().findViewById(R.id.favoritesContainer).setClickable(false);
                int count = favorites.size();
                int listHeight = count * (menuItemHeight) + Util.dp2px(1) * count;
                int viewHeight = (int) (Util.getScreenHeight() - Util.dp2px(26)); //
                // screen height without the
                // notifications bar
                int avaliableHeight = viewHeight - (menuItemHeight * (getMenuItemsCount() + (isEditMode ? 0 : 1)))
                        - (getMenuItemsCount() * dividerHeight);
                LOG.d("available height = " + avaliableHeight);
                LOG.d("list height = " + listHeight);
                if (listHeight > avaliableHeight) {
                    listHeight = avaliableHeight;
                }
                RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT,
                        (isEditMode ? listHeight : (listHeight + menuItemHeight)));
                favoritesContainerHeight = (isEditMode ? listHeight : (listHeight + menuItemHeight));
                params.addRule(RelativeLayout.CENTER_HORIZONTAL);
                params.addRule(RelativeLayout.BELOW, getView().findViewById(R.id.horizontalDivider1).getId());
                favoritesContainer.setLayoutParams(params);
                params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, listHeight);
                params.addRule(RelativeLayout.CENTER_HORIZONTAL);
                params.addRule(RelativeLayout.ALIGN_PARENT_TOP);
                getView().findViewById(R.id.favoritesListContainer).setLayoutParams(params);
                imgAdd.setImageResource(R.drawable.fav_add);
                textFavoriteHint.setTextColor(Color.WHITE);
                textNewFavorite.setTextColor(getAddFavoriteTextColor());
                lastListDivider.setVisibility((isEditMode || favorites.size() <= getFavoritesVisibleItemCount()) ? View.GONE : View.VISIBLE);
                updateFavoritesContainer();
                addContainer.setPadding((int) Util.dp2px(30), (int) Util.dp2px(0), 0, (int) Util.dp2px(0));
                params = new RelativeLayout.LayoutParams(Util.dp2px(14), Util.dp2px(14));
                params.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
                params.addRule(RelativeLayout.CENTER_VERTICAL);
                params.bottomMargin = Util.dp2px(0);
                // imgAdd.setLayoutParams(params);
                params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, menuItemHeight);
                params.addRule(RelativeLayout.BELOW, getView().findViewById(R.id.favoritesListContainer).getId());
                if (!isEditMode) {
                    addContainer.setLayoutParams(params);
                }
                addContainer.setBackgroundResource(R.drawable.add_fav_background_selector);
                addContainer.setClickable(true);
                textNewFavorite.setPadding(Util.dp2px(4), 0, 0, 0);
                ((FavoritesAdapter) listAdapter).notifyDataSetChanged();
            }
        }

    }

    protected int getMenuItemsCount() {
        return 5;
    }

    protected int getHintEnabledTextColor() {
        return Color.WHITE;
    }

    protected int getHintDisabledTextColor() {
        return Color.GRAY;
    }

    public int getFavoritesVisibleItemCount() {
        return Util.getDensity() >= 2 ? 9 : 8;
    }

    FavoritesAdapter adapter;

    protected FavoritesAdapter getAdapter() {
        if (adapter == null) {
            adapter = new FavoritesAdapter(getActivity(), favorites, this);
        }
        return adapter;
    }

    private class tFetchFavorites extends Thread {
        @Override
        public void run() {
            while (!interrupted()) {
                LOG.d("fetching the favorites");
                final ArrayList<FavoritesData> favs = (new DB(getActivity())).getFavoritesFromServer(getActivity(), null);
                if (LeftMenu.this != null && LeftMenu.this.getActivity() != null) {
                    LeftMenu.this.getActivity().runOnUiThread(new Runnable() {
                        public void run() {
                            favorites.clear();
                            favorites.addAll(favs);
                            ((FavoritesAdapter) listAdapter).notifyDataSetChanged();
                            if (getView() != null) {
                                LOG.d("update favorites from thread count = " + favorites.size());
                                updateControls();
                                if (favs.size() == 0) {
                                    ((MapActivity) getActivity()).showWelcomeScreen();
                                } else {
                                    IbikeApplication.setWelcomeScreenSeen(true);
                                }
                            }
                        }
                    });
                    if (Util.isNetworkConnected(getActivity())) {
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

    public void onEditFavorite(FavoritesData fd) {
        EditFavoriteFragment eff = getEditFavoriteFragment();
        Bundle args = new Bundle();
        args.putParcelable("favoritesData", fd);
        eff.setArguments(args);
        FragmentTransaction fragmentTransaction = ((FragmentActivity) getActivity()).getSupportFragmentManager().beginTransaction();
        fragmentTransaction.setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left, R.anim.slide_in_left, R.anim.slide_out_right);
        fragmentTransaction.replace(R.id.leftContainerDrawer, eff);
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
        ((FragmentActivity) getActivity()).getSupportFragmentManager().executePendingTransactions();

    }

    private void updateFavoritesContainer() {
        if (isEditMode || (favoritesList.getAdapter() != null && favoritesList.getAdapter().getCount() <= getFavoritesVisibleItemCount()))
            lastListDivider.setVisibility(View.GONE);
        else
            lastListDivider.setVisibility(View.VISIBLE);
    }

}
