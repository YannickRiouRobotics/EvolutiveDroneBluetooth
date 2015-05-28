//==================================================//
//= 			Homescreen.java 				 ===//
//= 				06/02/15					 ===//
//=				Yannick Riou EII3 				 ===//
//= 		First Activity that let the user	 ===//
//=			choose the bluetooth device (drone)  ===//
//= 		and connect to it					 ===//
//= 				ENSSAT©			     		 ===//
//==================================================//

// Name of the application's package
package com.projeteii3.enssatdrone;

//Import various libraries, press ctrl+shift+o to update
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

//Homescreen Activity class 
public class Homescreen extends Activity {

	// User Interface Components Definition
	private Button mBtnSearch;
	private Button mBtnConnect;
	private ListView mLstDevices;

	// Bluetooth Adaptater for communication
	private BluetoothAdapter mBTAdapter;

	// Bluetooth Paramaters
	private static final int BT_ENABLE_REQUEST = 10; // This is the code we use
														// for BT Enable
	private static final int SETTINGS = 20;

	// Device UUID needed for BT communication
	private UUID mDeviceUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); // Standard SPP UUID

	// Information about bluetooth device
	public static final String DEVICE_EXTRA = "com.projeteii3.enssatdrone.SOCKET";
	public static final String DEVICE_UUID = "com.projeteii3.enssatdrone.uuid";
	private static final String DEVICE_LIST = "com.projeteii3.enssatdrone.devicelist";
	private static final String DEVICE_LIST_SELECTED = "com.projeteii3.enssatdrone.devicelistselected";
	public static final String BUFFER_SIZE = "com.projeteii3.enssatdrone.buffersize";

	// Debug tag for logcat
	 // Debug tag to identify various message
    // Use Log.d(TAG,"Message") to write something to the LogCat (see window below)
	private static final String TAG = "Debug-HomescreenActivity";

	// Method called when Homescreen Activity is created (after user clicked on application Icon)
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Load the layout
		// For more information, take a look at: ProjectFolder/EnssatDrone/res/layout/activity_homescreen
		setContentView(R.layout.activity_homescreen);
		Log.d(TAG, "Created");

		// Setup the user interface
		setupHomescreenUI();

		/*
		 * Check if there is a savedInstanceState. If yes, that means the
		 * onCreate was probably triggered by a configuration changelike screen
		 * rotate etc. If that's the case then populate all the views that are
		 * necessary here
		 */
		if (savedInstanceState != null) {
			ArrayList<BluetoothDevice> list = savedInstanceState
					.getParcelableArrayList(DEVICE_LIST);
			if (list != null) {
				initList(list);
				MyAdapter adapter = (MyAdapter) mLstDevices.getAdapter();
				int selectedIndex = savedInstanceState
						.getInt(DEVICE_LIST_SELECTED);
				if (selectedIndex != -1) {
					adapter.setSelectedIndex(selectedIndex);
					mBtnConnect.setEnabled(true);
				}
			} else {
				initList(new ArrayList<BluetoothDevice>());
			}

		} else {
			initList(new ArrayList<BluetoothDevice>());
		}
		
		mBTAdapter = BluetoothAdapter.getDefaultAdapter();

		if (mBTAdapter == null) {
			Toast.makeText(getApplicationContext(),
					"Bluetooth not found", Toast.LENGTH_SHORT).show();
		} else if (!mBTAdapter.isEnabled()) {
			Intent enableBT = new Intent(
					BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableBT, BT_ENABLE_REQUEST);
		} else {
			new SearchDevices().execute();
		}

		

	} // End of onCreate

	
	// Setup various components for the User interface
	protected void setupHomescreenUI() {
		
		// Component for User Interface
		mBtnConnect = (Button) findViewById(R.id.btnConnect);
		mLstDevices = (ListView) findViewById(R.id.lstDevices);

		// When connect button is clicked, launch MainActivity and send info to it with Intent class
		mBtnConnect.setOnClickListener(new OnClickListener() {
			public void onClick(View arg0) {
				// Get the bluetooth device which was choose by the user
				BluetoothDevice device = ((MyAdapter) (mLstDevices.getAdapter())).getSelectedItem();
				
				// Create an Intent to send information from Homescreen Activity (this one) to the MainActivity
				Intent intent = new Intent(getApplicationContext(),MainActivity.class);
				// Put information about choosen bluetooth device in the intent
				intent.putExtra(DEVICE_EXTRA, device);
				intent.putExtra(DEVICE_UUID, mDeviceUUID.toString());
				
				// Start the activity and give the above information to it
				startActivity(intent);
			}
		});
	}

	/**
	 * Called when the screen rotates. If this isn't handled, data already
	 * generated is no longer available
	 */
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		MyAdapter adapter = (MyAdapter) (mLstDevices.getAdapter());
		ArrayList<BluetoothDevice> list = (ArrayList<BluetoothDevice>) adapter
				.getEntireList();

		if (list != null) {
			outState.putParcelableArrayList(DEVICE_LIST, list);
			int selectedIndex = adapter.selectedIndex;
			outState.putInt(DEVICE_LIST_SELECTED, selectedIndex);
		}
	}

	// When application is set to background
	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
	}

	// When application is stopped
	@Override
	protected void onStop() {
		// TODO Auto-generated method stub
		super.onStop();
	}

	// Start an activity and get result back
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		// When app ask user to activate bluetooth
		case BT_ENABLE_REQUEST:
			if (resultCode == RESULT_OK) {
				msg("Bluetooth Enabled successfully");
				new SearchDevices().execute();
			} else {
				msg("Bluetooth couldn't be enabled");
			}

			break;
		case SETTINGS: // If the settings have been updated
			SharedPreferences prefs = PreferenceManager
					.getDefaultSharedPreferences(this);
			String uuid = prefs.getString("prefUuid", "Null");
			mDeviceUUID = UUID.fromString(uuid);
			Log.d(TAG, "UUID: " + uuid);

			String orientation = prefs.getString("prefOrientation", "Null");
			Log.d(TAG, "Orientation: " + orientation);
			if (orientation.equals("Landscape")) {
				setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
			} else if (orientation.equals("Portrait")) {
				setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
			} else if (orientation.equals("Auto")) {
				setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR);
			}
			break;
		default:
			break;
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	/**
	 * Quick way to call the Toast
	 * 
	 * @param str
	 */
	private void msg(String str) {
		Toast.makeText(getApplicationContext(), str, Toast.LENGTH_SHORT).show();
	}

	// All that follow is about the list that show the various paired bluetooth device
	/**
	 * Initialize the List adapter
	 * 
	 * @param objects
	 */
	private void initList(List<BluetoothDevice> objects) {
		final MyAdapter adapter = new MyAdapter(getApplicationContext(),
				R.layout.list_item, R.id.lstContent, objects);
		mLstDevices.setAdapter(adapter);
		mLstDevices.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				adapter.setSelectedIndex(position);
				mBtnConnect.setEnabled(true);
			}
		});
	}

	/**
	 * Searches for paired devices. Doesn't do a scan! Only devices which are
	 * paired through Settings->Bluetooth will show up with this. I didn't see
	 * any need to re-build the wheel over here
	 * 
	 * @author ryder
	 * 
	 */
	private class SearchDevices extends
			AsyncTask<Void, Void, List<BluetoothDevice>> {

		@Override
		protected List<BluetoothDevice> doInBackground(Void... params) {
			Set<BluetoothDevice> pairedDevices = mBTAdapter.getBondedDevices();
			List<BluetoothDevice> listDevices = new ArrayList<BluetoothDevice>();
			for (BluetoothDevice device : pairedDevices) {
				listDevices.add(device);
			}
			return listDevices;

		}

		@Override
		protected void onPostExecute(List<BluetoothDevice> listDevices) {
			super.onPostExecute(listDevices);
			if (listDevices.size() > 0) {
				MyAdapter adapter = (MyAdapter) mLstDevices.getAdapter();
				adapter.replaceItems(listDevices);
			} else {
				msg("No paired devices found, please pair your serial BT device and try again");
			}
		}

	}

	/**
	 * Custom adapter to show the current devices in the list. This is a bit of
	 * an overkill for this project, but I figured it would be good learning
	 * Most of the code is lifted from somewhere but I can't find the link
	 * anymore
	 * 
	 * @author ryder
	 * 
	 */
	private class MyAdapter extends ArrayAdapter<BluetoothDevice> {
		private int selectedIndex;
		private Context context;
		private int selectedColor = Color.parseColor("#abcdef");
		private List<BluetoothDevice> myList;

		public MyAdapter(Context ctx, int resource, int textViewResourceId,
				List<BluetoothDevice> objects) {
			super(ctx, resource, textViewResourceId, objects);
			context = ctx;
			myList = objects;
			selectedIndex = -1;
		}

		public void setSelectedIndex(int position) {
			selectedIndex = position;
			notifyDataSetChanged();
		}

		public BluetoothDevice getSelectedItem() {
			return myList.get(selectedIndex);
		}

		@Override
		public int getCount() {
			return myList.size();
		}

		@Override
		public BluetoothDevice getItem(int position) {
			return myList.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		private class ViewHolder {
			TextView tv;
		}

		public void replaceItems(List<BluetoothDevice> list) {
			myList = list;
			notifyDataSetChanged();
		}

		public List<BluetoothDevice> getEntireList() {
			return myList;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View vi = convertView;
			ViewHolder holder;
			if (convertView == null) {
				vi = LayoutInflater.from(context).inflate(R.layout.list_item,
						null);
				holder = new ViewHolder();

				holder.tv = (TextView) vi.findViewById(R.id.lstContent);

				vi.setTag(holder);
			} else {
				holder = (ViewHolder) vi.getTag();
			}

			if (selectedIndex != -1 && position == selectedIndex) {
				holder.tv.setBackgroundColor(selectedColor);
			} else {
				holder.tv.setBackgroundColor(Color.WHITE);
			}
			BluetoothDevice device = myList.get(position);
			holder.tv.setText(device.getName() + "\n   " + device.getAddress());

			return vi;
		}

	}
}
