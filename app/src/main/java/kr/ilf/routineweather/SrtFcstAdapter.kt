package kr.ilf.routineweather

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kr.ilf.routineweather.databinding.ItemSrtFcstBinding
import kr.ilf.routineweather.model.UltraSrtFcst

class SrtFcstAdapter(private val context: Context, private val items: ArrayList<UltraSrtFcst>) :
    RecyclerView.Adapter<SrtFcstAdapter.ViewHolder>() {

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
        val ultraSrtFcst = items[position]
        val temp = ultraSrtFcst.t1h + "°"
        val time = ultraSrtFcst.fcstTime.substring(0, 2) + "시"
        val sky = ultraSrtFcst.sky
        val pty = ultraSrtFcst.pty

        val iconCode = sky + pty

        holder.tv_time.text = time
        holder.tv_temp.text = temp

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
}