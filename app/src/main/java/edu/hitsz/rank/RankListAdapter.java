package edu.hitsz.rank;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import edu.hitsz.R;

public class RankListAdapter extends BaseAdapter {

    public interface RecordActionListener {
        void onDeleteClicked(ScoreRecord record);
    }

    private final LayoutInflater inflater;
    private final RecordActionListener actionListener;
    private final List<ScoreRecord> records = new ArrayList<>();

    public RankListAdapter(Context context, RecordActionListener actionListener) {
        this.inflater = LayoutInflater.from(context);
        this.actionListener = actionListener;
    }

    public void submitList(List<ScoreRecord> newRecords) {
        records.clear();
        records.addAll(newRecords);
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return records.size();
    }

    @Override
    public Object getItem(int position) {
        return records.get(position);
    }

    @Override
    public long getItemId(int position) {
        return records.get(position).getId();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.item_rank_record, parent, false);
            holder = new ViewHolder(convertView);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        ScoreRecord record = records.get(position);
        holder.rankIndex.setText(String.valueOf(position + 1));
        holder.playerName.setText(record.getPlayerName());
        holder.score.setText(String.valueOf(record.getScore()));
        holder.difficulty.setText(record.getDifficulty());
        holder.recordTime.setText(record.getRecordTime());
        holder.deleteButton.setOnClickListener(v -> actionListener.onDeleteClicked(record));
        return convertView;
    }

    private static class ViewHolder {
        private final TextView rankIndex;
        private final TextView playerName;
        private final TextView score;
        private final TextView difficulty;
        private final TextView recordTime;
        private final Button deleteButton;

        private ViewHolder(View itemView) {
            rankIndex = itemView.findViewById(R.id.tvRankIndex);
            playerName = itemView.findViewById(R.id.tvPlayerName);
            score = itemView.findViewById(R.id.tvScore);
            difficulty = itemView.findViewById(R.id.tvDifficulty);
            recordTime = itemView.findViewById(R.id.tvRecordTime);
            deleteButton = itemView.findViewById(R.id.btnDeleteRecord);
        }
    }
}
