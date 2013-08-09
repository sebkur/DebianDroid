// TODO: settings reload when changed, all string to xml, udd ui, qr code scan/results etc
// add check for internet connection in parts of ui and notify user, pts/bts results ui,
// create launcher widget, code cleanup/comments

package com.debian.debiandroid;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.debian.debiandroid.apiLayer.*;
import com.debian.debiandroid.content.ContentMenu;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.view.GestureDetectorCompat;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;


/**
 * An activity representing a list of Items. This activity
 * has different presentations for handset and tablet-size devices. On
 * handsets, the activity presents a list of items, which when touched,
 * lead to a {@link ItemDetailActivity} representing
 * item details. On tablets, the activity presents the list of items and
 * item details side-by-side using two vertical panes.
 * <p>
 * The activity makes heavy use of fragments. The list of items is a
 * {@link ItemListFragment} and the item details
 * (if present) is a {@link ItemDetailFragment}.
 * <p>
 * This activity also implements the required
 * {@link ItemListFragment.Callbacks} interface
 * to listen for item selections.
 */
public class ItemListActivity extends SherlockFragmentActivity
        implements ItemListFragment.Callbacks {

	private GestureDetectorCompat gestureDetector;
	
    /**
     * Whether or not the activity is in two-pane mode, i.e. running on a tablet
     * device.
     */
    private boolean mTwoPane;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_item_list);

        if (findViewById(R.id.item_detail_container) != null) {
            // The detail container view will be present only in the
            // large-screen layouts (res/values-large and
            // res/values-sw600dp). If this view is present, then the
            // activity should be in two-pane mode.
            mTwoPane = true;

            // In two-pane mode, list items should be given the
            // 'activated' state when touched.
            ((ItemListFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.item_list))
                    .setActivateOnItemClick(true);
            
            gestureDetector = new GestureDetectorCompat(this, new SwipeListener());
            onItemSelected(ContentMenu.ITEM.PTS.toString());
            
        }
              
        new task().execute(); //temporary
        
        // Load stored settings before starting service
        SettingsActivity.loadSettings(getApplicationContext());
        
        // Start service that auto updates subscribed packages and notifies user
        PendingIntent pintent = PendingIntent.getService(this, 0, new Intent(this, DDNotifyService.class), 0);

        AlarmManager alarm = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
        // Start service again every 300 seconds
        alarm.setRepeating(AlarmManager.RTC_WAKEUP, Calendar.getInstance().getTimeInMillis(), 300*1000, pintent);
        
        // Check if app opened links to bugs.debian.org or packages.qa.debian.org 
        Intent intent = getIntent();
        if (Intent.ACTION_VIEW.equals(intent.getAction())) {
        	  Uri uri = intent.getData();
        	  
        	  System.out.println("URI: " + uri);
        	  
        	  // Parse uri to get search parameters and forward to corresponding fragment
        	  if(PTS.isPTSHost(uri.getHost())) {
        		  SearchCacher.setLastSearchByPTSURI(uri);
        		  onItemSelected(ContentMenu.ITEM.PTS.toString());
        	  }
        	  if(BTS.isBTSHost(uri.getHost())) {
        		  uri = Uri.parse(uri.toString().replace(';', '&'));
        		  SearchCacher.setLastSearchByBTSURI(uri);
        		  onItemSelected(ContentMenu.ITEM.BTS.toString());
        	  }
        }
    }
    
    class task extends AsyncTask<Void, Void, Void> {
		@Override
		protected Void doInBackground(Void... params) {
			
			//System.out.println( new BTSSoapCaller(getApplicationContext()).getBugLog(264023));
			//System.out.println(new UDDCaller(getApplicationContext()).getLastUploads());
			//Log.i("Debian", new BTSSoapCaller(getApplicationContext()).getStatus(new int[]{264023, 407364}));
			
		    /*Iterator<Integer> iterator = map.keySet().iterator();  
		       
		    while (iterator.hasNext()) {
		    	int key = (Integer) iterator.next();
		         key + " " + map.get(key));
		    }  */
			return null;
		}  
    }
    
    @Override 
    public boolean onTouchEvent(MotionEvent event){ 
    	if (mTwoPane && gestureDetector.onTouchEvent(event)) {
            	return true;
    	}
        return super.onTouchEvent(event);
    }
    
    @Override
    public boolean dispatchTouchEvent(MotionEvent ev){
    	if(!mTwoPane) {
    		return super.dispatchTouchEvent(ev);
    	}
    	super.dispatchTouchEvent(ev);
        return gestureDetector.onTouchEvent(ev);
    }
    
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
    	if (mTwoPane) {
	    	//Forward the qrcode scan result to the corresponding CIFFragment
	    	ItemDetailFragment fragment = (ItemDetailFragment) getSupportFragmentManager().findFragmentById(R.id.item_detail_container);
	    	if(ItemDetailFragment.currentFragmentID.equals(ContentMenu.ITEM.CIF.toString()))
	    		fragment.onActivityResult(requestCode, resultCode, intent);
    	}
	}

    /**
     * Callback method from {@link ItemListFragment.Callbacks}
     * indicating that the item with the given ID was selected
     * and that the user will be forwarded to the appropriate fragment
     * or ItemDetailActivity.
     */
    @Override
    public void onItemSelected(String id) {
    	if (mTwoPane) {
            // In two-pane mode, show the detail view in this activity by
            // adding or replacing the detail fragment using a
            // fragment transaction.
            ItemDetailFragment fragment = ItemDetailFragment.getDetailFragment(id);
            Bundle arguments = new Bundle();
            arguments.putString(ItemDetailFragment.ARG_ITEM_ID, id);
        	fragment.setArguments(arguments);
        	
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.item_detail_container, fragment)
                    .commit();
        } else {
            // In single-pane mode, simply start the detail activity
            // for the selected item ID.
            Intent detailIntent = new Intent(this, ItemDetailActivity.class);
            detailIntent.putExtra(ItemDetailFragment.ARG_ITEM_ID, id);
            startActivity(detailIntent);
        }
    }
    
