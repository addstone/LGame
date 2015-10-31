/**
 * Copyright 2008 - 2015 The Loon Game Engine Authors
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 * 
 * @project loon
 * @author cping
 * @email：javachenpeng@yahoo.com
 * @version 0.5
 */
package loon.android;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Hashtable;

import loon.LGame;
import loon.LSetting;
import loon.LSystem;
import loon.LazyLoading;
import loon.Platform;
import loon.android.AndroidGame.AndroidSetting;
import loon.android.AndroidGame.LMode;
import loon.canvas.LColor;
import loon.geom.RectBox;
import loon.utils.StringUtils;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;

public abstract class Loon extends Activity implements AndroidBase, Platform,
		LazyLoading {

	@SuppressLint("SetJavaScriptEnabled")
	final static class Web extends android.webkit.WebView {

		/**
		 * Web自定义脚本(name即代表JavaScript中类名,以"name."形式执行Object封装的具体对象)
		 * 比如构建一个脚本类名为App,提供一个具体类Home，其中只有一个函数go。那么执行脚本
		 * App.go时即自动调用相关的Home类中同名函数，并且在Android系统中执行。
		 * 
		 */
		public interface JavaScript {

			// 执行的对象，实际内部应该封装具体类
			public Object getObject();

			// 脚本类名
			public String getName();

		}

		/**
		 * Web加载进度监听器
		 * 
		 */
		public interface WebProcess {

			// 页面开始加载
			public void onPageStarted(String url, Bitmap favicon);

			// 页面加载完成
			public void onPageFinished(String url);

			// 资源载入
			public void onLoadResource(String url);

			// 即将重载链接前
			public void shouldOverrideUrlLoading(String url);

			// 接受请求
			public void onReceivedHttpAuthRequest(
					android.webkit.HttpAuthHandler handler, String host,
					String realm);

		}

		private Loon activity;

		private android.webkit.WebSettings webSettings;

		private String url;

		public Web(String url) {
			this(Loon.self, null, url);
		}

		public Web(String url, WebProcess webProcess) {
			this(Loon.self, webProcess, url);
		}

		public Web(Loon activity, String url) {
			this(activity, null, url);
		}

		@SuppressWarnings("deprecation")
		public Web(final Loon activity, final WebProcess webProcess,
				final String url) {

			super(activity);
			this.url = url;
			this.activity = activity;
			// 允许显示滚动条
			this.setHorizontalScrollBarEnabled(true);
			// 清空原有的缓存数据
			this.clearCache(true);
			// 隐藏当前View
			this.setVisible(false);
			// 不要背景图
			java.lang.reflect.Method drawable = null;
			try {
				drawable = this.getClass().getMethod("setBackgroundDrawable",
						android.graphics.drawable.Drawable.class);
				drawable.invoke(this, (android.graphics.drawable.Drawable) null);
			} catch (Exception ex) {
				try {
					drawable = this.getClass().getMethod("setBackground",
							android.graphics.drawable.Drawable.class);
					drawable.invoke(this,
							(android.graphics.drawable.Drawable) null);
				} catch (Exception e) {

				}
			}
			// 进行细节设置
			webSettings = getSettings();
			// 数据库访问权限开启
			webSettings.setAllowFileAccess(true);
			// 密码保存与Form信息不保存
			webSettings.setSavePassword(false);
			webSettings.setSaveFormData(false);
			if (!webSettings.getJavaScriptEnabled()) {
				// 响应JavaScript事件
				webSettings.setJavaScriptEnabled(true);
			}
			// 允许JavaScript脚本打开新的窗口
			webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
			// 允许自动加载图像资源
			webSettings.setLoadsImagesAutomatically(true);
			// 不支持网页缩放
			webSettings.setSupportZoom(false);

			// 当流程监听存在时
			if (webProcess != null) {
				setWebViewClient(new android.webkit.WebViewClient() {

					@Override
					public void onPageStarted(android.webkit.WebView view,
							String url, Bitmap favicon) {
						webProcess.onPageStarted(url, favicon);
						super.onPageStarted(view, url, favicon);
					}

					@Override
					public void onPageFinished(android.webkit.WebView view,
							String url) {
						webProcess.onPageFinished(url);
						super.onPageFinished(view, url);
					}

					@Override
					public void onLoadResource(android.webkit.WebView view,
							String url) {
						webProcess.onLoadResource(url);
						super.onLoadResource(view, url);
					}

					@Override
					public boolean shouldOverrideUrlLoading(
							android.webkit.WebView view, String url) {
						webProcess.shouldOverrideUrlLoading(url);
						return super.shouldOverrideUrlLoading(view, url);
					}

					@Override
					public void onReceivedHttpAuthRequest(
							android.webkit.WebView view,
							android.webkit.HttpAuthHandler handler,
							String host, String realm) {
						webProcess.onReceivedHttpAuthRequest(handler, host,
								realm);
						super.onReceivedHttpAuthRequest(view, handler, host,
								realm);
					}

				});
			}

			// 加载进度条
			final android.widget.ProgressBar progress = new android.widget.ProgressBar(
					activity);
			activity.addView(progress, AndroidLocation.CENTER);
			setWebChromeClient(new android.webkit.WebChromeClient() {
				@Override
				public void onProgressChanged(
						final android.webkit.WebView view, final int newProgress) {
					Loon.self.runOnUiThread(new Runnable() {
						@Override
						public void run() {
							progress.setProgress(newProgress);
							progress.setVisibility(newProgress == 100 ? View.GONE
									: View.VISIBLE);
							if (newProgress == 100) {
								activity.removeView(progress);
							}
							setVisible(newProgress == 100 ? true : false);
						}
					});

				}
			});
			if (url != null) {
				loadUrl(url);
			}
		}

		/**
		 * 像当前Web界面进行内部标记性赋值
		 * 
		 * @param name
		 * @param value
		 */
		@SuppressWarnings("unchecked")
		public void setWebParams(String name, Object value) {
			Hashtable<String, Object> params = null;
			if (getTag() == null) {
				params = new Hashtable<String, Object>();
				setTag(params);
			} else {
				params = (Hashtable<String, Object>) getTag();
			}
			params.put(name, value);
		}

		/**
		 * 获得当前Web界面的内部标记性传参
		 * 
		 * @param name
		 * @return
		 */
		@SuppressWarnings("unchecked")
		public Object getWebParams(String name) {
			if (getTag() == null) {
				setTag(new Hashtable<String, Object>());
			}
			Hashtable<String, Object> params = (Hashtable<String, Object>) getTag();
			return params.get(name);
		}

		/**
		 * 添加一组自制JavaScript脚本到当前Web界面当中
		 * 
		 * @param sprites
		 */
		public void addJavaScripts(JavaScript[] sprites) {
			for (int i = 0; i < sprites.length; i++) {
				this.addJavascriptInterface(sprites[i].getObject(),
						sprites[i].getName());
			}
		}

		/**
		 * 添加自制JavaScript脚本到当前Web界面当中
		 * 
		 * @param sprite
		 */
		public void addJavaScripts(JavaScript sprite) {
			this.addJavascriptInterface(sprite.getObject(), sprite.getName());
		}

		public void setVisible(boolean isVisible) {
			if (isVisible) {
				this.setVisibility(View.VISIBLE);
			} else {
				this.setVisibility(View.GONE);
			}
		}

		public void callScriptFunction(String function) {
			super.loadUrl("javascript:" + function);
		}

		/**
		 * 通过Intent进行跳转
		 * 
		 * @param intent
		 */
		public void loadIntent(Intent intent) {
			this.loadUrl(intent.getStringExtra(android.app.SearchManager.QUERY));
		}

		/**
		 * 通过Url进行跳转
		 */
		@Override
		public void loadUrl(String url) {
			boolean isURL = url.startsWith("http://")
					|| url.startsWith("https://") || url.startsWith("ftp://");
			if (!isURL) {
				try {
					if (activity.getAssets().open(url) != null) {
						super.loadUrl("file:///android_asset/" + url);
					} else {
						super.loadUrl("http://" + url);
					}
				} catch (IOException e) {
					super.loadUrl("http://" + url);
				}
			} else {
				super.loadUrl(url);
			}
		}

		public void loadData(String data) {
			loadData(data, "text/html", LSystem.ENCODING);
		}

		public void loadData(String data, String encoding) {
			super.loadData(data, "text/html", encoding);
		}

		public android.webkit.WebSettings getWebSettings() {
			return webSettings;
		}

		public String getURL() {
			return url;
		}

	}

	private FrameLayout frameLayout;

	private AndroidGame game;
	private AndroidGameViewGL gameView;
	protected static Loon self;
	private AndroidSetting setting;

	private LazyLoading.Data mainData;

	public static String getResourcePath(String name) throws IOException {
		if (self == null) {
			return name;
		}
		if (LSystem.base().type() == LGame.Type.ANDROID) {
			if (name.toLowerCase().startsWith("assets/")) {
				name = StringUtils.replaceIgnoreCase(name, "assets/", "");
			}
			if (name.startsWith("/") || name.startsWith("\\")) {
				name = name.substring(1, name.length());
			}
		}
		File file = new File(self.getFilesDir(), name);
		if (!file.exists()) {
			retrieveFromAssets(self, name);
		}
		return file.getAbsolutePath();
	}

	private static void retrieveFromAssets(Activity activity, String filename)
			throws IOException {
		InputStream is = activity.getAssets().open(filename);
		File outFile = new File(activity.getFilesDir(), filename);
		makedirs(outFile);
		FileOutputStream fos = new FileOutputStream(outFile);
		byte[] buffer = new byte[2048];
		int length;
		while ((length = is.read(buffer)) > 0) {
			fos.write(buffer, 0, length);
		}
		fos.flush();
		fos.close();
		is.close();
	}

	private static void makedirs(File file) throws IOException {
		checkFile(file);
		File parentFile = file.getParentFile();
		if (parentFile != null) {
			if (!parentFile.exists() && !parentFile.mkdirs()) {
				throw new IOException("Creating directories "
						+ parentFile.getPath() + " failed.");
			}
		}
	}

	private static void checkFile(File file) throws IOException {
		boolean exists = file.exists();
		if (exists && !file.isFile()) {
			throw new IOException("File " + file.getPath()
					+ " is actually not a file.");
		}
	}

	protected DisplayMetrics getSysDisplayMetrices() {
		DisplayMetrics dm = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(dm);
		return dm;
	}

	private Handler handler;

	public abstract void onMain();

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Loon.self = this;

		Context context = getApplicationContext();
		this.onMain();
		LMode mode = setting.showMode;
		if (mode == null) {
			mode = LMode.Fill;
		}

		if (setting != null && setting.fullscreen) {
			this.setFullScreen(setting.fullscreen);
		} else {
			int windowFlags = makeWindowFlags();
			getWindow().setFlags(windowFlags, windowFlags);
		}

		int width = setting.width;
		int height = setting.height;

		// 是否按比例缩放屏幕
		if (setting.useRatioScaleFactor) {
			float scale = scaleFactor();
			width *= scale;
			height *= scale;
			setting.width_zoom = width;
			setting.height_zoom = height;
			setting.updateScale();
			mode = LMode.MaxRatio;
		// 若缩放值为无法实现的数值，则默认操作
		} else if (setting.width_zoom <= 0 || setting.height_zoom <= 0) {
			updateViewSize(setting.landscape(), setting.width, setting.height,
					mode);
			width = this.maxWidth;
			height = this.maxHeight;
			setting.width_zoom = this.maxWidth;
			setting.height_zoom = this.maxHeight;
			setting.updateScale();
			mode = LMode.Fill;
		}

		this.game = createGame();
		this.gameView = new AndroidGameViewGL(context, game);
		this.handler = new Handler();

		setContentView(mode, gameView, width, height);

		setRequestedOrientation(orientation());

		createWakeLock(setting.useWakelock);
		hideStatusBar(setting.hideStatusBar);
		setImmersiveMode(setting.useImmersiveMode);
		if (setting.useImmersiveMode && AndroidGame.getSDKVersion() >= 19) {
			try {
				Class<?> vlistener = Class
						.forName("com.badlogic.gdx.backends.android.AndroidVisibilityListener");
				Object o = vlistener.newInstance();
				java.lang.reflect.Method method = vlistener.getDeclaredMethod(
						"createListener", AndroidBase.class);
				method.invoke(o, this);
			} catch (Exception e) {
			}
		}
		try {
			final int REQUIRED_CONFIG_CHANGES = android.content.pm.ActivityInfo.CONFIG_ORIENTATION
					| android.content.pm.ActivityInfo.CONFIG_KEYBOARD_HIDDEN;
			android.content.pm.ActivityInfo info = this.getPackageManager()
					.getActivityInfo(
							new android.content.ComponentName(context,
									this.getPackageName() + "."
											+ this.getLocalClassName()), 0);
			if ((info.configChanges & REQUIRED_CONFIG_CHANGES) != REQUIRED_CONFIG_CHANGES) {
				new android.app.AlertDialog.Builder(this)
						.setMessage(
								"Loon Tip : Please add the following line to the Activity manifest .\n[configChanges=\"keyboardHidden|orientation\"]")
						.show();
			}
		} catch (Exception e) {
			Log.w("Loon", "Cannot access game AndroidManifest.xml file !");
		}
	}

	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		AndroidGame.debugLog("onWindowFocusChanged(" + hasFocus + ")");
		if (setting != null) {
			setImmersiveMode(setting.useImmersiveMode);
			hideStatusBar(setting.hideStatusBar);
		}
		if (game != null && game.assets != null && game.assets._audio != null) {
			if (hasFocus) {
				game.assets.getNativeAudio().onResume();
			} else {
				game.assets.getNativeAudio().onPause();
			}
		}
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (game != null && game.input != null) {
			game.input.onKeyDown(keyCode, event);
		}
		return super.onKeyDown(keyCode, event);
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		if (game != null && game.input != null) {
			game.input.onKeyUp(keyCode, event);
		}
		return super.onKeyUp(keyCode, event);
	}

	public void setFullScreen(boolean fullScreen) {
		Window win = getWindow();
		if (AndroidGame.isAndroidVersionHigher(11)) {
			int flagHardwareAccelerated = 0x1000000;
			win.setFlags(flagHardwareAccelerated, flagHardwareAccelerated);
		}
		if (fullScreen) {
			win.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
			win.clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
			win.requestFeature(android.view.Window.FEATURE_NO_TITLE);
		} else {
			win.setFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN,
					WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
		}
	}

	@Override
	public void onBackPressed() {
		moveTaskToBack(false);
	}

	@Override
	protected void onDestroy() {
		AndroidGame.debugLog("onDestroy");
		for (File file : getCacheDir().listFiles()) {
			file.delete();
		}
		if (game != null && game.assets != null) {
			game.assets.getNativeAudio().onDestroy();
			game.onExit();
		}
		super.onDestroy();
	}

	@Override
	protected void onPause() {
		AndroidGame.debugLog("onPause");
		gameView.onPause();
		if (game != null) {
			game.onPause();
		}
		super.onPause();
	}

	@Override
	protected void onResume() {
		AndroidGame.debugLog("onResume");
		if (game != null) {
			game.onResume();
		}
		gameView.onResume();
		super.onResume();
	}

	protected int makeWindowFlags() {
		return (WindowManager.LayoutParams.FLAG_FULLSCREEN | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED);
	}

	protected boolean usePortraitOrientation() {
		return false;
	}

	protected int orientation() {
		return usePortraitOrientation() ? ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
				: ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
	}

	@SuppressWarnings("deprecation")
	protected Bitmap.Config preferredBitmapConfig() {
		ActivityManager activityManager = (ActivityManager) getApplication()
				.getSystemService(Context.ACTIVITY_SERVICE);
		int memoryClass = activityManager.getMemoryClass();
		int format = getWindowManager().getDefaultDisplay().getPixelFormat();
		return (format == PixelFormat.RGBA_4444 || memoryClass <= 16) ? Bitmap.Config.ARGB_4444
				: Bitmap.Config.ARGB_8888;
	}

	protected float scaleFactor() {
		return getResources().getDisplayMetrics().density;
	}

	protected int maxSimultaneousSounds() {
		return 8;
	}

	public AndroidGameViewGL gameView() {
		return gameView;
	}

	protected AndroidGame createGame() {
		return this.game = new AndroidGame(this, setting);
	}

	public LGame getGame() {
		return game;
	}

	protected AndroidGame initialize() {
		if (game != null) {
			game.register(mainData.onScreen());
		}
		return game;
	}

	@Override
	public void register(LSetting s, LazyLoading.Data data) {
		if (s instanceof AndroidSetting) {
			this.setting = (AndroidSetting) s;
		} else {
			AndroidSetting tmp = new AndroidSetting();
			tmp.copy(s);
			tmp.fullscreen = true;
			this.setting = tmp;
		}
		this.mainData = data;
	}

	@Override
	public Handler getHandler() {
		return this.handler;
	}

	@Override
	public Window getApplicationWindow() {
		return this.getWindow();
	}

	protected void createWakeLock(boolean use) {
		if (use) {
			getWindow()
					.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		}
	}

	protected void hideStatusBar(boolean hide) {
		if (!hide || AndroidGame.getSDKVersion() < 11) {
			return;
		}
		View rootView = getWindow().getDecorView();
		try {
			java.lang.reflect.Method m = View.class.getMethod(
					"setSystemUiVisibility", int.class);
			if (AndroidGame.getSDKVersion() <= 13)
				m.invoke(rootView, 0x0);
			m.invoke(rootView, 0x1);
		} catch (Exception e) {
		}
	}

	private void setContentView(LMode mode, AndroidGameViewGL view, int w, int h) {
		this.frameLayout = new FrameLayout(this);
		this.frameLayout .setBackgroundColor(LColor.black.getRGB());
		if (mode == LMode.Defalut) {
			// 添加游戏View，显示为指定大小，并居中
			this.addView(view, view.getWidth(), view.getHeight(),
					AndroidLocation.CENTER);
		} else if (mode == LMode.Ratio) {
			// 添加游戏View，显示为屏幕许可范围，并居中
			this.addView(view, w, h, AndroidLocation.CENTER);
		} else if (mode == LMode.MaxRatio) {
			// 添加游戏View，显示为屏幕许可的最大范围(可能比单纯的Ratio失真)，并居中
			this.addView(view, w, h, AndroidLocation.CENTER);
		} else if (mode == LMode.Max) {
			// 添加游戏View，显示为最大范围值，并居中
			this.addView(view, w, h, AndroidLocation.CENTER);
		} else if (mode == LMode.Fill) {
			// 添加游戏View，显示为全屏，并居中
			this.addView(view,
					android.view.ViewGroup.LayoutParams.MATCH_PARENT,
					android.view.ViewGroup.LayoutParams.MATCH_PARENT,
					AndroidLocation.CENTER);
		} else if (mode == LMode.FitFill) {
			// 添加游戏View，显示为按比例缩放情况下的最大值，并居中
			this.addView(view, w, h, AndroidLocation.CENTER);
		}
		getWindow().setContentView(frameLayout);
	}

	protected FrameLayout.LayoutParams createLayoutParams() {
		FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(
				android.view.ViewGroup.LayoutParams.MATCH_PARENT,
				android.view.ViewGroup.LayoutParams.MATCH_PARENT);
		layoutParams.gravity = Gravity.CENTER;
		return layoutParams;
	}

	private int maxWidth, maxHeight;

	private int zoomWidth, zoomHeight;

	protected void updateViewSize(final boolean landscape, int width,
			int height, LMode mode) {

		RectBox d = getScreenDimension();

		this.maxWidth = (int) d.getWidth();
		this.maxHeight = (int) d.getHeight();

		if (landscape && (d.getWidth() > d.getHeight())) {
			maxWidth = (int) d.getWidth();
			maxHeight = (int) d.getHeight();
		} else if (landscape && (d.getWidth() < d.getHeight())) {
			maxHeight = (int) d.getWidth();
			maxWidth = (int) d.getHeight();
		} else if (!landscape && (d.getWidth() < d.getHeight())) {
			maxWidth = (int) d.getWidth();
			maxHeight = (int) d.getHeight();
		} else if (!landscape && (d.getWidth() > d.getHeight())) {
			maxHeight = (int) d.getWidth();
			maxWidth = (int) d.getHeight();
		}

		if (mode != LMode.Max) {
			if (landscape) {
				this.zoomWidth = width;
				this.zoomHeight = height;
			} else {
				this.zoomWidth = height;
				this.zoomHeight = width;
			}
		} else {
			if (landscape) {
				this.zoomWidth = maxWidth >= width ? width : maxWidth;
				this.zoomHeight = maxHeight >= height ? height : maxHeight;
			} else {
				this.zoomWidth = maxWidth >= height ? height : maxWidth;
				this.zoomHeight = maxHeight >= width ? width : maxHeight;
			}
		}

		if (mode == LMode.Fill) {

			LSystem.setScaleWidth(((float) maxWidth) / zoomWidth);
			LSystem.setScaleHeight(((float) maxHeight) / zoomHeight);

		} else if (mode == LMode.FitFill) {

			RectBox res = AndroidGraphicsUtils.fitLimitSize(zoomWidth,
					zoomHeight, maxWidth, maxHeight);
			maxWidth = res.width;
			maxHeight = res.height;
			LSystem.setScaleWidth(((float) maxWidth) / zoomWidth);
			LSystem.setScaleHeight(((float) maxHeight) / zoomHeight);

		} else if (mode == LMode.Ratio) {

			maxWidth = View.MeasureSpec.getSize(maxWidth);
			maxHeight = View.MeasureSpec.getSize(maxHeight);

			float userAspect = (float) zoomWidth / (float) zoomHeight;
			float realAspect = (float) maxWidth / (float) maxHeight;

			if (realAspect < userAspect) {
				maxHeight = Math.round(maxWidth / userAspect);
			} else {
				maxWidth = Math.round(maxHeight * userAspect);
			}

			LSystem.setScaleWidth(((float) maxWidth) / zoomWidth);
			LSystem.setScaleHeight(((float) maxHeight) / zoomHeight);

		} else if (mode == LMode.MaxRatio) {

			maxWidth = View.MeasureSpec.getSize(maxWidth);
			maxHeight = View.MeasureSpec.getSize(maxHeight);

			float userAspect = (float) zoomWidth / (float) zoomHeight;
			float realAspect = (float) maxWidth / (float) maxHeight;

			if ((realAspect < 1 && userAspect > 1)
					|| (realAspect > 1 && userAspect < 1)) {
				userAspect = (float) zoomHeight / (float) zoomWidth;
			}

			if (realAspect < userAspect) {
				maxHeight = Math.round(maxWidth / userAspect);
			} else {
				maxWidth = Math.round(maxHeight * userAspect);
			}

			LSystem.setScaleWidth(((float) maxWidth) / zoomWidth);
			LSystem.setScaleHeight(((float) maxHeight) / zoomHeight);

		} else {

			LSystem.setScaleWidth(1f);
			LSystem.setScaleHeight(1f);

		}

		LSystem.setScaleWidth(((float) maxWidth) / zoomWidth);
		LSystem.setScaleHeight(((float) maxHeight) / zoomHeight);
		LSystem.viewSize.setSize(zoomWidth, zoomHeight);

		StringBuffer sbr = new StringBuffer();
		sbr.append("Mode:").append(mode);
		sbr.append("\nWidth:").append(zoomWidth)
				.append(",Height:" + zoomHeight);
		sbr.append("\nMaxWidth:").append(maxWidth)
				.append(",MaxHeight:" + maxHeight);
		Log.d("Android2DSize", sbr.toString());
	}

	// 检查ADView状态，如果ADView上附着有其它View则删除，
	// 从而起到屏蔽-广告屏蔽组件的作用。
	public void safeguardAndroidADView(android.view.View view) {
		try {
			final android.view.ViewGroup vgp = (android.view.ViewGroup) view
					.getParent().getParent();
			if (vgp.getChildAt(1) != null) {
				vgp.removeViewAt(1);
			}
		} catch (Exception ex) {
		}
	}

	public void setActionBarVisibility(boolean visible) {
		if (AndroidGame.isAndroidVersionHigher(11)) {
			try {
				java.lang.reflect.Method getBarMethod = Activity.class
						.getMethod("getActionBar");
				Object actionBar = getBarMethod.invoke(this);
				if (actionBar != null) {
					java.lang.reflect.Method showHideMethod = actionBar
							.getClass().getMethod((visible) ? "show" : "hide");
					showHideMethod.invoke(actionBar);
				}
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
	}

	public View inflate(final int layoutID) {
		final android.view.LayoutInflater inflater = android.view.LayoutInflater
				.from(this);
		return inflater.inflate(layoutID, null);
	}

	public void addView(final View view, AndroidLocation location) {
		if (view == null) {
			return;
		}
		addView(view, android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
				android.view.ViewGroup.LayoutParams.WRAP_CONTENT, location);
	}

	public void addView(final View view, int w, int h, AndroidLocation location) {
		if (view == null) {
			return;
		}
		android.widget.RelativeLayout viewLayout = new android.widget.RelativeLayout(
				this);
		android.widget.RelativeLayout.LayoutParams relativeParams = AndroidGame
				.createRelativeLayout(location, w, h);
		viewLayout.addView(view, relativeParams);
		addView(viewLayout);
	}

	public void addView(final View view) {
		if (view == null) {
			return;
		}
		frameLayout.addView(view, createLayoutParams());
		try {
			if (view.getVisibility() != View.VISIBLE) {
				view.setVisibility(View.VISIBLE);
			}
		} catch (Exception e) {
		}
	}

	public void removeView(final View view) {
		if (view == null) {
			return;
		}
		frameLayout.removeView(view);
		try {
			if (view.getVisibility() != View.GONE) {
				view.setVisibility(View.GONE);
			}
		} catch (Exception e) {
		}
	}

	public void setImmersiveMode(boolean use) {
		if (!use || AndroidGame.getSDKVersion() < 19) {
			return;
		}
		View view = getWindow().getDecorView();
		try {
			java.lang.reflect.Method m = View.class.getMethod(
					"setSystemUiVisibility", int.class);
			int code = 0x00000100 | 0x00000200 | 0x00000400 | 0x00000002
					| 0x00000004 | 0x00001000;
			m.invoke(view, code);
		} catch (Exception e) {
		}
	}

	public int setAD(String ad) {
		int result = 0;
		try {
			Class<LGame> clazz = LGame.class;
			java.lang.reflect.Field[] field = clazz.getDeclaredFields();
			if (field != null) {
				result = field.length;
			}
		} catch (Exception e) {
		}
		return result + ad.length();
	}

	public FrameLayout getLayout() {
		return frameLayout;
	}

	public RectBox getScreenDimension() {
		DisplayMetrics dm = getSysDisplayMetrices();
		return new RectBox(dm.xdpi, dm.ydpi, dm.widthPixels, dm.heightPixels);
	}

	@Override
	public int getContainerWidth() {
		DisplayMetrics dm = getSysDisplayMetrices();
		return dm.widthPixels;
	}

	@Override
	public int getContainerHeight() {
		DisplayMetrics dm = getSysDisplayMetrices();
		return dm.heightPixels;
	}

	public void close() {
		try {
			this.finish();
			System.exit(-1);
		} catch (Throwable noop) {
		}
	}

	public Orientation getOrientation() {
		if (getContainerHeight() > getContainerWidth()) {
			return Orientation.Portrait;
		} else {
			return Orientation.Landscape;
		}
	}

	public int getMaxWidth() {
		return maxWidth;
	}

	public int getMaxHeight() {
		return maxHeight;
	}
}