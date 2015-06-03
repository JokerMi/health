package siat.ncu.press.main;


import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;

import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.chart.PointStyle;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;

import siat.ncu.press.bean.PressInfo;
import siat.ncu.press.util.BluetoothCommunicateThread;
import siat.ncu.press.util.BluetoothService;
import siat.ncu.press.util.Cache;
import siat.ncu.press.util.DataProcess;
import siat.ncu.press.util.FileUtil;
import siat.ncu.press.util.XYRenderer;
import siat.ncu.press.util.MyContents.BlueTools;
import android.R.integer;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.view.DragEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnDragListener;
import android.view.View.OnGenericMotionListener;
import android.view.View.OnHoverListener;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.commlibrary.android.utils.CustomLog;
import com.commlibrary.android.utils.ToastManager;

public class MainActivity extends PressBaseActivity {

    private LinearLayout topLineLay;
    private LinearLayout btoothLinLay;
    private LinearLayout saveLinLay;
    private LinearLayout cleanLinLay;
    private ImageView btoothState;
    private TextView btoothName;
    private LinearLayout chartLineLay;
    private TextView voltageTv;
    
    private BluetoothCommunicateThread btoothCommunicateThread;
    
    private XYSeries xyseries;//����
    private XYSeries xyseries_up;//��ƽ����
    private XYSeries xyseries_down;//��ƽ����
    private XYMultipleSeriesDataset dataset;
    private GraphicalView chartview;
    private XYMultipleSeriesRenderer renderer;
    private XYSeriesRenderer datarenderer;
    private XYSeriesRenderer datarenderer_up;
    private XYSeriesRenderer datarenderer_down;
    private Context context;
   
    private double currentStep;
    private Thread pressThread;
    private double addX ;
    private double addY ;
    private int X_MIN = 0;
    private int X_MAX=10;//x�����ֵ
    //��׼����ֵ
    private double value=30 ;//ƽ�����м�ֵ
    private double bound=30;//������ �����Լ������趨
    private double boundTimes = 1.2; //Y����ʾ�ķ�Χ
    private double normalTimes = 1.5; //������С�߱���
    
    private String btName = BlueTools.BLUETOOTH_NAME;// ��������
    private String btMac = null;// ��������
    private boolean isAsyn = true;
    private Cache cache;
    private boolean pressFirstDrawflag = true;
    private boolean isThreadPause = false;
    
