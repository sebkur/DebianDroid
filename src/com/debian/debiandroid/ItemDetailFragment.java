package com.debian.debiandroid;

import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.debian.debiandroid.content.ContentMenu;

/**
 * A fragment representing a single Item detail screen.
 * This fragment is either contained in a {@link ItemListActivity}
 * in two-pane mode (on tablets) or a {@link ItemDetailActivity}
 * on handsets.
 */
public class ItemDetailFragment extends SherlockFragment {
    /**
     * The fragment argument representing the item ID that this fragment
     * represents.
     */
    public static final String ARG_ITEM_ID = "item_id";
    public static String currentFragmentID = "";
    public static final int SETTINGS_ID = Menu.FIRST+1;
    public static boolean isInListDisplayFrag = false;

    public ContentMenu.MenuItem mItem;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public ItemDetailFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        
        if (getArguments().containsKey(ARG_ITEM_ID)) {
            // Load the content specified by the fragment
            // arguments. In a real-world scenario, use a Loader
            // to load content from a content provider.
            mItem = ContentMenu.ITEM_MAP.get(getArguments().getString(ARG_ITEM_ID));
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
    	View rootView = inflater.inflate(R.layout.fragment_item_detail, container, false);
        return rootView;
    }
    
    /** Returns the appropriate ItemDetailFragment implementation based on the given id
     * @param id a string containing the ContentMenu.Item describing the fragment to be returned
     * @return 
     */
    public static ItemDetailFragment getDetailFragment(String id){
    	currentFragmentID = id;
    	Bundle arguments = new Bundle();
        arguments.putString(ItemDetailFragment.ARG_ITEM_ID, id);
        ItemDetailFragment fragment;
    	if(id.equalsIgnoreCase(ContentMenu.BTS))
    		fragment = new BTSFragment();
    	else if(id.equalsIgnoreCase(ContentMenu.PTS))
    		fragment = new PTSFragment();
    	else if(id.equalsIgnoreCase(ContentMenu.UDD))
    		fragment = new UDDFragment();
    	else if(id.equalsIgnoreCase(ContentMenu.CIF))
    		fragment = new CIFFragment();
    	else if(id.equalsIgnoreCase(ContentMenu.SUBS))
    		fragment = new SUBSFragment();
    	else if(id.equalsIgnoreCase(ContentMenu.LINKS))
    		fragment = new LinksFragment();
    	else
    		fragment = new ItemDetailFragment();
    	fragment.setArguments(arguments);
    	fragment.setHasOptionsMenu(true);
    	return fragment;
    }

    public static String getNextFragmentId(){
    	int position = getPositionOfItem(currentFragmentID);
    	if(position==-1) {
    		return ContentMenu.PTS;
    	}
    	if(position++!=-1 && position<ContentMenu.ITEMS.size())
    		return ContentMenu.ITEMS.get(position).id;
    	return currentFragmentID;
    }
    
    public static String getPreviousFragmentId(){
    	int position = getPositionOfItem(currentFragmentID);
    	// return to ItemListActivity and don't show fragments anymore
    	if(position==0 || position==-1) {
    		return null;
    	}
    	if(position--!=-1 && position>=0)
    		return ContentMenu.ITEMS.get(position).id;
    	return currentFragmentID;
    }
    
    private static int getPositionOfItem(String id){
    	if(id.equals("")) {
    		return -1;
    	}
    	List<ContentMenu.MenuItem> items = ContentMenu.ITEMS;
    	for(int i=0;i<items.size(); i++) {
    		if(items.get(i).id.equalsIgnoreCase(id)) {
    			return i;
    		}
    	} 
    	return -1;
    }
    
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    	getSettingsMenuItem(menu);
    }
    
    public static void getSettingsMenuItem(Menu menu) {
		menu.add(0, SETTINGS_ID, Menu.CATEGORY_CONTAINER, "Settings")
				.setIcon(R.drawable.settings)
				.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
	}
    
    public void hideSoftKeyboard(EditText input){
        if(getActivity().getCurrentFocus()!=null && getActivity().getCurrentFocus() instanceof EditText){
            InputMethodManager imm = (InputMethodManager)getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(input.getWindowToken(), 0);
        }
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	 switch(item.getItemId()){
	    	 case SETTINGS_ID:
	    		 startActivity(new Intent(this.getSherlockActivity(), SettingsActivity.class));
	        	return true;
	        default:
	        	return super.onOptionsItemSelected(item);
        }
    }
    
    public static void forwardToMailApp(Context context, String recipient, String subject, String body) {
    	String uri = new StringBuilder("mailto:" + Uri.encode(recipient))
		.append("?subject=" + subject)
		.append("&body=" +  body)
		.toString();

		Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.parse(uri));
		
		/* Send it off to the Activity-Chooser */
		context.startActivity(Intent.createChooser(emailIntent, "Send mail..."));
    }
}
