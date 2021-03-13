package com.example.mobile_dev

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.MutableLiveData
import com.github.kittinunf.fuel.Fuel
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.xml.sax.InputSource
import java.io.File
import java.io.StringReader
import java.text.DecimalFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.*
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathFactory
import kotlin.time.ExperimentalTime

fun Node.iterator(): Iterator<Node> {
    val outerThis = this
    return object : Iterator<Node> {
        var i = -1;
        val childNodes = outerThis.childNodes;

        override fun hasNext(): Boolean {
            return i + 1 < this.childNodes.length
        }

        override fun next(): Node {
            return this.childNodes.item(++i)
        }
    }
}

class MainActivity : AppCompatActivity() {
    companion object {
        private const val EUROFXREF =
            "https://www.ecb.europa.eu/stats/eurofxref/eurofxref-daily.xml"
        private const val CACHE_FILENAME = "eurofxref-daily.xml"

        // https://www.ecb.europa.eu/stats/policy_and_exchange_rates/euro_reference_exchange_rates/html/index.en.html
        private const val BEFORE_UPDATE_HOUR: Long = 15;

        private val builder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
        private val xPath = XPathFactory.newInstance().newXPath()
    }

    private lateinit var xmlFile: File

    private val srcWrapper = MutableLiveData<String>()
    private val destWrapper = MutableLiveData<String>()
    private lateinit var srcSpinner: Spinner
    private lateinit var destSpinner: Spinner

    private abstract class Currencies {
        abstract val time: LocalDateTime
        abstract val rates: LinkedHashMap<String, Float>
    }

    private lateinit var currencies: Currencies;

    private fun refreshCache() {
        Fuel.download(EUROFXREF)
            .fileDestination { _, _ -> xmlFile }
            .response { _ -> }
            .join()
    }

    private fun getTime(el: Element): LocalDateTime {
        return LocalDate.parse(el.getAttribute("time"), DateTimeFormatter.ISO_DATE)
            .atStartOfDay()
            .plusHours(BEFORE_UPDATE_HOUR);
    }

    @ExperimentalTime
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        xmlFile = File(filesDir, CACHE_FILENAME)
        if (!xmlFile.isFile) {
            xmlFile.createNewFile();
            refreshCache();
        }
        var document = builder.parse(InputSource(StringReader(xmlFile.readText())))
        var element = xPath.evaluate("//Cube[@time]", document, XPathConstants.NODE) as Element
        var time = getTime(element)
        if (time.until(LocalDateTime.now(), ChronoUnit.HOURS) > 24) {
            refreshCache()
            document = builder.parse(InputSource(StringReader(xmlFile.readText())))
            element = xPath.evaluate("//Cube[@time]", document, XPathConstants.NODE) as Element
            time = getTime(element)
        }

        val rates = LinkedHashMap<String, Float>()
        for (child in element.iterator()) {
            if (child !is Element) continue
            rates.put(child.getAttribute("currency"), child.getAttribute("rate").toFloat())
        }
        for (entry in rates.entries) {
            Log.d("XML", entry.key + ": " + entry.value)
        }

        currencies = object : Currencies() {
            override val time = time
            override val rates = rates
        }
        Log.d("XML", "updated: " + currencies.time);

        srcSpinner = findViewById<Spinner>(R.id.currency_src_DD)
        destSpinner = findViewById<Spinner>(R.id.currency_dest_DD)
    }

    override fun onStart() {
        super.onStart()
        Toast.makeText(this, R.string.welcome_TT, Toast.LENGTH_SHORT).show()

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            currencies.rates.keys.toTypedArray()
        );
        srcSpinner.adapter = adapter
        srcSpinner.adapter
        destSpinner.adapter = adapter

        srcSpinner.onItemSelectedListener =
            getOnItemSelectedListener(srcWrapper)
        destSpinner.onItemSelectedListener =
            getOnItemSelectedListener(destWrapper)

        findViewById<Button>(R.id.convert_BT).setOnClickListener {
            try {
                val srcAmount = findViewById<EditText>(R.id.input_ET).text.toString().toDouble()

                val kSrc = currencies.rates[srcWrapper.value!!] as Float
                val kDest = currencies.rates[destWrapper.value!!] as Float

                val destAmount = (srcAmount / kSrc * kDest).toBigDecimal().stripTrailingZeros()
                findViewById<TextView>(R.id.result_TV).text =
                    DecimalFormat("#.####").format(destAmount) + " " + destWrapper.value
            } catch (e: NumberFormatException) {
                Toast.makeText(this, R.string.bad_value_TT, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun getOnItemSelectedListener(wrapper: MutableLiveData<String>): AdapterView.OnItemSelectedListener {
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
                wrapper.value = parent!!.selectedItem as String
            }
        }
    }
}
