package carbon.component;

import android.view.ViewGroup;

import carbon.R;
import carbon.recycler.RowFactory;

public class IconTextRow extends DataBindingComponent<IconTextItem> {
    public static final RowFactory FACTORY = IconTextRow::new;

    public IconTextRow(ViewGroup parent) {
        super(parent, R.layout.carbon_row_icontext);
    }
}
