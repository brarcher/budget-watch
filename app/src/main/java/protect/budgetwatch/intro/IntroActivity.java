package protect.budgetwatch.intro;

import android.os.Bundle;
import androidx.annotation.LayoutRes;
import androidx.fragment.app.Fragment;

import com.github.paolorotolo.appintro.AppIntro;

public class IntroActivity extends AppIntro
{
    @Override
    public void init(Bundle savedInstanceState)
    {
        addSlide(new IntroSlide1());
        addSlide(new IntroSlide2());
        addSlide(new IntroSlide3());
        addSlide(new IntroSlide4());
        addSlide(new IntroSlide5());
        addSlide(new IntroSlide6());
        addSlide(new IntroSlide7());
    }

    @Override
    public void onSkipPressed(Fragment fragment) {
        finish();
    }

    @Override
    public void onDonePressed(Fragment fragment) {
        finish();
    }
}


