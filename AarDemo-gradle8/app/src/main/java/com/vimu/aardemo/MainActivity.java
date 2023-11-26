package com.vimu.aardemo;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.vimu.msolibrary.ComDefs;
import com.vimu.msolibrary.usb.BasicHsfUsbWaveV12;
import com.vimu.msolibrary.usb.BasicUsbDev;
import com.vimu.msolibrary.usb.UsbDevMng;
import com.vimu.msolibrary.usb.OscDdsFactory;
import com.vimu.msolibrary.usb.BasicSbqUsbCardVer12;

import java.util.Vector;

public class MainActivity extends AppCompatActivity
        implements UsbDevMng.UsbDevDetectLister, BasicSbqUsbCardVer12.WaveReceiveLister {

    private static final String TAG = "MainActivity";

    private UsbDevMng usbManger = null;
    private BasicSbqUsbCardVer12 cardWave = null;
    private BasicHsfUsbWaveV12 ddsWave = null;
    private String mUsbInfos = new String();

    private int m_max_capture_length = 0;
    private int m_capture_length = 0;
    private double m_buffer[] = null;

    private int m_read_length = 0;

    private boolean m_osc_capturing = false;
    private double m_osc_range_minv;
    private double m_osc_range_maxv;

    //Trigger
    ArrayAdapter<String> mTriggerModeAdapter;
    ArrayAdapter<String> mTriggerSourceAdapter;
    ArrayAdapter<String> mTriggerStyleAdapter;
    String[] mTriggerStyleStrings = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 获取布局中的控件
        Resources mResources = getResources();

        //DDS
        Switch mDdsSwitch = findViewById(R.id.dds_switch);
        mDdsSwitch.setOnCheckedChangeListener(ddsCheckedChangeListener);

        //Mode
        String[] mDdsModeStrings = mResources.getStringArray(R.array.DDS_Mode_Strings);
        ArrayAdapter<String> mDdsModeAdapter= new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, mDdsModeStrings);
        mDdsModeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);     //设置下拉列表框的下拉选项样式
        Spinner modeSpiner = (Spinner) findViewById(R.id.dds_mode_spinner);
        modeSpiner.setAdapter(mDdsModeAdapter);
        modeSpiner.setOnItemSelectedListener(spinnerSelectedListener);

        //Wave
        Vector<String> mDdsWaveStrings = new Vector<>();
        mDdsWaveStrings.add(mResources.getString(R.string.Sine));
        mDdsWaveStrings.add(mResources.getString(R.string.Rectangle));
        mDdsWaveStrings.add(mResources.getString(R.string.Pulse));
        mDdsWaveStrings.add(mResources.getString(R.string.White_Noise));
        mDdsWaveStrings.add(mResources.getString(R.string.Ramp));
        mDdsWaveStrings.add(mResources.getString(R.string.DC));
        //mDdsWaveStrings.add(mResources.getString(R.string.Arb));

        ArrayAdapter<String> mDdsWaveAdapter= new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, mDdsWaveStrings);
        mDdsWaveAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);     //设置下拉列表框的下拉选项样式
        Spinner wave = (Spinner) findViewById(R.id.dds_wave_spinner);
        wave.setAdapter(mDdsWaveAdapter);
        wave.setSelection(0);
        wave.setOnItemSelectedListener(spinnerSelectedListener);

        //幅度
        EditSpinnerVolt mDdsVppEdit = findViewById(R.id.dds_vpp_Number);
        mDdsVppEdit.SetValueRange(0, 6000);
        mDdsVppEdit.SetValue(1000);
        mDdsVppEdit.setOnEditSpinnerActionListener(editSpinnerActionListener);

        //偏置
        EditSpinnerVolt mDdsBiasEdit = findViewById(R.id.dds_bias_Number);
        mDdsBiasEdit.SetValueRange(-2000, 2000);
        mDdsBiasEdit.SetValue(0);
        mDdsBiasEdit.setOnEditSpinnerActionListener(editSpinnerActionListener);

        //频率
        double freq_min = 1;
        double freq_max = 1000000;
        double freq = 1000;
        EditSpinnerFreq mDdsFreqEdit = findViewById(R.id.dds_freq_Number);
        mDdsFreqEdit.SetValueRange(freq_min, freq_max);
        mDdsFreqEdit.SetValue(freq);
        mDdsFreqEdit.setOnEditSpinnerActionListener(editSpinnerActionListener);

        //
        TextView textView = findViewById(R.id.text_info);
        textView.setMovementMethod(ScrollingMovementMethod.getInstance());

        //
        EditSpinnerVolt mRangeMinEdit = findViewById(R.id.osc_range_minmv);
        mRangeMinEdit.setOnEditSpinnerActionListener(editSpinnerActionListener);

        EditSpinnerVolt mRangeMaxEdit = findViewById(R.id.osc_range_maxmv);
        mRangeMaxEdit.setOnEditSpinnerActionListener(editSpinnerActionListener);

        Spinner mSampleSpinner = findViewById(R.id.osc_sample_spinner);
        SampleSpinnerInteractionListener mTimeSpinnerInteractionListener = new SampleSpinnerInteractionListener();
        mSampleSpinner.setOnTouchListener(mTimeSpinnerInteractionListener);
        mSampleSpinner.setOnItemSelectedListener(mTimeSpinnerInteractionListener);

        EditSpinnerKb mLengthEdit = findViewById(R.id.osc_capture_length);
        mLengthEdit.setOnEditSpinnerActionListener(editSpinnerActionListener);

        CheckBox mOscCheck = findViewById(R.id.osc_capture_check);
        mOscCheck.setOnCheckedChangeListener(ddsCheckedChangeListener);

        //Trigger
        String[] mTriggerModeStrings = mResources.getStringArray(R.array.Trigger_Mode_Strings);
        mTriggerModeAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, mTriggerModeStrings);
        mTriggerModeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);     //设置下拉列表框的下拉选项样式
        Spinner mTriggerModeSpinner = findViewById(R.id.trigger_mode_spinner);
        mTriggerModeSpinner.setAdapter(mTriggerModeAdapter);
        mTriggerModeSpinner.setSelection(0);
        mTriggerModeSpinner.setOnItemSelectedListener(spinnerSelectedListener);

        String[] mTriggerSourceStrings = mResources.getStringArray(R.array.Trigger_Source_Strings);
        ArrayAdapter<String> mTriggerSourceAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, mTriggerSourceStrings);
        mTriggerSourceAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);     //设置下拉列表框的下拉选项样式
        Spinner mTriggerSourceSpinner = findViewById(R.id.trigger_source_spinner);
        mTriggerSourceSpinner.setAdapter(mTriggerSourceAdapter);
        mTriggerSourceSpinner.setSelection(0);
        mTriggerSourceSpinner.setOnItemSelectedListener(spinnerSelectedListener);

        String[] mTriggerStyleStrings = mResources.getStringArray(R.array.Trigger_Style_Strings);
        ArrayAdapter<String> mTriggerStyleAdapter= new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, mTriggerStyleStrings);
        mTriggerStyleAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);     //设置下拉列表框的下拉选项样式
        Spinner mTriggerStyleSpinner = findViewById(R.id.trigger_style_spinner);
        mTriggerStyleSpinner.setAdapter(mTriggerStyleAdapter);
        mTriggerStyleSpinner.setSelection(1);
        mTriggerStyleSpinner.setOnItemSelectedListener(spinnerSelectedListener);

        EditSpinnerVolt mTriggerLevelEdit = findViewById(R.id.trigger_Level_Number);
        mTriggerLevelEdit.SetValueRange(-100000, 100000);//mV
        mTriggerLevelEdit.SetValue(0);
        mTriggerLevelEdit.setOnEditSpinnerActionListener(editSpinnerActionListener);

        String[] mTriggerSensitivityStrings = mResources.getStringArray(R.array.Trigger_Sensitivity_Strings);
        ArrayAdapter<String> mTriggerSensitivityAdapter= new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, mTriggerSensitivityStrings);
        mTriggerSensitivityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);     //设置下拉列表框的下拉选项样式
        Spinner mTriggerSensitivitySpinner = findViewById(R.id.trigger_Sensitivity_spinner);
        mTriggerSensitivitySpinner.setAdapter(mTriggerSensitivityAdapter);
        mTriggerSensitivitySpinner.setSelection(1);
        mTriggerSensitivitySpinner.setOnItemSelectedListener(spinnerSelectedListener);

        EditTextRange mTriggerFrontPercentEditText = (EditTextRange)findViewById(R.id.trigger_Front_Percent_editTextNumber);
        mTriggerFrontPercentEditText.SetValueRange(1,99,0);
        mTriggerFrontPercentEditText.setValue(50);
        mTriggerFrontPercentEditText.setOnEditTextRangeActionListener(editorRangeActionListener);


        AddInfo(R.string.PleaseConnectUsb);

        usbManger = new UsbDevMng(this, this);
        usbManger.intiDetect(this);
    }

    public void ShowToastNoUi(int id, int duration) {
        Resources mResources = getResources();
        ShowToastNoUi(mResources.getString(id), duration);
    }
    public void ShowToastNoUi(String msg, int duration) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //Toast toast =
                Toast.makeText(getApplicationContext(), msg, duration).show();
                //toast.setGravity(Gravity.CENTER, 0, 0);
                //toast.show();
            }
        });
    }

    public void UsbDevDetectCallback(UsbDevMng.DEVICE_DETECT_STATE state, boolean success, BasicUsbDev dev) {

        //Log.d(TAG,"isUiThread " + (Thread.currentThread() == Looper.getMainLooper().getThread()));
        Log.d(TAG, state.toString() + " " + success + " " + (dev != null ? dev.GetDesId() : ""));

        if (state == UsbDevMng.DEVICE_DETECT_STATE.DEVICE_ADD) {
            assert dev != null;
            cardWave = OscDdsFactory.CreateSbqCardWave(this, dev);
            if (dev.IsSupportHsf()) {
                 assert (ddsWave == null);
                ddsWave = OscDdsFactory.CreateDDSWave(dev);
            }

            try {
                Thread.sleep(1000);

                //更新UI
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        usbDevDetectAdd();
                    }
                });

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        else if (state == UsbDevMng.DEVICE_DETECT_STATE.DEVICE_REMOVE) {
            //mOscWave = null;
            //mDdsWave = null;

            //更新UI
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    usbDevDetectRemove();
                }
            });
        }
        else if (state == UsbDevMng.DEVICE_DETECT_STATE.NEED_PERMISSION) {
            ShowToastNoUi(R.string.The_Device_Need_Permission, Toast.LENGTH_SHORT);
        }
    }

    private void AddInfo(String str)
    {
        mUsbInfos += str;
        TextView textView = findViewById(R.id.text_info);
        textView.setText(mUsbInfos);
    }

    private void AddInfo(int id)
    {
        Resources mResources = getResources();
        AddInfo(mResources.getString(id) + "\n");
    }

    void UpdateOscCtrls()
    {
        m_osc_range_minv = cardWave.GetRangeMinV();
        m_osc_range_maxv = cardWave.GetRangeMaxV();

        //采集范围
        EditSpinnerVolt mRangeMinEdit = findViewById(R.id.osc_range_minmv);
        mRangeMinEdit.SetValueRange(m_osc_range_minv * 1000, m_osc_range_maxv * 1000);
        mRangeMinEdit.SetValue(m_osc_range_minv * 1000);

        EditSpinnerVolt mRangeMaxEdit = findViewById(R.id.osc_range_maxmv);
        mRangeMaxEdit.SetValueRange(m_osc_range_minv * 1000, m_osc_range_maxv * 1000);
        mRangeMaxEdit.SetValue(m_osc_range_maxv * 1000);

        cardWave.SetRangeV((byte)0, m_osc_range_minv, m_osc_range_maxv);
        cardWave.SetRangeV((byte)1, m_osc_range_minv, m_osc_range_maxv);

        //采样率
        int sampleNum = cardWave.GetSamplesNum();
        String[] sampleStrings = new String[sampleNum];
        int samples[] = new int[sampleNum];
        if(cardWave.GetSamples(samples, sampleNum)>0)
        {
            for(int k=0; k<sampleNum; k++)
            {
                sampleStrings[k] = String.format("%d",  samples[k]);
                //mUsbInfos += sampleStrings[k] + "\n";
            }
        }

        ArrayAdapter<String> mSampleAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, sampleStrings);
        mSampleAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);     //设置下拉列表框的下拉选项样式
        Spinner mSampleSpinner = findViewById(R.id.osc_sample_spinner);
        mSampleSpinner.setAdapter(mSampleAdapter);
        //使用最大采样率
        mSampleSpinner.setSelection(sampleNum-1, false);
        cardWave.SetSample(samples[sampleNum-1]);

        //触发
        //注意DANCI无效，不要设置为单次；设置为
        //LIANXU模式，Capture通知数据采集完成，不再调用Capture就是单次模式
        cardWave.SetTriggerMode(ComDefs.TRIGGER_MODE.AUTO);
        Spinner mTriggerModeSpinner = findViewById(R.id.trigger_mode_spinner);
        mTriggerModeSpinner.setSelection(0);

        Spinner mTriggerSensitivitySpinner = findViewById(R.id.trigger_Sensitivity_spinner);
        mTriggerSensitivitySpinner.setSelection(1);
        //注意：先设置采集范围再设置触发电平，采集范围影响触发电平的值
        cardWave.SetTriggerSenseDiv(0.2, (m_osc_range_maxv - m_osc_range_minv)/10.0); //V //20000/5=4000*0.2=800mV

        EditSpinnerVolt mTriggerLevelEdit = findViewById(R.id.trigger_Level_Number);
        mTriggerLevelEdit.SetValue(1000);
        cardWave.SetTriggerLevel(1.0); //V

        Spinner mTriggerStyleSpinner = findViewById(R.id.trigger_style_spinner);
        mTriggerStyleSpinner.setSelection(1);
        cardWave.SetTriggerStyle(ComDefs.TRIGGER_STYLE.RISE_EDGE);

        Spinner mTriggerSourceSpinner = findViewById(R.id.trigger_source_spinner);
        mTriggerSourceSpinner.setSelection(0);
        cardWave.SetTriggerSource(ComDefs.TRIGGER_SOURCE.CH1);

        EditTextRange mTriggerFrontPercentEditText = (EditTextRange)findViewById(R.id.trigger_Front_Percent_editTextNumber);
        mTriggerFrontPercentEditText.setValue(50);
        cardWave.SetTriggerFrontPercent(50);

        //内存分配:低版本安卓分配太大内存失败，使用1MB测试    32MB/16==2MB采集
        //The lower version of Android failed to allocate too much memory, and it was collected with 1MB test 32MB/16==2MB
        m_max_capture_length = m_capture_length = cardWave.GetHardMemoryDepth() / 16;
        mUsbInfos += String.format("Memory alloc %d\n",  m_capture_length);
        m_buffer = new double[m_max_capture_length];

        EditSpinnerKb mCaptureLength = findViewById(R.id.osc_capture_length);
        mCaptureLength.SetValue(m_capture_length/1024);

        //
        CheckBox mOscCheck = findViewById(R.id.osc_capture_check);
        mOscCheck.setChecked(false);
    }

    @SuppressLint("DefaultLocale")
    private void usbDevDetectAdd(){
        if(cardWave!=null) {
            Resources mResources = getResources();

            //KeepScreenOn(true);
            String str = mResources.getString(R.string.The_Device_Add) + " " + cardWave.GetDesId() + "\n";
            //ShowToastNoUi(str, Toast.LENGTH_SHORT);
            AddInfo(R.string.The_Device_Add);

            //参数都填false
            cardWave.Start(false, false);

            UpdateOscCtrls();

            TextView textView = findViewById(R.id.text_info);
            textView.setText(mUsbInfos);
         }
    }

    private void usbDevDetectRemove() {
        //KeepScreenOn(false);
        AddInfo(R.string.The_Device_Remove);
        //ShowToastNoUi(R.string.The_Device_Remove, Toast.LENGTH_SHORT);

        if(cardWave!=null)
            cardWave.Stop();
    }

    public boolean WaveReceiveCallBack(boolean success, int length){

        if(success) {
            //更新UI
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    WaveReceive(length);
                }
            });
        }
        return true;
    }

    void NextCapture()
    {
        if(cardWave!=null)
        {
            //采集
            //m_capture_length 以KB为单位 1==1024 10=10240
            //ch1==0x01 ch2==0x02 ch1+ch2==0x03
            m_read_length = cardWave.Capture(m_capture_length / 1024, (short)0x03, (byte) 0);
            m_read_length *= 1024;  //转换成长度
        }
    }

    int m_count = 0;
    @SuppressLint("DefaultLocale")
    void WaveReceive(int length)
    {
        //AddInfo(String.format("WaveReceive %d\n", length));
        if(cardWave!=null)
        {
            Resources mResources = getResources();
            int m_length = cardWave.ReadVoltageDatas((byte)0, m_buffer, m_read_length);
            AddInfo(String.format("%s %d\n", mResources.getString(R.string.CaptureCompleteReadDatas), m_length));

            double min, max;
            min = max = m_buffer[0];
            for (int i = 0; i < m_length; i++)
            {
                min = Math.min(min, m_buffer[i]);
                max = Math.max(max, m_buffer[i]);
            }

            if(++m_count > 15)
            {
                m_count = 0;
                mUsbInfos = "";
            }
            String tmp = String.format("min = %.3f max = %.3f\n",min,max);
            AddInfo(tmp);

            //采集
            if(m_osc_capturing)
                NextCapture();
        }
    }

    public static ComDefs.BOXING_STYLE name2boxing(Context context, String name)
    {
        ComDefs.BOXING_STYLE style = ComDefs.BOXING_STYLE.W_SINE;
        Resources mResources = context.getResources();
        if(name.equals(mResources.getString(R.string.Sine)))
            style = ComDefs.BOXING_STYLE.W_SINE;
        else if(name.equals(mResources.getString(R.string.Rectangle)))
            style = ComDefs.BOXING_STYLE.W_SQUARE;
        else if(name.equals(mResources.getString(R.string.Ramp)))
            style = ComDefs.BOXING_STYLE.W_RAMP;
        else if(name.equals(mResources.getString(R.string.Pulse)))
            style = ComDefs.BOXING_STYLE.W_PULSE;
        else if(name.equals(mResources.getString(R.string.Pulse)))
            style = ComDefs.BOXING_STYLE.W_PULSE;
        else if(name.equals(mResources.getString(R.string.White_Noise)))
            style = ComDefs.BOXING_STYLE.W_NOISE;
        else if(name.equals(mResources.getString(R.string.DC)))
            style = ComDefs.BOXING_STYLE.W_DC;
        else if(name.equals(mResources.getString(R.string.Arb)))
            style = ComDefs.BOXING_STYLE.W_ARB;
        return style;
    }

    public static ComDefs.TRIGGER_MODE name2chufa_mode(String name, Resources res)
    {
        ComDefs.TRIGGER_MODE mode = ComDefs.TRIGGER_MODE.AUTO;
        if(res.getString(R.string.Auto).equals(name))
            mode = ComDefs.TRIGGER_MODE.AUTO;
        else if(res.getString(R.string.Normal).equals(name))
            mode = ComDefs.TRIGGER_MODE.LIANXU;
        return mode;
    }

    public static ComDefs.TRIGGER_STYLE name2chufa_style(String name, Resources res)
    {
        ComDefs.TRIGGER_STYLE style = ComDefs.TRIGGER_STYLE.NONE;
        String[] mTriggerStyleStrings = res.getStringArray(R.array.Trigger_Style_Strings);
        if(mTriggerStyleStrings[1].equals(name))
            style = ComDefs.TRIGGER_STYLE.RISE_EDGE;
        else if(mTriggerStyleStrings[2].equals(name))
            style = ComDefs.TRIGGER_STYLE.FALL_EDGE;
        else if(mTriggerStyleStrings[3].equals(name))
            style = ComDefs.TRIGGER_STYLE.EDGE;
        else if(mTriggerStyleStrings[4].equals(name))
            style = ComDefs.TRIGGER_STYLE.PULSE_P_MORE;
        else if(mTriggerStyleStrings[5].equals(name))
            style = ComDefs.TRIGGER_STYLE.PULSE_P_LESS;
        else if(mTriggerStyleStrings[6].equals(name))
            style = ComDefs.TRIGGER_STYLE.PULSE_P;
        else if(mTriggerStyleStrings[7].equals(name))
            style = ComDefs.TRIGGER_STYLE.PULSE_N_MORE;
        else if(mTriggerStyleStrings[8].equals(name))
            style = ComDefs.TRIGGER_STYLE.PULSE_N_LESS;
        else if(mTriggerStyleStrings[9].equals(name))
            style = ComDefs.TRIGGER_STYLE.PULSE_N;
        return style;
    }

    void updateDdsCtrls() {
        ComDefs.DDS_OUT_MODE mode = null;
        ComDefs.BOXING_STYLE bx_style = null;
        if(ddsWave!=null)
            mode = ddsWave.GetOutMode(0);
        else{
            Spinner modeSpiner = (Spinner) findViewById(R.id.dds_mode_spinner);
            mode = ComDefs.DDS_OUT_MODE.valueOf(modeSpiner.getSelectedItemPosition());
        }

        if(ddsWave!=null)
            bx_style = ddsWave.GetBoxing(0);
        else {
            Spinner wave = (Spinner) findViewById(R.id.dds_wave_spinner);
            bx_style = name2boxing(getApplicationContext(), wave.getItemAtPosition(wave.getSelectedItemPosition()).toString());
        }

        int amp = ddsWave!=null? ddsWave.GetCurBoxingAmplitudeMv(bx_style) : 1000;
        int bias_min = ddsWave!=null? ddsWave.GetCurBoxingBiasMvMin(bx_style) : -1000;
        int bias_max = ddsWave!=null? ddsWave.GetCurBoxingBiasMvMax(bx_style) : 1000;
        int m_dds_vpp = ddsWave!=null? ddsWave.GetAmplitudeMv(0) : 1000;
        int m_dds_bias = ddsWave!=null? ddsWave.GetBiasMv(0) : 0;

        //幅度
        TextView vppView = (TextView) findViewById(R.id.dds_vpp_textView);
        vppView.setVisibility(bx_style != ComDefs.BOXING_STYLE.W_DC? View.VISIBLE : View.GONE);

        EditSpinnerVolt mDdsVppEdit = (EditSpinnerVolt) findViewById(R.id.dds_vpp_Number);
        mDdsVppEdit.setVisibility(bx_style != ComDefs.BOXING_STYLE.W_DC? View.VISIBLE : View.GONE);
        mDdsVppEdit.SetValueRange(0, amp);
        mDdsVppEdit.SetValue(m_dds_vpp);

        //偏置
        EditSpinnerVolt mDdsBiasEdit = findViewById(R.id.dds_bias_Number);
        mDdsBiasEdit.SetValueRange(bias_min, bias_max);
        mDdsBiasEdit.SetValue(m_dds_bias);

        //频率
        double freq_min = ddsWave!=null? ddsWave.GetCurBoxingFreqStep(bx_style) : 1;
        double freq_max = ddsWave!=null? ddsWave.GetCurBoxingMaxFreq(bx_style) : 1000000;
        double freq = ddsWave!=null? ddsWave.GetPinlv(0) : 1000;
        TextView freqView = (TextView) findViewById(R.id.dds_freq_textView);
        int visibility = ((mode == ComDefs.DDS_OUT_MODE.SWEEP) ||
                (bx_style == ComDefs.BOXING_STYLE.W_DC) ||
                (bx_style == ComDefs.BOXING_STYLE.W_NOISE)) ? View.GONE : View.VISIBLE;
        freqView.setVisibility(visibility);

        EditSpinnerFreq freqNumber = (EditSpinnerFreq) findViewById(R.id.dds_freq_Number);
        freqNumber.SetValueRange(freq_min, freq_max);
        freqNumber.SetValue(freq);
        freqNumber.setVisibility(visibility);
    }

    Spinner.OnItemSelectedListener spinnerSelectedListener = new Spinner.OnItemSelectedListener(){

        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            Log.d(TAG,"ddsSpinnerSelectedListener " + parent.getItemAtPosition(position).toString());

            int parentId = parent.getId();
            if (parentId == R.id.dds_mode_spinner) {
                if (ddsWave != null){
                    ddsWave.SetOutMode(0, ComDefs.DDS_OUT_MODE.valueOf(position));
                }
                updateDdsCtrls();
            } else if (parentId == R.id.dds_wave_spinner) {
                if (ddsWave != null){
                    ComDefs.BOXING_STYLE bx_style = name2boxing(getApplicationContext(), parent.getItemAtPosition(position).toString());
                    ddsWave.SetBoxing(0, bx_style);
                }
                updateDdsCtrls();
            } else if (parentId == R.id.trigger_mode_spinner) {
                if (cardWave != null)
                    cardWave.SetTriggerMode(name2chufa_mode(parent.getItemAtPosition(position).toString(), getResources()));
            } else if (parentId == R.id.trigger_source_spinner) {
                if (cardWave != null) {
                    cardWave.SetTriggerSource(ComDefs.TRIGGER_SOURCE.name2chufa_source(parent.getItemAtPosition(position).toString(), getResources()));
                    //SetTriggerEditRange();
                }
            } else if (parentId == R.id.trigger_style_spinner) {
                if (cardWave != null) {
                    cardWave.SetTriggerStyle(name2chufa_style(parent.getItemAtPosition(position).toString(), getResources()));
                    //TriggerStyleChanged();
                }
            } else if (parentId == R.id.trigger_Sensitivity_spinner) {
                double m_trigger_sense_div = Double.parseDouble(parent.getItemAtPosition(position).toString());
                Log.d(TAG,"m_trigger_sense_div "+m_trigger_sense_div);
                if (cardWave != null)
                    cardWave.SetTriggerSenseDiv(m_trigger_sense_div, (m_osc_range_maxv - m_osc_range_minv)/10.0);
            }
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {
            //parent.setVisibility(View.VISIBLE);
        }
    };

    CompoundButton.OnCheckedChangeListener ddsCheckedChangeListener = new CompoundButton.OnCheckedChangeListener(){
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            //Log.d(TAG, buttonView.getText().toString() +" "+buttonView.getId() + " check " + isChecked);

            int id = buttonView.getId();
            if (id == R.id.dds_switch) {
                if (ddsWave != null) {
                    if (isChecked)
                        ddsWave.ChannelStart(0);
                    else
                        ddsWave.ChannelStop(0);
                }
            }
            else if(id == R.id.osc_capture_check)
            {
                if (cardWave != null) {
                    m_osc_capturing = isChecked;
                    if (m_osc_capturing)
                        NextCapture(); //采集
                }
            }
        }
    };

    EditSpinner.OnEditSpinnerActionListener editSpinnerActionListener = new EditSpinner.OnEditSpinnerActionListener(){
        @Override
        public void onEditSpinnerAction(int id, double value) {
            if (id == R.id.dds_vpp_Number) {
                Log.d(TAG, "dds_vpp_Number " + value);
                if (ddsWave != null)
                    ddsWave.SetAmplitudeMv(0, (int)value);
            }
            else if (id == R.id.dds_bias_Number) {
                Log.d(TAG, "dds_bias_Number " + value);
                if (ddsWave != null)
                    ddsWave.SetBiasMv(0, (int)value);
            }
            else if (id == R.id.dds_freq_Number) {
                Log.d(TAG, "dds_freq_Number " + value);
                if (ddsWave != null)
                    ddsWave.SetPinlv(0, value);
            }
            else if (id == R.id.osc_range_minmv) {
                Log.d(TAG, "osc_range_minmv " + value);
                m_osc_range_minv = value/1000.0;
                if (cardWave != null)
                {
                    cardWave.SetRangeV((byte)0, m_osc_range_minv, m_osc_range_maxv);
                    cardWave.SetRangeV((byte)1, m_osc_range_minv, m_osc_range_maxv);
                }
            }
            else if (id == R.id.osc_range_maxmv) {
                Log.d(TAG, "osc_range_maxmv " + value);
                m_osc_range_maxv = value/1000.0;
                if (cardWave != null)
                {
                    cardWave.SetRangeV((byte)0, m_osc_range_minv, m_osc_range_maxv);
                    cardWave.SetRangeV((byte)1, m_osc_range_minv, m_osc_range_maxv);
                }
            }
            else if (id == R.id.osc_capture_length) {
                Log.d(TAG, "osc_capture_length " + value);
                if(value * 1024 <= m_max_capture_length)
                    m_capture_length = (int)value * 1024;
                else
                    Log.e(TAG, "osc_capture_length > m_max_capture_length");
            }
            else if (id == R.id.trigger_Level_Number) {
                Log.d(TAG, "trigger_Level_Number " + value);
                if (cardWave != null)
                    cardWave.SetTriggerLevel(value/1000.0);
            }
        }
    };

    class SampleSpinnerInteractionListener implements AdapterView.OnItemSelectedListener, View.OnTouchListener {
        public
        boolean userSelect = false;

        @Override
        public boolean onTouch(android.view.View v, MotionEvent event) {
            userSelect = true;
            return false;
        }

        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
            if (userSelect) {
                userSelect = false;
                Log.d(TAG,"SampleSpinnerInteractionListener " + parent.getItemAtPosition(pos).toString());
                if(cardWave!=null)
                {
                    int sampleNum = cardWave.GetSamplesNum();
                    int samples[] = new int[sampleNum];
                    if(cardWave.GetSamples(samples, sampleNum)>0)
                        cardWave.SetSample(samples[pos]);
                }
            }
            //else
            //    parent.setSelection();
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {
        }
    }

    EditTextRange.OnEditTextRangeActionListener editorRangeActionListener = new EditTextRange.OnEditTextRangeActionListener(){

        @Override
        public void onEditTextRangeAction(int id, double value) {
            if (id == R.id.trigger_Front_Percent_editTextNumber) {
                Log.d(TAG, "trigger_Front_Percent_editTextNumber " + value);
                if (cardWave != null)
                    cardWave.SetTriggerFrontPercent((int) value);
            }
        }
    };
}