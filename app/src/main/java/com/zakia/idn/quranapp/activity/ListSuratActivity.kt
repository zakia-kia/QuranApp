package com.zakia.idn.quranapp.activity

import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.ResultReceiver
import android.text.format.DateFormat
import android.view.View
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.androidnetworking.AndroidNetworking
import com.androidnetworking.common.Priority
import com.androidnetworking.error.ANError
import com.androidnetworking.interfaces.JSONArrayRequestListener
import com.google.android.gms.location.*
import com.zakia.idn.quranapp.R
import com.zakia.idn.quranapp.adapter.SurahAdapter
import com.zakia.idn.quranapp.fragment.JadwalSholatFragment.Companion.newInstance
import com.zakia.idn.quranapp.model.ModelSurah
import com.zakia.idn.quranapp.network.Api
import com.zakia.idn.quranapp.utils.GetAddressIntentService
import kotlinx.android.synthetic.main.activity_list_surat.*
import org.json.JSONArray
import org.json.JSONException
import java.lang.Exception
import java.util.*
import java.util.jar.Manifest
import kotlin.collections.ArrayList

class ListSuratActivity : AppCompatActivity(), SurahAdapter.onSelectDataa {

    var surahAdapter : SurahAdapter? = null
    var progressDialog : ProgressDialog? = null
    var modelSurah : MutableList<ModelSurah> = ArrayList()
    var hariIni : String? = null
    var tanggal : String? = null
    private var fussedLocationClient : FusedLocationProviderClient? = null
    private var addressResultReceiver : LocationAddressResultReceive? = null
    private var currentLocation : Location? = null
    var locationCallback : LocationCallback? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_list_surat)

        progressDialog = ProgressDialog(this)
        progressDialog!!.setTitle("Please wait")
        progressDialog!!.setCancelable(false)
        progressDialog!!.setMessage("Sedang mengambil data")

        addressResultReceiver = LocationAddressResultReceive(Handler())
        val dateNow = Calendar.getInstance().time
        hariIni = DateFormat.format("EEEE", dateNow) as String
        tanggal = DateFormat.format("d MMMM yyyy", dateNow) as String

        tv_today.setText("$hariIni, ")
        tv_Data.setText(tanggal)

        val sendDetail = newInstance("detail")
        Ll_time.setOnClickListener(View.OnClickListener {
            sendDetail.show(supportFragmentManager, sendDetail.tag)
        })

        Ll_Mosque.setOnClickListener(View.OnClickListener {
            startActivity(Intent(this@ListSuratActivity, MasjidActivity::class.java))
        })

        rv_surat.layoutManager = LinearLayoutManager(this)
        rv_surat.setHasFixedSize(true)

        fussedLocationClient =  LocationServices.getFusedLocationProviderClient(this)
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
                currentLocation = locationResult!!.locations[0]
                address()

            }
        }
        startLocationUpdates()
        listSurah()
    }

    private val address : Unit
        get() {
            if (!Geocoder.isPresent()) {
                Toast.makeText(this@ListSuratActivity, "can't find current address,", Toast.LENGTH_SHORT).show()

                return
            }

            val intent = Intent(this, GetAddressIntentService::class.java)
            intent.putExtra("add_receiver", addressResultReceiver)
            intent.putExtra("add_receiver", currentLocation)
            startService(intent)
        }

    private fun listSurah() {
        progressDialog!!.show()
        AndroidNetworking.get(Api.URL_LIST_SURAH).setPriority(Priority.MEDIUM).build().getAsJSONArray(object : JSONArrayRequestListener{
            override fun onResponse(response: JSONArray) {
                for (i in 0 until response.length()) {
                    try {
                        progressDialog!!.dismiss()
                        val dataApi = ModelSurah()
                        val JsonObject = response.getJSONObject(i)
                        dataApi.nomor = JsonObject.getString("nomor")
                        dataApi.nama = JsonObject.getString("nama")
                        dataApi.type = JsonObject.getString("type")
                        dataApi.ayat = JsonObject.getString("ayat")
                        dataApi.asma = JsonObject.getString("asma")
                        dataApi.arti = JsonObject.getString("arti")
                        dataApi.audio = JsonObject.getString("audio")
                        dataApi.keterangan = JsonObject.getString("keterangan")
                        modelSurah.add(dataApi)
                        showListSurah()

                    }catch (e:JSONException) {
                        e.printStackTrace()
                        Toast.makeText(this@ListSuratActivity, "Gagal menampilkan data", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            override fun onError(anError: ANError?) {
                progressDialog!!.dismiss()
                Toast.makeText(this@ListSuratActivity, "Tidak ada jaringan", Toast.LENGTH_SHORT).show()
            }

        })
    }

    private fun showListSurah() {
        surahAdapter = SurahAdapter(this@ListSuratActivity, modelSurah, this)
        rv_surat!!.adapter = surahAdapter
    }

    private fun startLocationUpdates() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
        }else{
            val locationRequest = LocationRequest()
            locationRequest.interval = 1000
            locationRequest.fastestInterval = 1000
            locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            fussedLocationClient!!.requestLocationUpdates(locationRequest, locationCallback, null)
        }
    }

    override fun onSelected(modelSurah: ModelSurah) {
        val intent = Intent(this@ListSuratActivity, DetailSuratActivity::class.java)
        intent.putExtra("detailSurah", modelSurah)
        startActivity(intent)
    }

    private inner class LocationAddressResultReceive internal constructor(handler: Handler?) : ResultReceiver(handler) {
        override fun onReceiveResult(resultCode: Int, resultData: Bundle?) {
            if (resultCode == 0 ) {
                address
            }
            if (resultCode == 1 ) {
                Toast.makeText(this@ListSuratActivity, "Address not found", Toast.LENGTH_SHORT).show()
            }
            val currentAdd = resultData!!.getString("address_result")
            showResult(currentAdd)
        }

    }

    private fun showResult(currentAdd: String?) {
        tv_Txtlocation!!.text = currentAdd
    }

    override fun onResume() {
        super.onResume()
        startLocationUpdates()
    }

    override fun onPause() {
        super.onPause()
        fussedLocationClient!!.removeLocationUpdates(locationCallback)
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 2
    }


}