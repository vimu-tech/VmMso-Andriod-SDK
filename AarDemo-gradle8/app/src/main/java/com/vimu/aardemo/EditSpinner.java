package com.vimu.aardemo;

import android.content.Context;
import android.text.InputFilter;
import android.text.Spanned;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.annotation.Nullable;
import java.util.Locale;

abstract public class EditSpinner extends LinearLayout {

    private static final String TAG = "EditSpinner";
    private int mPosition = 0;
    private double mTotalValue=0;
    private double mTotalValueMin=Integer.MIN_VALUE, mTotalValueMax=Integer.MAX_VALUE;
    private EditText mEditText;
    private Spinner mSpinner;
    //private ColorStateList mTextColorOld;

    OnEditSpinnerActionListener mEditSpinnerActionListener = null;
    public interface OnEditSpinnerActionListener {
        void onEditSpinnerAction(int id, double value);
    }

    public EditSpinner(Context context) {
        super(context);
        init(context);
    }

    public EditSpinner(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public EditSpinner(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    public EditSpinner(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context);
    }

    private void init(Context context) {
        //使用布局资源填充视图
        LayoutInflater layoutInflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if(layoutInflater!=null) {
            View view = layoutInflater.inflate(R.layout.edit_spinner, this, true);
            //获得对子控件的引用
            mEditText = (EditText) view.findViewById(R.id.edit_spinner_editTextNumberDecimal);
            mEditText.setOnEditorActionListener(editorActionListener);
            //mTextColorOld = mEditText.getTextColors();

            mSpinner = (Spinner) view.findViewById(R.id.edit_spinner_spinner);
            mSpinner.setOnItemSelectedListener(spinSelectedListener);
        }
    }

    public void setEnabled(boolean enabled) {
        mEditText.setEnabled(enabled);
        mSpinner.setEnabled(enabled);
    }

    Spinner.OnItemSelectedListener spinSelectedListener = new Spinner.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            mPosition = position;
            SetValue(mTotalValue);
            //Log.d(TAG, "onItemSelected " + parent.getItemAtPosition(position).toString()+" "+mTotalValue);
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {
            //parent.setVisibility(View.VISIBLE);
        }
    };

    EditText.OnEditorActionListener editorActionListener = new EditText.OnEditorActionListener(){

        @Override
        public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
            String str = textView.getText().toString();

            //检验范围 超范围限制
            double newValue = Double.parseDouble(str)*converSpinnerToFactor(mPosition);
            if(newValue<mTotalValueMin)
                newValue = mTotalValueMin;
            else if(newValue>mTotalValueMax)
                newValue = mTotalValueMax;
            SetValue(str.isEmpty()? mTotalValue:newValue);

            Log.d(TAG, "onEditorAction " + mTotalValue);
            if(mEditSpinnerActionListener!=null)
                mEditSpinnerActionListener.onEditSpinnerAction(getId(), mTotalValue);
            return false;
        }
    };

    public void setOnEditSpinnerActionListener(OnEditSpinnerActionListener listener) {
        this.mEditSpinnerActionListener = listener;
    }

    protected abstract double converSpinnerToFactor(int position);

    public double getValue(){
        return mTotalValue;
    }

    public void SetValue(double value){
        mTotalValue = value;

        SetValueRange(mTotalValueMin, mTotalValueMax);

        double factor = converSpinnerToFactor(mPosition);
        double displayValue = mTotalValue/factor;
        String text = String.format(Locale.getDefault(),"%.3f",displayValue);
        //Log.d(TAG, "SetValue " + mTotalValue + " text " + text);
        mEditText.setText(text);
    }

    public void SetValueRange(double min, double max) {
        mTotalValueMin = min;
        mTotalValueMax = max;
        double factor = converSpinnerToFactor(mPosition);
        //InputFilter[] filter = mEditText.getFilters();
        /*if(filter.length>0){
            ((InputFilterMinMax)(filter[0])).ChangeMinMax(mTotalValueMin / factor, mTotalValueMax / factor);
        }*/
        //else {
            mEditText.setFilters(new InputFilter[]{new InputFilterMinMax(mTotalValueMin / factor, mTotalValueMax / factor)});
        //}
    }

    //--------------------------------------------------------InputFilter-----------------------------------------------------------
    public static class InputFilterMinMax implements InputFilter {
        private double min, max;

        public InputFilterMinMax(double min, double max) {
            this.min = min;
            this.max = max;
        }

        public void ChangeMinMax(double min, double max) {
            this.min = min;
            this.max = max;
        }

        /*public InputFilterMinMax(String min, String max) {
            this.min = Double.parseDouble(min);
            this.max = Double.parseDouble(max);
        }*/

        //edit.setFilters(new InputFilter[]{new InputFilterMinMax(0,100)});
        @Override
        public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dStart, int dEnd) {
            //Log.d(TAG, "filter source " + source.toString() + " start " + start + " end " + end +" dStart "+dStart + " dEnd "+dEnd + " dest "+dest);
            //负号
            if(source.equals("-")){
                if((dStart == 0)&&(dEnd == 0)){
                    return null;
                }
                else
                    return "";
            }

            //限制小数点位数
            if (source.equals(".") && dest.toString().length() == 0) {
                //Log.d(TAG, "filter " + "0.");
                return "0.";
            }
            if (dest.toString().contains(".")) {
                int dotIndex = dest.toString().indexOf(".");
                int mLength = dest.toString().substring(dotIndex).length();
                //添加了一个条件判断：输入光标是在小数点的后面
                if ((dStart > dotIndex) && (mLength == 4)) {
                    //Log.d(TAG, "filter " + "");
                    return "";
                }
            }
            //限制大小
            StringBuffer strBuf = new StringBuffer(dest.toString());
            strBuf.insert(dStart, source);
            //Log.d(TAG, "StringBuffer " + strBuf);
            try {
                double input = Double.parseDouble(strBuf.toString());
                if (isInRange(min, max, input)) {
                    //XLog.d(TAG + " 0 ...filter");
                    return null;
                } else {
                    //XLog.d(TAG + " 1 ...filter");
                    return "";
                }
            } catch (NumberFormatException e) {
                //XLog.d(TAG + "e ...filter");
                return "";
            }
        }

        private boolean isInRange(double a, double b, double c) {
            //Log.d(TAG, "isInRange " + a +"b "+b + "c " +c);
            return b > a ? (c >= a && c <= b) : (c >= b && c <= a);
        }
    }
}