    private double sampleRateDouble; //����Ƶ��
    private double baseTime = 1; //����ʱ��1s
    private double xGap;         //x��Ĳ���������1���Բ���Ƶ��
    private double timeInterval;   //ʱ�����������߳�˯�ߣ�����1000������Բ���Ƶ��
    private double time = 1000;   //1000����
    private int currentStandardLength ; //��ǰ��X�����ƺ���ʱʹ��
    private int currentVerticalX;    //��ǰ��X����������ʱʹ��
    private ArrayList<PressInfo> mArrayList;
    private ArrayList<Double> oldX;
    private ArrayList<Double> oldY;
    private int max_Interval = 30; //�ܻ���������ʱ����
    private int loopAmount;
    private String sampleNameStr;
    private PressRunnable mPressRunnable;
    private boolean isAppend = true;
    private boolean sdCardExist = false;
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE); //��title
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);
        
        initView();
        initDatas();
        initEvent();
    }
    public void initView() {
        topLineLay  = (LinearLayout)findViewById(R.id.linLay_top);
        cleanLinLay = (LinearLayout)findViewById(R.id.linearLayout_cleanData);
        saveLinLay =(LinearLayout)findViewById(R.id.linearLayout_save);
        btoothLinLay = (LinearLayout)findViewById(R.id.linearLayout_btooth);
        voltageTv = (TextView)findViewById(R.id.tV_voltage);
        saveLinLay.setOnClickListener(myClickListener);
        cleanLinLay.setOnClickListener(myClickListener);
        btoothLinLay.setOnClickListener(myClickListener);
        
        btoothState = (ImageView)findViewById(R.id.iV_btstatue);
        btoothName = (TextView)findViewById(R.id.tV_btDeviceNm);
        
        chartLineLay = (LinearLayout)findViewById(R.id.linearLayout_chat);
        chartLineLay.setBackgroundColor(Color.BLACK);
    }
    public OnClickListener myClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.linearLayout_cleanData:
                    //�����������ݣ������ڴ��sd������
                    if(cleanSaveData()){
                        ToastManager.showToast(context, R.string.clean_success);
                    }else {
                        ToastManager.showToast(context, R.string.clean_fail);
                    }
                    break;
                case R.id.linearLayout_save:
                    if(resultArrayList == null || resultArrayList.size() == 0) {
                        ToastManager.showToast(context, R.string.no_save_data);
                        return;
                    }
                    if(!sdCardExist) {
                        ToastManager.showToast(context, R.string.no_sdcard);
                        return;
                    }
                    //���SD�����ڣ����ڴ������ݸ��Ƶ�SD����
                    saveDataToSD();
                    break;
                case R.id.linearLayout_btooth:
                   if(isConnected()) {
                        Toast.makeText(MainActivity.this, "����������", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    startDeviceListActivity();
                    break;

                default:
                    break;
            }
        }
    };
    
    private double calibrationValue;
    /**
     * ����ѹ������
     */
    public void saveDataToSD() {
        try {
            FileUtil.writeFile(FileUtil.RAMPATH , FileUtil.DIRPATH+"/"+sampleNameStr, isAppend);
            ToastManager.showToast(context, R.string.save_success);
        }
        catch (IOException e) {
            ToastManager.showToast(context, R.string.save_fail);
            e.printStackTrace();
        }
    }
    
    public boolean cleanSaveData() {
        boolean isDelSuccess = false;
        try {
            FileUtil.delFile(FileUtil.RAMPATH);
            if(sdCardExist) {
                FileUtil.delFile(FileUtil.SDPATH);
            }
            isDelSuccess = true;
        }
        catch (Exception e) {
        }
        return isDelSuccess;
    }
    public void clearAll() {
        
        mPressRunnable = null;
        
        for(int i=0;i <= dataset.getSeriesCount();i++) {
            dataset.removeSeries(i);
        }
        dataset.clear();
        renderer.removeAllRenderers();
        chartLineLay.removeView(chartview);
        chartLineLay.invalidate();
        initDatas();
    }
    
    public void startDeviceListActivity(){
        Intent serverIntent = new Intent(this, DeviceScanActivity.class);
        startActivityForResult(serverIntent,
                DeviceScanActivity.REQUEST_CONNECT_DEVICE);
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        CustomLog.i(TAG, "onActivityResult() resultCode=" + resultCode);
        switch (requestCode) {
            case DeviceScanActivity.REQUEST_CONNECT_DEVICE:
                if(resultCode == DeviceScanActivity.RESULT_OK) {
                    String address = data.getExtras().getString(
                            DeviceScanActivity.EXTRA_DEVICE_ADDRESS);
                    System.out.println("activity : address " + address);
                    connectPressDevice(address);
                }
                break;
        }
    }
    public void connectPressDevice(String address) {
        btMac = address;
        BluetoothDevice device = mBluetoothService
                .getRemoteDeviceByAddress(address);
        btName = device.getName();
        mBluetoothService.connect(device);
    }
    
    private Integer status = 0;
   
    protected Handler btoothHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case BluetoothService.MESSAGE_STATE_CHANGE:
                switch (msg.arg1) {
                case BluetoothService.STATE_CONNECTED:
                    btoothState.setImageResource(R.drawable.id_bluetooth_connect);
                    btoothName.setText(btName);
//                    setRenderEnable();
                    ToastManager.showToast(context, "�����ӵ�   " + btName);
                    break;
                case BluetoothService.STATE_CONNECTING:
                    btoothName.setText(R.string.connecting);
                    btoothState.setImageResource(R.drawable.id_bluetooth_unconnect);
                    break;
                case BluetoothService.STATE_NONE:
                    btoothName.setText(R.string.unconnect);
                    btoothState.setImageResource(R.drawable.id_bluetooth_unconnect);
//                    setRenderEnable();
                    if(pressThread.isAlive()) {
                        isThreadPause = true;
                        voltageTv.setText("0");
                        mPressRunnable.setSuspendFlag();
                    }
                    break;
                }
                break;

            case BluetoothService.MESSAGE_WRITE:
                byte[] writeBuf = (byte[]) msg.obj;
                new String(writeBuf);
                break;
            case BluetoothService.MESSAGE_READ:

                byte[] readBuf = (byte[]) msg.obj;
                CustomLog.v(TAG, "handleMessage() readBuf=" + Arrays.toString(readBuf));
                try {
                    processReadData(readBuf);
                    if(pressFirstDrawflag) {
                        pressThread.start();
                        pressFirstDrawflag = false;
                    }else if(isThreadPause){
                        mPressRunnable.setResume();
                        isThreadPause = false;
                    }
                }
                catch (Exception e) {
                    CustomLog.v("txl", "��ȡ�쳣  " + e.getMessage());
                }
                
                break;
            case BluetoothService.MESSAGE_TOAST:
                ToastManager.showToast(context, msg.getData().getString(BluetoothService.TOAST));
                break;
            case BluetoothService.MESSAGE_DEVICE:
                btName = msg.getData().getString(BluetoothService.DEVICE_NAME);
                btMac = msg.getData().getString(BluetoothService.DEVICE_ADDRESS);
                System.out.println(btName+" BluetoothService.MESSAGE_DEVICE "+btMac);
                cache.saveDeviceAddress(Cache.PressDevice, btMac);// �����ַ,�Ա��´��Զ�����
                break;
