package com.zakia.idn.quranapp.activity

import android.annotation.SuppressLint
import android.app.ProgressDialog
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.text.Html
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.androidnetworking.AndroidNetworking
import com.androidnetworking.common.Priority
import com.androidnetworking.error.ANError
import com.androidnetworking.interfaces.JSONArrayRequestListener
import com.zakia.idn.quranapp.R
import com.zakia.idn.quranapp.adapter.AyatAdapter
import com.zakia.idn.quranapp.model.ModelAyat
import com.zakia.idn.quranapp.model.ModelSurah
import com.zakia.idn.quranapp.network.Api
import kotlinx.android.synthetic.main.activity_detail_surat.*
import org.json.JSONArray
import org.json.JSONException
import java.io.IOException

class DetailSuratActivity : AppCompatActivity() {
    var nomor : String?= null
    var nama : String? = null
    var arti : String? = null
    var type : String? = null
    var ayat : String? = null
    var keterangan : String? = null
    var audio : String? = null
    var modelSurah : ModelSurah? = null
    var ayatAdapter : AyatAdapter? = null
    var progressDialog : ProgressDialog? = null
    var modelAyat : MutableList<ModelAyat> = ArrayList()
    var mHandler : Handler? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail_surat)

        toolbar_detail.setTitle(null)
        setSupportActionBar(toolbar_detail)
        assert(supportActionBar !=null)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        mHandler = Handler()

        modelSurah = intent.getSerializableExtra("detailSurah") as ModelSurah
        if (modelSurah !=null) {
            nomor = modelSurah!!.nomor
            nama = modelSurah!!.nama
            arti = modelSurah!!.arti
            type = modelSurah!!.type
            ayat = modelSurah!!.ayat
            audio = modelSurah!!.audio
            keterangan = modelSurah!!.keterangan

            fab_stop.visibility = View.GONE
            fab_play.visibility = View.VISIBLE

            tv_header.setText(nama)
            tv_title.setText(nama)
            tv_subtitel.setText(arti)
            tv_info.setText("$type - $ayat Ayat")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                tv_ket.setText(Html.fromHtml(keterangan, Html.FROM_HTML_MODE_COMPACT))
            else {
                tv_ket.setText(Html.fromHtml(keterangan))
            }
            playAudio()
        }
        progressDialog = ProgressDialog(this)
        progressDialog!!.setTitle("Please wait . . .")
        progressDialog!!.setCancelable(false)
        progressDialog!!.setMessage("Sedang menampilkan data . . .")

        rv_ayat.layoutManager = LinearLayoutManager(this)
        rv_ayat.setHasFixedSize(true)

        listAyat()
    }

    private fun listAyat() {
        progressDialog!!.show()
        AndroidNetworking.get(Api.URL_LIST_AYAT)
            .addPathParameter("nomor", nomor)
            .setPriority(Priority.MEDIUM)
            .build()
            .getAsJSONArray(object : JSONArrayRequestListener{
                override fun onResponse(response: JSONArray) {
                    for (i in 0 until response.length()){
                        try {
                            progressDialog!!.dismiss()
                            val dataApi = ModelAyat()
                            val jsonObject = response.getJSONObject(i)
                            dataApi.nomor = jsonObject.getString("nomor")
                            dataApi.arab = jsonObject.getString("arab")
                            dataApi.indo = jsonObject.getString("indo")
                            dataApi.terjemahan = jsonObject.getString("terjemahan")
                            modelAyat.add(dataApi)
                            showListAyat()
                        }catch (e: JSONException) {
                            e.printStackTrace()
                            Toast.makeText(this@DetailSuratActivity, "Gagal mengambil data", Toast.LENGTH_SHORT).show()
                        }
                    }
                }

                override fun onError(anError: ANError?) {
                   progressDialog!!.dismiss()
                    Toast.makeText(this@DetailSuratActivity, "Tidak ada jaringan internet", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun showListAyat() {
        ayatAdapter = AyatAdapter(this@DetailSuratActivity,modelAyat)
        rv_ayat!!.adapter = ayatAdapter
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun playAudio() {
        val mediaPlayer = MediaPlayer()
        fab_play.setOnClickListener(View.OnClickListener {
            try {
                mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC)
                mediaPlayer.setDataSource(audio)
                mediaPlayer.prepare()
                mediaPlayer.start()
            } catch (e: IOException) {
                e.printStackTrace()
            }

            fab_play.visibility = View.GONE
            fab_stop.visibility = View.VISIBLE
        })

        fab_stop.setOnClickListener(View.OnClickListener {
            mediaPlayer.stop()
            mediaPlayer.reset()
            fab_play.visibility = View.VISIBLE
            fab_stop.visibility = View.GONE

        })
    }
}