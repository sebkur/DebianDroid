package com.debian.debiandroid.content;

import java.util.ArrayList;
import java.util.Arrays;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.debian.debiandroid.DExpandableAdapter;
import com.debian.debiandroid.ItemDetailFragment;
import com.debian.debiandroid.R;
import com.debian.debiandroid.apiLayer.BTS;
import com.debian.debiandroid.apiLayer.PTS;
import com.debian.debiandroid.utils.SearchCacher;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.TextView.OnEditorActionListener;
import androidStorageUtils.Cacher;

public class PTSFragment extends ItemDetailFragment {

	private ImageButton searchButton;
	private EditText ptsInput;
	private PTS pts;
	private ExpandableListView bugList;
	private TextView ptsPckgInfo;
	
	private ArrayList<String> bugListParentItems;
	private ArrayList<Object> bugListChildItems;
	
	/** ID for the (un)subscribe menu item. It starts from +2 
	 * because the settings icon is in the +1 position */
	public static final int SUBSCRIPTION_ID = Menu.FIRST+2;
	public static final int REFRESH_ID = Menu.FIRST+3;
	public static final int NEW_EMAIL_ID = Menu.FIRST+4;
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        pts = new PTS(getSherlockActivity().getApplicationContext());
        if(SearchCacher.hasLastPckgSearch()) {
        	new SearchPackageInfoTask().execute();
        }
        
