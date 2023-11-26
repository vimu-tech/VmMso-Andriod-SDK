package com.vimu.aardemo;

import android.content.Context;
import android.text.InputFilter;
import android.text.InputType;
import android.text.Spanned;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.widget.TextView;

import androidx.appcompat.widget.AppCompatEditText;

import java.util.Locale;

public class EditTextRange extends AppCompatEditText {
    private static final String TAG = "EditTextRange";

    private double mTotalValue=0;
    private double mTotalValueMin=Integer.MIN_VALUE, mTotalValueMax=Integer.MAX_VALUE;
    private int mDecimalPointNum=0;

    EditTextRange.OnEditTextRangeActionListener mEditTextRangeActionListener = null;
    public interface OnEditTextRangeActionListener {
        public void onEditTextRangeAction(int id, double value);
    }

    public EditTextRange(Context context) {
        super(context);
        init(context);
    }

    public EditTextRange(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public EditTextRange(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        setOnEditorActionListener(editorActionListener);
        //输入类型为数字文本
        //editText.setInputType(InputType.TYPE_CLASS_NUMBER);
        //输入类型为小数数字，允许十进制小数点提供分数值。
        //editText.setInputType(InputType.TYPE_NUMBER_FLAG_DECIMAL);
        //输入类型为数字是带符号的，允许在开头带正号或者负号
        //editText.setInputType(InputType.TYPE_NUMBER_FLAG_SIGNED);
        //输入类型为{@link#TYPE_CLASS_NUMBER}的缺省变化值：为纯普通数字文本
        //editText.setInputType(InputType.TYPE_NUMBER_VARIATION_NORMAL);
        //输入类型为{@link#TYPE_CLASS_NUMBER}的缺省变化值：为数字密码
        //editText.setInputType(InputType.TYPE_NUMBER_VARIATION_PASSWORD);

        setInputType(InputType.TYPE_CLASS_NUMBER);
    }

    AppCompatEditText.OnEditorActionListener editorActionListener = new AppCompatEditText.OnEditorActionListener(){

        @Override
        public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
            String str = textView.getText().toString();
            mTotalValue = str.isEmpty()? 0:Double.parseDouble(str);
            //Log.i(TAG, "onEditorAction " + str);
            if(mEditTextRangeActionListener!=null)
                mEditTextRangeActionListener.onEditTextRangeAction(getId(), mTotalValue);
            return false;
        }
    };

    public double getValue(){
        return mTotalValue;
    }

    public void setValue(double value){
        mTotalValue = value;

        SetValueRange(mTotalValueMin, mTotalValueMax, mDecimalPointNum);

        String text = String.format(Locale.getDefault(), (mDecimalPointNum>0)? "%.3f":"%.0f", mTotalValue);
        //Log.d(TAG, "SetValue " + mTotalValue + " text " + text);
        setText(text);
    }

    public void SetValueRange(double minv, double maxv, int decimalPointNum) {
        if(decimalPointNum>0)
            setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_SIGNED);
        else
            setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_VARIATION_NORMAL);

        mTotalValueMin = minv;
        mTotalValueMax = maxv;
        mDecimalPointNum = decimalPointNum;
        //InputFilter[] filter = getFilters();
        //if(filter.length>0)
        //    ((InputFilterMinMax)(filter[0])).ChangeMinMax(mTotalValueMin, mTotalValueMax, mDecimalPointNum);
        //else
            setFilters(new InputFilter[]{new InputFilterMinMax(mTotalValueMin, mTotalValueMax, mDecimalPointNum)});
    }

    public void setOnEditTextRangeActionListener(EditTextRange.OnEditTextRangeActionListener listener) {
        this.mEditTextRangeActionListener = listener;
    }

    //--------------------------------------------------------InputFilter-----------------------------------------------------------
    public class InputFilterMinMax implements InputFilter {
        private double min, max;
        private int mDecimalPointNum=0;

        public InputFilterMinMax(double min, double max, int num) {
            this.min = min;
            this.max = max;
            this.mDecimalPointNum = num;
        }

        public void ChangeMinMax(double min, double max, int num) {
            this.min = min;
            this.max = max;
            this.mDecimalPointNum = num;
        }

        //edit.setFilters(new InputFilter[]{new InputFilterMinMax(0,100)});
        @Override
        public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
            //Log.i(TAG, "filter source " + source.toString() + " start " + start + " end " + end +" dstart "+dstart + " dend "+dend + " dest "+dest);
            //负号
            if(source.equals("-")){
                if((dstart == 0)&&(dend == 0)){
                    return null;
                }
                else
                    return "";
            }

            //限制小数点位数
            if (source.equals(".") && dest.toString().length() == 0) {
                //Log.i(TAG, "filter " + "0.");
                return "0.";
            }
            if (dest.toString().contains(".")) {
                int dotIndex = dest.toString().indexOf(".");
                int mlength = dest.toString().substring(dotIndex).length();
                //添加了一个条件判断：输入光标是在小数点的后面
                if ((dstart > dotIndex) && (mlength == mDecimalPointNum+1)) {
                    //Log.i(TAG, "filter " + "");
                    return "";
                }
            }
            //限制大小
            StringBuffer strbuf = new StringBuffer(dest.toString());
            strbuf.insert(dstart, source.toString());
            //Log.i(TAG, "StringBuffer " + strbuf);
            double input = Double.parseDouble(strbuf.toString());
            if (isInRange(min, max, input)) {
                //Log.i(TAG, "filter " + input +" null");
                return null;
            }
            //Log.i(TAG, "filter " + "end");
            return "";
        }

        private boolean isInRange(double a, double b, double c) {
            return b > a ? (c >= a && c <= b) : (c >= b && c <= a);
        }
    }
}
