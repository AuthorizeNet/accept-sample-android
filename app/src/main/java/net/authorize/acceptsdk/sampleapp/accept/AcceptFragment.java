package net.authorize.acceptsdk.sampleapp.accept;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import net.authorize.acceptsdk.AcceptSDKApiClient;
import net.authorize.acceptsdk.datamodel.common.Message;
import net.authorize.acceptsdk.datamodel.merchant.ClientKeyBasedMerchantAuthentication;
import net.authorize.acceptsdk.datamodel.transaction.CardData;
import net.authorize.acceptsdk.datamodel.transaction.EncryptTransactionObject;
import net.authorize.acceptsdk.datamodel.transaction.TransactionObject;
import net.authorize.acceptsdk.datamodel.transaction.TransactionType;
import net.authorize.acceptsdk.datamodel.transaction.callbacks.EncryptTransactionCallback;
import net.authorize.acceptsdk.datamodel.transaction.response.EncryptTransactionResponse;
import net.authorize.acceptsdk.datamodel.transaction.response.ErrorTransactionResponse;
import net.authorize.acceptsdk.sampleapp.R;

/**
 * A simple {@link Fragment} subclass.
 */
public class AcceptFragment extends Fragment
    implements View.OnClickListener, EncryptTransactionCallback {

  public static final String TAG = "WebCheckoutFragment";
  private final String CARD_NUMBER = "4111111111111111";
  private final String EXPIRATION_MONTH = "11";
  private final String EXPIRATION_YEAR = "2017";
  private final String CVV = "256";
  private final String POSTAL_CODE = "98001";
  private final String CLIENT_KEY =
      "6gSuV295YD86Mq4d86zEsx8C839uMVVjfXm9N4wr6DRuhTHpDU97NFyKtfZncUq8";
  // replace with your CLIENT KEY
  private final String API_LOGIN_ID = "6AB64hcB"; // replace with your API LOGIN_ID

  private final int MIN_CARD_NUMBER_LENGTH = 13;
  private final int MIN_YEAR_LENGTH = 2;
  private final int MIN_CVV_LENGTH = 3;
  private final String YEAR_PREFIX = "20";

  private Button checkoutButton;
  private EditText cardNumberView;
  private EditText monthView;
  private EditText yearView;
  private EditText cvvView;

  private ProgressDialog progressDialog;
  private RelativeLayout responseLayout;
  private TextView responseTitle;
  private TextView responseValue;

  private String cardNumber;
  private String month;
  private String year;
  private String cvv;

  private AcceptSDKApiClient apiClient;

  public AcceptFragment() {
    // Required empty public constructor
  }

  @Override public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    /*
       build an Accept SDK Api client to make API calls.
       parameters:
         1) Context - current context
         2) AcceptSDKApiClient.Environment - Authorize.net ENVIRONMENT
    */

    try {
      apiClient = new AcceptSDKApiClient.Builder(getActivity(),
          AcceptSDKApiClient.Environment.SANDBOX).connectionTimeout(
          4000) // optional connection time out in milliseconds
          .build();
    } catch (NullPointerException e) {
      e.printStackTrace();
    }
  }

  @Override public View onCreateView(LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState) {

    View view = inflater.inflate(R.layout.fragment_accept, container, false);
    initialize(view);
    return view;
  }

  private void initialize(View view) {
    cardNumberView = (EditText) view.findViewById(R.id.card_number_view);
    setUpCreditCardEditText();
    monthView = (EditText) view.findViewById(R.id.date_month_view);
    yearView = (EditText) view.findViewById(R.id.date_year_view);
    cvvView = (EditText) view.findViewById(R.id.security_code_view);

    checkoutButton = (Button) view.findViewById(R.id.button_checkout_order);
    checkoutButton.setOnClickListener(this);

    responseLayout = (RelativeLayout) view.findViewById(R.id.response_layout);
    responseTitle = (TextView) view.findViewById(R.id.encrypted_data_title);
    responseValue = (TextView) view.findViewById(R.id.encrypted_data_view);
  }

  @Override public void onClick(View v) {
    if (!areFormDetailsValid()) return;

    progressDialog = ProgressDialog.show(getActivity(), this.getString(R.string.progress_title),
        this.getString(R.string.progress_message), true);
    if (responseLayout.getVisibility() == View.VISIBLE) responseLayout.setVisibility(View.GONE);

    try {
      EncryptTransactionObject transactionObject = prepareTransactionObject();

      /*
        Make a call to get Token API
        parameters:
          1) EncryptTransactionObject - The transactionObject for the current transaction
          2) callback - callback of transaction
       */
      apiClient.getTokenWithRequest(transactionObject, this);
    } catch (NullPointerException e) {
      // Handle exception transactionObject or callback is null.
      Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_LONG).show();
      if (progressDialog.isShowing()) progressDialog.dismiss();
      e.printStackTrace();
    }
  }

  /**
   * prepares a transaction object with dummy data to be used with the Gateway transactions
   */
  private EncryptTransactionObject prepareTransactionObject() {
    ClientKeyBasedMerchantAuthentication merchantAuthentication =
        ClientKeyBasedMerchantAuthentication.
            createMerchantAuthentication(API_LOGIN_ID, CLIENT_KEY);

    // create a transaction object by calling the predefined api for creation
    return TransactionObject.
        createTransactionObject(
            TransactionType.SDK_TRANSACTION_ENCRYPTION) // type of transaction object
        .cardData(prepareCardDataFromFields()) // card data to get Token
        .merchantAuthentication(merchantAuthentication).build();
  }

  private EncryptTransactionObject prepareTestTransactionObject() {
    ClientKeyBasedMerchantAuthentication merchantAuthentication =
        ClientKeyBasedMerchantAuthentication.
            createMerchantAuthentication(API_LOGIN_ID, CLIENT_KEY);

    // create a transaction object by calling the predefined api for creation
    return EncryptTransactionObject.
        createTransactionObject(
            TransactionType.SDK_TRANSACTION_ENCRYPTION) // type of transaction object
        .cardData(prepareTestCardData()) // card data to prepare token
        .merchantAuthentication(merchantAuthentication).build();
  }

  private CardData prepareTestCardData() {
    return new CardData.Builder(CARD_NUMBER, EXPIRATION_MONTH, EXPIRATION_YEAR).cvvCode("")
        .zipCode("")
        .cardHolderName("")
        .build();
  }

