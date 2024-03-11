package com.codingwithsaurav.calendarapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.bumptech.glide.Glide
import com.codingwithsaurav.calendarapp.databinding.ActivityMainBinding
import com.codingwithsaurav.firebasegooglesignin.OAuthService
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import com.google.android.gms.common.api.Scope


class MainActivity : AppCompatActivity() {

    private var mGoogleSignInClient: GoogleSignInClient? = null
    private var binding: ActivityMainBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestScopes(Scope("https://www.googleapis.com/auth/admin.directory.resource.calendar"))
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestServerAuthCode(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso)

        binding?.singInWithGoogle?.setOnClickListener {
            mGoogleSignInClient?.signOut()
            mGoogleSignInClient?.signInIntent?.let {
                Log.w("klnflkadsnflaks", "signInLauncher")
                signInLauncher.launch(it)
            }
        }
    }

    private val signInLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val data = result.data
            if (result.resultCode == RESULT_OK && data != null) {
                Log.w("klnflkadsnflaks", "resultCode ${result.resultCode}")
                val task = GoogleSignIn.getSignedInAccountFromIntent(data)
                handleSignInResult(task)
            } else {
                // Handle unsuccessful sign-in
                Log.e("MainActivity", "Sign in failed")
                Toast.makeText(this, "Sign in failed", Toast.LENGTH_SHORT).show()
            }
        }

    private fun handleSignInResult(completedTask: Task<GoogleSignInAccount>) {
        try {

            val account = completedTask.getResult(ApiException::class.java)
            val authCode = account.serverAuthCode
            Log.w("klnflkadsnflaks", "authCode=" + authCode)
            getRefreshToken(authCode)
            binding?.apply {
                nameTextView.text = account.displayName.toString()
                emailTextView.text = account.email.toString()
            }
            binding?.profileImageView?.let { image ->
                Glide.with(this)
                    .load(account.photoUrl.toString())
                    .into(image)
            }
            // Signed in successfully, show authenticated UI.
            //updateUI(account)
        } catch (e: ApiException) {
            // The ApiException status code indicates the detailed failure reason.
            // Please refer to the GoogleSignInStatusCodes class reference for more information.
            Log.w("klnflkadsnflaks", "signInResult:failed code=" + e.statusCode)
        }
    }


    private fun getRefreshToken(authCode: String?) {
        Log.w("klnflkadsnflaks", "authCode=" + authCode.toString())
        val retrofit = Retrofit.Builder()
            .baseUrl("https://oauth2.googleapis.com")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        val service = retrofit.create(OAuthService::class.java)
        val call = service.getToken(
            code = authCode.toString(),
            clientId = getString(R.string.default_web_client_id),
            clientSecret = getString(R.string.client_secret),
            redirectUri = "",
            grantType = "authorization_code"
        )
        call.enqueue(object : Callback<GoogleTokenResponse> {
            override fun onResponse(
                call: Call<GoogleTokenResponse>,
                response: Response<GoogleTokenResponse>
            ) {
                if (response.isSuccessful) {
                    val tokenResponse = response.body()
                    Log.w(
                        "klnflkadsnflaks",
                        "tokenResponse=" + tokenResponse?.accessToken.toString()
                    )
                    Log.w(
                        "klnflkadsnflaks",
                        "tokenResponse=" + tokenResponse?.refreshToken.toString()
                    )
                    Toast.makeText(
                        this@MainActivity,
                        tokenResponse?.scope.toString(),
                        Toast.LENGTH_SHORT
                    ).show()
                    Log.w("klnflkadsnflaks", "tokenResponse=" + tokenResponse?.scope.toString())
                } else {
                    // Handle error
                }
            }

            override fun onFailure(call: Call<GoogleTokenResponse>, t: Throwable) {
                // Handle failure
            }
        })
    }

}