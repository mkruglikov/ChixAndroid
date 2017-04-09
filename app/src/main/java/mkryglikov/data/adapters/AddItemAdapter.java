package mkryglikov.data.adapters;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.List;

import mkryglikov.data.models.TobaccosExtrasResponse;
import mkryglikov.shisha.R;

public class AddItemAdapter extends RecyclerView.Adapter<AddItemAdapter.TobaccoViewHolder> {
    private List<TobaccosExtrasResponse> tobaccos;

    public static class TobaccoViewHolder extends RecyclerView.ViewHolder {
        private TextView tvName, tvPrice;
        private RelativeLayout rlAddItem;

        TobaccoViewHolder(View itemView) {
            super(itemView);
            rlAddItem = (RelativeLayout) itemView.findViewById(R.id.rlAddItem);
            tvName = (TextView) itemView.findViewById(R.id.tvName);
            tvPrice = (TextView) itemView.findViewById(R.id.tvPrice);
        }
    }

    public AddItemAdapter(List<TobaccosExtrasResponse> tobaccos) {
        this.tobaccos = tobaccos;
    }

    @Override
    public TobaccoViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.add_item_view, parent, false);
        return new TobaccoViewHolder(v);
    }

    @Override
    public void onBindViewHolder(TobaccoViewHolder holder, int i) {
        holder.rlAddItem.setTag(tobaccos.get(i).getId());
        holder.tvName.setText(tobaccos.get(i).getName());
        holder.tvPrice.setText(String.valueOf(tobaccos.get(i).getPrice()) + " \u20BD");
    }

    @Override
    public int getItemCount() {
        return tobaccos.size();
    }
}