/* ---------------------- Callback Methods - Start -----------------------*/

  @Override public void onEncryptionFinished(EncryptTransactionResponse response) {
    hideSoftKeyboard();
    if (responseLayout.getVisibility() != View.VISIBLE) responseLayout.setVisibility(View.VISIBLE);
    if (progressDialog.isShowing()) progressDialog.dismiss();
    responseTitle.setText(R.string.token);
    responseValue.setText(
        getString(R.string.data_descriptor) + response.getDataDescriptor() + "\n" + getString(
            R.string.data_value) + response.getDataValue());
  }

  @Override public void onErrorReceived(ErrorTransactionResponse errorResponse) {
    hideSoftKeyboard();
    if (responseLayout.getVisibility() != View.VISIBLE) responseLayout.setVisibility(View.VISIBLE);
    if (progressDialog.isShowing()) progressDialog.dismiss();
    responseTitle.setText(R.string.error);
    Message error = errorResponse.getFirstErrorMessage();
    String errorString = getString(R.string.code) + error.getMessageCode() + "\n" +
        getString(R.string.message) + error.getMessageText();

    responseValue.setText(errorString);
  }

/* ---------------------- Callback Methods - End -----------------------*/

  public void hideSoftKeyboard() {
    InputMethodManager keyboard =
        (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
    if (getActivity() != null && getActivity().getCurrentFocus() != null) {
      keyboard.hideSoftInputFromInputMethod(getActivity().getCurrentFocus().getWindowToken(), 0);
    }
  }

  private boolean areFormDetailsValid() {
    cardNumber = cardNumberView.getText().toString().replace(" ", "");
    month = monthView.getText().toString();
    cvv = cvvView.getText().toString();
    year = yearView.getText().toString();

    if (isEmptyField()) {
      checkoutButton.startAnimation(
          AnimationUtils.loadAnimation(getActivity(), R.anim.shake_error));
      Toast.makeText(getActivity(), "Empty fields", Toast.LENGTH_LONG).show();
      return false;
    }

    year = YEAR_PREFIX + yearView.getText().toString();

    return validateFields();
  }

  private boolean isEmptyField() {
    return (cardNumber != null && cardNumber.isEmpty()) || (month != null && month.isEmpty()) || (
        year != null
            && year.isEmpty()) || (cvv != null && cvv.isEmpty());
  }

  private boolean validateFields() {
    if (cardNumber.length() < MIN_CARD_NUMBER_LENGTH) {
      cardNumberView.requestFocus();
      cardNumberView.setError(getString(R.string.invalid_card_number));
      checkoutButton.startAnimation(
          AnimationUtils.loadAnimation(getActivity(), R.anim.shake_error));
      return false;
    }
    int monthNum = Integer.parseInt(month);
    if (monthNum < 1 || monthNum > 12) {
      monthView.requestFocus();
      monthView.setError(getString(R.string.invalid_month));
      checkoutButton.startAnimation(
          AnimationUtils.loadAnimation(getActivity(), R.anim.shake_error));
      return false;
    }
    if (month.length() < MIN_YEAR_LENGTH) {
      monthView.requestFocus();
      monthView.setError(getString(R.string.two_digit_month));
      checkoutButton.startAnimation(
          AnimationUtils.loadAnimation(getActivity(), R.anim.shake_error));
      return false;
    }
    if (year.length() < MIN_YEAR_LENGTH) {
      yearView.requestFocus();
      yearView.setError(getString(R.string.invalid_year));
      checkoutButton.startAnimation(
          AnimationUtils.loadAnimation(getActivity(), R.anim.shake_error));
      return false;
    }
    if (cvv.length() < MIN_CVV_LENGTH) {
      cvvView.requestFocus();
      cvvView.setError(getString(R.string.invalid_cvv));
      checkoutButton.startAnimation(
          AnimationUtils.loadAnimation(getActivity(), R.anim.shake_error));
      return false;
    }
    return true;
  }

  private void setUpCreditCardEditText() {
    cardNumberView.addTextChangedListener(new TextWatcher() {
      private boolean spaceDeleted;

      public void onTextChanged(CharSequence s, int start, int before, int count) {
      }

      public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        // check if a space was deleted
        CharSequence charDeleted = s.subSequence(start, start + count);
        spaceDeleted = " ".equals(charDeleted.toString());
      }

      public void afterTextChanged(Editable editable) {
        // disable text watcher
        cardNumberView.removeTextChangedListener(this);

        // record cursor position as setting the text in the textview
        // places the cursor at the end
        int cursorPosition = cardNumberView.getSelectionStart();
        String withSpaces = formatText(editable);
        cardNumberView.setText(withSpaces);
        // set the cursor at the last position + the spaces added since the
        // space are always added before the cursor
        cardNumberView.setSelection(cursorPosition + (withSpaces.length() - editable.length()));

        // if a space was deleted also deleted just move the cursor
        // before the space
        if (spaceDeleted) {
          cardNumberView.setSelection(cardNumberView.getSelectionStart() - 1);
          spaceDeleted = false;
        }

        // enable text watcher
        cardNumberView.addTextChangedListener(this);
      }

      private String formatText(CharSequence text) {
        StringBuilder formatted = new StringBuilder();
        int count = 0;
        for (int i = 0; i < text.length(); ++i) {
          if (Character.isDigit(text.charAt(i))) {
            if (count % 4 == 0 && count > 0) formatted.append(" ");
            formatted.append(text.charAt(i));
            ++count;
          }
        }
        return formatted.toString();
      }
    });
  }

  private CardData prepareCardDataFromFields() {
    return new CardData.Builder(cardNumber, month, year).cvvCode(cvv) //CVV Code is optional
        .build();
  }
}
