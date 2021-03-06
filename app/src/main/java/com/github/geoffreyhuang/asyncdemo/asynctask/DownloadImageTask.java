package com.github.geoffreyhuang.asyncdemo.asynctask;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.widget.ImageView;

import com.github.geoffreyhuang.asyncdemo.R;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by Geoffrey Huang on 2016/9/19.
 */
public class DownloadImageTask extends AsyncTask<URL, Integer, Bitmap> {

    private final WeakReference<Context> mContextRef;
    private final WeakReference<ImageView> mImageViewWeakRef;
    private ProgressDialog mProgressDialog;

    public DownloadImageTask(Context ctx, ImageView imageView) {
        this.mContextRef = new WeakReference<>(ctx);
        this.mImageViewWeakRef = new WeakReference<>(imageView);
    }

    @Override
    protected void onPreExecute() {
        if (mContextRef.get() != null) {
            mProgressDialog = new ProgressDialog(mContextRef.get());
            mProgressDialog.setTitle("downloading...");
            mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            mProgressDialog.setIndeterminate(false);
            mProgressDialog.setProgress(0);
            mProgressDialog.setMax(100);
            mProgressDialog.setCancelable(true);
            mProgressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    cancel(false);
                }
            });
            mProgressDialog.show();
        }
    }

    @Override
    protected Bitmap doInBackground(URL... params) {
        URL url = params[0];
        Bitmap bitmap = null;
        final int totalBytes;
        publishProgress(0);
        try {
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            totalBytes = conn.getContentLength();
            InputStream inputStream = conn.getInputStream();
            BufferedInputStream bfi = new BufferedInputStream(inputStream) {
                int progress = 0;
                int downloadedBytes = 0;

                @Override
                public synchronized int read(byte[] buffer, int byteOffset, int byteCount)
                        throws IOException {
                    int readBytes = super.read(buffer, byteOffset, byteCount);
                    if (isCancelled()) {
                        return -1;
                    }
                    downloadedBytes += readBytes;
                    int percent = (downloadedBytes * 100) / totalBytes;
                    if (percent > progress) {
                        publishProgress(percent);
                        progress = percent;
                    }
                    return readBytes;
                }
            };
            if (!isCancelled()) {
                bitmap = BitmapFactory.decodeStream(bfi);
            }
            inputStream.close();
            bfi.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return bitmap;
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        mProgressDialog.setProgress(values[0]);
    }

    @Override
    protected void onPostExecute(Bitmap bitmap) {
        if (mProgressDialog != null) {
            mProgressDialog.dismiss();
        }
        ImageView imageView = mImageViewWeakRef.get();
        if (imageView != null) {
            imageView.setImageBitmap(bitmap);
        }
    }

    @Override
    protected void onCancelled() {
        if (mContextRef != null && mContextRef.get() != null
                && mImageViewWeakRef != null && mImageViewWeakRef.get() != null) {
            Bitmap bitmap = BitmapFactory.decodeResource(mContextRef.get().getResources(),
                    R.drawable.picture_default);
            mImageViewWeakRef.get().setImageBitmap(bitmap);
        }
        mProgressDialog.dismiss();
    }
}
