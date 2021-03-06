package com.dsource.idc.jellowintl;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.method.KeyListener;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.dsource.idc.jellowintl.models.LevelTwoVerbiageModel;
import com.dsource.idc.jellowintl.utility.ChangeAppLocale;
import com.dsource.idc.jellowintl.utility.DefaultExceptionHandler;
import com.dsource.idc.jellowintl.utility.IndexSorter;
import com.dsource.idc.jellowintl.utility.SessionManager;
import com.dsource.idc.jellowintl.utility.ToastWithCustomTime;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.StringTokenizer;

import static com.dsource.idc.jellowintl.utility.Analytics.bundleEvent;
import static com.dsource.idc.jellowintl.utility.Analytics.getAnalytics;
import static com.dsource.idc.jellowintl.utility.Analytics.singleEvent;
import static com.dsource.idc.jellowintl.utility.Analytics.startMeasuring;
import static com.dsource.idc.jellowintl.utility.Analytics.stopMeasuring;

public class LevelTwoActivity extends AppCompatActivity {
    private final int MY_PERMISSIONS_REQUEST_CALL_PHONE = 0;
    private final int REQ_HOME = 0;
    private final int CATEGORY_ICON_PEOPLE = 5;
    private final int CATEGORY_ICON_PLACES = 6;
    private final int CATEGORY_ICON_HELP = 8;
    private final boolean DISABLE_EXPR_BTNS = true;

    /* This flags are used to identify respective expressive button is pressed either
      once or twice. eg. mFlgLike used to identify Like expressive button pressed once or twice.*/
    private int mFlgLike = 0, mFlgYes = 0, mFlgMore = 0, mFlgDntLike = 0, mFlgNo = 0,
            mFlgLess = 0;
    /* This flag identifies which expressive button is pressed.*/
    private int mFlgImage = -1;
    /* This flag indicates custom keyboard input text layout is open or not, 0 indicates is not open.*/
    private int mFlgKeyboard = 0;
    /* This flag identifies that user is pressed a category icon and which border should appear
      on pressed category icon. If flag value = 0, then brown (initial border) will appear.*/
    private int mActionBtnClickCount;
    /*Image views which are visible on the layout such as six expressive buttons, below navigation
      buttons and speak button when keyboard is open.*/
    private ImageView mIvLike, mIvDontLike, mIvYes, mIvNo, mIvMore, mIvLess,
            mIvHome, mIvKeyboard, mIvBack, mIvTts;
    /*Input text view to speak custom text.*/
    private EditText mEtTTs;
    private KeyListener originalKeyListener;
    /*Recycler view which will populate category icons.*/
    private RecyclerView mRecyclerView;
    /*This variable is used to hold parent view (linear layout) of pressed category icon.*/
    private LinearLayout mMenuItemLinearLayout;
    /*This variable indicates index of category icon selected in level one and two respectively.*/
    private int mLevelOneItemPos, mLevelTwoItemPos = -1;
    /*This variable indicates index of category icon in adapter in level 2. This variable is
     different than mLevelTwoItemPos. */
    private int mSelectedItemAdapterPos = -1;
    /*This flag is sets to true, when category icon is pressed followed by an expressive button;
     in this case the full sentence associated with selected expressive button is spoken.
     In case flag is false only expressive button is spoken out.*/
    private boolean mShouldReadFullSpeech = false;
    /*This variable hold the views populated in recycler view (category icon) list.*/
    private ArrayList<View> mRecyclerItemsViewList;
    /*This variable store current action bar title.*/
    private String mActionBarTitle;
    private SessionManager mSession;

    /*Below list stores the verbiage that are spoken when category icon + expression buttons
    pressed in conjunction*/
    private ArrayList<ArrayList<ArrayList<String>>> mLayerTwoSpeech;
    /*Below array stores the speech text, below text, expressive button speech text,
     navigation button speech text respectively.*/
    private String[] mArrSpeechText, mArrAdapterTxt, mExprBtnTxt,
            mNavigationBtnTxt;
    /*Below array stores tap count and index sort array respectively. This variables are used
     only, when in level one people or places category is
      selected.*/
    private Integer[] mArrPeoplePlaceTapCount, mArrSort;

