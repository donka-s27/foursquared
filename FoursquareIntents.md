Foursquare Intents

Following is a simple application which creates a few buttons that call through to the intents the Foursquare application provides. If the user does not have the Foursquare app installed, the device will most likely try to show the url in a new browser instance as a fallback.

(Replace instances of "uid" and "vid" with a user ID and venue ID)

```
/**
  * @date 2010-07-01
  * @author Mark Wyszomierski (markww@gmail.com)
  *
  */
public class ActivityMain extends Activity {

    private ViewGroup mButtons;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        mButtons = (ViewGroup)findViewById(R.id.buttons);
         
        addButton("View my profile", 
            "http://m.foursquare.com/user");
        addButton("View specific user", 
            "http://m.foursquare.com/user?uid=userid");
        addButton("View a venue", 
            "http://m.foursquare.com/venue/venueid");
        addButton("Post a shout", 
            "http://m.foursquare.com/shout");
        addButton("Start empty search intent", 
            "http://m.foursquare.com/search");
        addButton("Check in to a specific venue", 
            "http://m.foursquare.com/checkin?vid=venueid");
        addButton("See latest checkins from friends", 
	    "http://m.foursquare.com/checkins");
        addButton("Start specific search intent (prepopulate only)", 
	    "http://m.foursquare.com/search?q=" + 
	     URLEncoder.encode("pizza"));
        addButton("Start specific search intent (immediate)", 
	    "http://m.foursquare.com/search?q=" + 
	     URLEncoder.encode("pizza") + "&immediate=1");
        addButton("Show main activity, tab Friends", 
            "http://m.foursquare.com/feed");
        addButton("Show main activity, tab Places", 
            "http://m.foursquare.com/places");
    }
    
    private void addButton(final String text, final String url) {
    	Button btn = new Button(this);
    	btn.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.FILL_PARENT, 
            LinearLayout.LayoutParams.WRAP_CONTENT));
    	btn.setText(text);
    	btn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                startActivity(intent);
            }
        });
    	
        mButtons.addView(btn);
    }
}
```