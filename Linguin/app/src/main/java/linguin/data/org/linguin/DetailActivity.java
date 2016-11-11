package linguin.data.org.linguin;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Toast;

import linguin.data.org.linguin.model.DataItem;
import linguin.data.org.linguin.sample.SampleDataProvider;

public class DetailActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

//        String itemId = getIntent().getExtras().getString(DataItemAdapter.ITEM_ID_KEY);
//        DataItem item = SampleDataProvider.dataItemMap.get(itemId);
        DataItem item = getIntent().getExtras().getParcelable(DataItemAdapter.ITEM_KEY);
        if (item != null) {
            Toast.makeText(this, "Received item " + item.getItemName(), Toast.LENGTH_SHORT).show();
        }
        else {
            Toast.makeText(this, "Didn't receive any data", Toast.LENGTH_SHORT).show();
        }
    }
}
