package in.co.gorest.grblcontroller.adapters;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

import in.co.gorest.grblcontroller.R;
import in.co.gorest.grblcontroller.model.CommandHistory;

public class CommandHistoryAdapter extends RecyclerView.Adapter<CommandHistoryAdapter.ViewHolder>{

    private final List<CommandHistory> dataSet;
    private View.OnClickListener onItemClickListener;
    private View.OnLongClickListener onLongClickListener;

    public CommandHistoryAdapter(List<CommandHistory> dataSet){
        this.dataSet = dataSet;
    }

    public void setItemLongClickListener(View.OnLongClickListener longClickListener){
        onLongClickListener = longClickListener;
    }

    public void setItemClickListener(View.OnClickListener clickListener) {
        onItemClickListener = clickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType){
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.command_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.commandText.setText(dataSet.get(position).getCommand());
    }

    @Override
    public int getItemCount() {
        return dataSet.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder{
        private final TextView commandText;

        public ViewHolder(View itemView){
            super(itemView);

            commandText = itemView.findViewById(R.id.history_command_text);
            itemView.setTag(this);
            itemView.setOnClickListener(onItemClickListener);
            itemView.setOnLongClickListener(onLongClickListener);
        }

    }
}
