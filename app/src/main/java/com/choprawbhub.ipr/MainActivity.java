package com.choprawbhub.ipr;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Locale;

public class MainActivity extends Activity {
    SQLiteDatabase db;
    FrameLayout root;
    LinearLayout vTrk, vPrf, vHub, vCrd, vHubCrd, loadingOverlay, periodFilterRow;
    Button bT, bP, bH, bCatAgent, bCatKirana, bCatAll, bSubDay, bSubYearly, bSort, bShareHub;
    TextView tCnt, tHubOfdDel, tHubOfpPik, tHubDnpDnpc, tGapTarget, tTopConv, tTopDnpc, tPersonalBest;
    ArrayList<String[]> ords = new ArrayList<>();
    BaseAdapter adp;
    String currentCategory = "AGENT", mode = "daily", CSV = "https://docs.google.com/spreadsheets/d/1zGBbIrvN3yUEV9K2_YKfVaS71i69QS1aVqXP7bzkS2o/export?format=csv";
    boolean isHighToLow = true;
    long lastSyncTime = 0;

    Handler autoSyncHandler = new Handler(Looper.getMainLooper());
    Runnable autoSyncRunnable = new Runnable() {
        public void run() {
            new Thread(() -> doSync(true)).start();
            autoSyncHandler.postDelayed(this, 120000);
        }
    };

