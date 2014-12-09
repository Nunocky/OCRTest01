package org.nunocky.ocrtest01;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.googlecode.tesseract.android.TessBaseAPI;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class MainActivity extends ActionBarActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    private static final String EXSTORAGE_PATH = String.format("%s/%s", Environment.getExternalStorageDirectory().toString(), "OCRTest01");
    private static final String TESSDATA_PATH = String.format("%s/%s", EXSTORAGE_PATH, "tessdata");
    private static final String TRAIN_LANG = "eng";
    private static final String TRAINEDDATA = String.format("%s.traineddata", TRAIN_LANG);
    private static final String TARGET_IMAGE = "sample.jpg";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        try {
            prepareTrainedFileIfNotExist();
        } catch (Exception e) {
            Log.e(TAG, e.toString());

            new AlertDialog.Builder(this)
                    .setTitle("Error")
                    .setMessage("Asset setup failed.")
                    .setPositiveButton("Close", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                        }})
                    .show();
        }
    }

    /**
     * traineddataを assetsから外部ストレージにコピーする。TessBaseAPIクラスがアセットを直接扱えないため。
     * @throws Exception アセットから外部ストレージへのコピーに失敗
     */
    private void prepareTrainedFileIfNotExist() throws Exception {

        // MEMO : Manifestの android.permission.WRITE_EXTERNAL_STORAGEを忘れずに

        String paths[] = {EXSTORAGE_PATH, EXSTORAGE_PATH + "/tessdata"};
        for (String path : paths) {
            File dir = new File(path);
            if (!dir.exists()) {
                if (!dir.mkdirs()) {
                    throw new Exception("ディレクトリ生成に失敗");
                }
            }
        }

        String traineddata_path = String.format("%s/%s", TESSDATA_PATH, TRAINEDDATA);

        if ( (new File(traineddata_path).exists()))
            return;

        try {
            InputStream in = getAssets().open(TRAINEDDATA);
            OutputStream out = new FileOutputStream(traineddata_path);

            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            in.close();
            out.close();
        } catch (IOException e) {
            Log.e(TAG, e.toString());
            throw new Exception("アセットのコピーに失敗");
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();

        try {
            // OCRの実験本体
            // * sample.jpgをロード (ARGB_8888に変換する)
            // * OCRを行う TessBaseAPIを初期化
            //  * traineddataを設定
            //  * 画像を指定
            //  * 文字を検出する範囲を指定

            AssetManager assetManager = getResources().getAssets();

            InputStream is = assetManager.open(TARGET_IMAGE);
            Bitmap bitmap = BitmapFactory.decodeStream(is);
            bitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);

            TessBaseAPI baseApi = new TessBaseAPI();
            baseApi.init(EXSTORAGE_PATH + "/", "eng");
            baseApi.setImage(bitmap);
            baseApi.setRectangle(56, 81, 230, 22);
            String recognizedText = baseApi.getUTF8Text();
            baseApi.end();

            Log.d(TAG, "Recognized: " + recognizedText);
        } catch (IOException e) {
		    /* 例外処理 */
            Log.d(TAG, e.toString());
        }
    }
}
