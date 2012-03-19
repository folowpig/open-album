package org.openalbum.android;

import android.app.Activity;
import android.os.Bundle;
//import android.view.View;
//import android.view.View.OnKeyListener;
//import android.widget.EditText;
import android.widget.TextView;

public class OpenAlbumActivity extends Activity {
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        RestClient client = new RestClient("http://localhost/openA-Srv/public_html/");
 //       client.AddParam("service", "photos");
        client.AddHeader("TestHeader", "2");

        try {
            client.Execute(RequestMethod.POST);
        } catch (Exception e) {
            e.printStackTrace();
        }

        String response = client.getResponse();
//       EditText edittext = (EditText) findViewById(R.id.edittext);
       TextView TextResponse = new TextView (this);
       TextResponse.setText(response);
    }
}