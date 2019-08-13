package com.msm.findme;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.annotation.Nullable;

public class MembershipActivity extends AppCompatActivity {

    private FirebaseFirestore fmfirestore;

    private MyAdapter myAdapter;

    private RecyclerView membershiplistview;

    private RecyclerView.LayoutManager layoutManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_membership);
        fmfirestore=FirebaseFirestore.getInstance();
        membershiplistview=findViewById(R.id.membershiplist);
        layoutManager = new LinearLayoutManager(this);
        membershiplistview.setLayoutManager(layoutManager);

        if (ContextCompat.checkSelfPermission(MembershipActivity.this, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MembershipActivity.this, new String[]{Manifest.permission.READ_SMS, Manifest.permission.RECEIVE_SMS}, 210);
        }
        else {
            fmfirestore.collection("membership").orderBy("cost").addSnapshotListener(new EventListener<QuerySnapshot>() {
                @Override
                public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots, @Nullable FirebaseFirestoreException e) {
                    List<Map<String,Object>> membershipslist=new ArrayList<>();
                    for(QueryDocumentSnapshot queryDocumentSnapshot:queryDocumentSnapshots){
                        membershipslist.add(queryDocumentSnapshot.getData());
                    }
                    myAdapter=new MyAdapter(membershipslist);
                    membershiplistview.setAdapter(myAdapter);
                }
            });
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 210) {
            if (grantResults[0] == PackageManager.PERMISSION_DENIED || grantResults[1] == PackageManager.PERMISSION_DENIED) {
                MembershipActivity.this.finish();
            } else {
                if (ContextCompat.checkSelfPermission(MembershipActivity.this, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(MembershipActivity.this, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED) {
                    fmfirestore.collection("membership").orderBy("cost").addSnapshotListener(new EventListener<QuerySnapshot>() {
                        @Override
                        public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots, @Nullable FirebaseFirestoreException e) {
                            List<Map<String,Object>> membershipslist=new ArrayList<>();
                            for(QueryDocumentSnapshot queryDocumentSnapshot:queryDocumentSnapshots){
                                membershipslist.add(queryDocumentSnapshot.getData());
                            }
                            myAdapter=new MyAdapter(membershipslist);
                            membershiplistview.setAdapter(myAdapter);
                        }
                    });
                }
            }
        }
    }

    public class MyAdapter extends RecyclerView.Adapter<MyAdapter.ViewHolder> {

        public class ViewHolder extends RecyclerView.ViewHolder {
            public TextView ltt;
            public TextView lat;
            public TextView ldt;
            public LinearLayout lil;

            public ViewHolder(View itemView) {
                super(itemView);
                ltt = (TextView) itemView.findViewById(R.id.loctitletext);
                lat = (TextView) itemView.findViewById(R.id.locaddresstext);
                ldt=(TextView) itemView.findViewById(R.id.locdisttext);
                lil=itemView.findViewById(R.id.listitemlayout);
            }

            public View getlayoyt(){
                return lil;
            }


        }

        private List<Map<String,Object>> loclist;


        public MyAdapter(List<Map<String,Object>> locslist) {
            loclist=locslist;
        }

        @Override
        public MyAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            Context context = parent.getContext();
            LayoutInflater inflater = LayoutInflater.from(context);
            View contactView = inflater.inflate(R.layout.listitem, parent, false);
            MyAdapter.ViewHolder viewHolder = new MyAdapter.ViewHolder(contactView);
            return viewHolder;
        }

        @Override
        public void onBindViewHolder(MyAdapter.ViewHolder viewHolder, final int position) {
            TextView lat1=viewHolder.lat;
            TextView ltt1=viewHolder.ltt;
            TextView ldt1=viewHolder.ldt;

            ltt1.setText(loclist.get(position).get("name").toString());
            lat1.setText("Validity: "+loclist.get(position).get("days").toString()+" days");
            ldt1.setText("Rs."+loclist.get(position).get("cost").toString());

            viewHolder.getlayoyt().setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    /*String merchantMid = "";
                    String merchantKey = "gKpu7IKaLSbkchFS";
                    String orderId = "order1";
                    String channelId = "WAP";
                    String custId = "cust123";
                    String mobileNo = "7777777777";
                    String email = "username@emailprovider.com";
                    String txnAmount = "100.12";
                    String website = "WEBSTAGING";
                    String industryTypeId = "Retail";
                    String callbackUrl = "https://securegw-stage.paytm.in/theia/paytmCallback?ORDER_ID=order1";
                    TreeMap<String, String> paytmParams = new TreeMap<String, String>();
                    paytmParams.put("MID",merchantMid);
                    paytmParams.put("ORDER_ID",orderId);
                    paytmParams.put("CHANNEL_ID",channelId);
                    paytmParams.put("CUST_ID",custId);
                    paytmParams.put("MOBILE_NO",mobileNo);
                    paytmParams.put("EMAIL",email);
                    paytmParams.put("TXN_AMOUNT",txnAmount);
                    paytmParams.put("WEBSITE",website);
                    paytmParams.put("INDUSTRY_TYPE_ID",industryTypeId);
                    paytmParams.put("CALLBACK_URL", callbackUrl);
                    HashMap<String,String> paramMap = new HashMap<String,String>();
                    paramMap.put( "MID" , "rxazcv89315285244163");
                    paramMap.put( "ORDER_ID" , "order1");
                    paramMap.put( "CUST_ID" , "cust123");
                    paramMap.put( "MOBILE_NO" , "7777777777");
                    paramMap.put( "EMAIL" , "username@emailprovider.com");
                    paramMap.put( "CHANNEL_ID" , "WAP");
                    paramMap.put( "TXN_AMOUNT" , "100.12");
                    paramMap.put( "WEBSITE" , "WEBSTAGING");
                    paramMap.put( "INDUSTRY_TYPE_ID" , "Retail");
                    paramMap.put( "CALLBACK_URL", "https://securegw-stage.paytm.in/theia/paytmCallback?ORDER_ID=order1");
                    paramMap.put( "CHECKSUMHASH" , "w2QDRMgp1234567JEAPCIOmNgQvsi+BhpqijfM9KvFfRiPmGSt3Ddzw+oTaGCLneJwxFFq5mqTMwJXdQE2EzK4px2xruDqKZjHupz9yXev4=");
                    PaytmOrder Order = new PaytmOrder(paramMap);*/
                }
            });

        }
        @Override
        public int getItemCount() {
            return loclist.size();
        }

        @Override
        public long getItemId(int position) {
            return super.getItemId(position);
        }
    }
}