//          case Uploader.MESSAGE_UPLOADE_RESULT:
//              Bundle bundler = msg.getData();
//              String item = bundler.getString(Cache.ITEM);
//              int status = bundler.getInt(Uploader.STUTAS);
//              if (Cache.BP.equals(item)) {
//                  setImageView(hBpImageView, status);
//                  setImageView(lBpImageView, status);
//                  setImageView(pulseImageView, status);
//              }
//              if (Cache.BO.equals(item))
//                  setImageView(boImageView, status);
//              if (Cache.TEMP.equals(item))
//                  setImageView(tempImageView, status);
//              break;
            }
        };
    };

    public void initDatas() {
        
        Intent mIntent = getIntent();
        sampleRateDouble = Integer.valueOf(mIntent.getStringExtra("sampleRate"));
        mArrayList = mIntent.getParcelableArrayListExtra("pressinfo");
        loopAmount = mIntent.getIntExtra("loopAmount", 1);
        sampleNameStr = mIntent.getStringExtra("sampleName");
        
        mPressRunnable = new PressRunnable();
        for(PressInfo m:mArrayList) {
            System.out.println("  "+m.getPressValue()+" " +m.getPressValue());
        }
        xGap = baseTime/sampleRateDouble;
        timeInterval = time/sampleRateDouble;
        currentStandardLength = 0;
        currentVerticalX = 0;
        currentStep = 0;
        
        context = MainActivity.this;
        cache = new Cache(context);
        mBluetoothService = BluetoothService.getService(btoothHandler, true);// �첽��ʽ
        pressThread=new Thread(mPressRunnable); 
        
        oldX = new ArrayList<Double>();
        oldY = new ArrayList<Double>();
        addX = 0;
        addY = 0;
        
        sdCardExist = Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED); //�ж�sd���Ƿ����
        if (sdCardExist) {
          //����������ݵ�sdcardĿ¼
            FileUtil.createDir(FileUtil.DIRPATH);
        }
        
        initChatDatas();
    }
    
    private double totalPressValue;
    private double voltageValue;
    private boolean isFirstProcess = true;
    private String dataLine = "";
    protected void processReadData(byte[] readBuf) {
        
        resultArrayList = DataProcess.processReadData(readBuf);
        
        for(int i = 0;i<resultArrayList.size();i++){
            
            System.out.println("result = "+ resultArrayList.toString());
            
            
            if(resultArrayList.get(i).indexOf("0x") == -1) {
                int value = Integer.parseInt(resultArrayList.get(i), 16);
                double pressValue = DataProcess.countPressValue(value);
                totalPressValue += pressValue;
            }else {
                String voltageStr = resultArrayList.get(i).substring(2);
                addY = totalPressValue;
                if(isFirstProcess) {
                    isFirstProcess = false;
                    calibrationValue = addY;
//                    dataLine = "ѹ��ֵ" + "\t" + "��ѹֵ" + "\r\n";
//                    writeStrContent(dataLine);
                }
                addY = addY - calibrationValue;
                addY = DataProcess.getThreedecimal(addY);
                totalPressValue = 0;
                int value = Integer.parseInt(voltageStr, 16);
                voltageValue = DataProcess.countElectricValue(value);
                voltageTv.setText(voltageValue+"");
                
            }
            
        }
        
        /*BigDecimal b = new BigDecimal(((Math.random()) * bound)); //ת��
        addY = b.setScale(3, BigDecimal.ROUND_HALF_UP).doubleValue();
        //double+double��תһ��
        b = new BigDecimal(addY + value); //������λС�� ��������
        addY = b.setScale(3, BigDecimal.ROUND_HALF_UP).doubleValue();*/
    }
    
    /**
     * ���ַ�������д���ڴ���
     * @param dataStr
     */
    public void writeStrContent(String dataStr) {
        try {
            FileUtil.writeContent(dataStr, FileUtil.RAMPATH, isAppend);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void setRenderEnable() {
        boolean isEnable;
        if(isConnected()) {
            isEnable = false;
            chartLineLay.setClickable(isEnable);
        }else {
            isEnable = true;
            chartLineLay.setClickable(isEnable);
        }
    }
    @SuppressLint("NewApi")
    public void initChatDatas(){
      //No.1 �趨����Ⱦ��������
        renderer = new XYRenderer("ѹ��ͼ", "ʱ��(s)", "ѹ��(kg)", X_MIN, X_MAX, -1, 1, Color.GRAY, Color.LTGRAY, XYRenderer.TEXT_SIZE, XYRenderer.TEXT_SIZE, XYRenderer.TEXT_SIZE,
                XYRenderer.TEXT_SIZE, 10, 10, true);
        dataset = new XYMultipleSeriesDataset();
        datarenderer = new XYSeriesRenderer();
//        datarenderer.setDisplayChartValues(true);
        xyseries = new XYSeries("ʵ��ѹ��");
        //2
        xyseries.add(X_MIN, 0);//������һ�������������renderer
        //3
        dataset.addSeries(0, xyseries);
        datarenderer.setColor(Color.GREEN);
        datarenderer.setPointStyle(PointStyle.POINT);
        datarenderer.setLineWidth(4.0f);
        //4
        renderer.addSeriesRenderer(datarenderer);
        new Thread(){
            public void run() {
                for(int i=0;i<loopAmount;i++) {
                    initHorizonLine();
                }
            };
        }.start();
        initVerticalLine();
        renderer.setShowLegend(false);
        //�տ�ʼ��Ⱦ��ʱ���ֹ�û��������������ź��ƶ�������Ȼ���ױ���
        renderer.setPanEnabled(false);
        renderer.setExternalZoomEnabled(false);
        renderer.setClickEnabled(false);
        renderer.setZoomEnabled(false, false);
        //5
        
        

        //���ð�ť ������¼�ߴ�ı�׼(value)����ֵ(bound),Ȼ������������ʾ������

        //����������� �ֱ����µ�XYSeriesRendererҪ��Ȼ�ᱨ��
   
        
        /*xyseries_up = new XYSeries("���ѹ��");
        xyseries_down = new XYSeries("��Сѹ��");
        xyseries_up.add(X_MIN, value + normalTimes * bound);
        xyseries_up.add(X_MAX, value + normalTimes * bound);
        xyseries_down.add(X_MIN, value - normalTimes * bound);
        xyseries_down.add(X_MAX, value - normalTimes * bound);
        dataset.addSeries(1, xyseries_up);
        dataset.addSeries(2, xyseries_down);

        datarenderer_up = new XYSeriesRenderer();
        datarenderer_down = new XYSeriesRenderer();
        datarenderer_up.setColor(Color.RED);
        datarenderer_down = datarenderer_up;
        renderer.addSeriesRenderer(datarenderer_up);
        renderer.addSeriesRenderer(datarenderer_down);*/
        renderer.setYAxisMin(value - (boundTimes * bound));// Y��Сֵ
        renderer.setYAxisMax(value + (boundTimes * bound));// Y��Сֵ
        //����chart����ͼ��Χ  ����//1x->start 2max 3y->start 4max 
        renderer.setRange(new double[]
        {(double)X_MIN, (double) X_MAX, value - (boundTimes * bound), value + (boundTimes * bound)});
        
        chartview = ChartFactory.getLineChartView(context, dataset, renderer);
        chartLineLay.addView(chartview, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        
       /* chartview.setOnTouchListener(new View.OnTouchListener(){
            @Override
            public boolean onTouch(View v, MotionEvent event)  {
                topLineLay.invalidate();
                System.out.println("touchhhhh");
                return false;
            }
        });*/
        chartview.repaint();
    }
    
    public void initHorizonLine() {
        //��ʼ������
        for(int i=0;i < mArrayList.size();i++){
            PressInfo mInfo = mArrayList.get(i);
            //��Ϊһ���ܻ��������ֵΪ30�����������Ҫ����ֵ����30����ּ��λ�
            if(mInfo.getPressTime() > max_Interval) {
                for(int j=0;j < mInfo.getPressTime()/max_Interval;j++) {
                    initBaseLine(currentStandardLength, mInfo.getPressValue(), currentStandardLength+max_Interval, mInfo.getPressValue());
                    currentStandardLength += max_Interval;
                }
                if(mInfo.getPressTime()%max_Interval != 0) {
                    initBaseLine(currentStandardLength, mInfo.getPressValue(), currentStandardLength+mInfo.getPressTime()%max_Interval, mInfo.getPressValue());
                    currentStandardLength += mInfo.getPressTime()%max_Interval;
                }
            }else {
                initBaseLine(currentStandardLength, mInfo.getPressValue(), currentStandardLength+mInfo.getPressTime(), mInfo.getPressValue());
                currentStandardLength += mInfo.getPressTime();
            }
            /*if(i == 0 && mInfo.getPressTime() > X_MAX) {
                mXySeries.add(currentStandardLength,mInfo.getPressValue());
                mXySeries.add(X_MAX, mInfo.getPressValue());
                
                XYSeriesRenderer mRenderer = new XYSeriesRenderer();
                mRenderer.setColor(Color.RED);
                dataset.addSeries(mXySeries);
                renderer.addSeriesRenderer(mRenderer);
                break;
            }
            
            if(currentStandardLength <= X_MAX) {
                mXySeries.add(currentStandardLength,mInfo.getPressValue());
                mXySeries.add(currentStandardLength+mInfo.getPressTime(), mInfo.getPressValue());
                
                XYSeriesRenderer mRenderer = new XYSeriesRenderer();
                mRenderer.setColor(Color.RED);
                dataset.addSeries(mXySeries);
                renderer.addSeriesRenderer(mRenderer);
            }else {
                mXySeries.add(currentStandardLength-mInfo.getPressTime(),mInfo.getPressValue());
                mXySeries.add(X_MAX, mInfo.getPressValue());
                
                XYSeriesRenderer mRenderer = new XYSeriesRenderer();
                mRenderer.setColor(Color.RED);
                dataset.addSeries(mXySeries);
                renderer.addSeriesRenderer(mRenderer);
                break;
            }*/
        }
        
    }
    public void initVerticalLine() {
      //��ʼ������
        ArrayList<PressInfo> newArrayList = new ArrayList<PressInfo>();
        for(int i=0;i < loopAmount;i++) {
            for(int j=0;j<mArrayList.size();j++) {
                newArrayList.add(mArrayList.get(j));
            }
        }
        for(int i=0; i < newArrayList.size()-1;i++) {
            currentVerticalX += newArrayList.get(i).getPressTime();
            initBaseLine(currentVerticalX, newArrayList.get(i).getPressValue(), currentVerticalX, newArrayList.get(i+1).getPressValue());
        }
    }
    
    public void initBaseLine(double startX, double startY, double endX, double endY ) {
        XYSeries mXySeries = new XYSeries("");
        mXySeries.add(startX,startY);
        mXySeries.add(endX, endY);
        
        XYSeriesRenderer mRenderer = new XYSeriesRenderer();
        mRenderer.setColor(Color.RED);
        mRenderer.setLineWidth(2.0f);
        dataset.addSeries(mXySeries);
        renderer.addSeriesRenderer(mRenderer);
    }
    
    public void initEvent() {
        topLineLay.invalidate();
//        chartview.setClickable(true);
//      connectPressDevice();
//      pressThread.start();
//      setVisibility();
    }
    
    /**
     * ���ü���button����ʾ������
     * 
     * @param status
     */
    private void setVisibility() {
        if (isConnected()) {
            btoothName.setText(btName);
            btoothState.setBackgroundResource(R.drawable.id_bluetooth_connect);
        } else {
            btoothName.setText("δ����");
            btoothState.setBackgroundResource(R.drawable.id_bluetooth_unconnect);
        }
    }
    /**
     * ���ӵ��豸
     */
    private void connectPressDevice() {
        if (mBluetoothService.getState() == BluetoothService.STATE_NONE) {// ����״̬������
            String address = cache.getDeviceAddress(Cache.PressDevice);
            System.out.println("address = " + address);
            if(address != null) {
                btMac = address;
                BluetoothDevice device = mBluetoothService
                        .getRemoteDeviceByAddress(address);
                if(device != null) {
                btName = device.getName();
                System.out.println("btName = " +btName);
                mBluetoothService.connect(device);
                }
            }
        }
    }
    protected void onResume() {
        if(getRequestedOrientation()!=ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE){
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
           }
           super.onResume();
    };
    //handler����UI����
    Handler chartHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
//          Bundle b=msg.getData();
//          double x=b.getDouble("part_X");
//          double y=b.getDouble("part_Y");
            
            chartview.invalidate();
            topLineLay.invalidate();
//           chartview.repaint();
            //part_id1.setText(x);
            //part_size1.setText(y);
             
            if(currentStep >= X_MAX) {//�ӳ�X_MAX�������Ч��

                X_MIN += 5;
                X_MAX += 5;//��2���ٶ��ӳ� �������ó�speed
                renderer.setXAxisMax(X_MAX);// ����X���ֵ
                renderer.setXAxisMin(X_MIN);
                
//                datarenderer_up = new XYSeriesRenderer();
//                datarenderer_down = new XYSeriesRenderer();
//                datarenderer_up.setColor(Color.RED);
//                datarenderer_down=datarenderer_up;
            }
        }
        
    };
    public class PressRunnable implements Runnable {
        BigDecimal b;
        private boolean flag = true;
        private boolean suspendFlag = false; //�����̵߳���ͣ�ͼ���
        
        public void setSuspendFlag() {
            this.suspendFlag = true;
        }
        
        public synchronized void setResume() {
            this.suspendFlag = false;
            this.notify();
        }
        public void run() {
            try {
                if(!isConnected()) {
                    return;
                }
                while (flag) {
                    synchronized (this) {
                        while (suspendFlag) {
                            this.wait();
                        }
                        currentStep = currentStep + xGap;
                        System.out.println("currentStep"+currentStep);
                        /*if (i > 1100) {
                            i = 100;
                            xyseries.clear();
                        }*/
                        Thread.sleep((int)timeInterval);
                        addX = currentStep;
                        //���ú���һ����Ҫ���ӵĽڵ�
                        
//                        b = new BigDecimal(((Math.random()) * bound)); //ת��
//                        addY = b.setScale(3, BigDecimal.ROUND_HALF_UP).doubleValue();
//                        //double+double��תһ��
//                        b = new BigDecimal(addY + value); //������λС�� ��������
//                        addY = b.setScale(3, BigDecimal.ROUND_HALF_UP).doubleValue();

                        // ���費�ܱ� 1��������� 2����ԭ�������� 3���������� 4����ԭ�������� 5������������ 
                        //1
                        dataset.removeSeries(xyseries);
                        //2���ȱ���ԭ����
                        for(int i=0;i<xyseries.getScaleNumber();i++) {
                            oldX.add((double)xyseries.getX(i));
                            oldY.add((double)xyseries.getY(i));
                        }
                     // �㼯����գ�Ϊ�������µĵ㼯��׼��
                        //3
                        xyseries.add(addX, addY);
                        //4
                        for(int i=0;i<oldX.size();i++) {
                            xyseries.add(oldX.get(i),oldY.get(i));
                        }
                        //5
                        dataset.addSeries(0,xyseries);
                        
                        double newAddX = DataProcess.getTwodecimal(addX);
                        dataLine = newAddX+"\t  "+Math.abs(addY) +"\r\n";
                        System.out.println("dataLine = " + dataLine);
                        writeStrContent(dataLine);

                        //����hanlder
                       Message message = chartHandler.obtainMessage();
                       message.what = 1;//��־���ĸ��̴߳�����  
                       chartHandler.sendMessage(message);//����message��Ϣ  
                    }
                }
            }
            catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
    
   /* Runnable PressRunnable = new Runnable() {
        BigDecimal b;
        public void run() {
            try {
                while (true)
                {
                    i = i + 100;
                    if (i > 1100) {
                        i = 100;
                        xyseries.clear();
                    }
                    Thread.sleep(100);
                    addX = i;
                    //���ú���һ����Ҫ���ӵĽڵ�
                    b = new BigDecimal(((Math.random()) * bound * 4) - (2 * bound)); //ת��
                    addY = b.setScale(3, BigDecimal.ROUND_HALF_UP).doubleValue();
                    //double+double��תһ��
                    b = new BigDecimal(addY + value); //������λС�� ��������
                    addY = b.setScale(3, BigDecimal.ROUND_HALF_UP).doubleValue();

                    dataset.removeSeries(xyseries);
                    xyseries.add(addX, addY);
                    dataset.addSeries(0, xyseries);
                    //����hanlder
                    Message message = chatHandler.obtainMessage();
                    //                 Bundle bundle=new Bundle();  
                    //                 bundle.putDouble("part_X", addX);  
                    //                 bundle.putDouble("part_Y", addY); 
                    //                 message.setData(bundle);//bundle��ֵ����ʱ��Ч�ʵ�  
                    message.what = 1;//��־���ĸ��̴߳�����  
                    chatHandler.sendMessage(message);//����message��Ϣ  
                    //chartview.repaint();
                    //                 chartview.postInvalidate();
                     //���費�ܱ� 1��������� 2����Դ���� 3��������
                     dataset.removeSeries(xyseries);
                     xyseries.add(addX, addY); 
                     dataset.addSeries(0,xyseries);
                     //chartview.repaint();
                     chartview.postInvalidate();
                     if(i*2>X_MAX)//�ӳ�X_MAX�������Ч��
                     {
                         X_MAX*=2;//��2���ٶ��ӳ� �������ó�speed
                         renderer.setXAxisMax(X_MAX);// ����X���ֵ
                         dataset.removeSeries(xyseries_up);
                         dataset.removeSeries(xyseries_down);
                         xyseries_up.add(X_MAX/2 , value+bound);
                         xyseries_up.add(X_MAX, value+bound);
                         xyseries_down.add(X_MAX/2, value-bound);
                         xyseries_down.add(X_MAX,value-bound);
                         dataset.addSeries(1,xyseries_up);
                         dataset.addSeries(2,xyseries_down);
                    //   datarenderer_up = new XYSeriesRenderer();
                    //   datarenderer_down = new XYSeriesRenderer();
                    //   datarenderer_up.setColor(Color.RED);
                    //   datarenderer_down=datarenderer_up;
                         renderer.addSeriesRenderer(datarenderer_up);
                         renderer.addSeriesRenderer(datarenderer_down);
                     }

                }
            }
            catch (InterruptedException e)
            {
                e.printStackTrace();
            }
        }
    };*/
        protected void onStop() {
            pressThread.interrupt();
//          dataset.clear();
//          renderer.removeAllRenderers();
//          chartLineLay.removeAllViews();
//          chartLineLay = null;
            super.onStop();
        };
        @Override
        protected void onDestroy() {
            pressThread.interrupt();
            BluetoothService.close();
//          dataset.clear();
//            renderer.removeAllRenderers();
            System.out.println("destory");
            
            super.onDestroy();
        }
}