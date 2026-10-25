package com.htcweather.flipclock.ui

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.htcweather.flipclock.data.model.GeoResult
import com.htcweather.flipclock.databinding.ActivityAddCityBinding
import com.htcweather.flipclock.databinding.ItemSearchResultBinding
import com.htcweather.flipclock.viewmodel.CityViewModel

class AddCityActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddCityBinding
    private val viewModel: CityViewModel by viewModels()
    private lateinit var adapter: SearchAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddCityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = SearchAdapter { result ->
            viewModel.addCity(result)
            finish()
        }

        binding.rvSearchResults.layoutManager = LinearLayoutManager(this)
        binding.rvSearchResults.adapter = adapter

        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                viewModel.search(s?.toString()?.trim() ?: "")
            }
        })

        viewModel.searchResults.observe(this) { list ->
            adapter.submitList(list)
        }
    }
}

class SearchAdapter(
    private val onSelect: (GeoResult) -> Unit
) : RecyclerView.Adapter<SearchAdapter.VH>() {

    private var items = listOf<GeoResult>()

    fun submitList(list: List<GeoResult>) {
        items = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemSearchResultBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size

    inner class VH(private val binding: ItemSearchResultBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: GeoResult) {
            binding.tvResultName.text = item.name
            binding.tvResultCountry.text = listOfNotNull(item.state, item.country).joinToString(", ")
            binding.root.setOnClickListener { onSelect(item) }
        }
    }
}
