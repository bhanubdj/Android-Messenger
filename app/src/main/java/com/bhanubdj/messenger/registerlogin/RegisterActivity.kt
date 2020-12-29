package com.bhanubdj.messenger.registerlogin

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Parcelable
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import com.bhanubdj.messenger.R
import com.bhanubdj.messenger.messages.LatestMessagesActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import kotlinx.android.parcel.Parcelize
import kotlinx.android.synthetic.main.activity_register.*
import java.util.*


class RegisterActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)


        register_button_register.setOnClickListener {

           performRegister()

        }

        already_have_account_text_view.setOnClickListener {
            Log.d("RegisterActivity", "Try to show login activity")

            //launch the login activity
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)

        }

        selectphoto_button_register.setOnClickListener{
            Log.d("RegisterActivity", "Try to show photo selector")

           val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent, 0)
        }

    }
     var selectedPhotoUris: Uri? = null

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 0 && resultCode == Activity.RESULT_OK && data != null) {
            //proceed and check what the selected image was....
            Log.d( "RegisterActivity", "photo was selected")

            selectedPhotoUris = data.data
            val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, selectedPhotoUris)

            selectphoto_imageview_register.setImageBitmap(bitmap)

            selectphoto_button_register.alpha = 0f

            //val bitmapDrawable = BitmapDrawable(bitmap)
            //selectphoto_button_register.setBackgroundDrawable(bitmapDrawable)


        }
    }

    private fun performRegister () {
        val email = email_edittext_register.text.toString()
        val password = password_edittext_register.text.toString()
        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this,"please Fill Details in Email & Password", Toast.LENGTH_SHORT).show()
            return
        }


        Log.d("RegisterActivity", "Email is:" + email)
        Log.d("RegisterActivity", "password: $password")
        //Firebase Authentication
        FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener {
                if (!it.isSuccessful) return@addOnCompleteListener
                //else if successful
                Log.d("RegisterActivity", "Successfully created user with Uid: ${it.result?.user?.uid}")

                uploadImageToFirebaseStorage()
            }
            .addOnFailureListener{
                Log.d("RegisterActivity", "Faild to create user: ${it.message}")
                Toast.makeText(this,"Faild to create user: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun uploadImageToFirebaseStorage () {

        if(selectedPhotoUris == null) return
        val filename = UUID.randomUUID().toString()
        val ref = FirebaseStorage.getInstance().getReference("/image/$filename")
        ref.putFile(selectedPhotoUris!!)
            .addOnSuccessListener {
                Log.d("RegisterActivity", "Successfully uploaded image: ${it.metadata?.path}")

                ref.downloadUrl.addOnSuccessListener {
                    Log.d("RegisterActivity", "File Location: $it")
                    saveUserToFirebaseDatabase(it.toString())
                }
            }
            .addOnFailureListener{
                //do some logging here

            }

    }
    private fun saveUserToFirebaseDatabase(profileImageUrl: String) {
        val uid = FirebaseAuth.getInstance().uid ?:""
        val ref = FirebaseDatabase.getInstance().getReference("/Users/$uid")

        val user = User(
            uid,
            username_edittext_register.text.toString(),
            profileImageUrl
        )
        ref.setValue(user)
            .addOnSuccessListener {
                Log.d("RegisterActivity", "Finally we saved the user to Firebase Database")

                val intent = Intent (this, LatestMessagesActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)

            }
            //.addOnFailureListener {
               // Log.d(TAG,"Failed to set value to database: ${it.message}")
            }

    }
//}
@Parcelize
class User(val uid: String, val username: String, val profileImageUrl: String):Parcelable {

    constructor() : this("","","")

}