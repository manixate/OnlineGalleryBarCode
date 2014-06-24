package com.manixate.OnlineGallery;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Button;
import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import com.manixate.OnlineGallery.Utils.NetworkManager;

/**
 * Created by manixate on 6/19/14.
 */
class MainActivity extends Activity {
    @InjectView(R.id.openScannerBtn)
    Button mScanBtn;
    @InjectView(R.id.viewPhotosBtn)
    Button mViewPhotosBtn;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        ButterKnife.inject(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.menu, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.logout:
                NetworkManager.getInstance().clearCredentials(this);
                NetworkManager.unauthenticatedAccess(this);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @OnClick(R.id.openScannerBtn)
    public void scanBtnPressed(Button button) {
        Intent intent = new Intent(this, BarCodeScannerActivity.class);
        startActivity(intent);
    }

    @OnClick(R.id.viewPhotosBtn)
    public void viewPhotosBtnPressed(Button button) {
        Intent intent = new Intent(this, ViewPhotosActivity.class);
        startActivity(intent);
    }
}