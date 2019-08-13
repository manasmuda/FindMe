package com.msm.findme;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentContainer;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.login.LoginResult;
import com.facebook.FacebookSdk;
import com.facebook.appevents.AppEventsLogger;
import com.facebook.login.widget.LoginButton;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Arrays;

public class MainActivity extends AppCompatActivity {

    private Button enterbutton;
    private RelativeLayout splashlayout;
    private RelativeLayout loginlayout;
    private ImageButton googlesigninbutton;
    private ImageButton fbsigninbutton;
    private LoginButton fblb;

    private GoogleApiClient googleApiClient;
    private GoogleSignInOptions gso;

    private GoogleSignInClient googleSignInClient;

    private CallbackManager mCallbackManager;

    private Intent homeintent;

    private FirebaseAuth fmauth=FirebaseAuth.getInstance();
    private FirebaseAuth.AuthStateListener fmauthlistener;

    private FirebaseFirestore fmfirestore;

    private final static int G_RC_SIGN_IN=101;

    @Override
    protected void onStart() {
        fmauth=FirebaseAuth.getInstance();
        super.onStart();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == G_RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account);
            } catch (ApiException e) {
                Log.w("gsi", "Google sign in failed", e);
            }
        }
        else {
            if(requestCode==5) {
                if(resultCode==RESULT_OK) {
                    fmauth.signOut();
                }
                else {
                    MainActivity.this.finish();
                }
            }
            else {
                mCallbackManager.onActivityResult(requestCode, resultCode, data);
            }
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //FacebookSdk.sdkInitialize(getApplicationContext());
        //AppEventsLogger.activateApp(this);

        enterbutton=findViewById(R.id.enterbutton);
        splashlayout=findViewById(R.id.splashlayout);
        loginlayout=findViewById(R.id.loginlayout);
        googlesigninbutton=findViewById(R.id.googlesigninbutton);
        fbsigninbutton=findViewById(R.id.fbesigninbutton);
        fblb=findViewById(R.id.fblb);
        splashlayout.setVisibility(View.VISIBLE);

        fmauth=FirebaseAuth.getInstance();

        homeintent = new Intent();
        homeintent.setClass(MainActivity.this, HomeActivity.class);

        mCallbackManager=CallbackManager.Factory.create();

        gso=new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).requestIdToken("21916541962-5himau3v83f6ces0jjo09n3re2b3f3gr.apps.googleusercontent.com").requestEmail().build();


        googleApiClient=new GoogleApiClient.Builder(this).enableAutoManage(this, new GoogleApiClient.OnConnectionFailedListener() {
            @Override
            public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
                Toast.makeText(MainActivity.this,connectionResult.getErrorMessage() ,Toast.LENGTH_LONG).show();;
            }
        }).addApi(Auth.GOOGLE_SIGN_IN_API, gso).build();

        googleSignInClient=GoogleSignIn.getClient(this,gso);

        enterbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(fmauth.getCurrentUser()!=null){
                    updateUI(fmauth.getCurrentUser());
                }
                splashlayout.setVisibility(View.GONE);
            }
        });

        googlesigninbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                GoogleSignIn();
            }
        });


        fbsigninbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fblb.performClick();
            }
        });

        fblb.setReadPermissions(Arrays.asList("email"));
        fblb.registerCallback(mCallbackManager,new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                Log.d("fbl", "facebook:onSuccess:" + loginResult);
                handleFacebookAccessToken(loginResult.getAccessToken());
            }

            @Override
            public void onCancel() {
                Log.d("fbl", "facebook:onCancel");
                updateUI(null);
            }

            @Override
            public void onError(FacebookException error) {
                Log.d("fbl", "facebook:onError", error);
                updateUI(null);
            }
        });

    }

    public void GoogleSignIn(){
        Intent googlesigninintent=googleSignInClient.getSignInIntent();
        startActivityForResult(googlesigninintent,G_RC_SIGN_IN );
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount acct) {
        Log.d("GSI", "firebaseAuthWithGoogle:" + acct.getId());

        //AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        fmauth.signInWithCredential(GoogleAuthProvider.getCredential(acct.getIdToken(), null)).addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            Log.d("gsi", "signInWithCredential:success");
                            FirebaseUser user = fmauth.getCurrentUser();
                            updateUI(user);
                        } else {
                            Log.w("gsi", "signInWithCredential:failure", task.getException());
                            updateUI(null);
                        }

                    }
                });
    }

    public void handleFacebookAccessToken(AccessToken token) {
        Log.d("fbl", "handleFacebookAccessToken:" + token);

        AuthCredential credential = FacebookAuthProvider.getCredential(token.getToken());
        fmauth.signInWithCredential(credential).addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            Log.d("fbl", "signInWithCredential:success");
                            FirebaseUser user = fmauth.getCurrentUser();
                            updateUI(user);
                        } else {
                            Log.w("fbl", "signInWithCredential:failure", task.getException());
                            Toast.makeText(MainActivity.this, "Authentication failed.", Toast.LENGTH_SHORT).show();
                            updateUI(null);
                        }
                    }
                });
    }

    public void updateUI(FirebaseUser user){
        if(user!=null) {
            Log.i("gsi","123" );
            homeintent.putExtra("user",user);
            startActivityForResult(homeintent,5);
        }
    }
}
