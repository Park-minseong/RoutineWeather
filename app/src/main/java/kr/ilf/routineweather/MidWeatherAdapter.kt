package kr.ilf.routineweather

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kr.ilf.routineweather.databinding.ItemMidBinding
import kr.ilf.routineweather.model.weather.MidTa
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class MidWeatherAdapter(
    private val context: Context,
    private val items: ArrayList<MidTa>
) :
    RecyclerView.Adapter<MidWeatherAdapter.ViewHolder>() {

    private var preFcstTime = "0000"

    class ViewHolder(binding: ItemMidBinding) : RecyclerView.ViewHolder(binding.root) {
        val ivMainAm = binding.ivMainAm
        val ivMainPm = binding.ivMainPm
        val tvRnStAm = binding.tvRnStAm
        val tvRnStPm = binding.tvRnStPm
        val tvTempMax = binding.tvTempMax
        val tvTempMin = binding.tvTempMin
        val tvDate = binding.tvDate
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

    @SuppressLint("UseCompatLoadingForDrawables", "SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val date = LocalDate.now().plusDays(items[position].date.toLong())
            .format(DateTimeFormatter.ofPattern("MM.dd"))

        holder.tvDate.text = date
        holder.tvRnStAm.text = "${items[position].rnStAm}%"
        holder.tvRnStPm.text = "${items[position].rnStPm}%"
        holder.tvTempMax.text = items[position].taMax.toString() + "°"
        holder.tvTempMin.text = items[position].taMin.toString() + "°"

        val wfAmCode = Constants.weatherCodeOpenApi[items[position].wfAm]!!
        val wfPmCode = Constants.weatherCodeOpenApi[items[position].wfPm]!!

        holder.ivMainAm.setImageDrawable(context.getDrawable(Constants.getDrawableIdWeather(wfAmCode)))
        holder.ivMainPm.setImageDrawable(context.getDrawable(Constants.getDrawableIdWeather(wfPmCode)))
    }
}