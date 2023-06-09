package kr.ilf.routineweather

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kr.ilf.routineweather.databinding.ItemSrtFcstBinding
import kr.ilf.routineweather.model.weather.VilageFcst

class TimeWeatherAdapter(
    private val context: Context,
    private val items: ArrayList<VilageFcst>
) :
    RecyclerView.Adapter<TimeWeatherAdapter.ViewHolder>() {

    private var preFcstTime = "0000"

    class ViewHolder(binding: ItemSrtFcstBinding) : RecyclerView.ViewHolder(binding.root) {
        val ivMain = binding.ivMain
        val tv_temp = binding.tvTemp
        val tv_time = binding.tvTime
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ItemSrtFcstBinding.inflate(
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
        val item = items[position]

        val time = item.fcstTime
        val hour =
            if (time.substring(0, 2) == "00") getDate(item.fcstDate) else time.substring(0, 2) + "시"
        val temp = item.tmp + "°"
        val sky = item.sky
        val pty = item.pty

        holder.tv_time.text = hour
        holder.tv_temp.text = temp

        preFcstTime = item.fcstTime

        when (pty) {
            "1", "4", "5" -> {
                holder.ivMain.setImageDrawable(context.getDrawable(R.drawable.rain))
            }

            "2", "3", "6", "7" -> {
                holder.ivMain.setImageDrawable(context.getDrawable(R.drawable.snowflake))
            }

            else -> {
                if (sky == "4") {
                    holder.ivMain.setImageDrawable(context.getDrawable(R.drawable.cloud))
                } else {
                    holder.ivMain.setImageDrawable(context.getDrawable(R.drawable.sunny))
                }
            }

        }
    }

    private fun getDate(fcstDate: String): String {
        val monthDay = fcstDate.substring(4)

        val month = monthDay.substring(0, 2).toInt()
        val day = monthDay.substring(2).toInt()

        return "$month.$day"
    }

}