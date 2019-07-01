package com.mei.baijiaproject;

import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.mei.baijiaproject.manager.FocusLayoutManager;
import com.mei.baijiaproject.manager.MeiLayoutManager;
import com.mei.baijiaproject.manager.StackLayoutManager;

import java.util.Random;

/**
 * @author wenshi
 * @github
 * @Description
 * @since 2019/6/26
 */
public class MeiActivity extends AppCompatActivity {

    RecyclerView mRecyclerView;

    StackLayoutManager stackLayoutManager;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.mei_activity);

        mRecyclerView = findViewById(R.id.recycler);

        //mRecyclerView.setLayoutManager(new MeiLayoutManager(this));
        mRecyclerView.setLayoutManager(stackLayoutManager = new StackLayoutManager(this));

        mRecyclerView.setAdapter(new RecyclerView.Adapter() {
            @NonNull
            @Override
            public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
                View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.item_card,
                        viewGroup, false);
                view.setBackgroundColor(Color.argb(255, new Random().nextInt(255), new Random().nextInt(255), new Random().nextInt(255)));
                BaseViewHolder holder = new BaseViewHolder(view);
                return holder;
            }

            @Override
            public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, final int i) {
                ((TextView) viewHolder.itemView.findViewById(R.id.tv)).setText("" + i);
                viewHolder.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        stackLayoutManager.smoothScrollToPosition(i);
                    }
                });
            }

            @Override
            public int getItemViewType(int position) {
                return 1;
            }

            @Override
            public int getItemCount() {
                return 18;
            }
        });

    }
}

class BaseViewHolder extends RecyclerView.ViewHolder {

    public BaseViewHolder(@NonNull View itemView) {
        super(itemView);
    }
}
