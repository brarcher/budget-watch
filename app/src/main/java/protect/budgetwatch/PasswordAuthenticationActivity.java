package protect.budgetwatch;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class PasswordAuthenticationActivity extends AppCompatActivity
{


    // UI references.
    private EditText _passwordEditText;
    private PasswordManager _pm;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.password_authentication_activity);

        _pm = new PasswordManager(this);
        _passwordEditText = findViewById(R.id.password);
        Button enterButton = findViewById(R.id.enter_button);

        _passwordEditText.setOnEditorActionListener(new TextView.OnEditorActionListener()
        {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent)
            {
                if (id == EditorInfo.IME_ACTION_DONE || id == EditorInfo.IME_NULL)
                {
                    attemptLogin();
                    return true;
                }
                return false;
            }
        });

        enterButton.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                attemptLogin();
            }
        });
    }


    private void attemptLogin()
    {
        // Reset errors.
        _passwordEditText.setError(null);
        String password = _passwordEditText.getText().toString();

        View focusView = _passwordEditText;
        if (TextUtils.isEmpty(password))
        {
            _passwordEditText.setError(getString(R.string.error_field_required));
            focusView.requestFocus();
        } else if (!_pm.isPasswordCorrect(password))
        {
            _passwordEditText.setError(getString(R.string.error_incorrect_password));
            focusView.requestFocus();
        } else
        {
            setResult(RESULT_OK);
            finish();
        }
    }

}

