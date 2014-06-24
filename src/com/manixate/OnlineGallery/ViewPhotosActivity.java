package com.manixate.OnlineGallery;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.http.AndroidHttpClient;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.LruCache;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import butterknife.ButterKnife;
import butterknife.InjectView;
import com.manixate.OnlineGallery.Utils.NetworkManager;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.util.ArrayList;

/**
 * Created by manixate on 6/19/14.
 */
class ViewPhotosActivity extends Activity {
    @InjectView(R.id.gridView)
    GridView mGridView;

    private LruCache<URL, Bitmap> mMemoryCache;
    private PhotosAdapter mPhotosAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.view_photos);

        ButterKnife.inject(this);

        final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
        final int cacheSize = maxMemory / 4;

        mMemoryCache = new LruCache<URL, Bitmap>(cacheSize) {
            @Override
            protected int sizeOf(URL key, Bitmap bitmap) {
                return bitmap.getByteCount() / 1024;
            }
        };

        mPhotosAdapter = new PhotosAdapter(this);
        mGridView.setAdapter(mPhotosAdapter);

        new PhotosTask(this).execute();
    }

    void addBitmapToMemoryCache(URL key, Bitmap bitmap) {
        if (getBitmapFromMemCache(key) == null) {
            mMemoryCache.put(key, bitmap);
        }
    }

    Bitmap getBitmapFromMemCache(URL key) {
        return mMemoryCache.get(key);
    }

    private class PhotosTask extends AsyncTask<Void, Void, ArrayList<URL>> {
        final Context context;

        private PhotosTask(Context context) {
            this.context = context;
        }

        @Override
        protected ArrayList<URL> doInBackground(Void... voids) {
            HttpGet getRequest = new HttpGet(Constants.PhotosURLString);

            AndroidHttpClient httpClient = NetworkManager.getInstance().getHttpClient();

            try {
                HttpResponse httpResponse = httpClient.execute(getRequest, NetworkManager.getInstance().getHttpContext());

                String response = EntityUtils.toString(httpResponse.getEntity());
                if (httpResponse.getStatusLine().getStatusCode() == 403) {
                    NetworkManager.unauthenticatedAccess(ViewPhotosActivity.this);
                    return null;
                }

                JSONArray photoArray = new JSONObject(response).getJSONArray("message");

                ArrayList<URL> photos = new ArrayList<URL>();
                for (int i = 0; i < photoArray.length(); i++) {
                    String photoURLString = photoArray.getString(i);

                    URL photoURL = new URL(photoURLString);
                    photos.add(photoURL);
                }

                return photos;
            } catch (IOException e) {
                e.printStackTrace();

                return null;
            } catch (JSONException e) {
                e.printStackTrace();

                return null;
            }
        }

        @Override
        protected void onPostExecute(ArrayList<URL> photosList) {
            if (photosList == null)
                return;

            mPhotosAdapter.photosList = photosList;
            mPhotosAdapter.notifyDataSetChanged();
        }
    }

    private class PhotosAdapter extends BaseAdapter {

        final Context mContext;
        ArrayList<URL> photosList = new ArrayList<URL>();
        public PhotosAdapter(Context context) {
            this.mContext = context;
        }

        @Override
        public int getCount() {
            return photosList.size();
        }

        @Override
        public Object getItem(int i) {
            return photosList.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View convertView, ViewGroup viewGroup) {
            ImageView imageView;
            if (convertView == null) {
                imageView = new ImageView(mContext);
                int imageSize = (int) getResources().getDimension(R.dimen.IMAGE_SIZE);
                imageView.setLayoutParams(new GridView.LayoutParams(imageSize, imageSize));
                imageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                imageView.setPadding(10, 10, 10, 10);
            } else {
                imageView = (ImageView) convertView;
            }

            URL imageURL = (URL)getItem(i);
            loadBitmap(imageView, imageURL);

            return imageView;
        }
    }

    void loadBitmap(ImageView imageView, URL imageURL) {
        final Bitmap bitmap = getBitmapFromMemCache(imageURL);
        if (bitmap != null) {
            WeakReference<LoadImageTask> imageTaskWeakReference = (WeakReference<LoadImageTask>) imageView.getTag();
            if (imageTaskWeakReference == null)
                return;

            LoadImageTask loadImageTask = imageTaskWeakReference.get();
            if (loadImageTask != null && !loadImageTask.imageURL.equals(imageURL))
                loadImageTask.cancel(true);

            imageView.setImageBitmap(bitmap);
        } else {
            imageView.setImageResource(android.R.drawable.ic_menu_gallery);
            new LoadImageTask(imageView, imageURL).execute();
        }
    }

    private class LoadImageTask extends AsyncTask<Void, Void, Bitmap> {

        final WeakReference<ImageView> imageViewWeakReference;
        final URL imageURL;

        public LoadImageTask(ImageView imageView, URL url) {
            imageViewWeakReference = new WeakReference <ImageView>(imageView);
            this.imageURL = url;
            imageView.setTag(new WeakReference<LoadImageTask>(this));
        }

        @Override
        protected Bitmap doInBackground(Void... params) {

            try {
                InputStream inputStream = imageURL.openStream();

                final BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;

                BitmapFactory.decodeStream(inputStream);

                final int imageSize = (int) getResources().getDimension(R.dimen.IMAGE_SIZE);
                options.inSampleSize = calculateInSampleSize(options, imageSize, imageSize);

                options.inJustDecodeBounds = false;
                Bitmap bitmap = BitmapFactory.decodeStream(imageURL.openStream());

                if (bitmap != null)
                    addBitmapToMemoryCache(imageURL, bitmap);

                return bitmap;
            } catch (IOException e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if (isCancelled())
                bitmap = null;

            if (imageViewWeakReference != null && bitmap != null) {
                ImageView imageView = imageViewWeakReference.get();
                if (imageView != null) {
                    WeakReference<LoadImageTask> imageTaskWeakReference = (WeakReference<LoadImageTask>) imageView.getTag();
                    if (imageTaskWeakReference.get() == this)
                        imageView.setImageBitmap(bitmap);
                }
            }
        }
    }

    private static int calculateInSampleSize(
            BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) > reqHeight
                    && (halfWidth / inSampleSize) > reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }
}