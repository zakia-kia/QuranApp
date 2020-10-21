package com.zakia.idn.quranapp.utils

import android.app.IntentService
import android.content.Intent
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.os.ResultReceiver
import java.util.*

class GetAddressIntentService : IntentService(IDENTIFIER) {

    private var addressResultReceiver : ResultReceiver? = null
    companion object {
        private const val IDENTIFIER = "GetAddressIntentService"
    }

    override fun onHandleIntent(intent: Intent?) {
        var msg = ""

        addressResultReceiver = intent!!.getParcelableExtra("add_receiver")
        if (addressResultReceiver == null) {
            return
        }
        val location = intent.getParcelableExtra<Location>("add_location")
        if (location == null) {
            msg = "No Location, Can't go further without location"
            sendResultsToReceiver(0,msg)
            return
        }
        val geocoder = Geocoder(this, Locale.getDefault())
        var address : List<Address> ? = null
        try {
            address = geocoder.getFromLocation(
                location.latitude,
                location.longitude,1
            )
        }catch (ignored : Exception) {

        }

        if (address == null || address.size == 0) {
            msg = "No Address found fot the location"
            sendResultsToReceiver(1,msg)

        } else {
            val address = address[0]
            val addressDetails = StringBuffer()

            addressDetails.append(address.adminArea)
            addressDetails.append("\n")
            sendResultsToReceiver(2, addressDetails.toString())
        }
    }

    private fun sendResultsToReceiver(resultCode: Int, msg: String) {
        val bundle = Bundle()
        bundle.putString("address_result",msg)
        addressResultReceiver!!.send(resultCode,bundle)
    }
}