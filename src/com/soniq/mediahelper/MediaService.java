package com.soniq.mediahelper;

import java.io.File;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.widget.Toast;

import com.geniusgithub.mediarender.center.MediaRenderProxy;
import com.geniusgithub.mediarender.util.DlnaUtils;
import com.geniusgithub.mediarender.util.SharedUtils;

public class MediaService extends Service {
	private AirplayServer airServer;
	private MediaRenderProxy mRenderProxy;
	boolean isStartServer = false;
	boolean threadRuning = true;
	boolean isShowToast = false;
	private Handler mHandler = new Handler() {
		@Override
		public void handleMessage(android.os.Message msg) {
			switch (msg.what) {
			case 0:
				String str = (String) msg.obj;
				if (isShowToast) {
					Toast.makeText(MediaService.this, str, Toast.LENGTH_SHORT)
							.show();
				}
				break;
			default:
				break;
			}

		}
	};

	private void sendMessage(String str) {
		Message msg = mHandler.obtainMessage();
		msg.what = 0;
		msg.obj = str;
		mHandler.sendMessage(msg);
	}

	private void startCheckServerState() {
		new Thread() {
			public void run() {
				while (threadRuning) {
					checkServerState();
					try {
						sleep(1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		}.start();

	}

	private void checkServerState() {
		if (SharedUtils.getServerState() == 1
				&& DlnaUtils.getLocalIP(this) != null) {
			if (!isStartServer) {
				startAriPlayAndDlna();
			}
		} else if (SharedUtils.getServerState() == 0) {
			if (isStartServer) {
				stopAriPlayAndDlna();
			}
		}
	}

	private void startAriPlayAndDlna() {
		if (!NetworkUtils.isNetworkAvailable(this)) {
			return;
		}
		String ip = DlnaUtils.getLocalIP(MediaService.this);
		if (null == ip) {
			return;
		}
		sendMessage("同屏助手服务正在开启,请稍候...");
		sendBroadcast(new Intent(Constants.OPEN_CLOSE_STATE).putExtra("state",
				Constants.STATE_OPENING));
		mRenderProxy.startEngine();
		airServer.startService(
				MediaService.this,
				DlnaUtils.getDevName(MediaService.this) + "@"
						+ DlnaUtils.getIpLastNum(ip));
		sendMessage("同屏助手服务已开启");
		sendBroadcast(new Intent(Constants.OPEN_CLOSE_STATE).putExtra("state",
				Constants.STATE_OPENED));
		isStartServer = true;
	}

	private void stopAriPlayAndDlna() {
		if (!NetworkUtils.isNetworkAvailable(this)) {
			// sendMessage("网络不可用，请检查网络");
			return;
		}
		String ip = DlnaUtils.getLocalIP(MediaService.this);
		if (null == ip) {
			return;
		}
		sendMessage("同屏助手服务正在关闭,请稍候...");
		sendBroadcast(new Intent(Constants.OPEN_CLOSE_STATE).putExtra("state",
				Constants.STATE_CLOSEING));
		mRenderProxy.stopEngine();
		airServer.stopService();
		sendMessage("同屏助手服务已关闭");
		sendBroadcast(new Intent(Constants.OPEN_CLOSE_STATE).putExtra("state",
				Constants.STATE_CLOSEED));
		isStartServer = false;

	}

	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}

	@Override
	public void onCreate() {
		airServer = AirplayServer.getInstance(this);
		mRenderProxy = MediaRenderProxy.getInstance();
		startCheckServerState();
		deleteDirectory(this.getFilesDir().getAbsolutePath());
		CheckImageLoaderConfiguration.checkImageLoaderConfiguration(this);
		super.onCreate();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if (intent != null) {
			isShowToast = intent.getBooleanExtra("isShowToast", true);
		}
		return super.onStartCommand(intent, flags, startId);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		mHandler.removeCallbacksAndMessages(null);
		threadRuning = false;
	}

	public static boolean deleteDirectory(String filePath) {
		if (null == filePath) {
			return false;
		}

		File file = new File(filePath);

		if (file == null || !file.exists()) {
			return false;
		}

		if (file.isDirectory()) {
			File[] list = file.listFiles();

			for (int i = 0; i < list.length; i++) {
				// log.d("delete filePath: " + list[i].getAbsolutePath());
				if (list[i].isDirectory()) {
					deleteDirectory(list[i].getAbsolutePath());
				} else {
					list[i].delete();
				}
			}
		}

		file.delete();
		return true;
	}
}
