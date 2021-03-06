package com.example.mobile_dev

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.lifecycle.MutableLiveData
import java.text.DecimalFormat

class MainActivity : AppCompatActivity() {
    private var srcWrapper = MutableLiveData<Int>();
    private var destWrapper = MutableLiveData<Int>();

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    override fun onStart() {
        super.onStart()
        Toast.makeText(this.baseContext, R.string.welcome_TT, Toast.LENGTH_SHORT).show()

        findViewById<Spinner>(R.id.currency_src_DD).onItemSelectedListener =
            getOnItemSelectedListener(srcWrapper)
        findViewById<Spinner>(R.id.currency_dest_DD).onItemSelectedListener =
            getOnItemSelectedListener(destWrapper)

        findViewById<Button>(R.id.convert_BT).setOnClickListener {
            try {
                val srcAmount = findViewById<EditText>(R.id.input_ET).text.toString().toDouble()
                val rates = resources.getStringArray(R.array.currencies_rates)
                val kSrc = rates[srcWrapper.value!!].toDouble()
                val kDest = rates[destWrapper.value!!].toDouble()
                val destAmount = (srcAmount / kSrc * kDest).toBigDecimal().stripTrailingZeros()
                findViewById<TextView>(R.id.result_TV).text =
                    DecimalFormat("#.####").format(destAmount) + " " + resources.getStringArray(R.array.currencies_codes)[destWrapper.value!!]
            } catch (e: NumberFormatException) {
                Toast.makeText(this.baseContext, R.string.bad_value_TT, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun getOnItemSelectedListener(wrapper: MutableLiveData<Int>): AdapterView.OnItemSelectedListener {
        return object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {
                wrapper.value = null;
            }

            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                wrapper.value = parent!!.selectedItemPosition
            }
        }
    }
}