package carbon.recycler;

import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import carbon.component.Component;

public class RowListAdapter<Type> extends ListAdapter<RowViewHolder<Type>, Type> {
    private Map<Class, Integer> types = new HashMap<>();
    private List<RowFactory> factories = new ArrayList<>();

    public RowListAdapter(Class<? extends Type> type, RowFactory factory) {
        addFactory(type, factory);
    }

    public RowListAdapter(List<Type> items, RowFactory factory) {
        super(items);
        addFactory((Class<? extends Type>) items.get(0).getClass(), factory);
    }

    public void addFactory(Class<? extends Type> type, RowFactory factory) {
        types.put(type, types.size());
        factories.add(factory);
    }

    @Override
    public RowViewHolder<Type> onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        Component component = factories.get(viewType).create(viewGroup);
        RowViewHolder viewHolder = new RowViewHolder(component.getView());
        viewHolder.setComponent(component);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(final RowViewHolder<Type> holder, final int position) {
        Type data = getItem(position);
        Component component = holder.getComponent();
        component.bind(data);
        component.getView().setOnClickListener(view -> fireOnItemClickedEvent(component.getView(), holder.getAdapterPosition()));
    }

    @Override
    public int getItemViewType(int position) {
        return types.get(getItem(position).getClass());
    }
}

