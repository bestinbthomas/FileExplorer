package com.hackathon.fileexplorer

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.FileWriter
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var adapter: FilesRecyclerAdapter
    private var files = listOf<File>()
    private val selFiles = mutableListOf<File>()
    private val selections = mutableListOf<Int>()
    private var heirarchy = Stack<File>()
    private var isLocked = false
    private val TAG = "MainActivity"
    private var isCopy: Boolean? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    override fun onStart() {
        super.onStart()
        checkPermission()
        setOnClicks()
        optionsLayout.visibility = View.GONE
    }

    private fun setOnClicks() {
        optionsLayout.setOnClickListener { }
        addTextFile.setOnClickListener {
            if (isLocked)
                copyOrMoveTo()
            else
                createNewTextFile()
        }
        rename.setOnClickListener {
            val view = layoutInflater.inflate(R.layout.dialog_layout, null, false)
            view.findViewById<EditText>(R.id.fileBody).visibility = View.GONE
            AlertDialog.Builder(this)
                .setTitle("TextName")
                .setView(view)
                .setPositiveButton("OK") { _, _ ->
                    if (files[0].isDirectory)
                        files[0].renameTo(
                            File(
                                files[0].parentFile,
                                view.findViewById<EditText>(R.id.editText).text.toString()
                            )
                        )
                    else {
                        files[0].renameTo(
                            File(
                                files[0].parentFile,
                                view.findViewById<EditText>(R.id.editText).text.toString() + files[0].extension
                            )
                        )
                    }
                    selections.removeAll(selections)
                    adapter.setSelection(selections)
                   adapter.updateFiles(heirarchy.peek().listFiles().filter { !it.name.startsWith(".") })
                }
                .create()
                .show()
        }
        copy.setOnClickListener {
            copyOrMove(true)
        }
        move.setOnClickListener {
            copyOrMove(false)
        }
        delete.setOnClickListener {
            selFiles.forEach {
                if (!it.delete()) {
                    Toast.makeText(this, "cannot delete ${it.name}", Toast.LENGTH_SHORT).show()
                }
            }
            selFiles.removeAll(selFiles)
            selections.removeAll(selections)
            optionsLayout.visibility = View.GONE
            adapter.setSelection(selections)
            adapter.updateFiles(heirarchy.peek().listFiles().filter { !it.name.startsWith(".") })
        }
        share.setOnClickListener {
            val shareFile = Intent(Intent.ACTION_SEND_MULTIPLE)
            val path = FileProvider.getUriForFile(
                this,
                BuildConfig.APPLICATION_ID + ".provider",
                files[0]
            )
            shareFile.type = selFiles[0].getType()
            shareFile.putExtra(Intent.EXTRA_STREAM, path)
            startActivity(Intent.createChooser(shareFile, "Share to ..."))
        }
    }


    private fun createNewTextFile() {
        val view = layoutInflater.inflate(R.layout.dialog_layout, null, false)
        view.findViewById<EditText>(R.id.fileBody).visibility = View.VISIBLE
        AlertDialog.Builder(this)
            .setTitle("TextName")
            .setView(view)
            .setPositiveButton("OK") { _, _ ->
                createText(view.findViewById<EditText>(R.id.editText).text.toString(),view.findViewById<EditText>(R.id.fileBody).text.toString())
            }
            .create()
            .show()
    }

    private fun createText(filename: String,body : String) {
        val file = File(heirarchy.peek(),"$filename.txt")
        Log.e(TAG, "file creating with filename $filename at ${file.path}")
        val writer = FileWriter(file,true)
                writer.append(body)
                writer.flush()
                writer.close()
        openFile(file)

    }

    private fun setRecycler(file: File) {
        heirarchy = Stack()
        heirarchy.push(file)
        files = file.listFiles().filter { !it.name.startsWith(".") }
        adapter = FilesRecyclerAdapter(this, files, selections)
        adapter.fileClickLiveData.observe(
            this,
            Observer {
                if (!isLocked) {
                    Log.e(TAG, "file index $it clicked")
                    if (selections.isNotEmpty()) {
                        if (selections.contains(it)) {
                            selections.remove(it)
                            selFiles.remove(files[it])
                            if (selections.size == 1) {
                                rename.visibility = View.VISIBLE
                            }
                            if (selections.isEmpty()) {
                                optionsLayout.visibility = View.GONE
                            }
                        } else {
                            selections.add(it)
                            selFiles.add(files[it])
                            share.visibility = View.GONE
                            rename.visibility = View.GONE
                        }
                    } else if (files[it].isDirectory)
                        goToDir(files[it])
                    else
                        openFile(files[it])
                    adapter.setSelection(selections)
                } else {
                    goToDir(files[it])
                }
            }
        )
        adapter.fileLongpressLiveData.observe(
            this,
            Observer {
                if (!selections.contains(it)) {
                    selections.add(it)
                    adapter.setSelection(selections)
                    fileSelected(files[it])
                }
            }
        )
        FilesListRecView.adapter = adapter
        FilesListRecView.itemAnimator = DefaultItemAnimator()
        FilesListRecView.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))
        FilesListRecView.layoutManager = LinearLayoutManager(this)
    }

    private fun fileSelected(file: File) {
        selFiles.add(file)
        optionsLayout.visibility = View.VISIBLE
    }

    private fun openFile(file: File) {
        val path = FileProvider.getUriForFile(
            this,
            BuildConfig.APPLICATION_ID + ".provider",
            file
        )
        val intent = Intent(Intent.ACTION_VIEW, path)
        intent.putExtra(Intent.EXTRA_STREAM, path)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        startActivity(intent)
    }

    private fun goToDir(file: File) {
        heirarchy.push(file)
        selections.removeAll(selections)
        adapter.setSelection(selections)
        HierarchyTxt.text = HierarchyTxt.text.toString() + " / " + file.nameWithoutExtension
        files = file.listFiles().filter { !it.name.startsWith(".") }
        adapter.updateFiles(files)
    }

    private fun checkPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            permissionOK()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                1234
            )
        }

    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == 1234 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            permissionOK()
        } else
            permissionDeclined()
    }

    private fun permissionDeclined() =
        AlertDialog.Builder(this)
            .setTitle("Permission Needed")
            .setMessage("Click Allow in the next popup to continue using")
            .setOnCancelListener {
                checkPermission()
            }
            .create()
            .show()


    private fun permissionOK() {
        val file = Environment.getExternalStorageDirectory()
        heirarchy.push(file)
        HierarchyTxt.text = "Internal Storage"
        setRecycler(file)
    }

    override fun onBackPressed() {
        if (heirarchy.size <= 1 && selFiles.isEmpty())
            super.onBackPressed()
        else if (selFiles.isNotEmpty() && !isLocked) {
            selFiles.removeAll(selFiles)
            selections.removeAll(selections)
            adapter.setSelection(selections)
            optionsLayout.visibility = View.GONE
        } else if (heirarchy.size > 1) {
            heirarchy.pop()
            selections.removeAll(selections)
            val string = HierarchyTxt.text.toString()
            val i = string.lastIndexOf("/")
            HierarchyTxt.text = string.substring(0, i)
            files = heirarchy.peek().listFiles().filter { !it.name.startsWith(".") }
            adapter.updateFiles(files)

        }
    }

    private fun copyOrMove(copy: Boolean) {
        this.isCopy = copy
        isLocked = true
        addTextFile.background = resources.getDrawable(R.drawable.done_btn_back, this.theme)
        optionsLayout.visibility = View.GONE

    }

    private fun copyOrMoveTo() {
        selFiles.forEach { file ->
            try {
                Log.e(TAG, "copy / move in progress")
                val inStream = FileInputStream(file)
                val output = File(heirarchy.peek(),file.name)
                val outStream = FileOutputStream(output)

                var buffer = ByteArray(1024)
                var read = inStream.read(buffer)
                while(read != -1){
                    Log.e(TAG, "copy / move in progress")
                    outStream.write(buffer,0,read)
                    read = inStream.read(buffer)
                }
                inStream.close()
                outStream.flush()
                outStream.close()
                Log.e(TAG, "Copy file successful to ${output.path}")
                if(isCopy != true)
                    file.delete()
            } catch (e: Exception) {
                Log.e(TAG, "copy / move failed", e)
            }
        }
        adapter.updateFiles(heirarchy.peek().listFiles().filter { !it.name.startsWith(".") })
        Log.e(TAG, "copy / move completed")
        selFiles.removeAll(selFiles)
        selections.removeAll(selections)
        isLocked = false
        addTextFile.background = resources.getDrawable(R.drawable.add_btn_back,this.theme)
    }


}
