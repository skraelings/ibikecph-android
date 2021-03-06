// Copyright (C) 2013 City of Copenhagen.
//
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
// If a copy of the MPL was not distributed with this file, You can obtain one at 
// http://mozilla.org/MPL/2.0/.
package com.spoiledmilk.ibikecph.search;

import java.util.ArrayList;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.spoiledmilk.ibikecph.IbikeApplication;
import com.spoiledmilk.ibikecph.R;

public class HistoryAdapter extends ArrayAdapter<SearchListItem> {

	private LayoutInflater inflater;

	public HistoryAdapter(Context context, ArrayList<SearchListItem> objects) {
		super(context, R.layout.list_row_history, objects);
		inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ViewHolder holder;
		View view = convertView;
		if (view == null) {
			view = inflater.inflate(R.layout.list_row_history, parent, false);
			holder = new ViewHolder();
			view.setTag(holder);
			holder.imgIcon = (ImageView) view.findViewById(R.id.imgIcon);
			holder.textLocation = (TextView) view.findViewById(R.id.textHistory);
		} else {
			holder = (ViewHolder) view.getTag();
		}
		final SearchListItem item = getItem(position);

		holder.textLocation.setTypeface(IbikeApplication.getNormalFont());
		String text = item.getName();
		if (item.getName().length() > 30) {
			text = item.getName().substring(0, 27) + "...";
		}
		holder.textLocation.setText(text);
		holder.imgIcon.setImageResource(item.getIconResourceId());
		return view;
	}

	class ViewHolder {
		public ImageView imgIcon;
		public TextView textLocation;
	}
}
