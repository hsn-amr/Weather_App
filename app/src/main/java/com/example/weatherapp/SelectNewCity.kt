package com.example.weatherapp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast

class SelectNewCity : AppCompatActivity() {

    lateinit var zipCodeInput: EditText
    lateinit var subminButton: Button
    var zipCode = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_select_new_city)

        zipCodeInput = findViewById(R.id.etZipCode)
        subminButton = findViewById(R.id.btnSubmit)

        subminButton.setOnClickListener {
            if(zipCodeInput.text.isNotEmpty()){
                zipCode = zipCodeInput.text.toString()
                val intent = Intent(this, MainActivity::class.java)
                intent.putExtra("zipCode", zipCode)
                startActivity(intent)
            }else{
                Toast.makeText(this, "Please, enter zip code first", Toast.LENGTH_LONG).show()
            }
        }
    }
}