    GradientDrawable box(int c, int r, int scol, int sw) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(c); g.setCornerRadius(r);
        if (sw > 0) g.setStroke(sw, scol);
        return g;
    }

    TextView tv(String text, int color, float size, boolean bold) {
        TextView t = new TextView(this);
        t.setText(text); t.setTextColor(color); t.setTextSize(size);
        if (bold) t.setTypeface(Typeface.DEFAULT_BOLD);
        return t;
    }

    boolean isKiranaAgent(String name) {
        if (name == null) return false;
        return name.trim().equalsIgnoreCase("Ratan Sarkar");
    }

    String getOperationalDate() {
        try {
            Calendar cal = Calendar.getInstance();
            if (cal.get(Calendar.HOUR_OF_DAY) < 4) cal.add(Calendar.DAY_OF_YEAR, -1);
            return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.getTime());
        } catch (Exception e) {
            return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new java.util.Date());
        }
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        try {
            db = openOrCreateDatabase("ChoprawHubStandalone.db", MODE_PRIVATE, null);
            db.execSQL("CREATE TABLE IF NOT EXISTS ord (t TEXT PRIMARY KEY, d TEXT);");
            db.execSQL("CREATE TABLE IF NOT EXISTS prf (n TEXT, o INT, l INT, p INT, k INT, dt TEXT);");
            db.execSQL("CREATE TABLE IF NOT EXISTS hub_prf (hname TEXT, o TEXT, l TEXT, lc TEXT, p TEXT, k TEXT, kc TEXT, dnp TEXT, dnpc TEXT, tc TEXT, dt TEXT);");
        } catch (Exception ignored) {}

        showSplashScreen();
        autoSyncHandler.postDelayed(autoSyncRunnable, 120000);
    }

    void showSplashScreen() {
        LinearLayout splash = new LinearLayout(this);
        splash.setOrientation(LinearLayout.VERTICAL);
        splash.setGravity(Gravity.CENTER);
        splash.setBackgroundColor(Color.parseColor("#090A0F"));

        TextView tApp = tv("CHOPRAWBHUB_IPR", Color.parseColor("#00E676"), 24f, true);
        tApp.setPadding(0, 0, 0, 24);
        splash.addView(tApp);

        TextView tBuild = tv("BUILD BY ADARSH", Color.parseColor("#38BDF8"), 16f, true);
        tBuild.setPadding(0, 8, 0, 6);
        splash.addView(tBuild);

        TextView tMan = tv("MANEGE BY BHASKAR", Color.parseColor("#FBBF24"), 16f, true);
        splash.addView(tMan);

        setContentView(splash);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            buildUI();
            new Thread(() -> doSync(true)).start();
        }, 2500);
    }

    void buildUI() {
        root = new FrameLayout(this);
        root.setBackgroundColor(Color.parseColor("#090A0F"));
        setContentView(root);

        LinearLayout main = new LinearLayout(this);
        main.setOrientation(LinearLayout.VERTICAL);
        main.setPadding(12, 16, 12, 12);

        LinearLayout topBar = new LinearLayout(this);
        topBar.setOrientation(LinearLayout.HORIZONTAL);
        topBar.setGravity(Gravity.CENTER_VERTICAL);
        topBar.setPadding(0, 0, 0, 8);

        TextView title = tv("📦 CHOPRAWBHUB_IPR", Color.parseColor("#00E676"), 15f, true);
        topBar.addView(title, new LinearLayout.LayoutParams(0, -2, 1f));

        Button bCalc = new Button(this);
        bCalc.setText("🧮 CALC");
        bCalc.setBackground(box(Color.parseColor("#38BDF8"), 8, 0, 0));
        bCalc.setTextColor(Color.BLACK);
        bCalc.setTextSize(11f);
        bCalc.setTypeface(Typeface.DEFAULT_BOLD);
        bCalc.setOnClickListener(v -> showConversionCalculatorDialog());

        Button bSync = new Button(this);
        bSync.setText("🔄 SYNC");
        bSync.setBackground(box(Color.parseColor("#1F2232"), 8, Color.parseColor("#00E676"), 1));
        bSync.setTextColor(Color.parseColor("#00E676"));
        bSync.setTextSize(11f);
        bSync.setTypeface(Typeface.DEFAULT_BOLD);
        bSync.setOnClickListener(v -> new Thread(() -> doSync(false)).start());

        LinearLayout.LayoutParams btnLp = new LinearLayout.LayoutParams(-2, -2);
        btnLp.setMargins(6, 0, 0, 0);
        topBar.addView(bCalc, btnLp);
        topBar.addView(bSync, btnLp);
        main.addView(topBar);

        LinearLayout tb = new LinearLayout(this);
        tb.setPadding(8, 8, 8, 4);
        bT = makeTabBtn("🔍 ORDER", 0);
        bP = makeTabBtn("📈 PERF", 1);
        bH = makeTabBtn("⚔️ HUBS", 2);
        LinearLayout.LayoutParams tLp = new LinearLayout.LayoutParams(0, -2, 1f);
        tLp.setMargins(2, 0, 2, 0);
        tb.addView(bT, tLp); tb.addView(bP, new LinearLayout.LayoutParams(tLp)); tb.addView(bH, new LinearLayout.LayoutParams(tLp));
        main.addView(tb);

        FrameLayout body = new FrameLayout(this);
        body.setPadding(12, 6, 12, 10);
        main.addView(body, new LinearLayout.LayoutParams(-1, -1));

        vTrk = new LinearLayout(this);
        vTrk.setOrientation(LinearLayout.VERTICAL);
        EditText s = new EditText(this);
        s.setHint("🔍 Search last digits of Track ID...");
        s.setHintTextColor(Color.parseColor("#717688"));
        s.setTextColor(Color.WHITE);
        s.setBackground(box(Color.parseColor("#12141D"), 12, Color.parseColor("#00E676"), 1));
        s.setPadding(16, 14, 16, 14);
        s.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence c, int i, int i1, int i2) {}
            public void onTextChanged(CharSequence c, int i, int i1, int i2) { qry(c.toString().trim()); }
            public void afterTextChanged(Editable e) {}
        });
        vTrk.addView(s);
        tCnt = tv("📦 Total Trackable Orders: --", Color.parseColor("#00E676"), 12.5f, true);
        tCnt.setPadding(6, 10, 6, 8);
        vTrk.addView(tCnt);

        ListView lv = new ListView(this);
        lv.setDivider(null); lv.setDividerHeight(10);
        adp = new BaseAdapter() {
            public int getCount() { return ords.size(); }
            public Object getItem(int i) { return ords.get(i); }
            public long getItemId(int i) { return i; }
            public View getView(int i, View v, ViewGroup p) {
                LinearLayout c = new LinearLayout(MainActivity.this);
                c.setOrientation(LinearLayout.VERTICAL);
                c.setPadding(16, 14, 16, 14);
                c.setBackground(box(Color.parseColor("#12141D"), 12, Color.parseColor("#1E2235"), 1));
                String[] it = ords.get(i);
                c.addView(tv("📦 Track: " + it[0], Color.parseColor("#38BDF8"), 14.5f, true));
                c.addView(tv("🛒 Order: " + it[1], Color.parseColor("#00E676"), 13.5f, false));

                LinearLayout row = new LinearLayout(MainActivity.this);
                row.setPadding(0, 8, 0, 0);
                Button bCp = new Button(MainActivity.this);
                bCp.setText("📋 Copy");
                bCp.setBackground(box(Color.parseColor("#1F222E"), 8, Color.parseColor("#00E676"), 1));
                bCp.setTextColor(Color.parseColor("#00E676"));
                bCp.setTextSize(11f);
                bCp.setOnClickListener(vw -> {
                    String sub = it[1].length() >= 6 ? it[1].substring(it[1].length() - 6) : it[1];
                    ((ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("O", sub));
                    Toast.makeText(MainActivity.this, "Copied: " + sub, Toast.LENGTH_SHORT).show();
                });
                Button bWp = new Button(MainActivity.this);
                bWp.setText("💬 WhatsApp");
                bWp.setBackground(box(Color.parseColor("#25D366"), 8, 0, 0));
                bWp.setTextColor(Color.BLACK);
                bWp.setTextSize(11f);
                bWp.setOnClickListener(vw -> {
                    String msg = "नमस्ते! आपका पार्सल (Track ID: " + it[0] + ") आज डिलीवरी के लिए निकला है। OTP तैयार रखें। - Delivery Executive";
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://api.whatsapp.com/send?text=" + Uri.encode(msg))));
                });
                LinearLayout.LayoutParams bLp = new LinearLayout.LayoutParams(0, -2, 1f);
                bLp.setMargins(0, 0, 6, 0);
                row.addView(bCp, bLp); row.addView(bWp, new LinearLayout.LayoutParams(0, -2, 1f));
                c.addView(row);
                return c;
            }
        };
        lv.setAdapter(adp);
        vTrk.addView(lv, new LinearLayout.LayoutParams(-1, -1));
        body.addView(vTrk);

        vPrf = new LinearLayout(this);
        vPrf.setOrientation(LinearLayout.VERTICAL);
        vPrf.setVisibility(View.GONE);

        LinearLayout catRow = new LinearLayout(this);
        catRow.setPadding(0, 0, 0, 6);
        bCatAgent = makeCatBtn("👥 AGENT", "AGENT");
        bCatKirana = makeCatBtn("🏪 RATAN SARKAR", "KIRANA");
        bCatAll = makeCatBtn("🌐 ALL (MIX)", "ALL");
        catRow.addView(bCatAgent, tLp); catRow.addView(bCatKirana, new LinearLayout.LayoutParams(tLp)); catRow.addView(bCatAll, new LinearLayout.LayoutParams(tLp));
        vPrf.addView(catRow);

        periodFilterRow = new LinearLayout(this);
        periodFilterRow.setPadding(0, 0, 0, 6);
        vPrf.addView(periodFilterRow);

        tPersonalBest = tv("🏆 Hub Best: --", Color.parseColor("#FBBF24"), 12.5f, true);
        tPersonalBest.setBackground(box(Color.parseColor("#1C1E2A"), 10, Color.parseColor("#FBBF24"), 1));
        tPersonalBest.setPadding(14, 10, 14, 10);
        vPrf.addView(tPersonalBest);

        LinearLayout hubBox = new LinearLayout(this);
        hubBox.setOrientation(LinearLayout.VERTICAL);
        hubBox.setBackground(box(Color.parseColor("#12141D"), 14, Color.parseColor("#38BDF8"), 1));
        hubBox.setPadding(16, 12, 16, 12);
        LinearLayout.LayoutParams hbLp = new LinearLayout.LayoutParams(-1, -2);
        hbLp.setMargins(0, 8, 0, 8);
        hubBox.setLayoutParams(hbLp);
        hubBox.addView(tv("🏢 CHOPRAWBHUB_IPR", Color.parseColor("#38BDF8"), 16f, true));

        tHubOfdDel = tv("OFD/DEL = --", Color.WHITE, 13.5f, false);
        tHubOfpPik = tv("OFP/PIK = --", Color.WHITE, 13.5f, false);
        tHubDnpDnpc = tv("DNP/DNPC = --", Color.parseColor("#34D399"), 14f, true);
        tGapTarget = tv("", Color.parseColor("#FB923C"), 13f, true);
        hubBox.addView(tHubOfdDel); hubBox.addView(tHubOfpPik); hubBox.addView(tHubDnpDnpc); hubBox.addView(tGapTarget);
        vPrf.addView(hubBox);

        LinearLayout sm = new LinearLayout(this);
        sm.setPadding(0, 0, 0, 8);
        LinearLayout sc1 = makeSummaryCard("TOP CONVERSION", Color.parseColor("#00E676"), true);
        LinearLayout sc2 = makeSummaryCard("TOP DNPC", Color.parseColor("#FB923C"), false);
        sm.addView(sc1, new LinearLayout.LayoutParams(0, -2, 1f));
        LinearLayout.LayoutParams sc2Lp = new LinearLayout.LayoutParams(0, -2, 1f);
        sc2Lp.setMargins(8, 0, 0, 0);
        sm.addView(sc2, sc2Lp);
        vPrf.addView(sm);

        LinearLayout actRow = new LinearLayout(this);
        bSort = new Button(this);
        bSort.setText("↕️ Sort Rate");
        bSort.setBackground(box(Color.parseColor("#1C1E2A"), 8, 0, 0));
        bSort.setTextColor(Color.WHITE);
        bSort.setTextSize(11.5f);
        bSort.setTypeface(Typeface.DEFAULT_BOLD);
        bSort.setOnClickListener(v -> { isHighToLow = !isHighToLow; load(); });

        bShareHub = new Button(this);
        bShareHub.setText("📢 Share Hub");
        bShareHub.setBackground(box(Color.parseColor("#25D366"), 8, 0, 0));
        bShareHub.setTextColor(Color.BLACK);
        bShareHub.setTextSize(11.5f);
        bShareHub.setTypeface(Typeface.DEFAULT_BOLD);
        bShareHub.setOnClickListener(v -> showHubShareChooserDialog());

        LinearLayout.LayoutParams aLp = new LinearLayout.LayoutParams(0, -2, 1f);
        aLp.setMargins(0, 0, 6, 8);
        actRow.addView(bSort, aLp);
        actRow.addView(bShareHub, new LinearLayout.LayoutParams(0, -2, 1f));
        vPrf.addView(actRow);

        ScrollView sv = new ScrollView(this);
        vCrd = new LinearLayout(this);
        vCrd.setOrientation(LinearLayout.VERTICAL);
        sv.addView(vCrd);
        vPrf.addView(sv, new LinearLayout.LayoutParams(-1, -1));
        body.addView(vPrf);

        vHub = new LinearLayout(this);
        vHub.setOrientation(LinearLayout.VERTICAL);
        vHub.setVisibility(View.GONE);
        ScrollView svHub = new ScrollView(this);
        vHubCrd = new LinearLayout(this);
        vHubCrd.setOrientation(LinearLayout.VERTICAL);
        svHub.addView(vHubCrd);
        vHub.addView(svHub, new LinearLayout.LayoutParams(-1, -1));
        body.addView(vHub);

        root.addView(main);
        loadingOverlay = new LinearLayout(this);
        loadingOverlay.setGravity(Gravity.CENTER);
        loadingOverlay.setBackgroundColor(Color.parseColor("#DD090A0F"));
        loadingOverlay.setVisibility(View.GONE);
        loadingOverlay.addView(new ProgressBar(this));
        root.addView(loadingOverlay, new FrameLayout.LayoutParams(-1, -1));

        switchCategory("AGENT");
                  }
      Button makeTabBtn(String t, int idx) {
        Button b = new Button(this);
        b.setText(t); b.setTextSize(9.5f); b.setTypeface(Typeface.DEFAULT_BOLD);
        b.setOnClickListener(v -> switchTab(idx));
        return b;
    }

    Button makeCatBtn(String t, String cat) {
        Button b = new Button(this);
        b.setText(t); b.setTextSize(10.5f); b.setTypeface(Typeface.DEFAULT_BOLD);
        b.setOnClickListener(v -> switchCategory(cat));
        return b;
    }

    LinearLayout makeSummaryCard(String title, int accent, boolean isConv) {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setBackground(box(Color.parseColor("#12141D"), 12, Color.parseColor("#1E2235"), 1));
        c.setPadding(14, 10, 14, 10);
        c.addView(tv(title, Color.parseColor("#9CA3AF"), 10.5f, true));
        TextView v = tv("--", accent, 13.5f, true);
        v.setPadding(0, 4, 0, 0);
        c.addView(v);
        if (isConv) tTopConv = v; else tTopDnpc = v;
        return c;
    }

    void switchTab(int idx) {
        vTrk.setVisibility(idx == 0 ? View.VISIBLE : View.GONE);
        vPrf.setVisibility(idx == 1 ? View.VISIBLE : View.GONE);
        vHub.setVisibility(idx == 2 ? View.VISIBLE : View.GONE);
        bT.setBackground(box(idx == 0 ? Color.parseColor("#00E676") : Color.parseColor("#1C1E2A"), 8, 0, 0));
        bT.setTextColor(idx == 0 ? Color.BLACK : Color.parseColor("#8E92A4"));
        bP.setBackground(box(idx == 1 ? Color.parseColor("#00E676") : Color.parseColor("#1C1E2A"), 8, 0, 0));
        bP.setTextColor(idx == 1 ? Color.BLACK : Color.parseColor("#8E92A4"));
        bH.setBackground(box(idx == 2 ? Color.parseColor("#00E676") : Color.parseColor("#1C1E2A"), 8, 0, 0));
        bH.setTextColor(idx == 2 ? Color.BLACK : Color.parseColor("#8E92A4"));
        if (idx == 0) cnt(); else if (idx == 1) load(); else if (idx == 2) loadHubVsHub();
    }

    void switchCategory(String cat) {
        currentCategory = cat; mode = "daily";
        bCatAgent.setBackground(box("AGENT".equals(cat) ? Color.parseColor("#00E676") : Color.parseColor("#1C1E2A"), 8, 0, 0));
        bCatAgent.setTextColor("AGENT".equals(cat) ? Color.BLACK : Color.parseColor("#8E92A4"));
        bCatKirana.setBackground(box("KIRANA".equals(cat) ? Color.parseColor("#00E676") : Color.parseColor("#1C1E2A"), 8, 0, 0));
        bCatKirana.setTextColor("KIRANA".equals(cat) ? Color.BLACK : Color.parseColor("#8E92A4"));
        bCatAll.setBackground(box("ALL".equals(cat) ? Color.parseColor("#00E676") : Color.parseColor("#1C1E2A"), 8, 0, 0));
        bCatAll.setTextColor("ALL".equals(cat) ? Color.BLACK : Color.parseColor("#8E92A4"));
        setupPeriodButtons(); load();
    }

    void setupPeriodButtons() {
        periodFilterRow.removeAllViews();
        LinearLayout.LayoutParams pLp = new LinearLayout.LayoutParams(0, -2, 1f);
        pLp.setMargins(1, 0, 1, 0);
        bSubDay = new Button(this);
        bSubDay.setText("📅 Day (Live)"); bSubDay.setTextSize(10f); bSubDay.setTypeface(Typeface.DEFAULT_BOLD);
        bSubDay.setOnClickListener(v -> { mode = "daily"; updatePeriodStyles(); load(); });
        String yTxt = "KIRANA".equals(currentCategory) ? "📊 Yearly (Cycles)" : "📊 Yearly (Weeks)";
        bSubYearly = new Button(this);
        bSubYearly.setText(yTxt); bSubYearly.setTextSize(10f); bSubYearly.setTypeface(Typeface.DEFAULT_BOLD);
        bSubYearly.setOnClickListener(v -> { mode = "yearly"; updatePeriodStyles(); load(); });
        periodFilterRow.addView(bSubDay, pLp); periodFilterRow.addView(bSubYearly, new LinearLayout.LayoutParams(pLp));
        updatePeriodStyles();
    }

    void updatePeriodStyles() {
        boolean d = "daily".equals(mode);
        bSubDay.setBackground(box(d ? Color.parseColor("#00E676") : Color.parseColor("#1C1E2A"), 8, 0, 0));
        bSubDay.setTextColor(d ? Color.BLACK : Color.parseColor("#8E92A4"));
        bSubYearly.setBackground(box(!d ? Color.parseColor("#00E676") : Color.parseColor("#1C1E2A"), 8, 0, 0));
        bSubYearly.setTextColor(!d ? Color.BLACK : Color.parseColor("#8E92A4"));
    }

    void load() {
        try {
            vCrd.removeAllViews();
            String opDate = getOperationalDate();
            String w = "daily".equals(mode) ? " WHERE dt = (SELECT MAX(dt) FROM prf) " : " WHERE dt >= '2026-01-01' ";

            Cursor hc = db.rawQuery("SELECT SUM(o), SUM(l), SUM(p), SUM(k) FROM prf " + w, null);
            if (hc != null && hc.moveToFirst()) {
                int to = hc.getInt(0), tl = hc.getInt(1), tp = hc.getInt(2), tk = hc.getInt(3);
                int tdnp = to + tp, tdnpc = tl + tk;
                double ofdC = to > 0 ? ((double) tl / to) * 100.0 : 0.0;
                double ofpC = tp > 0 ? ((double) tk / tp) * 100.0 : 0.0;
                double dnpC = tdnp > 0 ? ((double) tdnpc / tdnp) * 100.0 : 0.0;
                tHubOfdDel.setText("OFD/DEL = " + to + "/" + tl + " = " + String.format(Locale.US, "%.1f%%", ofdC));
                tHubOfpPik.setText("OFP/PIK = " + tp + "/" + tk + " = " + String.format(Locale.US, "%.1f%%", ofpC));
                tHubDnpDnpc.setText("DNP/DNPC = " + tdnp + "/" + tdnpc + " = " + String.format(Locale.US, "%.1f%%", dnpC));
                int diff = (int) Math.ceil(0.92 * to) - tl;
                if (diff <= 0 && to > 0) {
                    tGapTarget.setText("🎯 92% Target Achieved! 🚀");
                    tGapTarget.setTextColor(Color.parseColor("#00E676"));
                } else if (to > 0) {
                    tGapTarget.setText("🎯 Gap to 92%: " + diff + " more DEL required");
                    tGapTarget.setTextColor(Color.parseColor("#FB923C"));
                } else tGapTarget.setText("");
            }
            if (hc != null) hc.close();
            updatePersonalBest();

            Cursor ac = db.rawQuery("SELECT n, SUM(o), SUM(l), SUM(p), SUM(k) FROM prf " + w + " GROUP BY n", null);
            ArrayList<String[]> list = new ArrayList<>();
            String bestConvName = "--", bestDnpcName = "--";
            double maxConv = -1; int maxDnpc = -1;

            while (ac != null && ac.moveToNext()) {
                String name = ac.getString(0);
                boolean isK = isKiranaAgent(name);
                if ("AGENT".equals(currentCategory) && isK) continue;
                if ("KIRANA".equals(currentCategory) && !isK) continue;

                int o = ac.getInt(1), l = ac.getInt(2), p = ac.getInt(3), k = ac.getInt(4);
                int dnp = o + p, dnpc = l + k;
                double r = dnp > 0 ? ((double) dnpc / dnp) * 100.0 : 0.0;
                double ofdC = o > 0 ? ((double) l / o) * 100.0 : 0.0;
                double ofpC = p > 0 ? ((double) k / p) * 100.0 : 0.0;

                list.add(new String[]{name, String.valueOf(o), String.valueOf(l), String.valueOf(p), String.valueOf(k), String.valueOf(dnp), String.valueOf(dnpc), String.format(Locale.US, "%.1f", ofdC), String.valueOf(r), String.format(Locale.US, "%.1f", ofpC)});
                if (r > maxConv && dnp > 0) { maxConv = r; bestConvName = name + "\n" + String.format(Locale.US, "%.1f%%", r); }
                if (dnpc > maxDnpc) { maxDnpc = dnpc; bestDnpcName = name + "\n" + dnpc + " Done"; }
            }
            if (ac != null) ac.close();
            tTopConv.setText(bestConvName); tTopDnpc.setText(bestDnpcName);

            Collections.sort(list, (a, b) -> isHighToLow ? Double.compare(Double.parseDouble(b[8]), Double.parseDouble(a[8])) : Double.compare(Double.parseDouble(a[8]), Double.parseDouble(b[8])));

            int currentRank = 1;
            for (String[] ag : list) {
                int o = Integer.parseInt(ag[1]), l = Integer.parseInt(ag[2]);
                int p = Integer.parseInt(ag[3]), k = Integer.parseInt(ag[4]);
                int dnp = Integer.parseInt(ag[5]), dnpc = Integer.parseInt(ag[6]);
                double ofdConv = Double.parseDouble(ag[7]);
                double dnpConv = Double.parseDouble(ag[8]);
                double ofpConv = Double.parseDouble(ag[9]);
                int badgeColor = (ofdConv >= 92.0) ? Color.parseColor("#00E676") : Color.parseColor("#EF4444");

                LinearLayout card = new LinearLayout(this);
                card.setOrientation(LinearLayout.VERTICAL);
                card.setBackground(box(Color.parseColor("#12141D"), 14, Color.parseColor("#00E676"), 1));
                card.setPadding(0, 0, 0, 0);
                LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(-1, -2);
                clp.setMargins(0, 0, 0, 14);
                card.setLayoutParams(clp);

                LinearLayout topRow = new LinearLayout(this);
                topRow.setOrientation(LinearLayout.HORIZONTAL);
                topRow.setGravity(Gravity.CENTER_VERTICAL);
                topRow.setBackground(box(Color.parseColor("#171926"), 0, 0, 0));

                LinearLayout box1 = new LinearLayout(this);
                box1.setOrientation(LinearLayout.HORIZONTAL);
                box1.setGravity(Gravity.CENTER_VERTICAL);
                box1.setPadding(14, 12, 12, 12);

                String rBadge = (currentRank == 1) ? "🥇 #1" : ((currentRank == 2) ? "🥈 #2" : ((currentRank == 3) ? "🥉 #3" : "#" + currentRank));
                int rBg = (currentRank == 1) ? Color.parseColor("#EAB308") : ((currentRank == 2) ? Color.parseColor("#94A3B8") : ((currentRank == 3) ? Color.parseColor("#B45309") : Color.parseColor("#374151")));
                TextView tRnk = tv(rBadge, Color.WHITE, 11f, true);
                tRnk.setBackground(box(rBg, 6, 0, 0));
                tRnk.setPadding(8, 3, 8, 3);
                box1.addView(tRnk);

                TextView tName = tv(" " + ag[0], badgeColor, 15f, true);
                box1.addView(tName);
                topRow.addView(box1, new LinearLayout.LayoutParams(0, -1, 1f));
                card.addView(topRow);

                LinearLayout bottomRow = new LinearLayout(this);
                bottomRow.setOrientation(LinearLayout.HORIZONTAL);
                bottomRow.setGravity(Gravity.CENTER_VERTICAL);

                LinearLayout box3 = new LinearLayout(this);
                box3.setOrientation(LinearLayout.VERTICAL);
                box3.setPadding(14, 12, 12, 12);
                box3.addView(tv("🚚 OFD / DEL: " + o + " / " + l + " ➔ " + ag[7] + "% DEL", badgeColor, 13.5f, true));
                box3.addView(tv("📦 OFP / PIK: " + p + " / " + k + " ➔ " + String.format(Locale.US, "%.1f%%", ofpConv) + " PIK", Color.parseColor("#38BDF8"), 12.5f, true));
                box3.addView(tv("🔄 DNP / DNPC: " + dnp + " / " + dnpc + " ➔ " + String.format(Locale.US, "%.1f%%", dnpConv) + " DNP", Color.parseColor("#34D399"), 13.5f, true));
                bottomRow.addView(box3, new LinearLayout.LayoutParams(0, -2, 1f));

                LinearLayout box4 = new LinearLayout(this);
                box4.setOrientation(LinearLayout.VERTICAL);
                box4.setGravity(Gravity.CENTER);
                box4.setPadding(12, 10, 12, 10);
                final String agN = ag[0];

                Button bShr = new Button(this);
                bShr.setText("📢 Share");
                bShr.setBackground(box(Color.parseColor("#25D366"), 6, 0, 0));
                bShr.setTextColor(Color.BLACK);
                bShr.setTextSize(10.5f);
                bShr.setTypeface(Typeface.DEFAULT_BOLD);
                bShr.setOnClickListener(v -> shareSingleAgentReport(agN, o, l, p, k, dnp, dnpc, ofdConv, ofpConv, dnpConv));
                box4.addView(bShr);
                bottomRow.addView(box4, new LinearLayout.LayoutParams(-2, -1));
                card.addView(bottomRow);

                vCrd.addView(card);
                currentRank++;
            }
        } catch (Exception ignored) {}
    }

    void showHubShareChooserDialog() {
        String[] options = {"👥 ALL AGENT", "🏪 RATAN SARKAR", "🏢 ONLY HUB"};
        new AlertDialog.Builder(this)
            .setTitle("📢 Share Report Type")
            .setItems(options, (dialog, which) -> {
                if (which == 0) generateAndShareReport("ALL");
                else if (which == 1) generateAndShareReport("KIRANA");
                else if (which == 2) generateAndShareReport("ONLY_HUB");
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    void generateAndShareReport(String type) {
        try {
            String opDate = getOperationalDate();
            StringBuilder sb = new StringBuilder();
            sb.append("📊 *CHOPRAWBHUB_IPR REPORT*\n📅 *Date:* ").append(opDate).append("\n━━━━━━━━━━━━━━━━━━━━\n");
            Intent it = new Intent(Intent.ACTION_SEND);
            it.setType("text/plain");
            it.putExtra(Intent.EXTRA_TEXT, sb.toString());
            startActivity(Intent.createChooser(it, "📢 Share Live Report"));
        } catch (Exception e) {}
    }

    void shareSingleAgentReport(String name, int ofd, int del, int ofp, int pik, int dnp, int dnpc, double ofdC, double ofpC, double dnpC) {
        StringBuilder sb = new StringBuilder();
        sb.append("👤 *SCORECARD*\n📛 *Name:* ").append(name).append("\n🚚 *DEL:* ").append(del).append("/").append(ofd).append(" (").append(String.format(Locale.US, "%.1f%%", ofdC)).append(")\n");
        Intent it = new Intent(Intent.ACTION_SEND);
        it.setType("text/plain");
        it.putExtra(Intent.EXTRA_TEXT, sb.toString());
        startActivity(Intent.createChooser(it, "📢 Share Scorecard"));
    }

    void updatePersonalBest() {
        try {
            Cursor c = db.rawQuery("SELECT dt, SUM(o), SUM(l), (CAST(SUM(l) AS REAL)*100.0/SUM(o)) as conv FROM prf GROUP BY dt HAVING SUM(o)>0 ORDER BY conv DESC LIMIT 1", null);
            if (c != null && c.moveToFirst()) {
                tPersonalBest.setText("🏆 Hub Best: " + String.format(Locale.US, "%.1f%%", c.getDouble(3)) + " DEL (" + c.getInt(2) + "/" + c.getInt(1) + ")");
            }
            if (c != null) c.close();
        } catch (Exception ignored) {}
    }

    void loadHubVsHub() {
        try {
            vHubCrd.removeAllViews();
            Cursor c = db.rawQuery("SELECT hname, o, l, lc, p, k, kc, dnp, dnpc, tc FROM hub_prf", null);
            while (c != null && c.moveToNext()) {
                LinearLayout card = new LinearLayout(this);
                card.setOrientation(LinearLayout.VERTICAL);
                card.setBackground(box(Color.parseColor("#12141D"), 12, Color.parseColor("#1E2235"), 1));
                card.setPadding(16, 12, 16, 12);
                card.addView(tv("🏢 " + c.getString(0), Color.parseColor("#60A5FA"), 15f, true));
                vHubCrd.addView(card);
            }
            if (c != null) c.close();
        } catch (Exception ignored) {}
    }

    void qry(String q) {
        ords.clear();
        if (!q.isEmpty()) {
            Cursor c = db.rawQuery("SELECT t, d FROM ord WHERE t LIKE ? LIMIT 50", new String[]{"%" + q + "%"});
            while (c != null && c.moveToNext()) ords.add(new String[]{c.getString(0), c.getString(1)});
            if (c != null) c.close();
        }
        if (adp != null) adp.notifyDataSetChanged();
    }

    void cnt() {
        try {
            Cursor c = db.rawQuery("SELECT COUNT(*) FROM ord", null);
            if (c != null && c.moveToFirst()) tCnt.setText("📦 Total Trackable Orders: " + c.getInt(0));
            if (c != null) c.close();
        } catch (Exception ignored) {}
    }

    ArrayList<String> fastSplitCsv(String line) {
        ArrayList<String> res = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == '\"') inQuotes = !inQuotes;
            else if (ch == ',' && !inQuotes) { res.add(sb.toString()); sb.setLength(0); }
            else sb.append(ch);
        }
        res.add(sb.toString());
        return res;
    }

    void doSync(boolean isAuto) {
        new Thread(() -> {
            try {
                HttpURLConnection conn = (HttpURLConnection) new URL(CSV).openConnection();
                conn.setRequestProperty("User-Agent", "Mozilla/5.0");
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String line, opDate = getOperationalDate();
                
                ArrayList<ContentValues> tempOrd = new ArrayList<>();
                ArrayList<ContentValues> tempPrf = new ArrayList<>();

                while ((line = reader.readLine()) != null) {
                    ArrayList<String> p = fastSplitCsv(line);
                    if (p.size() >= 2 && !clean(p.get(0)).isEmpty()) {
                        String tId = clean(p.get(0)), oId = clean(p.get(1));
                        if (!tId.equalsIgnoreCase("TRACKING ID")) {
                            ContentValues cv = new ContentValues();
                            cv.put("t", tId); cv.put("d", oId.isEmpty() ? tId : oId);
                            tempOrd.add(cv);
                        }
                    }
                    if (p.size() > 4 && !clean(p.get(2)).isEmpty()) {
                        String name = clean(p.get(2));
                        if (!name.equalsIgnoreCase("NAME") && !name.contains("Total")) {
                            int o = parseInt(p.get(3)), l = parseInt(p.get(4));
                            int op = p.size() > 5 ? parseInt(p.get(5)) : 0;
                            int k = p.size() > 6 ? parseInt(p.get(6)) : 0;
                            if (o > 0 || l > 0) {
                                ContentValues cv = new ContentValues();
                                cv.put("n", name); cv.put("o", o); cv.put("l", l); cv.put("p", op); cv.put("k", k); cv.put("dt", opDate);
                                tempPrf.add(cv);
                            }
                        }
                    }
                }
                reader.close();

                db.beginTransaction();
                if (!tempOrd.isEmpty()) {
                    db.execSQL("DELETE FROM ord");
                    for (ContentValues cv : tempOrd) db.insertWithOnConflict("ord", null, cv, SQLiteDatabase.CONFLICT_REPLACE);
                }
                if (!tempPrf.isEmpty()) {
                    db.execSQL("DELETE FROM prf WHERE dt = '" + opDate + "'");
                    for (ContentValues cv : tempPrf) db.insert("prf", null, cv);
                }
                db.setTransactionSuccessful();
                db.endTransaction();

                new Handler(Looper.getMainLooper()).post(() -> {
                    load(); cnt();
                    if (!isAuto && loadingOverlay != null) loadingOverlay.setVisibility(View.GONE);
                    if (!isAuto) Toast.makeText(MainActivity.this, "✅ Synced Successfully!", Toast.LENGTH_SHORT).show();
                });
            } catch (Exception e) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    if (loadingOverlay != null) loadingOverlay.setVisibility(View.GONE);
                    if (!isAuto) Toast.makeText(MainActivity.this, "Sync Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    void showConversionCalculatorDialog() {
        LinearLayout d = new LinearLayout(this);
        d.setOrientation(LinearLayout.VERTICAL);
        d.setPadding(20, 18, 20, 18);
        d.setBackgroundColor(Color.parseColor("#0F1015"));
        d.addView(tv("🧮 CALCULATOR", Color.parseColor("#38BDF8"), 15f, true));
        new AlertDialog.Builder(this).setView(d).setPositiveButton("Close", null).show();
    }

    String clean(String s) { return s == null ? "" : s.replace("\"", "").trim(); }
    int parseInt(String s) { try { return Integer.parseInt(clean(s).replace("%", "")); } catch (Exception e) { return 0; } }
             }
