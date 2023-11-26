package com.vimu.aardemo;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import androidx.annotation.Nullable;

public class EditSpinnerFreq extends EditSpinner {

    ArrayAdapter<String> spinnerAdapter;
    String[] spinnerStrings = new String[]{"Hz","KHz","MHz"};

    public EditSpinnerFreq(Context context) {
        super(context);
        init(context);
    }

    public EditSpinnerFreq(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public EditSpinnerFreq(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    public EditSpinnerFreq(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context);
    }

    private void init(Context context){
        //获得对子控件的引用
        //EditText editText = (EditText) findViewById(R.id.edit_spinner_editTextNumberDecimal);

        spinnerAdapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, spinnerStrings);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        Spinner spinner = (Spinner) findViewById(R.id.edit_spinner_spinner);
        spinner.setAdapter(spinnerAdapter);
        spinner.setSelection(0);
        //spinner.setOnItemSelectedListener(spinSelectedListener);
    }

    protected double converSpinnerToFactor(int position){
        double value = 1;
        if(position==1)
            value*=1000;
        else  if(position==1)
            value*=1000000;
        return value;
    }
}
