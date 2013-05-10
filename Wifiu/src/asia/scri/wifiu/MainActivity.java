package asia.scri.wifiu;

import java.util.List;
import java.util.Timer;

import org.json.JSONArray;
import org.json.JSONException;

import twitter4j.Twitter;
import twitter4j.TwitterException;

import net.arnx.jsonic.JSON; //jsonicを使う
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import android.net.wifi.WifiManager; //wifiの利用
import android.net.wifi.ScanResult; //wifiスキャン
import android.net.wifi.WifiInfo; //wifiの接続情報
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.view.Menu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;
import android.util.Log;

public class MainActivity extends Activity {
	public String responses="hoge";
	private String mInputText; //ついーとよう
    private Twitter mTwitter; //ついーとよう
    final static String TAG = "MyService";
    final int INTERVAL_PERIOD = 5000;
    Timer timer = new Timer();
	 
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//setContentView(R.layout.activity_main);
		
		if (!TwitterUtils.hasAccessToken(this)) {
            Intent intent = new Intent(this, TwitterOAuthActivity.class);
            startActivity(intent);
            finish();
        }
		

		//wifiスキャンした結果をリストへ
		final WifiManager manager = (WifiManager) getSystemService(WIFI_SERVICE);
		//wifi情報取得時
		if (manager.getWifiState() == WifiManager.WIFI_STATE_ENABLED) {//wifiを取得出来た
			WifiInfo info = manager.getConnectionInfo(); //wifi接続情報
			int ipAdr = info.getIpAddress(); //ipアドレスを取得
			//wifiに接続されていない状態ならば
			if(info.getSSID() == null){
				wifi_scan();
			}else{//wifiに接続されている
				wifi_connect(); //接続しているwifiアクセスポイントの情報
			}
			//Log.d("wifi",info.toString());
			//Toast.makeText(this,info.toString(), Toast.LENGTH_LONG).show();
		    /**/
		}else{//wifiを取得できなかった
			// ボタンを生成
			Button btn = new Button(this);
			btn.setText("wifiが取得できません");
			// レイアウトにボタンを追加
			LinearLayout layout = new LinearLayout(this);
			layout.addView(btn, new LinearLayout.LayoutParams(
					LinearLayout.LayoutParams.WRAP_CONTENT,
					LinearLayout.LayoutParams.WRAP_CONTENT));
			setContentView(layout);
		}
	}
	
	/**
	 * 接続しているwifi情報をリスト表示
	 */
	private void wifi_connect(){
		//リスト使うよ
		ListView lv2 = new ListView(this);
		setContentView(lv2);
		final WifiManager manager = (WifiManager) getSystemService(WIFI_SERVICE);
		WifiInfo info = manager.getConnectionInfo(); //wifi接続情報
	    String[] apInfo = new String[4];
	    //　SSIDを取得
	    apInfo[0] = String.format("%s", info.getSSID());
		mTwitter = TwitterUtils.getTwitterInstance(this);
        mInputText = apInfo[0]+"に接続しました";
       
	    // IPアドレスを取得
	    int ipAdr = info.getIpAddress();
	    apInfo[1] = String.format("IP Adrress : %02d.%02d.%02d.%02d", 
	    		(ipAdr>>0)&0xff, (ipAdr>>8)&0xff, (ipAdr>>16)&0xff, (ipAdr>>24)&0xff);
	    // MACアドレスを取得
	    apInfo[2] = String.format("MAC Address : %s", info.getMacAddress());
	    // 受信信号強度&信号レベルを取得
	    int rssi = info.getRssi();
	    int level = WifiManager.calculateSignalLevel(rssi, 5);
	    apInfo[3] = String.format("RSSI : %d / Level : %d/4", rssi, level);
	    //リストビューを登録
	    ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, apInfo);
	    lv2.setAdapter(adapter);
	    Log.d("ssid",apInfo[0]);
	    Log.d("ip", String.format("%02d.%02d.%02d.%02d", 
	    		(ipAdr>>0)&0xff, (ipAdr>>8)&0xff, (ipAdr>>16)&0xff, (ipAdr>>24)&0xff));
		Log.d("mac",apInfo[2]);
	    //Toast.makeText(this, apInfo[2], Toast.LENGTH_LONG).show();
		
		//リストビューが選択された時の処理
		lv2.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				//ここに処理を書く
				 tweet();
			}
		});
		//リストビューが長押しされた時の処理
		lv2.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
	        @Override
	        public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
	            //ここに処理を書く
	        	//リロード
	        	finish();
	        	startActivity(getIntent());
	            return false;
	        }
	    });		
		
	}
	/**
	 *周囲のwifi情報を取得してリスト表示 
	 */
	private void wifi_scan(){
		//リスト使うよ
		ListView lv = new ListView(this);
		setContentView(lv);
		String[] items = null; //変数初期化
		String[] bssids = null; //変数初期化
		//wifiスキャンした結果をリストへ
		final WifiManager manager = (WifiManager) getSystemService(WIFI_SERVICE);
		//wifi情報取得時
		if (manager.getWifiState() == WifiManager.WIFI_STATE_ENABLED) {//wifiを取得出来た
			List<ScanResult> results = manager.getScanResults(); //wifiスキャン
			items = new String[results.size()];
			bssids = new String[results.size()];  	
			int huga=0;
			for (int i=0;i<results.size();++i) {
				bssids[i] = results.get(i).BSSID;
				items[i] = results.get(i).SSID+"\n"
						+results.get(i).BSSID+"\n"
				        +results.get(i).level;
				//bssids.put(results.get(i).BSSID);
				huga = i;
			}
			items[huga] = "↑近くのwifiを表示してます";
			showvenue(bssids);
			//リストビューを登録
			ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, items);
			lv.setAdapter(adapter);	    
			//リストビューが選択された時の処理
			lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
				@Override
				public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
					//ここに処理を書く
					ListView listView = (ListView) parent;
				    //String s1=String.valueOf(id);
				    // クリックされたアイテムを取得します
				    String item = (String) listView.getItemAtPosition(position);
				    String[] data = item.split("\n",0);//改行コードで分割
				    String bssid= data[1];
				    String ssid= data[0];
				    Listview_OnClick(bssid,ssid); //bssidとssidを送信
				}
			});
			
			//リストビューが長押しされた時の処理
			lv.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
		        @Override
		        public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
		            //ここに処理を書く
		        	//リロード
		        	finish();
		        	startActivity(getIntent());
		            return false;
		        }
		    });
		}
	}	
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}
	
	/**
	 * リストを押した時にダイアログを出す処理
	 */
	private void Listview_OnClick(final String bssid,final String ssid){
		//テキスト入力を受け付けるビューを作成します。
	    final EditText editView = new EditText(MainActivity.this);
	    //ダイアログを作成
		AlertDialog.Builder AlertDlgBldr = new AlertDialog.Builder(MainActivity.this);   
        AlertDlgBldr.setTitle("ベニューを追加しますか?");  
        AlertDlgBldr.setMessage("ベニュー:"+ssid);
        AlertDlgBldr.setView(editView);//テキスト入力ダイアログにする
        //okが押されたときの処理
        AlertDlgBldr.setPositiveButton("ok", new DialogInterface.OnClickListener(){
            @Override  
            public void onClick(DialogInterface dialog, int which) {
            	String name = editView.getText().toString(); //入力された文字をnameに
            	addvenue(bssid,ssid,name); //ベニュー追加
            }
        });
        //キャンセルが押された時の処理
        AlertDlgBldr.setNegativeButton("cancel", new DialogInterface.OnClickListener() {  
            @Override  
            public void onClick(DialogInterface dialog, int which) {  
            }
        });
        AlertDialog AlertDlg = AlertDlgBldr.create();  
        AlertDlg.show();
    }
	
	/**
	 * ベニュー情報を取得します．bssid -> ベニュー名
	 */
	//responseの取り出し
	public void setValue(String response){
		//int count=0;
		/*
		JSONArray itemArray=null; //JSONArray型のローカル変数宣言と初期化

		try{
			itemArray = new JSONArray(response);//jsonを配列に変換
		}catch (JSONException e){// TODO 自動生成された catch ブロック 例外処理
			e.printStackTrace();
		}
		Toast.makeText(this, itemArray.toString(), Toast.LENGTH_LONG).show();
		*/
		Toast.makeText(this,response.toString(), Toast.LENGTH_LONG).show();
		//String str = "{\"id\":\"141052897642283009\",\"screen_name\":\"tomy_kaira\",\"text\":\"@tomy_kaira test2\",\"reply_to\":141045475737485312,\"created_at\":\"2011-11-28 16:16:46 +0900\",\"reply_chain\":[{\"id\":\"141045475737485312\",\"screen_name\":\"tomy_kaira\",\"text\":\"記事かきました: Twitter 関連の動向など #twitter #ruby http://t.co/KnJ6g1g3\",\"reply_to\":null,\"created_at\":\"2011-11-28 15:47:16 +0900\",\"reply_chain\":null}]}";
		//JSONTweet parentTweet = JSON.decode(str, JSONTweet.class);
		/*
		count = itemArray.toString().length();
		JSONObject[] bookObject = new JSONObject[count];
        for (int i=0; i<count; i++){
        	try {
				bookObject[i] = itemArray.getJSONObject(i);
				Log.d("tag7"+i, bookObject[i].toString());
			} catch (JSONException e) {
				// TODO 自動生成された catch ブロック
				e.printStackTrace();
			}
        }
        /*
		for(int i=0; i<count; i++){
			try {
				Log.d("tag6", bookObject[i].getString("ssid"));
			} catch (JSONException e) {
				// TODO 自動生成された catch ブロック
				e.printStackTrace();
			}
		}*/
		//Toast.makeText(this, list, Toast.LENGTH_LONG).show();
		/*
		//リスト使うよ
		ListView lv = new ListView(this);
		setContentView(lv);
		//wifiスキャンした結果をリストへ
		final WifiManager manager = (WifiManager) getSystemService(WIFI_SERVICE);
		
		String[] items = null; //変数初期化
		String[] bssids = null; //変数初期化
		//wifi情報取得時
		if (manager.getWifiState() == WifiManager.WIFI_STATE_ENABLED) {//wifiを取得出来た
			List<ScanResult> results = manager.getScanResults(); //wifiスキャン
			items = new String[results.size()];
			bssids = new String[results.size()];  	
			int huga=0;
			for (int i=0;i<results.size();++i) {
				bssids[i] = results.get(i).BSSID;
				items[i] = results.get(i).SSID+"\n"
				        	+results.get(i).BSSID+"\n"
				        	+results.get(i).level;
				        	//bssids.put(results.get(i).BSSID);
				huga = i;
			}
			items[huga] = "↑近くのwifiを表示してます";
			showvenue(bssids);
			//リストビューを登録
			ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, items);
			lv.setAdapter(adapter);
			//リストビューが選択された時の処理
			lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
				@Override
				public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
					//ここに処理を書く
					ListView listView = (ListView) parent;
					//String s1=String.valueOf(id);
					// クリックされたアイテムを取得します
					String item = (String) listView.getItemAtPosition(position);
					String[] data = item.split("\n",0);//改行コードで分割
					String bssid= data[1];
					String ssid= data[0];
					Listview_OnClick(bssid,ssid); //bssidとssidを送信
				}
			});
		}else{//wifiを取得できなかった
		}
		*/
	}
	private void showvenue(String[] bssids){
		String jBssids = JSON.encode(bssids); //jsonicを使って 配列->jsonへ変換
		//POSTメソッド
		String reqUrl = "http://n0.x0.to/rsk/Chicken/showvenue.json";
		RequestParams params = new RequestParams();
		
		params.put("bssids", jBssids);
		AsyncHttpClient clientpost = new AsyncHttpClient();
		clientpost.post(reqUrl, params, new AsyncHttpResponseHandler(){
			@Override
			public void onSuccess(String response){
				// ここに通信が成功したときの処理をかく
				setValue(response);
			}
		});
		//Log.d("tag3",getValue());
		//Toast.makeText(this, getValue(), Toast.LENGTH_LONG).show();
	}
	
	/**
	 * サーバー側にベニューを追加します． bssidとssidとname(任意のベニュー名)を登録
	 */
	private void addvenue(String bssid,String ssid,String name){
		//postメソッド
		String reqUrl = "http://n0.x0.to/rsk/Chicken/addvenue.json";
		RequestParams params = new RequestParams();
		params.put("name", name);
		params.put("ssid", ssid);
		params.put("bssid", bssid);
		AsyncHttpClient clientpost = new AsyncHttpClient();
		clientpost.post(reqUrl, params, new AsyncHttpResponseHandler() {
			@Override
			public void onSuccess(String response){
				// ここに通信が成功したときの処理をかく
				Log.d("tag2", response);
			}
		});
	}
	
    private void tweet() {
        AsyncTask<String, Void, Boolean> task = new AsyncTask<String, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(String... params) {
                try {
                    mTwitter.updateStatus(params[0]);
                    return true;
                } catch (TwitterException e) {
                    e.printStackTrace();
                    return false;
                }
            }

            @Override
            protected void onPostExecute(Boolean result) {
                if (result) {
                    showToast("ツイートが完了しました！");
                    finish();
                } else {
                    showToast("ツイートに失敗しました。。。");
                }
            }
        };
        task.execute(mInputText);
    }

    private void showToast(String text) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }
	
}//end class