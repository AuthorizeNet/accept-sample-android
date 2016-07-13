package net.authorize.acceptsdk.sampleapp.fragments;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
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
import net.authorize.acceptsdk.AcceptInvalidCardException;
import net.authorize.acceptsdk.AcceptSDKApiClient;
import net.authorize.acceptsdk.datamodel.error.AcceptError;
import net.authorize.acceptsdk.datamodel.merchant.ClientKeyBasedMerchantAuthentication;
import net.authorize.acceptsdk.datamodel.transaction.CardData;
import net.authorize.acceptsdk.datamodel.transaction.EncryptTransactionObject;
import net.authorize.acceptsdk.datamodel.transaction.TransactionType;
import net.authorize.acceptsdk.datamodel.transaction.callbacks.EncryptTransactionCallback;
import net.authorize.acceptsdk.datamodel.transaction.response.EncryptTransactionResponse;
import net.authorize.acceptsdk.sampleapp.R;

/**
 * A simple {@link Fragment} subclass.
 */
public class AcceptCheckoutFragment extends Fragment
    implements View.OnClickListener, EncryptTransactionCallback {

  public static final String TAG = "WebCheckoutFragment";
  private final String ACCOUNT_NUMBER = "4111111111111111";
  private final String EXPIRATION_MONTH = "11";
  private final String EXPIRATION_YEAR = "2017";
  private final String CVV = "256";
  private final String POSTAL_CODE = "98001";
  private final String CLIENT_KEY =
      "6gSuV295YD86Mq4d86zEsx8C839uMVVjfXm9N4wr6DRuhTHpDU97NFyKtfZncUq81111";
  private final String API_LOGIN_ID = "6AB64hcB"; // replace with YOUR_API_LOGIN_ID

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

  public AcceptCheckoutFragment() {
    // Required empty public constructor
  }

  @Override public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    //         build an Accept SDK Api client to make API calls.
    //         parameters:
    //         1) Context - current context
    //         2) AcceptSDKApiClient.Environment - Authorize.net ENVIRONMENT
    apiClient = new AcceptSDKApiClient.Builder(getActivity(),
        AcceptSDKApiClient.Environment.SANDBOX).setConnectionTimeout(
        4000) // optional connection time out in milliseconds
        .build();
  }

  @Override public View onCreateView(LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState) {

    View view = inflater.inflate(R.layout.fragment_accept_checkout, container, false);
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
    preFillLayoutWithDummyData();
  }

  //TODO: This is only for testing purpose need to remove in final code.
  private void preFillLayoutWithDummyData() {
    cardNumberView.setText(ACCOUNT_NUMBER);
    monthView.setText(EXPIRATION_MONTH);
    yearView.setText(EXPIRATION_YEAR);
    cvvView.setText(CVV);
  }

  @Override public void onClick(View v) {
    if (!areFormDetailsValid()) return;

    progressDialog =
        ProgressDialog.show(getActivity(), "Please Wait", "Encrypting Card Data...", true);
    if (responseLayout.getVisibility() == View.VISIBLE) responseLayout.setVisibility(View.GONE);

    try {
      EncryptTransactionObject transactionObject = prepareTransactionObject();
      // make a call to encryption to API
      // parameters:
      // 1) EncryptTransactionObject - The transactionObject for the current transaction
      // 2) callback - callback of transaction
      apiClient.performEncryption(transactionObject, this);
    } catch (NullPointerException e) {
      Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_LONG).show();
      if (progressDialog.isShowing()) progressDialog.dismiss();
      e.printStackTrace();
    } catch (AcceptInvalidCardException e) {
      // Handle exception if the card is invalid
      Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_LONG).show();
      if (progressDialog.isShowing()) progressDialog.dismiss();

      e.printStackTrace();
    }
  }

  private boolean areFormDetailsValid() {
    cardNumber = cardNumberView.getText().toString().replace(" ", "");
    month = monthView.getText().toString();
    year = YEAR_PREFIX + yearView.getText().toString();
    cvv = cvvView.getText().toString();

    if (isEmptyField()) {
      checkoutButton.startAnimation(
          AnimationUtils.loadAnimation(getActivity(), R.anim.shake_error));
      Toast.makeText(getActivity(), "Empty fields", Toast.LENGTH_LONG).show();
      return false;
    }
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

  private EncryptTransactionObject prepareTestTransactionObject()
      throws AcceptInvalidCardException {
    ClientKeyBasedMerchantAuthentication merchantAuthentication =
        ClientKeyBasedMerchantAuthentication.
            createMerchantAuthentication(API_LOGIN_ID, CLIENT_KEY);

    // create a transaction object by calling the predefined api for creation
    return EncryptTransactionObject.
        createTransactionObject(
            TransactionType.SDK_TRANSACTION_ENCRYPTION) // type of transaction object
        .cardData(prepareCardDataFromFields()) // card data to be encrypted
        .merchantAuthentication(merchantAuthentication).build();
  }

  private CardData prepareTestCardData() throws AcceptInvalidCardException {
    return new CardData.Builder(ACCOUNT_NUMBER, EXPIRATION_MONTH, EXPIRATION_YEAR).build();
  }

  private CardData prepareCardDataFromFields() throws AcceptInvalidCardException {
    return new CardData.Builder(cardNumber, month, year).build();
  }

  /**
   * prepares a transaction object with dummy data to be used with the Gateway transactions
   */
  private EncryptTransactionObject prepareTransactionObject() throws AcceptInvalidCardException {
    ClientKeyBasedMerchantAuthentication merchantAuthentication =
        ClientKeyBasedMerchantAuthentication.
            createMerchantAuthentication(API_LOGIN_ID, CLIENT_KEY);

    // create a transaction object by calling the predefined api for creation
    return EncryptTransactionObject.
        createTransactionObject(
            TransactionType.SDK_TRANSACTION_ENCRYPTION) // type of transaction object
        .cardData(prepareCardDataFromFields()) // card data to be encrypted
        .merchantAuthentication(merchantAuthentication).build();
  }

  public void hideSoftKeyboard() {
    if (getActivity().getCurrentFocus() != null) {
      InputMethodManager imm =
          (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
      imm.hideSoftInputFromInputMethod(getActivity().getCurrentFocus().getWindowToken(), 0);
    }
  }

  @Override public void onEncryptionFinished(EncryptTransactionResponse response) {
    hideSoftKeyboard();
    if (responseLayout.getVisibility() != View.VISIBLE) responseLayout.setVisibility(View.VISIBLE);
    if (progressDialog.isShowing()) progressDialog.dismiss();
    responseTitle.setText(R.string.encrypted_card_data);
    responseValue.setText(getString(R.string.encrypted_data) + response.getDataValue());
  }

  @Override public void onErrorReceived(AcceptError error) {
    hideSoftKeyboard();
    if (responseLayout.getVisibility() != View.VISIBLE) responseLayout.setVisibility(View.VISIBLE);
    if (progressDialog.isShowing()) progressDialog.dismiss();
    responseTitle.setText(R.string.error);
    responseValue.setText(
        getString(R.string.code) + error.getErrorCode() + "\n" + getString(R.string.message) + error
            .getErrorMessage()
            .toString() + "\n" + getString(R.string.extra_message));
  }
}
