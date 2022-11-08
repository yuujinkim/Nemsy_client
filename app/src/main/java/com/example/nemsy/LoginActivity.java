package com.example.nemsy;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Objects;

public class LoginActivity extends AppCompatActivity {

    TextView loginError ;
    EditText et_password ;
    EditText et_email ;
    Button btn_login ;
    Button btn_register ;
    ToggleButton showPassword ;

    private FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        firebaseAuth = FirebaseAuth.getInstance();

        ActionBar actionBar = getSupportActionBar();
        actionBar.hide();

        editTextWatcher();
        clickEyeButton();
        initSignUp();
        initLogin();
    }

    private void editTextWatcher(){
        loginError = (TextView) findViewById(R.id.tv_error);
        et_password = (EditText) findViewById(R.id.et_password);
        // EditText 입력 변화 이벤트 처리
        et_password.addTextChangedListener(new TextWatcher() { // 아이디
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                loginError.setVisibility(View.INVISIBLE);
            }
            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private void clickEyeButton(){
        showPassword= (ToggleButton) findViewById(R.id.tbtn_eye);
        et_password = (EditText) findViewById(R.id.et_password);
        // 눈모양 토글 버튼 클릭 시 눈 이미지 변경 및 비밀번호 보이기/숨기기
        showPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean isClicked = ((ToggleButton) view).isChecked();

                if (isClicked) {
                    showPassword.setBackgroundDrawable(getResources().getDrawable(R.drawable.eye_visible));
                    et_password.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                } else {
                    showPassword.setBackgroundDrawable(getResources().getDrawable(R.drawable.eye_invisible));
                    et_password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                }
            }
        });
    }

    private void initSignUp(){
        btn_register = (Button) findViewById(R.id.btn_register);
        btn_register.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(LoginActivity.this, SignUpActivity.class);
                startActivity(intent);
            }
        });
    }

    private void initLogin(){
        et_password = (EditText) findViewById(R.id.et_password);
        et_email = (EditText) findViewById(R.id.et_email);
        btn_login = (Button) findViewById(R.id.btn_login);

        btn_login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = et_email.getText().toString().trim();
                String password = et_password.getText().toString().trim();

//                FirebaseDatabase database = FirebaseDatabase.getInstance();
//                DatabaseReference reference = database.getReference("Users");
//
//                reference.addValueEventListener(new ValueEventListener() {
//                    @Override
//                    public void onDataChange(@NonNull DataSnapshot snapshot) {
//                        String value = snapshot.getValue().toString();
//                        Log.d("Database", "Value is: " + value);
//                        for (DataSnapshot userSnapshot: snapshot.getChildren()) {
//                            String key = userSnapshot.getKey();
//                            Log.d("Database", "key: " + key);
//                            HashMap<String, HashMap<String, Object>> userInfo = (HashMap<String, HashMap<String, Object>>) userSnapshot.getValue();
//                            Log.d("Database", "value: " + userInfo);
//                            Log.d("Database", "value: " + userInfo.get("password"));
//
////                            String[] getData = {userInfo.get("uid").get("email").toString(), userInfo.get("uid").get("password").toString()};
////                            Log.d("Database", "getData[0]: " + getData[0]);
//                        }
//                    }
//
//                    @Override
//                    public void onCancelled(@NonNull DatabaseError error) {
//
//                    }
//                });

                firebaseAuth.signInWithEmailAndPassword(email,password).addOnCompleteListener(LoginActivity.this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            Intent intent = new Intent(LoginActivity.this, MainActivity.class); // 일단 Main으로 intent
//                            firebaseAuth.addAuthStateListener(firebaseAuthListener);
                            startActivity(intent);
                        }else{
                            loginError = (TextView) findViewById(R.id.tv_error);
                            loginError.setVisibility(View.VISIBLE);
                        }
                    }
                });
            }
        });
    }

}