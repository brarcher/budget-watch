package protect.budgetwatch;

import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.Toast;

public class PasswordCreationActivity extends AppCompatActivity {

    private EditText _passwordEdit;
    private EditText _passwordConfirmEdit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.password_creation_activity);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        final PasswordManager pm = new PasswordManager(this);
        final Switch _usePasswordSwitch = findViewById(R.id.usePassword_switch);
        final Button _createPasswordButton = findViewById(R.id.createPassword_Button);
        _passwordEdit = findViewById(R.id.newPassword);
        _passwordConfirmEdit = findViewById(R.id.confirmPassword);

        _usePasswordSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                _passwordEdit.setEnabled(b);
                _passwordConfirmEdit.setEnabled(b);
                _createPasswordButton.setEnabled(b);
                if (!b) {
                    pm.clearPassword();
                    Toast.makeText(getApplicationContext(), R.string.passwordClearedMessage,
                            Toast.LENGTH_LONG).show();
                }
            }
        });

        _createPasswordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (IsValidePassword()) {
                    pm.setPassword(_passwordEdit.getText().toString());
                    Toast.makeText(getApplicationContext(), R.string.passwordCreatedMessage,
                            Toast.LENGTH_LONG).show();
                    finish();
                }
            }
        });

        if (pm.isPasswordEnabled()) {
            _createPasswordButton.setText(R.string.changePasswordButton);
            _usePasswordSwitch.setChecked(true);
        }

    }

    private boolean IsValidePassword() {
        String password1 = _passwordEdit.getText().toString();
        String password2 = _passwordConfirmEdit.getText().toString();

        if (TextUtils.isEmpty(password1))
            _passwordEdit.setError(getString(R.string.error_field_required));
        else if (TextUtils.isEmpty(password2))
            _passwordConfirmEdit.setError(getString(R.string.error_field_required));
        else if (!password1.equals(password2))
            _passwordConfirmEdit.setError(getString(R.string.passwords_mismatch));
        else
            return true;

        return false;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

}
