package edu.hitsz;

import android.os.Bundle;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.List;

import edu.hitsz.rank.RankListAdapter;
import edu.hitsz.rank.RankRepository;
import edu.hitsz.rank.ScoreRecord;

public class RankActivity extends AppCompatActivity {

    private RankRepository repository;
    private RankListAdapter adapter;
    private TextView emptyView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rank);

        repository = new RankRepository(this);
        emptyView = findViewById(R.id.tvEmptyHint);
        ListView listView = findViewById(R.id.lvRankRecords);
        Button backButton = findViewById(R.id.btnRankBack);

        adapter = new RankListAdapter(this, this::confirmDeleteRecord);
        listView.setAdapter(adapter);
        backButton.setOnClickListener(v -> finish());

        refreshList();
    }

    private void refreshList() {
        List<ScoreRecord> records = repository.getAllRecords();
        adapter.submitList(records);
        emptyView.setText(records.isEmpty() ? "No ranking records" : "");
    }

    private void confirmDeleteRecord(ScoreRecord record) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Record")
                .setMessage("Delete this ranking record?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    repository.deleteRecord(record.getId());
                    refreshList();
                    Toast.makeText(this, "Record deleted", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
