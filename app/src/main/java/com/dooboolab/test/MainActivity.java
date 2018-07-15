package com.dooboolab.test;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.kakao.auth.AuthType;
import com.kakao.auth.ISessionCallback;
import com.kakao.auth.KakaoSDK;
import com.kakao.auth.Session;
import com.kakao.network.ErrorResult;
import com.kakao.usermgmt.UserManagement;
import com.kakao.usermgmt.LoginButton;
import com.kakao.usermgmt.callback.LogoutResponseCallback;
import com.kakao.usermgmt.callback.MeResponseCallback;
import com.kakao.usermgmt.response.model.UserProfile;
import com.kakao.util.exception.KakaoException;
import com.kakao.util.helper.log.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{
  private String TAG = "MainActivity";
  // private LoginButton btnKakaoLogin;
  private Button btnLogin;
  private Button btnLogout;
  private Button btnProfile;
  private SessionCallback callback;
  private UserManagement userManagement;

  private static class Item {
    final int textId;
    public final int icon;
    final int contentDescId;
    final AuthType authType;
    Item(final int textId, final Integer icon, final int contentDescId, final AuthType authType) {
      this.textId = textId;
      this.icon = icon;
      this.contentDescId = contentDescId;
      this.authType = authType;
    }
  }

  private List<AuthType> getAuthTypes() {
    final List<AuthType> availableAuthTypes = new ArrayList<>();
    if (Session.getCurrentSession().getAuthCodeManager().isTalkLoginAvailable()) {
      availableAuthTypes.add(AuthType.KAKAO_TALK);
    }
    if (Session.getCurrentSession().getAuthCodeManager().isStoryLoginAvailable()) {
      availableAuthTypes.add(AuthType.KAKAO_STORY);
    }
    availableAuthTypes.add(AuthType.KAKAO_ACCOUNT);

    AuthType[] authTypes = KakaoSDK.getAdapter().getSessionConfig().getAuthTypes();
    if (authTypes == null || authTypes.length == 0 || (authTypes.length == 1 && authTypes[0] == AuthType.KAKAO_LOGIN_ALL)) {
      authTypes = AuthType.values();
    }
    availableAuthTypes.retainAll(Arrays.asList(authTypes));

    // 개발자가 설정한 것과 available 한 타입이 없다면 직접계정 입력이 뜨도록 한다.
    if(availableAuthTypes.size() == 0){
      availableAuthTypes.add(AuthType.KAKAO_ACCOUNT);
    }

    return availableAuthTypes;
  }

  private Item[] createAuthItemArray(final List<AuthType> authTypes) {
    final List<Item> itemList = new ArrayList<Item>();
    if(authTypes.contains(AuthType.KAKAO_TALK)) {
      itemList.add(new Item(com.kakao.usermgmt.R.string.com_kakao_kakaotalk_account, com.kakao.usermgmt.R.drawable.talk, com.kakao.usermgmt.R.string.com_kakao_kakaotalk_account_tts, AuthType.KAKAO_TALK));
    }
    if(authTypes.contains(AuthType.KAKAO_STORY)) {
      itemList.add(new Item(com.kakao.usermgmt.R.string.com_kakao_kakaostory_account, com.kakao.usermgmt.R.drawable.story, com.kakao.usermgmt.R.string.com_kakao_kakaostory_account_tts, AuthType.KAKAO_STORY));
    }
    if(authTypes.contains(AuthType.KAKAO_ACCOUNT)){
      itemList.add(new Item(com.kakao.usermgmt.R.string.com_kakao_other_kakaoaccount, com.kakao.usermgmt.R.drawable.account, com.kakao.usermgmt.R.string.com_kakao_other_kakaoaccount_tts, AuthType.KAKAO_ACCOUNT));
    }

    return itemList.toArray(new Item[itemList.size()]);
  }

  @SuppressWarnings("deprecation")
  private ListAdapter createLoginAdapter(final Item[] authItems) {
    /*
      가능한 auth type들을 유저에게 보여주기 위한 준비.
     */
    return new ArrayAdapter<Item>(
        this,
        android.R.layout.select_dialog_item,
        android.R.id.text1, authItems){
      @Override
      public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
          LayoutInflater inflater = (LayoutInflater) getContext()
              .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
          convertView = inflater.inflate(com.kakao.usermgmt.R.layout.layout_login_item, parent, false);
        }
        ImageView imageView = convertView.findViewById(com.kakao.usermgmt.R.id.login_method_icon);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
          imageView.setImageDrawable(getResources().getDrawable(authItems[position].icon, getContext().getTheme()));
        } else {
          imageView.setImageDrawable(getResources().getDrawable(authItems[position].icon));
        }
        TextView textView = convertView.findViewById(com.kakao.usermgmt.R.id.login_method_text);
        textView.setText(authItems[position].textId);
        return convertView;
      }
    };
  }

  /**
   * 실제로 유저에게 보여질 dialog 객체를 생성한다.
   * @param authItems 가능한 AuthType들의 정보를 담고 있는 Item array
   * @param adapter Dialog의 list view에 쓰일 adapter
   * @return 로그인 방법들을 팝업으로 보여줄 dialog
   */
  private Dialog createLoginDialog(final Item[] authItems, final ListAdapter adapter) {
    final Dialog dialog = new Dialog(this, com.kakao.usermgmt.R.style.LoginDialog);
    dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
    dialog.setContentView(com.kakao.usermgmt.R.layout.layout_login_dialog);
    if (dialog.getWindow() != null) {
      dialog.getWindow().setGravity(Gravity.CENTER);
    }

//        TextView textView = (TextView) dialog.findViewById(R.id.login_title_text);
//        Typeface customFont = Typeface.createFromAsset(getContext().getAssets(), "fonts/KakaoOTFRegular.otf");
//        if (customFont != null) {
//            textView.setTypeface(customFont);
//        }

    ListView listView = dialog.findViewById(com.kakao.usermgmt.R.id.login_list_view);
    listView.setAdapter(adapter);
    listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
      @Override
      public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        final AuthType authType = authItems[position].authType;
        if (authType != null) {
          openSession(authType);
        }
        dialog.dismiss();
      }
    });

    Button closeButton = dialog.findViewById(com.kakao.usermgmt.R.id.login_close_button);
    closeButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        dialog.dismiss();
      }
    });
    return dialog;
  }

  public void openSession(final AuthType authType) {
    Session.getCurrentSession().open(authType, this);
  }

  /**
   * 로그인 버튼을 클릭 했을시 access token을 요청하도록 설정한다.
   *
   * @param savedInstanceState 기존 session 정보가 저장된 객체
   */
  @Override
  protected void onCreate(final Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

//    try {
//      PackageInfo info = getPackageManager().getPackageInfo("com.dooboolab.test", PackageManager.GET_SIGNATURES);
//      for (Signature signature : info.signatures) {
//        MessageDigest md = MessageDigest.getInstance("SHA");
//        md.update(signature.toByteArray());
//        Log.d("KeyHash:", Base64.encodeToString(md.digest(), Base64.DEFAULT));
//      }
//    } catch (PackageManager.NameNotFoundException e) {
//      e.printStackTrace();
//    } catch (NoSuchAlgorithmException e) {
//      e.printStackTrace();
//    }

    // btnKakaoLogin = findViewById(R.id.btn_kakao_login);
    btnLogin = findViewById(R.id.btn_login);
    btnLogout = findViewById(R.id.btn_logout);
    btnProfile = findViewById(R.id.btn_profile);

    btnLogin.setOnClickListener(this);
    btnLogout.setOnClickListener(this);
    btnProfile.setOnClickListener(this);

    callback = new SessionCallback();

    Session.getCurrentSession().addCallback(callback);
    Session.getCurrentSession().checkAndImplicitOpen();
  }

  @Override
  public void onClick(View v) {
    switch (v.getId()) {
      case R.id.btn_login:
        // Session.getCurrentSession().open(AuthType.KAKAO_TALK, MainActivity.this);
        // btnKakaoLogin.callOnClick();
        final List<AuthType> authTypes = getAuthTypes();
        if (authTypes.size() == 1) {
          Session.getCurrentSession().open(authTypes.get(0), this);
        } else {
          final Item[] authItems = createAuthItemArray(authTypes);
          ListAdapter adapter = createLoginAdapter(authItems);
          final Dialog dialog = createLoginDialog(authItems, adapter);
          dialog.show();
        }
        break;
      case R.id.btn_logout:
        Log.d(TAG, "btnLogout clicked");
        UserManagement.getInstance().requestLogout(new LogoutResponseCallback() {
          @Override
          public void onSessionClosed(ErrorResult errorResult) {
            Log.d(TAG, "sessionClosed!!\n" + errorResult.toString());
          }
          @Override
          public void onNotSignedUp() {
            Log.d(TAG, "NotSignedUp!!");
          }
          @Override
          public void onSuccess(Long result) {
            Toast.makeText(MainActivity.this, "Logout!", Toast.LENGTH_SHORT).show();
          }
          @Override
          public void onCompleteLogout() {
            Toast.makeText(MainActivity.this, "Logout!", Toast.LENGTH_SHORT).show();
          }
        });
        break;
      case R.id.btn_profile:
        requestMe();
        break;
    }
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (Session.getCurrentSession().handleActivityResult(requestCode, resultCode, data)) {
      Log.d(TAG, "requestCode: " + requestCode + ", resultCode: " + resultCode);
      return;
    }

    super.onActivityResult(requestCode, resultCode, data);
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    Session.getCurrentSession().removeCallback(callback);
  }

  private class SessionCallback implements ISessionCallback {

    @Override
    public void onSessionOpened() {
      Log.d(TAG, "onSessionOpen");
      if (Session.getCurrentSession().getTokenInfo() != null) {
        Toast.makeText(
            MainActivity.this,
            "Logged in!\ntoken: " + Session.getCurrentSession().getAccessToken(),
            Toast.LENGTH_SHORT
        ).show();
      }
    }

    @Override
    public void onSessionOpenFailed(KakaoException exception) {
      Log.d(TAG, "onSessionOpenFailed");
      if(exception != null) {
        Logger.e(exception);
      }
    }
  }

  private void requestMe() {
    Log.d(TAG, "requestMe");
    UserManagement.getInstance().requestMe(new MeResponseCallback() {
      @Override
      public void onFailure(ErrorResult errorResult) {
        String message = "failed to get user info. msg=" + errorResult;
        Log.e(TAG, message);
      }

      @Override
      public void onSessionClosed(ErrorResult errorResult) {
        Log.e(TAG, "seesionClosed");
      }

      @Override
      public void onSuccess(UserProfile userProfile) {
        Toast.makeText(MainActivity.this, "userProfile!\n" + userProfile.toString(), Toast.LENGTH_SHORT).show();
      }

      @Override
      public void onNotSignedUp() {
        Log.e(TAG, "NotSignedUp");
      }
    });
  }
}
