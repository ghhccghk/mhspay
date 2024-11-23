package com.mihuashi.paybyfinger.ui.fragment

import android.annotation.SuppressLint
import android.app.LauncherActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.mihuashi.paybyfinger.BuildConfig
import com.mihuashi.paybyfinger.R

class AboutFragment : AppCompatActivity() {

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.fragment_about)
        val aboutToolbar: MaterialToolbar = findViewById(R.id.about_toolbar)
        val view : RecyclerView = findViewById(R.id.about_recycler_view)

        aboutToolbar.setNavigationOnClickListener {
            finish() // 或者 finish()
        }

        // 示例数据
        val items = listOf(
            ListItem("版本", BuildConfig.VERSION_CODE.toString(),R.drawable.ic_round_error_outline),
            ListItem("Item 2", "Description for Item 2",R.drawable.ic_round_settings),
            ListItem("Item 3", "Description for Item 3",R.drawable.ic_round_settings)
        )

        // 初始化适配器并设置点击事件
        val adapter = ListAdapter(items) { item ->
            // 点击事件处理
            Toast.makeText(this, "Clicked: ${item.title}", Toast.LENGTH_SHORT).show()
        }


        view.layoutManager = LinearLayoutManager(this)
        view.adapter = adapter

    }
}


class ListAdapter(
    private val items: List<ListItem>,
    private val onItemClick: (ListItem) -> Unit // 点击事件回调
) : RecyclerView.Adapter<ListAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val title = view.findViewById<TextView>(R.id.itemTitle)
        private val description = view.findViewById<TextView>(R.id.itemDescription)
        private val icon = view.findViewById<ImageView>(R.id.itemIcon)

        fun bind(item: ListItem) {
            title.text = item.title
            description.text = item.description
            // 设置图标，可以根据 item 的某些条件设置不同的图标
            icon.setImageResource(item.iconRes) // 设置图标资源
            // 设置点击事件
            itemView.setOnClickListener { onItemClick(item) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_list, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size
}

data class ListItem(val title: String, val description: String,val iconRes: Int )



