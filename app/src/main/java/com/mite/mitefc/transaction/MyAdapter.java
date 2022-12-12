package com.mite.mitefc.transaction;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.mite.mitefc.R;
import com.mite.mitefc.Trans;

import java.util.ArrayList;

public class MyAdapter extends RecyclerView.Adapter<MyAdapter.MyViewHolder> {

    Context context;
    ArrayList<Trans> list;

    public MyAdapter(Context context, ArrayList<Trans> list) {
        this.context = context;
        this.list = list;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item,parent,false);
        return new MyViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        Trans trans = list.get(position);
        holder.usn.setText(trans.getUSN());
        holder.date.setText(trans.getDate());
        holder.utr.setText(trans.getUtr());
        holder.amount.setText(trans.getAmount());
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder {

        TextView usn, date, utr, amount;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            usn = itemView.findViewById(R.id.itemUsn);
            date = itemView.findViewById(R.id.itemDate);
            utr = itemView.findViewById(R.id.itemUtr);
            amount = itemView.findViewById(R.id.itemAmount);
        }
    }
}