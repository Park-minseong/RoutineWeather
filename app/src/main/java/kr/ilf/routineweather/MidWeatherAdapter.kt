package kr.ilf.routineweather

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kr.ilf.routineweather.databinding.ItemMidBinding
import kr.ilf.routineweather.databinding.ItemSrtFcstBinding
import kr.ilf.routineweather.model.weather.MidTa

class MidWeatherAdapter(
    private val context: Context,
    private val items: ArrayList<MidTa>
) :
    RecyclerView.Adapter<MidWeatherAdapter.ViewHolder>() {

    private var preFcstTime = "0000"

    class ViewHolder(binding: ItemMidBinding) : RecyclerView.ViewHolder(binding.root) {
        val ivMain = binding.ivMain
        val tvTempMax = binding.tvTempMax
        val tvTempMin = binding.tvTempMin
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ItemMidBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun getItemCount(): Int {
        return items.size
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.tvTempMax.text = items[position].taMax.toString()
        holder.tvTempMin.text = items[position].taMin.toString()
    }

    private fun getDate(fcstDate: String): String {
        val monthDay = fcstDate.substring(4)

        val month = monthDay.substring(0, 2).toInt()
        val day = monthDay.substring(2).toInt()

        return "$month.$day"
    }

}