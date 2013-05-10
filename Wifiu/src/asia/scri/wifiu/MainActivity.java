package asia.scri.wifiu;

import java.util.List;
import java.util.Timer;

import org.json.JSONArray;
import org.json.JSONException;

import twitter4j.Twitter;
import twitter4j.TwitterException;

import net.arnx.jsonic.JSON; //jsonic���g��
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import android.net.wifi.WifiManager; //wifi�̗��p
import android.net.wifi.ScanResult; //wifi�X�L����
import android.net.wifi.WifiInfo; //wifi�̐ڑ����
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
	private String mInputText; //���[�Ƃ悤
    private Twitter mTwitter; //���[�Ƃ悤
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
		

		//wifi�X�L�����������ʂ����X�g��
		final WifiManager manager = (WifiManager) getSystemService(WIFI_SERVICE);
		//wifi���擾��
		if (manager.getWifiState() == WifiManager.WIFI_STATE_ENABLED) {//wifi���擾�o����
			WifiInfo info = manager.getConnectionInfo(); //wifi�ڑ����
			int ipAdr = info.getIpAddress(); //ip�A�h���X���擾
			//wifi�ɐڑ�����Ă��Ȃ���ԂȂ��
			if(info.getSSID() == null){
				wifi_scan();
			}else{//wifi�ɐڑ�����Ă���
				wifi_connect(); //�ڑ����Ă���wifi�A�N�Z�X�|�C���g�̏��
			}
			//Log.d("wifi",info.toString());
			//Toast.makeText(this,info.toString(), Toast.LENGTH_LONG).show();
		    /**/
		}else{//wifi���擾�ł��Ȃ�����
			// �{�^���𐶐�
			Button btn = new Button(this);
			btn.setText("wifi���擾�ł��܂���");
			// ���C�A�E�g�Ƀ{�^����ǉ�
			LinearLayout layout = new LinearLayout(this);
			layout.addView(btn, new LinearLayout.LayoutParams(
					LinearLayout.LayoutParams.WRAP_CONTENT,
					LinearLayout.LayoutParams.WRAP_CONTENT));
			setContentView(layout);
		}
	}
	
	/**
	 * �ڑ����Ă���wifi�������X�g�\��
	 */
	private void wifi_connect(){
		//���X�g�g����
		ListView lv2 = new ListView(this);
		setContentView(lv2);
		final WifiManager manager = (WifiManager) getSystemService(WIFI_SERVICE);
		WifiInfo info = manager.getConnectionInfo(); //wifi�ڑ����
	    String[] apInfo = new String[4];
	    //�@SSID���擾
	    apInfo[0] = String.format("%s", info.getSSID());
		mTwitter = TwitterUtils.getTwitterInstance(this);
        mInputText = apInfo[0]+"�ɐڑ����܂���";
       
	    // IP�A�h���X���擾
	    int ipAdr = info.getIpAddress();
	    apInfo[1] = String.format("IP Adrress : %02d.%02d.%02d.%02d", 
	    		(ipAdr>>0)&0xff, (ipAdr>>8)&0xff, (ipAdr>>16)&0xff, (ipAdr>>24)&0xff);
	    // MAC�A�h���X���擾
	    apInfo[2] = String.format("MAC Address : %s", info.getMacAddress());
	    // ��M�M�����x&�M�����x�����擾
	    int rssi = info.getRssi();
	    int level = WifiManager.calculateSignalLevel(rssi, 5);
	    apInfo[3] = String.format("RSSI : %d / Level : %d/4", rssi, level);
	    //���X�g�r���[��o�^
	    ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, apInfo);
	    lv2.setAdapter(adapter);
	    Log.d("ssid",apInfo[0]);
	    Log.d("ip", String.format("%02d.%02d.%02d.%02d", 
	    		(ipAdr>>0)&0xff, (ipAdr>>8)&0xff, (ipAdr>>16)&0xff, (ipAdr>>24)&0xff));
		Log.d("mac",apInfo[2]);
	    //Toast.makeText(this, apInfo[2], Toast.LENGTH_LONG).show();
		
		//���X�g�r���[���I�����ꂽ���̏���
		lv2.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				//�����ɏ���������
				 tweet();
			}
		});
		//���X�g�r���[�����������ꂽ���̏���
		lv2.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
	        @Override
	        public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
	            //�����ɏ���������
	        	//�����[�h
	        	finish();
	        	startActivity(getIntent());
	            return false;
	        }
	    });		
		
	}
	/**
	 *���͂�wifi�����擾���ă��X�g�\�� 
	 */
	private void wifi_scan(){
		//���X�g�g����
		ListView lv = new ListView(this);
		setContentView(lv);
		String[] items = null; //�ϐ�������
		String[] bssids = null; //�ϐ�������
		//wifi�X�L�����������ʂ����X�g��
		final WifiManager manager = (WifiManager) getSystemService(WIFI_SERVICE);
		//wifi���擾��
		if (manager.getWifiState() == WifiManager.WIFI_STATE_ENABLED) {//wifi���擾�o����
			List<ScanResult> results = manager.getScanResults(); //wifi�X�L����
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
			items[huga] = "���߂���wifi��\�����Ă܂�";
			showvenue(bssids);
			//���X�g�r���[��o�^
			ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, items);
			lv.setAdapter(adapter);	    
			//���X�g�r���[���I�����ꂽ���̏���
			lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
				@Override
				public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
					//�����ɏ���������
					ListView listView = (ListView) parent;
				    //String s1=String.valueOf(id);
				    // �N���b�N���ꂽ�A�C�e�����擾���܂�
				    String item = (String) listView.getItemAtPosition(position);
				    String[] data = item.split("\n",0);//���s�R�[�h�ŕ���
				    String bssid= data[1];
				    String ssid= data[0];
				    Listview_OnClick(bssid,ssid); //bssid��ssid�𑗐M
				}
			});
			
			//���X�g�r���[�����������ꂽ���̏���
			lv.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
		        @Override
		        public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
		            //�����ɏ���������
		        	//�����[�h
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
	 * ���X�g�����������Ƀ_�C�A���O���o������
	 */
	private void Listview_OnClick(final String bssid,final String ssid){
		//�e�L�X�g���͂��󂯕t����r���[���쐬���܂��B
	    final EditText editView = new EditText(MainActivity.this);
	    //�_�C�A���O���쐬
		AlertDialog.Builder AlertDlgBldr = new AlertDialog.Builder(MainActivity.this);   
        AlertDlgBldr.setTitle("�x�j���[��ǉ����܂���?");  
        AlertDlgBldr.setMessage("�x�j���[:"+ssid);
        AlertDlgBldr.setView(editView);//�e�L�X�g���̓_�C�A���O�ɂ���
        //ok�������ꂽ�Ƃ��̏���
        AlertDlgBldr.setPositiveButton("ok", new DialogInterface.OnClickListener(){
            @Override  
            public void onClick(DialogInterface dialog, int which) {
            	String name = editView.getText().toString(); //���͂��ꂽ������name��
            	addvenue(bssid,ssid,name); //�x�j���[�ǉ�
            }
        });
        //�L�����Z���������ꂽ���̏���
        AlertDlgBldr.setNegativeButton("cancel", new DialogInterface.OnClickListener() {  
            @Override  
            public void onClick(DialogInterface dialog, int which) {  
            }
        });
        AlertDialog AlertDlg = AlertDlgBldr.create();  
        AlertDlg.show();
    }
	
	/**
	 * �x�j���[�����擾���܂��Dbssid -> �x�j���[��
	 */
	//response�̎��o��
	public void setValue(String response){
		//int count=0;
		/*
		JSONArray itemArray=null; //JSONArray�^�̃��[�J���ϐ��錾�Ə�����

		try{
			itemArray = new JSONArray(response);//json��z��ɕϊ�
		}catch (JSONException e){// TODO �����������ꂽ catch �u���b�N ��O����
			e.printStackTrace();
		}
		Toast.makeText(this, itemArray.toString(), Toast.LENGTH_LONG).show();
		*/
		Toast.makeText(this,response.toString(), Toast.LENGTH_LONG).show();
		//String str = "{\"id\":\"141052897642283009\",\"screen_name\":\"tomy_kaira\",\"text\":\"@tomy_kaira test2\",\"reply_to\":141045475737485312,\"created_at\":\"2011-11-28 16:16:46 +0900\",\"reply_chain\":[{\"id\":\"141045475737485312\",\"screen_name\":\"tomy_kaira\",\"text\":\"�L�������܂���: Twitter �֘A�̓����Ȃ� #twitter #ruby http://t.co/KnJ6g1g3\",\"reply_to\":null,\"created_at\":\"2011-11-28 15:47:16 +0900\",\"reply_chain\":null}]}";
		//JSONTweet parentTweet = JSON.decode(str, JSONTweet.class);
		/*
		count = itemArray.toString().length();
		JSONObject[] bookObject = new JSONObject[count];
        for (int i=0; i<count; i++){
        	try {
				bookObject[i] = itemArray.getJSONObject(i);
				Log.d("tag7"+i, bookObject[i].toString());
			} catch (JSONException e) {
				// TODO �����������ꂽ catch �u���b�N
				e.printStackTrace();
			}
        }
        /*
		for(int i=0; i<count; i++){
			try {
				Log.d("tag6", bookObject[i].getString("ssid"));
			} catch (JSONException e) {
				// TODO �����������ꂽ catch �u���b�N
				e.printStackTrace();
			}
		}*/
		//Toast.makeText(this, list, Toast.LENGTH_LONG).show();
		/*
		//���X�g�g����
		ListView lv = new ListView(this);
		setContentView(lv);
		//wifi�X�L�����������ʂ����X�g��
		final WifiManager manager = (WifiManager) getSystemService(WIFI_SERVICE);
		
		String[] items = null; //�ϐ�������
		String[] bssids = null; //�ϐ�������
		//wifi���擾��
		if (manager.getWifiState() == WifiManager.WIFI_STATE_ENABLED) {//wifi���擾�o����
			List<ScanResult> results = manager.getScanResults(); //wifi�X�L����
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
			items[huga] = "���߂���wifi��\�����Ă܂�";
			showvenue(bssids);
			//���X�g�r���[��o�^
			ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, items);
			lv.setAdapter(adapter);
			//���X�g�r���[���I�����ꂽ���̏���
			lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
				@Override
				public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
					//�����ɏ���������
					ListView listView = (ListView) parent;
					//String s1=String.valueOf(id);
					// �N���b�N���ꂽ�A�C�e�����擾���܂�
					String item = (String) listView.getItemAtPosition(position);
					String[] data = item.split("\n",0);//���s�R�[�h�ŕ���
					String bssid= data[1];
					String ssid= data[0];
					Listview_OnClick(bssid,ssid); //bssid��ssid�𑗐M
				}
			});
		}else{//wifi���擾�ł��Ȃ�����
		}
		*/
	}
	private void showvenue(String[] bssids){
		String jBssids = JSON.encode(bssids); //jsonic���g���� �z��->json�֕ϊ�
		//POST���\�b�h
		String reqUrl = "http://n0.x0.to/rsk/Chicken/showvenue.json";
		RequestParams params = new RequestParams();
		
		params.put("bssids", jBssids);
		AsyncHttpClient clientpost = new AsyncHttpClient();
		clientpost.post(reqUrl, params, new AsyncHttpResponseHandler(){
			@Override
			public void onSuccess(String response){
				// �����ɒʐM�����������Ƃ��̏���������
				setValue(response);
			}
		});
		//Log.d("tag3",getValue());
		//Toast.makeText(this, getValue(), Toast.LENGTH_LONG).show();
	}
	
	/**
	 * �T�[�o�[���Ƀx�j���[��ǉ����܂��D bssid��ssid��name(�C�ӂ̃x�j���[��)��o�^
	 */
	private void addvenue(String bssid,String ssid,String name){
		//post���\�b�h
		String reqUrl = "http://n0.x0.to/rsk/Chicken/addvenue.json";
		RequestParams params = new RequestParams();
		params.put("name", name);
		params.put("ssid", ssid);
		params.put("bssid", bssid);
		AsyncHttpClient clientpost = new AsyncHttpClient();
		clientpost.post(reqUrl, params, new AsyncHttpResponseHandler() {
			@Override
			public void onSuccess(String response){
				// �����ɒʐM�����������Ƃ��̏���������
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
                    showToast("�c�C�[�g���������܂����I");
                    finish();
                } else {
                    showToast("�c�C�[�g�Ɏ��s���܂����B�B�B");
                }
            }
        };
        task.execute(mInputText);
    }

    private void showToast(String text) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }
	
}//end class