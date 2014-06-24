package com.manixate.OnlineGallery.Utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.http.AndroidHttpClient;
import android.widget.Toast;
import com.manixate.OnlineGallery.AuthenticationActivity;
import com.manixate.OnlineGallery.Constants;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;

/**
 * Created by manixate on 6/19/14.
 */
public class NetworkManager {
    private static final NetworkManager sharedNetworkManager = new NetworkManager();

    private final AndroidHttpClient httpClient;
    private final BasicCookieStore cookieStore;
    private final HttpContext httpContext;

    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSZ");

    private NetworkManager() {
        cookieStore = new BasicCookieStore();
        httpContext = new BasicHttpContext();
        httpContext.setAttribute(ClientContext.COOKIE_STORE, cookieStore);

        httpClient = AndroidHttpClient.newInstance("Android");
    }

    public static NetworkManager getInstance() {
        return sharedNetworkManager;
    }

    public AndroidHttpClient getHttpClient() {
        return httpClient;
    }

    public BasicCookieStore getCookieStore() {
        return cookieStore;
    }

    public HttpContext getHttpContext() {
        return httpContext;
    }

    public boolean isAuthenticated(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(Constants.COOKIE_PREF, Context.MODE_PRIVATE);
        return sharedPreferences.getBoolean(Constants.IS_AUTHENTICATED, false);
    }

    public static void unauthenticatedAccess(final Activity activity){
        NetworkManager.getInstance().clearCredentials(activity);

        Intent intent = new Intent(activity, AuthenticationActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        activity.startActivity(intent);

        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(activity, "Please login again.", Toast.LENGTH_LONG).show();
            }
        });

        activity.finish();
    }

    public boolean saveCredentials(Context context) throws JSONException {
        List<Cookie> cookieList = cookieStore.getCookies();
        JSONArray cookieArray = new JSONArray();
        for (Cookie cookie : cookieList) {
            JSONObject jsonCookie = new JSONObject();
            jsonCookie.put(Constants.COOKIE_NAME, cookie.getName());
            jsonCookie.put(Constants.COOKIE_VALUE, cookie.getValue());
            jsonCookie.put(Constants.COOKIE_DOMAIN, cookie.getDomain());
            jsonCookie.put(Constants.COOKIE_PATH, cookie.getPath());

            cookieArray.put(jsonCookie);
        }

        String cookieListString = cookieArray.toString();

        SharedPreferences sharedPreferences = context.getSharedPreferences(Constants.COOKIE_PREF, Context.MODE_PRIVATE);
        return sharedPreferences.edit().
                putString(Constants.COOKIE_LIST, cookieListString).
                putBoolean(Constants.IS_AUTHENTICATED, true).
                commit();
    }

    public void loadCredentials(Context context) throws JSONException {
        SharedPreferences sharedPreferences = context.getSharedPreferences(Constants.COOKIE_PREF, Context.MODE_PRIVATE);
        String cookieListString = sharedPreferences.getString(Constants.COOKIE_LIST, null);

        if (cookieListString == null)
            return;

        JSONArray cookieArray = new JSONArray(cookieListString);
        for (int i = 0; i < cookieArray.length(); i++) {
            JSONObject jsonCookie = (JSONObject) cookieArray.get(i);
            String name = jsonCookie.getString(Constants.COOKIE_NAME);
            String value = jsonCookie.getString(Constants.COOKIE_VALUE);
            String domain = jsonCookie.getString(Constants.COOKIE_DOMAIN);
            String path = jsonCookie.getString(Constants.COOKIE_PATH);

            BasicClientCookie cookie = new BasicClientCookie(name, value);
            cookie.setDomain(domain);
            cookie.setPath(path);

            cookieStore.addCookie(cookie);
        }
    }

    public boolean clearCredentials(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(Constants.COOKIE_PREF, Context.MODE_PRIVATE);
        return sharedPreferences.edit().remove(Constants.COOKIE_LIST).remove(Constants.IS_AUTHENTICATED).commit();
    }
}
