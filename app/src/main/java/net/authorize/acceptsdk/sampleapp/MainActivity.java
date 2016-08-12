package net.authorize.acceptsdk.sampleapp;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import java.math.BigDecimal;
import net.authorize.acceptsdk.sampleapp.accept.AcceptCheckoutFragment;
import net.authorize.acceptsdk.sampleapp.androidpay.ItemInfo;
import net.authorize.acceptsdk.sampleapp.androidpay.MposTransaction;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{
  private static final String TAG_FRAGMENT_CHECKOUT = "TAG_FRAGMENT_CHECKOUT";
  private Button androidPayButton;
  private EditText amountView;
  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    amountView = (EditText) findViewById(R.id.amountView);
    androidPayButton = (Button) findViewById(R.id.pay_with_google);
    androidPayButton.setOnClickListener(this);
   // pullWebCheckoutFragment();
  }
  @Override
  public void onClick(View v) {
    switch (v.getId()) {
      case R.id.pay_with_google:
        //double amount = stringAmountToDouble();
        double amount = stringAmountToDouble();
        if(amount > 0.0) {
          MposTransaction.getInstance().setItemInfo(
              new ItemInfo("Simple item", "Features", amount, 0,
                  Constants.CURRENCY_CODE_USD, "seller data 0", 0));
          moveToCheckoutActivity();
        }
        else
          Toast.makeText(this, "Please enter amount", Toast.LENGTH_LONG).show();
        break;
    }
  }

  public void moveToCheckoutActivity(){
    Intent intent = new Intent(this, CheckoutActivity.class);
    startActivity(intent);
  }

  private double stringAmountToDouble() {
    if(amountView.getText().toString().compareTo("") == 0)
      return 0.0;
    else
      return Double.parseDouble(amountView.getText().toString());
  }

  private BigDecimal stringAmountToLong() {
    if(amountView.getText().toString().compareTo("") == 0)
      return BigDecimal.ZERO;
    else
      return BigDecimal.valueOf(Double.valueOf(amountView.getText().toString()));
  }


  private void pullWebCheckoutFragment() {
    //FragmentManager fragmentManager = getSupportFragmentManager();
    //AcceptCheckoutFragment checkoutFragment =
    //    (AcceptCheckoutFragment) fragmentManager.findFragmentByTag(TAG_FRAGMENT_CHECKOUT);
    //if (checkoutFragment == null) {
    //  checkoutFragment = new AcceptCheckoutFragment();
    //  fragmentManager.beginTransaction()
    //      .replace(R.id.accept_checkout_fragment_container, checkoutFragment, TAG_FRAGMENT_CHECKOUT)
    //      .commit();
    //}
  }

  @Override public void onConfigurationChanged(Configuration newConfig) {
    super.onConfigurationChanged(newConfig);
  }
}
