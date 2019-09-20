package com.hackathon.fileexplorer

import android.content.Context
import android.graphics.Color
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import java.io.File

class FilesRecyclerAdapter(
    private val context: Context,
    private var files: List<File>,
    var selections : List<Int>,
    val fileClickLiveData: MutableLiveData<Int> = MutableLiveData(),
    val fileLongpressLiveData: MutableLiveData<Int> = MutableLiveData()
) : RecyclerView.Adapter<FilesRecyclerAdapter.MyHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyHolder {
        return MyHolder(LayoutInflater.from(parent.context).inflate(R.layout.directory_item,parent,false))
    }

    override fun getItemCount(): Int = files.size

    override fun onBindViewHolder(holder: MyHolder, position: Int) {
        val file = files[position]

        holder.name.text = file.name
        if(file.isDirectory){
            holder.icon.setImageResource(R.drawable.ic_folder)
            holder.count.visibility = View.VISIBLE
            holder.count.text = context.resources.getString(R.string.item_count,file.getChildCount())
        } else{
            holder.count.visibility = View.GONE

            if(file.isImage()){
                if(file.checkDuplicateIn(files)){
                    holder.root.setBackgroundColor(Color.YELLOW)
                }
                Glide.with(context).load(Uri.fromFile(file)).apply(RequestOptions.circleCropTransform()).placeholder(R.drawable.ic_image).into(holder.icon)
            }
            else
                holder.icon.setImageResource(R.drawable.ic_file)
        }
        holder.root.tag = position

        if(position in selections)
            holder.root.setBackgroundColor(context.resources.getColor(R.color.secondaryLightColor,context.theme))
        else
            holder.root.setBackgroundColor(Color.WHITE)


        holder.root.setOnClickListener {
            fileClickLiveData.value = it.tag as Int
        }

        holder.root.setOnLongClickListener {
            fileLongpressLiveData.value = it.tag as Int
            true
        }

    }

    fun setSelection(sel : List<Int>){
        this.selections = sel
        notifyDataSetChanged()
    }
    fun updateFiles(files : List<File>){
        this.files = files
        notifyDataSetChanged()
    }

    class MyHolder(view : View) : RecyclerView.ViewHolder(view){
        val root = view.findViewById<ConstraintLayout>(R.id.FileItemRoot)
        val icon = view.findViewById<ImageView>(R.id.FileIcon)
        val count = view.findViewById<TextView>(R.id.FileItemCount)
        val name = view.findViewById<TextView>(R.id.FileName)
    }
}