class SwipeListener extends GestureDetector.SimpleOnGestureListener {
    	
    	private static final int SWIPE_THRESHOLD = 100;
        private static final int SWIPE_VELOCITY_THRESHOLD = 100;
        
        @Override
        public boolean onDown(MotionEvent event) { 
            return true;
        }
        
        @Override
        public boolean onFling(MotionEvent event1, MotionEvent event2, float velocityX, float velocityY) {
            try {
                float diffY = event2.getY() - event1.getY();
                float diffX = event2.getX() - event1.getX();
                if (Math.abs(diffX) > Math.abs(diffY)) {
                    if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                        if (diffX > 0) {
                        	// Swipe right
                        	onItemSelected(ItemDetailFragment.getPreviousFragmentId());
                        } else {
                        	// Swipe left
                        	onItemSelected(ItemDetailFragment.getNextFragmentId());
                        }
                        return true;
                    }
                } else {
                    if (Math.abs(diffY) > SWIPE_THRESHOLD && Math.abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {
                        if (diffY > 0) {
                        	//Bottom swipe
                        } else {
                        	//Top swipe
                        }
                    }
                }
            } catch (Exception exception) {
                exception.printStackTrace();
            }
            return false;
        }
    }

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		if (!mTwoPane) {
		    ItemDetailFragment.getSettingsMenuItem(menu);
		    return true;
		}
	    return false;
	}
	
	@Override
    public boolean onOptionsItemSelected(MenuItem item) {
		if (!mTwoPane) {
	    	 switch(item.getItemId()){
		    	 case ItemDetailFragment.SETTINGS_ID: 
		    		 startActivity(new Intent(this, SettingsActivity.class));
		        	return true;
	        }
    	}
		return super.onOptionsItemSelected(item);
    }
}
