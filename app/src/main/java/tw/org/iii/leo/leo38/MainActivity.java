package tw.org.iii.leo.leo38;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.inuker.bluetooth.library.BluetoothClient;
import com.inuker.bluetooth.library.connect.listener.BleConnectStatusListener;
import com.inuker.bluetooth.library.connect.listener.BluetoothStateListener;
import com.inuker.bluetooth.library.connect.options.BleConnectOptions;
import com.inuker.bluetooth.library.connect.response.BleConnectResponse;
import com.inuker.bluetooth.library.connect.response.BleNotifyResponse;
import com.inuker.bluetooth.library.model.BleGattCharacter;
import com.inuker.bluetooth.library.model.BleGattProfile;
import com.inuker.bluetooth.library.model.BleGattService;
import com.inuker.bluetooth.library.search.SearchRequest;
import com.inuker.bluetooth.library.search.SearchResult;
import com.inuker.bluetooth.library.search.response.SearchResponse;

import java.util.List;
import java.util.UUID;

import static com.inuker.bluetooth.library.Constants.REQUEST_SUCCESS;
import static com.inuker.bluetooth.library.Constants.STATUS_CONNECTED;
import static com.inuker.bluetooth.library.Constants.STATUS_DISCONNECTED;

public class MainActivity extends AppCompatActivity {

    private BluetoothClient mClient ;


    private final BluetoothStateListener mBluetoothStateListener = new BluetoothStateListener() {
        @Override
        public void onBluetoothStateChanged(boolean openOrClosed) {
            ////
        }

    };

    private final BleConnectStatusListener mBleConnectStatusListener = new BleConnectStatusListener() {

        @Override
        public void onConnectStatusChanged(String mac, int status) {
            if (status == STATUS_CONNECTED) {
                Log.v("leo","真正連接成功");
            } else if (status == STATUS_DISCONNECTED) {
                Log.v("leo","disconnected");

            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {


            // No explanation needed; request the permission
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    123);

        } else {
            init();

        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        init();
    }

    private void init(){

        mClient = new BluetoothClient(this);
        mClient.registerBluetoothStateListener(mBluetoothStateListener);
//        mClient.isBluetoothOpened()
        mClient.openBluetooth();

    }
    //開始掃描
    public void test1(View view) {
        SearchRequest request = new SearchRequest.Builder()
                .searchBluetoothLeDevice(3000, 3)   // 先扫BLE设备3次，每次3s
                .searchBluetoothClassicDevice(5000) // 再扫经典蓝牙5s
                .searchBluetoothLeDevice(2000)      // 再扫BLE设备2s
                .build();

        mClient.search(request,new MySearchListener());

    }
        //  停止掃描
    public void test2(View view) {
        mClient.stopSearch();
    }

    public void test3(View view) {
        Log.v("leo","connecting...");

        BleConnectOptions options = new BleConnectOptions.Builder()
                .setConnectRetry(3)   // 连接如果失败重试3次
                .setConnectTimeout(30000)   // 连接超时30s
                .setServiceDiscoverRetry(3)  // 发现服务如果失败重试3次
                .setServiceDiscoverTimeout(20000)  // 发现服务超时20s
                .build();

        mClient.connect(connectMac,options, new BleConnectResponse() {
            @Override
            public void onResponse(int code, BleGattProfile profile) {
                if (code == REQUEST_SUCCESS) {
                    Log.v("leo","got response");

                    List<BleGattService> services = profile.getServices();
                    for(BleGattService service : services){
                        String uuid=service.getUUID().toString();
//                        Log.v("leo","service= "+uuid);

                        List<BleGattCharacter> cs = service.getCharacters();
                        for(BleGattCharacter c : cs){

//                            Log.v("leo","c = "+c.getUuid().toString());
                        }
                    }

                }
            }
        });
    }
    private String connectMac = "";
    private UUID serviceUUID = UUID.fromString("0000180f-0000-1000-8000-00805f9b34fb");
    private UUID characterUUID = UUID.fromString("00002a19-0000-1000-8000-00805f9b34fb");

    public void test4(View view) {

        mClient.notify(connectMac, serviceUUID, characterUUID, new BleNotifyResponse() {
            @Override
            public void onNotify(UUID service, UUID character, byte[] value) {
                for(byte v: value){
                    Log.v("leo","Value = "+v);
                }

            }

            @Override
            public void onResponse(int code) {
                if (code == REQUEST_SUCCESS) {
                    Log.v("leo","success");

                }else {
                    Log.v("leo","no Response");
                }
            }
        });
    }

    private class MySearchListener implements SearchResponse{

        @Override
        public void onSearchStarted() {

        }

        @Override
        public void onDeviceFounded(SearchResult result) {
            BluetoothDevice device = result.device;
            String name = result.getName();
            String mac = result.getAddress();
//            Log.v("leo",name+":"+mac);
            //要連別的機種就要改這個設備的名稱
            if(name.equals("OPPO R15")){
                Log.v("leo","I got it" + mac);
                connectMac = mac;
                mClient.stopSearch();
                mClient.registerConnectStatusListener(connectMac, mBleConnectStatusListener);

            }


        }

        @Override
        public void onSearchStopped() {

        }

        @Override
        public void onSearchCanceled() {

        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mClient.disconnect(connectMac);
        mClient.closeBluetooth();

        mClient.unregisterBluetoothStateListener(mBluetoothStateListener);
        mClient.unregisterConnectStatusListener(connectMac, mBleConnectStatusListener);


    }
}
