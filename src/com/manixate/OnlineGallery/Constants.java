package com.manixate.OnlineGallery;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.text.InputType;
import android.util.Patterns;
import android.widget.EditText;
import android.widget.Toast;
import com.manixate.OnlineGallery.Utils.NetworkManager;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by manixate on 6/19/14.
 */
public class Constants {
    public static final String DefaultBaseURL = "http://localhost:5000/";
    public static final String LoginURLString = "login";
    public static final String SignupURLString = "signup";
    public static final String PhotosURLString = "photos";

    public static final String BASE_URL_PREF = "BASE_URL";

    public static final String COOKIE_PREF = "COOKIE_PREF";
    public static final String COOKIE_LIST = "COOKIE_LIST";

    public static final String COOKIE_NAME = "COOKIE_NAME";
    public static final String COOKIE_VALUE = "COOKIE_VALUE";
    public static final String COOKIE_DOMAIN = "COOKIE_DOMAIN";
    public static final String COOKIE_PATH = "COOKIE_PATH";

    public static final String IS_AUTHENTICATED = "IS_AUTHENTICATED";

    public static void getServerAddress(final Activity activity) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle("Title");

        final EditText input = new EditText(activity);
        input.setInputType(InputType.TYPE_TEXT_VARIATION_URI);
        String baseURLString = "";
        try {
            baseURLString = String.valueOf(NetworkManager.getInstance().getBaseURL(activity));
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        input.setText(baseURLString);
        builder.setView(input);

        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String urlString = input.getText().toString().trim();

                try {
                    if (!Patterns.WEB_URL.matcher(urlString).matches())
                        throw new MalformedURLException("Invalid URL");

                    URL url = new URL(urlString);

                    NetworkManager.getInstance().setBaseURL(activity, url);
                } catch (MalformedURLException e) {
                    e.printStackTrace();

                    Toast.makeText(activity, "Please enter valid URL", Toast.LENGTH_LONG).show();
                }
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }
}