        bugListParentItems = new ArrayList<String>();
		bugListChildItems = new ArrayList<Object>();
    }
	
	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
    	View rootView = inflater.inflate(R.layout.pts_fragment, container, false);

    	bugList = (ExpandableListView) rootView.findViewById(R.id.ptsBugsList);
    	ViewGroup header = (ViewGroup)inflater.inflate(R.layout.pts_exp_list_header, bugList, false);
    	bugList.addHeaderView(header, null, false);
    	
    	getSherlockActivity().getSupportActionBar().setTitle(getString(R.string.search_packages));
    	
    	searchButton = (ImageButton) rootView.findViewById(R.id.ptsSearchButton);
    	ptsInput = (EditText) rootView.findViewById(R.id.ptsInputSearch);
    	ptsPckgInfo = (TextView) rootView.findViewById(R.id.ptsPckgInfo);
    	
  		searchButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            	String input = ptsInput.getText().toString().trim();
            	if(input!=null && !input.equals("")) {
	            	SearchCacher.setLastSearchByPckgName(input);
	            	new SearchPackageInfoTask().execute();
            	}
            }
        });
  		
  		ptsInput.setOnEditorActionListener(new OnEditorActionListener() {
  		    @Override
  		    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
		        String input = ptsInput.getText().toString().trim();
  		        if (actionId == EditorInfo.IME_ACTION_SEARCH && input!=null && !input.equals("")) {
  		        	SearchCacher.setLastSearchByPckgName(input);
  		        	new SearchPackageInfoTask().execute();
  		            return true;
  		        }
  		        return false;
  		    }
  		});
    	
        return rootView;
    }
	
	public void setupBugsList() {    	
		bugList.setDividerHeight(1);
		bugList.setClickable(true);
    	
    	final DExpandableAdapter adapter = new DExpandableAdapter(bugListParentItems, bugListChildItems);
    	adapter.setInflater((LayoutInflater) getSherlockActivity()
    						.getSystemService(Context.LAYOUT_INFLATER_SERVICE));
    	bugList.setAdapter(adapter);
    	registerForContextMenu(bugList);
    	
    	bugList.setOnChildClickListener(new OnChildClickListener() {
            public boolean onChildClick(ExpandableListView parent, View view,
                    int groupPosition, int childPosition, long id) {
            	String itemClicked = ((TextView)view).getText().toString().trim();
            	if(!"".equals(itemClicked)) {
	                //save search by bug num
	                SearchCacher.setLastBugSearch(BTS.BUGNUMBER, itemClicked);
	                // Move to bts fragment
	      		  	ItemDetailFragment fragment = ItemDetailFragment.getDetailFragment(
	      				  Content.BTS);
	          		getActivity().getSupportFragmentManager().beginTransaction()
	              	.replace(R.id.item_detail_container, fragment)
	              	.commit();
            	}
                return true;
            }
        });
	}
	
	public void setBugData(String pkgName) {
		bugListParentItems = new ArrayList<String>();
		bugListChildItems = new ArrayList<Object>();
		
		Context context = getSherlockActivity().getApplicationContext();
		BTS bts = new BTS(context);
		
		ArrayList<String> bugs = bts.getBugs(new String[]{BTS.PACKAGE}, new String[]{pkgName});
		bugListParentItems.add(getString(R.string.all_bugs) + " (" + bugs.size() + ")");
	    bugListChildItems.add(bugs);
	}
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		//Add subscription icon
		MenuItem subMenuItem = menu.add(0, SUBSCRIPTION_ID, Menu.CATEGORY_SECONDARY, "(Un)Subscribe");
		subMenuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
	 	setSubscriptionIcon(subMenuItem, SearchCacher.getLastPckgName());
		
		menu.add(0, REFRESH_ID, Menu.CATEGORY_ALTERNATIVE, getString(R.string.refresh))
				.setIcon(R.drawable.refresh)
				.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
		menu.add(0, NEW_EMAIL_ID, Menu.CATEGORY_SECONDARY, getString(R.string.submit_new_bug_report))
				.setIcon(R.drawable.new_email)
				.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
		
	    super.onCreateOptionsMenu(menu, inflater);
	}
	
	@Override
	public void onPrepareOptionsMenu(Menu menu){
		setSubscriptionIcon(menu.findItem(SUBSCRIPTION_ID), SearchCacher.getLastPckgName());
		super.onPrepareOptionsMenu(menu);
	}

	public void setSubscriptionIcon(MenuItem subMenuItem, String pckgName) {
		if(pckgName!=null && pts.isSubscribedTo(pckgName)) {
			subMenuItem.setIcon(R.drawable.subscribed);
			subMenuItem.setTitle(getString(R.string.unsubscribe));
		} else {
			subMenuItem.setIcon(R.drawable.unsubscribed);
			subMenuItem.setTitle(getString(R.string.subscribe));
		}
	}
	
	@Override
    public boolean onOptionsItemSelected(MenuItem item) {
	    	 switch(item.getItemId()){
		    	 case SUBSCRIPTION_ID:
		    		 	String pckgName = SearchCacher.getLastPckgName();
		    		 	if(pckgName!=null) {
				    		if(pts.isSubscribedTo(pckgName)) {
				    			item.setIcon(R.drawable.unsubscribed);
				    			item.setTitle(getString(R.string.subscribe));
				    			pts.removeSubscriptionTo(pckgName);
				    		} else {
				    			item.setIcon(R.drawable.subscribed);
				    			item.setTitle(getString(R.string.unsubscribe));
				    			pts.addSubscriptionTo(pckgName);
				    		}
				    	}
			    		return true;
		    	 case REFRESH_ID:
			    		 if(SearchCacher.hasLastPckgSearch()) {
			    		 	new SearchPackageInfoTask().execute(true);
			    		 }
			    		return true;
		    	 case NEW_EMAIL_ID:
		    		 	forwardToMailApp();
		    		 	return true;
	        }
		return super.onOptionsItemSelected(item);
    }
	
	private void forwardToMailApp() {
		if(SearchCacher.hasLastPckgSearch()) {
			String pckgName = SearchCacher.getLastPckgName();
			
			forwardToMailApp(getSherlockActivity(), BTS.NEWBUGREPORTMAIL, BTS.getNewBugReportSubject(pckgName),
					BTS.getNewBugReportBody(pckgName, SearchCacher.getLastPckgVersion() ));
		 }
	}

	class SearchPackageInfoTask extends AsyncTask<Boolean, Integer, Void> {
		private String pckgName, pckgVersion, pckgBugCount;
		private StringBuffer pckgInfo;
		private final static int pckgInfoCount = 6;
		
		private ProgressDialog progressDialog;
		private String progressMessage =  getString(R.string.searching_info_about) + " " + SearchCacher.getLastPckgName() 
				   + ". " + getString(R.string.please_wait) + "...";
		
		protected void onPreExecute(){ 
		   super.onPreExecute();
		   hideSoftKeyboard(ptsInput);
		   progressDialog = ProgressDialog.show(getSherlockActivity(), getString(R.string.searching),
				   progressMessage, true, false);  
		}
		
		protected Void doInBackground(Boolean... params) {
			//If called with execute(true) set the cache to always bring fresh results
			if(params.length!=0 && params[0]) {
				Cacher.disableCache();
			}
			pckgName = SearchCacher.getLastPckgName(); //Last Package Name
			pckgInfo = new StringBuffer(getString(R.string.pckg));
			pckgInfo.append(": \n");
			pckgInfo.append(pckgName);
			pckgInfo.append("\n\n");
			
			if(pckgName!=null) {
				
				pckgVersion = pts.getLatestVersion(pckgName).trim();
				pckgInfo.append(getString(R.string.latest_version));
				pckgInfo.append(":\n  ");
				pckgInfo.append(pckgVersion);
				pckgInfo.append("\n\n");
				publishProgress(2);
				SearchCacher.setLastPckgVersion(pckgVersion);
				String maintEmail =  pts.getMaintainerEmail(pckgName).trim();

				pckgInfo.append(getString(R.string.maintainer));
				pckgInfo.append(":\n  ");
				//set Maintainer Info								
				pckgInfo.append(pts.getMaintainerName(pckgName));pckgInfo.append("\n\n");
				pckgInfo.append(getString(R.string.packages_overview));pckgInfo.append(":\n  ");
				pckgInfo.append("http://qa.debian.org/developer.php?login=");
				pckgInfo.append(maintEmail);pckgInfo.append("\n\n");pckgInfo.append(getString(R.string.mail));
				pckgInfo.append(":\n  ");pckgInfo.append(maintEmail);
				
				pckgInfo.append("\n\n");
				publishProgress(3);
				pckgBugCount = pts.getBugCounts(pckgName).trim();
				pckgInfo.append(getString(R.string.bug_count));
				pckgInfo.append(":\n  ");
				pckgInfo.append(pckgBugCount);
				pckgInfo.append("\n\n");
				publishProgress(4);
				if(!pckgVersion.equals("") && !pckgBugCount.equals("") ) {
					pckgInfo.append(getString(R.string.uploaders));
					pckgInfo.append(":\n  ");
					//Set Uploader Names
					pckgInfo.append(Arrays.toString(pts.getUploaderNames(pckgName)).trim());
					pckgInfo.append("\n\n");
					publishProgress(5);
					pckgInfo.append(getString(R.string.binary_names));
					pckgInfo.append(":\n  ");
					//Set Binary Names
					pckgInfo.append(Arrays.toString(pts.getBinaryNames(pckgName)).trim());
					publishProgress(6);
					setBugData(pckgName);
				}
			}
			
			if(params.length!=0 && params[0]) {
				Cacher.enableCache();
			}
			return null;
		}  
		@SuppressLint("NewApi")
		protected void onPostExecute (Void result) {
			progressDialog.dismiss();
			if(pckgName!=null) {
				ptsInput.setText(pckgName);
				 
				if(!pckgVersion.equals("") && !pckgBugCount.equals("") ) {
					ptsPckgInfo.setText(pckgInfo.toString());
					ptsPckgInfo.setMovementMethod(LinkMovementMethod.getInstance());
		    	} else {
		    		ptsPckgInfo.setText(getString(R.string.no_info_found_for) + " " + pckgName);
		    		bugListParentItems = new ArrayList<String>();
		    		bugListChildItems = new ArrayList<Object>();
		    	}
				setupBugsList();
			}
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
	    		getSherlockActivity().invalidateOptionsMenu();
			}
	    }
		@Override
	    public void onProgressUpdate(Integer... args){
            progressDialog.setMessage(progressMessage + " " + args[0] + "/" + pckgInfoCount + " info retrieved!");
        }
    }
}