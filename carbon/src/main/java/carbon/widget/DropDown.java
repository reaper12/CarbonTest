package carbon.widget;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;

import java.util.Arrays;

import carbon.Carbon;
import carbon.R;
import carbon.drawable.VectorDrawable;
import carbon.internal.DropDownMenu;
import carbon.recycler.ArrayAdapter;

public class DropDown extends EditText {

    public enum Mode {
        Over, Fit
    }

    DropDownMenu dropDownMenu;

    private int selectedIndex;

    Adapter defaultAdapter;

    AdapterView.OnItemSelectedListener onItemSelectedListener;

    private boolean isShowingPopup = false;

    public DropDown(Context context) {
        super(context, null, R.attr.carbon_dropDownStyle);
        initSpinner(context, null, R.attr.carbon_dropDownStyle);
    }

    public DropDown(Context context, AttributeSet attrs) {
        super(context, attrs, R.attr.carbon_dropDownStyle);
        initSpinner(context, attrs, R.attr.carbon_dropDownStyle);
    }

    public DropDown(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initSpinner(context, attrs, defStyleAttr);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public DropDown(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initSpinner(context, attrs, defStyleAttr);
    }

    private void initSpinner(Context context, AttributeSet attrs, int defStyleAttr) {
        VectorDrawable drawable = new VectorDrawable(getResources(), R.raw.carbon_dropdown);
        int size = (int) (Carbon.getDip(getContext()) * 24);
        drawable.setBounds(0, 0, size, size);
        setCompoundDrawables(null, null, drawable, null);

        TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.DropDown, defStyleAttr, R.style.carbon_DropDown);

        int theme = a.getResourceId(R.styleable.DropDown_carbon_popupTheme, -1);

        defaultAdapter = new Adapter();
        dropDownMenu = new DropDownMenu(new ContextThemeWrapper(context, theme));
        dropDownMenu.setAdapter(defaultAdapter);
        dropDownMenu.setOnItemClickedListener(onItemClickedListener);
        dropDownMenu.setOnDismissListener(() -> isShowingPopup = false);
        dropDownMenu.setMode(Mode.values()[a.getInt(R.styleable.DropDown_carbon_mode, Mode.Over.ordinal())]);

        setOnClickListener(view -> {
            dropDownMenu.show(DropDown.this);
            isShowingPopup = true;
        });

        a.recycle();
    }

    public Mode getMode() {
        return dropDownMenu.getMode();
    }

    public void setMode(Mode mode) {
        dropDownMenu.setMode(mode);
    }

    public void setSelectedIndex(int index) {
        selectedIndex = index;
        if (getAdapter() != null)
            setText(getAdapter().getItem(index).toString());
    }

    public int getSelectedIndex() {
        return selectedIndex;
    }

    public void setSelectedItem(Object item) {
        setSelectedIndex(Arrays.binarySearch(getAdapter().getItems(), item));
    }

    public Object getSelectedItem() {
        return getAdapter().getItem(selectedIndex);
    }

    public void setAdapter(final RecyclerView.Adapter adapter) {
        if (adapter == null) {
            dropDownMenu.setAdapter(defaultAdapter);
        } else {
            dropDownMenu.setAdapter(adapter);
        }
        setText(getAdapter().getItem(selectedIndex).toString());
    }

    public ArrayAdapter getAdapter() {
        return dropDownMenu.getAdapter();
    }

    RecyclerView.OnItemClickedListener onItemClickedListener = new RecyclerView.OnItemClickedListener() {
        @Override
        public void onItemClicked(View view, Object item, int position) {
            setText(item.toString());
            selectedIndex = position;
            if (onItemSelectedListener != null)
                onItemSelectedListener.onItemSelected(null, view, selectedIndex, 0);
            dropDownMenu.dismiss();
        }
    };

    public void setOnItemSelectedListener(AdapterView.OnItemSelectedListener onItemSelectedListener) {
        this.onItemSelectedListener = onItemSelectedListener;
    }

    public void setItems(Object[] items) {
        dropDownMenu.setAdapter(defaultAdapter);
        defaultAdapter.setItems(items);
        defaultAdapter.notifyDataSetChanged();
        setSelectedIndex(0);
    }

    public static class Adapter extends ArrayAdapter<ViewHolder, Object> {

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            View view = inflater.inflate(R.layout.carbon_popupmenu_item, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, final int position) {
            holder.tv.setText(items[position].toString());
            holder.itemView.setOnClickListener(view -> fireOnItemClickedEvent(holder.itemView, holder.getAdapterPosition()));
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        TextView tv;

        public ViewHolder(View itemView) {
            super(itemView);
            tv = (TextView) itemView.findViewById(R.id.carbon_itemText);
        }
    }

    @Override
    protected boolean setFrame(int l, int t, int r, int b) {
        boolean result = super.setFrame(l, t, r, b);

        if (dropDownMenu != null) {
            carbon.widget.FrameLayout container = (FrameLayout) dropDownMenu.getContentView().findViewById(R.id.carbon_popupContainer);
            if (container.getAnimator() == null)
                dropDownMenu.update();
        }

        return result;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        if (isShowingPopup)
            dropDownMenu.showImmediate(DropDown.this);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        if (isShowingPopup)
            dropDownMenu.dismissImmediate();
    }

    @Override
    public Parcelable onSaveInstanceState() {
        //begin boilerplate code that allows parent classes to save state
        Parcelable superState = super.onSaveInstanceState();

        SavedState ss = new SavedState(superState);
        //end

        ss.isShowingPopup = this.isShowingPopup ? 1 : 0;

        return ss;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        //begin boilerplate code so parent classes can restore state
        if (!(state instanceof SavedState)) {
            super.onRestoreInstanceState(state);
            return;
        }

        SavedState ss = (SavedState) state;
        super.onRestoreInstanceState(ss.getSuperState());
        //end

        this.isShowingPopup = ss.isShowingPopup > 0;
    }

    static class SavedState implements Parcelable {

        public static final SavedState EMPTY_STATE = new SavedState() {
        };

        int isShowingPopup;

        Parcelable superState;

        SavedState() {
            superState = null;
        }

        SavedState(Parcelable superState) {
            this.superState = superState != EMPTY_STATE ? superState : null;
        }

        private SavedState(Parcel in) {
            Parcelable superState = in.readParcelable(EditText.class.getClassLoader());
            this.superState = superState != null ? superState : EMPTY_STATE;
            this.isShowingPopup = in.readInt();
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(@NonNull Parcel out, int flags) {
            out.writeParcelable(superState, flags);
            out.writeInt(this.isShowingPopup);
        }

        public Parcelable getSuperState() {
            return superState;
        }

        //required field that makes Parcelables from a Parcel
        public static final Parcelable.Creator<SavedState> CREATOR =
                new Parcelable.Creator<SavedState>() {
                    public SavedState createFromParcel(Parcel in) {
                        return new SavedState(in);
                    }

                    public SavedState[] newArray(int size) {
                        return new SavedState[size];
                    }
                };
    }
}
