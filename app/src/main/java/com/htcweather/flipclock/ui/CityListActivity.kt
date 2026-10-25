package com.htcweather.flipclock.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.htcweather.flipclock.data.model.CityEntity
import com.htcweather.flipclock.databinding.ActivityCityListBinding
import com.htcweather.flipclock.databinding.ItemCityBinding
import com.htcweather.flipclock.viewmodel.CityViewModel

class CityListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCityListBinding
    private val viewModel: CityViewModel by viewModels()
    private lateinit var adapter: CityAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCityListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = CityAdapter(
            onClick = { city ->
                // Return selected city to MainActivity via result or SharedPreferences
                // For simplicity we just finish; MainActivity reloads onResume
                // You can extend with setResult if needed
                finish()
            },
            onDelete = { city ->
                viewModel.deleteCity(city)
            }
        )

        binding.rvCities.layoutManager = LinearLayoutManager(this)
        binding.rvCities.adapter = adapter

        viewModel.cities.observe(this) { list ->
            adapter.submitList(list)
        }

        binding.btnAddCity.setOnClickListener {
            startActivity(Intent(this, AddCityActivity::class.java))
        }
    }
}

class CityAdapter(
    private val onClick: (CityEntity) -> Unit,
    private val onDelete: (CityEntity) -> Unit
) : RecyclerView.Adapter<CityAdapter.VH>() {

    private var items = listOf<CityEntity>()

    fun submitList(list: List<CityEntity>) {
        items = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemCityBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size

    inner class VH(private val binding: ItemCityBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(city: CityEntity) {
            binding.tvCityName.text = city.name + (city.country?.let { ", $it" } ?: "")
            binding.tvCityTemp.text = if (city.isHome) "Maison" else "${"%.2f".format(city.lat)}, ${"%.2f".format(city.lon)}"
            binding.root.setOnClickListener { onClick(city) }
            binding.btnDelete.setOnClickListener { onDelete(city) }
        }
    }
}
