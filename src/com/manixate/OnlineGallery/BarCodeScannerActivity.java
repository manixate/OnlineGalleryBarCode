package com.manixate.OnlineGallery;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.net.http.AndroidHttpClient;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Pair;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;
import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import com.manixate.OnlineGallery.Utils.NetworkManager;
import com.manixate.OnlineGallery.ZBar.CameraPreview;
import net.sourceforge.zbar.*;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

class BarCodeScannerActivity extends Activity
{
    @InjectView(R.id.cameraPreview)
    FrameLayout mCameraPreview;
    @InjectView(R.id.scanText)
    TextView mScanText;
    @InjectView(R.id.scanButton)
    Button mScanButton;

    private Camera mCamera;
    private Handler autoFocusHandler;

    private ImageScanner scanner;

    private boolean previewing = true;
    private boolean captureNextFrame = false;

    static {
        System.loadLibrary("iconv");
    } 

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.camera);

        ButterKnife.inject(this);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        autoFocusHandler = new Handler();
        mCamera = getCameraInstance();

        /* Instance barcode scanner */
        scanner = new ImageScanner();
        scanner.setConfig(0, Config.X_DENSITY, 3);
        scanner.setConfig(0, Config.Y_DENSITY, 3);

        CameraPreview mPreview = new CameraPreview(this, mCamera, previewCb, null);
        mCameraPreview.addView(mPreview);
    }

    @OnClick(R.id.scanButton)
    public void scanBtnPressed(Button button) {
        if (!captureNextFrame) {
            mScanText.setText("Scanning ...");
            captureNextFrame = true;
        }
    }

    public void onPause() {
        super.onPause();
        releaseCamera();
    }

    private static Camera getCameraInstance() {
        try {
            return Camera.open();
        } catch (Exception e){
            return null;
        }
    }

    private void releaseCamera() {
        if (mCamera != null) {
            previewing = false;
            mCamera.setPreviewCallback(null);
            mCamera.release();
            mCamera = null;
            captureNextFrame = false;
        }
    }

    interface UploadProgress {
        void onUploadStarted();
        void onUploadCompleted();
    }

    private final UploadProgress uploadProgressCb = new UploadProgress() {
        @Override
        public void onUploadStarted() {
            mScanButton.setEnabled(false);
        }

        @Override
        public void onUploadCompleted() {
            mScanButton.setEnabled(true);
        }
    };

    private final Camera.PreviewCallback previewCb = new Camera.PreviewCallback() {
        public void onPreviewFrame(byte[] data, Camera camera) {
            Camera.Parameters parameters = camera.getParameters();
            Camera.Size size = parameters.getPreviewSize();

            Image barcode = new Image(size.width, size.height, "Y800");
            barcode.setData(data);

            int result = scanner.scanImage(barcode);

            String barCode = null;
            if (result != 0) {
                SymbolSet syms = scanner.getResults();
                for (Symbol sym : syms) {
                    barCode = sym.getData();

                    if (captureNextFrame) {
                        mScanText.setText("Barcode result " + barCode);

                        YuvImage yuvImage = new YuvImage(data, parameters.getPreviewFormat(), size.width, size.height, null);
                        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                        yuvImage.compressToJpeg(new Rect(0, 0, size.width, size.height), 100, byteArrayOutputStream);
                        byte[] imageData = byteArrayOutputStream.toByteArray();

                        new UploadPhotoTask(BarCodeScannerActivity.this, uploadProgressCb).execute(new Pair<String, byte[]>(barCode, imageData));

                        try {
                            byteArrayOutputStream.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        captureNextFrame = false;
                        mScanText.setText("");
                    }
                }
            } else {
                mScanText.setText("");
            }
        }
    };

    private final Runnable doAutoFocus = new Runnable() {
        public void run() {
            if (previewing)
                mCamera.autoFocus(autoFocusCB);
        }
    };

    // Mimic continuous auto-focusing
    private final Camera.AutoFocusCallback autoFocusCB = new Camera.AutoFocusCallback() {
        public void onAutoFocus(boolean success, Camera camera) {
            autoFocusHandler.postDelayed(doAutoFocus, 1000);
        }
    };

    class UploadPhotoTask extends AsyncTask<Pair<String, byte[]>, Void, String> {
        final Context context;
        final UploadProgress uploadProgress;

        public UploadPhotoTask(Context context, UploadProgress uploadProgress) {
            this.context = context;
            this.uploadProgress = uploadProgress;
        }

        @Override
        protected void onPreExecute() {
            uploadProgress.onUploadStarted();
        }

        @Override
        protected String doInBackground(Pair<String, byte[]>... imageDatas) {
            HttpPost httpPost;
            try {
                URL url = new URL(NetworkManager.getInstance().getBaseURL(context), Constants.PhotosURLString);
                httpPost = new HttpPost(new URI(url.toString()));
            } catch (MalformedURLException e) {
                e.printStackTrace();

                return "{success: false, message: " + e.getMessage() + " }";
            } catch (URISyntaxException e) {
                e.printStackTrace();

                return "{success: false, message: " + e.getMessage() + " }";
            }

            AndroidHttpClient httpClient = NetworkManager.getInstance().getHttpClient();

            Pair<String, byte[]> imageData = imageDatas[0];
            byte[] jpegImageData = imageData.second;
            if (jpegImageData == null)
                return "{success: false, message: Empty image }";

            MultipartEntityBuilder multipartEntityBuilder = MultipartEntityBuilder.create();
            multipartEntityBuilder.addBinaryBody("file", jpegImageData, ContentType.create("image/jpeg"), imageData.first);

            MultipartEntity multipartEntity = multipartEntityBuilder.build();
            httpPost.setEntity(multipartEntity);

            try {
                HttpResponse httpResponse = httpClient.execute(httpPost, NetworkManager.getInstance().getHttpContext());

                if (httpResponse.getStatusLine().getStatusCode() == 401) {
                    NetworkManager.unauthenticatedAccess(BarCodeScannerActivity.this);
                }
                return EntityUtils.toString(httpResponse.getEntity());

            } catch (IOException e) {
                e.printStackTrace();

                return "{success: false, message: " + e.getMessage() + " }";
            }
        }

        @Override
        protected void onPostExecute(String s) {
            uploadProgress.onUploadCompleted();

            try {
                JSONObject jsonObject = new JSONObject(s);

                Toast.makeText(BarCodeScannerActivity.this, jsonObject.getString("message"), Toast.LENGTH_SHORT).show();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }
}