    private String end, actionBarTitleTxt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_levelx_layout);
        getSupportActionBar().setBackgroundDrawable(getResources().getDrawable(R.drawable.yellow_bg));
        // Get index of category icons (position in recycler view) selected in level one.
        mLevelOneItemPos = getIntent().getExtras().getInt("mLevelOneItemPos");
        // Get and set title of category icons selected in level one.
        getSupportActionBar().setTitle(getIntent().getExtras().getString("selectedMenuItemPath"));
        // when layout is loaded on activity, using the tag attribute of a parent view in layout
        // the device size is identified. If device size is large (10' tablets) enable the
        // hardware acceleration. As seen in testing device, scrolling recycler items on 10' tab
        // have jagged views (lags in scrolling) hence hardware acceleration is enabled.
        if(findViewById(R.id.parent).getTag().toString().equals("large"))
            getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED);
        // Set app locale which is set in settings by user.
        new ChangeAppLocale(this).setLocale();
        // Initialize default exception handler for this activity.
        // If any exception occurs during this activity usage,
        // handle it using default exception handler.
        Thread.setDefaultUncaughtExceptionHandler(new DefaultExceptionHandler(this));
        mSession = new SessionManager(this);
        // The below string has value "" in english (all regions) and "है।" in Hindi (India).
        // It is used when user select category "Help" -> "About me".
        // To complete full sentence verbiage in Hindi below text is used.
        end = getString(R.string.endString);
        loadArraysFromResources();
        initializeArrayListOfRecycler();
        initializeLayoutViews();
        initializeRecyclerViewAdapter();
        initializeViewListeners();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Start measuring user app screen timer .
        getAnalytics(this, mSession.getCaregiverName());
        startMeasuring();
        //After resume from other app if the locale is other than
        // app locale, set it back to app locale.
        new ChangeAppLocale(this).setLocale();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Stop measuring user app screen timer .
        stopMeasuring("LevelTwoActivity");
        new ChangeAppLocale(this).setLocale();
    }

    @Override
    protected void onDestroy() {
        mRecyclerView = null;
        mLayerTwoSpeech = null;
        mRecyclerItemsViewList = null;
        mMenuItemLinearLayout = null;
        mExprBtnTxt = null;
        mArrSort = null;
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.languageSelect:
                startActivity(new Intent(this, LanguageSelectActivity.class));
                break;
            case R.id.profile:
                startActivity(new Intent(this, ProfileFormActivity.class));
                break;
            case R.id.info:
                startActivity(new Intent(this, AboutJellowActivity.class));
                break;
            case R.id.usage:
                startActivity(new Intent(this, TutorialActivity.class));
                break;
            case R.id.keyboardinput:
                startActivity(new Intent(this, KeyboardInputActivity.class));
                break;
            case R.id.feedback:
                startActivity(new Intent(this, FeedbackActivity.class));
                break;
            case R.id.settings:
                startActivity(new Intent(this, SettingActivity.class));
                break;
            case R.id.reset:
                startActivity(new Intent(this, ResetPreferencesActivity.class));
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // If user pressed home button in level three, it indicate that user be redirected to level
        // one (MainActivity). When this activity receives RESULT_CANCELED as resultCode it
        // understands Home request is generated, so it closes itself.
        if(requestCode == REQ_HOME && resultCode == RESULT_CANCELED)
            finish();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    @Override
    public void onRequestPermissionsResult (int requestCode, String Permissions[], int[] grantResults){
        // Unused code in current version
        if (requestCode == MY_PERMISSIONS_REQUEST_CALL_PHONE){
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                //startCall("tel:" + mSession.getCaregiverNumber());
            } else {
                Toast.makeText(this, "Call permission was denied.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * <p>This function will initialize the views that are populated on the activity layout.
     * It also assigns content description to the views to enable speech in
     * Talk-back feature. The Talk-back feature is not available in this version.</p>
     * */
    private void initializeLayoutViews() {
        mIvLike = findViewById(R.id.ivlike);
        mIvLike.setContentDescription(mExprBtnTxt[0]);
        mIvDontLike = findViewById(R.id.ivdislike);
        mIvDontLike.setContentDescription(mExprBtnTxt[6]);
        mIvMore = findViewById(R.id.ivadd);
        mIvMore.setContentDescription(mExprBtnTxt[4]);
        mIvLess = findViewById(R.id.ivminus);
        mIvLess.setContentDescription(mExprBtnTxt[10]);
        mIvYes = findViewById(R.id.ivyes);
        mIvYes.setContentDescription(mExprBtnTxt[2]);
        mIvNo = findViewById(R.id.ivno);
        mIvNo.setContentDescription(mExprBtnTxt[8]);
        mIvHome = findViewById(R.id.ivhome);
        mIvHome.setContentDescription(mNavigationBtnTxt[0]);
        mIvBack = findViewById(R.id.ivback);
        mIvBack.setContentDescription(mNavigationBtnTxt[1]);
        mIvBack.setAlpha(1f);
        mIvKeyboard = findViewById(R.id.keyboard);
        mIvKeyboard.setContentDescription(mNavigationBtnTxt[2]);
        mEtTTs = findViewById(R.id.et);
        mEtTTs.setContentDescription(getString(R.string.string_to_speak));
        //Initially custom input text is invisible
        mEtTTs.setVisibility(View.INVISIBLE);

        mIvTts = findViewById(R.id.ttsbutton);
        mIvTts.setContentDescription(getString(R.string.speak_written_text));
        //Initially custom input text speak button is invisible
        mIvTts.setVisibility(View.INVISIBLE);

        originalKeyListener = mEtTTs.getKeyListener();
        // Set it to null - this will make the field non-editable
        mEtTTs.setKeyListener(null);
        mRecyclerView = findViewById(R.id.recycler_view);
        // Initiate 3 columns in Recycler View.
        mRecyclerView.setLayoutManager( new GridLayoutManager(this, 3));
        mRecyclerView.setVerticalScrollBarEnabled(true);
        mRecyclerView.setScrollbarFadingEnabled(false);
    }

    /**
     * <p>This function will initialize the adapter for recycler view. In this level two
     * types of adapter are used:
     *      a) {@link PeoplePlacesAdapter}
     *      b) {@link LevelTwoAdapter}
     * As per the category icon selected in the {@link MainActivity}, an adapter is selected and hence
     * category icons are populated in this level.</p>
     * */
    private void initializeRecyclerViewAdapter() {
        if(mLevelOneItemPos != CATEGORY_ICON_PEOPLE && mLevelOneItemPos != CATEGORY_ICON_PLACES) {
            mRecyclerView.setAdapter(new LevelTwoAdapter(this, mLevelOneItemPos));
        }else{
            mRecyclerView.setAdapter(new PeoplePlacesAdapter(this, mLevelOneItemPos,
                    mArrAdapterTxt, mArrSort));
        }
    }

    /**
     * <p>This function will initialize the action listeners to the views which are populated on
     * on this activity.</p>
     * */
    private void initializeViewListeners() {
        initRecyclerViewListeners();
        initBackBtnListener();
        initHomeBtnListener();
        initKeyboardBtnListener();
        initLikeBtnListener();
        initDontLikeBtnListener();
        initYesBtnListener();
        initNoBtnListener();
        initMoreBtnListener();
        initLessBtnListener();
        initTTsBtnListener();
        initTTsEditTxtListener();
    }

    /**
     * <p>This function initializes {@link RecyclerTouchListener} and
     * {@link RecyclerView.OnChildAttachStateChangeListener} for recycler view.
     * {@link RecyclerTouchListener} is a custom defined Touch event listener class.
     * {@link RecyclerView.OnChildAttachStateChangeListener} is defined to manage view state of
     * recycler child view. It is useful to retain current state of child, when recycler view is scrolled
     * and recycler child views are recycled for memory usage optimization.</p>
     * */
    private void initRecyclerViewListeners() {
        mRecyclerView.addOnItemTouchListener(new RecyclerTouchListener(getApplicationContext(),
                mRecyclerView, new RecyclerTouchListener.ClickListener() {
            @Override
            public void onClick(final View view, final int position) {
                mMenuItemLinearLayout = view.findViewById(R.id.linearlayout_icon1);
                mMenuItemLinearLayout.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        tappedCategoryItemEvent(view, v, position);
                    }
                });
            }
            @Override   public void onLongClick(View view, int position) {}
        }));

        // When user scrolls into category, the child views are attached and detached from
        // recycler view. Also, the child views in recycler view have scrolled
        // off-screen are kept for reuse. Many times the reused view is assigned to
        // on-screen view. In our app this behaviour be can seen as follows:
        // When user scrolls into category, the child views are attached and detached from
        // recycler view. Also, the child views in recycler view have scrolled
        // off-screen are kept for reuse. Many times the reused view is assigned to
        // on-screen view. In our app this behaviour be can seen as follows:
        // Select "Dog" category icon in "Learning -> Animals & birds" then scrolled it off-screen
        // you will see selection border is appeared to another on-screen child of recycler view.
        // To overcome this situation a global list of state to every child of recycler view
        // is maintained.
        // When view is detached from recycler view it is removed from global list (mRecyclerItemsViewList)
        // and when child is attached to recycler view it is added to global list.
        // If a category icon is selected and it is scrolled off-screen, other on-screen view reusing
        // same view will get selection border so its border is removed first before removing from
        // global list.
        // When recycler view is scrolled, every newly attached child is checked if it is selected
        // previously or not. If the child is selected and it is reattached to recycler view then
        // set its border.
        mRecyclerView.addOnChildAttachStateChangeListener(new RecyclerView.OnChildAttachStateChangeListener() {
            @Override
            public void onChildViewAttachedToWindow(View view) {
                mRecyclerItemsViewList.set(mRecyclerView.getChildLayoutPosition(view), view);
                if(mRecyclerItemsViewList.contains(view) && mSelectedItemAdapterPos > -1 &&
                        mRecyclerView.getChildLayoutPosition(view) == mSelectedItemAdapterPos)
                    setMenuImageBorder(view, true);
            }

            @Override
            public void onChildViewDetachedFromWindow(View view) {
                setMenuImageBorder(view, false);
                mRecyclerItemsViewList.set(mRecyclerView.getChildLayoutPosition(view), null);
            }
        });
    }

    /**
     * <p>This function will initialize the click listener to Navigation "back" button.
     * When pressed navigation back button:
     *  a) If user is using custom keyboard input text then custom keyboard input text layout
     *     is closed.
     *  b) If user is not using custom keyboard input text then current level is closed returning
     *  successful closure (RESULT_OK) of a screen. User will returned back to {@link MainActivity}.
     * </p>
     * */
    private void initBackBtnListener() {
        mIvBack.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                //Firebase event
                singleEvent("Navigation","Back");
                speakSpeech(mNavigationBtnTxt[1]);
                //when mFlgKeyboardOpened is set to 1, it means user is using custom keyboard input
                // text and system keyboard is visible.
                if (mFlgKeyboard == 1) {
                    // As user is using custom keyboard input text and then back button is pressed,
                    // user intent to close custom keyboard input text so below steps will follow:
                    // a) set keyboard button to unpressed state.
                    // b) set back button to pressed state.
                    // c) hide custom keyboard input text view.
                    // d) show category icons.
                    // e) set flag mFlgKeyboardOpened = false, as user not using custom keyboard input
                    //    anymore.
                    // e) retain expressive button state as they were before custom keyboard input text
                    //    open.
                    mIvKeyboard.setImageResource(R.drawable.keyboard);
                    mIvBack.setImageResource(R.drawable.back_pressed);
                    mEtTTs.setVisibility(View.INVISIBLE);
                    mRecyclerView.setVisibility(View.VISIBLE);
                    mIvTts.setVisibility(View.INVISIBLE);
                    mFlgKeyboard = 0;

                    // after closing custom keyboard input text layout, retain expressive button
                    // states as they were before opening custom keyboard input text layout
                    //Below if identify that category icon Help -> About me is selected category
                    // icon
                    if(mLevelOneItemPos == CATEGORY_ICON_HELP && mLevelTwoItemPos == 1) {
                        setExpressiveButtonToAboutMe(mFlgImage);
                        changeTheExpressiveButtons(!DISABLE_EXPR_BTNS);
                    //Below if check that selected category icon
                    // Help -> Emergency,
                    // Help -> I am hurt,
                    // Help -> I feel sick,
                    // Help -> I feel tired
                    // Help -> Help me do this,
                    // Help -> Allergy,
                    // Help -> Danger,
                    // Help -> Hazard
                    // respectively is selected or not. If selected then
                    //disable all expressive buttons.
                    }else if(mLevelOneItemPos == CATEGORY_ICON_HELP &&
                            ((mLevelTwoItemPos == 0) ||(mLevelTwoItemPos == 2) || (mLevelTwoItemPos == 3) ||
                                    (mLevelTwoItemPos == 4) ||(mLevelTwoItemPos == 5) ||
                                    (mLevelTwoItemPos == 12) ||(mLevelTwoItemPos == 13) ||
                                    (mLevelTwoItemPos == 14)))
                        changeTheExpressiveButtons(DISABLE_EXPR_BTNS);
                    //Below if check that selected category icon is Help -> Unsafe touch.
                    // If yes, then enable only don't like, no, less expressive buttons.
                    else if(mLevelOneItemPos == CATEGORY_ICON_HELP && mLevelTwoItemPos == 10)
                        unsafeTouchDisableExpressiveButtons();
                    //Below if check that selected category icon is Help -> safety.
                    // If yes, then disable only don't like expressive button
                    else if(mLevelOneItemPos == CATEGORY_ICON_HELP && mLevelTwoItemPos == 15)
                        safetyDisableExpressiveButtons();
                    else
                        changeTheExpressiveButtons(!DISABLE_EXPR_BTNS);
                //when back button is pressed and if mFlgKeyboard == 0, then custom keyboard
                // input text is not open and user intends to close current screen. Then simply
                // close activity and after setting result code. The result code is useful
                // to identify for returning activity that user is returned by pressing "back" button.
                } else {
                    mIvBack.setImageResource(R.drawable.back_pressed);
                    setResult(RESULT_OK);
                    finish();
                }
                showActionBarTitle(true);
            }
        });
    }

    /**
     * <p>This function will initialize the click listener to Navigation home button.
     * When user press this button user navigated to {@link MainActivity} with
     *  every state of views is like app launched as fresh. Action bar title is set
     *  to 'home'</p>
     * */
    private void initHomeBtnListener() {
        mIvHome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Firebase event
                singleEvent("Navigation","Home");
                mIvHome.setImageResource(R.drawable.home_pressed);
                mIvKeyboard.setImageResource(R.drawable.keyboard);
                speakSpeech(mNavigationBtnTxt[0]);
                //setting up result code to RESULT_CANCELED, is used in returning activity.
                // This imply that user pressed the home button in level three activity.
                setResult(RESULT_CANCELED);
                finish();
            }
        });
    }
    /**
     * <p>This function will initialize the click listener to Navigation "keyboard" button.
     * {@link LevelThreeActivity} navigation keyboard button either enable or disable
     * the custom keyboard input text layout.
     * When custom keyboard input text layout is enabled using keyboard button, it is visible to user
     * and action bar title set to "keyboard".
     * When custom keyboard input text layout is disabled using keyboard button, the state of the
     * {@link LevelThreeActivity} retrieved as it was before opening custom keyboard
     * input text layout.
     * */
    private void initKeyboardBtnListener() {
        mIvKeyboard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                speakSpeech(mNavigationBtnTxt[2]);
                //Firebase event
                singleEvent("Navigation","Keyboard");
                mIvTts.setImageResource(R.drawable.speaker_button);
                //when mFlgKeyboardOpened is set to 1, it means user is using custom keyboard input
                // text and system keyboard is visible.
                if (mFlgKeyboard == 1) {
                    // As user is using custom keyboard input text and then press the keyboard button,
                    // user intent to close custom keyboard input text so below steps will follow:
                    // a) set keyboard button to unpressed state.
                    // b) set back button to unpressed state
                    // c) hide custom keyboard input text.
                    // d) show category icons
                    // e) hide custom keyboard input text speak button
                    mIvKeyboard.setImageResource(R.drawable.keyboard);
                    mIvBack.setImageResource(R.drawable.back);
                    mEtTTs.setVisibility(View.INVISIBLE);
                    mRecyclerView.setVisibility(View.VISIBLE);
                    mIvTts.setVisibility(View.INVISIBLE);

                    // after closing custom keyboard input text layout, retain expressive button
                    // states as they were before opening custom keyboard input text layout
                    //Below if identify that category icon Help -> About me is selected
                    if (mLevelOneItemPos == CATEGORY_ICON_HELP && mLevelTwoItemPos == 1){
                        setExpressiveButtonToAboutMe(mFlgImage);
                        changeTheExpressiveButtons(!DISABLE_EXPR_BTNS);
                    //Below if check that selected category icon
                    // Help -> Emergency,
                    // Help -> I am hurt,
                    // Help -> I feel sick,
                    // Help -> I feel tired
                    // Help -> Help me do this,
                    // Help -> Allergy,
                    // Help -> Danger,
                    // Help -> Hazard
                    // respectively is selected or not. If selected then
                    //disable all expressive buttons.
                    }else if(mLevelOneItemPos == CATEGORY_ICON_HELP &&
                            ((mLevelTwoItemPos == 0) ||(mLevelTwoItemPos == 2) || (mLevelTwoItemPos == 3) ||
                                    (mLevelTwoItemPos == 4) ||(mLevelTwoItemPos == 5) ||
                                    (mLevelTwoItemPos == 12) ||(mLevelTwoItemPos == 13) ||
                                    (mLevelTwoItemPos == 14)))
                        changeTheExpressiveButtons(DISABLE_EXPR_BTNS);
                    //Below if check that selected category icon is Help -> Unsafe touch.
                    // If yes, then enable only don't like, no, less expressive buttons.
                    else if(mLevelOneItemPos == CATEGORY_ICON_HELP && mLevelTwoItemPos == 10)
                        unsafeTouchDisableExpressiveButtons();
                        //Below if check that selected category icon is Help -> safety.
                        // If yes, then disable only don't like expressive button
                    else if(mLevelOneItemPos == CATEGORY_ICON_HELP && mLevelTwoItemPos == 15)
                        safetyDisableExpressiveButtons();
                    else
                        changeTheExpressiveButtons(!DISABLE_EXPR_BTNS);
                    mFlgKeyboard = 0;
                    showActionBarTitle(true);
                //when mFlgKeyboardOpened is set to 0, it means user intend to use custom
                //keyboard input text so below steps will follow:
                } else {
                    // a) keyboard button to pressed state
                    // c) show custom keyboard input text and speak button view
                    // b) set back button unpressed state
                    // d) hide category icons
                    // e) disable expressive buttons
                    mIvKeyboard.setImageResource(R.drawable.keyboard_pressed);
                    mEtTTs.setVisibility(View.VISIBLE);
                    mEtTTs.setKeyListener(originalKeyListener);
                    // Focus the field.
                    mRecyclerView.setVisibility(View.INVISIBLE);
                    changeTheExpressiveButtons(DISABLE_EXPR_BTNS);
                    mEtTTs.requestFocus();
                    mIvTts.setVisibility(View.VISIBLE);
                    // when user intend to use custom keyboard input text system keyboard should
                    // only appear when user taps on custom keyboard input view. Setting
                    // InputMethodManager to InputMethodManager.HIDE_NOT_ALWAYS does this task.
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.toggleSoftInput(InputMethodManager.HIDE_NOT_ALWAYS, 0);
                    // when user is typing in custom keyboard input text it is necessary
                    // for user to see input text. The function setSoftInputMode() does this task.
                    getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
                    mIvBack.setImageResource(R.drawable.back);
                    mIvBack.setAlpha(1f);
                    mIvBack.setEnabled(true);
                    mFlgKeyboard = 1;
                    showActionBarTitle(false);
                    getSupportActionBar().setTitle(getString(R.string.keyboard));
                }
            }
        });
    }

    /**
     * <p>This function will initialize the click listener for expressive "like" button.
     * Expressive like button is works in four ways:
     *  a) press expressive like button once
     *  b) press expressive like button twice
     *  c) press category icon first then press expressive like button once
     *  d) press category icon first then press expressive like button twice
     * This function execute task as follows:
     *  a) reset the all expressive button speech flag except like button
     *  b) set pressed icon to like button
     *  c) produce speech using output for like
     *  d) set border to category icon of color associated with like button</p>
     * */
    private void initLikeBtnListener() {
        mIvLike.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // When user press the like button all expressive button speech flag (except like)
                // are set to reset.
                mFlgYes = mFlgMore = mFlgDntLike = mFlgNo = mFlgLess = 0;
                //Value of mFlgImage = 0, indicates like expressive button is pressed
                mFlgImage = 0;
                // If selected category icon is Help -> About me, then show
                // expressive icons of About me category.
                // and set like button icon to pressed using mFlgImage.
                if (mLevelOneItemPos == 8 && mLevelTwoItemPos == 1)
                    setExpressiveButtonToAboutMe(mFlgImage);
                else
                    resetExpressiveButtons(mFlgImage);
                // if value of mShouldReadFullSpeech is false then do not speak full sentence verbiage.
                if (!mShouldReadFullSpeech) {
                    // if value of mFlgLike is 1, then should speak "really like".
                    if (mFlgLike == 1) {
                        speakSpeech(mExprBtnTxt[1]);
                        mFlgLike = 0;
                        //Firebase event
                        singleEvent("ExpressiveIcon","ReallyLike");
                    // if value of mFlgLike is 0, then should speak "like".
                    } else {
                        speakSpeech(mExprBtnTxt[0]);
                        mFlgLike = 1;
                        //Firebase event
                        singleEvent("ExpressiveIcon","Like");
                    }
                // if value of mShouldReadFullSpeech is true, then speak associated like
                // expression verbiage to selected category icon.
                } else {
                    ++mActionBtnClickCount;
                    // Set like button color border to selected category icon.
                    // border color is applied using mActionBtnClickCount and mFlgImage.
                    if(mRecyclerItemsViewList.get(mSelectedItemAdapterPos) != null)
                        setMenuImageBorder(mRecyclerItemsViewList.get(mSelectedItemAdapterPos), true);
                    // if value of mFlgLike is 1, then should speak "really like" expression
                    // verbiage associated to selected category icon.
                    if (mFlgLike == 1) {
                        //Firebase event
                        singleEvent("ExpressiveIcon","ReallyLike");
                        // People and places will have preferences. To speak the correct speech text
                        // preference sort array is used.
                        if (mLevelOneItemPos == CATEGORY_ICON_PEOPLE ||
                                mLevelOneItemPos == CATEGORY_ICON_PLACES) {
                            speakSpeech(mLayerTwoSpeech.get(mLevelOneItemPos).
                                    get(mArrSort[mLevelTwoItemPos]).get(1));
                            singleEvent("ExpressiveGridIcon",
                                    mLayerTwoSpeech.get(mLevelOneItemPos).
                                            get(mArrSort[mLevelTwoItemPos]).get(1));
                        // If Help -> About me category icon is selected,
                        // "really like" expression will speak child's name
                        }else if(mLevelOneItemPos == 8 && mLevelTwoItemPos == 1) {
                            speakSpeech(mLayerTwoSpeech.get(mLevelOneItemPos).
                                    get(mLevelTwoItemPos).get(1) + mSession.getName() + end);
                            singleEvent("ExpressiveGridIcon",
                                    mLayerTwoSpeech.get(mLevelOneItemPos).
                                        get(mLevelTwoItemPos).get(1) + mSession.getName() + end);
                        }else {
                            speakSpeech(mLayerTwoSpeech.get(mLevelOneItemPos).
                                    get(mLevelTwoItemPos).get(1));
                            singleEvent("ExpressiveGridIcon",
                                    mLayerTwoSpeech.get(mLevelOneItemPos).
                                            get(mLevelTwoItemPos).get(1));
                        }
                        //reset mFlgLike to speak "like" expression
                        mFlgLike = 0;
                    // if value of mFlgLike is 0 then Speak associated like expression
                    // verbiage to selected category icon.
                    } else {
                        //Firebase event
                        singleEvent("ExpressiveIcon","Like");
                        // People and places will have preferences. To speak the correct speech text
                        // preference sort array is used.
                        if (mLevelOneItemPos == CATEGORY_ICON_PEOPLE ||
                                mLevelOneItemPos == CATEGORY_ICON_PLACES) {
                            speakSpeech(mLayerTwoSpeech.get(mLevelOneItemPos).
                                    get(mArrSort[mLevelTwoItemPos]).get(0));
                            singleEvent("ExpressiveGridIcon",
                                    mLayerTwoSpeech.get(mLevelOneItemPos).
                                            get(mArrSort[mLevelTwoItemPos]).get(0));
                            // If Help -> About me category icon is selected,
                            // "really like" expression will speak child's name
                        }else if(mLevelOneItemPos == 8 && mLevelTwoItemPos == 1) {
                            speakSpeech(mLayerTwoSpeech.get(mLevelOneItemPos).
                                    get(mLevelTwoItemPos).get(0) + mSession.getName() + end);
                            singleEvent("ExpressiveGridIcon",
                                mLayerTwoSpeech.get(mLevelOneItemPos).
                                    get(mLevelTwoItemPos).get(0) + mSession.getName() + end);
                        }else {
                            speakSpeech(mLayerTwoSpeech.get(mLevelOneItemPos).
                                    get(mLevelTwoItemPos).get(0));
                            singleEvent("ExpressiveGridIcon",
                                    mLayerTwoSpeech.get(mLevelOneItemPos).
                                            get(mLevelTwoItemPos).get(0));
                        }
                        //reset mFlgLike to speak "really like" expression
                        mFlgLike = 1;
                    }
                }
                mIvBack.setImageResource(R.drawable.back);
            }
        });
    }

    /**
     * <p>This function will initialize the click listener for expressive "don't like" button.
     * Expressive don't like button is works in four ways:
     *  a) press expressive don't like button once
     *  b) press expressive don't like button twice
     *  c) press category icon first then press expressive don't like button once
     *  d) press category icon first then press expressive don't like button twice
     * This function execute task as follows:
     *  a) reset the all expressive button speech flag except don't like button
     *  b) set pressed icon to don't like button
     *  c) produce speech using output for don't like
     *  d) set border to category icon of color associated with don't like button</p>
     * */
    private void initDontLikeBtnListener() {
        mIvDontLike.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // When user press the don't like button all expressive button speech flag (except don't like)
                // are set to reset.
                mFlgLike = 0; mFlgYes = 0; mFlgMore = 0; mFlgNo = 0; mFlgLess = 0;
                //Value of mFlgImage = 1, indicates don't like expressive button is pressed
                mFlgImage = 1;
                // If selected category icon is Help -> About me, then show
                // expressive icons of About me category.
                // and set don't like button icon to pressed using mFlgImage.
                if (mLevelOneItemPos == 8 && mLevelTwoItemPos == 1)
                    setExpressiveButtonToAboutMe(mFlgImage);
                else
                    resetExpressiveButtons(mFlgImage);
                // if value of mShouldReadFullSpeech is false then do not speak full sentence verbiage.
                if (!mShouldReadFullSpeech) {
                    // if value of mFlgDntLike is 1, then should speak "really dont like".
                    if (mFlgDntLike == 1) {
                        speakSpeech(mExprBtnTxt[7]);
                        mFlgDntLike = 0;
                        //Firebase event
                        singleEvent("ExpressiveIcon","ReallyDon'tLike");
                    // if value of mFlgDntLike is 0, then should speak " dont like".
                    } else {
                        speakSpeech(mExprBtnTxt[6]);
                        mFlgDntLike = 1;
                        //Firebase event
                        singleEvent("ExpressiveIcon","Don'tLike");
                    }
                // if value of mShouldReadFullSpeech is true, then speak associated don't like
                // expression verbiage to selected category icon.
                } else {
                    ++mActionBtnClickCount;
                    // Set don't like button color border to selected category icon.
                    // border color is applied using mActionBtnClickCount and mFlgImage.
                    if(mRecyclerItemsViewList.get(mSelectedItemAdapterPos) != null)
                        setMenuImageBorder(mRecyclerItemsViewList.get(mSelectedItemAdapterPos), true);

                    // if value of mFlgDntLike is 1, then should speak "really don't like" expression
                    // verbiage associated to selected category icon.
                    if (mFlgDntLike == 1) {
                        //Firebase event
                        singleEvent("ExpressiveIcon","ReallyDon'tLike");
                        // People and places will have preferences. To speak the correct speech text
                        // preference sort array is used.
                        if (mLevelOneItemPos == CATEGORY_ICON_PEOPLE ||
                                mLevelOneItemPos == CATEGORY_ICON_PLACES) {
                            speakSpeech(mLayerTwoSpeech.get(mLevelOneItemPos).
                                    get(mArrSort[mLevelTwoItemPos]).get(7));
                            singleEvent("ExpressiveGridIcon",
                                mLayerTwoSpeech.get(mLevelOneItemPos).
                                    get(mArrSort[mLevelTwoItemPos]).get(7));
                        // If Help -> About me category icon is selected,
                        // "really don't like" expression will speak child's caregiver's name
                        }else if(mLevelOneItemPos == 8 && mLevelTwoItemPos == 1) {
                            speakSpeech(mLayerTwoSpeech.get(mLevelOneItemPos).
                                    get(mLevelTwoItemPos).get(7) + mSession.getCaregiverName() + end);
                            singleEvent("ExpressiveGridIcon",
                                mLayerTwoSpeech.get(mLevelOneItemPos).
                                    get(mLevelTwoItemPos).get(7) + mSession.getCaregiverName() + end);
                        }else {
                            speakSpeech(mLayerTwoSpeech.get(mLevelOneItemPos).
                                    get(mLevelTwoItemPos).get(7));
                            singleEvent("ExpressiveGridIcon",
                                    mLayerTwoSpeech.get(mLevelOneItemPos).
                                            get(mLevelTwoItemPos).get(7));
                        }
                        //reset mFlgDntLike to speak "don't like" expression
                        mFlgDntLike = 0;
                    // if value of mFlgDntLike is 0 then Speak associated don't like expression
                    // verbiage to selected category icon.
                    } else {
                        //Firebase event
                        singleEvent("ExpressiveIcon", "Don'tLike");
                        // People and places will have preferences. To get correct speech text sort
                        // is applied.
                        if (mLevelOneItemPos == CATEGORY_ICON_PEOPLE ||
                                mLevelOneItemPos == CATEGORY_ICON_PLACES){
                            speakSpeech(mLayerTwoSpeech.get(mLevelOneItemPos).
                                    get(mArrSort[mLevelTwoItemPos]).get(6));
                            singleEvent("ExpressiveGridIcon",
                                    mLayerTwoSpeech.get(mLevelOneItemPos).
                                            get(mArrSort[mLevelTwoItemPos]).get(6));
                        // If Help -> About me category icon is selected,
                        // "really don't like" expression will speak child's caregiver name
                        }else if(mLevelOneItemPos == 8 && mLevelTwoItemPos == 1) {
                                speakSpeech(mLayerTwoSpeech.get(mLevelOneItemPos).
                                    get(mLevelTwoItemPos).get(6) + mSession.getCaregiverName() + end);
                                singleEvent("ExpressiveGridIcon",
                                    mLayerTwoSpeech.get(mLevelOneItemPos).
                                        get(mLevelTwoItemPos).get(6) + mSession.getCaregiverName() + end);
                        }else {
                                speakSpeech(mLayerTwoSpeech.get(mLevelOneItemPos).
                                        get(mLevelTwoItemPos).get(6));
                                singleEvent("ExpressiveGridIcon",
                                        mLayerTwoSpeech.get(mLevelOneItemPos).
                                                get(mLevelTwoItemPos).get(6));
                        }
                        //reset mFlgDntLike to speak "really don't like" expression
                        mFlgDntLike = 1;
                    }
                }
                mIvBack.setImageResource(R.drawable.back);
            }
        });
    }

    /**
     * <p>This function will initialize the click listener for expressive "yes" button.
     * Expressive yes button is works in four ways:
     *  a) press expressive yes button once
     *  b) press expressive yes button twice
     *  c) press category icon first then press expressive yes button once
     *  d) press category icon first then press expressive yes button twice
     * This function execute task as follows:
     *  a) reset the all expressive button speech flag except yes button
     *  b) set pressed icon to yes button
     *  c) produce speech using output for yes
     *  d) set border to category icon of color associated with yes button</p>
     * */
    private void initYesBtnListener() {
        mIvYes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // When user press the yes button all expressive button speech flag (except yes)
                // are set to reset.
                mFlgLike = 0; mFlgMore = 0; mFlgDntLike = 0; mFlgNo = 0; mFlgLess = 0;
                //Value of mFlgImage = 2, indicates yes expressive button is pressed
                mFlgImage = 2;
                // If selected category icon is Help -> About me, then show
                // expressive icons of About me category.
                // and set yes button icon to pressed using mFlgImage.
                if (mLevelOneItemPos == 8 && mLevelTwoItemPos == 1)
                    setExpressiveButtonToAboutMe(mFlgImage);
                else
                    resetExpressiveButtons(mFlgImage);
                // if value of mShouldReadFullSpeech is false then do not speak full sentence verbiage.
                if (!mShouldReadFullSpeech) {
                    // if value of mFlgYes is 1, then should speak "really yes".
                    if (mFlgYes == 1) {
                        speakSpeech(mExprBtnTxt[3]);
                        mFlgYes = 0;
                        //Firebase event
                        singleEvent("ExpressiveIcon","ReallyYes");
                    // if value of mFlgYes is 0, then should speak "yes".
                    } else {
                        speakSpeech(mExprBtnTxt[2]);
                        mFlgYes = 1;
                        //Firebase event
                        singleEvent("ExpressiveIcon","Yes");
                    }
                // if value of mShouldReadFullSpeech is true, then speak associated yes
                // expression verbiage to selected category icon.
                } else  {
                    ++mActionBtnClickCount;
                    // Set yes button color border to selected category icon.
                    // border color is applied using mActionBtnClickCount and mFlgImage.
                    if(mRecyclerItemsViewList.get(mSelectedItemAdapterPos) != null)
                        setMenuImageBorder(mRecyclerItemsViewList.get(mSelectedItemAdapterPos), true);
                    // if value of mFlgYes is 1, then should speak "really yes" expression
                    // verbiage associated to selected category icon.
                    if (mFlgYes == 1) {
                        //Firebase event
                        singleEvent("ExpressiveIcon","ReallyYes");
                        // People and places will have preferences. To speak the correct speech text
                        // preference sort array is used.
                        if (mLevelOneItemPos == CATEGORY_ICON_PEOPLE ||
                                mLevelOneItemPos == CATEGORY_ICON_PLACES) {
                            speakSpeech(mLayerTwoSpeech.get(mLevelOneItemPos).
                                    get(mArrSort[mLevelTwoItemPos]).get(3));
                            singleEvent("ExpressiveGridIcon",
                                    mLayerTwoSpeech.get(mLevelOneItemPos).
                                            get(mArrSort[mLevelTwoItemPos]).get(3));
                        // If Help -> About me category icon is selected,
                        // "really yes" expression will speak caregivers email id
                        }else if(mLevelOneItemPos == 8 && mLevelTwoItemPos == 1) {
                            speakSpeech(mLayerTwoSpeech.get(mLevelOneItemPos).
                                    get(mLevelTwoItemPos).get(3) + mSession.getEmailId() + end);
                            singleEvent("ExpressiveGridIcon",
                                    mLayerTwoSpeech.get(mLevelOneItemPos).
                                            get(mLevelTwoItemPos).get(3) + mSession.getEmailId() + end);
                        }else{
                            speakSpeech(mLayerTwoSpeech.get(mLevelOneItemPos).
                                    get(mLevelTwoItemPos).get(3));
                            singleEvent("ExpressiveGridIcon",
                                    mLayerTwoSpeech.get(mLevelOneItemPos).
                                            get(mLevelTwoItemPos).get(3));
                        }
                        //reset mFlgYes to speak "yes" expression
                        mFlgYes = 0;
                    // if value of mFlgYes is 0 then Speak associated yes expression
                    // verbiage to selected category icon.
                    } else {
                        //Firebase event
                        singleEvent("ExpressiveIcon","Yes");
                        // People and places will have preferences. To speak the correct speech text
                        // preference sort array is used.
                        if (mLevelOneItemPos == CATEGORY_ICON_PEOPLE ||
                                mLevelOneItemPos == CATEGORY_ICON_PLACES) {
                            speakSpeech(mLayerTwoSpeech.get(mLevelOneItemPos).
                                    get(mArrSort[mLevelTwoItemPos]).get(2));
                            singleEvent("ExpressiveGridIcon",
                                    mLayerTwoSpeech.get(mLevelOneItemPos).
                                            get(mArrSort[mLevelTwoItemPos]).get(2));
                        // If Help -> About me category icon is selected,
                        // "yes" expression will speak caregivers email id
                        }else if(mLevelOneItemPos == 8 && mLevelTwoItemPos == 1) {
                            speakSpeech(mLayerTwoSpeech.get(mLevelOneItemPos).
                                    get(mLevelTwoItemPos).get(2) + mSession.getEmailId() + end);
                            singleEvent("ExpressiveGridIcon",
                                mLayerTwoSpeech.get(mLevelOneItemPos).
                                    get(mLevelTwoItemPos).get(2) + mSession.getEmailId() + end);
                        }else {
                            speakSpeech(mLayerTwoSpeech.get(mLevelOneItemPos).
                                    get(mLevelTwoItemPos).get(2));
                            singleEvent("ExpressiveGridIcon",
                                mLayerTwoSpeech.get(mLevelOneItemPos).
                                    get(mLevelTwoItemPos).get(2));
                        }
                        //reset mFlgYes to speak "really yes" expression
                        mFlgYes = 1;
                    }
                }
                mIvBack.setImageResource(R.drawable.back);
            }
        });
    }

    /**
     * <p>This function will initialize the click listener for expressive "no" button.
     * Expressive no button is works in four ways:
     *  a) press expressive no button once
     *  b) press expressive no button twice
     *  c) press category icon first then press expressive no button once
     *  d) press category icon first then press expressive no button twice
     * This function execute task as follows:
     *  a) reset the all expressive button speech flag except no button
     *  b) set pressed icon to no button
     *  c) produce speech using output for no
     *  d) set border to category icon of color associated with no button</p>
     * */
    private void initNoBtnListener() {
        mIvNo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // When user press the no button all expressive button speech flag (except no)
                // are set to reset.
                mFlgLike = 0; mFlgYes = 0; mFlgMore = 0; mFlgDntLike = 0; mFlgLess = 0;
                //Value of mFlgImage = 3, indicates no expressive button is pressed
                mFlgImage = 3;
                // If selected category icon is Help -> About me, then show
                // expressive icons of About me category.
                // and set no button icon to pressed using mFlgImage.
                if (mLevelOneItemPos == 8 && mLevelTwoItemPos == 1)
                    setExpressiveButtonToAboutMe(mFlgImage);
                else
                    resetExpressiveButtons(mFlgImage);
                // if value of mShouldReadFullSpeech is false then do not speak full sentence verbiage.
                if (!mShouldReadFullSpeech) {
                    // if value of mFlgNo is 1, then should speak "really no".
                    if (mFlgNo == 1) {
                        speakSpeech(mExprBtnTxt[9]);
                        mFlgNo = 0;
                        //Firebase event
                        singleEvent("ExpressiveIcon","ReallyNo");
                    // if value of mFlgNo is 0, then should speak "no".
                    } else {
                        speakSpeech(mExprBtnTxt[8]);
                        mFlgNo = 1;
                        //Firebase event
                        singleEvent("ExpressiveIcon","No");
                    }
                // if value of mShouldReadFullSpeech is true, then speak associated no
                // expression verbiage to selected category icon.
                } else {
                    ++mActionBtnClickCount;
                    // Set no button color border to selected category icon.
                    // border color is applied using mActionBtnClickCount and mFlgImage.
                    if(mRecyclerItemsViewList.get(mSelectedItemAdapterPos) != null)
                        setMenuImageBorder(mRecyclerItemsViewList.get(mSelectedItemAdapterPos), true);
                    // if value of mFlgNo is 1, then should speak "really no" expression
                    // verbiage associated to selected category icon.
                    if (mFlgNo == 1) {
                        //Firebase event
                        singleEvent("ExpressiveIcon","ReallyNo");
                        // People and places will have preferences. To speak the correct speech text
                        // preference sort array is used.
                        if (mLevelOneItemPos == CATEGORY_ICON_PEOPLE ||
                                mLevelOneItemPos == CATEGORY_ICON_PLACES) {
                            speakSpeech(mLayerTwoSpeech.get(mLevelOneItemPos).
                                    get(mArrSort[mLevelTwoItemPos]).get(9));
                            singleEvent("ExpressiveGridIcon",
                                    mLayerTwoSpeech.get(mLevelOneItemPos).
                                            get(mArrSort[mLevelTwoItemPos]).get(9));
                            // If Help -> About me category icon is selected,
                            // "really no" expression will speak child's address
                        }else if(mLevelOneItemPos == 8 && mLevelTwoItemPos == 1) {
                            speakSpeech(mLayerTwoSpeech.get(mLevelOneItemPos).
                                    get(mLevelTwoItemPos).get(9) + mSession.getAddress() + end);
                            singleEvent("ExpressiveGridIcon",
                             mLayerTwoSpeech.get(mLevelOneItemPos).
                                        get(mLevelTwoItemPos).get(9) + mSession.getAddress() + end);
                        }else {
                            speakSpeech(mLayerTwoSpeech.get(mLevelOneItemPos).
                                    get(mLevelTwoItemPos).get(9));
                            singleEvent("ExpressiveGridIcon",
                                    mLayerTwoSpeech.get(mLevelOneItemPos).
                                            get(mLevelTwoItemPos).get(9));
                        }
                        //reset mFlgNo to speak "no" expression
                        mFlgNo = 0;
                    // if value of mFlgNo is 0 then Speak associated like expression
                    // verbiage to selected category icon.
                    } else {
                        //Firebase event
                        singleEvent("ExpressiveIcon","No");
                        // People and places will have preferences. To speak the correct speech text
                        // preference sort array is used.
                        if (mLevelOneItemPos == CATEGORY_ICON_PEOPLE ||
                                mLevelOneItemPos == CATEGORY_ICON_PLACES) {
                            speakSpeech(mLayerTwoSpeech.get(mLevelOneItemPos).
                                    get(mArrSort[mLevelTwoItemPos]).get(8));
                            singleEvent("ExpressiveGridIcon",
                                    mLayerTwoSpeech.get(mLevelOneItemPos).
                                            get(mArrSort[mLevelTwoItemPos]).get(8));
                            // If Help -> About me category icon is selected,
                            // "really no" expression will speak child's address
                        }else if(mLevelOneItemPos == 8 && mLevelTwoItemPos == 1) {
                            speakSpeech(mLayerTwoSpeech.get(mLevelOneItemPos).
                                    get(mLevelTwoItemPos).get(8) + mSession.getAddress() + end);
                            singleEvent("ExpressiveGridIcon",
                                mLayerTwoSpeech.get(mLevelOneItemPos).
                                    get(mLevelTwoItemPos).get(8) + mSession.getAddress() + end);
                        }else {
                            speakSpeech(mLayerTwoSpeech.get(mLevelOneItemPos).
                                    get(mLevelTwoItemPos).get(8));
                            singleEvent("ExpressiveGridIcon",
                                    mLayerTwoSpeech.get(mLevelOneItemPos).
                                            get(mLevelTwoItemPos).get(8));
                        }
                        //reset mFlgLike to speak "really no" expression
                        mFlgNo = 1;
                    }
                }
                mIvBack.setImageResource(R.drawable.back);
            }
        });
    }

    /**
     * <p>This function will initialize the click listener for expressive "more" button.
     * Expressive more button is works in four ways:
     *  a) press expressive more button once
     *  b) press expressive more button twice
     *  c) press category icon first then press expressive more button once
     *  d) press category icon first then press expressive more button twice
     * This function execute task as follows:
     *  a) reset the all expressive button speech flag except more button
     *  b) reset all expressive buttons
     *  c) produce speech using output for more
     *  d) set border to category icon of color associated with more button</p>
     * */
    private void initMoreBtnListener() {
        mIvMore.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // When user press the more button all expressive button speech flag (except more)
                // are set to reset.
                mFlgLike = 0; mFlgYes = 0; mFlgDntLike = 0; mFlgNo = 0; mFlgLess = 0;
                //Value of mFlgImage = 4, indicates more expressive button is pressed
                mFlgImage = 4;
                // If selected category icon is Help -> About me, then show
                // expressive icons of About me category.
                // and set more button icon to pressed using mFlgImage.
                if (mLevelOneItemPos == 8 && mLevelTwoItemPos == 1)
                    setExpressiveButtonToAboutMe(mFlgImage);
                else
                    resetExpressiveButtons(mFlgImage);
                // if value of mShouldReadFullSpeech is false then do not speak full sentence verbiage.
                if (!mShouldReadFullSpeech) {
                    // if value of mFlgMore is 1, then should speak "really more".
                    if (mFlgMore == 1) {
                        speakSpeech(mExprBtnTxt[5]);
                        mFlgMore = 0;
                        //Firebase event
                        singleEvent("ExpressiveIcon","ReallyMore");
                    // if value of mFlgMore is 0, then should speak "more".
                    } else {
                        speakSpeech(mExprBtnTxt[4]);
                        mFlgMore = 1;
                        //Firebase event
                        singleEvent("ExpressiveIcon","More");
                    }
                // if value of mShouldReadFullSpeech is true, then speak associated more
                // expression verbiage to selected category icon.
                } else {
                    ++mActionBtnClickCount;
                    // Set more button color border to selected category icon.
                    // border color is applied using mActionBtnClickCount and mFlgImage.
                    if(mRecyclerItemsViewList.get(mSelectedItemAdapterPos) != null)
                        setMenuImageBorder(mRecyclerItemsViewList.get(mSelectedItemAdapterPos), true);
                    // if value of mFlgmore is 1, then should speak "really more" expression
                    // verbiage associated to selected category icon.
                    if (mFlgMore == 1) {
                        //Firebase event
                        singleEvent("ExpressiveIcon","ReallyMore");
                        // People and places will have preferences. To speak the correct speech text
                        // preference sort array is used.
                        if (mLevelOneItemPos == CATEGORY_ICON_PEOPLE ||
                                mLevelOneItemPos == CATEGORY_ICON_PLACES) {
                            speakSpeech(mLayerTwoSpeech.get(mLevelOneItemPos).
                                    get(mArrSort[mLevelTwoItemPos]).get(5));
                            singleEvent("ExpressiveGridIcon",
                                    mLayerTwoSpeech.get(mLevelOneItemPos).
                                            get(mArrSort[mLevelTwoItemPos]).get(5));
                        // If Help -> About me category icon is selected,
                        // "really more" expression will speak caregivers number
                        }else if(mLevelOneItemPos == 8 && mLevelTwoItemPos == 1) {
                            speakSpeech(mLayerTwoSpeech.get(mLevelOneItemPos).
                                    get(mLevelTwoItemPos).get(5) + mSession.getCaregiverNumber().
                                    replaceAll("\\B", " ") + end);
                            singleEvent("ExpressiveGridIcon",
                                mLayerTwoSpeech.get(mLevelOneItemPos).
                                    get(mLevelTwoItemPos).get(5) + mSession.getCaregiverNumber().
                                        replaceAll("\\B", " ") + end);
                        }else {
                            speakSpeech(mLayerTwoSpeech.get(mLevelOneItemPos).
                                    get(mLevelTwoItemPos).get(5));
                            singleEvent("ExpressiveGridIcon",
                                    mLayerTwoSpeech.get(mLevelOneItemPos).
                                            get(mLevelTwoItemPos).get(5));
                        }
                        //reset mFlgMore to speak "more" expression
                        mFlgMore = 0;
                    // if value of mFlgMore is 0 then Speak associated more expression
                    // verbiage to selected category icon.
                    } else {
                        //Firebase event
                        singleEvent("ExpressiveIcon","More");
                        // People and places will have preferences. To speak the correct speech text
                        // preference sort array is used.
                        if (mLevelOneItemPos == CATEGORY_ICON_PEOPLE ||
                                mLevelOneItemPos == CATEGORY_ICON_PLACES) {
                            speakSpeech(mLayerTwoSpeech.get(mLevelOneItemPos).
                                    get(mArrSort[mLevelTwoItemPos]).get(4));
                            singleEvent("ExpressiveGridIcon",
                                    mLayerTwoSpeech.get(mLevelOneItemPos).
                                            get(mArrSort[mLevelTwoItemPos]).get(4));
                        }else if(mLevelOneItemPos == 8 && mLevelTwoItemPos == 1) {
                            // If Help -> About me category icon is selected,
                            // "really more" expression will speak caregivers number
                            speakSpeech(mLayerTwoSpeech.get(mLevelOneItemPos).
                                    get(mLevelTwoItemPos).get(5) + mSession.getCaregiverNumber().
                                    replaceAll("\\B", " ") + end);
                            singleEvent("ExpressiveGridIcon",
                                mLayerTwoSpeech.get(mLevelOneItemPos).
                                    get(mLevelTwoItemPos).get(5) + mSession.getCaregiverNumber().
                                        replaceAll("\\B", " ") + end);
                        }else {
                            speakSpeech(mLayerTwoSpeech.get(mLevelOneItemPos).
                                    get(mLevelTwoItemPos).get(4));
                            singleEvent("ExpressiveGridIcon",
                                    mLayerTwoSpeech.get(mLevelOneItemPos).
                                            get(mLevelTwoItemPos).get(4));
                        }
                        //reset mFlgMore to speak "really more" expression
                        mFlgMore = 1;
                    }
                }
                mIvBack.setImageResource(R.drawable.back);
            }
        });
    }

    /**
     * <p>This function will initialize the click listener for expressive "less" button.
     * Expressive less button is works in four ways:
     *  a) press expressive less button once
     *  b) press expressive less button twice
     *  c) press category icon first then press expressive less button once
     *  d) press category icon first then press expressive less button twice
     * This function execute task as follows:
     *  a) reset the all expressive button speech flag except less button
     *  b) reset all expressive buttons
     *  c) produce speech using output for less
     *  d) set border to category icon of color associated with less button</p>
     * */
    private void initLessBtnListener() {
        mIvLess.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // When user press the like button all expressive button speech flag (except less)
                // are set to reset.
                mFlgLike = 0; mFlgYes = 0; mFlgMore = 0; mFlgDntLike = 0; mFlgNo = 0;
                //Value of mFlgImage = 5, indicates less expressive button is pressed
                mFlgImage = 5;
                // If selected category icon is Help -> About me, then show
                // expressive icons of About me category.
                // and set less button icon to pressed using mFlgImage.
                if (mLevelOneItemPos == 8 && mLevelTwoItemPos == 1)
                    setExpressiveButtonToAboutMe(mFlgImage);
                else
                    resetExpressiveButtons(mFlgImage);
                // if value of mShouldReadFullSpeech is false then do not speak full sentence verbiage.
                if (!mShouldReadFullSpeech) {
                    // if value of mFlgLess is 1, then should speak "really less".
                    if (mFlgLess == 1) {
                        speakSpeech(mExprBtnTxt[11]);
                        mFlgLess = 0;
                        //Firebase event
                        singleEvent("ExpressiveIcon","ReallyLess");
                    // if value of mFlgLess is 0, then should speak "less".
                    } else {
                        speakSpeech(mExprBtnTxt[10]);
                        mFlgLess = 1;
                        //Firebase event
                        singleEvent("ExpressiveIcon","Less");
                    }
                // if value of mShouldReadFullSpeech is true, then speak associated less
                // expression verbiage to selected category icon.
                } else {
                    ++mActionBtnClickCount;
                    // Set less button color border to selected category icon.
                    // border color is applied using mActionBtnClickCount and mFlgImage.
                    if(mRecyclerItemsViewList.get(mSelectedItemAdapterPos) != null)
                        setMenuImageBorder(mRecyclerItemsViewList.get(mSelectedItemAdapterPos), true);

                    // if value of mFlgLess is 1, then should speak "really less" expression
                    // verbiage associated to selected category icon.
                    if (mFlgLess == 1) {
                        //Firebase event
                        singleEvent("ExpressiveIcon","ReallyLess");
                        // People and places will have preferences. To speak the correct speech text
                        // preference sort array is used.
                        if (mLevelOneItemPos == CATEGORY_ICON_PEOPLE ||
                                mLevelOneItemPos == CATEGORY_ICON_PLACES) {
                            speakSpeech(mLayerTwoSpeech.get(mLevelOneItemPos).
                                    get(mArrSort[mLevelTwoItemPos]).get(11));
                            singleEvent("ExpressiveGridIcon",
                                    mLayerTwoSpeech.get(mLevelOneItemPos).
                                            get(mArrSort[mLevelTwoItemPos]).get(11));
                        // If Help -> About me category icon is selected,
                        // "really less" expression will speak child's blood group
                        }else if(mLevelOneItemPos == 8 && mLevelTwoItemPos == 1){
                            speakSpeech(mLayerTwoSpeech.get(mLevelOneItemPos).
                                    get(mLevelTwoItemPos).get(10) + getBloodGroup() +end);
                            singleEvent("ExpressiveGridIcon",
                                    mLayerTwoSpeech.get(mLevelOneItemPos).
                                            get(mLevelTwoItemPos).get(10) + getBloodGroup() +end);
                        } else
                            speakSpeech(mLayerTwoSpeech.get(mLevelOneItemPos).
                                    get(mLevelTwoItemPos).get(11));
                        singleEvent("ExpressiveGridIcon",
                                mLayerTwoSpeech.get(mLevelOneItemPos).
                                        get(mLevelTwoItemPos).get(11));
                        //reset mFlgLess to speak "less" expression
                        mFlgLess = 0;
                    // if value of mFlgLess is 0 then Speak associated less expression
                    // verbiage to selected category icon.
                    } else {
                        //Firebase event
                        singleEvent("ExpressiveIcon","Less");
                        // People and places will have preferences. To speak the correct speech text
                        // preference sort array is used.
                        if (mLevelOneItemPos == CATEGORY_ICON_PEOPLE ||
                                mLevelOneItemPos == CATEGORY_ICON_PLACES) {
                            speakSpeech(mLayerTwoSpeech.get(mLevelOneItemPos).
                                    get(mArrSort[mLevelTwoItemPos]).get(10));
                            singleEvent("ExpressiveGridIcon",
                                mLayerTwoSpeech.get(mLevelOneItemPos).
                                    get(mArrSort[mLevelTwoItemPos]).get(10));
                        // If Help -> About me category icon is selected,
                        // "really less" expression will speak child's blood group
                        }else if(mLevelOneItemPos == 8 && mLevelTwoItemPos == 1){
                            speakSpeech(mLayerTwoSpeech.get(mLevelOneItemPos).
                                    get(mLevelTwoItemPos).get(10)+ getBloodGroup()+end);
                            singleEvent("ExpressiveGridIcon",
                                    mLayerTwoSpeech.get(mLevelOneItemPos).
                                            get(mLevelTwoItemPos).get(10)+ getBloodGroup()+end);
                        } else
                            speakSpeech(mLayerTwoSpeech.get(mLevelOneItemPos).
                                    get(mLevelTwoItemPos).get(10));
                            singleEvent("ExpressiveGridIcon",
                                    mLayerTwoSpeech.get(mLevelOneItemPos).
                                            get(mLevelTwoItemPos).get(10));
                        //reset mFlgLess to speak "really less" expression
                        mFlgLess = 1;
                    }
                }
                mIvBack.setImageResource(R.drawable.back);
            }
        });
    }

    /**
     * <p>This function will initialize the click listener to Tts Speak button.
     * When Tts speak button is pressed, broadcast speak request is sent to Text-to-speech service.
     * Message typed in Text-to-speech input view, is synthesized by the service.</p>
     * */
    private void initTTsBtnListener() {
        mIvTts.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                speakSpeech(mEtTTs.getText().toString());
                if(!mEtTTs.getText().toString().equals(""))
                    mIvTts.setImageResource(R.drawable.speaker_pressed);
                //Firebase event
                Bundle bundle = new Bundle();
                bundle.putString("InputName", Settings.Secure.getString(getContentResolver(),
                        Settings.Secure.DEFAULT_INPUT_METHOD));
                bundle.putString("utterence", mEtTTs.getText().toString());
                bundleEvent("Keyboard", bundle);
                //singleEvent("Keyboard", mEtTTs.getText().toString());
                //if expressive buttons always disabled during custom text speech output
                mIvLike.setEnabled(false);
                mIvDontLike.setEnabled(false);
                mIvMore.setEnabled(false);
                mIvLess.setEnabled(false);
                mIvYes.setEnabled(false);
                mIvNo.setEnabled(false);
            }
        });
    }

    /**
     * <p>This function will initialize the click listener to EditText which is used by user to
     * input custom strings.</p>
     * */
    private void initTTsEditTxtListener() {
        mEtTTs.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                // If custom keyboard input text loses focus.
                if (!hasFocus) {
                    // Hide soft mIvKeyboard.
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(mEtTTs.getWindowToken(), 0);
                    // Make it non-editable.
                    mEtTTs.setKeyListener(null);
                }
            }
        });
    }

    /**
     * <p>This function is called when user taps a category icon. It will change the state of
     * category icon pressed. Also, it set the flag for app speak full verbiage sentence.
     * @param view is parent view in selected RecyclerView.
     * @param v is parent relative layout of category icon tapped,
     * @param position is position of a tapped category icon in the RecyclerView.
     * This function
     *             a) Clear every expressive button flags
     *             b) Reset expressive button icons
     *             c) Reset category icon views
     *             d) Set the border to newly selected category icon
     *             e) If same category icon clicked twice that category will open up.
     *             f) Set action bar title.
     *             g) Increment preference count of tapped category icon.</p>
     * */
    public void tappedCategoryItemEvent(View view, View v, int position) {
        mFlgLike = mFlgYes = mFlgMore = mFlgDntLike = mFlgNo = mFlgLess = 0;
        // Clear the last expressive button selection. Set each expressive button to unpressed.
        resetExpressiveButtons(-1);
        // reset every populated category icon before setting the border to selected icon.
        resetRecyclerAllItems();
        mActionBtnClickCount = 0;
        // set border to selected category icon
        setMenuImageBorder(v, true);
        // set true to speak verbiage associated with category icon
        mShouldReadFullSpeech = true;
        String title = getIntent().getExtras().getString("selectedMenuItemPath")+ " ";
        // if user is in people or places category and user pressed any of the category icon then
        // create bundle for firebase event.
        // bundle has values category icon position (index), "level two"
        if (mLevelOneItemPos == CATEGORY_ICON_PEOPLE || mLevelOneItemPos == CATEGORY_ICON_PLACES) {
            speakSpeech(mArrSpeechText[position]);
            Bundle bundle = new Bundle();
            bundle.putString("Icon", mArrSpeechText[position]);
            bundleEvent("Grid",bundle);
        // In below if category icon selected in level one is neither people/places nor help.
        // Also, mLevelTwoItemPos == position is true it means user taps twice on same category icon.
        // If above both conditions are true then open category icon selected in level three.
        }else if(mLevelTwoItemPos == position && mLevelOneItemPos != CATEGORY_ICON_HELP){
            // set intent to open level three category
            Intent intent = new Intent(LevelTwoActivity.this, LevelThreeActivity.class);
            int CATEGORY_ICON_DAILY_ACT = 1;
            // if Daily Activities category is selected in level one and
            // if category icon selected in level two is
            // Daily Activities ->Brushing or
            // Daily Activities ->Toilet or
            // Daily Activities ->Bathing or
            // Daily Activities ->Morning routine or
            // Daily Activities ->Bedtime routine
            // then change intent to open sequence activity.
            if(mLevelOneItemPos == CATEGORY_ICON_DAILY_ACT &&
                    ( mLevelTwoItemPos == 0 ||  mLevelTwoItemPos == 1 || mLevelTwoItemPos == 2 ||
                            mLevelTwoItemPos == 7 || mLevelTwoItemPos == 8 ))
                intent = new Intent(LevelTwoActivity.this, SequenceActivity.class);
            Bundle bundle = new Bundle();
            bundle.putString("Icon", "Opened " + mArrSpeechText[position]);
            bundleEvent("Grid",bundle);
            //intent to open new activity have extra data such position of level one category icon,
            // level two category icon and action bar title (bread crumb)
            intent.putExtra("mLevelOneItemPos", mLevelOneItemPos);
            intent.putExtra("mLevelTwoItemPos", mLevelTwoItemPos);
            intent.putExtra("selectedMenuItemPath", mActionBarTitle+ "/");
            startActivityForResult(intent, REQ_HOME);
        }else {
            speakSpeech(mArrSpeechText[position]);
            Bundle bundle = new Bundle();
            bundle.putString("Icon", mArrSpeechText[position]);
            bundleEvent("Grid",bundle);
        }
        mLevelTwoItemPos = mRecyclerView.getChildLayoutPosition(view);
        mSelectedItemAdapterPos = mRecyclerView.getChildAdapterPosition(view);

        // set title to breadcrumb (or actionbar) of activity.
        if(mLevelOneItemPos == CATEGORY_ICON_PEOPLE ||
                mLevelOneItemPos == CATEGORY_ICON_PLACES ||
                mLevelOneItemPos == CATEGORY_ICON_HELP)
            title += mArrAdapterTxt[mLevelTwoItemPos];
        else
            title += mArrAdapterTxt[mLevelTwoItemPos].substring
                    (0, mArrAdapterTxt[mLevelTwoItemPos].length()-1);
        mActionBarTitle = title;
        getSupportActionBar().setTitle(mActionBarTitle);

        // increment preference count an item if people or place category is selected in level one
        if(mLevelOneItemPos == CATEGORY_ICON_PEOPLE || mLevelOneItemPos == CATEGORY_ICON_PLACES)
            incrementTouchCountOfItem(mLevelTwoItemPos);

        // Help -> About me category have different set of expressive buttons. If user selected
        // About me category then all expressive button icons are changed.
        // sending -1 value to setExpressiveButtonToAboutMe(-1), will only change expressive
        // button icons to About me icons.
        if(mLevelOneItemPos == CATEGORY_ICON_HELP && mLevelTwoItemPos == 1)
            setExpressiveButtonToAboutMe(-1);

        //Below if checks that selected category icon is one of from below listed or not
        // Help -> I am hurt,
        // Help -> I feel sick,
        // Help -> I feel tired
        // Help -> Help me do this,
        // Help -> Allergy,
        // Help -> Danger,
        // Help -> Hazard
        // If it is from above category then disable all expressive icons
        if(mLevelOneItemPos == CATEGORY_ICON_HELP &&
                ((mLevelTwoItemPos == 2) || (mLevelTwoItemPos == 3) || (mLevelTwoItemPos == 4) ||
                        (mLevelTwoItemPos == 5) ||(mLevelTwoItemPos == 12) ||
                        (mLevelTwoItemPos == 13) ||(mLevelTwoItemPos == 14)))
            changeTheExpressiveButtons(DISABLE_EXPR_BTNS);
        // if category icon Help -> Emergency is selected, then disable all expressive icons
        else if(mLevelOneItemPos == CATEGORY_ICON_HELP && mLevelTwoItemPos == 0) {
            changeTheExpressiveButtons(DISABLE_EXPR_BTNS);
            //showCallPreview();
        // if category icon Help -> Unsafe touch is selected, then disable like, yes, more
        // expressive icons
        }else if(mLevelOneItemPos == CATEGORY_ICON_HELP && mLevelTwoItemPos == 10)
            unsafeTouchDisableExpressiveButtons();
        // if category icon Help -> Safety is selected, then disable no expressive icon only
        else if(mLevelOneItemPos == CATEGORY_ICON_HELP && mLevelTwoItemPos == 15)
            safetyDisableExpressiveButtons();
        else
            changeTheExpressiveButtons(!DISABLE_EXPR_BTNS);
        mIvBack.setImageResource(R.drawable.back);
    }

    /**
     * <p>This function will set capacity of ArrayList holding RecyclerView items to the array size.
     * Array is selected based on category icon opened in {@link MainActivity}</p>
     * */
    private void initializeArrayListOfRecycler() {
        int size = -1;
        switch (mLevelOneItemPos) {
            case 0:
                size = getResources().getStringArray(R.array.arrLevelTwoGreetFeelSpeechText).length;
                break;
            case 1:
                size = getResources().getStringArray(R.array.arrLevelTwoEatSpeechText).length;
                break;
            case 2:
                size = getResources().getStringArray(R.array.arrLevelTwoEatSpeechText).length;
                break;
            case 3:
                size = getResources().getStringArray(R.array.arrLevelTwoFunSpeechText).length;
                break;
            case 4:
                size = getResources().getStringArray(R.array.arrLevelTwoLearningSpeechText).length;
                break;
            case 5:
                size = getResources().getStringArray(R.array.arrLevelTwoPeopleSpeechText).length;
                break;
            case 6:
                size = getResources().getStringArray(R.array.arrLevelTwoPlacesSpeechText).length;
                break;
            case 7:
                size = getResources().getStringArray(R.array.arrLevelTwoTimeWeatherSpeechText).length;
                break;
            case 8:
                size = getResources().getStringArray(R.array.arrLevelTwoHelpSpeechText).length;
                break;
        }
        // Set the capacity of mRecyclerItemsViewList list to total number of category icons to be
        // populated on the screen.
        mRecyclerItemsViewList = new ArrayList<>(size);
        while(mRecyclerItemsViewList.size() <= size) mRecyclerItemsViewList.add(null);
    }

    /**
     * <p>This function will display/ hide action bar title.
     * If {@param showTitle} is set then title is displayed otherwise not.</p>
     * */
    private void showActionBarTitle(boolean showTitle){
        if (showTitle)
            getSupportActionBar().setTitle(actionBarTitleTxt);
        else{
            actionBarTitleTxt = getSupportActionBar().getTitle().toString();
            getSupportActionBar().setTitle("");
        }
    }

    /**
     * <p>This function will send speech output request to
     * {@link com.dsource.idc.jellowintl.utility.JellowTTSService} Text-to-speech Engine.
     * The string in {@param speechText} is speech output request string.</p>
     * */
    private void speakSpeech(String speechText){
        Intent intent = new Intent("com.dsource.idc.jellowintl.SPEECH_TEXT");
        intent.putExtra("speechText", speechText.toLowerCase());
        sendBroadcast(intent);
    }

    /**
     * <p>This function will:
     *     a) Read speech text from arrays for navigation buttons.
     *     b) Read speech text from arrays for expressive buttons.
     *     c) Read verbiage lines into {@link LevelTwoVerbiageModel} model.
     *     d) Read speech and adapter text from arrays for category icons
     *        for People/Places if in {@link MainActivity } mArrIcon/places
     *        category is selected.
     *     e) Read speech and adapter text from arrays for category icons
     *        other than People/Places.</p>
     * */
    private void loadArraysFromResources() {
        mExprBtnTxt = getResources().getStringArray(R.array.arrActionSpeech);
        mNavigationBtnTxt = getResources().getStringArray(R.array.arrNavigationSpeech);

        LevelTwoVerbiageModel verbiageModel = new Gson()
                .fromJson(getString(R.string.levelTwoVerbiage), LevelTwoVerbiageModel.class);
        mLayerTwoSpeech = verbiageModel.getVerbiageModel();
        // fill speech and adapter text arrays
        retrieveSpeechAndAdapterArrays(mLevelOneItemPos);
    }

    /**
     * <p>This function will find and return the blood group of user
     * @return blood group of user.</p>
     * */
    private String getBloodGroup() {
        switch(mSession.getBlood()){
            case  1: return getString(R.string.aPos);
            case  2: return getString(R.string.aNeg);
            case  3: return getString(R.string.bPos);
            case  4: return getString(R.string.bNeg);
            case  5: return getString(R.string.abPos);
            case  6: return getString(R.string.abNeg);
            case  7: return getString(R.string.oPos);
            case  8: return getString(R.string.oNeg);
            default: return "";
        }
    }

    /**
     * <p>Category icon "Help" -> "Safety" when selected, it needed to disable expressive
     * no button. This function disables the No expressive button and keep
     * enabled other buttons</p>
     * */
    private void safetyDisableExpressiveButtons() {
        mIvLike.setAlpha(1f);
        mIvDontLike.setAlpha(1f);
        mIvYes.setAlpha(1f);
        mIvNo.setAlpha(0.5f);
        mIvMore.setAlpha(1f);
        mIvLess.setAlpha(1f);
        mIvLike.setEnabled(true);
        mIvDontLike.setEnabled(true);
        mIvYes.setEnabled(true);
        mIvNo.setEnabled(false);
        mIvMore.setEnabled(true);
        mIvLess.setEnabled(true);
    }

    /**
     * <p>Category icon "Help" -> "Unsafe touch" when selected, it needed to disable expressive
     * buttons Like, Yes, More. This function disables the Like, Yes, More expressive buttons
     * and keep enabled other buttons</p>
     * */
    private void unsafeTouchDisableExpressiveButtons() {
        mIvDontLike.setAlpha(1f);
        mIvNo.setAlpha(1f);
        mIvLess.setAlpha(1f);
        mIvDontLike.setEnabled(true);
        mIvNo.setEnabled(true);
        mIvLess.setEnabled(true);
        mIvLike.setAlpha(0.5f);
        mIvYes.setAlpha(0.5f);
        mIvMore.setAlpha(0.5f);
        mIvLike.setEnabled(false);
        mIvYes.setEnabled(false);
        mIvMore.setEnabled(false);
    }

    /**
     * <p>This function enable or disable the expressive buttons using {@param setDisable}:
     * {@param setDisable}, if setDisable = true, buttons are disabled otherwise
     * enabled.</p>
     * */
    private void changeTheExpressiveButtons(boolean setDisable) {
        if(setDisable) {
            mIvLike.setAlpha(0.5f);
            mIvDontLike.setAlpha(0.5f);
            mIvYes.setAlpha(0.5f);
            mIvNo.setAlpha(0.5f);
            mIvMore.setAlpha(0.5f);
            mIvLess.setAlpha(0.5f);
            mIvLike.setEnabled(false);
            mIvDontLike.setEnabled(false);
            mIvYes.setEnabled(false);
            mIvNo.setEnabled(false);
            mIvMore.setEnabled(false);
            mIvLess.setEnabled(false);
        }else{
            mIvLike.setAlpha(1f);
            mIvDontLike.setAlpha(1f);
            mIvYes.setAlpha(1f);
            mIvNo.setAlpha(1f);
            mIvMore.setAlpha(1f);
            mIvLess.setAlpha(1f);
            mIvLike.setEnabled(true);
            mIvDontLike.setEnabled(true);
            mIvYes.setEnabled(true);
            mIvNo.setEnabled(true);
            mIvMore.setEnabled(true);
            mIvLess.setEnabled(true);
        }
    }

    /**
     * <p>This function set the border to category icons. This function first extracts the view to
     * which border should applied then apply the border.
     * {@param recyclerChildView} is a parent view extracted from recycler view when category icon is tapped.
     * {@param setBorder} is set if category icon tapped and a color border should appear on the view other wise
     *  transparent color is set to border.
     * </p>
     * */
    private void setMenuImageBorder(View recyclerChildView, boolean setBorder) {
        GradientDrawable gd = (GradientDrawable) recyclerChildView.findViewById(R.id.borderView).getBackground();
        if(setBorder){
            // mActionBtnClickCount = 0, brown color border is set.
            if (mActionBtnClickCount > 0) {
                // mFlgImage define color of border.
                switch (mFlgImage) {
                    case 0: gd.setColor(ContextCompat.getColor(this,R.color.colorLike)); break;
                    case 1: gd.setColor(ContextCompat.getColor(this,R.color.colorDontLike)); break;
                    case 2: gd.setColor(ContextCompat.getColor(this,R.color.colorYes)); break;
                    case 3: gd.setColor(ContextCompat.getColor(this,R.color.colorNo)); break;
                    case 4: gd.setColor(ContextCompat.getColor(this,R.color.colorMore)); break;
                    case 5: gd.setColor(ContextCompat.getColor(this,R.color.colorLess)); break;
                }
            } else
                gd.setColor(ContextCompat.getColor(this,R.color.colorSelect));
        } else
            gd.setColor(ContextCompat.getColor(this,android.R.color.transparent));
    }

    /**
     * <p>This function first reset if any expressive button is pressed. Then set an
     * expressive button to pressed. {@param image_flag} identifies which expressive button
     * is pressed.
     * If user had previously pressed any expressive button (i.e. state of any expressive
     * button is pressed) and then user presses some other expressive button, it is needed to
     * clear previously pressed expressive button state and home button state (if pressed).
     * {@param image_flag} is a index of expressive button.
     *  e.g. From top to bottom 0 - like button, 1 - don't like button likewise.
     *  To set home button to pressed state image_flag value must be 6</p>
     * */
    private void resetExpressiveButtons(int image_flag) {
        // clear previously selected any expressive button or home button
        mIvLike.setImageResource(R.drawable.like);
        mIvDontLike.setImageResource(R.drawable.dontlike);
        mIvYes.setImageResource(R.drawable.yes);
        mIvNo.setImageResource(R.drawable.no);
        mIvMore.setImageResource(R.drawable.more);
        mIvLess.setImageResource(R.drawable.less);
        mIvHome.setImageResource(R.drawable.home);
        // set expressive button or home button to pressed state
        switch (image_flag){
            case 0: mIvLike.setImageResource(R.drawable.like_pressed); break;
            case 1: mIvDontLike.setImageResource(R.drawable.dontlike_pressed); break;
            case 2: mIvYes.setImageResource(R.drawable.yes_pressed); break;
            case 3: mIvNo.setImageResource(R.drawable.no_pressed); break;
            case 4: mIvMore.setImageResource(R.drawable.more_pressed); break;
            case 5: mIvLess.setImageResource(R.drawable.less_pressed); break;
            case 6: mIvHome.setImageResource(R.drawable.home_pressed); break;
            default: break;
        }
    }

    /**
     * <p>This function reset the border for all category icons that are populated
     * in recycler view.</p>
     * */
    private void resetRecyclerAllItems() {
        for(int i = 0; i< mRecyclerView.getChildCount(); ++i)
            setMenuImageBorder(mRecyclerView.getChildAt(i), false);
    }

    /**
     * <p>This function increment the touch count for the selected category icon.
     * In {@link LevelTwoActivity}, only People and Places has preferences which are stored
     * directly into preferences using this function.</p>
     * */
    private void incrementTouchCountOfItem(int levelTwoItemPosition) {
        StringBuilder stringBuilder = new StringBuilder();
        // increment tap count of selected category icon
        mArrPeoplePlaceTapCount[mArrSort[levelTwoItemPosition]] += 1;
        // create preference string.
        for (Integer countPeople : mArrPeoplePlaceTapCount)
            stringBuilder.append(countPeople).append(",");
        // store preference string in session preferences.
        if(mLevelOneItemPos == CATEGORY_ICON_PEOPLE) {
            mSession.setPeoplePreferences(stringBuilder.toString());
        }else {
            mSession.setPlacesPreferences(stringBuilder.toString());
        }
    }

    /**
     * <p>This function first set the all expressive button to About me expressive buttons.
     * Then set an about me expressive button to pressed using {@param image_flag}.
     * image_flag identifies which expressive button is pressed.
     * If user pressed had previously pressed any expressive button (i.e. state of any expressive
     * button is pressed) then user is pressed some other expressive button, it is needed to
     * clear previously pressed expressive button state and home button state (if pressed).
     * {@param image_flag} is a index of expressive button.
     *  e.g. From top to bottom 0 - like button, 1 - don't like button likewise.
     *  To set home button to pressed state image_flag value must be 6</p>
     * */
    private void setExpressiveButtonToAboutMe(int image_flag){
        //change icon images of expressive button to about me expressive button icons
        mIvLike.setImageResource(R.drawable.mynameis);
        mIvDontLike.setImageResource(R.drawable.caregiver);
        mIvYes.setImageResource(R.drawable.email);
        mIvNo.setImageResource(R.drawable.address);
        mIvMore.setImageResource(R.drawable.contact);
        mIvLess.setImageResource(R.drawable.bloodgroup);
        // set expressive button to pressed state
        switch (image_flag){
            case 0: mIvLike.setImageResource(R.drawable.mynameis_pressed); break;
            case 1: mIvDontLike.setImageResource(R.drawable.caregiver_pressed); break;
            case 2: mIvYes.setImageResource(R.drawable.email_pressed); break;
            case 3: mIvNo.setImageResource(R.drawable.address_pressed); break;
            case 4: mIvMore.setImageResource(R.drawable.contact_pressed); break;
            case 5: mIvLess.setImageResource(R.drawable.blooedgroup_pressed); break;
            case 6: mIvHome.setImageResource(R.drawable.home_pressed); break;
            default: break;
        }
    }

    /**
     * <p>This function retrieve speech text array and adapter text array from arrays
     * resource using category icon index selected in {@link MainActivity}.
     * @param levelOneItemPos is a index of category icon selected in
     * {@link MainActivity}.</p>
     * */
    public void retrieveSpeechAndAdapterArrays(int levelOneItemPos){
        switch (levelOneItemPos){
            case 0:
                mArrSpeechText = getResources().getStringArray(R.array.arrLevelTwoGreetFeelSpeechText);
                mArrAdapterTxt = getResources().getStringArray(R.array.arrLevelTwoGreetFeelAdapterText);
                break;
            case 1:
                mArrSpeechText = getResources().getStringArray(R.array.arrLevelTwoDailyActSpeechText);
                mArrAdapterTxt = getResources().getStringArray(R.array.arrLevelTwoDailyActAdapterText);
                break;
            case 2:
                mArrSpeechText = getResources().getStringArray(R.array.arrLevelTwoEatSpeechText);
                mArrAdapterTxt = getResources().getStringArray(R.array.arrLevelTwoEatAdapterText);
                break;
            case 3:
                mArrSpeechText = getResources().getStringArray(R.array.arrLevelTwoFunSpeechText);
                mArrAdapterTxt = getResources().getStringArray(R.array.arrLevelTwoFunAdapterText);
                break;
            case 4:
                mArrSpeechText = getResources().getStringArray(R.array.arrLevelTwoLearningSpeechText);
                mArrAdapterTxt = getResources().getStringArray(R.array.arrLevelTwoLearningAdapterText);
                break;
            case 5:
                // As People category uses preferences, speech and text arrays are sorted using
                // preference algorithm.
                useSortToLoadArray(getResources().getStringArray(R.array.arrLevelTwoPeopleSpeechText),
                        getResources().getStringArray(R.array.arrLevelTwoPeopleAdapterText));
                break;
            case 6:
                // As People category uses preferences, speech and text arrays are sorted using
                // preference algorithm.
                useSortToLoadArray(getResources().getStringArray(R.array.arrLevelTwoPlacesSpeechText),
                                getResources().getStringArray(R.array.arrLevelTwoPlacesAdapterText));
                break;
            case 7:
                mArrSpeechText = getResources().getStringArray(R.array.arrLevelTwoTimeWeatherSpeechText);
                mArrAdapterTxt = getResources().getStringArray(R.array.arrLevelTwoTimeWeatherAdapterText);
                break;
            case 8:
                mArrSpeechText = getResources().getStringArray(R.array.arrLevelTwoHelpSpeechText);
                mArrAdapterTxt = getResources().getStringArray(R.array.arrLevelTwoHelpAdapterText);
                break;
        }
    }

    /**
     * <p>This function will create sort index array for people/places category.
     * If any category from people or places is not selected in level one {@link MainActivity}
     * then this function is not used.
     * As per the category icon selected in the level one {@link MainActivity}
     * category icons are populated in this level using preferences.
     * "Most tapped category icon will have highest preference value",
     * in this fashion preferences are defined for category icons.
     * In level two, all category icon preferences are stored in session
     * preferences (shared preferences). Preferences are stored in the
     * from comma separated values string.
     *  e.g. "5,4,9,2,5"
     * This preference string is retrieved using {@link SessionManager} class.
     * Then each value in preference string is converted into individual tokens and
     * followed by storing it into arrays. This array is known as Tap count array. Value
     * in 0th index of tap count array is tap count (number of times user tapped) for 0th element
     * in adapter array/speech array.
     *  e.g mArrIconTapCount = [5,4,9,2,5]
     * Preferences are applied to a category during adapter setup only. To apply preferences to a
     * category below steps to be followed sequentially:
     *   1. Retrieves users stored preferences from {@link SessionManager} as "savedString".
     *   2. Convert savedString into tokens and then store preferences "savedString"
     *      into preference array.
     *   3. From preference array create index array. Index array is a integer array, it
     *      has index of most-preferred/most-tapped element first,
     *      then followed by second most-preferred/most-tapped element index and so on.
     *      e.g. Consider preference array as
     *      [5, 4, 9 , 2, 5] then its index array is [2, 0, 4, 1, 3].
     *   4. Then speech and below text array elements are rearranged using index array. In this
     *      step, Speech and below text array elements are arranged in such a way that most
     *      used icon will appear first in the category.
     * @param arrSpeechTxt, raw speech text array,  used to sort the speech elements.
     * @param arrAdapterTxt, raw below text array,  used to sort the below text elements.</p>
     * */
    private void useSortToLoadArray(String[] arrSpeechTxt, String[] arrAdapterTxt) {
        // Get preference string from shared preferences into "savedString" variable
        String savedString = "";
        if (mLevelOneItemPos == CATEGORY_ICON_PEOPLE) {
            savedString = mSession.getPeoplePreferences();
        }else if (mLevelOneItemPos == CATEGORY_ICON_PLACES) {
            savedString = mSession.getPlacesPreferences();
        }

        // Create temporary array of user taps on category icons
        Integer[]  mArrIconTapCount = new Integer[arrSpeechTxt.length];
        mArrSort = new Integer[arrSpeechTxt.length];

        //initialize mArrIconTapCount[i] = 0 so that every category icon have same preference.
        //mArrSort array is global index sort array.
        for (int i = 0; i < arrSpeechTxt.length; ++i) {
            mArrIconTapCount[i] = 0;
            mArrSort[i] = i;
        }

        // Each value in savedString is now converted into individual tokens.
        if(!savedString.equals("")){
            StringTokenizer st = new StringTokenizer(savedString, ",");

            // create tap count array (mArrIconTapCount) of user taps using individual these tokens
            for (int i = 0; i < arrSpeechTxt.length; ++i)
                mArrIconTapCount[i] = Integer.parseInt(st.nextToken());
        }

        //mArrPeoplePlaceTapCount is global variable to activity;
        // It holds tap count of each sub-category icon within People or places categories.
        mArrPeoplePlaceTapCount = new Integer[arrSpeechTxt.length];
        for(int i = 0; i< arrSpeechTxt.length ; ++i)
            mArrPeoplePlaceTapCount[i] = mArrIconTapCount[i];

        //create index sort array using tap count array (mArrIconTapCount)
        IndexSorter<Integer> is = new IndexSorter<Integer>(mArrIconTapCount);
        is.sort();

        //get index sort array for given tap count array(mArrIconTapCount)
        mArrSort =  new Integer[mLayerTwoSpeech.get(mLevelOneItemPos).size()];
        int j = -1;
        for (Integer i : is.getIndexes()) {
            mArrSort[++j] = i;
        }

        mArrSpeechText = new String[mArrIconTapCount.length];
        mArrAdapterTxt = new String[mArrIconTapCount.length];
        int idx;
        // arrange speech and adapter icons using preferences.
        for (int i = 0; i < mArrIconTapCount.length; ++i) {
            idx = mArrSort[i];
            mArrSpeechText[i] = arrSpeechTxt[idx];
            mArrAdapterTxt[i] = arrAdapterTxt[idx];
        }
    }

    /**
     * Unused code in current version
     **/
    private void showCallPreview(){
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED){
            //startCall("tel:" + mSession.getCaregiverNumber());
        } else {
            //requestCallPermission();
        }
    }

    /**
     * Unused code in current version
     **/
    private void requestCallPermission(){
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.CALL_PHONE)){
            new ToastWithCustomTime(this,"Call permission is not available. Requesting permission for making a call in case of an emergency.", 10000);
        } else {
            new ToastWithCustomTime(this,"Call access is required to make an emergency call. Please enable call permission from app settings.", 10000);
        }
        ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.CALL_PHONE}, MY_PERMISSIONS_REQUEST_CALL_PHONE);
    }

    /**
     * Unused code in current version
     **/
    public void startCall(String contact){
        Intent callintent = new Intent(Intent.ACTION_CALL);
        callintent.setData(Uri.parse(contact));
        startActivity(callintent);
    }
}