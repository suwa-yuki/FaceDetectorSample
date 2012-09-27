package jp.classmethod.android.sample.facedetector;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import android.app.Activity;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.FontMetrics;
import android.graphics.PointF;
import android.media.FaceDetector;
import android.media.FaceDetector.Face;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.MediaStore.Images.Media;
import android.util.FloatMath;
import android.widget.ImageView;

public class FaceDetectorActivity extends Activity {

	private ImageView mImageView;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mImageView = new ImageView(this);
		setContentView(mImageView);
		// ローカルデータから写真ファイルを取得
		Bitmap original = getLocalPicture();
		// 顔認証する処理は以下のメソッドに記述する
		Bitmap bitmap = mark(original);
		mImageView.setImageBitmap(bitmap);
	}

	private Bitmap getLocalPicture() {
		// ContentProvider から最新の画像ファイルを取得する
		Uri uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
		String sortOrder = Media.DATE_TAKEN + " DESC";
		Cursor c = getContentResolver().query(uri, null, null, null, sortOrder);
		c.moveToFirst();
		// ファイル名を取得
		int index = c.getColumnIndex(MediaStore.MediaColumns.DATA);
		String path = c.getString(index);
		// Bitmap を取得
		InputStream in = null;
		Bitmap bitmap = null;
		try {
			in = new FileInputStream(path);
			bitmap = BitmapFactory.decodeStream(in);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return null;
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
					e.printStackTrace();
					return null;
				}
			}
		}
		return bitmap;
	}

	private Bitmap mark(Bitmap original) {
		// 目にマークをつける
		// Bitmap のコピー (16bitにする)
		Bitmap bitmap = original.copy(Bitmap.Config.RGB_565, true);
		Canvas canvas = new Canvas(bitmap);
	
		// テキスト色の設定
		Paint paint = new Paint();
		paint.setAntiAlias(true);
		paint.setDither(true);
		paint.setColor(Color.MAGENTA);
		paint.setStyle(Paint.Style.FILL_AND_STROKE);
		paint.setTextSize(100);
	
		// 顔認証
		final int MAX_FACES = 4;
		Face[] faces = new Face[MAX_FACES];
		FaceDetector detector = new FaceDetector(bitmap.getWidth(),
				bitmap.getHeight(), MAX_FACES);
		int num = detector.findFaces(bitmap, faces);
	
		if (num > 0) {
			// マーク
			String mark = "♥";
			// テキストの width, height を取得
			FontMetrics fontMetrics = paint.getFontMetrics();
			int textWidth = (int) FloatMath.ceil(paint.measureText(mark));
			int textHeight = (int) FloatMath
					.ceil(Math.abs(fontMetrics.ascent)
							+ Math.abs(fontMetrics.descent)
							+ Math.abs(fontMetrics.leading));
			for (Face face : faces) {
				if (face == null) {
					continue;
				}
				PointF point = new PointF();
				face.getMidPoint(point);
				float distH = face.eyesDistance() / 2;
				// 左目
				float lx = point.x - distH - (textWidth / 2);
				float ly = point.y + (textHeight / 2);
				canvas.drawText(mark, lx, ly, paint);
				// 右目
				float rx = point.x + distH - (textWidth / 2);
				float ry = point.y + (textHeight / 2);
				canvas.drawText(mark, rx, ry, paint);
			}
		}
		original.recycle();
		return bitmap;
	}
	
	private Bitmap mosaic(Bitmap original) {
		// モザイクをかける
		// Bitmap のコピー (16bitにする)
		Bitmap bitmap = original.copy(Bitmap.Config.RGB_565, true);
		Canvas canvas = new Canvas(bitmap);
	
		// Paintの設定
		Paint paint = new Paint();
		paint.setAntiAlias(true);
		paint.setDither(true);
	
		// 顔認証
		final int MAX_FACES = 4;
		Face[] faces = new Face[MAX_FACES];
		FaceDetector detector = new FaceDetector(bitmap.getWidth(),
				bitmap.getHeight(), MAX_FACES);
		int num = detector.findFaces(bitmap, faces);
	
		if (num > 0) {
			for (Face face : faces) {
				if (face == null) {
					continue;
				}
				PointF point = new PointF();
				face.getMidPoint(point);
				
				// 幅・高さ
				int width = (int) face.eyesDistance() * 3;
				int height = width;
	
				float left = point.x - width / 2;
				float top = point.y - height / 2;
				
				if (left <= 0) {
					left = 0;
				}
				if (top <= 0) {
					top = 0;
				}
	
				// 画像を顔のサイズに切り出す
				Bitmap dist = Bitmap.createBitmap(bitmap, (int) left,
						(int) top, width, height);
				// モザイクをかける
				int w = dist.getWidth();
				int h = dist.getHeight();
				int dot = 20;
				for (int i = 0; i < w / dot; i++) {
					for (int j = 0; j < h / dot; j++) {
						int rr = 0;
						int gg = 0;
						int bb = 0;
						for (int k = 0; k < dot; k++) {
							for (int l = 0; l < dot; l++) {
								int dotColor = dist.getPixel(i * dot + k, j
										* dot + l);
								rr += Color.red(dotColor);
								gg += Color.green(dotColor);
								bb += Color.blue(dotColor);
							}
						}
						rr = rr / (dot * dot);
						gg = gg / (dot * dot);
						bb = bb / (dot * dot);
						for (int k = 0; k < dot; k++) {
							for (int l = 0; l < dot; l++) {
								dist.setPixel(i * dot + k, j * dot + l,
										Color.rgb(rr, gg, bb));
							}
						}
					}
				}
	
				// 描画する
				canvas.drawBitmap(dist, left, top, paint);
	
				// おわり
				dist.recycle();
			}
		}
		original.recycle();
		return bitmap;
	}
}
